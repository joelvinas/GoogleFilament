# Implementation Plan - Hello Camera (Revised)

Implement the **Hello Camera** sample, introducing dynamic 3D camera controls, perspective projection, and touch-based gesture interactions using Filament's `Manipulator`.

## User Review Required

> [!IMPORTANT]
> - **Touch Handling**: We will use a dual-detector approach (`GestureDetector` + `ScaleGestureDetector`) to handle orbit and zoom. Single-finger orbit deltas will be gated by `ScaleGestureDetector.isInProgress()` to prevent erratic jumps during pinch gestures.
> - **Manipulator API**: The camera state will be managed by `com.google.android.filament.utils.Manipulator`, which converts touch coordinates into 3D view matrices.
> - **Threading**: Material creation will follow the off-main-thread pattern established in `HelloTriangle`, ensuring `MaterialBuilder` results are applied to the `Engine` safely.

## Proposed Changes

### UI & Navigation

#### [MODIFY] [MainActivity.kt](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/src/main/java/com/example/filamentdemo/MainActivity.kt)
- Replace the placeholder route for `hello_camera` with `HelloCameraScreen()`.

#### [NEW] [HelloCameraScreen.kt](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/src/main/java/com/example/filamentdemo/ui/samples/HelloCameraScreen.kt)
- Create a new Composable that hosts a `SurfaceView`.
- **Touch Routing**:
    - Feed all `MotionEvent` instances to both `GestureDetector` and `ScaleGestureDetector`.
    - In `OnGestureListener.onScroll`, ignore updates if `scaleDetector.isInProgress` is true.
- **Race Condition Guard**: Add explicit null-checks (`manipulator?.let { ... }`) in touch/gesture listener callbacks.
- Manage the `CameraRenderer` lifecycle using `DisposableEffect`.

### Renderer

#### [NEW] [CameraRenderer.kt](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/src/main/java/com/example/filamentdemo/ui/samples/CameraRenderer.kt)
- Create a new renderer class based on `TriangleRenderer`.
- **3D Geometry**: Construct a colored pyramid (5 vertices, 6 triangles) to provide a clear 3D perspective.
- **Material**: Re-use the `UNLIT` shading model but apply color per-vertex (similar to the triangle).
- **Manipulator Integration**:
    - Single-finger drag (orbit): Call `manipulator.grabBegin(x, y, false)`, `manipulator.grabUpdate(x, y)`, and `manipulator.grabEnd()`.
    - Two-finger pinch (zoom): Translate scale deltas into `manipulator.scroll(x, y, delta)`.
    - **Read-side**: In the `render` loop, pull `manipulator.getLookAt(eye, target, up)` and update `camera.lookAt(...)`.
- **Idempotent Cleanup**: Ensure `destroy()` is strictly idempotent with null-checks and logs `[FilamentCamera] Camera Destroyed`.
- **Initialization Logging**: Log `[FilamentCamera] Camera Created` upon successful engine/camera setup.

## Verification Plan

### Manual Verification
1. **Camera Orbit**: Drag one finger; verify the pyramid rotates smoothly.
2. **Camera Zoom**: Pinch with two fingers; verify zooming. Ensure single-finger orbit doesn't "jump" when fingers are lifted from a pinch.
3. **Resize/Rotation**: Rotate the device mid-render; confirm the screen does not crash and the aspect ratio updates seamlessly.
4. **Lifecycle**: Navigate to the sample and back multiple times; verify Logcat for 1-to-1 "Created" and "Destroyed" pairs.
