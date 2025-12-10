package com.solemate.app.solemate_app

import android.util.Log

/**
 * Singleton manager for tracking the currently selected shoe model.
 * Supports both single shoe and left/right pair loading for multi-shoe AR rendering.
 */
object SelectedShoeManager {
    
    private const val TAG = "SelectedShoeManager"
    
    // Default model path (fallback)
    private const val DEFAULT_MODEL = "models/shoes/nike_journey_run_left.glb"
    
    // Currently selected shoe model path (base path)
    @Volatile
    private var selectedModelPath: String = DEFAULT_MODEL
    
    // Separate paths for left and right shoes (for mirrored pairs)
    @Volatile
    private var leftModelPath: String? = null
    
    @Volatile
    private var rightModelPath: String? = null
    
    /**
     * Set the selected shoe model path.
     * Call this from Flutter via MethodChannel before starting AR session.
     * This sets the base model which will be used for both left and right shoes.
     * 
     * @param modelPath The asset path to the GLB model (e.g., "models/shoes/airmax_270_left.glb")
     */
    fun setSelectedModel(modelPath: String?) {
        val cleanPath = if (modelPath.isNullOrBlank()) {
            DEFAULT_MODEL
        } else {
            // Ensure path doesn't start with "assets/" as Android assets don't need that prefix
            modelPath.removePrefix("assets/")
        }
        
        selectedModelPath = cleanPath
        // By default, use the same model for both feet (right will be mirrored)
        leftModelPath = cleanPath
        rightModelPath = cleanPath
        
        Log.d(TAG, "Selected shoe model: $selectedModelPath")
    }
    
    /**
     * Set specific models for left and right shoes.
     * Use this if you have separate model files for each foot.
     * 
     * @param leftPath The asset path to the left shoe GLB
     * @param rightPath The asset path to the right shoe GLB (can be same as left for mirroring)
     */
    fun setLeftRightModels(leftPath: String?, rightPath: String?) {
        leftModelPath = leftPath?.removePrefix("assets/")?.ifBlank { null }
        rightModelPath = rightPath?.removePrefix("assets/")?.ifBlank { null }
        
        // Update base path to left if available
        if (leftModelPath != null) {
            selectedModelPath = leftModelPath!!
        }
        
        Log.d(TAG, "Set left/right models - Left: $leftModelPath, Right: $rightModelPath")
    }
    
    /**
     * Get the currently selected shoe model path (base/primary model).
     * 
     * @return The asset path to the GLB model
     */
    fun getSelectedModel(): String {
        return selectedModelPath
    }
    
    /**
     * Get the model path for a specific foot.
     * 
     * @param isLeft true for left shoe, false for right shoe
     * @return The asset path to the GLB model for that foot
     */
    fun getModelForFoot(isLeft: Boolean): String {
        return if (isLeft) {
            leftModelPath ?: selectedModelPath
        } else {
            rightModelPath ?: selectedModelPath
        }
    }
    
    /**
     * Get left shoe model path.
     */
    fun getLeftModel(): String {
        return leftModelPath ?: selectedModelPath
    }
    
    /**
     * Get right shoe model path.
     */
    fun getRightModel(): String {
        return rightModelPath ?: selectedModelPath
    }
    
    /**
     * Check if we should load both shoes (pair mode).
     * Returns true if both left and right paths are set.
     */
    fun isPairMode(): Boolean {
        return leftModelPath != null && rightModelPath != null
    }
    
    /**
     * Reset to the default model.
     */
    fun reset() {
        selectedModelPath = DEFAULT_MODEL
        leftModelPath = null
        rightModelPath = null
    }
    
    /**
     * Check if a specific model is currently selected.
     */
    fun isSelected(modelPath: String): Boolean {
        val cleanPath = modelPath.removePrefix("assets/")
        return selectedModelPath == cleanPath || 
               leftModelPath == cleanPath ||
               rightModelPath == cleanPath
    }
}
