# Implementation Plan - Main Page (Landing Menu) with Robust Lifecycle Management

This plan outlines the steps to create an interactive directory for Filament render samples using Jetpack Compose Navigation, with a critical focus on robust native resource lifecycle management.

## User Review Required

> [!IMPORTANT]
> **Lifecycle Management**: We are shifting away from Activity-based lifecycle observation for Filament resource cleanup. Instead, we will bind the `Engine` and other native resources strictly to the Compose `DisposableEffect` of each sample screen. This ensures that navigating back within the `NavHost` correctly triggers resource teardown even while the Activity remains alive.

## Proposed Changes

### Dependencies
- Add `androidx.navigation:navigation-compose` to `app/build.gradle.kts`.

### Models
- **`SampleItem.kt`**: Data class for sample metadata (Title, Description, Complexity Tag).

### UI Components
- **`SampleListScreen.kt`**: A `LazyColumn` of Material 3 `Card` items. Each card displays sample details and triggers navigation.
- **`ComingSoonScreen.kt`**: A placeholder scaffold for samples not yet implemented.
- **`HelloTriangleScreen.kt`**: Refactored screen and renderer for the "Hello Triangle" sample.

### Navigation
- Define a `NavHost` in `MainActivity.kt`.
- Set `SampleListScreen` as the start destination.
- Map routes for all 9 samples, wiring "Hello Triangle" to its implementation and the rest to `ComingSoonScreen`.

### Native Resource Cleanup (Robustness)
- **`TriangleRenderer.kt`**:
    - Add `Log.d("Filament", "[Filament] Engine Created")` in `init`.
    - Add `Log.d("Filament", "[Filament] Engine Destroyed")` in `destroy()`.
    - Ensure `destroy()` checks for null/zero handles to be idempotent.
- **`HelloTriangleScreen.kt`**:
    - Use `DisposableEffect(Unit)` to manage the `TriangleRenderer` lifecycle.
    - Call `renderer.destroy()` in `onDispose`.

## Detailed Breakdown

### Infrastructure

#### [MODIFY] [build.gradle.kts](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/build.gradle.kts)
- Add navigation dependency.

### Models & UI

#### [NEW] [SampleItem.kt](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/src/main/java/com/example/filamentdemo/model/SampleItem.kt)
- `enum class Complexity { Beginner, Intermediate, Advanced }`
- `data class SampleItem(val id: String, val title: String, val description: String, val complexity: Complexity)`

#### [NEW] [SampleListScreen.kt](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/src/main/java/com/example/filamentdemo/ui/SampleListScreen.kt)
- Implement `SampleListScreen` using `Scaffold` and `LazyColumn`.
- Define the list of 9 samples.

#### [NEW] [ComingSoonScreen.kt](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/src/main/java/com/example/filamentdemo/ui/ComingSoonScreen.kt)
- Simple placeholder UI.

### Hello Triangle Refactoring

#### [NEW] [HelloTriangleScreen.kt](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/src/main/java/com/example/filamentdemo/ui/samples/HelloTriangleScreen.kt)
- Contains `HelloTriangleScreen` Composable and `TriangleRenderer` class.
- Incorporate logging and idempotency in `TriangleRenderer.destroy()`.

### Main Entry Point

#### [MODIFY] [MainActivity.kt](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/src/main/java/com/example/filamentdemo/MainActivity.kt)
- Setup `NavHost` with all routes.
- Remove old `FilamentTriangleScreen` and `TriangleRenderer` implementation (moved to separate file).

## Verification Plan

### Automated Tests
- Build project to verify dependency resolution.

### Manual Verification (Crucial)
1. **Navigation Flow**:
    - Launch app -> `SampleListScreen`.
    - Click "Hello Triangle" -> Verify render.
    - Back button -> Verify return to `SampleListScreen`.
    - Repeat navigation 10+ times.
2. **Lifecycle Verification (Logcat)**:
    - Filter Logcat by `Filament`.
    - Verify `[Filament] Engine Created` appears on every enter.
    - Verify `[Filament] Engine Destroyed` appears on every back navigation.
    - Confirm 1-to-1 pairing and no "leaked" engines.
3. **Coming Soon**:
    - Verify all other samples navigate to the placeholder screen and back correctly.
