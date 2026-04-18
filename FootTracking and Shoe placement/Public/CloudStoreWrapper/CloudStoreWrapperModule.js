/**
 * @module CloudStoreWrapperModule
 * Module wrapping CloudStorage to enable await/async and other helpful interfaces.
 * @author Snap Inc.
 * @version 1.0.0
 * 
 * ====  Example ====
 * @example

// Import module
const CloudStoreWrapper = require("./CloudStoreWrapperModule");

// Get a reference to the CloudStorageModule
// Resources panel > + > CloudStorageModule
//@input Asset.CloudStorageModule myCloudStorageModule

// We wrap our entire function in an async function block, 
// since we can't use `await` at top level
async function init() {
    // First we build the wrapped CloudStore, by passing in a CloudStorageModule asset 
    // to the `from` static method of CloudStoreWrappe.
    const wrappedCloudStore = await CloudStoreWrapper.from(script.myCloudStorageModule);

    // Using the CloudStoreWrapper we can use await/async ES6 keywords, instead of callback.
    // In other words, any code following will only proceed to the next line after the promise has resolved.
    await wrappedCloudStore.setValueInScope(StorageScope.User, "key", "originalValue");

    // We can await the stored value, which will be returned as an object {key, value}.
    // We can use ES6 destructuring syntax to turn the objects into variables.
    const {key, value} = await wrappedCloudStore.getValueInScope(StorageScope.User, "key");
    
    // Or list all the values in the store. Passing in true requests scripts to print out the store. 
    // This is useful to get multiple keys without getting rate limited.
    await wrappedCloudStore.listValuesInScope(StorageScope.User, true);

    // We can delete a key/value pair in the store.
    await wrappedCloudStore.deleteValueInScope(StorageScope.User, "key");
    Studio.log("Delete 'key' in scope");
}

// We write a catch on the function to report any error from await/async calls.
init()
    .catch((e) => {
        if(e.stack !== undefined) {
            Studio.log(e.message + e.stack);
        } else {
            Studio.log(`Error: ${e}`);
        }
    })

 * ====  End Example ====
 * Checkout the `Handle Store` section below to see APIs available in this class.
 * Checkout the CloudStoreWrapperExample in the Asset Library to see more examples!
*/

// Storing some private key only available to this file
// to ensure CloudStoreWrapper constructor is called only from this file.
// E.g from the `CloudStoreWrapper.from` static method.
const privateConstructorKey = Symbol();

class CloudStoreWrapper {
    /**
     * Creates the CloudStoreWrapper which this class will wrap.
     * @param {CloudStorageModule} cloudStorageModule Reference to a CloudStorageModule
     * @returns {CloudStoreWrapper} Reference to the CloudStoreWrapper
     * @private
     */
    constructor(key, cloudStorageModule, wrappedCloudStore) {
        if (key !== privateConstructorKey) {
            throw new Error("Cannot call constructor directly. Use `CloudStoreWrapper.from()` instead.");
          }

        this.cloudStorageModule = cloudStorageModule;
        this.store = wrappedCloudStore;
    }

    // We call this init from constructor so we can do async constructor
    /**
     * Creates a new wrapped store from a cloudStorageModule
     * @param {CloudStorageModule} Reference to the CloudStorageModule
     * @returns {CloudStoreWrapper}
     */
    static async from(cloudStorageModule, session = undefined) {
        const wrappedCloudStore = await CloudStoreWrapper._createCloudStore(cloudStorageModule, session);
        return new CloudStoreWrapper(privateConstructorKey, cloudStorageModule, wrappedCloudStore);
    }

    /*-----------------------------------------------------------------------------------
    Initialize Store
    -----------------------------------------------------------------------------------*/

