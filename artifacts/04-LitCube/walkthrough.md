# Walkthrough - Sample 05: Lit Cube

We have successfully implemented the "Lit Cube" sample, introducing physically-based lighting and tangent-space geometry to the Filament demo project. Additionally, we refactored the gesture logic into a reusable utility.

## Changes Made

### Core Filament Implementation
- **Lit Cube Renderer**: Created [LitCubeRenderer.kt](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/src/main/java/com/example/filamentdemo/ui/samples/LitCubeRenderer.kt) which handles:
    - **Flat-Shaded Geometry**: 24 vertices to ensure sharp edges under lighting.
    - **Tangent Frame**: Utilization of `SurfaceOrientation` to generate quaternions for `VertexAttribute.TANGENTS`.
    - **Physical Lighting**: A 100,000 lux Directional Light (Sun) and 20,000 intensity Indirect Light (Ambient).
    - **Exposure Control**: Explicit camera exposure pairing to prevent blowout.
    - **Async Materials**: Off-thread compilation of `Shading.LIT` materials.
- **Compose UI**: Created [LitCubeScreen.kt](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/src/main/java/com/example/filamentdemo/ui/samples/LitCubeScreen.kt) to host the renderer.

### Architecture & Refactoring
- **Shared Gestures**: Extracted the touch logic from Sample 04 into [OrbitGestureHandler.kt](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/src/main/java/com/example/filamentdemo/ui/utils/OrbitGestureHandler.kt).
- **Hello Camera Update**: Updated [HelloCameraScreen.kt](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/src/main/java/com/example/filamentdemo/ui/samples/HelloCameraScreen.kt) and [CameraRenderer.kt](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/src/main/java/com/example/filamentdemo/ui/samples/CameraRenderer.kt) to use the new shared handler.
- **Navigation**: Integrated the new route in [MainActivity.kt](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/src/main/java/com/example/filamentdemo/MainActivity.kt).

## Technical Highlights

> [!IMPORTANT]
> **Tangent Quaternions**: Because Filament doesn't use a dedicated `NORMAL` attribute, we encoded surface normals into the `TANGENTS` slot as quaternions. This is handled efficiently using the `SurfaceOrientation` utility from the `filament-utils` package.

> [!TIP]
> **Lighting Intensity**: The use of 100,000 lux (Sunlight) requires the camera to have a realistic exposure (e.g., ISO 100, 1/125s, f/16). Without this, the scene would appear completely white.

## Verification Results

### Visuals
- Cube faces are distinctly lit based on orientation.
- Highlights appear sharp due to the 24-vertex duplicated layout.
- Ambient fill ensures shadows aren't pitch black.

### Gestures
- Orbit and zoom work seamlessly across both `Hello Camera` and `Lit Cube` samples.
- Multi-touch transitions are smooth without snapping artifacts.

### Logs
- Logcat verified for 1:1 `Created` and `Destroyed` traces for the engine, material, and geometry components.
