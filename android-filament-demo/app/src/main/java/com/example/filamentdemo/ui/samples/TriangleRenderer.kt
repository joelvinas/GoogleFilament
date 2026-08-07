package com.example.filamentdemo.ui.samples

import android.util.Log
import android.view.Choreographer
import android.view.SurfaceHolder
import com.google.android.filament.*
import com.google.android.filament.View
import com.google.android.filament.filamat.MaterialBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import java.nio.ByteBuffer
import java.nio.ByteOrder

class TriangleRenderer {
    private var engine: Engine? = Engine.create(Engine.Backend.OPENGL)
    private var renderer: Renderer? = engine?.createRenderer()
    private var scene: Scene? = engine?.createScene()
    private var view: View? = engine?.createView()
    private var camera: Camera? = engine?.createCamera(engine!!.entityManager.create())
    private var swapChain: SwapChain? = null

    private var vertexBuffer: VertexBuffer? = null
    private var indexBuffer: IndexBuffer? = null
    private var material: Material? = null
    private var triangleEntity: Int = 0

    private val rendererScope = CoroutineScope(Dispatchers.Default)

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
        Log.d("Filament", "[Filament] Engine Created")
        val engine = engine!!
        val view = view!!
        val scene = scene!!
        val camera = camera!!
        val renderer = renderer!!

        view.scene = scene
        view.camera = camera
        
        renderer.clearOptions = Renderer.ClearOptions().apply {
            clear = true
            clearColor = doubleArrayOf(0.1, 0.1, 0.1, 1.0)
        }
        
        setupGeometry(engine, scene)
        setupMaterialAsync(engine)
    }

    private fun setupGeometry(engine: Engine, scene: Scene) {
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
            .boundingBox(Box(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f))
            .geometry(0, RenderableManager.PrimitiveType.TRIANGLES, vertexBuffer!!, indexBuffer!!, 0, 3)
            .culling(false)
            .receiveShadows(false)
            .castShadows(false)
            .build(engine, triangleEntity)

        scene.addEntity(triangleEntity)
    }

    private fun setupMaterialAsync(engine: Engine) {
        val builder = MaterialBuilder()
            .name("TriangleMaterial")
            .shading(MaterialBuilder.Shading.UNLIT)
            .material(
                """
                void material(inout MaterialInputs material) {
                    prepareMaterial(material);
                    material.baseColor = vec4(1.0, 1.0, 1.0, 1.0);
                }
                """.trimIndent()
            )
            .targetApi(MaterialBuilder.TargetApi.ALL)
            .platform(MaterialBuilder.Platform.MOBILE)

        val result = builder.build(engine)

        if (result.isValid) {
            val buffer = result.getBuffer()
            val materialInstance = Material.Builder()
                .payload(buffer, buffer.remaining())
                .build(engine)
            material = materialInstance

            val instance = materialInstance.defaultInstance
            val rm = engine.renderableManager
            rm.setMaterialInstanceAt(rm.getInstance(triangleEntity), 0, instance)
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
        val engine = engine ?: return
        swapChain = engine.createSwapChain(holder.surface)
    }

    fun onSurfaceChanged(holder: SurfaceHolder, width: Int, height: Int) {
        val engine = engine ?: return
        val view = view ?: return
        val camera = camera ?: return

        view.viewport = Viewport(0, 0, width, height)
        
        val aspect = width.toDouble() / height.toDouble()
        camera.setProjection(45.0, aspect, 0.1, 10.0, Camera.Fov.VERTICAL)
        camera.lookAt(0.0, 0.0, 3.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0)
        
        swapChain?.let { engine.destroySwapChain(it) }
        swapChain = engine.createSwapChain(holder.surface)
    }

    fun onSurfaceDestroyed() {
        val engine = engine ?: return
        swapChain?.let { engine.destroySwapChain(it) }
        swapChain = null
    }

    private fun render(frameTimeNanos: Long) {
        val engine = engine ?: return
        val renderer = renderer ?: return
        val view = view ?: return
        val swapChain = swapChain ?: return

        if (renderer.beginFrame(swapChain, frameTimeNanos)) {
            renderer.render(view)
            renderer.endFrame()
        }
    }

    fun destroy() {
        val engine = engine ?: return // Idempotency guard

        Log.d("Filament", "[Filament] Engine Destroyed")
        stopFrameCallback()
        rendererScope.cancel()
        
        if (triangleEntity != 0) {
            engine.destroyEntity(triangleEntity)
            triangleEntity = 0
        }

        vertexBuffer?.let { engine.destroyVertexBuffer(it) }
        vertexBuffer = null

        indexBuffer?.let { engine.destroyIndexBuffer(it) }
        indexBuffer = null

        material?.let { engine.destroyMaterial(it) }
        material = null
        
        scene?.let { engine.destroyScene(it) }
        scene = null

        view?.let { engine.destroyView(it) }
        view = null

        renderer?.let { engine.destroyRenderer(it) }
        renderer = null

        camera?.let { engine.destroyCameraComponent(it.entity) }
        camera = null
        
        swapChain?.let { engine.destroySwapChain(it) }
        swapChain = null
        
        engine.destroy()
        this.engine = null
    }
}
