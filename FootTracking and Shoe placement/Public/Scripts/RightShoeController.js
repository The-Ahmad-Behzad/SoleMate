//// RightShoeController.js
//// Version: 3.0.0
//// Event: On Awake
//// Description: Controls shoe selection for the RIGHT FOOT carousel.
////              Expects shoeSceneObjects to contain RIGHT-FOOT-ONLY shoe objects
////              (each with only a Right Foot Binding child, no Left Foot Binding).
////              Because the arrays are fully separate from LeftShoeController,
////              plain enable/disable of the whole SceneObject is safe and sufficient.
//
//// ===================== Inspector Inputs =====================
//
////@input SceneObject[] shoeSceneObjects
////@input SceneObject footHint
////@input bool enableVisualization = true
////@input SceneObject visualizationRoot {"showIf": "enableVisualization"}
////@input SceneObject cameraHint {"showIf": "enableVisualization"}
//
//// ===================== Runtime State =====================
//
//var isTryOn = true;
//var updateEvent = script.createEvent("UpdateEvent");
//var hintShown = false;
//var lastIndex = 0;
//var visualizationScript;
//var activeTracker = null;
//
//// ===================== Foot Hint Polling =====================
//
//// Poll until the right foot tracker locks on, then permanently hide the hint.
//updateEvent.bind(function () {
//    if (!isNull(activeTracker) && activeTracker.isTracking()) {
//        if (script.footHint != null) script.footHint.enabled = false;
//        hintShown = true;
//        updateEvent.enabled = false;
//    }
//});
//updateEvent.enabled = false;
//
//// ===================== Public API =====================
//
///**
// * Activates the right-foot shoe at `index`, disabling all others.
// * Because every object in this array is Right-foot-only, no foot
// * filtering is needed — enabling the whole SceneObject is enough.
// *
// * @param {number} index - Zero-based index into shoeSceneObjects.
// */
//script.setShoe = function (index) {
//    // Disable all right-foot shoes first.
//    script.shoeSceneObjects.forEach(function (so) {
//        so.enabled = false;
//    });
//
//    // Enable only the chosen shoe.
//    script.shoeSceneObjects[index].enabled = isTryOn;
//
//    // Cache script component for foot-detection polling.
//    activeTracker = script.shoeSceneObjects[index].getFirstComponent("Component.ScriptComponent");
//
//    if (script.enableVisualization) {
//        script.visualizationRoot.enabled = !isTryOn;
//        visualizationScript.setActive(index);
//    }
//
//    lastIndex = index;
//};
//
///** Returns the total number of right-foot shoe variants. */
//script.getShoeCount = function () {
//    return script.shoeSceneObjects.length;
//};
//
///** Returns the index currently shown on the right foot. */
//script.getShoeIndex = function () {
//    return lastIndex;
//};
//
//// ===================== Hint Logic =====================
//
//function setHint() {
//    if (isTryOn) {
//        if (!hintShown) {
//            updateEvent.enabled = true;
//            if (script.footHint != null) script.footHint.enabled = true;
//        }
//    } else {
//        updateEvent.enabled = false;
//        if (script.footHint != null) script.footHint.enabled = false;
//    }
//}
//
//// ===================== Camera Events =====================
//
//script.createEvent("CameraFrontEvent").bind(function () {
//    isTryOn = true;
//    script.setShoe(lastIndex);
//    setHint();
//});
//
//script.createEvent("CameraBackEvent").bind(function () {
//    isTryOn = true;
//    script.setShoe(lastIndex);
//    setHint();
//});
//
//// ===================== Initialization =====================
//
//function initialize() {
//    if (script.hint != null) script.hint.enabled = false;
//
//    if (script.enableVisualization && script.visualizationRoot) {
//        visualizationScript = script.visualizationRoot.getFirstComponent("Component.ScriptComponent");
//        script.shoeSceneObjects.forEach(function (o) {
//            visualizationScript.addObject(o);
//        });
//    } else if (script.visualizationRoot) {
//        script.visualizationRoot.enabled = false;
//        script.cameraHint.enabled = false;
//    }
//
//    // Show the first right-foot shoe by default on startup.
//    script.setShoe(0);
//}
//
//initialize();
//



// RightShoeController.js
// Version: 3.1.0 - Voice Enabled

//@input SceneObject[] shoeSceneObjects
//@input SceneObject footHint
//@input bool enableVisualization = true
//@input SceneObject visualizationRoot {"showIf": "enableVisualization"}
//@input SceneObject cameraHint {"showIf": "enableVisualization"}

var isTryOn = true;
var updateEvent = script.createEvent("UpdateEvent");
var hintShown = false;
var lastIndex = 0;
var visualizationScript;
var activeTracker = null;

updateEvent.bind(function () {
    if (!isNull(activeTracker) && activeTracker.isTracking()) {
        if (script.footHint != null) script.footHint.enabled = false;
        hintShown = true;
        updateEvent.enabled = false;
    }
});
updateEvent.enabled = false;

script.setShoe = function (index) {
    script.shoeSceneObjects.forEach(function (so) { so.enabled = false; });
    script.shoeSceneObjects[index].enabled = isTryOn;
    activeTracker = script.shoeSceneObjects[index].getFirstComponent("Component.ScriptComponent");

    if (script.enableVisualization) {
        script.visualizationRoot.enabled = !isTryOn;
        visualizationScript.setActive(index);
    }
    lastIndex = index;
};

script.getShoeCount = function () { return script.shoeSceneObjects.length; };
script.getShoeIndex = function () { return lastIndex; };

function setHint() {
    if (isTryOn) {
        if (!hintShown) {
            updateEvent.enabled = true;
            if (script.footHint != null) script.footHint.enabled = true;
        }
    } else {
        updateEvent.enabled = false;
        if (script.footHint != null) script.footHint.enabled = false;
    }
}

script.createEvent("CameraFrontEvent").bind(function () {
    isTryOn = true;
    script.setShoe(lastIndex);
    setHint();
});

script.createEvent("CameraBackEvent").bind(function () {
    isTryOn = true;
    script.setShoe(lastIndex);
    setHint();
});

function initialize() {
    if (script.hint != null) script.hint.enabled = false;

    if (script.enableVisualization && script.visualizationRoot) {
        visualizationScript = script.visualizationRoot.getFirstComponent("Component.ScriptComponent");
        script.shoeSceneObjects.forEach(function (o) {
            visualizationScript.addObject(o);
        });
    } else if (script.visualizationRoot) {
        script.visualizationRoot.enabled = false;
        script.cameraHint.enabled = false;
    }

    // --- VOICE CONTROL & PERSISTENCE ---
    var store = global.persistentStorageSystem.store;
    var savedIdx = 0;
    if (store.has("right_shoe_idx")) {
        savedIdx = store.getInt("right_shoe_idx");
    }

    var voiceCmd = "";
    if (global.launchParams.has("voice_command")) {
        voiceCmd = global.launchParams.getString("voice_command");
    }

    // Update index safely!
    if (voiceCmd === "next_right" || voiceCmd === "change_both") {
        savedIdx = savedIdx + 1;
        if (savedIdx >= script.getShoeCount()) savedIdx = 0;
    } else if (voiceCmd === "prev_right") {
        savedIdx = savedIdx - 1;
        if (savedIdx < 0) savedIdx = script.getShoeCount() - 1;
    }

    store.putInt("right_shoe_idx", savedIdx);
    script.setShoe(savedIdx);
}

initialize();
