package com.ritikqrscanner.app.scanner

import android.view.MotionEvent
import android.view.ScaleGestureDetector
import com.journeyapps.barcodescanner.DecoratedBarcodeView

/** Uses the camera thread; unsupported zoom/focus modes are left unchanged. */
fun installCameraGestures(view: DecoratedBarcodeView) {
    val scale = ScaleGestureDetector(view.context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            view.barcodeView.cameraInstance?.changeCameraParameters { parameters ->
                if (parameters.isZoomSupported) parameters.zoom = (parameters.zoom + if (detector.scaleFactor > 1f) 1 else -1).coerceIn(0, parameters.maxZoom)
                parameters
            }
            return true
        }
    })
    view.setOnTouchListener { _, event ->
        scale.onTouchEvent(event)
        if (event.action == MotionEvent.ACTION_UP && !scale.isInProgress) {
            val x = ((event.x / view.width) * 2000 - 1000).toInt().coerceIn(-900, 900)
            val y = ((event.y / view.height) * 2000 - 1000).toInt().coerceIn(-900, 900)
            view.barcodeView.cameraInstance?.changeCameraParameters { parameters ->
                if (parameters.maxNumFocusAreas > 0) parameters.focusAreas = listOf(android.hardware.Camera.Area(android.graphics.Rect(x - 100, y - 100, x + 100, y + 100), 800))
                if (parameters.supportedFocusModes?.contains(android.hardware.Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE) == true) parameters.focusMode = android.hardware.Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
                parameters
            }
            view.performClick()
        }
        true
    }
}
