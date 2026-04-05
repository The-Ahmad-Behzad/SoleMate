// Shoe Controller.js
// Version: 1.0.0
// Event: On Awake
// Description: Handles switching between visualization and try-on modes, managing
//              foot-detection hints, and swapping the active shoe model.

// ===================== Inspector Inputs =====================
// Array of shoe SceneObjects — each element is one shoe variant the user can try on.
//@input SceneObject[] shoeSceneObjects

// The hint object shown to the user when no foot is detected yet (e.g. "Point camera at feet").
//@input SceneObject footHint

// Enable/disable the 3D visualization (360° spin) mode.
//@input bool enableVisualization = true

// Root SceneObject that owns the ShoeVisualization script. Only visible when enableVisualization is true.
//@input SceneObject visualizationRoot {"showIf": "enableVisualization"}

// Hint shown when the back camera is active and visualization is not available.
//@input SceneObject cameraHint {"showIf": "enableVisualization"}

// ===================== Runtime State =====================

// True while the front/back camera is in try-on mode (AR overlay on feet).
var isTryOn = true;

// UpdateEvent used to poll whether the active foot tracker has acquired a lock.
var updateEvent = script.createEvent("UpdateEvent");

// Tracks whether the foot hint has already been shown and dismissed once.
// Prevents it from flickering on/off every frame.
var hintShown = false;

// Index of the shoe that was last activated via setShoe().
// Used so camera-switch events can restore the correct shoe.
var lastIndex = 0;

// Reference to the ShoeVisualization script component (populated during initialize()).
var visualizationScript;

// Reference to the ScriptComponent of the currently active shoe.
// Polled each frame to check whether the foot tracker is locked.
var activeTracker = null;

// ===================== Foot Hint Polling =====================

// Each frame, check whether the active shoe's tracker has found a foot.
// Once it has, hide the hint permanently for this session and stop polling.
updateEvent.bind(function () {
    if (!isNull(activeTracker) && activeTracker.isTracking()) {
        // Foot detected — hide the hint.
        if (script.footHint != null) {
            script.footHint.enabled = false;
        }
        hintShown = true;          // Don't show it again.
        updateEvent.enabled = false; // Stop polling to save performance.
    }
});
// Start with polling disabled; it is re-enabled in setHint() when needed.
updateEvent.enabled = false;

// ===================== Public API =====================

/**
 * Activates the shoe at `index` and deactivates all others.
 * In try-on mode the 3D model is enabled on the feet;
 * in visualization mode the spinning preview is shown instead.
 *
 * @param {number} index - Zero-based index into shoeSceneObjects.
 */
script.setShoe = function (index) {
    // Disable every shoe so we start from a clean slate.
    script.shoeSceneObjects.forEach(function (so) {
        so.enabled = false;
    });

    // Enable only the chosen shoe (only when in try-on mode;
    // visualization mode manages its own visibility separately).
    script.shoeSceneObjects[index].enabled = isTryOn;

    // Grab the shoe's script component so we can poll isTracking() in the UpdateEvent.
    activeTracker = script.shoeSceneObjects[index].getFirstComponent("Component.ScriptComponent");

    if (script.enableVisualization) {
        // Show/hide the visualization root depending on the current mode.
        script.visualizationRoot.enabled = !isTryOn;
        // Tell the visualization script which model to spin.
        visualizationScript.setActive(index);
    }

    // Remember this index so camera events can restore it.
    lastIndex = index;
};

// ===================== Hint Logic =====================

/**
 * Shows or hides the foot-detection hint based on the current mode.
 * Called every time the camera mode changes.
 */
function setHint() {
    if (isTryOn) {
        // In try-on mode, show the hint only if the foot hasn't been
        // detected yet in this session.
        if (!hintShown) {
            updateEvent.enabled = true; // Start polling for foot detection.
            if (script.footHint != null) {
                script.footHint.enabled = true;
            }
        }
    } else {
        // In visualization mode there's no foot to detect — hide the hint.
        updateEvent.enabled = false;
        if (script.footHint != null) {
            script.footHint.enabled = false;
        }
    }
}

// ===================== Camera Events =====================

// Fired when the user switches to the front-facing camera.
// We always stay in try-on mode and restore the last active shoe.
script.createEvent("CameraFrontEvent").bind(function () {
    isTryOn = true;
    script.setShoe(lastIndex);
    setHint();
});

// Fired when the user switches to the rear-facing camera.
// Same behaviour as the front camera event.
script.createEvent("CameraBackEvent").bind(function () {
    isTryOn = true;
    script.setShoe(lastIndex);
    setHint();
});

// ===================== Initialization =====================

/**
 * Runs once on Awake.
 * Wires up the visualization script and registers all shoe objects with it.
 */
function initialize() {
    // Hide any legacy hint object that may be set on the script directly.
    if (script.hint != null) {
        script.hint.enabled = false;
    }

    if (script.enableVisualization && script.visualizationRoot) {
        // Get the ShoeVisualization script from the visualization root object.
        visualizationScript = script.visualizationRoot.getFirstComponent("Component.ScriptComponent");

        // Register every shoe with the visualization script so it can build
        // its internal preview model copies.
        script.shoeSceneObjects.forEach(function (o) {
            visualizationScript.addObject(o);
        });
    } else if (script.visualizationRoot) {
        // Visualization is disabled — hide the root and its camera hint.
        script.visualizationRoot.enabled = false;
        script.cameraHint.enabled = false;
    }
}

initialize();
