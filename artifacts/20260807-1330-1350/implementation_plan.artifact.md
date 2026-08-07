# Implementation Plan - Filament Hello Triangle (Revised)

Create a basic Android application that demonstrates rendering a colored triangle using the Google Filament engine. This project will use runtime material compilation performed off the main thread to ensure UI responsiveness.

## User Review Required

> [!IMPORTANT]
> **Off-Main-Thread Compilation:** Filament’s `MaterialBuilder` is a heavy operation. We will use Kotlin Coroutines (`Dispatchers.Default`) to compile the material package and then hand it off to the Filament engine on the main/rendering thread.

> [!NOTE]
> **Build Setup:** We will rely on standard Gradle defaults for native library handling (no `abiFilters`) to ensure compatibility with the target device (Galaxy A16).

## Proposed Changes

### Build Configuration

#### [MODIFY] [build.gradle.kts](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/build.gradle.kts)
Ensure the root build script supports the app module and necessary repositories.

#### [NEW] [settings.gradle.kts](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/settings.gradle.kts)
Include the `:app` module.

#### [NEW] [app/build.gradle.kts](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/build.gradle.kts)
Configure the Android app with:
- `com.android.application` and `org.jetbrains.kotlin.android` plugins.
- Filament dependencies: `filament-android`, `filamat-android`, `filament-utils-android`.
- Compose dependencies.

### Application Code

#### [NEW] [AndroidManifest.xml](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/src/main/AndroidManifest.xml)
Standard manifest with `MainActivity` set as the entry point.

#### [NEW] [MainActivity.kt](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/src/main/java/com/example/filamentdemo/MainActivity.kt)
- **Filament Initialization:** Initialize the engine using `Engine.Backend.OPENGL`.
- **Compose UI:** Use `AndroidView` to host a `SurfaceView`.
- **Async Material Builder:** Implement a coroutine-based material generation that compiles the shader package in the background.
- **ECS Components:** Create `VertexBuffer`, `IndexBuffer`, and `Entity` for the triangle.
- **Rendering Loop:** Bind to `Choreographer.postFrameCallback` for frame updates.
- **Lifecycle Cleanup:** Explicitly destroy all Filament resources in `onDispose` to prevent memory leaks and JNI crashes.

## Verification Plan

### Automated Tests
- Build check: `./gradlew assembleDebug`.

### Manual Verification
- **Deployment:** Connect the Galaxy A16 via USB and run `./gradlew installDebug`.
- **UI Check:** Verify the colored triangle appears correctly.
- **Stress Test:** Rotate the device and background/foreground the app multiple times to verify lifecycle stability.
