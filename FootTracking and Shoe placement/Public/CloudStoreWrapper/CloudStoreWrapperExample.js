// CloudStoreWrapperExample.js
// Version: 0.1.0
// Event: Lens Initialized
// Description: This example demonstrates the CloudStoreWrapper, which allows you 
// to use ES6 await/async to handle CloudStorageModule.

//@input Asset.CloudStorageModule myCloudStorageModule
/** @type {CloudStorageModule} */
const myCloudStorageModule = script.myCloudStorageModule;

// First we get the CloudStoreWrapper
const CloudStoreWrapper = require("./CloudStoreWrapperModule");

// We wrap our entire function in an async function block, 
// since we can't use `await` at top level
async function init() {
    // First we build the wrapped CloudStore, passing in a CloudStorageModule asset
    // Resources panel > + > CloudStorageModule
    const wrappedCloudStore = await CloudStoreWrapper.from(myCloudStorageModule);

    // Using the CloudStoreWrapper we can use await/async ES6 keywords, instead of callback.
    // In other words, any code following will only proceed to the next line after the promise has resolved.
    await wrappedCloudStore.setValueInScope(StorageScope.User, "key", "originalValue");

    // We can await the stored value, which will be returned as an object {key, value}.
    // We can use ES6 destructuring syntax to turn the objects into variables.
    const {key, value} = await wrappedCloudStore.getValueInScope(StorageScope.User, "key");

    // We can use Studio.log instead of Logger, so we can see logs from a Lens running on Pushed to Device.
    // We can use ES6 template literals to print our key and value within a string.
    Studio.log(`Stored in the key: ${key} is the value: ${value}`);

    // Or list all the values in the store. Passing in true requests scripts to print out the store.
    Studio.log("Print out current store:");
    await wrappedCloudStore.listValuesInScope(StorageScope.User, true);

    // We can delete a key/value pair in the store.
    await wrappedCloudStore.deleteValueInScope(StorageScope.User, "key");
    Studio.log("Delete 'key' in scope");

    /**
     * This wrapper provide some additional convenience functions:
     **/

    // We can check whether a key has been initialized in a store.
    const doesExist = await wrappedCloudStore.hasValueInScope(StorageScope.User, "key");
    Studio.log("Result of hasValueInScope 'key': " + doesExist);

    // We can set value, if a value hasn't already been set.
    const initValue = await wrappedCloudStore.initializeValueInScope(StorageScope.User, "keyTwo", "two");
    Studio.log("Result of initializeValueInScope: " + JSON.stringify(initValue));

    // Protip: When destructuring, you can optionally pass in the variable name you want to use.
    const {key: myKey, value: valueOfKeyTwo} = await wrappedCloudStore.getValueInScope(StorageScope.User, "keyTwo");
    Studio.log(`Stored in the key: ${myKey} is the value: ${valueOfKeyTwo}`);
    
    // Passing in false to listValuesInScope skips the print within the helper function.
    const r = await wrappedCloudStore.listValuesInScope(StorageScope.User, false);
    Studio.log("Result of listValuesInScope: " + JSON.stringify(r));

    // Using the wrapper, we can also initialize multiple values in scope, setting values only if it hasn't existed previously.
    const dataToSync =  {
        "key": "new value one", 
        "keyTwo": "new value two", 
        "keyThree": "new value three", 
        "keyFour": 4
    };
    // This call will return only the changed key/value pairs.
    const keyValuePairsAdded = await wrappedCloudStore.initializeValuesInScope(StorageScope.User, dataToSync);
    Studio.log("Result of initializeValuesInScope: " + JSON.stringify(keyValuePairsAdded));

    Studio.log("Print out current store:");
    await wrappedCloudStore.listValuesInScope(StorageScope.User, true);

    // Finally, we can clear every key/value in the store.
    await wrappedCloudStore.clear(StorageScope.User);
    Studio.log("All key/values cleared!");
}

// We write a catch on the function to report any error from await/async calls.
init()
    .catch((e) => {
        Studio.log(e.message + e.stack);
    })

