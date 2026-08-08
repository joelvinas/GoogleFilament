### Prompt for Stu (Android Studio Gemini)

 **Context & Bug Fix Request:**
 Stu, during our code review of the Lit Cube and Camera samples, we identified two critical bugs that need to be resolved immediately: an `EntityManager` memory leak and a logging convention mismatch.
 Please apply the following fixes across `LitCubeRenderer.kt` and `CameraRenderer.kt`:
 1. **Fix `EntityManager` ID Leak in `destroy()` (CRITICAL):**
 In Filament, calling `engine.destroyEntity(entity)` or `engine.destroyCameraComponent(camera.entity)` only destroys engine-side components (like Renderable, Light, or Camera). It **does not** release the entity ID itself back to Filament's global entity pool.
 * In `LitCubeRenderer.kt`'s `destroy()`, add explicit `EntityManager.get().destroy(...)` calls for `cubeEntity`, `lightEntity`, and `camera.entity`.
 * In `CameraRenderer.kt`'s `destroy()`, add explicit `EntityManager.get().destroy(...)` calls for `pyramidEntity` and `camera.entity`.
 
 
 *Example Teardown Pattern:*
 ```kotlin
 // Destroy components in engine
 engine.destroyEntity(cubeEntity)
 engine.destroyEntity(lightEntity)
 engine.destroyCameraComponent(camera.entity)
 
 ```
 
 
 
 

 // Destroy entity IDs in global pool
 val em = EntityManager.get()
 em.destroy(cubeEntity)
 em.destroy(lightEntity)
 em.destroy(camera.entity)
 ```
 
 2. **Align Logcat Tags & Lifecycle Logs:**  
 - In `LitCubeRenderer.kt`: Change the log tag from `LitCubeRenderer` to `FilamentLitCube` and ensure all lifecycle messages use the `[FilamentLitCube]` prefix (e.g., `Log.d("FilamentLitCube", "[FilamentLitCube] Renderer Destroyed")`).
 - In `CameraRenderer.kt`: Update any non-standard debug logs (e.g., the `onScroll` log using `HelloCamera`) to use the standard `FilamentCamera` tag.
 
 **Deliverable Requested:**  
 Updated Kotlin source files for `LitCubeRenderer.kt` and `CameraRenderer.kt` with these fixes applied.
 
 ```
 
 
