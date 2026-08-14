### Prompt for Stu (Android Studio Gemini)

 **Context & Plan Revision Required:**
 Stu, before writing the code diffs for the `Manipulator.update()` fix and test additions, we need to update our **Implementation Plan** to address one critical timing math risk and two consistency/verification gaps identified during architectural review.
 Please update the Implementation Plan to incorporate these **Three Plan Revisions**:
 1. **Frame Timing Sentinel Fix (CRITICAL):**
 * `Choreographer.frameTimeNanos` provides nanoseconds since system boot. Defaulting `lastFrameTimeNanos = 0L` will compute a `deltaTime` of thousands of seconds on the first frame ($(\text{frameTimeNanos} - 0) / 10^9$), causing a massive visual camera jump/snap on screen launch or surface recreation.
 * **Required Fix:** Use an explicit sentinel (`lastFrameTimeNanos = -1L` or a `hasRenderedFrame: Boolean = false` flag).
 * **Frame 1 (or Post-Reset):** Skip `manipulator.update()`, record `lastFrameTimeNanos = currentFrameTimeNanos`, set flag to `true`.
 * **Frame 2+:** Compute `deltaTimeSeconds = (currentFrameTimeNanos - lastFrameTimeNanos) / 1_000_000_000f`, invoke `manipulator.update(deltaTimeSeconds)` immediately before `manipulator.getLookAt(...)`, and update `lastFrameTimeNanos`.
  
 2. **Symmetric Timing Resets Across Renderers:**
 * Align `LitCubeRenderer.kt` and `CameraRenderer.kt` so that `lastFrameTimeNanos` is reset (`-1L` / `hasRenderedFrame = false`) symmetrically in **both** of the following scenarios:
 1. When the frame callback is stopped/paused (`onDispose` / navigation away).
 2. During surface changes/resizes (`onSurfaceChanged`), to prevent invalid delta calculations across orientation changes or surface recreations.
  
 3. **Specific Double-Sample Verification Protocol:**
 * Update the manual verification section of the plan to explicitly test **both** `Hello Camera` and `Lit Cube` using these three precise test steps:
 * **Test A (Stationary 2-Finger Zoom):** Device flat on desk, two fingers down, move apart $\rightarrow$ verify smooth zoom in; move together $\rightarrow$ verify smooth zoom out.
 * **Test B (Single-Axis Pivot Zoom):** Hold one finger stationary, move only the other finger along a single axis $\rightarrow$ verify zoom responds correctly without pan jumping.
 * **Test C (First-Frame Snap Check):** Enter each sample screen and perform a surface rotation $\rightarrow$ verify zero position snap/jump artifacts on the initial rendered frame.
  
 **Deliverable Requested:**
 Updated **Implementation Plan** reflecting these precise timing, symmetry, and testing requirements. *Do not output code diffs yet; provide the corrected plan first for final sign-off.*