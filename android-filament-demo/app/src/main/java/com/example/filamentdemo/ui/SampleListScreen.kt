package com.example.filamentdemo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.filamentdemo.model.Complexity
import com.example.filamentdemo.model.SampleItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SampleListScreen(
    onSampleClick: (SampleItem) -> Unit
) {
    val samples = listOf(
        SampleItem("hello_triangle", "Hello Triangle", "Basic geometry setup, simple shader, surface lifecycle.", Complexity.Beginner),
        SampleItem("hello_camera", "Hello Camera", "3D perspective camera with orbit/pan touch gesture support.", Complexity.Beginner),
        SampleItem("lit_cube", "Lit Cube", "Directional light, point lights, surface normals, and basic PBR materials.", Complexity.Beginner),
        SampleItem("material_builder", "Material Builder", "Dynamic runtime compilation of custom Filament material files (.mat).", Complexity.Intermediate),
        SampleItem("material_stress", "Material Instance Stress", "Rendering multiple instances with varying parameters to test performance.", Complexity.Intermediate),
        SampleItem("procedural_effect", "Procedural Effect", "Custom animated post-processing and surface shader effects.", Complexity.Advanced),
        SampleItem("procedural_texture", "Procedural Texture Quad", "Programmatically generated dynamic textures mapped to 2D geometry.", Complexity.Intermediate),
        SampleItem("transparent_view", "Transparent View", "Render view blending over native Compose UI elements.", Complexity.Advanced),
        SampleItem("gltf_viewer", "gLTF Viewer", "Loading, parsing, and animating complete 3D .gltf/.glb models.", Complexity.Intermediate)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Filament Samples") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(samples) { sample ->
                SampleCard(sample = sample, onClick = { onSampleClick(sample) })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SampleCard(
    sample: SampleItem,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = sample.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                ComplexityTag(complexity = sample.complexity)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = sample.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ComplexityTag(complexity: Complexity) {
    val color = when (complexity) {
        Complexity.Beginner -> MaterialTheme.colorScheme.primaryContainer
        Complexity.Intermediate -> MaterialTheme.colorScheme.secondaryContainer
        Complexity.Advanced -> MaterialTheme.colorScheme.tertiaryContainer
    }
    
    val textColor = when (complexity) {
        Complexity.Beginner -> MaterialTheme.colorScheme.onPrimaryContainer
        Complexity.Intermediate -> MaterialTheme.colorScheme.onSecondaryContainer
        Complexity.Advanced -> MaterialTheme.colorScheme.onTertiaryContainer
    }

    Surface(
        color = color,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = complexity.name,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Medium
        )
    }
}
