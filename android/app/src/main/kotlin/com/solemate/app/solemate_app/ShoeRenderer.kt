package com.solemate.app.solemate_app

import android.content.Context
import android.content.res.AssetManager
import android.graphics.SurfaceTexture
import android.os.Handler
import android.os.HandlerThread
import android.view.Choreographer
import com.google.android.filament.Viewport
import android.util.Log
import android.view.Surface
import android.view.TextureView
import com.google.android.filament.Camera
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.Material
import com.google.android.filament.MaterialInstance
import com.google.android.filament.RenderableManager
import com.google.android.filament.Renderer
import com.google.android.filament.Scene
import com.google.android.filament.SwapChain
import com.google.android.filament.Texture
import com.google.android.filament.TextureSampler
import com.google.android.filament.View
import com.google.android.filament.gltfio.AssetLoader
import com.google.android.filament.gltfio.FilamentAsset
import com.google.android.filament.gltfio.MaterialProvider
import com.google.android.filament.gltfio.ResourceLoader
import com.google.android.filament.LightManager
import com.google.android.filament.utils.Utils
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import com.google.android.filament.VertexBuffer
import com.google.android.filament.IndexBuffer
import com.google.android.filament.Box
import com.google.android.filament.RenderableManager.PrimitiveType

/**
 * Filament renderer for shoe models with ARCore depth-based occlusion support.
 * 
 * Occlusion is implemented by:
 * 1. Loading a custom depth occlusion material
 * 2. Passing ARCore depth texture and UV transform to the material
 * 3. Fragment shader compares object depth against scene depth
 * 
 * This renders into a TextureView overlay on top of the ARCore camera feed.
 */
class ShoeRenderer(private val context: Context) {

    companion object {
        private const val TAG = "ShoeRenderer"
        private const val DEPTH_MATERIAL_PATH = "materials/depth_occlusion.filamat"
    }

    private var engine: Engine? = null
    private var renderer: Renderer? = null
    private var scene: Scene? = null
    private var view: View? = null
    private var camera: Camera? = null
    private var cameraEntity: Int = 0
    private var swapChain: SwapChain? = null

    private var materialProvider: MaterialProvider? = null
    private var assetLoader: AssetLoader? = null
    private var resourceLoader: ResourceLoader? = null

    private var asset: FilamentAsset? = null
    private var rootEntity: Int = 0
    private var lightEntity: Int = 0
    private var viewportWidth = 0
    private var viewportHeight = 0
    private var modelScale: Float = 1.0f
    
    // Rendering thread for Filament
    private var renderThread: HandlerThread? = null
    private var renderHandler: Handler? = null
    private var choreographer: Choreographer? = null
    private var isRendering = false
    
    // === Depth Occlusion ===
    private var depthMaterial: Material? = null
    private var depthMaterialInstance: MaterialInstance? = null
    private var occlusionEnabled = true
    
    // Depth texture from ARCore (passed in from ObjectAttachedRenderer)
    private var depthTexture: Texture? = null
    private var depthUvTransform = FloatArray(9) { if (it == 0 || it == 4 || it == 8) 1f else 0f }
    private var hasDepthData = false
    
    // Sampler for depth texture - lazy init to ensure Filament is loaded first
    private val depthSampler: TextureSampler by lazy {
        TextureSampler(
            TextureSampler.MinFilter.NEAREST,
            TextureSampler.MagFilter.NEAREST,
            TextureSampler.WrapMode.CLAMP_TO_EDGE
        )
    }

    // === Depth Occluder Mesh ===
    private var occluderMaterial: Material? = null
    private var occluderMaterialInstance: MaterialInstance? = null
    private var occluderEntity: Int = 0
    private var occluderVertexBuffer: VertexBuffer? = null
    private var occluderIndexBuffer: IndexBuffer? = null
    private val occluderGridSize = 120 // 120x120 grid

    init {
        // Ensure Filament JNI is loaded
        try {
            Utils.init()
        } catch (_: Throwable) { }
    }

