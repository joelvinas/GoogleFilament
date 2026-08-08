package com.example.filamentdemo.ui.samples

import android.util.Log
import android.view.Choreographer
import android.view.SurfaceHolder
import com.google.android.filament.*
import com.google.android.filament.View
import com.google.android.filament.filamat.MaterialBuilder
import com.google.android.filament.utils.Manipulator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import java.nio.ByteBuffer
import java.nio.ByteOrder

class CameraRenderer {
    private var engine: Engine? = Engine.create(Engine.Backend.OPENGL)
    private var renderer: Renderer? = engine?.createRenderer()
    private var scene: Scene? = engine?.createScene()
    private var view: View? = engine?.createView()
    private var camera: Camera? = engine?.createCamera(engine!!.entityManager.create())
    private var swapChain: SwapChain? = null

    private var vertexBuffer: VertexBuffer? = null
    private var indexBuffer: IndexBuffer? = null
    private var material: Material? = null
    private var pyramidEntity: Int = 0

    private var manipulator: Manipulator? = null

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
        Log.d("FilamentCamera", "[FilamentCamera] Camera Created")
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

        // Setup Manipulator
        manipulator = Manipulator.Builder()
            .targetPosition(0.0f, 0.5f, 0.0f)
            .orbitHomePosition(0.0f, 1.0f, 4.0f)
            .viewport(1080, 1920) // Initial guess, updated in onSurfaceChanged
            .build(Manipulator.Mode.ORBIT)
        
