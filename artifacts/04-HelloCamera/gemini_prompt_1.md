Please revise the Implementation Plan for the **Hello Camera** sample to address the following technical callouts:

### Required Technical Clarifications & Edge-Case Coverage

1. **Touch Dispatch & Gesture Disambiguation Strategy**:
   - Detail the exact `onTouchEvent` routing for combining single-finger orbit and two-finger pinch-zoom:
     - Feed all `MotionEvent` instances to **both** `GestureDetector` and `ScaleGestureDetector`.
     - Explicitly gate the orbit-drag deltas using `ScaleGestureDetector.isInProgress()` (e.g., ignore single-finger orbit deltas when a pinch gesture is currently active to prevent camera jumps on finger touchdown/lift).

2. **Filament `Manipulator` API Interaction Contract**:
   - Specify the exact write-side methods mapped to gesture callbacks:
     - Single-finger drag (orbit): Call `manipulator.grabBegin(x, y, false)`, `manipulator.grabUpdate(x, y)`, and `manipulator.grabEnd()`.
     - Two-finger pinch (zoom): Translate scale deltas or scroll offsets directly into `manipulator.scroll(x, y, delta)`.
     - Frame update (read-side): On every frame update, pull `manipulator.getLookAt(eye, target, up)` to re-center and position `camera.lookAt(...)`.

3. **Touch Race Condition Guard**:
   - Add explicit null-checks in touch/gesture listener callbacks (`manipulator?.let { ... }`) to guard against touch events firing before `Manipulator` or `Engine` initialization completes inside `DisposableEffect`.

4. **Material Creation & Engine Threading**:
   - Explicitly define how the 3D geometry material is created.
   - If reusing the existing unlit material compiled for Hello Triangle, state it clearly.
   - If generating a new material instance via `MaterialBuilder`, enforce Main-thread allocation (or the off-main-thread pattern established in the Hello Triangle walkthrough) to avoid JNI `SIGABRT` thread-adoption issues.

5. **Idempotent Cleanup & Logging Enforcement**:
   - Restate explicitly under `CameraRenderer.kt` that `destroy()` must be strictly idempotent (guarded with null checks on native handles like `engine`, `manipulator`, and `swapChain`).
   - Retain Logcat verification logging (`[FilamentCamera] Camera Created` / `[FilamentCamera] Camera Destroyed`) to verify 1-to-1 teardown on back-navigation.

6. **Configuration Change (Rotation) Scope Alignment**:
   - Frame the device rotation verification step cleanly without enforcing a premature architectural pattern: "Rotate the device mid-render and confirm the screen does not crash or leave orphaned native handles."
   - Detailed surface-resize optimization across samples will be standardized in a dedicated refactoring step.

Please produce the revised **Implementation Plan** incorporating these exact mechanics and save it to `artifacts/04-hello-camera/plan.md`.