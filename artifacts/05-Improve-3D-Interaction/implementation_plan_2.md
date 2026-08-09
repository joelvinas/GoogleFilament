# Implementation Plan - Fix Manipulator Updates and Expand Gesture Tests

Address the non-functional pinch-zoom by integrating per-frame `manipulator.update(deltaTime)` calls and improve the robustness of the gesture state machine with better testing and documentation.

## User Review Required

> [!IMPORTANT]
> The change to the renderers introduces time-tracking logic. While standard for graphics loops, it's a structural change to how `render()` is invoked.

## Proposed Changes

### [Component] Renderer Logic Fix (Pinch-Zoom)

#### [MODIFY] [CameraRenderer.kt](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/src/main/java/com/example/filamentdemo/ui/samples/CameraRenderer.kt)
- Add `private var lastFrameTimeNanos = 0L` field.
- In `render(frameTimeNanos: Long)`:
    - Calculate `deltaTimeSeconds`.
    - Call `manipulator.update(deltaTimeSeconds)` before `getLookAt`.
    - Reset `lastFrameTimeNanos` when the frame callback is stopped or on surface changes.

#### [MODIFY] [LitCubeRenderer.kt](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/src/main/java/com/example/filamentdemo/ui/samples/LitCubeRenderer.kt)
- Add `private var lastFrameTimeNanos = 0L` field.
- In `render(frameTimeNanos: Long)`:
    - Calculate `deltaTimeSeconds`.
    - Call `manipulator.update(deltaTimeSeconds)` before `getLookAt`.
    - Reset `lastFrameTimeNanos` when the frame callback is stopped.

### [Component] Gesture Logic Refinement

#### [MODIFY] [CameraGestureStateMachine.kt](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/src/main/java/com/example/filamentdemo/ui/utils/CameraGestureStateMachine.kt)
- Add KDoc to the class and `processEvent` documenting the 3rd-finger limitation: untracked fingers are not promoted if the tracked fingers are lifted.

#### [MODIFY] [CameraGestureStateMachineTest.kt](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/src/test/java/com/example/filamentdemo/ui/utils/CameraGestureStateMachineTest.kt)
- Add `testCompoundGestureSequence`:
    - `DOWN` (P1) -> `POINTER_DOWN` (P2) -> `MOVE` (P1, P2) -> `POINTER_UP` (P2) -> `MOVE` (P1).
    - Verify `grabBegin/End` counts and `strafe` flags at each transition.

## Verification Plan

### Automated Tests
- Run `:app:testDebugUnitTest` to verify the new compound gesture test.

### Manual Verification
- Deploy to device.
- Open **Hello Camera**.
- **Pinch-Zoom**: Verify that pinching now zooms the camera (requires the `update()` call).
- **Smooth Handoffs**: Verify the sequence (Orbit -> Pan -> Orbit) remains glitch-free.
