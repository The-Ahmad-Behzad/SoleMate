package com.solemate.app.solemate_app

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicReference
// NOTE: No separate PoseLandmarkerOptions import — it's inner: PoseLandmarker.PoseLandmarkerOptions



data class FootDetectionResult(
    val detected: Boolean,
    val imgWidth: Int = 0,
    val imgHeight: Int = 0,
    val side: String = "UNKNOWN",
    val ankleX: Float = 0f,   // normalized [0,1]
    val ankleY: Float = 0f,   // normalized [0,1]
    val toeX: Float? = null,  // normalized [0,1]
    val toeY: Float? = null,  // normalized [0,1]
    val heelX: Float? = null, // not available from PoseLandmarker; reserved
    val heelY: Float? = null,
    val visibility: Float = 0f,
    val timestampNs: Long = 0L  // For LIVE_STREAM mode
)

class FootTracker(private val context: Context) {

    private var poseLandmarker: PoseLandmarker? = null
    private var useLiveStreamMode = false
    
    // Thread-safe queue for LIVE_STREAM results
    private val resultQueue = ConcurrentLinkedQueue<FootDetectionResult>()
    private val lastResult = AtomicReference<FootDetectionResult?>(null)

    /**
     * Initialize with LIVE_STREAM mode for real-time tracking.
     * Falls back to IMAGE mode if LIVE_STREAM fails.
     */
    fun init(useLiveStream: Boolean = true) {
        useLiveStreamMode = useLiveStream
        
        try {
            val baseOptions = BaseOptions.builder()
                // Use asset relative name; must be placed in android/app/src/main/assets/
                .setModelAssetPath("pose_landmarker_full.task")
                .build()

            if (useLiveStream) {
                // === Phase 1.1: LIVE_STREAM mode for real-time tracking ===
                val options = PoseLandmarker.PoseLandmarkerOptions.builder()
                    .setBaseOptions(baseOptions)
                    .setRunningMode(RunningMode.LIVE_STREAM)
                    .setResultListener { result, image ->
                        // Process result asynchronously on MediaPipe's thread
                        processPoseResultAsync(result, image)
                    }
                    .build()

                poseLandmarker = PoseLandmarker.createFromOptions(context, options)
                Log.i("FootTracker", "✅ PoseLandmarker initialized in LIVE_STREAM mode (v0.10.14)")
            } else {
                // Fallback to IMAGE mode for backward compatibility
                val options = PoseLandmarker.PoseLandmarkerOptions.builder()
                    .setBaseOptions(baseOptions)
                    .setRunningMode(RunningMode.IMAGE)
                    .build()

                poseLandmarker = PoseLandmarker.createFromOptions(context, options)
                Log.i("FootTracker", "✅ PoseLandmarker initialized in IMAGE mode (v0.10.14)")
            }
        } catch (e: Exception) {
            Log.e("FootTracker", "❌ Init failed: ${e.message}")
            // Fallback to IMAGE mode if LIVE_STREAM fails
            if (useLiveStream) {
                Log.w("FootTracker", "Falling back to IMAGE mode")
                init(false)
            }
        }
    }
    
    /**
     * Process pose result asynchronously (called from MediaPipe's thread).
     */
    private fun processPoseResultAsync(result: PoseLandmarkerResult, image: MPImage) {
        try {
            val footResult = extractFootFromResult(result, image.width, image.height)
            if (footResult.detected) {
                resultQueue.offer(footResult.copy(timestampNs = System.nanoTime()))
                lastResult.set(footResult)
            } else {
                // Still update last result even if not detected (for occlusion tracking)
                lastResult.set(footResult)
            }
        } catch (e: Exception) {
            Log.e("FootTracker", "Error processing async result: ${e.message}")
        }
    }
    
    /**
     * Get latest result from LIVE_STREAM mode (non-blocking).
     */
    fun getLatestResult(): FootDetectionResult? {
        // Drain queue and return most recent
        var latest: FootDetectionResult? = null
        while (true) {
            val result = resultQueue.poll() ?: break
            latest = result
        }
        return latest ?: lastResult.get()
    }
    
