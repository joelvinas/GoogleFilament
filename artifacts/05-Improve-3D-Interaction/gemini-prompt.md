### Prompt for Stu (Android Studio Gemini)

 **Task & Objective:**
 Redesign `OrbitGestureHandler.kt` to support seamless, non-modal multi-touch gestures (1-finger orbit, 2-finger pan, 2-finger pinch-zoom) with clean handoffs and zero finger re-touch requirement.
 **Target Interaction Model (Google Maps / 3D Viewer Standard):**
 1. **1-Finger Drag:** Orbit/Rotate (`manipulator.grabBegin(x, y, strafe = false)` + `grabUpdate(x, y)`).
 2. **2-Finger Translation:** Pan/Strafe (`manipulator.grabBegin(centroidX, centroidY, strafe = true)` + `grabUpdate(centroidX, centroidY)`).
 3. **2-Finger Pinch:** Zoom/Dolly (using pinch scale factor delta fed to `manipulator.scroll()`).
 4. **Seamless Transitions:** Dropping from 2 fingers to 1 finger must seamlessly transition from Pan/Zoom into Orbit without requiring the user to lift and re-touch the screen.
 
 
 **Filament Manipulator API Capabilities:**
 * In `Mode.ORBIT`, `manipulator.grabBegin(x, y, strafe = false)` initiates rotation.
 * `manipulator.grabBegin(x, y, strafe = true)` initiates translation (pan) using the same lifecycle (`grabBegin` $\rightarrow$ `grabUpdate` $\rightarrow$ `grabEnd`).
 
 
 **Implementation Requirements:**
 1. **Explicit Pointer ID Tracking:** Track active `MotionEvent` pointer IDs (`getPointerId()`) and indices (`findPointerIndex()`). Do not rely on simple `pointerCount` checks alone.
 2. **Transition Matrix Handling:**
 * **1-Finger Initial Drag (`ACTION_DOWN` / `ACTION_MOVE`):** Start orbit grab (`strafe = false`).
 * **1 $\rightarrow$ 2 Fingers (`ACTION_POINTER_DOWN`):** Call `manipulator.grabEnd()`, calculate initial 2-finger centroid $(X_c, Y_c)$, and immediately call `manipulator.grabBegin(centroidX, centroidY, strafe = true)` to start panning.
 * **2 Fingers Active (`ACTION_MOVE`):**
 * *Pan Component:* Feed centroid delta updates to `manipulator.grabUpdate(centroidX, centroidY)`.
 * *Zoom Component:* Calculate pinch distance delta (or integrate with `ScaleGestureDetector`) and feed step adjustments to `manipulator.scroll(x, y, delta)`.
 
 
 * **2 $\rightarrow$ 1 Finger (`ACTION_POINTER_UP`):** Call `manipulator.grabEnd()` for the pan/zoom grab, identify the remaining active pointer ID, and immediately call `manipulator.grabBegin(remainingX, remainingY, strafe = false)` to resume orbiting without a jump or requiring a re-touch.
 * **All Fingers Lifted (`ACTION_UP` / `ACTION_CANCEL`):** Call `manipulator.grabEnd()`, reset all tracking state.
 
 
 3. **Preserve Baseline Guards:** Maintain `ScaleGestureDetector` dispatch order and prevent fling/jump artifacts across all transition states.
 
 
 **Deliverable Requested:**
 1. Complete updated Kotlin code for `OrbitGestureHandler.kt` (or `CameraGestureHandler.kt` if renamed).
 2. A brief explanatory note on how 2-finger motion decomposition (centroid pan vs. scale zoom) was structured.
 
 
