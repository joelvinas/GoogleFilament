# Implementation Plan - Hello Camera (Revised v2)

Implement the **Hello Camera** sample, introducing dynamic 3D camera controls, perspective projection, and touch-based gesture interactions using Filament's `Manipulator`.

## User Review Required

> [!IMPORTANT]
> - **Touch Handling**: A dual-detector approach (`GestureDetector` + `ScaleGestureDetector`) will be used. Single-finger orbit deltas will be gated by `ScaleGestureDetector.isInProgress()`.
> - **Grab-State Cleanup**: We will explicitly call `manipulator.grabEnd()` on `ACTION_UP` and `ACTION_CANCEL` to prevent "stuck" states and position jumps.
> - **In-Place Resize**: `MainActivity` will be configured with `configChanges` to avoid recreation on rotation. `CameraRenderer` will update the `SwapChain` and `Viewport` in-place, preserving `Manipulator` state.
> - **Constraint**: Camera panning (strafing) is explicitly **out of scope**.

## Proposed Changes

### UI & Navigation

#### [MODIFY] [MainActivity.kt](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/src/main/java/com/example/filamentdemo/MainActivity.kt)
- Replace the placeholder route for `hello_camera` with `HelloCameraScreen()`.

#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/src/main/AndroidManifest.xml)
- Ensure `android:configChanges="orientation|screenSize|screenLayout"` is present for `MainActivity`.

#### [NEW] [HelloCameraScreen.kt](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/src/main/java/com/example/filamentdemo/ui/samples/HelloCameraScreen.kt)
- Create a new Composable hosting a `SurfaceView`.
- **Touch Interaction**:
    - Override `onTouchEvent` on the `SurfaceView` (or a wrapper `View`).
    - Dispatch to `GestureDetector` (orbit) and `ScaleGestureDetector` (zoom).
    - **Explicit Cleanup**: Call `manipulator.grabEnd()` on `ACTION_UP` and `ACTION_CANCEL`.
    - **Race Condition Guard**: Null-check `manipulator` before dispatching.

### Renderer

#### [NEW] [CameraRenderer.kt](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/src/main/java/com/example/filamentdemo/ui/samples/CameraRenderer.kt)
- **3D Geometry**: Construct a colored pyramid.
- **In-Place Surface Resize**:
    - In `onSurfaceChanged`, update `viewport`, `camera.setProjection`, and `swapChain`.
    - **State Retention**: Do NOT destroy `Engine`, `Camera`, or `Manipulator` during resize.
    - Add `// TODO` to retrofit `HelloTriangleScreen` with this pattern later.
- **Manipulator Integration**:
    - Orbit: `grabBegin(x, y, false)` (strafing disabled).
    - Zoom: `scroll(x, y, delta)`.
    - Update camera in `render` loop via `manipulator.getLookAt`.
- **Lifecycle & Logging**: Idempotent `destroy()` with `[FilamentCamera] Camera Created/Destroyed` logs.

## Verification Plan

### Manual & Automated Verification
1. **Camera orbit**: Drag single finger; geometry orbits smoothly.
2. **Camera zoom**: Pinch two fingers; camera zooms cleanly without position jumping on lift.
3. **Grab-state release**: After `ACTION_UP`/`ACTION_CANCEL`, verify the next touch begins a fresh orbit without jump artifacts.
4. **Mid-render rotation**: Rotate device while idle; confirm zero crashes, proper viewport aspect ratio update, and retained camera orbit/zoom coordinates.
5. **Mid-drag rotation**: Rotate device during an active drag; verify graceful recovery on the next gesture (implemented via state-safe `grabEnd` handling).
6. **Lifecycle isolation**: Navigate back and forth 10+ times, verifying 1-to-1 matching `[FilamentCamera] Camera Created` / `[FilamentCamera] Camera Destroyed` Logcat lines.
