# Implementation Plan - Hello Camera

Implement the **Hello Camera** sample, which introduces dynamic 3D camera controls, perspective projection, and touch-based gesture interactions using Filament's `Manipulator`.

## User Review Required

> [!IMPORTANT]
> - This implementation uses the `com.google.android.filament.utils.Manipulator` to handle camera orbit and zoom.
> - A `GestureDetector` and `ScaleGestureDetector` will be used within the `AndroidView` to bridge native Android touch events to the Filament `Manipulator`.

## Proposed Changes

### UI & Navigation

#### [MODIFY] [MainActivity.kt](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/src/main/java/com/example/filamentdemo/MainActivity.kt)
- Replace the placeholder route for `hello_camera` with `HelloCameraScreen()`.

#### [NEW] [HelloCameraScreen.kt](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/src/main/java/com/example/filamentdemo/ui/samples/HelloCameraScreen.kt)
- Create a new Composable that hosts the `SurfaceView`.
- Integrate native gesture detectors to capture touch events.
- Manage the `CameraRenderer` lifecycle using `DisposableEffect`.

### Renderer

#### [NEW] [CameraRenderer.kt](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/src/main/java/com/example/filamentdemo/ui/samples/CameraRenderer.kt)
- Create a new renderer class based on `TriangleRenderer`.
- Add a 3D geometry (a colored pyramid) to distinguish from the 2D-like triangle.
- Integrate `com.google.android.filament.utils.Manipulator`.
- Implement touch handling to update the manipulator.
- Update the camera transform in the `render` loop using `manipulator.getLookAt()`.

## Verification Plan

### Manual Verification
1. **Camera Orbit**: Drag one finger on the screen; verify the pyramid rotates/orbits around the center.
2. **Camera Zoom**: Pinch with two fingers; verify the pyramid gets larger/smaller.
3. **Resize/Rotation**: Rotate the device; verify the aspect ratio updates and geometry is not distorted.
4. **Lifecycle**: Navigate to the sample and back 10 times; check Logcat for `[FilamentCamera] Camera Created` and `[FilamentCamera] Camera Destroyed` to ensure no leaks.
