## Google Filament Android Demo Suite

This project is a progressive set of Android + [Google Filament](https://github.com/google/filament) rendering samples.
It was built through human-guided AI pair-programming with Gemini and Claude — from a bare triangle up through lit, textured, and procedurally-driven scenes.

<img width="230" height="500" alt="Home Page" src="https://github.com/user-attachments/assets/0abe0c01-e7d2-454b-8145-c0528f1567d5" />
<img width="230" height="500" alt="Hello-Triangle" src="https://github.com/user-attachments/assets/7638797f-54b5-4d48-a381-65eca3edcfaf" />


## Why am I doing this?

This project started as a scoped technical spike to validate whether [Filament](https://github.com/google/filament) could be reliably built with heavy AI assistance (Gemini Flash 3 + Claude Sonnet 5) before committing it as the rendering engine for a larger production app. Each sample folder tackles one additional piece of Filament's API surface, in roughly increasing order of difficulty.
I had a good idea of what I wanted to build, but also knew that I didn't have the technical knowhow to get it done, nor the coin to hire someone to do it for me.
I realized there would be some mistakes along the way, and figured a slight detour would prove out the tech, and build "meh" code - while refining my AI-Council process.

## Samples

| Sample | Demonstrates |
|---|---|
| [`sample-hello-triangle`](./sample-hello-triangle) | Minimal Filament scene: a single flat-colored triangle, using **runtime** material compilation (`filamat-android`) instead of the offline `matc` build step |
| [`sample-hello-camera`](./sample-hello-camera) | Interactive orbit camera — pan, rotate, and zoom around a scene |
| [`sample-lit-cube`](./sample-lit-cube) | A cube rendered with Filament's default physically-based (lit) shading model |
| [`sample-material-builder`](./sample-material-builder) | Building and swapping materials programmatically via the `MaterialBuilder` runtime API |
| [`sample-material-instance-stress`](./sample-material-instance-stress) | Many concurrent material instances, to check performance and stability at scale |
| [`sample-procedural-effect`](./sample-procedural-effect) | An animated, shader-driven procedural visual effect |
| [`sample-procedural-texture-quad`](./sample-procedural-texture-quad) | A textured quad using a procedurally generated (non-file-based) texture |
| [`sample-transparent-view`](./sample-transparent-view) | Filament content rendered over a transparent background, composited into the native Android view hierarchy |
| [`sample-gltf-viewer`](./sample-gltf-viewer) | Loading and displaying an external glTF model asset |

## Setup

- **Android Studio:** Quail 2 (2026.1.2) or later — validated against Quail 3 (2026.1.3) pending
- **Min / Target SDK:** 26 / 34
- **Test hardware:** Samsung Galaxy A16 (physical device, connected via USB) — testing was deliberately done on-device rather than an emulator to sidestep ABI/toolchain questions that only matter at release-size optimization time, not during a validation spike.

```bash
git clone https://github.com/joelvinas/GoogleFilament.git
cd GoogleFilament/<sample-folder>
./gradlew assembleDebug
./gradlew installDebug
```

Each sample is a self-contained Gradle module — open the specific `sample-*` folder directly in Android Studio to run just that one.

## Lessons learned

A few non-obvious things that came out of building these, which cost real debugging time the first time around:

- **`MaterialBuilder.build()` has a real threading contract, not just a "keep it off the main thread" guideline.** It must either be called with the shared `Engine` passed in as the job-system provider, or invoked from a thread that isn't also making other Filament API calls. Wrapping the call in a coroutine isn't sufficient on its own if that contract isn't honored.
- **Three-State Lifecycle:** Rendering needs three explicit states (Resume, Pause, Destroy). The `Choreographer.postFrameCallback` needs to be removed on pause and re-added on resume, separately from full resource teardown on destroy to prevent background rendering into inactive surfaces.
- **Explicit Surface Resize:** Device rotation requires explicit updates to the swap chain and the Filament `View` viewport to match new `SurfaceView` dimensions. Without this, frames stretch or freeze silently instead of crashing.
- **Runtime material compilation (`filamat-android`) over the offline `matc` pipeline** was a deliberate choice for this spike: it removes an entire native-toolchain/Gradle-build-step integration from the validation, keeping everything in pure Kotlin — which matters when most of the code is being generated through an AI coding assistant rather than hand-written. The trade-off is a larger APK (the shader compiler ships inside the app); worth revisiting `matc` for a shipping build later.
- **Left the default Gradle ABI configuration untouched** (no explicit `abiFilters`) — restricting it is a release-size optimization, not something that needs solving during a toolchain validation spike.
- **`Manipulator.update(deltaTime)` must be called once per frame, before `getLookAt()`.** Missing this doesn't crash — it silently breaks `scroll()`-driven zoom while grab-driven orbit/pan keep working fine, because only zoom's effect depends on that per-frame integration tick. Easy to miss since the symptom (zoom does nothing) doesn't point at the render loop.
- **Filament's `grabBegin`/`grabUpdate`/`grabEnd` session model is built for one continuous, single-point drag — it does not safely coexist with a concurrent `scroll()` call if held open across multiple frames.** A long-lived two-finger pan session left `scroll()`'s zoom changes vulnerable to being overwritten mid-gesture. Fix was to stop treating pan as a multi-frame session at all: open, update, and close a fresh "micro-session" every single frame, so pan behaves as statelessly as zoom already does.
- **`Engine.destroyEntity()` / `destroyCameraComponent()` only remove Filament's components from an entity — they don't release the entity ID itself.** Without a separate `EntityManager.get().destroy(entity)` call, entity IDs leak from the global pool on every screen navigation.
- **Raw touch input gets noisier as two contact points get closer together, and that noise doesn't average out on its own.** Android's `ScaleGestureDetector` already smooths its own span calculation, but any midpoint/centroid math we compute ourselves inherits sensor noise directly unless we explicitly filter it (EMA, in our case).

### Known Gaps & Technical Debt
- **Offline Material Compilation (`matc`):** Using runtime `filamat-android` compilation is an intentional spike shortcut; a production release should migrate to offline `matc` to minimize binary footprint.
- **ABI Filtering:** Left default Gradle ABI configurations untouched (`abiFilters` omitted). Restricting ABIs remains a future release-size optimization.
- **Hello Triangle Rotation Alignment:** `HelloTriangleScreen` still uses an earlier "tear down on rotation" pattern and needs retrofitting to match the in-place `configChanges` surface resize pattern validated in `HelloCamera`.
- **Mid-Drag Rotation Test Coverage:** Rotating the device mid-gesture is manually verified as non-crashing, but automated coverage was deliberately deferred — either an instrumented `androidTest` simulating the interrupted touch sequence, or extracting the grab-state transitions into a plain, unit-testable state machine. Decision not yet made.
- **iOS rendering path — evaluated and deliberately deferred.** Considered SceneView (a Compose-native wrapper around Filament) as a shortcut for gesture handling. Its current multiplatform version renders iOS via RealityKit rather than Filament — adopting it would trade away the single-engine, cross-platform premise this project is built around, and complicate a scoped, fixed-cost iOS contractor port later. Staying on raw Filament on both platforms; iOS itself is out of scope for this spike and the MVP.

- **Hand-rolled Tangent Utility vs Assimp/Filament Asset Loaders:** Currently building simple unit shapes (triangle, pyramid, cube) by hand-packing vertex buffers and deriving quaternions via SurfaceOrientation. Once we move to complex glTF models (Sample 06+), we will rely on Filament's gltfio library instead of manual VertexBuffer construction.
- **Thread Adoption Standard Locked:** Moving forward across all Filament samples, any background/off-thread MaterialBuilder invocation must use builder.build() (no-arg). Passing engine on a background Dispatcher without explicit Filament thread adoption will panic.

### Progress & Completed Artifacts
- ✅ `01-hello-triangle`: Unlit triangle setup.
- ✅ `02-main-page-navigation`: Compose NavHost routing.
- ✅ `03-hello-camera`: 3D pyramid, camera `Manipulator`, off-thread material compilation. 
        Multi-touch gestures (orbit/pan/zoom) went through several real fix passes — an initial pinch-snap fix, a deeper redesign for seamless orbit↔pan↔zoom coexistence (persistent grab sessions don't safely coexist with concurrent `scroll()` calls), and a centroid-smoothing pass for jitter at close finger separations. Shared gesture-handling code (`OrbitGestureHandler` / `CameraGestureStateMachine`) is now common infrastructure, not per-sample.
- ✅ `04-sample-lit-cube`: PBR-lit cube, tangent generation via SurfaceOrientation (Filament has no raw normal attribute — normals are packed into TANGENTS), off-thread material compile fix, entity-ID leak fix, standardized Logcat lifecycle tags.        

## Development Workflow: The "AI Council" Architecture

This repository was engineered using a human-guided multi-agent workflow balancing specialized LLMs to tackle Filament's native C++/JNI boundaries, Jetpack Compose lifecycles, and graphics constraints. 

Rather than relying on a single agent, the development followed a deliberate **triangular feedback loop**:

*   **Gemini "Gem" Web (Architect / Director):** Formulates high-level meta-prompts, refines architectural constraints, synthesizes feedback, and formats structured git commits. `Gemini Flash 3.6`
*   **Android Studio "Stu" (IDE Execution Agent):** Embedded directly in the IDE to author initial Implementation Plans and generate native Kotlin/Filament code. `Gemini Flash 3 Preview`
*   **Claude "Sonny" Sonnet (Red-Team Auditor):** Acts as an objective reviewer to audit Stu’s implementation plans—flagging edge cases, API hallucinations, and silent lifecycle bugs. `Claude Sonnet 5 Medium`
*   **Claude "CC" Code (Terminal Investigation Agent):** Runs supervised, scoped investigation and verification tasks directly against the repo from the terminal — e.g. tracing edge-case behavior into Filament's native source rather than assuming it from documentation. `Claude Sonnet 5`

**Note**: When working with Agentic systems, a confident diagnosis from any single agent in the loop should always be verified against a controlled on-device test before committing to a fix. During gesture debugging, more than one plausible-sounding root-cause analysis (including a specific claim about zoom sensitivity constants) turned out to be wrong once actually tested on hardware — the loop still converged on the right answer, but only because diagnosis and verification stayed separate steps rather than trusting either alone. "Trust, but verify."


```
              [ "Gem" ]
          (Architect & Orchestrator)
           ↗              ↖
    refines plan       feeds back audit
         ↗                      ↖
     [ "Stu" ]    ──────→   [ "Sonny" ]
 (IDE Executor)   audits   (Red-Team Reviewer)
```
**Note:** Claude Code entered the workflow later, initially for an ad-hoc scoped investigation rather than as a planned step — it's not yet formalized into the cadence above. That'll likely change as its role solidifies.

### The Iteration Cycle

For every new sample and non-trivial refactor, the process followed a strict 9-step cadence:

1. **Prompt Strategy:** Query **Gem** to craft an initial prompt tailored for Android Studio Gemini ("Stu").
2. **Draft Plan:** **Stu** generates a detailed technical Implementation Plan directly in the IDE.
3. **Red-Team Audit:** Feed Stu’s plan to **Sonny** to identify oversights, flawed threading models, or missed surface lifecycles.
4. **Synthesis:** Filter Sonny’s feedback (separating hard requirements from noise) and instruct **Gem** to draft an updated prompt.
5. **Plan Revision:** **Stu** updates the Implementation Plan based on the refined requirements.
6. **Double Verification:** Pass the updated plan through **Gem** and **Sonny** to ensure no new regressions were introduced.
7. **Execution:** Once the plan reaches consensus, authorize **Stu** to execute code generation.
8. **Automated Commits:** Feed the execution walkthrough to **Gem** to generate clean, standard-compliant Git commit messages.
9. **Debugging & Tech Debt:** Loop between all three models to squash runtime exceptions and document known trade-offs.

To see the raw artifacts and technical decisions produced by this process:

- 📋 **[Implementation Plans](./artifacts/plans)** — Pre-generation specs, architecture constraints, and red-team revisions.
- 🛠️ **[Walkthroughs](./artifacts/walkthroughs)** — Step-by-step verification, crash diagnostics (e.g., AABB frustum culling fixes), and hardware validation logs.

## License

MIT
