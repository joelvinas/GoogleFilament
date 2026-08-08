We need to create a "Main Page" (landing menu) for our Jetpack Compose + Filament Android app. This page will serve as an interactive directory for all upcoming Filament render samples.

### Requirements

1. **Navigation Architecture**:
   - Use Jetpack Compose Navigation (`androidx.navigation:navigation-compose`).
   - The root destination should be `SampleListScreen` (Main Page).
   - Each sample route must navigate to a dedicated screen (e.g., `HelloTriangleScreen`).
   - Pressing the OS system back button (or performing a back gesture) from any sample screen must cleanly navigate back to `SampleListScreen`.

2. **Main Page UI (`SampleListScreen`)**:
   - Display a clean, scrollable list (e.g., `LazyColumn`) of clickable sample items.
   - Design each item with a card layout displaying:
     - Sample Title
     - Brief Description
     - Complexity level tag (e.g., "Beginner", "Intermediate", "Advanced")
   - For now, wire up **Hello Triangle** to navigate to our existing `FilamentTriangleScreen`.
   - Add placeholder routes/screens for the remaining 8 samples so clicking them displays a simple "Coming Soon" scaffold.

3. **Sample List Items (Ordered by Complexity)**:
   1. **Hello Triangle**: Basic geometry setup, simple shader, surface lifecycle.
   2. **Hello Camera**: 3D perspective camera with orbit/pan touch gesture support.
   3. **Lit Cube**: Directional light, point lights, surface normals, and basic PBR materials.
   4. **Material Builder**: Dynamic runtime compilation of custom Filament material files (`.mat`).
   5. **Material Instance Stress**: Rendering multiple instances with varying parameters to test performance.
   6. **Procedural Effect**: Custom animated post-processing and surface shader effects.
   7. **Procedural Texture Quad**: Programmatically generated dynamic textures mapped to 2D geometry.
   8. **Transparent View**: Render view blending over native Compose UI elements.
   9. **gLTF Viewer**: Loading, parsing, and animating complete 3D `.gltf`/`.glb` models.

4. **Lifecycle & Native Resource Cleanup**:
   - Ensure that navigating away from a sample (via back button or gesture) completely releases the Filament `Engine`, native buffers, and resources without memory leaks or native crashes.

Please provide an **Implementation Plan** first, outlining the dependency updates, navigation graph structure, and file changes needed.