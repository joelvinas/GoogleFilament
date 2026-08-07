# Implementation Plan - Main Page (Landing Menu) with Robust Lifecycle & Edge-Case Management

This plan outlines the steps to create an interactive directory for Filament render samples using Jetpack Compose Navigation, with a critical focus on robust native resource lifecycle management and configuration change stability.

## User Review Required

> [!IMPORTANT]
> **Lifecycle Management**: Filament resources (`Engine`, `SwapChain`, etc.) are bound strictly to the `DisposableEffect` of each sample screen. This ensures teardown occurs correctly during navigation, independent of the Activity lifecycle.
>
> **Idempotency**: The `TriangleRenderer.destroy()` method will be designed to be idempotent, safely handling multiple calls (e.g., from rapid navigation or simultaneous Activity destruction).
>
> **File Architecture**: `TriangleRenderer` will be moved to its own file (`TriangleRenderer.kt`) to improve modularity and maintainability as we add more samples.

## Proposed Changes

### Dependencies
- Add `androidx.navigation:navigation-compose` to `app/build.gradle.kts`.

### Models
- **`SampleItem.kt`**: Data class for sample metadata.
  - `enum class Complexity { Beginner, Intermediate, Advanced }`
  - `data class SampleItem(val id: String, val title: String, val description: String, val complexity: Complexity)`

### UI Components
- **`SampleListScreen.kt`**: A scrollable `LazyColumn` of Material 3 `Card` items.
- **`ComingSoonScreen.kt`**: A stateless placeholder screen. It relies on the `NavHost` backstack for navigation and requires no Filament-specific teardown.
- **`HelloTriangleScreen.kt`**: The Composable screen hosting the Filament view. It will manage the `TriangleRenderer` lifecycle via `DisposableEffect`.
- **`TriangleRenderer.kt`**: Extracted renderer logic with added logging and idempotency guards.

### Navigation
- Define a `NavHost` in `MainActivity.kt`.
- Start destination: `SampleListScreen`.
- Routes for all 9 samples (1 implementation, 8 placeholders).

### Configuration Change & Surface Management
- **`HelloTriangleScreen`**: The `AndroidView` factory will create the `SurfaceView`. The `SurfaceHolder.Callback` will be attached within the `AndroidView` lifecycle.
- **Viewport Retention**: When the device rotates, `onSurfaceChanged` in the `TriangleRenderer` will handle viewport updates and `SwapChain` recreation. Since the `renderer` instance is `remember`ed in the Composable, it persists across recompositions unless the screen is disposed of (navigated away).

## Detailed Breakdown

### Infrastructure
#### [MODIFY] [build.gradle.kts](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/build.gradle.kts)
- Add `androidx.navigation:navigation-compose`.

### Models & UI
#### [NEW] [SampleItem.kt](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/src/main/java/com/example/filamentdemo/model/SampleItem.kt)
- Define metadata for samples.

#### [NEW] [SampleListScreen.kt](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/src/main/java/com/example/filamentdemo/ui/SampleListScreen.kt)
- Implement sample directory UI.

#### [NEW] [ComingSoonScreen.kt](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/src/main/java/com/example/filamentdemo/ui/ComingSoonScreen.kt)
- Implement stateless placeholder UI.

### Hello Triangle Sample
#### [NEW] [TriangleRenderer.kt](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/src/main/java/com/example/filamentdemo/ui/samples/TriangleRenderer.kt)
- Move `TriangleRenderer` class here.
- Add `Log.d` for creation/destruction.
- Implement idempotency in `destroy()`:
  ```kotlin
  fun destroy() {
      if (engine == null) return // Guard
      // ... cleanup ...
      engine.destroy()
      engine = null // Mark as destroyed
  }
  ```

#### [NEW] [HelloTriangleScreen.kt](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/src/main/java/com/example/filamentdemo/ui/samples/HelloTriangleScreen.kt)
- Move `FilamentTriangleScreen` here and rename to `HelloTriangleScreen`.
- Use `DisposableEffect` for `renderer.destroy()`.

### Main Entry Point
#### [MODIFY] [MainActivity.kt](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/src/main/java/com/example/filamentdemo/MainActivity.kt)
- Implement `NavHost`.
- Clean up old code.

## Verification Plan

### Automated Tests
- Build project and check for compilation errors.

### Manual Verification & Edge Cases
1. **Lifecycle Consistency**:
   - 10+ navigations between List and Triangle.
   - Verify 1:1 `[Filament] Engine Created` and `[Filament] Engine Destroyed` in Logcat.
2. **Idempotency Guard**:
   - **Rapid Double-Tap Back**: Quickly tap the back button/gesture to trigger multiple `onDispose` events. Verify no JNI crashes or handle errors.
3. **Configuration Change (Rotation)**:
   - **Mid-Render Rotation**: Rotate the device while the triangle is rendering. Verify the viewport adjusts correctly and rendering continues.
   - **Rotation followed by Back-Nav**: Rotate, then immediately navigate back. Verify no resource orphans or leaks.
4. **Coming Soon**:
   - Verify all 8 placeholder routes navigate to `ComingSoonScreen` and back correctly.
