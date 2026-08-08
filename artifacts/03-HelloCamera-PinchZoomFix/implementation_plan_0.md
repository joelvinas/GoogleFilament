# Implementation Plan - Hello Camera Pinch-Release Snap Fix

Address the issue where the camera snaps to extreme angles when releasing a pinch-zoom gesture.

## User Review Required

> [!IMPORTANT]
> - A new state variable `isPinchingOrReleasing` will be introduced to track when a pinch gesture has started or is in the process of being released.
> - Orbit (drag) events will be ignored as long as this flag is true, preventing the "jump" caused by the focal point shift when fingers are lifted.

## Proposed Changes

### UI & Interaction

#### [MODIFY] [HelloCameraScreen.kt](file:///C:/Users/audranian/source/repos/GoogleFilament/android-filament-demo/app/src/main/java/com/example/filamentdemo/ui/samples/HelloCameraScreen.kt)
- Introduce `var isPinchingOrReleasing = false` in the `AndroidView` factory.
- **`onScaleBegin`**: Set `isPinchingOrReleasing = true`.
- **`onDown`**: Reset `isPinchingOrReleasing = false` and call `renderer.onGrabBegin`.
- **`onScroll`**: Only call `renderer.onGrabUpdate` if `!isPinchingOrReleasing && !scaleDetector.isInProgress`.
- **`onTouchEvent`**:
    - Call `renderer.onGrabEnd()` on `ACTION_POINTER_UP`.
    - Ensure `ACTION_UP` and `ACTION_CANCEL` also call `renderer.onGrabEnd()`.
    - Keep `isPinchingOrReleasing` true until the next `ACTION_DOWN` resets it.

## Verification Plan

### Manual Verification
1. **Pinch-Release Stability**: Perform a pinch-zoom and lift fingers one by one. Verify that the camera does NOT snap to the poles (vertical extremes).
2. **Orbit Continuity**: Verify that single-finger orbit still works correctly when started from a fresh touch.
3. **Pinch-to-Orbit Transition**: Verify that after a pinch is finished and all fingers are lifted, a new drag gesture starts a clean orbit.
