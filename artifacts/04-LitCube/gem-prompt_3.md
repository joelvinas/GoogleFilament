### Prompt for Stu (Android Studio Gemini)

 **Status:** Plan APPROVED! Proceed directly to code generation for **Sample 05: Lit Cube**.
 **Two Micro-Adjustments for Implementation:**
 1. **Color Attribute Layout (`FLOAT4`):** Use `FLOAT4` (RGBA) for vertex color data in the `VertexBuffer` layout and `MaterialBuilder` bindings (e.g., `Attribute.COLOR` as `Type.FLOAT4` with $1.0\text{f}$ alpha per vertex) to match Filament's expected material color inputs and avoid uninitialized memory reads.
 2. **Shared Gesture Extraction:** Rather than duplicating the touch handling in `LitCubeScreen`, refactor/extract the touch dispatch and state guard from `HelloCameraScreen` into a shared composable/helper (e.g., `OrbitGestureHandler.kt` or `rememberOrbitGestureDetector`). Both `HelloCameraScreen` and `LitCubeScreen` must use this shared handler.
 
 
 **Required Source Files to Deliver:**
 1. **`LitCubeRenderer.kt`**: Engine setup, 24-vertex cube with `SurfaceOrientation` tangents, 100,000 lux directional light, 1-band SH `IndirectLight` (20,000 intensity), explicit exposure setting `(16f, 1f/125f, 100f)`, off-thread `Shading.LIT` material compilation, idempotent `destroy()`, and 1:1 Logcat logs.
 2. **`OrbitGestureHandler.kt`** (or shared gesture helper): Encapsulating scale-first dispatch, `isPinchingOrReleasing` flags, and `onGrabEnd()` calls.
 3. **`LitCubeScreen.kt`**: Compose entry point binding `LitCubeRenderer` via `DisposableEffect` and using the shared gesture handler.
 4. **`MainActivity.kt`**: NavHost updates routing `05-lit-cube` to `LitCubeScreen`.
 
 
 Please output the full code for these files.