        setupGeometry(engine, scene)
        setupMaterialAsync(engine)
    }

    private fun setupGeometry(engine: Engine, scene: Scene) {
        // Pyramid vertices (pos x,y,z, color r,g,b)
        val vertexCount = 5
        val vertexSize = (3 + 3) * 4
        val vertexData = ByteBuffer.allocate(vertexCount * vertexSize)
            .order(ByteOrder.nativeOrder())

        // 0: Base Back-Left (Red)
        vertexData.putFloat(-0.5f); vertexData.putFloat(0.0f); vertexData.putFloat(-0.5f)
        vertexData.putFloat(1.0f); vertexData.putFloat(0.0f); vertexData.putFloat(0.0f)
        // 1: Base Back-Right (Green)
        vertexData.putFloat(0.5f); vertexData.putFloat(0.0f); vertexData.putFloat(-0.5f)
        vertexData.putFloat(0.0f); vertexData.putFloat(1.0f); vertexData.putFloat(0.0f)
        // 2: Base Front-Right (Blue)
        vertexData.putFloat(0.5f); vertexData.putFloat(0.0f); vertexData.putFloat(0.5f)
        vertexData.putFloat(0.0f); vertexData.putFloat(0.0f); vertexData.putFloat(1.0f)
        // 3: Base Front-Left (Yellow)
        vertexData.putFloat(-0.5f); vertexData.putFloat(0.0f); vertexData.putFloat(0.5f)
        vertexData.putFloat(1.0f); vertexData.putFloat(1.0f); vertexData.putFloat(0.0f)
        // 4: Apex (White)
        vertexData.putFloat(0.0f); vertexData.putFloat(1.0f); vertexData.putFloat(0.0f)
        vertexData.putFloat(1.0f); vertexData.putFloat(1.0f); vertexData.putFloat(1.0f)

        vertexData.flip()

        vertexBuffer = VertexBuffer.Builder()
            .bufferCount(1)
            .vertexCount(vertexCount)
            .attribute(VertexBuffer.VertexAttribute.POSITION, 0, VertexBuffer.AttributeType.FLOAT3, 0, vertexSize)
            .attribute(VertexBuffer.VertexAttribute.COLOR, 0, VertexBuffer.AttributeType.FLOAT3, 12, vertexSize)
            .build(engine)

        vertexBuffer?.setBufferAt(engine, 0, vertexData)

        // 6 triangles = 18 indices
        val indexData = ByteBuffer.allocate(18 * 2).order(ByteOrder.nativeOrder())
        // Base
        indexData.putShort(0); indexData.putShort(2); indexData.putShort(1)
        indexData.putShort(0); indexData.putShort(3); indexData.putShort(2)
        // Sides
        indexData.putShort(0); indexData.putShort(1); indexData.putShort(4)
        indexData.putShort(1); indexData.putShort(2); indexData.putShort(4)
        indexData.putShort(2); indexData.putShort(3); indexData.putShort(4)
        indexData.putShort(3); indexData.putShort(0); indexData.putShort(4)
        indexData.flip()

        indexBuffer = IndexBuffer.Builder()
            .indexCount(18)
            .bufferType(IndexBuffer.Builder.IndexType.USHORT)
            .build(engine)

        indexBuffer?.setBuffer(engine, indexData)

        pyramidEntity = EntityManager.get().create()
        RenderableManager.Builder(1)
            .boundingBox(Box(0.0f, 0.5f, 0.0f, 0.5f, 0.5f, 0.5f))
            .geometry(0, RenderableManager.PrimitiveType.TRIANGLES, vertexBuffer!!, indexBuffer!!, 0, 18)
            .culling(false)
            .build(engine, pyramidEntity)

        scene.addEntity(pyramidEntity)
    }

    private fun setupMaterialAsync(engine: Engine) {
        val builder = MaterialBuilder()
            .name("PyramidMaterial")
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

        val result = builder.build(engine)

        if (result.isValid) {
            val buffer = result.getBuffer()
            val materialInstance = Material.Builder()
                .payload(buffer, buffer.remaining())
                .build(engine)
            material = materialInstance

            val instance = materialInstance.defaultInstance
            val rm = engine.renderableManager
            rm.setMaterialInstanceAt(rm.getInstance(pyramidEntity), 0, instance)
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
        manipulator?.setViewport(width, height)
        
        val aspect = width.toDouble() / height.toDouble()
        camera.setProjection(45.0, aspect, 0.1, 100.0, Camera.Fov.VERTICAL)
        
        // In-place update of SwapChain if needed, though usually just creating new one is fine for size changes
        swapChain?.let { engine.destroySwapChain(it) }
        swapChain = engine.createSwapChain(holder.surface)
        
        // TODO: Retrofit HelloTriangleScreen with this in-place resize pattern
    }

    fun onSurfaceDestroyed() {
        val engine = engine ?: return
        swapChain?.let { engine.destroySwapChain(it) }
        swapChain = null
    }

    fun onGrabBegin(x: Float, y: Float) {
        manipulator?.grabBegin(x.toInt(), y.toInt(), false) // strafe = false
    }

    fun onGrabUpdate(x: Float, y: Float) {
        manipulator?.grabUpdate(x.toInt(), y.toInt())
    }

    fun onGrabEnd() {
        manipulator?.grabEnd()
    }

    fun onScroll(x: Float, y: Float, delta: Float) {
        manipulator?.scroll(x.toInt(), y.toInt(), delta)
    }

    private fun render(frameTimeNanos: Long) {
        val engine = engine ?: return
        val renderer = renderer ?: return
        val view = view ?: return
        val swapChain = swapChain ?: return
        val camera = camera ?: return
        val manipulator = manipulator ?: return

        // Update camera from manipulator
        val eye = FloatArray(3)
        val target = FloatArray(3)
        val upward = FloatArray(3)
        manipulator.getLookAt(eye, target, upward)
        camera.lookAt(
            eye[0].toDouble(), eye[1].toDouble(), eye[2].toDouble(),
            target[0].toDouble(), target[1].toDouble(), target[2].toDouble(),
            upward[0].toDouble(), upward[1].toDouble(), upward[2].toDouble()
        )

        if (renderer.beginFrame(swapChain, frameTimeNanos)) {
            renderer.render(view)
            renderer.endFrame()
        }
    }

    fun destroy() {
        val engine = engine ?: return
        Log.d("FilamentCamera", "[FilamentCamera] Camera Destroyed")
        
        stopFrameCallback()
        rendererScope.cancel()
        
        if (pyramidEntity != 0) {
            engine.destroyEntity(pyramidEntity)
            pyramidEntity = 0
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

        // Manipulator doesn't have a destroy() in the Java API, it's just a helper object
        manipulator = null
        
        engine.destroy()
        this.engine = null
    }
}
