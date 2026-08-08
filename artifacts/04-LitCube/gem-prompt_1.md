### Prompt for Stu (Android Studio Gemini)

> **Context & Revisions Required:**
> Stu, excellent baseline plan for **Sample 03: Lit Cube**. Sonny (Claude) completed the technical architecture review and approved the geometry (24 vertices for flat-face normals) and lighting concept. However, we need to explicitly incorporate our established project standards to prevent regressions.
> Please update your plan and generate the code incorporating these **8 Architecture & Safety Rules**:
> 1. **Off-Thread Material Build:** Compile the `Shading.LIT` material asynchronously off the main thread (`Dispatchers.IO` / `Dispatchers.Default`) via `MaterialBuilder.build(engine)`, and apply the compiled material on `Dispatchers.Main`. Ensure state checks guard against applying the material if the engine is destroyed mid-compile.
> 2. **Rotation & In-Place Surface Resizing:** Implement the exact `04-hello-camera` pattern. The host Activity handles `configChanges="orientation|screenSize|screenLayout"`. Handle `SurfaceHolder.Callback.surfaceChanged` in-place by resizing the viewport/projection and `SwapChain`, keeping `Engine`, `Camera`, and `Manipulator` intact.
> 3. **Gesture Safety & Dispatch:** Ensure `LitCubeScreen` reuses our exact gesture pipeline:
> * Unconditional dispatch to `ScaleGestureDetector` first, then `GestureDetector`.
> * `onScale()` returns `true`.
> * Multi-touch transitions guarded via `isPinchingOrReleasing` and `ACTION_POINTER_UP` / `ACTION_UP` calling `onGrabEnd()` to prevent snap/jump artifacts.
> 
> 
> 4. **Idempotent Resource Teardown:** Ensure `LitCubeRenderer.destroy()` is strictly idempotent (`val engine = engine ?: return`), releasing native Filament buffers, materials, lights, entities, and engine cleanly with 1:1 creation/destruction logging (`Log.d("LitCubeRenderer", "[LitCubeRenderer] <Component> Created/Destroyed")`).
> 5. **DisposableEffect Teardown Scope:** Bind the `LitCubeRenderer` native creation and teardown to a Compose `DisposableEffect` in `LitCubeScreen` so navigation-away triggers clean teardown.
> 6. **Reused Utilities:** Confirm `Utils.init()` is relied upon from our established companion/init path from `04-hello-camera` without redundant re-initialization.
> 7. **Package Alignment:** Verify all imports and paths use the actual project package namespace (`com.google.android.filament.demo` or your project's active root package).
> 8. **Lighting & Exposure Pairing:** Configure the directional light intensity (~100,000 lux) paired with proper camera exposure (`Camera.setExposure(...)`) or EV compensation so the lit cube renders with proper highlight definition without blowing out or turning black.
> 
> 
> **Deliverable Requested:**
> 1. A brief confirmation of the updated plan incorporating these 8 safety rules.
> 2. The complete, production-ready source code for:
> * `LitCubeRenderer.kt`
> * `LitCubeScreen.kt`
> * NavHost route updates in `MainActivity.kt` (or navigation structure).
> 
> 
> 
> 
