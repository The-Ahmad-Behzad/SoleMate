package com.solemate.app.solemate_app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.PixelCopy
import android.view.View
import android.view.ViewStub
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.drawToBitmap
import com.snap.camerakit.lenses.LensesComponent
import com.snap.camerakit.lenses.whenHasFirst
import com.snap.camerakit.support.camerax.CameraXImageProcessorSource
import com.snap.camerakit.supported
import com.snap.camerakit.*
import java.io.File

class ARActivity : AppCompatActivity() {

    private lateinit var cameraKitSession: com.snap.camerakit.Session
    private lateinit var imageProcessorSource: CameraXImageProcessorSource
    private lateinit var screenshotManager: ScreenshotManager
    
    // UI Components
    private lateinit var bottomPanel: CardView
    private lateinit var galleryPreviewCard: CardView
    private lateinit var galleryPreviewImage: ImageView
    private lateinit var galleryBadge: TextView
    private lateinit var captureButtonContainer: FrameLayout
    private lateinit var captureRing: View
    private lateinit var captureInner: View
    private lateinit var shoeCarouselContainer: LinearLayout

    companion object {
        // TODO: Paste your Lens IDs here
        const val LENS_GROUP_ID = "aee0a448-e956-4af3-9826-ea32e5a2092e"
        const val LENS_ID = "ea4344fa-399b-45ea-814b-ea4b165a6afc"
        private const val TAG = "ARActivity"
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startPreview()
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_LONG).show()
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ar)
        
        // Register with MainActivity
        MainActivity.currentARActivity = this
        screenshotManager = ScreenshotManager(this)

        setupUI()
        
        if (!supported(this)) {
            Log.e(TAG, "Camera Kit not supported on this device")
            Toast.makeText(this, "AR not supported", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupCameraKit()
        updateGalleryPreview()
    }
    
    private fun setupUI() {
        // Find Views
        bottomPanel = findViewById(R.id.bottom_panel)
        galleryPreviewCard = findViewById(R.id.btn_gallery)
        galleryPreviewImage = findViewById(R.id.gallery_preview_image)
        galleryBadge = findViewById(R.id.gallery_badge)
        captureButtonContainer = findViewById(R.id.btn_capture_container)
        captureRing = findViewById(R.id.capture_ring)
        captureInner = findViewById(R.id.capture_inner)
        shoeCarouselContainer = findViewById(R.id.shoe_carousel_container)
        
        // Listeners
        findViewById<ImageButton>(R.id.btn_back).setOnClickListener { onBackPressed() }
        findViewById<ImageButton>(R.id.btn_settings).setOnClickListener { 
            Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show() 
        }
        findViewById<ImageButton>(R.id.btn_share).setOnClickListener { shareLatestSnap() }
        
        galleryPreviewCard.setOnClickListener { openImageManager() }
        
        captureButtonContainer.setOnClickListener { 
            animateCapture()
            captureScreenshot()
        }
        
        captureButtonContainer.setOnLongClickListener {
            Toast.makeText(this, "Video recording coming soon", Toast.LENGTH_SHORT).show()
            true
        }
        
        // Populate Shoe Carousel (Mock data for now)
        populateShoeCarousel()
    }

    private fun setupCameraKit() {
        imageProcessorSource = CameraXImageProcessorSource(
            context = this,
            lifecycleOwner = this
        )

        // Check Permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
                == PackageManager.PERMISSION_GRANTED) {
            startPreview()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        // Initialize Session
        // Initialize Session
        val viewStub = findViewById<ViewStub>(R.id.camera_kit_stub)
        
        // Use the factory function from camerakit-kotlin
        cameraKitSession = Session(this) {
            imageProcessorSource(this@ARActivity.imageProcessorSource)
            attachTo(viewStub)
        }
        
        cameraKitSession.lenses.repository.observe(
            LensesComponent.Repository.QueryCriteria.ById(LENS_ID, LENS_GROUP_ID)
        ) { result ->
            result.whenHasFirst { requestedLens ->
                cameraKitSession.lenses.processor.apply(requestedLens)
                Log.d(TAG, "Lens applied: ${requestedLens.id}")
            }
        }
    }

    private fun startPreview() {
        imageProcessorSource.startPreview(facingFront = false)
    }

    private fun captureScreenshot() {
        if (!screenshotManager.canCapture()) {
            Toast.makeText(this, "Gallery full! Delete some snaps.", Toast.LENGTH_SHORT).show()
            return
        }

        // Capture logic: Use PixelCopy or drawToBitmap on the root view/surface
        // Since CameraKit renders to a SurfaceView inside ViewStub, we try to get that
        try {
            // Find the SurfaceView inflated by CameraKit (usually it's the first child of the inflated layout)
            // Or simpler: capture the whole window
            val rootView = window.decorView.rootView
            
            // For SurfaceView content, simple drawToBitmap won't work. We need PixelCopy.
            // But implementing PixelCopy requires finding the Surface.
            // Simplified fallback: Capture the view drawing cache (might miss SurfaceView content)
            // OR use the MediaProjection API (complicated)
            
            // NOTE: Ideally we use cameraKitSession.processor.capture() if available.
            // For this migration, we'll try a generic window capture or mock it if complex.
            // However, PixelCopy is the standard way for SurfaceView.
            
            val bitmap = Bitmap.createBitmap(rootView.width, rootView.height, Bitmap.Config.ARGB_8888)
            val locationOfViewInWindow = IntArray(2)
            rootView.getLocationInWindow(locationOfViewInWindow)
            
            // Hide UI for clean capture
            hideUI()
            
            // Allow a small delay for UI to disappear (PixelCopy takes current surface state)
            // Note: PixelCopy from Window might still capture the frame buffer as is. 
            // Ideally we wait for next vsync, but a small handler delay is a simple workaround.
            Handler(Looper.getMainLooper()).postDelayed({
                PixelCopy.request(window, 
                    android.graphics.Rect(0, 0, rootView.width, rootView.height), 
                    bitmap, { copyResult ->
                        runOnUiThread {
                            // Restore UI
                            showUI()
                            
                            if (copyResult == PixelCopy.SUCCESS) {
                                if (screenshotManager.captureAndSave(bitmap)) {
                                    Toast.makeText(this, "Snap Saved!", Toast.LENGTH_SHORT).show()
                                    updateGalleryPreview()
                                } else {
                                    bitmap.recycle()
                                }
                            } else {
                                Log.e(TAG, "PixelCopy failed: $copyResult")
                                bitmap.recycle()
                                Toast.makeText(this, "Capture failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }, Handler(Looper.getMainLooper())
                )
            }, 50)
            
        } catch (e: Exception) {
            Log.e(TAG, "Capture failed: ${e.message}")
            showUI()
        }
    }
    
    private fun hideUI() {
        bottomPanel.visibility = View.INVISIBLE
        findViewById<View>(R.id.btn_back).visibility = View.INVISIBLE
        findViewById<View>(R.id.btn_settings).visibility = View.INVISIBLE
        findViewById<View>(R.id.shoe_carousel_scroll).visibility = View.INVISIBLE
    }
    
    private fun showUI() {
        bottomPanel.visibility = View.VISIBLE
        findViewById<View>(R.id.btn_back).visibility = View.VISIBLE
        findViewById<View>(R.id.btn_settings).visibility = View.VISIBLE
        findViewById<View>(R.id.shoe_carousel_scroll).visibility = View.VISIBLE
    }
    
    private fun animateCapture() {
        captureInner.animate().scaleX(0.8f).scaleY(0.8f).setDuration(100).withEndAction {
            captureInner.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
        }.start()
        
        // Flash effect
        val flash = View(this)
        flash.setBackgroundColor(android.graphics.Color.WHITE)
        flash.alpha = 0f
        addContentView(flash, androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.MATCH_PARENT, 
            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.MATCH_PARENT))
            
        flash.animate().alpha(0.8f).setDuration(50).withEndAction {
            flash.animate().alpha(0f).setDuration(200).withEndAction {
                (flash.parent as? android.view.ViewGroup)?.removeView(flash)
            }.start()
        }.start()
    }
    
    private fun updateGalleryPreview() {
        val thumbnail = screenshotManager.getLatestThumbnail()
        val count = screenshotManager.getSnapCount()
        
        if (thumbnail != null) {
            galleryPreviewImage.setImageBitmap(thumbnail)
            galleryBadge.text = count.toString()
            galleryBadge.visibility = View.VISIBLE
        } else {
            galleryPreviewImage.setImageDrawable(null)
            galleryBadge.visibility = View.GONE
        }
    }
    
    private fun openImageManager() {
         val snapPaths = screenshotManager.getAllSnapPaths()
         if (snapPaths.isNotEmpty()) {
             MainActivity.notifyFlutterToOpenImageManager(snapPaths)
             val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
             }
             startActivity(intent)
         } else {
             Toast.makeText(this, "Gallery is empty", Toast.LENGTH_SHORT).show()
         }
    }
    
    private fun shareLatestSnap() {
        val paths = screenshotManager.getAllSnapPaths()
        if (paths.isNotEmpty()) {
            val lastPath = paths.last()
            // Trigger generic share
            // TODO: Implement cleaner share via FileProvider
            Toast.makeText(this, "Sharing latest snap...", Toast.LENGTH_SHORT).show()
            // Actual share logic would go here (Intent.ACTION_SEND)
        } else {
            Toast.makeText(this, "No snaps to share", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun populateShoeCarousel() {
        // Add dummy items
        val shoes = listOf(
            android.graphics.Color.RED, 
            android.graphics.Color.BLUE, 
            android.graphics.Color.GREEN,
            android.graphics.Color.YELLOW,
            android.graphics.Color.CYAN
        )
        
        shoes.forEach { color ->
            val item = CardView(this).apply {
                radius = 40f 
                cardElevation = 4f
                setCardBackgroundColor(color)
                layoutParams = LinearLayout.LayoutParams(160, 160).apply {
                    marginEnd = 32
                }
            }
            shoeCarouselContainer.addView(item)
        }
    }

    override fun onDestroy() {
        cameraKitSession.close()
        if (MainActivity.currentARActivity == this) {
            MainActivity.currentARActivity = null
        }
        super.onDestroy()
    }
    
    // Platform channel methods
    fun getSnapPaths(): List<String> = screenshotManager.getAllSnapPaths()
    fun getSnapCount(): Int = screenshotManager.getSnapCount()
    fun clearAllSnaps() {
        screenshotManager.clearAll()
        updateGalleryPreview()
    }
    fun deleteSnapByPath(path: String): Boolean {
        val success = screenshotManager.deleteSnapByPath(path)
        if (success) updateGalleryPreview()
        return success
    }
    
    fun setFilter(path: String, filterName: String?) {
        screenshotManager.setFilter(path, filterName)
    }
    
    fun getFilters(): Map<String, String?> {
        val filters = mutableMapOf<String, String?>()
        screenshotManager.getAllSnapPaths().forEach { path ->
            filters[path] = screenshotManager.getFilter(path)
        }
        return filters
    }
}
