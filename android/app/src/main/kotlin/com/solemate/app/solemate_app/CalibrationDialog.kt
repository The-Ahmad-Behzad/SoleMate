package com.solemate.app.solemate_app

import android.content.Context
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog

/**
 * Utility class for scale calibration with SharedPreferences persistence.
 * Provides methods to save/load phone height or shoe size calibration.
 */
object CalibrationManager {
    private const val PREFS_NAME = "solemate_calibration"
    private const val KEY_PHONE_HEIGHT = "phone_height_meters"
    private const val KEY_SHOE_SIZE = "shoe_size_cm"
    private const val KEY_CALIBRATION_METHOD = "calibration_method"  // "height" or "shoe"
    
    /**
     * Get saved phone height in meters.
     */
    fun getPhoneHeight(context: Context): Float? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val height = prefs.getFloat(KEY_PHONE_HEIGHT, -1f)
        return if (height > 0) height else null
    }
    
    /**
     * Get saved shoe size in cm.
     */
    fun getShoeSize(context: Context): Float? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val size = prefs.getFloat(KEY_SHOE_SIZE, -1f)
        return if (size > 0) size else null
    }
    
    /**
     * Get calibration method used.
     */
    fun getCalibrationMethod(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CALIBRATION_METHOD, null)
    }
    
    /**
     * Check if calibration exists.
     */
    fun hasCalibration(context: Context): Boolean {
        return getPhoneHeight(context) != null || getShoeSize(context) != null
    }
    
    /**
     * Save phone height calibration.
     */
    fun savePhoneHeight(context: Context, heightMeters: Float) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putFloat(KEY_PHONE_HEIGHT, heightMeters)
            .putString(KEY_CALIBRATION_METHOD, "height")
            .apply()
    }
    
    /**
     * Save shoe size calibration.
     */
    fun saveShoeSize(context: Context, sizeCm: Float) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putFloat(KEY_SHOE_SIZE, sizeCm)
            .putString(KEY_CALIBRATION_METHOD, "shoe")
            .apply()
    }
    
    /**
     * Clear calibration.
     */
    fun clearCalibration(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
    
    /**
     * Show calibration dialog.
     */
    fun showCalibrationDialog(context: Context, onSaved: ((String, Float) -> Unit)? = null) {
        val inflater = LayoutInflater.from(context)
        val customView = createCalibrationView(context, inflater)
        
        AlertDialog.Builder(context)
            .setTitle("AR Calibration")
            .setView(customView)
            .setPositiveButton("Save") { _, _ ->
                saveCalibrationFromView(context, customView, onSaved)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun createCalibrationView(context: Context, inflater: LayoutInflater): View {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 32)
        }

        // Radio group for method selection
        val methodGroup = RadioGroup(context).apply {
            orientation = RadioGroup.VERTICAL
        }

        val heightRadio = RadioButton(context).apply {
            text = "Phone Height (meters)"
            id = View.generateViewId()
        }
        val shoeRadio = RadioButton(context).apply {
            text = "Shoe Size (cm)"
            id = View.generateViewId()
        }

        methodGroup.addView(heightRadio)
        methodGroup.addView(shoeRadio)
        heightRadio.isChecked = true  // Default to height

        // Input field for phone height
        val heightInput = EditText(context).apply {
            hint = "Enter height in meters (e.g., 1.5)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or 
                       android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 16, 0, 0)
            }
        }

        // Input field for shoe size
        val shoeInput = EditText(context).apply {
            hint = "Enter shoe size in cm (e.g., 26)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or 
                       android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 16, 0, 0)
            }
            visibility = View.GONE
        }

        // Show/hide inputs based on selection
        heightRadio.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                heightInput.visibility = View.VISIBLE
                shoeInput.visibility = View.GONE
            }
        }
        shoeRadio.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                heightInput.visibility = View.GONE
                shoeInput.visibility = View.VISIBLE
            }
        }

        // Load existing values
        val savedHeight = getPhoneHeight(context)
        val savedShoe = getShoeSize(context)
        savedHeight?.let { heightInput.setText(it.toString()) }
        savedShoe?.let { shoeInput.setText(it.toString()) }

        container.addView(methodGroup)
        container.addView(heightInput)
        container.addView(shoeInput)

        // Store references for saveCalibration
        container.tag = CalibrationData(heightRadio, shoeRadio, heightInput, shoeInput)

        return container
    }
    
    private fun saveCalibrationFromView(
        context: Context,
        view: View,
        onSaved: ((String, Float) -> Unit)?
    ) {
        val data = view.tag as? CalibrationData ?: return

        try {
            if (data.heightRadio.isChecked) {
                val heightText = data.heightInput.text.toString()
                if (heightText.isNotEmpty()) {
                    val height = heightText.toFloat().coerceIn(0.5f, 3.0f)
                    savePhoneHeight(context, height)
                    onSaved?.invoke("height", height)
                    android.widget.Toast.makeText(
                        context,
                        "Phone height saved: ${height}m",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            } else if (data.shoeRadio.isChecked) {
                val shoeText = data.shoeInput.text.toString()
                if (shoeText.isNotEmpty()) {
                    val size = shoeText.toFloat().coerceIn(15f, 35f)
                    saveShoeSize(context, size)
                    onSaved?.invoke("shoe", size)
                    android.widget.Toast.makeText(
                        context,
                        "Shoe size saved: ${size}cm",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } catch (e: NumberFormatException) {
            android.widget.Toast.makeText(
                context,
                "Invalid input. Please enter a valid number.",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    private data class CalibrationData(
        val heightRadio: RadioButton,
        val shoeRadio: RadioButton,
        val heightInput: EditText,
        val shoeInput: EditText
    )
}

