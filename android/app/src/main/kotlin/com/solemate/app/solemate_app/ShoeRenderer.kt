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
import com.google.android.filament.Renderer
import com.google.android.filament.Scene
import com.google.android.filament.SwapChain
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

/**
 * Minimal Filament wrapper to load and render a GLB model on top of ARCore content.
 * This renders into the current GL context managed by GLSurfaceView.
 */
class ShoeRenderer(private val context: Context) {

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
                Log.d("ShoeRenderer", "SurfaceTexture available: ${width}x${height}")
                
                // ✅ Configure SurfaceTexture for transparency
                surfaceTexture.setDefaultBufferSize(width, height)
                
                // Create rendering thread for Filament
                renderThread = HandlerThread("FilamentRenderThread").apply { start() }
                renderHandler = Handler(renderThread!!.looper)
                
                // Initialize Filament on its own thread
                renderHandler?.post {
                    init(width, height, Surface(surfaceTexture))
                    // Load model after surface is ready - use dynamically selected model
                    loadGlbFromAssets(SelectedShoeManager.getSelectedModel())
                    // Don't set model scale here - it will be part of the model matrix from SimpleRenderer
                    
                    // Start rendering loop
                    startRenderLoop()
                }
            }
            
            override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                Log.d("ShoeRenderer", "SurfaceTexture size changed: ${width}x${height}")
                renderHandler?.post {
                    resize(width, height)
                }
            }
            
            override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
                Log.d("ShoeRenderer", "SurfaceTexture destroyed")
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
        // Viewport is a Java class - use positional parameters
        view!!.viewport = Viewport(0, 0, width, height)

        // Create swap chain from the provided Surface with transparency flag
        // SwapChain.CONFIG_TRANSPARENT = 0x1 (enables alpha channel for transparency)
        swapChain = try {
            if (surface != null) {
                Log.d("ShoeRenderer", "Creating SwapChain with transparency flag (0x1)")
                eng.createSwapChain(surface, 0x1)
            } else {
                Log.w("ShoeRenderer", "No surface provided, cannot create SwapChain")
                null
            }
        } catch (t: Throwable) {
            Log.e("ShoeRenderer", "SwapChain creation failed: ${t.message}", t)
            null
        }

        // Setup gltfio - EntityManager is required (reuse the one we got for camera)
        materialProvider = com.google.android.filament.gltfio.UbershaderProvider(eng)
        assetLoader = AssetLoader(eng, materialProvider!!, entityManager)
        // ResourceLoader
        resourceLoader = ResourceLoader(eng)

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
            Log.w("ShoeRenderer", "Failed to create light: ${t.message}")
        }
    }

    fun resize(width: Int, height: Int) {
        viewportWidth = width
        viewportHeight = height
        // Viewport is a Java class - use positional parameters
        view?.viewport = Viewport(0, 0, width, height)
    }

    fun loadGlbFromAssets(assetPath: String): Boolean {
        val eng = engine ?: return false
        val loader = assetLoader ?: return false
        val resLoader = resourceLoader ?: return false
        try {
            val buffer = readAssetToDirectBuffer(context.assets, assetPath)
            Log.d("ShoeRenderer", "Loading GLB: $assetPath, buffer size: ${buffer.remaining()} bytes")
            
            // Direct API call (Filament 1.65.2 uses createAsset, not createAssetFromBinary)
            val loaded = loader.createAsset(buffer)
            if (loaded == null) {
                Log.e("ShoeRenderer", "AssetLoader.createAsset returned null for: $assetPath")
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
            Log.i("ShoeRenderer", "GLB loaded: $assetPath, entities=${entities.size}, root=$rootEntity")
            return true
        } catch (t: Throwable) {
            Log.e("ShoeRenderer", "Exception loading GLB: ${t.message}", t)
            return false
        }
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
                // Use the model matrix as-is (scale is already baked in from SimpleRenderer)
                tm.setTransform(inst, modelMatrix)
            }
        }
    }

    fun setModelScale(scale: Float) {
        modelScale = scale
    }

    /**
     * Sets camera projection and pose from ARCore matrices.
     * ARCore provides view (camera) matrix; we need the world transform (inverse of view).
     * Thread-safe: Can be called from any thread, will execute on render thread.
     */
    fun setCamera(viewMatrix: FloatArray, projMatrix: FloatArray) {
        renderHandler?.post {
            val cam = camera ?: return@post
            
            // Convert FloatArray to DoubleArray for Filament
            val projDouble = DoubleArray(16)
            for (i in projMatrix.indices) {
                projDouble[i] = projMatrix[i].toDouble()
            }
            
            // Set custom projection directly from ARCore
            cam.setCustomProjection(projDouble, 0.1, 100.0)

            // Invert view to get camera model matrix
            val camModel = FloatArray(16)
            android.opengl.Matrix.invertM(camModel, 0, viewMatrix, 0)
            cam.setModelMatrix(camModel)
        }
    }

    fun draw(): Boolean {
        val eng = engine ?: return false
        val rnd = renderer ?: return false
        val scnView = view ?: return false
        val sc = swapChain ?: run {
            Log.w("ShoeRenderer", "No SwapChain available")
            return false
        }
        
        return try {
            val frameTimeNanos = System.nanoTime()
            if (rnd.beginFrame(sc, frameTimeNanos)) {
                rnd.render(scnView)
                rnd.endFrame()
                Log.d("ShoeDraw", "draw() ok")
                true
            } else {
                Log.w("ShoeDraw", "draw() returned false")
                false  // beginFrame returned false - normal for some frames
            }
        } catch (t: Throwable) {
            Log.e("ShoeRenderer", "Render failed: ${t.message}", t)
            false
        }
    }

    fun release() {
        try {
            val eng = engine ?: return
            asset?.let { a ->
                // Remove from scene and destroy entities
                val entities = a.entities
                for (e in entities) {
                    scene?.removeEntity(e)
                    eng.destroyEntity(e)
                }
                a.releaseSourceData()
            }
            asset = null

            // Correct cleanup methods
            assetLoader?.let { 
                // AssetLoader doesn't need explicit cleanup - it's auto-managed by gltfio
            }
            // Leave materialProvider / resourceLoader to GC; Filament Java APIs are limited

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

            // Engine doesn't have a destroy method - it will be GC'd when engine is nulled
            engine = null
        } catch (t: Throwable) {
            Log.e("ShoeRenderer", "Release error: ${t.message}", t)
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
