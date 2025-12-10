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

    companion object {
        private const val TAG = "ShoeRenderer"
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

    // Legacy single shoe support
    private var asset: FilamentAsset? = null
    private var rootEntity: Int = 0
    
    // Multi-shoe support (left and right)
    private var leftAsset: FilamentAsset? = null
    private var leftRootEntity: Int = 0
    private var rightAsset: FilamentAsset? = null
    private var rightRootEntity: Int = 0
    
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
                    
                    // Load shoes - use pair mode for left/right rendering
                    val modelPath = SelectedShoeManager.getSelectedModel()
                    loadShoe(modelPath, isLeft = true)
                    loadShoe(modelPath, isLeft = false)  // Same model, will be mirrored via transform
                    
                    Log.d(TAG, "Loaded shoe pair: $modelPath")
                    
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

    /**
     * Load a shoe model for multi-shoe comparison (left/right pair).
     * The same model can be loaded for both feet - the right shoe will be mirrored via transform.
     * 
     * @param assetPath The path to the GLB asset
     * @param isLeft true for left shoe, false for right shoe
     * @return true if loading succeeded
     */
    fun loadShoe(assetPath: String, isLeft: Boolean): Boolean {
        val eng = engine ?: return false
        val loader = assetLoader ?: return false
        val resLoader = resourceLoader ?: return false
        try {
            val buffer = readAssetToDirectBuffer(context.assets, assetPath)
            val label = if (isLeft) "LEFT" else "RIGHT"
            Log.d(TAG, "Loading $label shoe: $assetPath, buffer size: ${buffer.remaining()} bytes")
            
            val loaded = loader.createAsset(buffer)
            if (loaded == null) {
                Log.e(TAG, "AssetLoader.createAsset returned null for: $assetPath")
                return false
            }
            
            resLoader.loadResources(loaded)
            loaded.releaseSourceData()

            // Add all entities to the scene
            val entities: IntArray = loaded.entities
            for (e in entities) {
                scene?.addEntity(e)
            }
            
            // Store in appropriate slot
            if (isLeft) {
                leftAsset = loaded
                leftRootEntity = loaded.root
                // Also update legacy single-shoe references for backward compatibility
                asset = loaded
                rootEntity = loaded.root
            } else {
                rightAsset = loaded
                rightRootEntity = loaded.root
            }
            
            Log.i(TAG, "$label shoe loaded: entities=${entities.size}, root=${loaded.root}")
            return true
        } catch (t: Throwable) {
            Log.e(TAG, "Exception loading shoe: ${t.message}", t)
            return false
        }
    }

    /**
     * Legacy method: Load a single GLB from assets.
     * For backward compatibility - internally uses loadShoe for left shoe.
     */
    fun loadGlbFromAssets(assetPath: String): Boolean {
        return loadShoe(assetPath, isLeft = true)
    }

    /**
     * Sets the model matrix for the left shoe.
     * Thread-safe: Can be called from any thread, will execute on render thread.
     */
    fun setLeftModelMatrix(modelMatrix: FloatArray) {
        renderHandler?.post {
            val eng = engine ?: return@post
            if (leftRootEntity == 0) return@post
            val tm = eng.transformManager
            val inst = tm.getInstance(leftRootEntity)
            if (inst != 0) {
                tm.setTransform(inst, modelMatrix)
            }
        }
    }

    /**
     * Sets the model matrix for the right shoe.
     * The matrix should include mirroring (negative X scale) for proper foot orientation.
     * Thread-safe: Can be called from any thread, will execute on render thread.
     */
    fun setRightModelMatrix(modelMatrix: FloatArray) {
        renderHandler?.post {
            val eng = engine ?: return@post
            if (rightRootEntity == 0) return@post
            val tm = eng.transformManager
            val inst = tm.getInstance(rightRootEntity)
            if (inst != 0) {
                tm.setTransform(inst, modelMatrix)
            }
        }
    }

    /**
     * Sets the model matrix for the loaded asset's root entity (legacy single shoe).
     * For backward compatibility - now sets the left shoe matrix.
     * Thread-safe: Can be called from any thread, will execute on render thread.
     */
    fun setModelMatrix(modelMatrix: FloatArray) {
        setLeftModelMatrix(modelMatrix)
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
            
            // Helper function to clean up an asset
            fun cleanupAsset(a: FilamentAsset?) {
                a?.let { asset ->
                    val entities = asset.entities
                    for (e in entities) {
                        scene?.removeEntity(e)
                        eng.destroyEntity(e)
                    }
                    asset.releaseSourceData()
                }
            }
            
            // Clean up left shoe
            cleanupAsset(leftAsset)
            leftAsset = null
            leftRootEntity = 0
            
            // Clean up right shoe
            cleanupAsset(rightAsset)
            rightAsset = null
            rightRootEntity = 0
            
            // Legacy single shoe reference (may point to leftAsset)
            asset = null
            rootEntity = 0

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
