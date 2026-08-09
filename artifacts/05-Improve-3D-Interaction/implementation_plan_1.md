# Implementation Plan - Seamless Multi-Touch Orbit Gesture Handler

Redesign `OrbitGestureHandler.kt` to support seamless, non-modal multi-touch gestures (1-finger orbit, 2-finger pan, 2-finger pinch-zoom) with explicit pointer-ID tracking and zero finger re-touch requirement, following a "Google Maps" style interaction model.

## User Review Required

> [!IMPORTANT]
> The `OrbitGestureListener` interface will be updated to include a `strafe` parameter in `onGrabBegin`. This will require updates to `CameraRenderer.kt` and `LitCubeRenderer.kt`.
> The new implementation will bypass `GestureDetector` for raw movement tracking to ensure precise state control via pointer IDs.

## Proposed Changes

### [Component] Gesture Handling Logic

#### [MODIFY] [OrbitGestureHandler.kt](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/src/main/java/com/example/filamentdemo/ui/utils/OrbitGestureHandler.kt)
- **Interface Update**: Update `OrbitGestureListener` to `onGrabBegin(x: Float, y: Float, strafe: Boolean)`.
- **Pointer Tracking**: Implement `activePointerId1` and `activePointerId2` variables (initially `INVALID_POINTER_ID`).
- **State Machine**:
    - `ACTION_DOWN`: Set `activePointerId1`, call `onGrabBegin(strafe = false)`.
    - `ACTION_POINTER_DOWN`:
        - If `activePointerId2` is invalid, set it, call `onGrabEnd()`, then `onGrabBegin(centroid, strafe = true)`.
        - If already tracking two fingers, ignore subsequent fingers.
    - `ACTION_MOVE`:
        - 1 finger: `onGrabUpdate(x1, y1)`.
        - 2 fingers: `onGrabUpdate(centroidX, centroidY)`. `ScaleGestureDetector` handles zoom.
    - `ACTION_POINTER_UP`:
        - If one of the tracked pointers is lifted, call `onGrabEnd()`.
        - Assign the remaining pointer to `activePointerId1`, reset `activePointerId2`.
        - Call `onGrabBegin(remainingX, remainingY, strafe = false)` immediately.
    - `ACTION_UP` / `ACTION_CANCEL`: Reset all state and call `onGrabEnd()`.
- **Centroid Logic**: Helper method to calculate the midpoint between two active pointers.

### [Component] Renderer Integration

#### [MODIFY] [CameraRenderer.kt](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/src/main/java/com/example/filamentdemo/ui/samples/CameraRenderer.kt)
- Update `onGrabBegin` to pass `strafe` to `manipulator.grabBegin`.

#### [MODIFY] [LitCubeRenderer.kt](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/src/main/java/com/example/filamentdemo/ui/samples/LitCubeRenderer.kt)
- Update `onGrabBegin` to pass `strafe` to `manipulator.grabBegin`.

## Verification Plan

### Automated Tests
- Create a scratch unit test for the `OrbitGestureHandler` state logic (mocking `MotionEvent`) to verify pointer ID transitions and centroid calculations.

### Manual Verification
- **1-Finger Orbit**: Dragging one finger rotates the camera.
- **2-Finger Pan**: Adding a second finger during orbit stops orbit and starts panning.
- **2-Finger Zoom**: Pinching during pan zooms in/out.
- **Seamless Hand-off**: Releasing one finger during a pan immediately resumes orbiting from the remaining finger's location without jump or re-touch.
- **3rd Finger Guard**: Touching with a 3rd finger does not disrupt the 2-finger pan/zoom.