    /**
     * Creates a Cloud Store we can wrap around.
     * @param {MultiplayerSession?} session a ConnectedLens Session if we want to share this cloud storage.
     * @returns {Promise<CloudStore>} Reference to the CloudStore
     * @private
     */
    static async _createCloudStore(cloudStorageModule, session = undefined) {
        const cloudStorageOptions = CloudStorageOptions.create();
    
        // We might want to pass in a session if we want
        // other people to have access to the Cloud Store
        if (session) {
            cloudStorageOptions.session = session;
        }

        return new Promise((resolve, reject) => {
                cloudStorageModule.getCloudStore(
                    cloudStorageOptions, 
                    resolve,
                    (message) => {
                        reject(new Error(message))
                    }
                );    
            });
    }

    /*-----------------------------------------------------------------------------------
    Handle Store
    -----------------------------------------------------------------------------------*/

    /**
     * Gets a value from the persistence backend.
     * 
     * If you are trying to get multiple values, you should use `listValuesInScope` instead
     * since there is a ratelimit to getting values from the backend.
     * 
     * Note that scope must match that which was used when the value was originally saved.
     * @param {StorageScope} scope The scope this value should be retrieved from.
     * @param {string} key The key the value was stored in.
     * @returns {Promise<(mat4|mat3|mat2|vec4|vec2|vec3|Boolean|quat|Number|String)>}
     */
    async getValueInScope(scope, key) {
        const readOptions = CloudStorageReadOptions.create();
        readOptions.scope = scope;
        
        return new Promise((resolve, reject) => {   
                this.store.getValue(
                    key,
                    readOptions,
                    (key, value)=> {
                        resolve({key, value})
                    },
                    (message) => {
                        reject(new Error(message))
                    }
                );
            });   
    }

    /**
     * Sets a value in the persistence backend.
     * Note that scope must match that which was used when the value was originally saved.
     * @param {StorageScope} scope The scope this value should be stored in.
     * @param {string} key The key the value should be stored in.
     * @returns {Promise<{key: string, value: (mat4|mat3|mat2|vec4|vec2|vec3|Boolean|quat|Number|String)}>}
     */
    async setValueInScope(scope, key, value) {
        const writeOptions = CloudStorageWriteOptions.create();
        writeOptions.scope = scope;

        return new Promise((resolve, reject) => {   
                this.store.setValue(
                    key,
                    value,
                    writeOptions,
                    () => {
                        // Note: original APIs return void, this is for niceties
                        // For example: when initializing multiple values to see 
                        // what key,value was added.
                        resolve({key, value})
                    },
                    (message) => {
                        reject(new Error(message))
                    }
                );    
            });    
    }

    /**
     * Lists values from the persistence backend.
     * Note that scope must match that which was used when the value was originally saved.
     * @param {StorageScope} scope The scope this value should be stored in.
     * @param {bool} autoPrint Whether the contents of the storage should be printed.
     * @returns {Promise<{results: [String, (mat4|mat3|mat2|vec4|vec2|vec3|Boolean|quat|Number|String)], cursor: string}>}
     */
    async listValuesInScope(scope, autoPrint) {
        const listOptions = CloudStorageListOptions.create();
        listOptions.scope = scope;

        return new Promise((resolve, reject) => {   
                this.store.listValues(
                    listOptions, 
                    (results, cursor) => {
                        if (autoPrint) {
                            this.printListValuesResult(results, cursor);
                        }

                        resolve({results, cursor});
                    },
                    (message) => {
                        reject(new Error(message))
                    }
                );    
            });
    }

    /**
     * Deletes a value in the persistence backend.
     * Note that scope must match that which was used when the value was originally saved.
     * @param {StorageScope} scope The scope this value is stored in.
     * @param {Promise<string>} key The key of the value to be deleted.
     */
    async deleteValueInScope(scope, key) {
        const readOptions = CloudStorageReadOptions.create();
        readOptions.scope = scope;
        
        return new Promise((resolve, reject) => {   
                this.store.deleteValue(
                    key,
                    readOptions,
                    resolve,
                    (message) => {
                        reject(new Error(message))
                    }
                );
            });   
    }


