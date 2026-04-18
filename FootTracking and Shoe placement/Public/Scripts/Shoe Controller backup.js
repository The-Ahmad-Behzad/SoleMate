// Shoe Controller.js
// Version: 2.0.0
// Event: On Awake
// Description: Handles switching between visualization and try-on, managing hints,
//              switching shoe models, and multi-shoe comparison mode

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

// ===================== Comparison Mode State =====================
var isComparisonMode = false;
var leftShoeIndex = 0;
var rightShoeIndex = 1;

updateEvent.bind(function () {
    if (!isNull(activeTracker) && activeTracker.isTracking()) {
        if (script.footHint != null) {
            script.footHint.enabled = false;
        }
        hintShown = true;
        updateEvent.enabled = false;
    }
});
updateEvent.enabled = false;

// ===================== Foot Visibility Helpers =====================

/**
 * Show only the left foot mesh of a shoe object, hide the right foot.
 */
function showLeftFootOnly(shoeObj) {
    var shoeComp = shoeObj.getFirstComponent("Component.ScriptComponent");
    if (!shoeComp) return;

    if (shoeComp.leftFoot) {
        shoeComp.leftFoot.enabled = true;
    }
    if (shoeComp.rightFoot) {
        shoeComp.rightFoot.enabled = false;
    }
}

/**
 * Show only the right foot mesh of a shoe object, hide the left foot.
 */
function showRightFootOnly(shoeObj) {
    var shoeComp = shoeObj.getFirstComponent("Component.ScriptComponent");
    if (!shoeComp) return;

    if (shoeComp.leftFoot) {
        shoeComp.leftFoot.enabled = false;
    }
    if (shoeComp.rightFoot) {
        shoeComp.rightFoot.enabled = true;
    }
}

/**
 * Show both feet of a shoe object (normal mode).
 */
function showBothFeet(shoeObj) {
    var shoeComp = shoeObj.getFirstComponent("Component.ScriptComponent");
    if (!shoeComp) return;

    if (shoeComp.leftFoot) {
        shoeComp.leftFoot.enabled = true;
    }
    if (shoeComp.rightFoot) {
        shoeComp.rightFoot.enabled = true;
    }
}

// ===================== Normal Mode =====================

script.setShoe = function (index) {
    if (isComparisonMode) {
        // In comparison mode, carousel changes are ignored for normal setShoe
        // Use setLeftShoe / setRightShoe instead
        return;
    }

    script.shoeSceneObjects.forEach(function (so) {
        so.enabled = false;
    });

    script.shoeSceneObjects[index].enabled = isTryOn;
    showBothFeet(script.shoeSceneObjects[index]);
    activeTracker = script.shoeSceneObjects[index].getFirstComponent("Component.ScriptComponent");

    if (script.enableVisualization) {
        script.visualizationRoot.enabled = !isTryOn;
        visualizationScript.setActive(index);
    }

    lastIndex = index;
};

// ===================== Comparison Mode =====================

/**
 * Enter comparison mode with specified shoes on each foot.
 */
function applyComparisonMode() {
    // Disable all shoes first
    script.shoeSceneObjects.forEach(function (so) {
        so.enabled = false;
    });

    if (leftShoeIndex === rightShoeIndex) {
        // Same shoe on both feet — just show normally
        script.shoeSceneObjects[leftShoeIndex].enabled = isTryOn;
        showBothFeet(script.shoeSceneObjects[leftShoeIndex]);
        activeTracker = script.shoeSceneObjects[leftShoeIndex].getFirstComponent("Component.ScriptComponent");
    } else {
        // Different shoes: left foot from leftShoeIndex, right foot from rightShoeIndex
        script.shoeSceneObjects[leftShoeIndex].enabled = isTryOn;
        showLeftFootOnly(script.shoeSceneObjects[leftShoeIndex]);

        script.shoeSceneObjects[rightShoeIndex].enabled = isTryOn;
        showRightFootOnly(script.shoeSceneObjects[rightShoeIndex]);

        // Use left shoe's tracker as primary
        activeTracker = script.shoeSceneObjects[leftShoeIndex].getFirstComponent("Component.ScriptComponent");
    }

    if (script.enableVisualization) {
        script.visualizationRoot.enabled = !isTryOn;
        if (visualizationScript && visualizationScript.setComparisonActive) {
            visualizationScript.setComparisonActive(leftShoeIndex, rightShoeIndex);
        } else if (visualizationScript) {
            visualizationScript.setActive(leftShoeIndex);
        }
    }
}

