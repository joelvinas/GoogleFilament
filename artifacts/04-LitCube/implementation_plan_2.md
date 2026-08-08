# Implementation Plan - Sample 05: Lit Cube (Updated)

This plan outlines the implementation of a lit 3D cube in Filament, strictly adhering to the project's architecture, safety, and performance standards.

## User Review Required

> [!IMPORTANT]
> **Vertex Attributes & Tangents**: Filament uses `VertexAttribute.TANGENTS` (encoded as quaternions) for surface normals. We will use `SurfaceOrientation.Builder` from `filament-utils` to generate these quaternions for our 24-vertex flat-shaded cube.

> [!NOTE]
> **Ambient Fill**: We will implement a programmatically generated `IndirectLight` using a 1-band Spherical Harmonics DC term to provide base ambient lighting without external assets.

## Architecture & Safety Rules (Mandatory)

1.  **Off-Thread Material Build**: `MaterialBuilder` compilation (`Shading.LIT`) will run on `Dispatchers.IO`/`Default`. Application to the engine will occur on `Dispatchers.Main` with strict null checks.
2.  **In-Place Surface Resizing**: `onSurfaceChanged` will handle `SwapChain` recreation and viewport/projection updates without tearing down the `Engine`.
3.  **Gesture Pipeline**: Precise touch dispatch to `ScaleGestureDetector` and `GestureDetector` with artifact-free transitions (reusing Sample 04 pattern).
4.  **Idempotent Teardown**: `destroy()` will safely release all native resources (entities, buffers, materials, lights, IndirectLight) with 1:1 logging.
5.  **Explicit Exposure**: Camera exposure will be set to `(16f, 1f / 125f, 100f)` to pair with the 100,000 lux directional light.

## Proposed Changes

### 3D Geometry & Attributes
- **Vertex Buffer**: Interleaved (Position FLOAT3, Tangents FLOAT4, Color FLOAT3).
- **Index Buffer**: 36 indices forming 12 triangles.
- **Surface Orientation**: Use `SurfaceOrientation.Builder` to convert normals to tangents.

### Lighting & Material
- **LightManager**: Directional light @ 100,000 lux.
- **Indirect Light**: DC-only SH `IndirectLight` @ 20,000 intensity.
- **Material**: `Shading.LIT` with `baseColor` from vertex attributes.

### New Components

#### [NEW] [LitCubeRenderer.kt](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/src/main/java/com/example/filamentdemo/ui/samples/LitCubeRenderer.kt)
#### [NEW] [LitCubeScreen.kt](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/src/main/java/com/example/filamentdemo/ui/samples/LitCubeScreen.kt)

### Navigation Integration

#### [MODIFY] [MainActivity.kt](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/src/main/java/com/example/filamentdemo/MainActivity.kt)

## Verification Plan

### Automated Tests
- Build verification.
- Logcat audit: Verify `[LitCubeRenderer] ... Created` and `[LitCubeRenderer] ... Destroyed` parity.

### Manual Verification
- **Visuals**: Sharp highlights on cube faces, distinct shadows, and base ambient fill.
- **Gestures**: Orbit and zoom responsiveness.
- **Lifecycle**: Orientation change resilience and navigation cleanup.
