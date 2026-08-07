## AI-Assisted Architecture & Engineering Process

A progressive set of Android + [Google Filament](https://github.com/google/filament) rendering samples.
It was built through human-guided AI pair-programming with Gemini and Claude — from a bare triangle up through lit, textured, and procedurally-driven scenes.

## Why am I doing this?

This project started as a scoped technical spike to validate whether [Filament](https://github.com/google/filament) could be reliably built with heavy AI assistance (Gemini Flash 3 + Claude Sonnet 5) before committing it as the rendering engine for a larger production app. Each sample folder tackles one additional piece of Filament's API surface, in roughly increasing order of difficulty.

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
- **Lifecycle handling needs three states, not two.** It's not just "start rendering / stop rendering and clean up" — the frame callback (`Choreographer.postFrameCallback`) needs to be removed on pause and re-added on resume, separately from full resource teardown on destroy. Skipping the pause/resume step means the app keeps trying to render into a backgrounded surface.
- **Device rotation needs an explicit resize handler.** The swap chain and the Filament `View`'s viewport need to be updated to match the `SurfaceView`'s new dimensions on rotation — otherwise you get a stretched or frozen frame rather than a crash, which is easy to mistake for "it works."
- **Runtime material compilation (`filamat-android`) over the offline `matc` pipeline** was a deliberate choice for this spike: it removes an entire native-toolchain/Gradle-build-step integration from the validation, keeping everything in pure Kotlin — which matters when most of the code is being generated through an AI coding assistant rather than hand-written. The trade-off is a larger APK (the shader compiler ships inside the app); worth revisiting `matc` for a shipping build later.
- **Left the default Gradle ABI configuration untouched** (no explicit `abiFilters`) — restricting it is a release-size optimization, not something that needs solving during a toolchain validation spike.

## How this was built

This project demonstrates human-guided AI engineering using Gemini and Claude to work through native C++/JNI boundaries, Jetpack Compose lifecycle bindings, and Filament's graphics engine constraints.

To see the raw iterative technical decisions, refer to the archived logs, timestamped by when the work happened:

- 📋 **[Implementation Plans](./artifacts/plans)** — Pre-generation technical specs, constraints, and architecture rules.
- 🛠️ **[Walkthroughs](./artifacts/walkthroughs)** — Step-by-step verification, crash diagnostics (e.g. AABB frustum culling fixes), and hardware validation logs.

## License

MIT