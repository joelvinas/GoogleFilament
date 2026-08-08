### Prompt for Stu (Android Studio Gemini)

 **Context & Critical Architectural Adjustments:**
 Stu, we have completed the review for **Sample 03: Lit Cube**. Sonny (Claude) identified a critical Filament API requirement regarding surface normals, as well as a few essential lighting/exposure standards.
 Please generate the complete, production-ready implementation incorporating these **Key Adjustments & Safety Rules**:
 1. **Vertex Attributes & Tangents (CRITICAL):**
 * Filament has **no `Attribute.NORMAL` slot**. Normals must be passed via `VertexAttribute.TANGENTS` encoded as a 4-float unit quaternion (`FLOAT4`).
 * For our 24-vertex flat-shaded cube, provide per-vertex positions (`FLOAT3`), colors (`FLOAT3`), and tangent-frame quaternions (`FLOAT4`) representing each face's normal orientation (or generate them using `SurfaceOrientation.Builder()` from `filament-utils`).
 * Update the `VertexBuffer` layout and `MaterialBuilder` attributes (`Attribute.POSITION`, `Attribute.COLOR`, `Attribute.TANGENTS`) accordingly.
 
 
 2. **Lighting & IndirectLight Setup:**
 * **Directional Light:** Single sun-like directional light at ~100,000 lux added via `LightManager`.
 * **Indirect Light (Ambient Fill):** Create a 1-band Spherical Harmonics `IndirectLight` programmatically without needing a `.ktx` file:
 ```kotlin
 IndirectLight.Builder()
     .irradiance(1, floatArrayOf(r, g, b)) // Flat ambient DC term
     .intensity(20_000f)
     .build(engine)
 
 ```
  
 
 3. **Explicit Camera Exposure:**
 * Explicitly set camera exposure in `LitCubeRenderer` during initialization to pair with 100,000 lux:
 `camera.setExposure(16f, 1f / 125f, 100f)`
 
 
 4. **Off-Thread Material Compilation:**
 * Compile `Shading.LIT` on `Dispatchers.IO` / `Dispatchers.Default` via `MaterialBuilder.build(engine)` and apply on `Dispatchers.Main`. Guard against applying the material if `engine` was destroyed mid-build.
 
 
 5. **Lifecycle, Rotation & Teardown Baseline:**
 * **In-place surface resizing:** Handle `surfaceChanged` updating only `SwapChain`, viewport, and projection (leaving `Engine`, `Camera`, `Manipulator` intact).
 * **Idempotent destruction:** `destroy()` must check `val engine = engine ?: return` with 1:1 `[LitCubeRenderer] <Component Created/Destroyed` Logcat tracing.
 * **Gesture safety:** Retain multi-touch event dispatch order (`ScaleGestureDetector` first, unconditional dispatch), `onScale()` returning `true`, and `isPinchingOrReleasing` guard on `ACTION_POINTER_UP` / `ACTION_UP` calling `onGrabEnd()`.
 
  
 **Deliverable Requested:**
 Provide the complete Kotlin source code files for:
 1. `LitCubeRenderer.kt`
 2. `LitCubeScreen.kt`
 3. `MainActivity.kt` (or navigation updates for route `05-lit-cube`)
 
 
