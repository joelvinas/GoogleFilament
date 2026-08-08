# Implementation Plan - Asynchronous Material Compilation

Refactor `setupMaterialAsync` in `CameraRenderer.kt` to perform heavy material compilation off the main thread using Kotlin Coroutines, while ensuring thread-safe interaction with the Filament `Engine`.

## User Review Required

> [!IMPORTANT]
> - **Threading**: `MaterialBuilder.build()` will be executed on `Dispatchers.IO` to prevent main-thread UI stutters.
> - **JNI Safety**: `Material.Builder().build(engine)` and `RenderableManager` updates will be performed on `Dispatchers.Main` to comply with Filament's thread-adoption rules (Filament engines are typically bound to the thread that created them).
> - **Cancellation**: The compilation coroutine will be scoped to `rendererScope`, which is canceled in `destroy()`. We will also add explicit null-checks for the `engine` and `pyramidEntity` to handle cases where `destroy()` is called mid-compilation.

## Proposed Changes

### Renderer

#### [MODIFY] [CameraRenderer.kt](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/src/main/java/com/example/filamentdemo/ui/samples/CameraRenderer.kt)
- **Imports**: Add `kotlinx.coroutines.withContext` and `kotlinx.coroutines.launch`.
- **`setupMaterialAsync`**:
    - Wrap the `MaterialBuilder` logic in `rendererScope.launch`.
    - Use `withContext(Dispatchers.IO)` for the `builder.build(engine)` call.
    - Use `withContext(Dispatchers.Main)` (or similar main-thread guarantee) for `Material.Builder` and `setMaterialInstanceAt`.
    - Add safety checks to ensure `engine` and `pyramidEntity` are still valid before applying the material.

## Verification Plan

### Automated Tests
- Build and run the app.

### Manual Verification
1. **Performance**: Observe the transition from `SampleListScreen` to `HelloCameraScreen`. The UI should remain responsive (no "frame dropped" logs or visible hitch) while the material compiles.
2. **Race Conditions**: Rapidly enter and exit the `HelloCameraScreen` multiple times. Verify in Logcat that no native crashes occur if `destroy()` is called while a material is compiling.
3. **Rendering**: Ensure the pyramid still renders with its multi-colored vertices once the material compiles.
