package com.example.filamentdemo.ui.utils

import kotlin.math.abs

/**
 * Data class representing a pointer (finger) on the screen.
 */
data class Pointer(val id: Int, val x: Float, val y: Float)

enum class GestureAction {
    DOWN,
    POINTER_DOWN,
    MOVE,
    POINTER_UP,
    UP,
    CANCEL
}

/**
 * Pure JVM state machine for handling camera gestures.
 * Handles Orbit (1-finger) and Pan (2-finger) transitions.
 *
 * NOTE: This state machine implements a strict 2-pointer tracking limit.
 * If both tracked pointers are lifted while a 3rd (untracked) finger remains on screen,
 * the 3rd finger is NOT promoted to tracking. A full reset (grabEnd) occurs, and
 * the system waits for all fingers to be lifted before a new gesture can begin.
 */
class CameraGestureStateMachine(private val listener: OrbitGestureListener) {

    private var activePointerId1: Int = INVALID_POINTER_ID
    private var activePointerId2: Int = INVALID_POINTER_ID

    private var lastX1 = 0f
    private var lastY1 = 0f
    private var lastX2 = 0f
    private var lastY2 = 0f

    private var isDragging = false
    private var isStrafing = false

    companion object {
        const val INVALID_POINTER_ID = -1
    }

    /**
     * Processes a gesture event from the UI layer.
     *
     * @param action The type of gesture action (DOWN, MOVE, etc.)
     * @param pointers The current list of all active pointers on screen.
     * @param actionIndex The index of the pointer triggering the ACTION_POINTER_DOWN/UP event.
     */
    fun processEvent(action: GestureAction, pointers: List<Pointer>, actionIndex: Int = 0) {
        when (action) {
            GestureAction.DOWN -> {
                val p = pointers[0]
                activePointerId1 = p.id
                lastX1 = p.x
                lastY1 = p.y
                isDragging = true
                isStrafing = false
                listener.onGrabBegin(p.x, p.y, false)
            }
            GestureAction.POINTER_DOWN -> {
                if (activePointerId2 == INVALID_POINTER_ID && pointers.size >= 2) {
                    // Transition to 2-finger Pan
                    val p2 = pointers[actionIndex]
                    activePointerId2 = p2.id
                    
                    // Update tracked positions for both pointers
                    updateTrackedPositions(pointers)

                    if (isDragging) {
                        listener.onGrabEnd()
                    }

                    val (cx, cy) = getCentroid()
                    isDragging = true
                    isStrafing = true
                    listener.onGrabBegin(cx, cy, true)
                }
            }
            GestureAction.MOVE -> {
                updateTrackedPositions(pointers)
                if (isDragging) {
                    if (isStrafing && activePointerId2 != INVALID_POINTER_ID) {
                        val (cx, cy) = getCentroid()
                        listener.onGrabUpdate(cx, cy)
                    } else if (!isStrafing && activePointerId1 != INVALID_POINTER_ID) {
                        listener.onGrabUpdate(lastX1, lastY1)
                    }
                }
            }
            GestureAction.POINTER_UP -> {
                val liftingId = pointers[actionIndex].id
                if (liftingId == activePointerId1 || liftingId == activePointerId2) {
                    if (isDragging) {
                        listener.onGrabEnd()
                    }

                    // Identify remaining pointer
                    val remainingId = if (liftingId == activePointerId1) activePointerId2 else activePointerId1
                    
                    if (remainingId != INVALID_POINTER_ID) {
                        activePointerId1 = remainingId
                        activePointerId2 = INVALID_POINTER_ID
                        
                        // Update the remaining pointer's position from the current list
                        val p = pointers.find { it.id == remainingId }
                        if (p != null) {
                            lastX1 = p.x
                            lastY1 = p.y
                            isStrafing = false
                            isDragging = true
                            listener.onGrabBegin(lastX1, lastY1, false)
                        } else {
                            reset()
                        }
                    } else {
                        reset()
                    }
                }
            }
            GestureAction.UP, GestureAction.CANCEL -> {
                if (isDragging) {
                    listener.onGrabEnd()
                }
                reset()
            }
        }
    }

    private fun updateTrackedPositions(pointers: List<Pointer>) {
        pointers.forEach { p ->
            if (p.id == activePointerId1) {
                lastX1 = p.x
                lastY1 = p.y
            } else if (p.id == activePointerId2) {
                lastX2 = p.x
                lastY2 = p.y
            }
        }
    }

    private fun getCentroid(): Pair<Float, Float> {
        return Pair((lastX1 + lastX2) / 2f, (lastY1 + lastY2) / 2f)
    }

    private fun reset() {
        activePointerId1 = INVALID_POINTER_ID
        activePointerId2 = INVALID_POINTER_ID
        isDragging = false
        isStrafing = false
    }
    
    // For Testing
    fun getActivePointerId1() = activePointerId1
    fun getActivePointerId2() = activePointerId2
    fun isStrafing() = isStrafing
}
