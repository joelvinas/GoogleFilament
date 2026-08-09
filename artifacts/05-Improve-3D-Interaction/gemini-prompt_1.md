### Prompt for Stu (Android Studio Gemini)

 **Task:**
 Redesign `OrbitGestureHandler.kt` to support seamless, non-modal multi-touch gestures (1-finger orbit, 2-finger pan, 2-finger pinch-zoom) with pointer-ID tracking and zero finger re-touch requirement.
 **Key Architecture Decisions & Explicit Constraints:**
 1. **Dispatch Flow & Motion Event Pipeline:**
 * Maintain **scale-detector-first unconditional dispatch**: `scaleDetector.onTouchEvent(event)` **must** execute unconditionally at the start of `onTouchEvent()`.
 * Move raw drag tracking directly to `onTouchEvent` pointer-ID logic (bypassing `GestureDetector.onScroll` for manual state control) **OR** keep explicit state sync. Manual `MotionEvent` handling per active pointer ID is preferred to avoid callback race conditions.
 
 
 2. **Pointer Tracking & Transition Matrix:**
 * **Primary/Secondary IDs:** Maintain `activePointerId1` and `activePointerId2`.
 * **`ACTION_DOWN` (0 $\rightarrow$ 1 finger):** Capture `activePointerId1`, call `manipulator.grabBegin(x, y, strafe = false)` (Orbit).
 * **`ACTION_POINTER_DOWN` (1 $\rightarrow$ 2 fingers):** Capture `activePointerId2`. Explicitly call `manipulator.grabEnd()`. Compute initial centroid $(X_c, Y_c)$ of the two active pointers, and call `manipulator.grabBegin(centroidX, centroidY, strafe = true)` (Pan).
 * **`ACTION_MOVE` (2 fingers active):**
 * *Continuous Centroid Update:* On **every** `ACTION_MOVE` frame while 2 fingers are tracked, recalculate the current centroid $(X_c, Y_c)$ from `activePointerId1` and `activePointerId2` and call `manipulator.grabUpdate(centroidX, centroidY)`.
 * *Pinch-Zoom:* Let `ScaleGestureDetector` handle scale factor updates and feed `manipulator.scroll()`.
 
 
 * **`ACTION_POINTER_UP` (2 $\rightarrow$ 1 finger):** Identify which pointer is lifting. Call `manipulator.grabEnd()`. Set the remaining active pointer as `activePointerId1` (`activePointerId2 = INVALID_POINTER_ID`), get its current screen coordinates $(X_{rem}, Y_{rem})$, and immediately call `manipulator.grabBegin(X_rem, Y_rem, strafe = false)` to resume orbiting cleanly with no re-touch required.
 * **`ACTION_UP` / `ACTION_CANCEL` (All lifted):** Call `manipulator.grabEnd()` unconditionally, reset all pointer IDs, and clear grab state.
 
 
 3. **3rd-Finger / Overflow Guard:**
 * If a 3rd (or 4th) finger touches down (`ACTION_POINTER_DOWN` when 2 pointers are already tracked), **ignore it**. Do not update `activePointerId1` or `activePointerId2`, and do not interrupt the active 2-finger Pan/Zoom state.
 
 
 4. **State Machine Extraction (Testability):**
 * Encapsulate the transition logic (pointer tracking, centroid math, and grab state transitions) into a clean, testable state controller or helper methods so that the centroid/pointer calculations are isolated from Android-view mechanics.
 
 
 
 
 **Deliverables Requested:**
 1. Complete updated Kotlin code for `OrbitGestureHandler.kt` (or renamed `CameraGestureHandler.kt`).
 2. Explanation of how the 2-finger continuous centroid update and 3rd-finger guard are handled in code.
 
 