    /*-----------------------------------------------------------------------------------
    Example additional helpers
    -----------------------------------------------------------------------------------*/

    /**
     * Checkes whether exists in a store.
     * Note that scope must match that which was used when the value was originally saved.
     * @param {StorageScope} scope The scope this value is stored in.
     * @param {Promise<string>} key The key of the value to be deleted.
     */
    async hasValueInScope(scope, key) {
        const {results, cursor} = await this.listValuesInScope(scope);
        let doesExist = false;

        for(let i = 0; i < results.length; i++) {
            if (results[i][0] === key) {
                doesExist = true;
                break;
            }
        }

        return doesExist;
    }

    /**
     * Sets a value in the persistence backend if the key does not exist in the store yet.
     * Note that scope must match that which was used when the value was originally saved.
     * 
     * If you are trying to set multiple values, you should use `initializeValuesInScope` instead
     * since there is a ratelimit to getting values (i.e. has this key been init) from the backend.
     * 
     * @param {StorageScope} scope The scope this value should be stored in.
     * @param {string} key The key the value should be stored in.
     * @returns {Promise<{key: string, value: (mat4|mat3|mat2|vec4|vec2|vec3|Boolean|quat|Number|String)}>}
     */
    async initializeValueInScope(scope, key, value) {
        const doesExist = await this.hasValueInScope(scope, key);
        
        if (!doesExist) {
            return await this.setValueInScope(scope, key, value);
        }
    }

    /**
     * Sets multiple values in the persistence backend if the key does not exist in the store yet.
     * Note that scope must match that which was used when the value was originally saved.
     * @param {StorageScope} scope The scope this value should be stored in.
     * @param {{}} Key value pairs that we want to store.
     */
    async initializeValuesInScope(scope, keyValuePairs) {
        let modifiedPairs = [];

        const {results, cursor} = await this.listValuesInScope(scope);
        const existingkeys = results.map(kvPair => kvPair[0]);

        for (const k in keyValuePairs) {
            const keyDoesNotExist = !existingkeys.includes(k);
            
            if(keyDoesNotExist) {
                const key = k;
                const value = keyValuePairs[k];

                const newlyAddedKeyPromise = this.setValueInScope(scope, key, value);
                modifiedPairs.push(newlyAddedKeyPromise);
            }
        }

        return Promise.all(modifiedPairs);
    }

    /**
     * Deletes everything in the persistence backend.
     * Note that scope must match that which was used when the value was originally saved.
     * @param {StorageScope} scope The scope this value is stored in.
     */
    async clear(scope) {
        let deletePromises = [];

        const {results, cursor} = await this.listValuesInScope(scope);
        const existingkeys = results.map(kconstr => kconstr[0]);

        existingkeys.forEach(key => {
            const deletePromise = this.deleteValueInScope(scope, key);
            deletePromises.push(deletePromise);
        })

        return Promise.all(deletePromises);
    }

    /*-----------------------------------------------------------------------------------
    Utilities
    -----------------------------------------------------------------------------------*/

    /**
     * Gets the store that's being wrapped.
     * @returns {CloudStore}
     */
    getStore() {
        return this.store;
    }

    /**
     * Utility function that prints out the result of a CloudStore.listValues.
     * @param {(mat4|mat3|mat2|vec4|vec2|vec3|Boolean|quat|Number|String[][])} results 
     * @param {string} cursor 
     */
    printListValuesResult (results, cursor) {
        // Results are returned as a list of [key, value] tuples
        for (let i = 0; i < results.length; ++i) {
            const key = results[i][0];
            const value = results[i][1];
            Studio.log(' - key: ' + key + ' value: ' + value);
        }
    }
}

/*-----------------------------------------------------------------------------------
Exposed the module
-----------------------------------------------------------------------------------*/
module.exports = CloudStoreWrapper