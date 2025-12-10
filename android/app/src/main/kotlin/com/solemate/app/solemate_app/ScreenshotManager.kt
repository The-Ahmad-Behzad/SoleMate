package com.solemate.app.solemate_app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Manages captured AR screenshots during a session.
 * Stores full-res images in cache directory and thumbnails in memory.
 * Maximum 10 snaps per session.
 */
class ScreenshotManager(private val context: Context) {
    
    companion object {
        private const val TAG = "ScreenshotManager"
        const val MAX_SNAPS = 10
        private const val THUMBNAIL_SIZE = 200 // pixels
        private const val SNAP_DIR = "ar_snaps"
    }
    
    // Full-res image paths stored in cache
    private val snapPaths = mutableListOf<String>()
    
    // Thumbnails kept in memory for quick access
    private val thumbnailCache = mutableMapOf<String, Bitmap>()
    
    // Filter state for each snap (null = no filter applied)
    private val appliedFilters = mutableMapOf<String, String?>()
    
    init {
        // Ensure snap directory exists
        getSnapDirectory().mkdirs()
    }
    
    private fun getSnapDirectory(): File {
        return File(context.cacheDir, SNAP_DIR)
    }
    
    /**
     * Capture and save a bitmap as PNG.
     * Returns true if successful, false if max snaps reached or error occurred.
     */
    fun captureAndSave(bitmap: Bitmap): Boolean {
        if (snapPaths.size >= MAX_SNAPS) {
            Log.w(TAG, "Maximum snaps ($MAX_SNAPS) reached")
            return false
        }
        
        return try {
            val snapId = UUID.randomUUID().toString()
            val snapFile = File(getSnapDirectory(), "$snapId.png")
            
            // Save full-res PNG to cache
            FileOutputStream(snapFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            
            val path = snapFile.absolutePath
            snapPaths.add(path)
            appliedFilters[path] = null
            
            // Create and cache thumbnail
            val thumbnail = createThumbnail(bitmap)
            thumbnailCache[path] = thumbnail
            
            Log.d(TAG, "Snap saved: $path (${snapPaths.size}/$MAX_SNAPS)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save snap: ${e.message}")
            false
        }
    }
    
    private fun createThumbnail(original: Bitmap): Bitmap {
        val aspectRatio = original.width.toFloat() / original.height.toFloat()
        val thumbWidth: Int
        val thumbHeight: Int
        
        if (aspectRatio > 1) {
            thumbWidth = THUMBNAIL_SIZE
            thumbHeight = (THUMBNAIL_SIZE / aspectRatio).toInt()
        } else {
            thumbHeight = THUMBNAIL_SIZE
            thumbWidth = (THUMBNAIL_SIZE * aspectRatio).toInt()
        }
        
        return Bitmap.createScaledBitmap(original, thumbWidth, thumbHeight, true)
    }
    
    /**
     * Get current snap count.
     */
    fun getSnapCount(): Int = snapPaths.size
    
    /**
     * Check if more snaps can be captured.
     */
    fun canCapture(): Boolean = snapPaths.size < MAX_SNAPS
    
    /**
     * Get the latest captured thumbnail, or null if no snaps.
     */
    fun getLatestThumbnail(): Bitmap? {
        if (snapPaths.isEmpty()) return null
        return thumbnailCache[snapPaths.last()]
    }
    
    /**
     * Get thumbnail for a specific snap path.
     */
    fun getThumbnail(path: String): Bitmap? {
        // Return cached thumbnail or load from file
        thumbnailCache[path]?.let { return it }
        
        return try {
            val options = BitmapFactory.Options().apply {
                inSampleSize = 4 // Quick decode for thumbnail
            }
            val bitmap = BitmapFactory.decodeFile(path, options)
            bitmap?.let {
                val thumb = createThumbnail(it)
                thumbnailCache[path] = thumb
                it.recycle()
                thumb
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load thumbnail: ${e.message}")
            null
        }
    }
    
    /**
     * Get all snap paths.
     */
    fun getAllSnapPaths(): List<String> = snapPaths.toList()
    
    /**
     * Delete a snap at the given index.
     * Returns true if successful.
     */
    fun deleteSnap(index: Int): Boolean {
        if (index < 0 || index >= snapPaths.size) {
            Log.w(TAG, "Invalid snap index: $index")
            return false
        }
        
        return try {
            val path = snapPaths[index]
            File(path).delete()
            snapPaths.removeAt(index)
            thumbnailCache.remove(path)?.recycle()
            appliedFilters.remove(path)
            Log.d(TAG, "Snap deleted: $path")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete snap: ${e.message}")
            false
        }
    }
    
    /**
     * Delete a snap by its path.
     */
    fun deleteSnapByPath(path: String): Boolean {
        val index = snapPaths.indexOf(path)
        return if (index >= 0) deleteSnap(index) else false
    }
    
    /**
     * Clear all snaps and cached data.
     */
    fun clearAll() {
        Log.d(TAG, "Clearing all ${snapPaths.size} snaps")
        
        // Delete all snap files
        snapPaths.forEach { path ->
            try {
                File(path).delete()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete $path: ${e.message}")
            }
        }
        
        // Recycle all thumbnails
        thumbnailCache.values.forEach { it.recycle() }
        
        // Clear collections
        snapPaths.clear()
        thumbnailCache.clear()
        appliedFilters.clear()
    }
    
    /**
     * Set applied filter for a snap.
     */
    fun setFilter(path: String, filterName: String?) {
        if (snapPaths.contains(path)) {
            appliedFilters[path] = filterName
        }
    }
    
    /**
     * Get applied filter for a snap.
     */
    fun getFilter(path: String): String? = appliedFilters[path]
    
    /**
     * Check if there are any unsaved snaps.
     */
    fun hasUnsavedSnaps(): Boolean = snapPaths.isNotEmpty()
    
    /**
     * Load full-res bitmap for a snap path.
     * Caller is responsible for recycling the bitmap.
     */
    fun loadFullBitmap(path: String): Bitmap? {
        return try {
            BitmapFactory.decodeFile(path)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load bitmap: ${e.message}")
            null
        }
    }
}
