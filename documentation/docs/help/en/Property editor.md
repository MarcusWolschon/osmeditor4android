# Vespucci Property Editor

The property editor screen is the central place for changing object attributes and relation memberships. To exit and save your changes tap the Vespucci icon, pressing the back button will give you the option of exiting without saving. Tags with empty value fields will not be saved.

Most of the fields use context sensitive auto complete provided by the active presets. Clicking twice (with a short pause) will re-display the auto complete menu. 

Depending on size and orientation of your device the layout will change to make maximum use of the space available.
 
 * small devices: tabbed layout with _Presets_, _Tags_ (including the recently used presets) and _Relations_ tabs. If the selected object is a relation a further _Members_ tab will be available. 
 * large devices in portrait orientation: multi pane layout with a pane for the tabs, one for the _Recently Used Presets_ and for the _Presets_ screen 
 
If the property editor is started with multiple elements selected only the _Tags_ tab will be available. Tabs can be changed by swiping or by tapping the header.

In the tabs tapping the checkbox in the header row will select/de-select all elements.

## Tags Tab

Display of the key - value attributes of the edited object.

The following operations can be performed on selected tags

 * Delete - delete the tag from the current object
 * Copy - copy the tag to the internal clipboard and to the system clipboard in a text representation.
 * Cut - same as copy, but delete the tag from this object.


### Special auto complete behaviour

If the key of a field is "addr:street" a list of the nearest roads in the down loaded data will be provided.

If the key is "name", selection of a value from the auto complete list will add corresponding tags and if enabled in the settings, apply an appropriate preset. Typical use case: add a name key and start typing "mcd", select "McDonalds's" from the auto completion list and an amenity with the correct tags and name spelling will be added.

If multiple objects with different values for the same tags are being edited, such value fields will display 'Multiple values' and the auto-complete list will contain the existing values with a count. Setting the value to a specific value will set it for all objects, an empty value will leave the values untouched.

### ![](../images/address.png) Add address tags

Add address tags for the object. This function will try to predict the house number to suggest. It works best along straight roads with regular numbering schemes. 

### ![](../images/tag_menu_apply_preset.png) Apply best preset

Will locate the best matching preset to the existing tags and apply the preset adding further suggested fields.

### ![](../images/tag_menu_repeat.png) Paste

Paste and merge the tags from the internal clipboard. 

### ![](../images/undolist_undo.png) Revert

Tapping the icon once will reset the tags to the values as they were when the property editor was started.

### Paste from clipboard

Paste and merge multi-line text from the system clipboard. If the lines have the form _key_=_value_ the lines will be split at the = and pasted in to key and values fields, otherwise the complete lines will be copied to the value field. This function can be used for example with OCR apps. 

### ![](../images/tag_menu_sourcesurvey.png) Surveyed

Short cut for adding source=survey to the object. This is deprecated, the normal place to add source tags is in the corresponding field on the upload form. 

### ![](../images/tag_menu_mapfeatures.png) Map Features Wiki

Invoke a web browser on the map features page in the OpenStreetMap wiki. *(requires network connectivity)* 

### Reset address prediction

Empty the current address cache and re-seed from downloaded OpenStreetMap data. 

### Reset Presets

Reset the most recently used preset list and remove short cuts from the tag editor screen. 

### ![](../images/menu_help.png) Help

Start the on device help browser.

## Relations Tab

Display of the relations the edited object is a member of and the role in the relation.

The following operation can be performed on selected relations.

 * Delete - remove the edited object from this relation.

### ![](../images/undolist_undo.png) Revert

Tapping the icon once will reset the relations to the values as they were when the property editor was started.

### Add to relation

Add the current object to a existing relation in the download.

### ![](../images/menu_help.png) Help

Start the on device help browser.

## Members Tab

For relations only, display the members of the relation.

Members with a dark object field and only a numeric id displayed have not been downloaded.

The following operation can be performed on selected relation members.

 * Delete - remove the object from this (the edited) relation.
 
_There will likely be more operations added in the future, for example sorting_

### ![](../images/undolist_undo.png) Revert

Tapping the icon once will reset the members to the values as they were when the property editor was started.

### ![](../images/menu_help.png) Help

Start the on device help browser.

## Preset display

Find, select and apply a [preset](Presets.md). Groups of presets have a light background, presets a dark one. 

### Top

Go to the top of the preset hierarchy.

### Up

Go up one level.

### ![](../images/menu_help.png) Help

Start the on device help browser.
