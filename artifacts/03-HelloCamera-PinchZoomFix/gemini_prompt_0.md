We have identified an active bug in the `HelloCameraScreen` touch handling: when releasing a two-finger pinch-zoom gesture, the camera sharply snaps to vertical extreme angles (looking straight up or straight down).

### Root Cause
When ending a pinch, one finger almost always lifts before the other (pointer count transitions 2 -> 1 -> 0). When the first finger lifts (`ACTION_POINTER_UP`), `ScaleGestureDetector.isInProgress()` becomes `false`. However, `GestureDetector` sees the touch focal point jump instantly from the two-finger midpoint to the remaining finger's location, generating a massive single-frame scroll delta that gets applied directly to `Manipulator.grabUpdate()`.

### Required Fixes in `HelloCameraScreen.kt`

1. **Handle `ACTION_POINTER_UP` in `onTouchEvent()`**:
   - Intercept `MotionEvent.ACTION_POINTER_UP` in `onTouchEvent()`.
   - When a secondary pointer lifts, immediately terminate the current manipulator grab state by invoking `renderer.onGrabEnd()` (or `manipulator.grabEnd()`).

2. **Flag-Gate Orbit Motion After Pinch Release**:
   - Introduce a boolean flag (e.g., `isPinchingOrReleasing`) in `HelloCameraScreen`.
   - Set `isPinchingOrReleasing = true` inside `onScale()` / `onScaleBegin()`.
   - On `onScaleEnd()` or `ACTION_POINTER_UP`, ensure `renderer.onGrabEnd()` is called, but **keep** `isPinchingOrReleasing = true` until all pointers are lifted (`ACTION_UP` / pointer count == 0).
   - In `onScroll()` / orbit drag callback, ignore all orbit deltas whenever `isPinchingOrReleasing` is `true` or `scaleDetector.isInProgress` is `true`.
   - Reset `isPinchingOrReleasing = false` only when `MotionEvent.ACTION_DOWN` occurs (a brand-new single-finger touch).

3. **Re-anchor Orbit On Clean Touches**:
   - Ensure single-finger orbit calls `grabBegin()` ONLY on a fresh `ACTION_DOWN` or explicit single-finger drag start where `isPinchingOrReleasing == false`.

Please provide an **Implementation Plan** confirming how these guards will be integrated, followed by the code diff for `HelloCameraScreen.kt`.