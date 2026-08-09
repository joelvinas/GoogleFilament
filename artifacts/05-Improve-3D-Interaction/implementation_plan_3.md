# Implementation Plan - Fix Manipulator Updates and Expand Gesture Tests

Address the non-functional pinch-zoom by integrating per-frame `manipulator.update(deltaTime)` calls and improve the robustness of the gesture state machine with better testing and documentation.

## User Review Required

> [!IMPORTANT]
> The change to the renderers introduces time-tracking logic. While standard for graphics loops, it's a structural change to how `render()` is invoked.

## Proposed Changes

### [Component] Renderer Logic Fix (Pinch-Zoom & Timing)

#### [MODIFY] [CameraRenderer.kt](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/src/main/java/com/example/filamentdemo/ui/samples/CameraRenderer.kt)
- Add `private var lastFrameTimeNanos = -1L` field.
- In `render(frameTimeNanos: Long)`:
    - If `lastFrameTimeNanos == -1L`, set `lastFrameTimeNanos = frameTimeNanos` and return early (skip update for first frame).
    - Calculate `deltaTimeSeconds = (frameTimeNanos - lastFrameTimeNanos) / 1_000_000_000f`.
    - Call `manipulator.update(deltaTimeSeconds)` before `getLookAt`.
    - Update `lastFrameTimeNanos = frameTimeNanos`.
- Reset `lastFrameTimeNanos = -1L` in:
    - `stopFrameCallback()`
    - `onSurfaceChanged()`

#### [MODIFY] [LitCubeRenderer.kt](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/src/main/java/com/example/filamentdemo/ui/samples/LitCubeRenderer.kt)
- Add `private var lastFrameTimeNanos = -1L` field.
- In `render(frameTimeNanos: Long)`:
    - If `lastFrameTimeNanos == -1L`, set `lastFrameTimeNanos = frameTimeNanos` and return early.
    - Calculate `deltaTimeSeconds` and call `manipulator.update(deltaTimeSeconds)`.
    - Update `lastFrameTimeNanos = frameTimeNanos`.
- Reset `lastFrameTimeNanos = -1L` in:
    - `stopFrameCallback()`
    - `onSurfaceChanged()`

### [Component] Gesture Logic Refinement

#### [MODIFY] [CameraGestureStateMachine.kt](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/src/main/java/com/example/filamentdemo/ui/utils/CameraGestureStateMachine.kt)
- Add KDoc documentation for class-level and `processEvent` behaviors regarding the 3rd-finger limitation: untracked fingers are not promoted to tracking even if active fingers are lifted, requiring a clean state reset once all touches are cleared.

#### [MODIFY] [CameraGestureStateMachineTest.kt](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/src/test/java/com/example/filamentdemo/ui/utils/CameraGestureStateMachineTest.kt)
- Add `testCompoundGestureSequence`:
    - Simulate: `DOWN` (P1) -> `POINTER_DOWN` (P2) -> `MOVE` (P1, P2) -> `POINTER_UP` (P2) -> `MOVE` (P1) -> `UP`.
    - Verify `grabBegin/End` counts and `strafe` flags are correctly toggled at every transition point.

## Verification Plan

### Automated Tests
- Run `:app:testDebugUnitTest` to verify state machine transitions and the new compound gesture sequence.

### Manual Verification Protocol
*Perform these tests on both **Hello Camera** and **Lit Cube** samples:*

- **Test A (Stationary 2-Finger Zoom)**: With the device flat, place two fingers and move them apart/together. Verify smooth zooming in and out.
- **Test B (Single-Axis Pivot Zoom)**: Hold one finger stationary and move only the second finger. Verify the zoom responds correctly without unexpected panning jumps.
- **Test C (First-Frame Snap Check)**:
    - Enter each sample screen and verify the initial view is stable.
    - Perform a surface rotation (portrait <-> landscape) and verify zero camera "snapping" or position jumping on the first rendered frame after resize.
- **Test D (Continuous Handoff)**: Verify the Orbit -> Pan -> Orbit handoff remains seamless with no stale momentum or position jumping.