/**
 * Toggle between normal and comparison mode.
 * Returns the new mode state (true = comparison, false = normal).
 */
script.toggleComparisonMode = function () {
    isComparisonMode = !isComparisonMode;

    if (isComparisonMode) {
        // Enter comparison with current shoe as left, next shoe as right
        leftShoeIndex = lastIndex;
        rightShoeIndex = (lastIndex + 1) % script.shoeSceneObjects.length;
        applyComparisonMode();
    } else {
        // Return to normal mode with left shoe
        lastIndex = leftShoeIndex;
        script.shoeSceneObjects.forEach(function (so) {
            so.enabled = false;
        });
        script.shoeSceneObjects[lastIndex].enabled = isTryOn;
        showBothFeet(script.shoeSceneObjects[lastIndex]);
        activeTracker = script.shoeSceneObjects[lastIndex].getFirstComponent("Component.ScriptComponent");

        if (script.enableVisualization) {
            script.visualizationRoot.enabled = !isTryOn;
            visualizationScript.setActive(lastIndex);
        }
    }

    return isComparisonMode;
};

/**
 * Set the left foot shoe in comparison mode.
 */
script.setLeftShoe = function (index) {
    if (index < 0 || index >= script.shoeSceneObjects.length) return;
    leftShoeIndex = index;
    if (isComparisonMode) {
        applyComparisonMode();
    }
};

/**
 * Set the right foot shoe in comparison mode.
 */
script.setRightShoe = function (index) {
    if (index < 0 || index >= script.shoeSceneObjects.length) return;
    rightShoeIndex = index;
    if (isComparisonMode) {
        applyComparisonMode();
    }
};

/**
 * Set both comparison shoes at once.
 */
script.setComparisonShoes = function (leftIdx, rightIdx) {
    leftShoeIndex = leftIdx;
    rightShoeIndex = rightIdx;
    if (isComparisonMode) {
        applyComparisonMode();
    }
};

/**
 * Get comparison mode state.
 */
script.isComparisonMode = function () {
    return isComparisonMode;
};

/**
 * Get current left shoe index.
 */
script.getLeftShoeIndex = function () {
    return leftShoeIndex;
};

/**
 * Get current right shoe index.
 */
script.getRightShoeIndex = function () {
    return rightShoeIndex;
};

/**
 * Get total number of shoes.
 */
script.getShoeCount = function () {
    return script.shoeSceneObjects.length;
};

// ===================== Hints =====================

function setHint() {
    if (isTryOn) {
        if (!hintShown) {
            updateEvent.enabled = true;
            if (script.footHint != null) {
                script.footHint.enabled = true;
            }
        }
    } else {
        updateEvent.enabled = false;
        if (script.footHint != null) {
            script.footHint.enabled = false;
        }
    }

}

// ===================== Camera Events =====================

script.createEvent("CameraFrontEvent").bind(function () {
    isTryOn = true;
    if (isComparisonMode) {
        applyComparisonMode();
    } else {
        script.setShoe(lastIndex);
    }
    setHint();
});

script.createEvent("CameraBackEvent").bind(function () {
    isTryOn = true;
    if (isComparisonMode) {
        applyComparisonMode();
    } else {
        script.setShoe(lastIndex);
    }
    setHint();
});

// ===================== Initialization =====================

function initialize() {
    if (script.hint != null) {
        script.hint.enabled = false;
    }

    if (script.enableVisualization && script.visualizationRoot) {
        visualizationScript = script.visualizationRoot.getFirstComponent("Component.ScriptComponent");

        script.shoeSceneObjects.forEach(function (o) {
            visualizationScript.addObject(o);
        });
    } else if (script.visualizationRoot) {
        script.visualizationRoot.enabled = false;

        script.cameraHint.enabled = false;
    }
}

initialize();
