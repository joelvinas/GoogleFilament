# Implementation Plan - Fix App Crash (Empty AABB)

The app is crashing on startup with an `IllegalArgumentException` from Filament's `RenderableManager`. The error indicates that the renderable (the triangle) is missing a bounding box (AABB), which is required because it is a shadow receiver by default.

## Proposed Changes

### [Component Name] :app

#### [MODIFY] [MainActivity.kt](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/src/main/java/com/example/filamentdemo/MainActivity.kt)

- **Define Bounding Box**: In `TriangleRenderer.setupGeometry`, update `RenderableManager.Builder` to include a bounding box centered at (0, 0, 0) with half-extents (1, 1, 1).
- **Disable Shadows**: Explicitly set `.receiveShadows(false)` and `.castShadows(false)` on the `RenderableManager.Builder` to optimize performance and satisfy the culling requirement.
- **Refine Lifecycle Management**:
    - Update `FilamentTriangleScreen` to handle `TriangleRenderer` cleanup strictly within `onDispose` of the `DisposableEffect`.
    - Ensure `TriangleRenderer.destroy()` is idempotent (prevents multiple crashes/errors if called more than once).
    - Remove redundant `onDestroy` call in the lifecycle observer if it conflicts with `onDispose` timing.

## Verification Plan

### Manual Verification
- Deploy the app to the device and ensure it no longer crashes on startup.
- Verify that the triangle is rendered correctly.
