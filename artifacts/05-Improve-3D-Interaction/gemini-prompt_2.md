### Prompt for Stu (Android Studio Gemini)

 **Task & Objective:**
 Implement the complete redesign of `OrbitGestureHandler.kt` (or `CameraGestureHandler.kt`) to support continuous, non-modal 3D scene navigation (1-finger orbit, 2-finger pan, 2-finger pinch-zoom) with clean pointer-ID tracking and zero finger re-touch requirements.
 **Architectural Requirements & Constraints:**
 1. **Unconditional Scale Dispatch Discipline:**
 * At the top of `onTouchEvent(event)`, call `scaleDetector.onTouchEvent(event)` **unconditionally** before any custom pointer-tracking logic or state machine execution.
 
 
 2. **MotionEvent Adapter & Isolated Pure State Machine:**
 * Extract the core tracking and calculation state into a pure Kotlin state machine (e.g., `CameraGestureStateMachine`).
 * The state machine should accept pure Kotlin inputs (e.g., `Pointer` data classes containing `id`, `x`, `y` coordinates) rather than raw `MotionEvent` objects. This allows the state machine to be trivially unit-tested on a plain JVM.
 * The outer gesture handler reads `MotionEvent` actions, converts active pointers into pure data tuples, passes them into the state machine, and routes state machine output events directly to Filament's `Manipulator` (`grabBegin`, `grabUpdate`, `grabEnd`, `scroll`).
 
 
 3. **Transition Matrix & Pointer-ID Discipline:**
 * **0 $\rightarrow$ 1 Finger (`ACTION_DOWN`):** Capture `activePointerId1`. Trigger `manipulator.grabBegin(x, y, strafe = false)` (Orbit).
 * **1 $\rightarrow$ 2 Fingers (`ACTION_POINTER_DOWN`):** Capture `activePointerId2`. Trigger `manipulator.grabEnd()`. Calculate the initial centroid $(X_c, Y_c)$ of the two active pointers and trigger `manipulator.grabBegin(centroidX, centroidY, strafe = true)` (Pan).
 * **2 Active Fingers (`ACTION_MOVE`):**
 * *Continuous Pan:* Recalculate the two-finger centroid $(X_c, Y_c)$ on **every frame** and invoke `manipulator.grabUpdate(centroidX, centroidY)`.
 * *Continuous Pinch-Zoom:* `ScaleGestureDetector` handles pinch scale deltas and invokes `manipulator.scroll()`.
 
 
 * **2 $\rightarrow$ 1 Finger (`ACTION_POINTER_UP`):** Identify which finger lifted. Trigger `manipulator.grabEnd()`. Re-assign the remaining active pointer to `activePointerId1` (and invalidate `activePointerId2`), grab its current $(X, Y)$ position, and immediately trigger `manipulator.grabBegin(x, y, strafe = false)` to resume orbiting cleanly without requiring the user to lift and re-touch.
 * **All Fingers Lifted (`ACTION_UP` / `ACTION_CANCEL`):** Trigger `manipulator.grabEnd()`, reset all pointer IDs, and clear all grab state.
 * **3rd-Finger / Overflow Guard:** Ignore any 3rd (or 4th) pointer down events while two active pointers are already tracked. Do not update primary/secondary tracking IDs or disrupt active pan/zoom state.
 
 
 
 
 **Verification Requirements:**
 * Include unit tests verifying the pure state machine using mock pointer data.
 * Verify the continuous compound gesture sequence manually on-device: *Start single-finger orbit $\rightarrow$ drop second finger to pan $\rightarrow$ pinch and drift sideways $\rightarrow$ lift second finger back to one finger $\rightarrow$ continue orbiting seamlessly* (confirming no stale momentum, jumping, or position snapping across transitions).
 
 
 **Deliverable Requested:**
 1. Source code for `CameraGestureStateMachine.kt` (pure JVM state machine) and unit tests.
 2. Source code for updated `OrbitGestureHandler.kt` / `CameraGestureHandler.kt` using the state machine.
 3. Integration updates in `LitCubeScreen.kt` and `HelloCameraScreen.kt`.
 
 