    /**
     * Extract foot data from MediaPipe pose result (common logic for both modes).
     */
    private fun extractFootFromResult(result: PoseLandmarkerResult, imgWidth: Int, imgHeight: Int): FootDetectionResult {
        val poses = result.landmarks()
        if (poses.isEmpty()) {
            return FootDetectionResult(false, imgWidth, imgHeight)
        }

        // Choose the best pose (first is fine for single-person use case)
        val lm: List<NormalizedLandmark> = poses[0]

        // MediaPipe Pose indices
        val LEFT_ANKLE = 27
        val RIGHT_ANKLE = 28
        val LEFT_HEEL = 29
        val RIGHT_HEEL = 30
        val LEFT_FOOT_INDEX = 31
        val RIGHT_FOOT_INDEX = 32

        // Fetch landmarks safely
        fun lmOrNull(i: Int): NormalizedLandmark? = lm.getOrNull(i)

        val la = lmOrNull(LEFT_ANKLE)
        val ra = lmOrNull(RIGHT_ANKLE)
        val lh = lmOrNull(LEFT_HEEL)
        val rh = lmOrNull(RIGHT_HEEL)
        val lfi = lmOrNull(LEFT_FOOT_INDEX)
        val rfi = lmOrNull(RIGHT_FOOT_INDEX)

        if (la == null && ra == null) {
            return FootDetectionResult(false, imgWidth, imgHeight)
        }

        // Score left vs right: prioritize higher visibility then lower y (closer to bottom)
        data class Candidate(
            val side: String, 
            val ankle: NormalizedLandmark, 
            val heel: NormalizedLandmark?,
            val toe: NormalizedLandmark?
        )

        val candidates = mutableListOf<Candidate>()
        if (la != null) candidates.add(Candidate("LEFT", la, lh, lfi))
        if (ra != null) candidates.add(Candidate("RIGHT", ra, rh, rfi))

        fun optionalToFloat(opt: java.util.Optional<Float>?): Float = opt?.orElse(0f) ?: 0f
        fun visibilityOf(n: NormalizedLandmark?): Float = optionalToFloat(n?.visibility())

        val chosen = candidates.maxWithOrNull(compareBy<Candidate> { visibilityOf(it.ankle) }
            .thenByDescending { it.ankle.y() }) ?: return FootDetectionResult(false, imgWidth, imgHeight)

        val chosenAnkle = chosen.ankle
        val chosenHeel = chosen.heel
        val chosenToe = chosen.toe
        
        // Return normalized coordinates (0-1 range), clamped for safety
        val ankleX = chosenAnkle.x().coerceIn(0f, 1f)
        val ankleY = chosenAnkle.y().coerceIn(0f, 1f)
        val toeX = chosenToe?.x()?.coerceIn(0f, 1f)
        val toeY = chosenToe?.y()?.coerceIn(0f, 1f)
        val heelX = chosenHeel?.x()?.coerceIn(0f, 1f)
        val heelY = chosenHeel?.y()?.coerceIn(0f, 1f)
        val ankleVis = visibilityOf(chosen.ankle)

        return FootDetectionResult(
            detected = true,
            imgWidth = imgWidth,
            imgHeight = imgHeight,
            side = chosen.side,
            ankleX = ankleX,
            ankleY = ankleY,
            toeX = toeX,
            toeY = toeY,
            heelX = null,
            heelY = null,
            visibility = ankleVis,
            timestampNs = System.nanoTime()
        )
    }
    
    /**
     * Detect foot synchronously (for IMAGE mode or fallback).
     */
    fun detectFoot(bitmap: Bitmap): FootDetectionResult {
        val detector = poseLandmarker ?: run {
            Log.w("FootTracker", "PoseLandmarker not initialized")
            return FootDetectionResult(false)
        }

        return try {
            val mpImage: MPImage = BitmapImageBuilder(bitmap).build()
            val result: PoseLandmarkerResult = detector.detect(mpImage)
            extractFootFromResult(result, bitmap.width, bitmap.height)
        } catch (t: Throwable) {
            Log.e("FootTracker", "Detection failed: ${t.message}")  
            FootDetectionResult(false)
        }
    }
    
    /**
     * Detect foot asynchronously using LIVE_STREAM mode.
     * Call this method with MPImage, results will be available via getLatestResult().
     */
    fun detectFootAsync(mpImage: MPImage) {
        if (!useLiveStreamMode) {
            Log.w("FootTracker", "detectFootAsync called but not in LIVE_STREAM mode")
            return
        }
        
        val detector = poseLandmarker ?: run {
            Log.w("FootTracker", "PoseLandmarker not initialized")
            return
        }
        
        try {
            detector.detectAsync(mpImage, System.nanoTime())
        } catch (t: Throwable) {
            Log.e("FootTracker", "Async detection failed: ${t.message}")
        }
    }

    fun close() {
        poseLandmarker?.close()
        poseLandmarker = null
    }
}
