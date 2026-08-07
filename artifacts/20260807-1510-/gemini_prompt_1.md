We need to create a "Main Page" (landing menu) for our Jetpack Compose + Filament Android app using Jetpack Compose Navigation (`androidx.navigation:navigation-compose`).

### Requirements

1. **Navigation Architecture**:
   - The root destination must be `SampleListScreen` (Main Page).
   - Each sample route must navigate to a dedicated screen (e.g., `HelloTriangleScreen`).
   - Pressing the OS back button/gesture from any sample screen must navigate back to `SampleListScreen`.

2. **Crucial Lifecycle & Native Resource Teardown Fix**:
   - **Do NOT rely exclusively on `Activity` lifecycle observers (`onPause`/`onDestroy`) for native resource cleanup.** Navigating back from `HelloTriangleScreen` leaves the `Activity` alive and resumed, meaning Activity observers will fail to fire upon navigation.
   - Bind all Filament initialization (`Engine`, `SwapChain`, `Renderer`) and teardown strictly inside a Compose `DisposableEffect` scoped to the individual sample screen.
   - Call `TriangleRenderer.destroy()` inside `onDispose { }` of `DisposableEffect`. Ensure this teardown is fully idempotent to prevent double-destruction crashes if the Activity is destroyed simultaneously.

3. **Main Page UI (`SampleListScreen`)**:
   - Scrollable `LazyColumn` listing all current and upcoming samples.
   - Each card item should list: Sample Title, Brief Description, and Complexity Tag.
   - Wire up **Hello Triangle** to navigate to `HelloTriangleScreen`.
   - Provide placeholder routes displaying "Coming Soon" screens for future samples:
     1. Hello Triangle
     2. Hello Camera
     3. Lit Cube
     4. Material Builder
     5. Material Instance Stress
     6. Procedural Effect
     7. Procedural Texture Quad
     8. Transparent View
     9. gLTF Viewer

4. **Verification Plan**:
   - Add explicit Logcat logging to verify lifecycle pairs: print `[Filament] Engine Created` during setup and `[Filament] Engine Destroyed` inside `onDispose`.
   - Test by navigating into `HelloTriangleScreen` and back to `SampleListScreen` 10+ consecutive times while observing Logcat to confirm every creation has a 1-to-1 matching teardown log.

Please provide a detailed **Implementation Plan** first before generating any code changes.