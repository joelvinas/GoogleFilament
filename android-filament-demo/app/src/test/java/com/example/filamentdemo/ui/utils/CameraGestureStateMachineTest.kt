package com.example.filamentdemo.ui.utils

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CameraGestureStateMachineTest {

    private lateinit var stateMachine: CameraGestureStateMachine
    private lateinit var mockListener: MockOrbitGestureListener

    class MockOrbitGestureListener : OrbitGestureListener {
        var grabBeginCount = 0
        var grabUpdateCount = 0
        var grabEndCount = 0
        var lastStrafe = false
        var lastX = 0f
        var lastY = 0f

        override fun onGrabBegin(x: Float, y: Float, strafe: Boolean) {
            grabBeginCount++
            lastX = x
            lastY = y
            lastStrafe = strafe
        }

        override fun onGrabUpdate(x: Float, y: Float) {
            grabUpdateCount++
            lastX = x
            lastY = y
        }

        override fun onGrabEnd() {
            grabEndCount++
        }

        override fun onScroll(x: Float, y: Float, delta: Float) {}
    }

    @Before
    fun setup() {
        mockListener = MockOrbitGestureListener()
        stateMachine = CameraGestureStateMachine(mockListener)
    }

    @Test
    fun testOneFingerOrbit() {
        val p1 = Pointer(1, 10f, 10f)
        stateMachine.processEvent(GestureAction.DOWN, listOf(p1))
        
        assertEquals(1, mockListener.grabBeginCount)
        assertFalse(mockListener.lastStrafe)
        assertEquals(10f, mockListener.lastX)
        
        stateMachine.processEvent(GestureAction.MOVE, listOf(Pointer(1, 20f, 20f)))
        assertEquals(1, mockListener.grabUpdateCount)
        assertEquals(20f, mockListener.lastX)
    }

    @Test
    fun testTwoFingerPanTransition() {
        val p1 = Pointer(1, 10f, 10f)
        stateMachine.processEvent(GestureAction.DOWN, listOf(p1))
        
        val p2 = Pointer(2, 30f, 30f)
        stateMachine.processEvent(GestureAction.POINTER_DOWN, listOf(p1, p2), actionIndex = 1)
        
        // Should end orbit and begin strafe
        assertEquals(1, mockListener.grabEndCount)
        assertEquals(2, mockListener.grabBeginCount)
        assertTrue(mockListener.lastStrafe)
        assertEquals(20f, mockListener.lastX) // Centroid of (10,10) and (30,30)
    }

    @Test
    fun testTwoFingerToMainHandoff() {
        val p1 = Pointer(1, 10f, 10f)
        stateMachine.processEvent(GestureAction.DOWN, listOf(p1))
        
        val p2 = Pointer(2, 30f, 30f)
        stateMachine.processEvent(GestureAction.POINTER_DOWN, listOf(p1, p2), actionIndex = 1)
        
        // Lift finger 2
        stateMachine.processEvent(GestureAction.POINTER_UP, listOf(p1, p2), actionIndex = 1)
        
        assertEquals(2, mockListener.grabEndCount) // End orbit, then end strafe
        assertEquals(3, mockListener.grabBeginCount) // Start orbit, start strafe, resume orbit
        assertFalse(mockListener.lastStrafe)
        assertEquals(10f, mockListener.lastX)
    }

    @Test
    fun testThirdFingerGuard() {
        val p1 = Pointer(1, 10f, 10f)
        stateMachine.processEvent(GestureAction.DOWN, listOf(p1))
        
        val p2 = Pointer(2, 30f, 30f)
        stateMachine.processEvent(GestureAction.POINTER_DOWN, listOf(p1, p2), actionIndex = 1)
        
        val p3 = Pointer(3, 50f, 50f)
        stateMachine.processEvent(GestureAction.POINTER_DOWN, listOf(p1, p2, p3), actionIndex = 2)
        
        // Should NOT call grabBegin/End again
        assertEquals(2, mockListener.grabBeginCount)
        assertEquals(1, mockListener.grabEndCount)
    }
}
