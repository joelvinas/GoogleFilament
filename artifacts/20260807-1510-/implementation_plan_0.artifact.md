# Implementation Plan - Main Page (Landing Menu) for Filament Samples

This plan outlines the steps to create an interactive directory for Filament render samples using Jetpack Compose Navigation.

## Proposed Changes

### Dependencies
- Add `androidx.navigation:navigation-compose` to `app/build.gradle.kts`.

### Models
- Create `SampleItem` data class to represent each sample (title, description, complexity).

### UI Components
- **`SampleListScreen`**: A `LazyColumn` of cards displaying sample details and navigating to the respective screens.
- **`ComingSoonScreen`**: A simple placeholder screen for samples not yet implemented.
- **`HelloTriangleScreen`**: Refactor the existing triangle rendering logic from `MainActivity.kt` into this new screen.

### Navigation
- Define routes for each sample.
- Set up `NavHost` in `MainActivity` with `SampleListScreen` as the start destination.

### File Structure
- `com.example.filamentdemo.model.SampleItem`
- `com.example.filamentdemo.ui.SampleListScreen`
- `com.example.filamentdemo.ui.ComingSoonScreen`
- `com.example.filamentdemo.ui.HelloTriangleScreen` (Extracted from `MainActivity.kt`)
- `com.example.filamentdemo.MainActivity` (Updated to host `NavHost`)

## Detailed Breakdown

### [Component Name]

#### [MODIFY] [build.gradle.kts](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/build.gradle.kts)
- Add `androidx.navigation:navigation-compose`.

#### [NEW] [SampleItem.kt](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/src/main/java/com/example/filamentdemo/model/SampleItem.kt)
- Define `SampleItem` and `Complexity` enum.

#### [NEW] [SampleListScreen.kt](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/src/main/java/com/example/filamentdemo/ui/SampleListScreen.kt)
- Implement the list of samples using `LazyColumn` and `Card`.

#### [NEW] [ComingSoonScreen.kt](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/src/main/java/com/example/filamentdemo/ui/ComingSoonScreen.kt)
- Implement a simple `Scaffold` with "Coming Soon" text.

#### [NEW] [HelloTriangleScreen.kt](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/src/main/java/com/example/filamentdemo/ui/HelloTriangleScreen.kt)
- Move `FilamentTriangleScreen` and `TriangleRenderer` here.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/src/main/java/com/example/filamentdemo/MainActivity.kt)
- Replace direct call to `FilamentTriangleScreen` with a `NavHost`.

## Verification Plan

### Automated Tests
- Build the project to ensure all dependencies and code changes are correct.

### Manual Verification
1. Launch the app and verify the `SampleListScreen` is displayed.
2. Click on "Hello Triangle" and verify it navigates to the triangle render.
3. Press the back button and verify it returns to the list.
4. Click on any other sample and verify it shows the "Coming Soon" screen.
5. Verify that navigating back and forth does not cause crashes (ensuring `Engine.destroy()` is called).
