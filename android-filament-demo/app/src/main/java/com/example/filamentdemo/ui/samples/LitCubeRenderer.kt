package com.example.filamentdemo.ui.samples

import android.util.Log
import android.view.Choreographer
import android.view.SurfaceHolder
import com.example.filamentdemo.ui.utils.OrbitGestureListener
import com.google.android.filament.*
import com.google.android.filament.View
import com.google.android.filament.filamat.MaterialBuilder
import com.google.android.filament.utils.Manipulator
import kotlinx.coroutines.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class LitCubeRenderer : OrbitGestureListener {
    private var engine: Engine? = Engine.create(Engine.Backend.OPENGL)
    private var renderer: Renderer? = engine?.createRenderer()
    private var scene: Scene? = engine?.createScene()
    private var view: View? = engine?.createView()
    private var camera: Camera? = engine?.createCamera(engine!!.entityManager.create())
    private var swapChain: SwapChain? = null

    private var vertexBuffer: VertexBuffer? = null
    private var indexBuffer: IndexBuffer? = null
    private var material: Material? = null
    private var cubeEntity: Int = 0
    private var lightEntity: Int = 0
    private var indirectLight: IndirectLight? = null

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
        Log.d("LitCubeRenderer", "[LitCubeRenderer] Renderer Created")
        val engine = engine!!
        val view = view!!
        val scene = scene!!
        val camera = camera!!
        val renderer = renderer!!

        view.scene = scene
        view.camera = camera
        
        renderer.clearOptions = Renderer.ClearOptions().apply {
            clear = true
            clearColor = doubleArrayOf(0.05, 0.05, 0.05, 1.0)
        }

        // Physical Camera Settings
        camera.setExposure(16.0f, 1.0f / 125.0f, 100.0f)

        // Setup Manipulator
        manipulator = Manipulator.Builder()
            .targetPosition(0.0f, 0.0f, 0.0f)
            .orbitHomePosition(0.0f, 0.0f, 4.0f)
            .viewport(1080, 1920)
            .build(Manipulator.Mode.ORBIT)
        
        setupGeometry(engine, scene)
        setupLighting(engine, scene)
        setupMaterialAsync(engine)
    }

    private fun setupGeometry(engine: Engine, scene: Scene) {
        val vertexCount = 24
        // Position(3) + Tangents(4) + Color(4) = 11 floats
        val vertexSize = 11 * 4
        val vertexData = ByteBuffer.allocateDirect(vertexCount * vertexSize)
            .order(ByteOrder.nativeOrder())

        // Cube vertices: 4 per face for flat shading
        val p = 0.5f
        val n = -0.5f

        // Front Face (+Z)
        putVertex(vertexData, n, n, p, 1f, 0f, 0f) // BL
        putVertex(vertexData, p, n, p, 1f, 0f, 0f) // BR
        putVertex(vertexData, p, p, p, 1f, 0f, 0f) // TR
        putVertex(vertexData, n, p, p, 1f, 0f, 0f) // TL
        // Back Face (-Z)
        putVertex(vertexData, n, n, n, 0f, 1f, 0f) 
        putVertex(vertexData, n, p, n, 0f, 1f, 0f)
        putVertex(vertexData, p, p, n, 0f, 1f, 0f)
        putVertex(vertexData, p, n, n, 0f, 1f, 0f)
        // Left Face (-X)
        putVertex(vertexData, n, n, n, 0f, 0f, 1f)
        putVertex(vertexData, n, n, p, 0f, 0f, 1f)
        putVertex(vertexData, n, p, p, 0f, 0f, 1f)
        putVertex(vertexData, n, p, n, 0f, 0f, 1f)
        // Right Face (+X)
        putVertex(vertexData, p, n, n, 1f, 1f, 0f)
        putVertex(vertexData, p, p, n, 1f, 1f, 0f)
        putVertex(vertexData, p, p, p, 1f, 1f, 0f)
        putVertex(vertexData, p, n, p, 1f, 1f, 0f)
        // Top Face (+Y)
        putVertex(vertexData, n, p, n, 0f, 1f, 1f)
        putVertex(vertexData, n, p, p, 0f, 1f, 1f)
        putVertex(vertexData, p, p, p, 0f, 1f, 1f)
        putVertex(vertexData, p, p, n, 0f, 1f, 1f)
        // Bottom Face (-Y)
        putVertex(vertexData, n, n, n, 1f, 0f, 1f)
        putVertex(vertexData, p, n, n, 1f, 0f, 1f)
        putVertex(vertexData, p, n, p, 1f, 0f, 1f)
        putVertex(vertexData, n, n, p, 1f, 0f, 1f)

        // Normals for SurfaceOrientation
        val normals = FloatArray(24 * 3)
        fun setNormal(face: Int, x: Float, y: Float, z: Float) {
            for (i in 0..3) {
                val idx = (face * 4 + i) * 3
                normals[idx] = x; normals[idx+1] = y; normals[idx+2] = z
            }
        }
        setNormal(0, 0f, 0f, 1f)  // Front
        setNormal(1, 0f, 0f, -1f) // Back
        setNormal(2, -1f, 0f, 0f) // Left
        setNormal(3, 1f, 0f, 0f)  // Right
        setNormal(4, 0f, 1f, 0f)  // Top
        setNormal(5, 0f, -1f, 0f) // Bottom

        val orientation = SurfaceOrientation.Builder()
            .vertexCount(vertexCount)
            .normals(FloatBuffer.wrap(normals))
            .build()
        
        val quaternions = FloatBuffer.allocate(vertexCount * 4)
        orientation.getQuatsAsFloat(quaternions)
        orientation.destroy()
        
        // Interleave tangents into vertexData
        vertexData.rewind()
        for (i in 0 until vertexCount) {
            vertexData.position(i * vertexSize + 12) // Skip Position
            vertexData.putFloat(quaternions.get(i * 4))
            vertexData.putFloat(quaternions.get(i * 4 + 1))
            vertexData.putFloat(quaternions.get(i * 4 + 2))
            vertexData.putFloat(quaternions.get(i * 4 + 3))
        }
        vertexData.rewind()

        vertexBuffer = VertexBuffer.Builder()
            .bufferCount(1)
            .vertexCount(vertexCount)
            .attribute(VertexBuffer.VertexAttribute.POSITION, 0, VertexBuffer.AttributeType.FLOAT3, 0, vertexSize)
            .attribute(VertexBuffer.VertexAttribute.TANGENTS, 0, VertexBuffer.AttributeType.FLOAT4, 12, vertexSize)
            .attribute(VertexBuffer.VertexAttribute.COLOR, 0, VertexBuffer.AttributeType.FLOAT4, 28, vertexSize)
            .build(engine)
        vertexBuffer?.setBufferAt(engine, 0, vertexData)

        val indexData = ByteBuffer.allocateDirect(36 * 2).order(ByteOrder.nativeOrder())
        for (face in 0 until 6) {
            val v = (face * 4).toShort()
            indexData.putShort(v); indexData.putShort((v + 1).toShort()); indexData.putShort((v + 2).toShort())
            indexData.putShort(v); indexData.putShort((v + 2).toShort()); indexData.putShort((v + 3).toShort())
        }
        indexData.flip()

        indexBuffer = IndexBuffer.Builder()
            .indexCount(36)
            .bufferType(IndexBuffer.Builder.IndexType.USHORT)
            .build(engine)
        indexBuffer?.setBuffer(engine, indexData)

        cubeEntity = EntityManager.get().create()
        RenderableManager.Builder(1)
            .boundingBox(Box(0.0f, 0.0f, 0.0f, 0.5f, 0.5f, 0.5f))
            .geometry(0, RenderableManager.PrimitiveType.TRIANGLES, vertexBuffer!!, indexBuffer!!, 0, 36)
            .build(engine, cubeEntity)

        scene.addEntity(cubeEntity)
        Log.d("LitCubeRenderer", "[LitCubeRenderer] Cube Geometry Created")
    }

    private fun putVertex(buffer: ByteBuffer, x: Float, y: Float, z: Float, r: Float, g: Float, b: Float) {
        buffer.putFloat(x); buffer.putFloat(y); buffer.putFloat(z)
        buffer.position(buffer.position() + 16) // Skip Tangents (FLOAT4)
        buffer.putFloat(r); buffer.putFloat(g); buffer.putFloat(b); buffer.putFloat(1.0f) // Color FLOAT4
    }

    private fun setupLighting(engine: Engine, scene: Scene) {
        // Directional Light (Sun)
        lightEntity = EntityManager.get().create()
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(1.0f, 1.0f, 1.0f)
            .intensity(100_000.0f)
            .direction(0.5f, -1.0f, -0.5f)
            .castShadows(true)
            .build(engine, lightEntity)
        scene.addEntity(lightEntity)

        // Indirect Light (Ambient Fill)
        // 1-band SH: DC term (index 0) only.
        // SH coefficients for 1-band is just [R, G, B] / sqrt(4 * PI)
        // But Filament's irradiance method takes 1 band as just the color.
        indirectLight = IndirectLight.Builder()
            .irradiance(1, floatArrayOf(0.5f, 0.5f, 0.5f)) 
            .intensity(20_000.0f)
            .build(engine)
        scene.indirectLight = indirectLight
        
        Log.d("LitCubeRenderer", "[LitCubeRenderer] Lighting Created")
    }

    private fun setupMaterialAsync(engine: Engine) {
        rendererScope.launch {
            val builder = MaterialBuilder()
                .name("LitCubeMaterial")
                .shading(MaterialBuilder.Shading.LIT)
                .require(MaterialBuilder.VertexAttribute.COLOR)
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

            val result = withContext(Dispatchers.IO) {
                builder.build(engine)
            }

            withContext(Dispatchers.Main) {
                val currentEngine = this@LitCubeRenderer.engine
                if (currentEngine != null && result.isValid && cubeEntity != 0) {
                    val buffer = result.getBuffer()
                    val mat = Material.Builder()
                        .payload(buffer, buffer.remaining())
                        .build(currentEngine)
                    material = mat

                    val rm = currentEngine.renderableManager
                    rm.setMaterialInstanceAt(rm.getInstance(cubeEntity), 0, mat.defaultInstance)
                    Log.d("LitCubeRenderer", "[LitCubeRenderer] Material Compiled and Applied")
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
        
        swapChain?.let { engine.destroySwapChain(it) }
        swapChain = engine.createSwapChain(holder.surface)
    }

    fun onSurfaceDestroyed() {
        val engine = engine ?: return
        swapChain?.let { engine.destroySwapChain(it) }
        swapChain = null
    }

    override fun onGrabBegin(x: Float, y: Float) {
        manipulator?.grabBegin(x.toInt(), y.toInt(), false)
    }

    override fun onGrabUpdate(x: Float, y: Float) {
        manipulator?.grabUpdate(x.toInt(), y.toInt())
    }

    override fun onGrabEnd() {
        manipulator?.grabEnd()
    }

    override fun onScroll(x: Float, y: Float, delta: Float) {
        manipulator?.scroll(x.toInt(), y.toInt(), delta)
    }

    private fun render(frameTimeNanos: Long) {
        val engine = engine ?: return
        val renderer = renderer ?: return
        val view = view ?: return
        val swapChain = swapChain ?: return
        val camera = camera ?: return
        val manipulator = manipulator ?: return

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
        Log.d("LitCubeRenderer", "[LitCubeRenderer] Renderer Destroyed")
        
        stopFrameCallback()
        rendererScope.cancel()
        
        if (cubeEntity != 0) {
            engine.destroyEntity(cubeEntity)
            cubeEntity = 0
        }
        
        if (lightEntity != 0) {
            engine.destroyEntity(lightEntity)
            lightEntity = 0
        }

        indirectLight?.let { engine.destroyIndirectLight(it) }
        indirectLight = null

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
