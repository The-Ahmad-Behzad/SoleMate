// RemoteApiHandler.js
// Version: 1.0.0
// Event: On Awake

//@input Asset.RemoteServiceModule remoteServiceModule

var catalogue = null;

script.createEvent("OnStartEvent").bind(function () {
    print("[RemoteApi] Requesting shoe catalogue from app...");
    requestCatalogue();
});

function requestCatalogue() {
    var request = RemoteApiRequest.create();
    request.endpoint = "get_shoe_catalogue";

    if (!script.remoteServiceModule) {
        print("[RemoteApi] ERROR: RemoteServiceModule not assigned!");
        return;
    }

    script.remoteServiceModule.performApiRequest(
        request,
        function (response, error) {
            if (error || !response || response.statusCode !== 200) {
                print("[RemoteApi] Error fetching catalogue (Is your mobile app responding?) " + (error || "Bad status"));
                return;
            }
            try {
                var data = JSON.parse(response.body);
                catalogue = data.shoes || [];
                global.shoeCatalogue = catalogue;
                print("[RemoteApi] Loaded " + catalogue.length + " shoes");
                
                // For test purposes, let's automatically load the first shoe
                if (catalogue.length > 0) {
                     global.onShoeSelected(catalogue[0].id, "both");
                }
            } catch (e) {
                print("[RemoteApi] JSON parse error: " + e);
            }
        }
    );
}

global.onShoeSelected = function (shoeId, foot) {
    if (!catalogue) return;
    
    var shoe = null;
    for (var i = 0; i < catalogue.length; i++) {
        if (catalogue[i].id === shoeId) {
            shoe = catalogue[i];
            break;
        }
    }
    
    if (!shoe) return;
    print("[RemoteApi] Selected shoe " + shoe.name + " for " + foot);
    
    var config = {
        scale: shoe.scale || [1, 1, 1],
        positionOffset: shoe.positionOffset || [0, 0, 0],
        rotationOffset: shoe.rotationOffset || [0, 0, 0]
    };

    if (global.dynamicShoeLoader) {
        if (foot === "both") {
            global.dynamicShoeLoader.loadShoeBothFeet(shoe.modelUrl, config);
        } else {
            global.dynamicShoeLoader.loadShoe(shoe.modelUrl, foot, config);
        }
    }
};

// Periodic polling for shoe selection changes from Mobile App
var pollTimer = 0;
global.lastSelectedShoe = null;

script.createEvent("UpdateEvent").bind(function (eventData) {
    if (!script.remoteServiceModule) return;
    
    pollTimer += eventData.getDeltaTime();
    if (pollTimer < 1.5) return; // poll every 1.5 seconds
    pollTimer = 0;

    var request = RemoteApiRequest.create();
    request.endpoint = "get_selected_shoe";
    script.remoteServiceModule.performApiRequest(request, function (res, err) {
        if (err || !res || res.statusCode !== 200) return; // Silent polling failures
        try {
            var data = JSON.parse(res.body);
            if (data.shoeId && data.shoeId !== global.lastSelectedShoe) {
                global.lastSelectedShoe = data.shoeId;
                global.onShoeSelected(data.shoeId, data.foot || "both");
            }
        } catch (e) {} 
    }); 
});

// ===================== TEMPORARY SCENE TEST =====================
script.createEvent("OnStartEvent").bind(function () {
    print("[Test] Automatically loading the shoe directly from S3 to test the scene setup...");
    
    var url = "https://solemate-mod-buck-213433448245-ap-southeast-2-an.s3.ap-southeast-2.amazonaws.com/shoes/Sandals/girl_sandal1.glb";
    var config = {
        scale: [-1.00, -1.00, -1.02],
        positionOffset: [-0.1237, -0.0353, -0.0023],
        rotationOffset: [-178.3509, 4.8744, 0.1402]
    };

    // Load left and right
    if (global.dynamicShoeLoader) {
        global.dynamicShoeLoader.loadShoeBothFeet(url, config, function(left, right) {
            print("[Test] Successfully loaded and mapped both shoes!");
        });
    } else {
        print("[Test] ERROR: DynamicShoeLoader not found globally.");
    }
});
