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
// NOTE: No separate PoseLandmarkerOptions import — it's inner: PoseLandmarker.PoseLandmarkerOptions



data class FootDetectionResult(
    val detected: Boolean,
    val xPx: Int = 0,
    val yPx: Int = 0,
    val toeXPx: Int? = null,
    val toeYPx: Int? = null
)

class FootTracker(private val context: Context) {

    private var poseLandmarker: PoseLandmarker? = null

    fun init() {
        try {
            val baseOptions = BaseOptions.builder()
                // Use asset relative name; must be placed in android/app/src/main/assets/
                .setModelAssetPath("pose_landmarker_full.task")
                .build()

            val options = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.IMAGE)
                .build()

            poseLandmarker = PoseLandmarker.createFromOptions(context, options)
            Log.i("FootTracker", "✅ PoseLandmarker initialized (v0.10.14)")
        } catch (e: Exception) {
            Log.e("FootTracker", "❌ Init failed: ${e.message}")
        }
    }

    fun detectFoot(bitmap: Bitmap): FootDetectionResult {
        val detector = poseLandmarker ?: run {
            Log.w("FootTracker", "PoseLandmarker not initialized")
            return FootDetectionResult(false)
        }

        return try {
            val mpImage: MPImage = BitmapImageBuilder(bitmap).build()
            val result: PoseLandmarkerResult = detector.detect(mpImage)

            val poses = result.landmarks()
            if (poses.isEmpty()) {
                Log.d("FootTracker", "No pose detected")
                return FootDetectionResult(false)
            }

            // Choose the best pose (first is fine for single-person use case)
            val lm: List<NormalizedLandmark> = poses[0]

            // MediaPipe Pose indices
            val LEFT_ANKLE = 27
            val RIGHT_ANKLE = 28
            val LEFT_FOOT_INDEX = 31
            val RIGHT_FOOT_INDEX = 32

            // Fetch landmarks safely
            fun lmOrNull(i: Int): NormalizedLandmark? = lm.getOrNull(i)

            val la = lmOrNull(LEFT_ANKLE)
            val ra = lmOrNull(RIGHT_ANKLE)
            val lfi = lmOrNull(LEFT_FOOT_INDEX)
            val rfi = lmOrNull(RIGHT_FOOT_INDEX)

            if (la == null && ra == null) {
                Log.d("FootTracker", "Ankles not found in landmarks")
                return FootDetectionResult(false)
            }

            // Score left vs right: prioritize higher visibility then lower y (closer to bottom)
            data class Candidate(val side: String, val ankle: NormalizedLandmark, val toe: NormalizedLandmark?)

            val candidates = mutableListOf<Candidate>()
            if (la != null) candidates.add(Candidate("LEFT", la, lfi))
            if (ra != null) candidates.add(Candidate("RIGHT", ra, rfi))

            fun optionalToFloat(opt: java.util.Optional<Float>?): Float = opt?.orElse(0f) ?: 0f
            fun visibilityOf(n: NormalizedLandmark?): Float = optionalToFloat(n?.visibility())

            val chosen = candidates.maxWithOrNull(compareBy<Candidate> { visibilityOf(it.ankle) }
                .thenByDescending { it.ankle.y() })

            val chosenAnkle = chosen!!.ankle
            val xPx = (chosenAnkle.x() * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
            val yPx = (chosenAnkle.y() * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)

            val toe = chosen.toe
            val toeXPx = toe?.let { (it.x() * bitmap.width).toInt().coerceIn(0, bitmap.width - 1) }
            val toeYPx = toe?.let { (it.y() * bitmap.height).toInt().coerceIn(0, bitmap.height - 1) }

            Log.d(
                "FootTracker",
                "Pose detected: side=${chosen.side} ankle=(${"%.3f".format(chosenAnkle.x())},${"%.3f".format(chosenAnkle.y())}) -> px=($xPx,$yPx) vis=${"%.2f".format(visibilityOf(chosen.ankle))}"
            )

            FootDetectionResult(true, xPx, yPx, toeXPx, toeYPx)
        } catch (t: Throwable) {
            Log.e("FootTracker", "Detection failed: ${t.message}")
            FootDetectionResult(false)
        }
    }

    fun close() {
        poseLandmarker?.close()
        poseLandmarker = null
    }
}
