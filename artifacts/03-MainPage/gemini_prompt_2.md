Please revise the Implementation Plan for the Main Page Navigation feature to address the following technical callouts:

### Required Revisions & Edge-Case Coverage

1. **Explicit Edge-Case Testing in Verification Plan**:
   - The verification plan must explicitly exercise the idempotent `destroy()` guard. Add specific test steps for:
     - **Rapid double-tap back navigation** (rapidly tapping back/gesturing while in `HelloTriangleScreen` to ensure double `onDispose` execution does not throw JNI or handles-based exceptions).
     - **Device rotation mid-render** right before back-navigating to ensure state isn't orphaned during configuration changes.

2. **Configuration Change (Rotation) Retention**:
   - Explicitly detail how `SurfaceHolder.Callback` and viewport/surface resize logic are preserved during rotation within `HelloTriangleScreen`.
   - Ensure the surface change listener logic lives within the `DisposableEffect` scope in `HelloTriangleScreen` so screen rotation doesn't drop rendering or leak surfaces during recomposition.

3. **Stateless `ComingSoonScreen` Target Route**:
   - Explicitly note in the Detailed Breakdown that `ComingSoonScreen` is stateless and requires no custom `DisposableEffect` or Filament teardown logic, but must handle system back-navigation natively via the Compose NavHost backstack.

4. **File Architecture Clarification**:
   - Clarify whether `TriangleRenderer` should be co-located inside `HelloTriangleScreen.kt` or remain in its own standalone file (`TriangleRenderer.kt`). (Specify your preference here: e.g., "Keep `TriangleRenderer.kt` as a separate file for modularity" or "Co-locate inside `HelloTriangleScreen.kt`").

Please produce the updated **Implementation Plan** incorporating these structural and verification updates.