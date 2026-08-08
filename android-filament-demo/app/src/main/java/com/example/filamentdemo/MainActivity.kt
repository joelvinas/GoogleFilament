package com.example.filamentdemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.filamentdemo.ui.ComingSoonScreen
import com.example.filamentdemo.ui.SampleListScreen
import com.example.filamentdemo.ui.samples.HelloCameraScreen
import com.example.filamentdemo.ui.samples.HelloTriangleScreen
import com.google.android.filament.Filament
import com.google.android.filament.filamat.MaterialBuilder

class MainActivity : ComponentActivity() {
    companion object {
        init {
            Filament.init()
            MaterialBuilder.init()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FilamentApp()
        }
    }
}

@Composable
fun FilamentApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "sample_list") {
        composable("sample_list") {
            SampleListScreen(
                onSampleClick = { sample ->
                    navController.navigate(sample.id)
                }
            )
        }
        
        composable("hello_triangle") {
            HelloTriangleScreen()
        }
        
        composable("hello_camera") {
            HelloCameraScreen()
        }
        
        // Placeholder routes
        val placeholders = listOf(
            "lit_cube" to "Lit Cube",
            "material_builder" to "Material Builder",
            "material_stress" to "Material Instance Stress",
            "procedural_effect" to "Procedural Effect",
            "procedural_texture" to "Procedural Texture Quad",
            "transparent_view" to "Transparent View",
            "gltf_viewer" to "gLTF Viewer"
        )
        
        placeholders.forEach { (id, title) ->
            composable(id) {
                ComingSoonScreen(
                    title = title,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
