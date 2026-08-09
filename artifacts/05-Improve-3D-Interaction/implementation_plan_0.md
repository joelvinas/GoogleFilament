# Implementation Plan - Redesign OrbitGestureHandler for Seamless Multi-Touch

Redesign `OrbitGestureHandler.kt` to support seamless transitions between 1-finger orbit, 2-finger pan, and 2-finger zoom, mimicking the interaction model of Google Maps or 3D viewers.

## User Review Required

> [!IMPORTANT]
> The `OrbitGestureListener` interface will be updated to include a `strafe` parameter in `onGrabBegin`. This will require updates to `CameraRenderer.kt` and `LitCubeRenderer.kt`.

## Proposed Changes

### [Component] Gesture Handling

#### [MODIFY] [OrbitGestureHandler.kt](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/src/main/java/com/example/filamentdemo/ui/utils/OrbitGestureHandler.kt)
- Update `OrbitGestureListener` interface: `onGrabBegin(x: Float, y: Float, strafe: Boolean)`.
- Implement explicit pointer tracking using `activePointerId1` and `activePointerId2`.
- Handle `ACTION_POINTER_DOWN` to transition from Orbit to Pan/Zoom:
    - Call `listener.onGrabEnd()`.
    - Calculate centroid of the two fingers.
    - Call `listener.onGrabBegin(centroidX, centroidY, strafe = true)`.
- Handle `ACTION_POINTER_UP` to transition from Pan/Zoom back to Orbit:
    - Call `listener.onGrabEnd()`.
    - Identify the remaining finger.
    - Call `listener.onGrabBegin(remainingX, remainingY, strafe = false)`.
- Integrate `ScaleGestureDetector` for zooming during 2-finger interaction.
- Ensure `ACTION_MOVE` updates the correct grab (orbit or pan) based on finger count.

### [Component] Renderer Implementations

#### [MODIFY] [CameraRenderer.kt](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/src/main/java/com/example/filamentdemo/ui/samples/CameraRenderer.kt)
- Update `onGrabBegin` to pass the `strafe` parameter to `manipulator.grabBegin`.

#### [MODIFY] [LitCubeRenderer.kt](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/src/main/java/com/example/filamentdemo/ui/samples/LitCubeRenderer.kt)
- Update `onGrabBegin` to pass the `strafe` parameter to `manipulator.grabBegin`.

## Verification Plan

### Automated Tests
- N/A (UI interaction logic is best verified manually or via unit tests for centroid calculation if needed).

### Manual Verification
- Deploy the app and test the "Camera Viewport" or "Lit Cube" samples.
- Verify 1-finger rotation.
- Verify 2-finger panning and zooming.
- Verify seamless transition from 2 fingers to 1 finger (e.g., pan then release one finger to immediately orbit).
- Verify seamless transition from 1 finger to 2 fingers (e.g., orbit then add second finger to immediately pan).
