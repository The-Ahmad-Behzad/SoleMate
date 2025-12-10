package com.solemate.app.solemate_app

/**
 * Singleton manager for tracking the currently selected shoe model.
 * This allows dynamic model selection based on user choice from Flutter.
 */
object SelectedShoeManager {
    
    // Default model path (fallback)
    private const val DEFAULT_MODEL = "models/shoes/nike_journey_run_left.glb"
    
    // Currently selected shoe model path
    @Volatile
    private var selectedModelPath: String = DEFAULT_MODEL
    
    /**
     * Set the selected shoe model path.
     * Call this from Flutter via MethodChannel before starting AR session.
     * 
     * @param modelPath The asset path to the GLB model (e.g., "models/shoes/airmax_270_left.glb")
     */
    fun setSelectedModel(modelPath: String?) {
        selectedModelPath = if (modelPath.isNullOrBlank()) {
            DEFAULT_MODEL
        } else {
            // Ensure path doesn't start with "assets/" as Android assets don't need that prefix
            modelPath.removePrefix("assets/")
        }
    }
    
    /**
     * Get the currently selected shoe model path.
     * 
     * @return The asset path to the GLB model
     */
    fun getSelectedModel(): String {
        return selectedModelPath
    }
    
    /**
     * Reset to the default model.
     */
    fun reset() {
        selectedModelPath = DEFAULT_MODEL
    }
    
    /**
     * Check if a specific model is currently selected.
     */
    fun isSelected(modelPath: String): Boolean {
        return selectedModelPath == modelPath || 
               selectedModelPath == modelPath.removePrefix("assets/")
    }
}
