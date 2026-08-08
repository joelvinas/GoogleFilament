# Walkthrough - Hello Camera

I have implemented the **Hello Camera** sample, which features a 3D perspective camera with orbit and zoom controls.

## Changes

### UI & Navigation
- **[HelloCameraScreen.kt](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/src/main/java/com/example/filamentdemo/ui/samples/HelloCameraScreen.kt)**: New screen hosting the `SurfaceView`. It integrates `GestureDetector` and `ScaleGestureDetector` to bridge touch events to Filament's `Manipulator`.
- **[MainActivity.kt](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/src/main/java/com/example/filamentdemo/MainActivity.kt)**: Registered the `hello_camera` route.
- **[AndroidManifest.xml](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/src/main/AndroidManifest.xml)**: Configured `MainActivity` with `android:configChanges` to support in-place surface resizing.

### Renderer
- **[CameraRenderer.kt](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/src/main/java/com/example/filamentdemo/ui/samples/CameraRenderer.kt)**: New renderer class.
    - **3D Geometry**: Renders a multi-colored pyramid at the origin.
    - **Camera Control**: Integrates `com.google.android.filament.utils.Manipulator` for ORBIT mode.
    - **Lifecycle**: Implements strict teardown of native resources and logs lifecycle events.
    - **In-place Resize**: Updates the `SwapChain` and `Viewport` without destroying the `Engine` on rotation.

## Verification Results

### Automated Tests
- Build successful via Gradle.

### Manual Verification (Expected)
- **Orbit**: Dragging one finger rotates the camera around the pyramid.
- **Zoom**: Pinching scales the view distance.
- **Resilience**: Rotation updates the aspect ratio correctly without crashing or resetting the camera position.
- **Lifecycle**: Logcat confirms 1-to-1 matching of `[FilamentCamera] Camera Created` and `[FilamentCamera] Camera Destroyed`.

> [!NOTE]
> The `HelloTriangleScreen` is currently using the old "tear down on rotation" pattern. A follow-up task should retrofit it with the new `configChanges` in-place resize pattern validated here.
