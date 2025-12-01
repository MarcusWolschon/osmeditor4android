# Safe mode 

While we put a lot of effort in to avoiding issues, they can't completely be ruled out particularly as we do not control a major part of the inputs (the OSM data and configurations), if the app is crashing or unresponsive, you should try to start in safe mode. Except if the actual image or Android app specific cache has been corrupted deleting the cache and/or reinstalling the app should not be necessary. 

Before the app is actually started in safe mode you will be shown a modal with three options: 

 - Set data style to minimal (default on)
 - Disable all layers (default on)
 - Remove selection and UI state (default off)
 - Remove saved data state (default off)
 
The 1st two options are non-destructive and can be easily undone once the app has started, this is typically useful if a specific layer is causing an issue or the app is unresponsive. As disabling all layers already inhibits a lot of potential sources of issues, this is the first step you should try, for example you can still upload changes with layers disabled. 

The third option will remove any selections and other user interface state, for example a specific mode. While this option does not remove edits or similar, you will not be able to complete edits if they are part of a multi-step action mode.

The last option will remove the saved OSM data state including all edits, this is mainly useful if the data has become corrupted or for example if you are running out of memory. While this will delete any pending edits, you should still be able to retrieve them via the [auto-save](Advanced%20preferences.md#auto-save-configuration) feature.

To start the app in Safe mode in launchers that support short cuts, long press the app icon and select _Safe_.