// DynamicShoeLoader.js
// Version: 1.0.0
// Event: On Awake

//@input Asset.RemoteMediaModule remoteMediaModule
//@input Asset.RemoteServiceModule s3BucketAPI
//@input SceneObject leftShoeSlot
//@input SceneObject rightShoeSlot
//@input SceneObject loadingIndicator

var currentLeftModel = null;
var currentRightModel = null;
var isLoadingLeft = false;
var isLoadingRight = false;

// ----------------- Helpers -----------------

function clearSlot(slot) {
    if (!slot) return;
    var childCount = slot.getChildrenCount();
    for (var i = childCount - 1; i >= 0; i--) {
        slot.getChild(i).destroy();
    }
}

function applyConfig(instance, config, isLeft) {
    if (!config) return;
    var transform = instance.getTransform();
    if (config.scale) {
        var sx = config.scale[0];
        var sy = config.scale[1];
        var sz = config.scale[2];
        transform.setLocalScale(new vec3(sx, sy, sz));
    }
    if (config.positionOffset) {
        // Invert X position if it's the right foot being mirrored
        var px = isLeft ? config.positionOffset[0] : -config.positionOffset[0];
        transform.setLocalPosition(new vec3(px, config.positionOffset[1], config.positionOffset[2]));
    }
    if (config.rotationOffset) {
        var deg2rad = Math.PI / 180;
        var rx = config.rotationOffset[0];
        var ry = isLeft ? config.rotationOffset[1] : -config.rotationOffset[1];
        var rz = isLeft ? config.rotationOffset[2] : -config.rotationOffset[2];
        transform.setLocalRotation(quat.fromEulerAngles(rx * deg2rad, ry * deg2rad, rz * deg2rad));
    }
}

// Ensure materials render as TwoSided because of negative scaling flips
function forceTwoSided(instance) {
    var visuals = instance.getComponentsInChildren("Component.RenderMeshVisual");
    for (var i = 0; i < visuals.length; i++) {
        var mat = visuals[i].mainPass;
        if (mat) {
            mat.twoSided = true;
            mat.cullMode = CullMode.None;
        }
    }
}

// ----------------- Main Functions -----------------

script.loadShoe = function (url, foot, config, callback) {
    var isLeft = (foot === "left");
    var slot = isLeft ? script.leftShoeSlot : script.rightShoeSlot;

    if (!slot) {
        print("[DynamicShoeLoader] ERROR: Shoe slot missing for " + foot);
        return;
    }

    if (isLeft && isLoadingLeft) return;
    if (!isLeft && isLoadingRight) return;

    if (isLeft) isLoadingLeft = true;
    else isLoadingRight = true;

    if (script.loadingIndicator) script.loadingIndicator.enabled = true;

    clearSlot(slot);
    
    // The 'endpointPath' is what we want to download (e.g. "shoes/Sandals/girl_sandal1.glb")
    var endpointPath = url.replace("https://solemate-mod-buck-213433448245-ap-southeast-2-an.s3.ap-southeast-2.amazonaws.com/", "");
    
    var req = RemoteApiRequest.create();
    
    // IMPORTANT: The 'req.endpoint' must exactly match the Endpoint Name you typed in the Snap Portal.
    // If you named it "GetShoe", this must be "GetShoe".
    req.endpoint = "GetShoe"; 
    
    // We pass the required path parameter to the portal's `{path}` variable
    req.parameters = {
        "path": endpointPath
    };

    if (!script.s3BucketAPI) {
        print("[DynamicShoeLoader] ERROR: s3BucketAPI is required to download 3D models.");
        if (isLeft) isLoadingLeft = false; else isLoadingRight = false;
        if (script.loadingIndicator && !isLoadingLeft && !isLoadingRight) script.loadingIndicator.enabled = false;
        return;
    }

    script.s3BucketAPI.performApiRequest(req, function(response) {
        if (response.statusCode !== 1) {
            print("[DynamicShoeLoader] Download failed from S3. HTTP Status code: " + response.statusCode);
            if (isLeft) isLoadingLeft = false; else isLoadingRight = false;
            if (script.loadingIndicator && !isLoadingLeft && !isLoadingRight) script.loadingIndicator.enabled = false;
            return;
        }

        var resource = response.asResource();

        script.remoteMediaModule.loadResourceAsGltfAsset(
            resource,
            function (gltfAsset) {
                var instance = gltfAsset.tryInstantiate(slot);
                if (!instance) {
                    print("[DynamicShoeLoader] ERROR instantiating " + foot);
                    if (isLeft) isLoadingLeft = false; else isLoadingRight = false;
                    if (script.loadingIndicator && !isLoadingLeft && !isLoadingRight) script.loadingIndicator.enabled = false;
                    return;
                }

                applyConfig(instance, config, isLeft);
                forceTwoSided(instance);

                if (isLeft) {
                    currentLeftModel = instance;
                    isLoadingLeft = false;
                } else {
                    currentRightModel = instance;
                    isLoadingRight = false;
                }

                if (script.loadingIndicator && !isLoadingLeft && !isLoadingRight) script.loadingIndicator.enabled = false;
                print("[DynamicShoeLoader] Successfully loaded " + foot + " model.");
                if (callback) callback(instance);
            },
            function (error) {
                print("[DynamicShoeLoader] Load Error: " + error);
                if (isLeft) isLoadingLeft = false; else isLoadingRight = false;
                if (script.loadingIndicator && !isLoadingLeft && !isLoadingRight) script.loadingIndicator.enabled = false;
            }
        );
    });
};

script.loadShoeBothFeet = function (url, config, callback) {
    config = config || {};
    
    // Load left first
    script.loadShoe(url, "left", config, function (leftInstance) {
        
        // Setup config for right foot mirror
        var rightConfig = JSON.parse(JSON.stringify(config));
        if (rightConfig.scale) {
            rightConfig.scale[0] = -rightConfig.scale[0]; // Mirror X scale
        }
        
        script.loadShoe(url, "right", rightConfig, function (rightInstance) {
            if (callback) callback(leftInstance, rightInstance);
        });
    });
};

global.dynamicShoeLoader = script;
