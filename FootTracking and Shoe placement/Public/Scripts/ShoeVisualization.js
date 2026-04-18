// ShoeVisualization.js
// Version: 1.0.0
// Event: On Awake
// Description: Presents shoe models in a 360° spinning visualization mode.
//              Shoe Controller calls addObject() for each shoe variant, and
//              setActive(index) to switch which model is visible.
//
// Public API:
//   script.addObject(SceneObject)  — Register a shoe SceneObject for visualization.
//   script.setActive(index)        — Show only the model at `index`, hide the rest.

// ===================== Inspector Inputs =====================

// Show extra configuration fields in the Lens Studio inspector.
//@input bool advanced

// Allow the model to also spin on the vertical (Y) axis during drag.
//@input bool verticalRotation {"showIf": "advanced"}

// The parent SceneObject whose world rotation drives the spin animation.
//@input SceneObject parent {"showIf": "advanced"}

// A proxy object whose world scale is used to handle pinch-to-zoom.
// Its scale is written back to the visualization box each frame.
//@input SceneObject scaleProxy {"showIf": "advanced"}

// Local-space offset applied to each copied model so it sits correctly
// within the visualization box (e.g. raise it off the floor).
//@input vec3 rotationOffset {"showIf": "advanced"}

// Render order assigned to the copied visualization meshes, ensuring they
// draw on top of (or behind) other elements as required.
//@input int renderOrder {"showIf": "advanced"}

// The InteractionComponent used to receive touch input for spin and zoom.
//@input Component.InteractionComponent interactionComponent

// ===================== Guard Checks =====================

// Both parent and scaleProxy are essential — abort early if missing.
if (!script.parent) {
    print("error: parent SceneObject not set");
    script.enabled = false;
    return;
}

if (!script.scaleProxy) {
    print("error: scaleProxy SceneObject not set");
    script.enabled = false;
    return;
}

// ===================== Internal Setup =====================

// The first child of parent is the "box" that holds all copied shoe models.
script.box = script.parent.getChild(0);

// Utility module for recursive scene-graph helpers (findChild, getComponents…).
var objHelpers = require("./SceneObjectHelpersModule");

// Constant angular velocity applied when the user is not touching the screen,
// making the model gently auto-spin so it looks alive.
const SPIN_DELTA = new vec2(0.001, 0);

// Name of the foot-tracking custom occluder mesh added by the CC asset.
// We destroy these from visualization copies so they don't occlude the preview.
const CUSTOM_OCCLUDER_NAME = "Custom Occluder";

// Name of the left-foot occluder specifically added by the Foot Tracking CC.
const CC_LEFT_OCCLUDER_NAME = "Left Foot Occluder 2";

// ===================== Touch / Spin State =====================

var touchPos = new vec2(0, 0);      // Current touch position.
var touchDelta = SPIN_DELTA;        // How much to rotate this frame (position delta).
var lastTouchPos = new vec2(0, 0);  // Previous frame's touch position (for delta).
var speed = 0.5;                    // Multiplier applied to the raw touch delta.

// Accumulated quaternion representing the total spin built up from user input.
// Lerped back toward identity to create the deceleration / momentum feel.
var accum = quat.quatIdentity();

var touches = [];   // Active touch IDs. We only react to the first finger.

// Capture the initial world scale of scaleProxy (= 1/worldScale) so pinch-zoom
// can express the new scale as a multiple of that baseline.
var initialScale = 1 / script.scaleProxy.getTransform().getWorldScale().x;

// Visualization copies of shoe models, indexed in the same order as shoeSceneObjects.
var models = [];

// Cached ScriptComponent references for each registered shoe (not currently used
// after addObject(), but kept for potential future API extensions).
var shoeComponents = [];

// Pixel-space sensitivity for horizontal and vertical drag.
// Negative values invert the drag direction so swiping right spins the model right.
var X_SENSITIVITY = -2;
var Y_SENSITIVITY = script.verticalRotation ? -2 : 0; // Zero if vertical rotation is disabled.

// ===================== Touch Event Binding =====================

// Record the start position and register the touch ID when a finger lands.
script.interactionComponent.onTouchStart.add(function (touchStartEventArgs) {
    lastTouchPos = touchStartEventArgs.position;
    touches.push(touchStartEventArgs.touchId);
});

// Update the touch delta on each move event, but only for the primary finger
// (index 0) to avoid jumpy behaviour when a second finger is added.
script.interactionComponent.onTouchMove.add(function (touchStartEventArgs) {
    if (touchStartEventArgs.touchId != touches[0]) {
        return; // Ignore secondary fingers.
    }
    touchPos = touchStartEventArgs.position;
    // Delta = how far the finger moved since last frame, scaled by speed.
    touchDelta = lastTouchPos.sub(touchPos).uniformScale(speed);
    lastTouchPos = touchPos;
});

// When all fingers lift, clear the touch list and revert to the idle auto-spin.
script.interactionComponent.onTouchEnd.add(function (eventData) {
    touches = [];
    touchDelta = SPIN_DELTA; // Resume gentle auto-spin.
});

// ===================== Update Loop =====================

