package com.example.filamentdemo.ui.utils

import android.content.Context
import android.view.MotionEvent
import android.view.ScaleGestureDetector

/**
 * Interface for the renderer to receive gesture updates from OrbitGestureHandler.
 */
interface OrbitGestureListener {
    fun onGrabBegin(x: Float, y: Float, strafe: Boolean)
    fun onGrabUpdate(x: Float, y: Float)
    fun onGrabEnd()
    fun onScroll(x: Float, y: Float, delta: Float)
}

private const val ZOOM_SENSITIVITY = 100.0f

/**
 * Encapsulates the multi-touch gesture pipeline for Filament orbit manipulators.
 * Uses CameraGestureStateMachine for explicit pointer tracking and seamless transitions.
 */
class OrbitGestureHandler(
    context: Context,
    private val listener: OrbitGestureListener
) {
    private val stateMachine = CameraGestureStateMachine(listener)

    // Set by onScale() when ScaleGestureDetector actually fires a scale change for the
    // current onTouchEvent() call (it has its own internal threshold before firing).
    // Read-and-reset per event so pan is only suppressed on frames where scroll() ran,
    // not for the whole two-finger touch.
    private var scaleFiredThisEvent = false

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val factor = detector.scaleFactor
            // Pinch open (factor > 1) -> Zoom IN -> negative delta.
            val delta = (1.0f - factor) * ZOOM_SENSITIVITY
            listener.onScroll(detector.focusX, detector.focusY, delta)
            scaleFiredThisEvent = true
            return true
        }
    })

    fun onTouchEvent(event: MotionEvent): Boolean {
        // Unconditional dispatch to ScaleGestureDetector first
        scaleFiredThisEvent = false
        scaleDetector.onTouchEvent(event)

        val action = event.actionMasked
        val actionIndex = event.actionIndex
        val pointers = mutableListOf<Pointer>()
        for (i in 0 until event.pointerCount) {
            pointers.add(
                Pointer(
                    id = event.getPointerId(i),
                    x = event.getX(i),
                    y = event.getY(i)
                )
            )
        }

        val gestureAction = when (action) {
            MotionEvent.ACTION_DOWN -> GestureAction.DOWN
            MotionEvent.ACTION_POINTER_DOWN -> GestureAction.POINTER_DOWN
            MotionEvent.ACTION_MOVE -> GestureAction.MOVE
            MotionEvent.ACTION_POINTER_UP -> GestureAction.POINTER_UP
            MotionEvent.ACTION_UP -> GestureAction.UP
            MotionEvent.ACTION_CANCEL -> GestureAction.CANCEL
            else -> null
        }

        if (gestureAction != null) {
            stateMachine.processEvent(gestureAction, pointers, actionIndex, scaleFiredThisEvent)
        }

        return true
    }
}
