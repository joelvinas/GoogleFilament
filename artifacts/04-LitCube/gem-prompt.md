### Prompt for "Stu" (Android Studio Gemini 3 Flash Preview)

> **Context & Objective:**
> We are building the next sample in our GoogleFilament repository: **Sample 03: Lit Cube**.
> The previous sample (`02-hello-camera`) established off-thread material compilation, 3D geometry with vertex/index buffers, camera manipulation, and smooth gesture handling. In this step, we are adding dynamic lighting and standard material shading to transition from unlit geometry to realistic 3D lighting.
> **Technical Requirements for Sample 03:**
> 1. **Geometry & Attributes:**
> * Construct a 3D Cube (8 vertices or duplicate per-face vertices for flat shading/distinct normals).
> * Include positions, colors, and standard surface normals in the `VertexBuffer`.
> 
> 
> 2. **Material & Shading:**
> * Create/compile a lit material using Filament's standard material builder (`Shading.LIT`).
> * Map surface attributes correctly so Filament's lighting pipeline calculates diffuse and specular highlights.
> 
> 
> 3. **Lighting & Environment:**
> * Add a `LightManager` directional light (acting as a main sun/directional light) to the Filament `Scene`.
> * Configure light parameters: intensity (lux), direction vector, and color.
> * Set up an ambient light/indirect light (`IndirectLight`) or skybox if necessary, or configure base environment lighting.
> 
> 
> 4. **Architecture & Standards (Mandatory):**
> * Keep native allocation/destruction bound strictly to `DisposableEffect`. Log native creation/destruction 1:1 in Logcat.
> * Run `MaterialBuilder` compilation on `Dispatchers.IO` and apply the material on `Dispatchers.Main`.
> * Retain our existing camera `Manipulator` gesture binding (orbit drag + pinch zoom) and handle activity config changes without tearing down the renderer.
> 
> 
> 5. **Navigation & Integration:**
> * Add `03-lit-cube` to the Compose `NavHost` menu structure.
> 
> 
> 
> 
> **Deliverable Requested:**
> Please provide a step-by-step **Implementation Plan** outlining:
> 1. Proposed class structure and updates.
> 2. Vertex buffer layout (positions + normals + colors).
> 3. Material definition and lighting setup logic.
> 4. Integration steps into the existing Compose navigation and camera system.
> 5. Potential edge cases or performance bottlenecks to watch for.
> 