### Prompt for Stu (Android Studio Gemini)

 **Context & Task:**
 Sonny's review identified the root cause of the non-functional pinch-zoom in `LitCubeRenderer.kt` and `CameraRenderer.kt`, along with two pending quality-of-life additions. We need an **Implementation Plan** to fix the frame-update loop and complete the gesture state machine test suite.
 **Issues to Address:**
 1. **Root Cause Fix for Zoom — `Manipulator.update(deltaTime)` (CRITICAL):**
 * **Root Cause:** Filament's `Manipulator.scroll()` updates internal camera momentum state, but that state is only integrated into `getLookAt()` when `manipulator.update(deltaTimeInSeconds)` is called **once per frame** before reading the transform matrix. Currently, `update()` is never invoked in `LitCubeRenderer` or `CameraRenderer`.
 * **Fix Pattern:**
 * In both `LitCubeRenderer.kt` and `CameraRenderer.kt`, track the previous frame timestamp (`lastFrameTimeNanos: Long`).
 * In `render(frameTimeNanos: Long)`, calculate `deltaTimeSeconds = (frameTimeNanos - lastFrameTimeNanos) / 1_000_000_000f` (guarding against the first frame where `lastFrameTimeNanos == 0L`).
 * Call `manipulator.update(deltaTimeSeconds)` **immediately before** `manipulator.getLookAt(...)`.
 * Ensure this becomes part of our standardized baseline rendering pattern for all future samples.
   
 2. **Add Continuous Compound Gesture Test:**
 * In `CameraGestureStateMachineTest.kt`, add an explicit test covering a full compound sequence:
 `Orbit (1 finger) - Pan/Zoom (2nd finger down) - Drift/Pinch - Lift 1 finger - Resume Orbit (remaining finger) - Continue Orbiting`.
 * Assert correct grab states, pointer assignments, and `strafe` flags at every step in the sequence.
  
 3. **Document 3rd-Finger Edge Case Behavior:**
 * In `CameraGestureStateMachine.kt`, add an explicit KDoc/comment documenting the tracked-pointer lifetime boundary:
 *If both tracked pointers (`activePointerId1`, `activePointerId2`) are lifted while an untracked 3rd finger remains on screen, the 3rd finger is NOT promoted into tracking. A clean reset (`grabEnd()`) occurs until all touch points are cleared.*
   
 **Deliverable Requested:**
 Please provide a step-by-step **Implementation Plan** detailing:
 1. Proposed changes to `LitCubeRenderer.kt` and `CameraRenderer.kt` for per-frame `manipulator.update(deltaTime)` calls.
 2. Test structure for the compound gesture scenario in `CameraGestureStateMachineTest.kt`.
 3. Location and text of the 3rd-finger limitation documentation.
 4. Verification plan (manual pinch-zoom verification + JVM unit test execution).