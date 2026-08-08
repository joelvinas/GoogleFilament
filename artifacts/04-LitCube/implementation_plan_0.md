# Implementation Plan - Sample 03: Lit Cube

This plan outlines the steps to implement a lit 3D cube in Filament, transitioning from unlit geometry to standard PBR shading with dynamic lighting.

## User Review Required

> [!IMPORTANT]
> **Flat Shading vs. Smooth Shading**: To achieve distinct highlights on cube faces, we will use **24 vertices** (4 per face) instead of 8. This allows each face to have its own surface normal, preventing lighting from "bleeding" across edges.

> [!NOTE]
> **Indirect Lighting**: We will implement a basic `IndirectLight` (ambient light) to ensure the dark sides of the cube aren't pitch black. This will be a simple constant ambient factor or a basic spherical harmonics setup.

## Proposed Changes

### 3D Geometry & Attributes
- **Vertex Buffer Layout**: Interleaved buffer containing:
    - `Position` (FLOAT3)
    - `Normal` (FLOAT3) - Crucial for lighting calculations.
    - `Color` (FLOAT3) - Per-vertex base color.
- **Index Buffer**: 36 indices (12 triangles) to form the 6 faces of the cube.

### Lighting Setup
- **Directional Light**: A "Sun" light using Filament's `LightManager`.
    - Intensity: ~100,000 lux (Filament uses physical units).
    - Direction: Vector pointing slightly down and across the scene.
- **Indirect Light**: Basic environmental lighting to provide ambient occlusion/fill.

### Material Definition
- **Shading Model**: `Shading.LIT`.
- **Material Logic**: Map the `Normal` attribute to Filament's lighting pipeline. We will use a simple shader that passes vertex colors to the baseColor input of the lit material.

### Class Structure & Integration

#### [NEW] [LitCubeRenderer.kt](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/src/main/java/com/example/filamentdemo/ui/samples/LitCubeRenderer.kt)
- Encapsulates Filament engine state, cube geometry, lighting, and material compilation.
- Handles `Manipulator` updates for orbit/zoom gestures.
- Manages native resource destruction in `destroy()`.

#### [NEW] [LitCubeScreen.kt](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/src/main/java/com/example/filamentdemo/ui/samples/LitCubeScreen.kt)
- Compose entry point for the sample.
- Binds the `LitCubeRenderer` to a `SurfaceView`.
- Implements gesture detection (Orbit + Scale).

#### [MODIFY] [MainActivity.kt](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/src/main/java/com/example/filamentdemo/MainActivity.kt)
- Replace the `lit_cube` route placeholder with the new `LitCubeScreen`.

## Verification Plan

### Automated Tests
- **Build & Run**: Ensure the project compiles after adding the new files.
- **Memory Leak Check**: Monitor Logcat for "Created" vs "Destroyed" logs to verify 1:1 parity during navigation changes.

### Manual Verification
- **Visual Inspection**:
    - Verify the cube has distinct faces (flat shading).
    - Verify one side is brightly lit while others are in shadow.
    - Verify highlights move as the camera orbits.
- **Gestures**:
    - Test single-finger drag for orbiting.
    - Test pinch-to-zoom.
- **Lifecycle**:
    - Rotate the screen and verify the renderer recovers correctly without crashing.
    - Navigate back to the list and return to the sample to verify destruction/re-creation.
