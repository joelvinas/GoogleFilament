package com.example.filamentdemo

import android.os.Bundle
import android.view.Choreographer
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.filament.*
import com.google.android.filament.View
import com.google.android.filament.filamat.MaterialBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MainActivity : ComponentActivity() {
    companion object {
        init {
            Filament.init()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FilamentTriangleScreen()
        }
    }
}

@Composable
fun FilamentTriangleScreen() {
    val lifecycleOwner = LocalLifecycleOwner.current
    val renderer = remember { TriangleRenderer() }

    DisposableEffect(lifecycleOwner) {
        val observer = object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                renderer.startFrameCallback()
            }

            override fun onPause(owner: LifecycleOwner) {
                renderer.stopFrameCallback()
            }

            override fun onDestroy(owner: LifecycleOwner) {
                renderer.destroy()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            renderer.destroy()
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            SurfaceView(context).apply {
                holder.addCallback(object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        renderer.onSurfaceCreated(holder)
                    }

                    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                        renderer.onSurfaceChanged(holder, width, height)
                    }

                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                        renderer.onSurfaceDestroyed()
                    }
                })
            }
        }
    )
}

class TriangleRenderer {
    private var engine: Engine = Engine.create(Engine.Backend.OPENGL)
    private var renderer: Renderer = engine.createRenderer()
    private var scene: Scene = engine.createScene()
    private var view: View = engine.createView()
    private var camera: Camera = engine.createCamera(engine.entityManager.create())
    private var swapChain: SwapChain? = null

    private var vertexBuffer: VertexBuffer? = null
    private var indexBuffer: IndexBuffer? = null
    private var material: Material? = null
    private var triangleEntity: Int = 0

    private val choreographer = Choreographer.getInstance()
    private var frameCallbackActive = false

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (frameCallbackActive) {
                render(frameTimeNanos)
                choreographer.postFrameCallback(this)
            }
        }
    }

    init {
        view.scene = scene
        view.camera = camera
        setupGeometry()
        setupMaterialAsync()
    }

    private fun setupGeometry() {
        val vertexCount = 3
        val vertexSize = (3 + 3) * 4 // pos(float3) + color(float3)
        val vertexData = ByteBuffer.allocate(vertexCount * vertexSize)
            .order(ByteOrder.nativeOrder())

        // Triangle vertices (x, y, z, r, g, b)
        // Top
        vertexData.putFloat(0.0f); vertexData.putFloat(0.5f); vertexData.putFloat(0.0f)
        vertexData.putFloat(1.0f); vertexData.putFloat(0.0f); vertexData.putFloat(0.0f)
        // Left
        vertexData.putFloat(-0.5f); vertexData.putFloat(-0.5f); vertexData.putFloat(0.0f)
        vertexData.putFloat(0.0f); vertexData.putFloat(1.0f); vertexData.putFloat(0.0f)
        // Right
        vertexData.putFloat(0.5f); vertexData.putFloat(-0.5f); vertexData.putFloat(0.0f)
        vertexData.putFloat(0.0f); vertexData.putFloat(0.0f); vertexData.putFloat(1.0f)

        vertexData.flip()

        vertexBuffer = VertexBuffer.Builder()
            .bufferCount(1)
            .vertexCount(vertexCount)
            .attribute(VertexBuffer.VertexAttribute.POSITION, 0, VertexBuffer.AttributeType.FLOAT3, 0, vertexSize)
            .attribute(VertexBuffer.VertexAttribute.COLOR, 0, VertexBuffer.AttributeType.FLOAT3, 12, vertexSize)
            .build(engine)

        vertexBuffer?.setBufferAt(engine, 0, vertexData)

        val indexData = ByteBuffer.allocate(3 * 2).order(ByteOrder.nativeOrder())
        indexData.putShort(0); indexData.putShort(1); indexData.putShort(2)
        indexData.flip()

        indexBuffer = IndexBuffer.Builder()
            .indexCount(3)
            .bufferType(IndexBuffer.Builder.IndexType.USHORT)
            .build(engine)

        indexBuffer?.setBuffer(engine, indexData)

        triangleEntity = EntityManager.get().create()
        RenderableManager.Builder(1)
            .geometry(0, RenderableManager.PrimitiveType.TRIANGLES, vertexBuffer!!, indexBuffer!!, 0, 3)
            .culling(false)
            .build(engine, triangleEntity)

        scene.addEntity(triangleEntity)
    }

    private fun setupMaterialAsync() {
        MaterialBuilder.init()
        CoroutineScope(Dispatchers.Default).launch {
            val builder = MaterialBuilder()
                .name("TriangleMaterial")
                .shading(MaterialBuilder.Shading.UNLIT)
                .material(
                    """
                    void material(inout MaterialInputs material) {
                        prepareMaterial(material);
                        material.baseColor = getColor();
                    }
                    """.trimIndent()
                )
                .targetApi(MaterialBuilder.TargetApi.ALL)
                .platform(MaterialBuilder.Platform.MOBILE)

            // Rule 1: Pass engine instance into build(engine)
            val result = builder.build(engine)

            if (result.isValid) {
                val buffer = result.getBuffer()
                withContext(Dispatchers.Main) {
                    material = Material.Builder()
                        .payload(buffer, buffer.remaining())
                        .build(engine)

                    val instance = material?.defaultInstance
                    engine.getRenderableManager().setMaterialInstanceAt(
                        engine.getRenderableManager().getInstance(triangleEntity),
                        0,
                        instance!!
                    )
                }
            }
        }
    }

    fun startFrameCallback() {
        if (!frameCallbackActive) {
            frameCallbackActive = true
            choreographer.postFrameCallback(frameCallback)
        }
    }

    fun stopFrameCallback() {
        frameCallbackActive = false
        choreographer.removeFrameCallback(frameCallback)
    }

    fun onSurfaceCreated(holder: SurfaceHolder) {
        swapChain = engine.createSwapChain(holder.surface)
    }

    fun onSurfaceChanged(holder: SurfaceHolder, width: Int, height: Int) {
        // Rule 3: Update viewport and recreate swapchain
        view.viewport = Viewport(0, 0, width, height)
        
        swapChain?.let { engine.destroySwapChain(it) }
        swapChain = engine.createSwapChain(holder.surface)
    }

    fun onSurfaceDestroyed() {
        swapChain?.let { engine.destroySwapChain(it) }
        swapChain = null
    }

    private fun render(frameTimeNanos: Long) {
        if (swapChain != null) {
            if (renderer.beginFrame(swapChain!!, frameTimeNanos)) {
                renderer.render(view)
                renderer.endFrame()
            }
        }
    }

    fun destroy() {
        stopFrameCallback()
        
        engine.destroyEntity(triangleEntity)
        vertexBuffer?.let { engine.destroyVertexBuffer(it) }
        indexBuffer?.let { engine.destroyIndexBuffer(it) }
        material?.let { engine.destroyMaterial(it) }
        
        engine.destroyScene(scene)
        engine.destroyView(view)
        engine.destroyRenderer(renderer)
        engine.destroyCameraComponent(camera.entity)
        
        swapChain?.let { engine.destroySwapChain(it) }
        
        engine.destroy()
    }
}
