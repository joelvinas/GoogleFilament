# Task List - Filament Hello Triangle

- [x] Configure Project Structure
    - [x] Create `settings.gradle.kts`
    - [x] Update root `build.gradle.kts`
    - [x] Create `app/build.gradle.kts`
- [x] Setup Android Application
    - [x] Create `app/src/main/AndroidManifest.xml`
- [x] Implement Core Rendering
    - [x] Implement `MainActivity.kt` with:
        - [x] Async `MaterialBuilder` on `Dispatchers.Default`
        - [x] `DefaultLifecycleObserver` for frame loop and cleanup
        - [x] `SurfaceHolder.Callback` for viewport and `SwapChain` management
        - [x] Filament ECS setup (Engine, Scene, View, Camera, Renderer)
        - [x] Triangle geometry (VertexBuffer, IndexBuffer)
- [x] Verification
    - [x] Run `./gradlew assembleDebug`
    - [ ] Run `./gradlew installDebug` (Manual step for user)
