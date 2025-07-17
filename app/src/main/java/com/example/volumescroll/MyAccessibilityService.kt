package com.example.volumescroll

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.PixelFormat
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView

class MyAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "VolumeScrollService"
        private var instance: MyAccessibilityService? = null
        private var isServiceEnabled = false
        private var scrollDistance = 600 // Default scroll distance in pixels

        fun setServiceEnabled(enabled: Boolean) {
            isServiceEnabled = enabled
            instance?.updateOverlayVisibility()
        }

        fun setScrollDistance(distance: Int) {
            scrollDistance = distance
        }

        fun isServiceActive(): Boolean {
            return instance != null && isServiceEnabled
        }
    }

    private var overlayView: View? = null
    private var windowManager: WindowManager? = null
    private var lastVolumeKeyTime = 0L
    private val volumeKeyDebounceMs = 100L // Prevent rapid key presses

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "Accessibility Service Connected")

        setupOverlay()
        Toast.makeText(this, "Volume Scroll Service Connected", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        removeOverlay()
        Log.d(TAG, "Accessibility Service Destroyed")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // We don't need to handle accessibility events for this use case
        // Volume key handling is done in onKeyEvent
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility Service Interrupted")
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (!isServiceEnabled) return false

        event?.let { keyEvent ->
            // Only handle key down events to avoid double triggers
            if (keyEvent.action == KeyEvent.ACTION_DOWN) {
                val currentTime = System.currentTimeMillis()

                // Debounce rapid key presses
                if (currentTime - lastVolumeKeyTime < volumeKeyDebounceMs) {
                    return true
                }

                when (keyEvent.keyCode) {
                    KeyEvent.KEYCODE_VOLUME_UP -> {
                        lastVolumeKeyTime = currentTime
                        performScrollUp()
                        return true // Consume the event
                    }
                    KeyEvent.KEYCODE_VOLUME_DOWN -> {
                        lastVolumeKeyTime = currentTime
                        performScrollDown()
                        return true // Consume the event
                    }
                }
            }
        }

        return false // Don't consume other key events
    }

    private fun performScrollUp() {
        Log.d(TAG, "Performing scroll up")
        performScroll(true)
    }

    private fun performScrollDown() {
        Log.d(TAG, "Performing scroll down")
        performScroll(false)
    }

    private fun performScroll(scrollUp: Boolean) {
        try {
            val displayMetrics = resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val screenHeight = displayMetrics.heightPixels

            // Create gesture path for vertical scroll
            val path = Path()

            // Start from center of screen
            val startX = screenWidth / 2f
            val startY = screenHeight / 2f

            // Calculate end position based on scroll direction
            val endY = if (scrollUp) {
                startY + scrollDistance // Scroll up shows previous content
            } else {
                startY - scrollDistance // Scroll down shows next content
            }

            // Ensure end position is within screen bounds
            val clampedEndY = endY.coerceIn(0f, screenHeight.toFloat())

            path.moveTo(startX, startY)
            path.lineTo(startX, clampedEndY)

            // Create gesture description
            val gestureBuilder = GestureDescription.Builder()
            val strokeDescription = GestureDescription.StrokeDescription(
                path,
                0, // Start time
                200 // Duration in milliseconds
            )

            gestureBuilder.addStroke(strokeDescription)
            val gesture = gestureBuilder.build()

            // Perform the gesture
            val result = dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    super.onCompleted(gestureDescription)
                    Log.d(TAG, "Scroll gesture completed")
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    super.onCancelled(gestureDescription)
                    Log.w(TAG, "Scroll gesture cancelled")
                }
            }, null)

            if (!result) {
                Log.e(TAG, "Failed to dispatch scroll gesture")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error performing scroll", e)
        }
    }

    private fun setupOverlay() {
        try {
            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

            val inflater = LayoutInflater.from(this)
            overlayView = inflater.inflate(R.layout.overlay_indicator, null)

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT
            )

            params.gravity = Gravity.TOP or Gravity.END
            params.x = 20
            params.y = 100

            updateOverlayVisibility()

        } catch (e: Exception) {
            Log.e(TAG, "Error setting up overlay", e)
        }
    }

    private fun updateOverlayVisibility() {
        try {
            overlayView?.let { view ->
                if (isServiceEnabled) {
                    if (view.parent == null) {
                        windowManager?.addView(view, view.layoutParams)
                    }
                    view.findViewById<TextView>(R.id.overlayText)?.text = "Volume Scroll: ON"
                } else {
                    if (view.parent != null) {
                        windowManager?.removeView(view)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating overlay visibility", e)
        }
    }

    private fun removeOverlay() {
        try {
            overlayView?.let { view ->
                if (view.parent != null) {
                    windowManager?.removeView(view)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing overlay", e)
        }
    }
}