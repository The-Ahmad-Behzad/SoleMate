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
    
    // Model matrix storage for occluder positioning
    private var lastModelMatrix: FloatArray? = null

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
        // Store for leg occluder positioning
        if (lastModelMatrix == null) {
            lastModelMatrix = FloatArray(16)
        }
        System.arraycopy(modelMatrix, 0, lastModelMatrix!!, 0, 16)
        
        renderHandler?.post {
            val eng = engine ?: return@post
            if (rootEntity == 0) return@post
            val tm = eng.transformManager
            val inst = tm.getInstance(rootEntity)
            if (inst != 0) {
                tm.setTransform(inst, modelMatrix)
            }
            
            // Also update leg occluder position (if enabled)
            updateDepthMaterialParams()
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
        val eng = engine ?: return
        
        // Update leg occluder transform to follow shoe position + upward offset
        // NOTE: The shoe transform includes SHOE_SCALE (0.08), so we need to compensate
        if (occluderEntity != 0 && lastModelMatrix != null) {
            val tm = eng.transformManager
            val occInst = tm.getInstance(occluderEntity)
            
            if (occInst != 0) {
                // Create a new transform for the leg occluder
                // Extract just the translation from shoe transform (ignore scale/rotation)
                val legTransform = FloatArray(16)
                android.opengl.Matrix.setIdentityM(legTransform, 0)
                
                // Copy translation (position) from shoe
                legTransform[12] = lastModelMatrix!![12] // X position
                legTransform[13] = lastModelMatrix!![13] + 0.15f  // Y position + 15cm up
                legTransform[14] = lastModelMatrix!![14] + 0.20f // Z position + 20cm backward (toward leg, away from camera)
                
                // Apply larger scale for the leg box (1.0 = real size, not shoe-scaled)
                android.opengl.Matrix.scaleM(legTransform, 0, 1.0f, 1.0f, 1.0f)
                
                tm.setTransform(occInst, legTransform)
            }
        }
        
        // Update depth occlusion parameters (only if depth data available)
        if (hasDepthData && depthTexture != null && occlusionEnabled) {

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
     * Setup a simple leg occluder box.
     * This creates an invisible box positioned above the shoe to represent the leg,
     * causing the shoe heel to be occluded behind it.
     */
    private fun setupOccluder() {
        val eng = engine ?: return
        val entityManager = EntityManager.get()
        
        try {
            // 1. Create Simple Occluder Material (depth-only, invisible)
            val buffer = readAssetToDirectBuffer(context.assets, "materials/leg_occluder.filamat")
            occluderMaterial = Material.Builder()
                .payload(buffer, buffer.remaining())
                .build(eng)
            occluderMaterialInstance = occluderMaterial?.createInstance()
            
            // 2. Generate Simple Box Mesh (representing leg cross-section)
            // Box dimensions: width 0.1m, height 0.3m, depth 0.15m (leg-like)
            val halfW = 0.05f   // 10cm wide
            val halfH = 0.15f  // 30cm tall
            val halfD = 0.075f // 15cm deep
            
            // 8 vertices for a box
            val vertices = floatArrayOf(
                // Front face (Z+)
                -halfW, -halfH, halfD,   halfW, -halfH, halfD,   halfW, halfH, halfD,   -halfW, halfH, halfD,
                // Back face (Z-)
                -halfW, -halfH, -halfD,  -halfW, halfH, -halfD,  halfW, halfH, -halfD,  halfW, -halfH, -halfD
            )
            
            // 12 triangles (36 indices) for a box
            val indices = intArrayOf(
                // Front
                0, 1, 2, 2, 3, 0,
                // Back
                4, 5, 6, 6, 7, 4,
                // Left
                0, 3, 5, 5, 4, 0,
                // Right
                1, 7, 6, 6, 2, 1,
                // Top
                3, 2, 6, 6, 5, 3,
                // Bottom
                0, 4, 7, 7, 1, 0
            )
            
            // 3. Create Filament Buffers
            occluderVertexBuffer = VertexBuffer.Builder()
                .bufferCount(1)
                .vertexCount(8)
                .attribute(VertexBuffer.VertexAttribute.POSITION, 0, VertexBuffer.AttributeType.FLOAT3, 0, 12)
                .build(eng)
            
            occluderIndexBuffer = IndexBuffer.Builder()
                .indexCount(36)
                .bufferType(IndexBuffer.Builder.IndexType.UINT)
                .build(eng)
                
            // Copy vertex data
            val vByteBuf = ByteBuffer.allocateDirect(vertices.size * 4).order(ByteOrder.nativeOrder())
            vByteBuf.asFloatBuffer().put(vertices)
            vByteBuf.rewind()
            occluderVertexBuffer!!.setBufferAt(eng, 0, vByteBuf)
            
            // Copy index data
            val iByteBuf = ByteBuffer.allocateDirect(indices.size * 4).order(ByteOrder.nativeOrder())
            iByteBuf.asIntBuffer().put(indices)
            iByteBuf.rewind()
            occluderIndexBuffer!!.setBuffer(eng, iByteBuf)
            
            // 4. Create Entity and Renderable
            occluderEntity = entityManager.create()
            RenderableManager.Builder(1)
                .boundingBox(Box(-halfW, -halfH, -halfD, halfW, halfH, halfD))
                .geometry(0, PrimitiveType.TRIANGLES, occluderVertexBuffer!!, occluderIndexBuffer!!)
                .material(0, occluderMaterialInstance!!)
                .culling(false)  // Never cull
                .castShadows(false)
                .receiveShadows(false)
                .priority(0)  // Render FIRST (before shoe at priority 4)
                .build(eng, occluderEntity)
            
            // 5. Add to scene (DISABLED - focusing on shoe alignment first)
            // scene?.addEntity(occluderEntity)
            Log.i(TAG, "Leg occluder box initialized (DISABLED)")
            
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to setup leg occluder: ${t.message}", t)
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
