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

    @Test
    fun testCompoundGestureSequence() {
        val p1 = Pointer(1, 10f, 10f)
        val p2 = Pointer(2, 30f, 30f)

        // 1. Orbit Start
        stateMachine.processEvent(GestureAction.DOWN, listOf(p1))
        assertEquals(1, mockListener.grabBeginCount)
        assertFalse(mockListener.lastStrafe)

        // 2. Transition to Pan
        stateMachine.processEvent(GestureAction.POINTER_DOWN, listOf(p1, p2), actionIndex = 1)
        assertEquals(2, mockListener.grabBeginCount)
        assertEquals(1, mockListener.grabEndCount)
        assertTrue(mockListener.lastStrafe)

        // 3. Pan Update (no scale fired this event, so grabUpdate proceeds normally)
        stateMachine.processEvent(
            GestureAction.MOVE,
            listOf(Pointer(1, 15f, 15f), Pointer(2, 35f, 35f)),
            scaleFired = false
        )
        assertEquals(25f, mockListener.lastX) // Centroid of 15,15 and 35,35

        // 4. Resume Orbit (Lift P2)
        stateMachine.processEvent(GestureAction.POINTER_UP, listOf(Pointer(1, 15f, 15f), Pointer(2, 35f, 35f)), actionIndex = 1)
        assertEquals(3, mockListener.grabBeginCount)
        assertEquals(2, mockListener.grabEndCount)
        assertFalse(mockListener.lastStrafe)
        assertEquals(15f, mockListener.lastX) // Resumes at P1's location

        // 5. Final Release
        stateMachine.processEvent(GestureAction.UP, listOf(Pointer(1, 15f, 15f)))
        assertEquals(3, mockListener.grabEndCount)
    }

    @Test
    fun testScaleFiredSuppressesGrabUpdateDuringPan() {
        val p1 = Pointer(1, 10f, 10f)
        val p2 = Pointer(2, 30f, 30f)

        stateMachine.processEvent(GestureAction.DOWN, listOf(p1))
        stateMachine.processEvent(GestureAction.POINTER_DOWN, listOf(p1, p2), actionIndex = 1)

        val updateCountAfterBegin = mockListener.grabUpdateCount

        // Scale fired on this MOVE: onGrabUpdate must be skipped so scroll() is the only
        // thing touching the manipulator this frame.
        stateMachine.processEvent(
            GestureAction.MOVE,
            listOf(Pointer(1, 15f, 15f), Pointer(2, 35f, 35f)),
            scaleFired = true
        )
        assertEquals(updateCountAfterBegin, mockListener.grabUpdateCount)

        // No scale fired on this MOVE: onGrabUpdate resumes normally.
        stateMachine.processEvent(
            GestureAction.MOVE,
            listOf(Pointer(1, 16f, 16f), Pointer(2, 36f, 36f)),
            scaleFired = false
        )
        assertEquals(updateCountAfterBegin + 1, mockListener.grabUpdateCount)
        assertEquals(26f, mockListener.lastX) // Centroid of 16,16 and 36,36
    }
}
