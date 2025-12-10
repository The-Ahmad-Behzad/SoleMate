package com.solemate.app.solemate_app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicInteger

/**
 * Represents a detected foot with tracking information
 */
data class DetectedFoot(
    val id: Int,                    // Tracking ID (persists across frames)
    val label: String,              // "left_foot" or "right_foot"
    val boundingBox: RectF,         // Normalized bounding box [0,1]
    val confidence: Float,          // Detection confidence
    val centerX: Float,             // Bounding box center X (normalized)
    val centerY: Float,             // Bounding box center Y (normalized)
    val width: Float,               // Bounding box width (normalized)
    val height: Float,              // Bounding box height (normalized)
    val orientation: Float = 0f     // Roll orientation in degrees (from image moments)
)

/**
 * Foot Object Detector using TFLite Person-Foot-Detection model
 * 
 * This replaces the pose landmark-based FootTracker with object detection.
 * The model detects persons and feet, outputting bounding boxes.
 * 
 * Model: qualcomm/Person-Foot-Detection (w8a8 quantized)
 * Input: 640x480 RGB image
 * Output: Bounding boxes for persons and feet
 */
class FootObjectDetector(private val context: Context) {
    
    companion object {
        private const val TAG = "FootObjectDetector"
        private const val MODEL_NAME = "Person-Foot-Detection.tflite"
        
        // Model input dimensions
        private const val INPUT_WIDTH = 640
        private const val INPUT_HEIGHT = 480
        
        // Detection thresholds
        private const val CONFIDENCE_THRESHOLD = 0.5f
        private const val NMS_THRESHOLD = 0.4f  // Non-maximum suppression threshold
        
        // Class labels (based on model documentation)
        private const val CLASS_PERSON = 0
        private const val CLASS_FOOT = 1
    }
    
    private var interpreter: Interpreter? = null
    private var imageProcessor: ImageProcessor? = null
    
    // Simple tracking: assign IDs based on position matching
    private val nextTrackingId = AtomicInteger(1)
    private var previousDetections: List<DetectedFoot> = emptyList()
    
