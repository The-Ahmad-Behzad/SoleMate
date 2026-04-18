// @input Asset.RemoteServiceModule remoteServiceModule

// Import module
const Module = require("./Public In-Lens Creation API Module");
const ApiModule = new Module.ApiModule(script.remoteServiceModule);

// Access functions defined in ApiModule like this:
//ApiModule.(function name)