script.createEvent("UpdateEvent").bind(function (eventData) {
    // --- Pinch-to-zoom ---
    if (touches.length > 1) {
        // Two fingers on screen: apply the scaleProxy's world scale to the box.
        // The scaleProxy scale is driven by the Lens Studio pinch gesture system.
        var newScale = script.scaleProxy.getTransform().getWorldScale().uniformScale(initialScale);
        script.box.getTransform().setWorldScale(newScale);
        return; // Skip rotation while zooming.
    }

    // --- Spin rotation ---
    // Build a small rotation quaternion from this frame's touch delta.
    var testQuat = quat.fromEulerAngles(Y_SENSITIVITY * touchDelta.y, X_SENSITIVITY * touchDelta.x, 0);

    // Accumulate the new rotation on top of existing momentum.
    accum = accum.multiply(testQuat);

    // Decay the accumulated rotation toward identity (no rotation) each frame.
    // This creates a smooth deceleration / momentum effect.
    accum = quat.slerp(accum, quat.quatIdentity(), 0.1);

    // Apply the accumulated rotation to the parent in world space.
    var objectRot = script.parent.getTransform().getWorldRotation();
    var newRot = objectRot.multiply(accum);
    script.parent.getTransform().setWorldRotation(newRot);

    // Transfer the world rotation from parent onto the box child, then reset
    // the parent to identity. This decouples the pivot point from the spin axis
    // so the model always rotates around its own center.
    var boxRot = script.box.getTransform().getWorldRotation();
    script.parent.getTransform().setWorldRotation(quat.quatIdentity());
    script.box.getTransform().setWorldRotation(boxRot);
});

// ===================== Public API =====================

/**
 * Show only the visualization copy at `index`; hide all others.
 *
 * @param {number} index - Which model to make visible.
 */
script.setActive = function (index) {
    models.forEach(function (o, i) {
        if (!isNull(o)) {
            // Enable the model only if it matches the requested index.
            o.enabled = i == index;
        } else if (i == index) {
            // Fallback: if the reference is somehow null but this is the active
            // index, try to enable it anyway (shouldn't normally happen).
            models[i].enabled = true;
        }
    });
};

// ===================== Internal Helpers =====================

/**
 * Creates a deep copy of a shoe's left-foot mesh inside the visualization box,
 * strips foot-tracking occluder objects, and assigns the correct render order.
 * The copy starts disabled — setActive() is used to enable the right one.
 *
 * @param {SceneObject} shoe - The left-foot SceneObject to copy.
 * @returns {SceneObject} The new visualization copy parented to script.box.
 */
function copyVisModel(shoe) {
    // Deep-copy the entire hierarchy under `shoe` into the visualization box.
    var newModel = script.box.copyWholeHierarchy(shoe);

    // Position the copy using the configured offset (e.g. float it above the floor).
    newModel.getTransform().setLocalPosition(script.rotationOffset);

    // Match the render order of the parent so layering stays consistent.
    newModel.renderOrder = script.parent.renderOrder;

    // Remove the left-foot occluder added by the Foot Tracking CC asset.
    // In visualization mode there are no real feet, so the occluder would just
    // punch a hole in the model.
    var occluder = objHelpers.findChildObjectWithName(newModel, CC_LEFT_OCCLUDER_NAME);
    if (occluder) {
        occluder.destroy();
    }

    // Also remove any custom occluder the project may have added.
    occluder = objHelpers.findChildObjectWithName(newModel, CUSTOM_OCCLUDER_NAME);
    if (!isNull(occluder)) {
        occluder.destroy();
    }

    // Apply the desired render order to every Visual component in the hierarchy.
    var visuals = objHelpers.getComponentsRecursive(newModel, "Component.Visual");
    visuals.forEach(function (o) {
        o.setRenderOrder(script.renderOrder);
    });

    // Start hidden — the correct model will be shown via setActive().
    newModel.enabled = false;

    return newModel;
}

/**
 * Registers a shoe SceneObject so it participates in visualization mode.
 * Internally, this temporarily enables the object to fire its OnAwake/Init,
 * copies its left-foot mesh into the visualization box, then disables it again.
 *
 * Called by Shoe Controller during initialization for every shoe variant.
 *
 * @param {SceneObject} sceneObject - A shoe root SceneObject with a ScriptComponent.
 */
script.addObject = function (sceneObject) {
    // Retrieve the shoe's script so we can access its leftFoot property.
    var shoeComp = sceneObject.getFirstComponent("ScriptComponent");
    if (!shoeComp) {
        print("error in addObject: could not find a script attached to the provided object");
        return;
    }

    // Store the script reference and reserve a slot in the models array.
    shoeComponents.push(shoeComp);
    models.push(null); // Placeholder; filled in below.

    // Enable the SceneObject so its script runs OnAwake and exposes leftFoot.
    sceneObject.enabled = true;

    if (shoeComp.leftFoot) {
        // Copy the left-foot mesh into the visualization box and store the copy.
        models[models.length - 1] = copyVisModel(shoeComp.leftFoot);
    }

    // Disable the shoe again — Shoe Controller will re-enable it via setShoe().
    sceneObject.enabled = false;
};