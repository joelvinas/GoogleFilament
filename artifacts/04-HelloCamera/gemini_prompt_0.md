We are implementing the second Filament sample: **Hello Camera**. This sample builds directly on top of the Hello Triangle codebase but introduces dynamic 3D camera controls, perspective projection, and touch-based gesture interactions.

### Requirements

1. **Feature Overview**:
   - Render a 3D geometry (e.g., a colored 3D triangle, pyramid, or cube) positioned in world space at `(0, 0, 0)`.
   - Implement dynamic camera positioning using Filament's `Camera` component.
   - Support touch-based orbit (drag to rotate) and zoom (pinch-to-zoom) camera controls over the surface view.

2. **Camera & Matrix Mechanics**:
   - Configure a perspective projection using `camera.setProjection(fov, aspect, near, far)`.
   - Update `camera.lookAt(eye, target, up)` dynamically on touch events to orbit around the center target `(0, 0, 0)`.
   - Use Filament's official `Manipulator` utility (`com.google.android.filament.utils.Manipulator`) or custom spherical coordinate math (azimuth, elevation, radius) to convert 2D drag/pinch gestures into 3D camera coordinates.

3. **Jetpack Compose Navigation & Surface Handling**:
   - Create `HelloCameraScreen.kt` and integrate it into the Compose `NavHost` route generated in the previous feature.
   - Use a `PointerInput` gesture modifier on the `AndroidView`/`SurfaceView` (or a native gesture detector) to capture drag and pinch inputs smoothly without blocking the main thread.
   - Handle surface resize / rotation events by updating `camera.setProjection()` with the new aspect ratio (`width / height`).

4. **Strict Lifecycle & Memory Management**:
   - Bind all Filament objects (`Engine`, `Camera`, `View`, `Scene`, `Renderer`, `SwapChain`) strictly within a `DisposableEffect` scoped to `HelloCameraScreen`.
   - Ensure `destroy()` cleans up the camera, entities, and manipulator resources cleanly.
   - Add Logcat logs (`[FilamentCamera] Camera Created` and `[FilamentCamera] Camera Destroyed`) to verify 1-to-1 teardown pairs on back-navigation.

5. **Verification Plan**:
   - Verify camera orbits around the object on single-finger drag.
   - Verify camera zooms in/out on two-finger pinch.
   - Verify aspect ratio updates seamlessly on device rotation without distorting geometry.
   - Verify navigating back to `SampleListScreen` 10+ times does not leak native camera/engine handles.

Please provide a detailed **Implementation Plan** first before generating any code changes. Save the plan to `artifacts/04-hello-camera/plan.md`.