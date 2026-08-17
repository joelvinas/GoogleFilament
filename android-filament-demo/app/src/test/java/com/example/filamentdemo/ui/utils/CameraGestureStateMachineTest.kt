package com.example.filamentdemo.ui.utils

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Two-finger pan no longer holds a persistent grabBegin/grabEnd session across the whole
 * gesture -- every two-finger MOVE opens and closes its own micro-session
 * (grabBegin -> grabUpdate -> grabEnd) scoped to that frame's centroid delta. These tests
 * assert that per-frame triplet behavior directly, rather than counting one begin/end pair
 * per gesture-level transition as the old persistent-session tests did.
 */
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
    fun testTwoFingerPointerDownEndsOrbitButDoesNotBeginPan() {
        val p1 = Pointer(1, 10f, 10f)
        stateMachine.processEvent(GestureAction.DOWN, listOf(p1))

        val p2 = Pointer(2, 30f, 30f)
        stateMachine.processEvent(GestureAction.POINTER_DOWN, listOf(p1, p2), actionIndex = 1)

        // Orbit session ends, but no pan session begins yet -- there's no prior centroid
        // to anchor a micro-session against until the first real MOVE arrives.
        assertEquals(1, mockListener.grabEndCount)
        assertEquals(1, mockListener.grabBeginCount)
        assertTrue(stateMachine.isStrafing())
        assertEquals(2, stateMachine.getActivePointerId2())
    }

    @Test
    fun testFirstMoveAfterPointerDownDoesNotFireGrabTriplet() {
        val p1 = Pointer(1, 10f, 10f)
        stateMachine.processEvent(GestureAction.DOWN, listOf(p1))

        val p2 = Pointer(2, 30f, 30f)
        stateMachine.processEvent(GestureAction.POINTER_DOWN, listOf(p1, p2), actionIndex = 1)

        val beginCountBeforeMove = mockListener.grabBeginCount
        val updateCountBeforeMove = mockListener.grabUpdateCount
        val endCountBeforeMove = mockListener.grabEndCount

        // First MOVE after the second finger lands only records the initial centroid --
        // there's nothing to delta from yet, so no grabBegin/grabUpdate/grabEnd triplet fires.
        stateMachine.processEvent(
            GestureAction.MOVE,
            listOf(Pointer(1, 15f, 15f), Pointer(2, 35f, 35f))
        )

        assertEquals(beginCountBeforeMove, mockListener.grabBeginCount)
        assertEquals(updateCountBeforeMove, mockListener.grabUpdateCount)
        assertEquals(endCountBeforeMove, mockListener.grabEndCount)
    }

    @Test
    fun testSecondMoveFiresPerFrameGrabTriplet() {
        val p1 = Pointer(1, 10f, 10f)
        stateMachine.processEvent(GestureAction.DOWN, listOf(p1))

        val p2 = Pointer(2, 30f, 30f)
        stateMachine.processEvent(GestureAction.POINTER_DOWN, listOf(p1, p2), actionIndex = 1)

        // First MOVE: establishes centroid (25, 25), no triplet.
        stateMachine.processEvent(
            GestureAction.MOVE,
            listOf(Pointer(1, 15f, 15f), Pointer(2, 35f, 35f))
        )

        // Second MOVE: raw centroid moves to (27, 27), but grabUpdate receives the
        // EMA-smoothed value (seeded at 25,25), not the raw centroid directly.
        stateMachine.processEvent(
            GestureAction.MOVE,
            listOf(Pointer(1, 17f, 17f), Pointer(2, 37f, 37f))
        )

        val expectedSmoothed = 25f + CameraGestureStateMachine.CENTROID_SMOOTHING_ALPHA * (27f - 25f)

        assertEquals(2, mockListener.grabBeginCount) // 1 orbit begin + 1 pan micro-session begin
        assertEquals(1, mockListener.grabUpdateCount)
        assertEquals(2, mockListener.grabEndCount) // 1 orbit end (at POINTER_DOWN) + 1 pan micro-session end
        assertTrue(mockListener.lastStrafe)
        assertEquals(expectedSmoothed, mockListener.lastX, 0.0001f) // smoothed, not raw 27f
    }

    @Test
    fun testThirdMoveFiresAnotherIndependentTriplet() {
        val p1 = Pointer(1, 10f, 10f)
        stateMachine.processEvent(GestureAction.DOWN, listOf(p1))

        val p2 = Pointer(2, 30f, 30f)
        stateMachine.processEvent(GestureAction.POINTER_DOWN, listOf(p1, p2), actionIndex = 1)

        stateMachine.processEvent(
            GestureAction.MOVE,
            listOf(Pointer(1, 15f, 15f), Pointer(2, 35f, 35f))
        ) // centroid -> (25, 25), no triplet

        stateMachine.processEvent(
            GestureAction.MOVE,
            listOf(Pointer(1, 17f, 17f), Pointer(2, 37f, 37f))
        ) // centroid -> (27, 27), triplet #1

        val beginCountAfterFirstTriplet = mockListener.grabBeginCount
        val updateCountAfterFirstTriplet = mockListener.grabUpdateCount
        val endCountAfterFirstTriplet = mockListener.grabEndCount

        val alpha = CameraGestureStateMachine.CENTROID_SMOOTHING_ALPHA
        val smoothedAfterFirstTriplet = 25f + alpha * (27f - 25f)

        stateMachine.processEvent(
            GestureAction.MOVE,
            listOf(Pointer(1, 19f, 19f), Pointer(2, 39f, 39f))
        ) // raw centroid -> (29, 29), triplet #2, independent of triplet #1

        val expectedSmoothed = smoothedAfterFirstTriplet + alpha * (29f - smoothedAfterFirstTriplet)

        assertEquals(beginCountAfterFirstTriplet + 1, mockListener.grabBeginCount)
        assertEquals(updateCountAfterFirstTriplet + 1, mockListener.grabUpdateCount)
        assertEquals(endCountAfterFirstTriplet + 1, mockListener.grabEndCount)
        assertEquals(expectedSmoothed, mockListener.lastX, 0.0001f) // smoothed, not raw 29f
    }

    @Test
    fun testTwoFingerToOneFingerHandoffWithNoMovesInBetween() {
        val p1 = Pointer(1, 10f, 10f)
        stateMachine.processEvent(GestureAction.DOWN, listOf(p1))

        val p2 = Pointer(2, 30f, 30f)
        stateMachine.processEvent(GestureAction.POINTER_DOWN, listOf(p1, p2), actionIndex = 1)

        // Lift finger 2 with no MOVE in between -- there was never a pan grab session open
        // to close, since it's scoped per-MOVE now.
        stateMachine.processEvent(GestureAction.POINTER_UP, listOf(p1, p2), actionIndex = 1)

        assertEquals(1, mockListener.grabEndCount) // Only the orbit-session end from POINTER_DOWN
        assertEquals(2, mockListener.grabBeginCount) // Start orbit, resume orbit
        assertFalse(mockListener.lastStrafe)
        assertEquals(10f, mockListener.lastX)
    }

    @Test
    fun testTwoFingerToOneFingerHandoffAfterPanMoves() {
        val p1 = Pointer(1, 10f, 10f)
        stateMachine.processEvent(GestureAction.DOWN, listOf(p1))

        val p2 = Pointer(2, 30f, 30f)
        stateMachine.processEvent(GestureAction.POINTER_DOWN, listOf(p1, p2), actionIndex = 1)

        stateMachine.processEvent(
            GestureAction.MOVE,
            listOf(Pointer(1, 15f, 15f), Pointer(2, 35f, 35f))
        ) // no triplet, establishes centroid

        stateMachine.processEvent(
            GestureAction.MOVE,
            listOf(Pointer(1, 17f, 17f), Pointer(2, 37f, 37f))
        ) // triplet fires

        val endCountBeforeHandoff = mockListener.grabEndCount
        val beginCountBeforeHandoff = mockListener.grabBeginCount

        stateMachine.processEvent(
            GestureAction.POINTER_UP,
            listOf(Pointer(1, 17f, 17f), Pointer(2, 37f, 37f)),
            actionIndex = 1
        )

        // No pan session left open to close -- the last MOVE's micro-session already
        // closed itself. Only a fresh orbit grabBegin fires, resuming at pointer 1.
        assertEquals(endCountBeforeHandoff, mockListener.grabEndCount)
        assertEquals(beginCountBeforeHandoff + 1, mockListener.grabBeginCount)
        assertFalse(mockListener.lastStrafe)
        assertEquals(17f, mockListener.lastX)
    }

    @Test
    fun testThirdFingerGuard() {
        val p1 = Pointer(1, 10f, 10f)
        stateMachine.processEvent(GestureAction.DOWN, listOf(p1))

        val p2 = Pointer(2, 30f, 30f)
        stateMachine.processEvent(GestureAction.POINTER_DOWN, listOf(p1, p2), actionIndex = 1)

        val p3 = Pointer(3, 50f, 50f)
        stateMachine.processEvent(GestureAction.POINTER_DOWN, listOf(p1, p2, p3), actionIndex = 2)

        // Should NOT call grabBegin/End again -- the 3rd pointer is ignored entirely.
        assertEquals(1, mockListener.grabBeginCount)
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

        // 2. Transition to Pan: ends orbit, no pan begin yet (no prior centroid)
        stateMachine.processEvent(GestureAction.POINTER_DOWN, listOf(p1, p2), actionIndex = 1)
        assertEquals(1, mockListener.grabBeginCount)
        assertEquals(1, mockListener.grabEndCount)
        assertTrue(stateMachine.isStrafing())

        // 3. First pan MOVE: establishes centroid (25, 25), no triplet fires
        stateMachine.processEvent(
            GestureAction.MOVE,
            listOf(Pointer(1, 15f, 15f), Pointer(2, 35f, 35f))
        )
        assertEquals(1, mockListener.grabBeginCount)
        assertEquals(0, mockListener.grabUpdateCount)
        assertEquals(1, mockListener.grabEndCount)

        // 4. Second pan MOVE: centroid moves to (27, 27), a full micro-session fires
        stateMachine.processEvent(
            GestureAction.MOVE,
            listOf(Pointer(1, 17f, 17f), Pointer(2, 37f, 37f))
        )
        assertEquals(2, mockListener.grabBeginCount)
        assertEquals(1, mockListener.grabUpdateCount)
        assertEquals(2, mockListener.grabEndCount)
        val expectedSmoothed = 25f + CameraGestureStateMachine.CENTROID_SMOOTHING_ALPHA * (27f - 25f)
        assertEquals(expectedSmoothed, mockListener.lastX, 0.0001f) // smoothed, not raw 27f

        // 5. Resume Orbit (Lift P2): no pan session left to close, just a fresh orbit begin
        stateMachine.processEvent(
            GestureAction.POINTER_UP,
            listOf(Pointer(1, 17f, 17f), Pointer(2, 37f, 37f)),
            actionIndex = 1
        )
        assertEquals(3, mockListener.grabBeginCount)
        assertEquals(2, mockListener.grabEndCount)
        assertFalse(mockListener.lastStrafe)
        assertEquals(17f, mockListener.lastX) // Resumes at P1's location

        // 6. Final Release
        stateMachine.processEvent(GestureAction.UP, listOf(Pointer(1, 17f, 17f)))
        assertEquals(3, mockListener.grabEndCount)
    }

    @Test
    fun testCentroidSmoothingConvergesGraduallyTowardStepChange() {
        val alpha = CameraGestureStateMachine.CENTROID_SMOOTHING_ALPHA

        stateMachine.processEvent(GestureAction.DOWN, listOf(Pointer(1, 0f, 0f)))
        stateMachine.processEvent(
            GestureAction.POINTER_DOWN,
            listOf(Pointer(1, 0f, 0f), Pointer(2, 0f, 0f)),
            actionIndex = 1
        )
        // Seeds the smoothed centroid at (0, 0) -- no triplet fires yet.
        stateMachine.processEvent(
            GestureAction.MOVE,
            listOf(Pointer(1, 0f, 0f), Pointer(2, 0f, 0f))
        )

        // Step change: both raw pointers jump straight to (200, 200) in one frame.
        stateMachine.processEvent(
            GestureAction.MOVE,
            listOf(Pointer(1, 200f, 200f), Pointer(2, 200f, 200f))
        )
        val afterFirstStep = mockListener.lastX
        val expectedAfterFirstStep = 0f + alpha * (200f - 0f)
        assertEquals(expectedAfterFirstStep, afterFirstStep, 0.0001f)
        // Must move gradually toward the step, not jump straight to the new raw value.
        assertTrue(afterFirstStep > 0f)
        assertTrue(afterFirstStep < 200f)

        // Raw input holds steady at (200, 200); the smoothed value should keep approaching
        // it without ever overshooting or snapping instantly.
        stateMachine.processEvent(
            GestureAction.MOVE,
            listOf(Pointer(1, 200f, 200f), Pointer(2, 200f, 200f))
        )
        val afterSecondStep = mockListener.lastX
        val expectedAfterSecondStep = afterFirstStep + alpha * (200f - afterFirstStep)
        assertEquals(expectedAfterSecondStep, afterSecondStep, 0.0001f)
        assertTrue(afterSecondStep > afterFirstStep)
        assertTrue(afterSecondStep < 200f)
    }

    @Test
    fun testCentroidSmoothingResetsBetweenGestures() {
        val alpha = CameraGestureStateMachine.CENTROID_SMOOTHING_ALPHA

        // First two-finger gesture: build up smoothing lag by seeding at (0, 0) then
        // stepping to (200, 200), so the running smoothed value trails behind the raw input.
        stateMachine.processEvent(GestureAction.DOWN, listOf(Pointer(1, 0f, 0f)))
        stateMachine.processEvent(
            GestureAction.POINTER_DOWN,
            listOf(Pointer(1, 0f, 0f), Pointer(2, 0f, 0f)),
            actionIndex = 1
        )
        stateMachine.processEvent(
            GestureAction.MOVE,
            listOf(Pointer(1, 0f, 0f), Pointer(2, 0f, 0f))
        )
        stateMachine.processEvent(
            GestureAction.MOVE,
            listOf(Pointer(1, 200f, 200f), Pointer(2, 200f, 200f))
        )
        assertTrue(mockListener.lastX < 200f) // confirms lag is present before ending the gesture

        // End the gesture entirely (lift both fingers).
        stateMachine.processEvent(
            GestureAction.POINTER_UP,
            listOf(Pointer(1, 200f, 200f), Pointer(2, 200f, 200f)),
            actionIndex = 1
        )
        stateMachine.processEvent(GestureAction.UP, listOf(Pointer(1, 200f, 200f)))

        // Start a brand new two-finger gesture at a completely different location with new
        // pointer IDs. If smoothing state leaked across gestures, the first real triplet
        // below would be pulled toward the old gesture's trailing (200,200)-ish value
        // instead of starting fresh from this gesture's own seed.
        stateMachine.processEvent(GestureAction.DOWN, listOf(Pointer(3, 500f, 500f)))
        stateMachine.processEvent(
            GestureAction.POINTER_DOWN,
            listOf(Pointer(3, 500f, 500f), Pointer(4, 500f, 500f)),
            actionIndex = 1
        )
        stateMachine.processEvent(
            GestureAction.MOVE,
            listOf(Pointer(3, 500f, 500f), Pointer(4, 500f, 500f))
        ) // seeds fresh at (500, 500), no triplet

        stateMachine.processEvent(
            GestureAction.MOVE,
            listOf(Pointer(3, 600f, 600f), Pointer(4, 600f, 600f))
        )
        val expectedSmoothed = 500f + alpha * (600f - 500f)
        assertEquals(expectedSmoothed, mockListener.lastX, 0.0001f)
    }
}