    /**
     * Attach this renderer to a TextureView for rendering.
     * The TextureView will be used to create the Filament SwapChain.
     */
    fun attachToTextureView(tv: TextureView) {
        tv.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                Log.d(TAG, "SurfaceTexture available: ${width}x${height}")
                
                // Configure SurfaceTexture for transparency
                surfaceTexture.setDefaultBufferSize(width, height)
                
                // Create rendering thread for Filament
                renderThread = HandlerThread("FilamentRenderThread").apply { start() }
                renderHandler = Handler(renderThread!!.looper)
                
                // Initialize Filament on its own thread
                renderHandler?.post {
                    init(width, height, Surface(surfaceTexture))
                    // Load model after surface is ready
                    loadGlbFromAssets("models/shoes/nike_journey_run_left.glb")
                    
                    // Start rendering loop
                    startRenderLoop()
                }
            }
            
            override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                Log.d(TAG, "SurfaceTexture size changed: ${width}x${height}")
                renderHandler?.post {
                    resize(width, height)
                }
            }
            
            override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
                Log.d(TAG, "SurfaceTexture destroyed")
                stopRenderLoop()
                renderHandler?.post {
                    release()
                    renderThread?.quitSafely()
                }
                return true
            }
            
            override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {
                // No-op
            }
        }
    }
    
    private fun startRenderLoop() {
        isRendering = true
        choreographer = Choreographer.getInstance()
        scheduleNextFrame()
    }
    
    private fun stopRenderLoop() {
        isRendering = false
        choreographer = null
    }
    
    private fun scheduleNextFrame() {
        if (!isRendering) return
        choreographer?.postFrameCallback { frameTimeNanos ->
            draw()
            scheduleNextFrame()
        }
    }

    fun init(width: Int, height: Int, surface: android.view.Surface?) {
        val eng = Engine.create()
        engine = eng

        renderer = eng.createRenderer()
        scene = eng.createScene()
        view = eng.createView()
        
        // Create camera entity using EntityManager
        val entityManager = EntityManager.get()
        cameraEntity = entityManager.create()
        camera = eng.createCamera(cameraEntity)

        // Attach scene and camera to the view
        view!!.scene = scene
        view!!.camera = camera
        
        viewportWidth = width
        viewportHeight = height
        view!!.viewport = Viewport(0, 0, width, height)

        // Create swap chain from the provided Surface with transparency flag
        swapChain = try {
            if (surface != null) {
                Log.d(TAG, "Creating SwapChain with transparency flag (0x1)")
                eng.createSwapChain(surface, 0x1)
            } else {
                Log.w(TAG, "No surface provided, cannot create SwapChain")
                null
            }
        } catch (t: Throwable) {
            Log.e(TAG, "SwapChain creation failed: ${t.message}", t)
            null
        }

        // Setup gltfio
        materialProvider = com.google.android.filament.gltfio.UbershaderProvider(eng)
        assetLoader = AssetLoader(eng, materialProvider!!, entityManager)
        resourceLoader = ResourceLoader(eng)

        // Load depth occlusion material (compiled with matc 1.65.2)
        loadDepthMaterial()
        
        // Setup depth occluder (invisible mesh that writes to depth)
        setupOccluder()

        // Basic directional light so PBR materials are visible
        try {
            lightEntity = EntityManager.get().create()
            LightManager.Builder(LightManager.Type.DIRECTIONAL)
                .color(1.0f, 1.0f, 1.0f)
                .intensity(50000.0f)
                .direction(0.0f, -1.0f, -0.3f)
                .castShadows(false)
                .build(eng, lightEntity)
            scene?.addEntity(lightEntity)
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to create light: ${t.message}")
        }
        
        Log.i(TAG, "ShoeRenderer initialized with depth occlusion support")
    }
    
    /**
     * Load the depth occlusion material from assets.
     */
    private fun loadDepthMaterial() {
        val eng = engine ?: return
        try {
            val buffer = readAssetToDirectBuffer(context.assets, DEPTH_MATERIAL_PATH)
            depthMaterial = Material.Builder()
                .payload(buffer, buffer.remaining())
                .build(eng)
            
            depthMaterialInstance = depthMaterial?.createInstance()
            
            // Set default parameter values
            depthMaterialInstance?.let { mi ->
                mi.setParameter("occlusionEnabled", if (occlusionEnabled) 1.0f else 0.0f)
                mi.setParameter("baseColor", 1.0f, 1.0f, 1.0f, 1.0f)
                mi.setParameter("metallic", 0.0f)
                mi.setParameter("roughness", 0.5f)
                
                // Set identity transform initially
                mi.setParameter("depthUvTransformCol0", 1f, 0f, 0f)
                mi.setParameter("depthUvTransformCol1", 0f, 1f, 0f)
                mi.setParameter("depthUvTransformCol2", 0f, 0f, 1f)
            }
            
            Log.i(TAG, "Depth occlusion material loaded successfully")
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to load depth material: ${t.message}", t)
            // Continue without occlusion - model will still render
        }
    }

    fun resize(width: Int, height: Int) {
        viewportWidth = width
        viewportHeight = height
        view?.viewport = Viewport(0, 0, width, height)
    }

    fun loadGlbFromAssets(assetPath: String): Boolean {
        val eng = engine ?: return false
        val loader = assetLoader ?: return false
        val resLoader = resourceLoader ?: return false
        try {
            val buffer = readAssetToDirectBuffer(context.assets, assetPath)
            Log.d(TAG, "Loading GLB: $assetPath, buffer size: ${buffer.remaining()} bytes")
            
            val loaded = loader.createAsset(buffer)
            if (loaded == null) {
                Log.e(TAG, "AssetLoader.createAsset returned null for: $assetPath")
                return false
            }
            
            asset = loaded
            resLoader.loadResources(loaded)
            loaded.releaseSourceData()

            // Add all entities to the scene
            val entities: IntArray = loaded.entities
            for (e in entities) {
                scene?.addEntity(e)
            }
            rootEntity = loaded.root
            
            // NOTE: Material replacement disabled - it removes original GLB textures
            // Depth occlusion with gltfio materials requires UberShader modification or
            // View-level depth testing. For now, shoe renders with original materials.
            // applyDepthMaterialToAsset(loaded)
            Log.i(TAG, "Using original GLB materials (depth occlusion via material disabled)")
            
            Log.i(TAG, "GLB loaded: $assetPath, entities=${entities.size}, root=$rootEntity")
            return true
        } catch (t: Throwable) {
            Log.e(TAG, "Exception loading GLB: ${t.message}", t)
            return false
        }
    }
    
    /**
     * Apply the depth occlusion material to all renderable entities in the asset.
     * This replaces the default UberShader materials with our occlusion-aware material.
     */
    private fun applyDepthMaterialToAsset(filamentAsset: FilamentAsset) {
        val eng = engine ?: return
        val mi = depthMaterialInstance ?: run {
            Log.w(TAG, "No depth material instance, occlusion disabled")
            return
        }
        
        val rm = eng.renderableManager
        
        // Iterate through all entities and replace their materials
        for (entity in filamentAsset.entities) {
            val inst = rm.getInstance(entity)
            if (inst == 0) continue
            
            val primitiveCount = rm.getPrimitiveCount(inst)
            for (primitiveIndex in 0 until primitiveCount) {
                // Get the current material to extract base color if available
                try {
                    // Replace with our depth-aware material
                    rm.setMaterialInstanceAt(inst, primitiveIndex, mi)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to set material on primitive $primitiveIndex: ${e.message}")
                }
            }
        }
        
        Log.i(TAG, "Applied depth occlusion material to asset")
    }

    /**
     * Sets the model matrix for the loaded asset's root entity.
     * Thread-safe: Can be called from any thread, will execute on render thread.
     */
    fun setModelMatrix(modelMatrix: FloatArray) {
        renderHandler?.post {
            val eng = engine ?: return@post
            if (rootEntity == 0) return@post
            val tm = eng.transformManager
            val inst = tm.getInstance(rootEntity)
            if (inst != 0) {
                tm.setTransform(inst, modelMatrix)
            }
        }
    }

    fun setModelScale(scale: Float) {
        modelScale = scale
    }

    /**
     * Sets camera projection and pose from ARCore matrices.
     * Thread-safe: Can be called from any thread, will execute on render thread.
     */
    fun setCamera(viewMatrix: FloatArray, projMatrix: FloatArray) {
        renderHandler?.post {
            val cam = camera ?: return@post
            
            val projDouble = DoubleArray(16)
            for (i in projMatrix.indices) {
                projDouble[i] = projMatrix[i].toDouble()
            }
            
            cam.setCustomProjection(projDouble, 0.1, 100.0)

            val camModel = FloatArray(16)
            android.opengl.Matrix.invertM(camModel, 0, viewMatrix, 0)
            cam.setModelMatrix(camModel)
        }
    }
    
    /**
     * Update the depth texture and UV transform for occlusion.
     * Called from ObjectAttachedRenderer when depth data is available.
     * 
     * @param texture The Filament depth texture (or null if unavailable)
     * @param uvTransform 9-element array representing 3x3 UV transform matrix (column-major)
     */
    fun setDepthData(texture: Texture?, uvTransform: FloatArray) {
        renderHandler?.post {
            depthTexture = texture
            if (uvTransform.size == 9) {
                System.arraycopy(uvTransform, 0, depthUvTransform, 0, 9)
            }
            hasDepthData = texture != null
            
            // Update material parameters
            updateDepthMaterialParams()
        }
    }
    
    /**
     * Enable or disable depth-based occlusion.
     */
    fun enableOcclusion(enable: Boolean) {
        occlusionEnabled = enable
        renderHandler?.post {
            depthMaterialInstance?.setParameter("occlusionEnabled", if (enable) 1.0f else 0.0f)
        }
        Log.i(TAG, "Occlusion ${if (enable) "enabled" else "disabled"}")
    }
    
    /**
     * Update the material instance with current depth data.
     */
    private fun updateDepthMaterialParams() {
        val mi = depthMaterialInstance ?: return
        // Update depth occlusion parameters
        if (hasDepthData && depthTexture != null && occlusionEnabled) {
            val eng = engine ?: return
            
            // Update occluder transform to match camera (so mesh is always in front of view)
            if (cameraEntity != 0 && occluderEntity != 0) {
                val tm = eng.transformManager
                val camInst = tm.getInstance(cameraEntity)
                val occInst = tm.getInstance(occluderEntity)
                
                if (camInst != 0 && occInst != 0) {
                    // Copy camera transform to occluder
                    val transform = FloatArray(16)
                    tm.getWorldTransform(camInst, transform)
                    tm.setTransform(occInst, transform)
                }
            }

            // Update occluder material
            occluderMaterialInstance?.let { mi ->
                mi.setParameter("depthTexture", depthTexture!!, depthSampler)
                
                // Pass camera position (world space) to shader via viewOrigin parameter
                if (camera != null) {
                    val camPos = floatArrayOf(0f, 0f, 0f)
                    // getPosition returns a float array
                    val posArray = camera!!.getPosition(camPos)
                    mi.setParameter("viewOrigin", posArray[0], posArray[1], posArray[2])
                }
            }

            // Set depth texture for the main material
            mi.setParameter("depthTexture", depthTexture!!, depthSampler)
            
            // Set UV transform matrix columns
            mi.setParameter("depthUvTransformCol0", 
                depthUvTransform[0], depthUvTransform[1], depthUvTransform[2])
            mi.setParameter("depthUvTransformCol1", 
                depthUvTransform[3], depthUvTransform[4], depthUvTransform[5])
            mi.setParameter("depthUvTransformCol2", 
                depthUvTransform[6], depthUvTransform[7], depthUvTransform[8])
            
            mi.setParameter("occlusionEnabled", 1.0f)
        } else {
            mi.setParameter("occlusionEnabled", 0.0f)
        }
    }
    
    /**
     * Get the Filament engine for external use (e.g., DepthTextureHandler).
     */
    fun getEngine(): Engine? = engine
    
    /**
     * Check if depth occlusion is currently active.
     */
    fun isOcclusionActive(): Boolean = hasDepthData && occlusionEnabled

    fun draw(): Boolean {
        val eng = engine ?: return false
        val rnd = renderer ?: return false
        val scnView = view ?: return false
        val sc = swapChain ?: run {
            Log.w(TAG, "No SwapChain available")
            return false
        }
        
        return try {
            val frameTimeNanos = System.nanoTime()
            if (rnd.beginFrame(sc, frameTimeNanos)) {
                rnd.render(scnView)
                rnd.endFrame()
                // Reduce log noise - only log occasionally
                // Log.d(TAG, "draw() ok")
                true
            } else {
                false
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Render failed: ${t.message}", t)
            return false
        }
    }
    
    /**
     * Setup the invisible occluder mesh.
     * This creates a dense grid attached to the camera that is displaced by depth data.
     */
    private fun setupOccluder() {
        val eng = engine ?: return
        val entityManager = EntityManager.get()
        
        try {
            // 1. Create Occluder Material
            val buffer = readAssetToDirectBuffer(context.assets, "materials/depth_displacement.filamat")
            occluderMaterial = Material.Builder()
                .payload(buffer, buffer.remaining())
                .build(eng)
            occluderMaterialInstance = occluderMaterial?.createInstance()
            
            // 2. Generate Grid Mesh
            // Grid spanning -2 to +2 in X/Y at Z = -1.0 (covering wide FOV)
            val N = occluderGridSize
            val vertexCount = (N + 1) * (N + 1)
            val indexCount = N * N * 6
            
            // Buffers
            val vertexData = FloatBuffer.allocate(vertexCount * 5) // Position(3) + UV(2)
            val indexData = ByteBuffer.allocateDirect(indexCount * 4).order(ByteOrder.nativeOrder()).asIntBuffer()
            
            val extent = 2.0f
            val step = (extent * 2.0f) / N
            val uvStep = 1.0f / N
            
            // Generate vertices
            for (y in 0..N) {
                for (x in 0..N) {
                    val posX = -extent + x * step
                    val posY = -extent + y * step
                    val posZ = -1.0f // 1 meter forward in view space
                    
                    val u = x * uvStep
                    val v = y * uvStep
                    
                    vertexData.put(posX).put(posY).put(posZ)
                    vertexData.put(u).put(v)
                }
            }
            vertexData.flip()
            
            // Generate indices (quads -> triangles)
            for (y in 0 until N) {
                for (x in 0 until N) {
                    val i0 = y * (N + 1) + x
                    val i1 = i0 + 1
                    val i2 = i0 + (N + 1)
                    val i3 = i2 + 1
                    
                    // Triangle 1
                    indexData.put(i0).put(i2).put(i1)
                    // Triangle 2
                    indexData.put(i1).put(i2).put(i3)
                }
            }
            indexData.flip()
            
            // 3. Create Filament Buffers
            occluderVertexBuffer = VertexBuffer.Builder()
                .bufferCount(1)
                .vertexCount(vertexCount)
                .attribute(VertexBuffer.VertexAttribute.POSITION, 0, VertexBuffer.AttributeType.FLOAT3, 0, 20) // stride 20 bytes (5 floats)
                .attribute(VertexBuffer.VertexAttribute.UV0, 0, VertexBuffer.AttributeType.FLOAT2, 12, 20) // offset 12 bytes
                .build(eng)
            
            occluderIndexBuffer = IndexBuffer.Builder()
                .indexCount(indexCount)
                .bufferType(IndexBuffer.Builder.IndexType.UINT)
                .build(eng)
                
            occluderVertexBuffer!!.setBufferAt(eng, 0, java.nio.ByteBuffer.allocateDirect(vertexData.capacity() * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(vertexData))
            occluderIndexBuffer!!.setBuffer(eng, java.nio.ByteBuffer.allocateDirect(indexCount * 4).order(ByteOrder.nativeOrder()).asIntBuffer().put(indexData))
            
            // 4. Create Entity and Renderable
            occluderEntity = entityManager.create()
            RenderableManager.Builder(1)
                .boundingBox(Box(-extent, -extent, -1.0f, extent, extent, 0.0f))
                .geometry(0, PrimitiveType.TRIANGLES, occluderVertexBuffer!!, occluderIndexBuffer!!)
                .material(0, occluderMaterialInstance!!)
                .culling(false) // Never cull
                .castShadows(false)
                .receiveShadows(false)
                .build(eng, occluderEntity)
                
            // 5. Attach to Camera so it stays locked to view
            // Note: Camera entity is 'cameraEntity'. Occluder should be its child?
            // Filament doesn't support scene graph hierarchy directly via EntityManager parent/child
            // without TransformManager.
            // Better: Just update its transform every frame to match camera * offset?
            // Or simpler: Attach it to the scene, and let the Vertex Shader logic handle the "View Space" assumption
            // by passing the Camera Position.
            // BUT, our Vertex Shader assumes input vertices are in World Space relative to Camera?
            // NO, proper way:
            // Use TransformManager to parent occluderEntity to cameraEntity?
            // Or just set occluderEntity transform to Identity and attach to Camera?
            // Filament Camera is an entity.
            // Let's rely on manually setting the transform to match the camera.
            
            scene?.addEntity(occluderEntity)
            Log.i(TAG, "Occluder mesh initialized")
            
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to setup occluder: ${t.message}", t)
        }
    }

    fun release() {
        try {
            val eng = engine ?: return
            
            // Release depth occlusion resources
            depthMaterialInstance?.let { 
                eng.destroyMaterialInstance(it)
            }
            depthMaterialInstance = null
            
            depthMaterial?.let {
                eng.destroyMaterial(it)
            }
            depthMaterial = null
            
            depthMaterial = null
            
            // Release occluder resources
            occluderMaterialInstance?.let { eng.destroyMaterialInstance(it) }
            occluderMaterialInstance = null
            occluderMaterial?.let { eng.destroyMaterial(it) }
            occluderMaterial = null
            occluderVertexBuffer?.let { eng.destroyVertexBuffer(it) }
            occluderVertexBuffer = null
            occluderIndexBuffer?.let { eng.destroyIndexBuffer(it) }
            occluderIndexBuffer = null
            if (occluderEntity != 0) {
                eng.destroyEntity(occluderEntity)
                occluderEntity = 0
            }
            
            // Note: depthTexture is owned by DepthTextureHandler, not destroyed here
            depthTexture = null
            
            asset?.let { a ->
                val entities = a.entities
                for (e in entities) {
                    scene?.removeEntity(e)
                    eng.destroyEntity(e)
                }
                a.releaseSourceData()
            }
            asset = null

            view?.let { eng.destroyView(it) }
            if (cameraEntity != 0) {
                eng.destroyEntity(cameraEntity)
                cameraEntity = 0
            }
            if (lightEntity != 0) {
                eng.destroyEntity(lightEntity)
                lightEntity = 0
            }
            scene?.let { eng.destroyScene(it) }
            renderer?.let { eng.destroyRenderer(it) }
            swapChain?.let { eng.destroySwapChain(it) }

            engine = null
            Log.i(TAG, "ShoeRenderer released")
        } catch (t: Throwable) {
            Log.e(TAG, "Release error: ${t.message}", t)
        }
    }

    private fun readAssetToDirectBuffer(am: AssetManager, path: String): ByteBuffer {
        val input: InputStream = am.open(path, AssetManager.ACCESS_STREAMING)
        val bytes = input.readBytes()
        input.close()
        val buffer = ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder())
        buffer.put(bytes)
        buffer.flip()
        return buffer
    }
}
