In `CameraRenderer.kt`, `setupMaterialAsync` is named "Async" but actually runs synchronously on the main thread during `CameraRenderer` instantiation, causing a minor main-thread compilation stall.

Please refactor `setupMaterialAsync` in `CameraRenderer.kt`:
1. Use Kotlin Coroutines (`rendererScope.launch(Dispatchers.IO)`) to build the `MaterialBuilder` byte payload off the main thread.
2. Once built, switch back to `Dispatchers.Main` (or post to the main thread) to invoke `Material.Builder()` and attach the material instance to `pyramidEntity` safely in compliance with Filament JNI main-thread rules.
3. Ensure that if `destroy()` is called while material compilation is in progress, the coroutine cancels cleanly and does not attach to a destroyed `Engine` handle.