    /**
     * Initialize the TFLite interpreter and image processor
     */
    fun init() {
        try {
            // Load model from assets
            val modelBuffer = FileUtil.loadMappedFile(context, MODEL_NAME)
            
            // Configure interpreter options
            val options = Interpreter.Options().apply {
                numThreads = 4
            }
            
            interpreter = Interpreter(modelBuffer, options)
            
            // Setup image processor for preprocessing
            imageProcessor = ImageProcessor.Builder()
                .add(ResizeOp(INPUT_HEIGHT, INPUT_WIDTH, ResizeOp.ResizeMethod.BILINEAR))
                .add(NormalizeOp(0f, 255f))  // Normalize to [0, 1]
                .build()
            
            Log.i(TAG, "✅ FootObjectDetector initialized with model: $MODEL_NAME")
            
            // Log model input/output info
            interpreter?.let { interp ->
                val inputTensor = interp.getInputTensor(0)
                val outputTensor = interp.getOutputTensor(0)
                Log.d(TAG, "Input shape: ${inputTensor.shape().contentToString()}")
                Log.d(TAG, "Output shape: ${outputTensor.shape().contentToString()}")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize FootObjectDetector: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Detect feet in the given bitmap image
     * 
     * @param bitmap Input camera frame
     * @return List of detected feet with tracking IDs
     */
    fun detectFeet(bitmap: Bitmap): List<DetectedFoot> {
        val interp = interpreter ?: run {
            Log.w(TAG, "Interpreter not initialized")
            return emptyList()
        }
        
        return try {
            // Resize bitmap to model input size
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_WIDTH, INPUT_HEIGHT, true)
            
            // Model is quantized (w8a8) - expects UINT8 input (1 byte per value), NOT float32
            // Input size: width * height * 3 channels = 640 * 480 * 3 = 921,600 bytes
            val inputBuffer = ByteBuffer.allocateDirect(INPUT_WIDTH * INPUT_HEIGHT * 3)
            inputBuffer.order(ByteOrder.nativeOrder())
            
            // Convert bitmap to byte buffer (RGB, 0-255 range as bytes)
            val pixels = IntArray(INPUT_WIDTH * INPUT_HEIGHT)
            resizedBitmap.getPixels(pixels, 0, INPUT_WIDTH, 0, 0, INPUT_WIDTH, INPUT_HEIGHT)
            
            for (pixel in pixels) {
                // Extract RGB as bytes (0-255 range)
                inputBuffer.put(((pixel shr 16) and 0xFF).toByte())  // R
                inputBuffer.put(((pixel shr 8) and 0xFF).toByte())   // G  
                inputBuffer.put((pixel and 0xFF).toByte())           // B
            }
            inputBuffer.rewind()
            
            // Recycle resized bitmap if different from input
            if (resizedBitmap != bitmap) {
                resizedBitmap.recycle()
            }
            
            // Prepare output buffers - model output is also quantized [1, 120, 160, 3]
            val outputShape = interp.getOutputTensor(0).shape()
            val outputSize = outputShape.reduce { acc, i -> acc * i }
            // Output is also uint8 for quantized model
            val outputBuffer = ByteBuffer.allocateDirect(outputSize)
                .order(ByteOrder.nativeOrder())
            
            Log.d(TAG, "Running inference: input=${INPUT_WIDTH}x${INPUT_HEIGHT}x3 (${inputBuffer.capacity()} bytes), output shape=${outputShape.contentToString()}")
            
            // Run inference
            interp.run(inputBuffer, outputBuffer)
            
            // Parse detections
            val detections = parseDetections(outputBuffer, outputShape, bitmap.width, bitmap.height)
            
            // Apply simple tracking (match with previous detections by IoU)
            val trackedDetections = assignTrackingIds(detections)
            
            previousDetections = trackedDetections
            
            if (trackedDetections.isNotEmpty()) {
                Log.d(TAG, "Detected ${trackedDetections.size} feet: ${trackedDetections.map { "${it.label}(${it.id})" }}")
            }
            
            trackedDetections
            
        } catch (e: Exception) {
            Log.e(TAG, "Detection failed: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }
    
    /**
     * Parse segmentation mask output into DetectedFoot objects
     * 
     * Model output format: [1, 120, 160, 3] = segmentation mask
     * - 120 rows x 160 cols (downscaled from 480x640)
     * - 3 channels: likely [background, person, foot] class probabilities
     */
    private fun parseDetections(
        outputBuffer: ByteBuffer,
        outputShape: IntArray,
        imageWidth: Int,
        imageHeight: Int
    ): List<DetectedFoot> {
        outputBuffer.rewind()
        
        val detections = mutableListOf<DetectedFoot>()
        
        // Expected shape: [1, 120, 160, 3]
        if (outputShape.size != 4) {
            Log.w(TAG, "Unexpected output shape: ${outputShape.contentToString()}")
            return emptyList()
        }
        
        val maskHeight = outputShape[1]  // 120
        val maskWidth = outputShape[2]   // 160
        val numClasses = outputShape[3]  // 3
        
        Log.d(TAG, "Parsing segmentation mask: ${maskHeight}x${maskWidth}x${numClasses}")
        
        // Read mask data (uint8 values 0-255)
        val maskData = ByteArray(maskHeight * maskWidth * numClasses)
        outputBuffer.get(maskData)
        
        // Find foot pixels (class index 1 for foot, assuming [background=0, person=1, foot=2] or similar)
        // We'll try both class 1 and class 2 to see which has foot-like regions
        val footClass = if (numClasses >= 3) 2 else 1  // Assume foot is class 2 if 3 classes
        
        // Track bounding boxes for foot regions
        var leftMinX = Int.MAX_VALUE
        var leftMaxX = Int.MIN_VALUE
        var leftMinY = Int.MAX_VALUE
        var leftMaxY = Int.MIN_VALUE
        
        var rightMinX = Int.MAX_VALUE
        var rightMaxX = Int.MIN_VALUE
        var rightMinY = Int.MAX_VALUE
        var rightMaxY = Int.MIN_VALUE
        
        // Collect pixel coordinates for orientation calculation
        val leftFootPixels = mutableListOf<Pair<Int, Int>>()
        val rightFootPixels = mutableListOf<Pair<Int, Int>>()
        
        // Scan mask for foot pixels
        for (y in 0 until maskHeight) {
            for (x in 0 until maskWidth) {
                val idx = (y * maskWidth + x) * numClasses
                
                // Get class scores
                val scores = FloatArray(numClasses)
                for (c in 0 until numClasses) {
                    scores[c] = (maskData[idx + c].toInt() and 0xFF) / 255f
                }
                
                // Find argmax (highest scoring class)
                val maxClass = scores.indices.maxByOrNull { scores[it] } ?: 0
                val maxScore = scores[maxClass]
                
                // Check if this pixel is foot class with sufficient confidence
                if (maxClass == footClass && maxScore >= CONFIDENCE_THRESHOLD) {
                    // Determine left/right based on x position (left half vs right half)
                    val isLeftSide = x < maskWidth / 2
                    
                    if (isLeftSide) {
                        leftMinX = minOf(leftMinX, x)
                        leftMaxX = maxOf(leftMaxX, x)
                        leftMinY = minOf(leftMinY, y)
                        leftMaxY = maxOf(leftMaxY, y)
                        leftFootPixels.add(x to y)
                    } else {
                        rightMinX = minOf(rightMinX, x)
                        rightMaxX = maxOf(rightMaxX, x)
                        rightMinY = minOf(rightMinY, y)
                        rightMaxY = maxOf(rightMaxY, y)
                        rightFootPixels.add(x to y)
                    }
                }
            }
        }
        
        val minPixels = 50  // Minimum pixels to consider a valid foot region
        
        // Create detection for left foot if found
        if (leftFootPixels.size >= minPixels) {
            val x1 = leftMinX.toFloat() / maskWidth
            val y1 = leftMinY.toFloat() / maskHeight
            val x2 = leftMaxX.toFloat() / maskWidth
            val y2 = leftMaxY.toFloat() / maskHeight
            
            // Compute orientation using image moments
            val leftOrientation = computeOrientation(leftFootPixels)
            
            detections.add(DetectedFoot(
                id = 0,
                label = "left_foot",
                boundingBox = RectF(x1, y1, x2, y2),
                confidence = leftFootPixels.size.toFloat() / (maskWidth * maskHeight),
                centerX = (x1 + x2) / 2f,
                centerY = (y1 + y2) / 2f,
                width = x2 - x1,
                height = y2 - y1,
                orientation = leftOrientation
            ))
            Log.d(TAG, "Found left foot: pixels=${leftFootPixels.size}, orientation=${leftOrientation}°")
        }
        
        // Create detection for right foot if found
        if (rightFootPixels.size >= minPixels) {
            val x1 = rightMinX.toFloat() / maskWidth
            val y1 = rightMinY.toFloat() / maskHeight
            val x2 = rightMaxX.toFloat() / maskWidth
            val y2 = rightMaxY.toFloat() / maskHeight
            
            // Compute orientation using image moments
            val rightOrientation = computeOrientation(rightFootPixels)
            
            detections.add(DetectedFoot(
                id = 0,
                label = "right_foot",
                boundingBox = RectF(x1, y1, x2, y2),
                confidence = rightFootPixels.size.toFloat() / (maskWidth * maskHeight),
                centerX = (x1 + x2) / 2f,
                centerY = (y1 + y2) / 2f,
                width = x2 - x1,
                height = y2 - y1,
                orientation = rightOrientation
            ))
            Log.d(TAG, "Found right foot: pixels=${rightFootPixels.size}, orientation=${rightOrientation}°")
        }
        
        if (detections.isEmpty()) {
            // Try alternative: check if any class has significant pixels
            val classPixelCounts = IntArray(numClasses)
            outputBuffer.rewind()
            outputBuffer.get(maskData)
            
            for (y in 0 until maskHeight) {
                for (x in 0 until maskWidth) {
                    val idx = (y * maskWidth + x) * numClasses
                    var maxClass = 0
                    var maxVal = 0
                    for (c in 0 until numClasses) {
                        val v = maskData[idx + c].toInt() and 0xFF
                        if (v > maxVal) {
                            maxVal = v
                            maxClass = c
                        }
                    }
                    classPixelCounts[maxClass]++
                }
            }
            Log.d(TAG, "Class pixel distribution: ${classPixelCounts.contentToString()}")
        }
        
        return detections
    }
    
    /**
     * Compute the orientation (roll angle) of a foot using image moments.
     * 
     * This calculates the principal axis of the foot shape using second-order
     * central moments (similar to PCA). The angle of the principal axis
     * indicates the foot's roll orientation.
     * 
     * @param pixels List of (x, y) pixel coordinates belonging to the foot
     * @return Orientation angle in degrees (-90 to +90)
     */
    private fun computeOrientation(pixels: List<Pair<Int, Int>>): Float {
        if (pixels.size < 10) return 0f  // Not enough data
        
        // Compute centroid
        var sumX = 0.0
        var sumY = 0.0
        for ((x, y) in pixels) {
            sumX += x
            sumY += y
        }
        val cx = sumX / pixels.size
        val cy = sumY / pixels.size
        
        // Compute second-order central moments
        var mu11 = 0.0  // Mixed moment (covariance)
        var mu20 = 0.0  // Variance in X
        var mu02 = 0.0  // Variance in Y
        
        for ((x, y) in pixels) {
            val dx = x - cx
            val dy = y - cy
            mu11 += dx * dy
            mu20 += dx * dx
            mu02 += dy * dy
        }
        
        // Compute orientation using atan2
        // This gives the angle of the principal axis
        val theta = 0.5 * kotlin.math.atan2(2.0 * mu11, mu20 - mu02)
        
        // Convert to degrees
        val degrees = Math.toDegrees(theta).toFloat()
        
        // Normalize to -90 to +90 range
        return when {
            degrees > 90 -> degrees - 180
            degrees < -90 -> degrees + 180
            else -> degrees
        }
    }
    /**
     * Apply Non-Maximum Suppression to remove overlapping detections
     */
    private fun applyNMS(detections: List<DetectedFoot>): List<DetectedFoot> {
        if (detections.isEmpty()) return emptyList()
        
        val sorted = detections.sortedByDescending { it.confidence }
        val kept = mutableListOf<DetectedFoot>()
        val suppressed = BooleanArray(sorted.size)
        
        for (i in sorted.indices) {
            if (suppressed[i]) continue
            
            kept.add(sorted[i])
            
            for (j in i + 1 until sorted.size) {
                if (suppressed[j]) continue
                
                val iou = calculateIoU(sorted[i].boundingBox, sorted[j].boundingBox)
                if (iou > NMS_THRESHOLD) {
                    suppressed[j] = true
                }
            }
        }
        
        return kept
    }
    
    /**
     * Calculate Intersection over Union (IoU) for two bounding boxes
     */
    private fun calculateIoU(box1: RectF, box2: RectF): Float {
        val intersectLeft = maxOf(box1.left, box2.left)
        val intersectTop = maxOf(box1.top, box2.top)
        val intersectRight = minOf(box1.right, box2.right)
        val intersectBottom = minOf(box1.bottom, box2.bottom)
        
        if (intersectLeft >= intersectRight || intersectTop >= intersectBottom) {
            return 0f
        }
        
        val intersectArea = (intersectRight - intersectLeft) * (intersectBottom - intersectTop)
        val box1Area = box1.width() * box1.height()
        val box2Area = box2.width() * box2.height()
        val unionArea = box1Area + box2Area - intersectArea
        
        return if (unionArea > 0) intersectArea / unionArea else 0f
    }
    
    /**
     * Assign tracking IDs to detections by matching with previous frame
     * Uses IoU for matching - if IoU > threshold, reuse previous ID
     */
    private fun assignTrackingIds(detections: List<DetectedFoot>): List<DetectedFoot> {
        if (detections.isEmpty()) return emptyList()
        
        val result = mutableListOf<DetectedFoot>()
        val usedPreviousIds = mutableSetOf<Int>()
        
        for (detection in detections) {
            var bestMatch: DetectedFoot? = null
            var bestIoU = 0.3f  // Minimum IoU threshold for matching
            
            // Find best matching previous detection
            for (prev in previousDetections) {
                if (prev.id in usedPreviousIds) continue
                
                val iou = calculateIoU(detection.boundingBox, prev.boundingBox)
                if (iou > bestIoU) {
                    bestIoU = iou
                    bestMatch = prev
                }
            }
            
            val trackingId = if (bestMatch != null) {
                usedPreviousIds.add(bestMatch.id)
                bestMatch.id
            } else {
                nextTrackingId.getAndIncrement()
            }
            
            result.add(detection.copy(id = trackingId))
        }
        
        return result
    }
    
    /**
     * Release resources
     */
    fun close() {
        try {
            interpreter?.close()
            interpreter = null
            previousDetections = emptyList()
            Log.i(TAG, "FootObjectDetector closed")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing detector: ${e.message}")
        }
    }
}
