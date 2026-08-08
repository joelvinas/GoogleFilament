package com.example.filamentdemo.ui.utils

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View

/**
 * Interface for the renderer to receive gesture updates from OrbitGestureHandler.
 */
interface OrbitGestureListener {
    fun onGrabBegin(x: Float, y: Float)
    fun onGrabUpdate(x: Float, y: Float)
    fun onGrabEnd()
    fun onScroll(x: Float, y: Float, delta: Float)
}

private const val ZOOM_SENSITIVITY = 100.0f

/**
 * Encapsulates the multi-touch gesture pipeline for Filament orbit manipulators.
 * Handles scale-first dispatch and prevents "snap" artifacts during transitions.
 */
class OrbitGestureHandler(
    context: Context,
    private val listener: OrbitGestureListener
) {
    private var isPinchingOrReleasing = false

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            isPinchingOrReleasing = true
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            isPinchingOrReleasing = true
            val factor = detector.scaleFactor
            // Zoom - Filament scroll: negative zooms in, positive zooms out.
            // Pinch open (factor > 1) -> Zoom IN -> negative delta.
            val delta = (1.0f - factor) * ZOOM_SENSITIVITY
            listener.onScroll(detector.focusX, detector.focusY, delta)
            return true
        }
    })

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            isPinchingOrReleasing = false
            listener.onGrabBegin(e.x, e.y)
            return true
        }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            if (!scaleDetector.isInProgress && !isPinchingOrReleasing) {
                listener.onGrabUpdate(e2.x, e2.y)
            }
            return true
        }
    })

    fun onTouchEvent(event: MotionEvent): Boolean {
        // Unconditional dispatch: Scale first, then Gesture
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)

        val action = event.actionMasked
        if (action == MotionEvent.ACTION_POINTER_UP) {
            isPinchingOrReleasing = true
            listener.onGrabEnd()
        }

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            listener.onGrabEnd()
        }

        return true
    }
}
