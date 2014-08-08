# Vespucci TagEditor

The TagEditor screen is the central place for changing object attributes and relation memberships. To exit and save your changes top the Vespucci icon, pressing the back button will give you the option of exiting without saving. Tags with empty value fields will not be saved.

Most of the fields use context sensitive auto complete provided by the active presets. Clicking twice (with a short pause) will re-display the auto complete menu.  

### Special auto complete behaviour

If the key of a field is "addr:street" a list of the nearest roads in the down loaded data will be provided.

If the key is "name", selection of a value from the auto complete list will add corresponding tags and if enabled in the settings, apply an appropriate preset. Typical use case: add a name key and start typing "mcd", select "McDonalds's" from the auto completion list and an amenity with the correct tags and name spelling will be added.

## ![](../images/address.png) Add address tags

Add address tags for the object. This function will try to predict the house number to suggest. It works best along straight roads with regular numbering schemes. 

## ![](../images/tag_menu_preset.png) Search and apply preset

Opens a screen which allows you to navigate through the currently active presets and select one to be applied to the current object. A shortcut button will be added to the TagEditor screen below the tags. The recently used preset screen can be completly emptied with the corresponding menu item, individual presets can be removed by a long press.

## ![](../images/tag_menu_repeat.png) Repeat last tags

Merge the tags from the last invocation of the TagEditor. Tags with the same keys will get the values from the previous invocation, tags not present will remain. 

## ![](../images/undolist_undo.png) Reset

Tapping the icon once will reset the tags to the values as they were when the TagEditor was started.

## ![](../images/tag_menu_sourcesurvey.png) Surveyed

Short cut for adding source=survey to the object. This is deprecated, the normal place to add source tags is in the corresponding field on the upload form. 

## ![](../images/tag_menu_mapfeatures.png) Map Features Wiki

Invoke a web browser on the map features page in the OpenStreetMap wiki. *(requires network connectivity)* 

## Add to relation

Add the current object to a existing relation in the download.

## Reset address prediction

Empty the current address cache and re-seed from downloaded OpenStreetMap data. 

## Reset Presets

Reset the most recently used preset list and remove short cuts from the tag editor screen. 

## ![](../images/menu_help.png) Help

Start the on device help browser.
