# Vespucci Property Editor

The property editor screen is the central place for changing object attributes and relation memberships. To exit and save your changes tap the check mark icon in the upper left hand corner, pressing the device back button will give you the option of exiting without saving. Tags with empty value fields will not be saved.

Many of the fields use context sensitive auto complete provided by the active presets and previously used tags. Clicking twice (with a short pause) will re-display the auto complete menu. 
 
Depending on size and orientation of your device the layout will change to make maximum use of the space available.
 
 * small devices: tab layout with _Presets_, _Properties_ (including the recently used presets), _Details_ and _Relations_ tabs. If the selected object is a relation a further _Members_ tab will be available. 
 * large devices in portrait orientation: multi pane layout with a pane for the tabs, one for the _Recently Used Presets_ and for the _Presets_ screen 
 
If the property editor is started with multiple elements selected only the _Details_ and _Presets_ tabs will be available. Tabs can be changed by swiping or by tapping the header.

If the _Display tag form_ preference in the _[Advanced preferences](Advanced%20preferences.md)_ has been disabled pre-0.9.8 behaviour is enabled and the _Details_ tab will have the heading _Properties_.

In the tabs tapping the checkbox in the header row will select/de-select all elements.

## Properties Tab

This tab gives a simplified, preset-driven, editing screen for the tags for the selected object. Instead of displaying the raw keys, they are represented by their description, which, if available will be translated. To remove individual attributes you can simply remove or reset the value (or delete them in the _Details_ tab).

Fields are added by applying presets, either via the _Preset_ tab or via the ![Apply preset](../images/preset.png) _Apply best preset_ or ![Apply preset with optional tags](../images/tag_menu_apply_preset.png) _Apply best preset with optional tags_ buttons. The later will add fields for any optional tags that are present in the preset.

If alternative tagging is available for the main object, the alternative presets will be displayed before those in the most recently used list. Long pressing on an item in the MRU preset view will display a menu allowing to

* Set _Apply with last values_, after setting this, further use of the preset will use the most recently used tag values as defaults. If this is not set (_default_) the use of the last value is determined by the *use_last_as_default* attribute in the preset.
* Set _Apply with optional tags_, after setting this, optional tags will be added when you apply the preset to an object, the same behaviour as pressing the ![Apply preset with optional tags](../images/tag_menu_apply_preset.png), but this will work when auto-applying the best matching preset on startup of the property editor too.
* _Remove_ remove the item from the MRU preset view. Note that the above settings are persistent and will remain in force even if the preset item is removed.  

Further behaviour and menu items are similar or the same as for the _Details_ tab, the _Add language tags_ menu item will add local language variants of the name and similar keys to the preset if configured for the region (see [the GeoContext repository](https://github.com/simonpoole/geocontext)).

## Details Tab
 
Display of the key - value attributes of the edited object.

The following operations can be performed on selected tags

 * Delete - delete the tag from the current object
 * Copy - copy the tag to the internal clipboard and to the system clipboard in a text representation.
 * Cut - same as copy, but delete the tag from this object.
 * Create custom preset - select the tags you want to include, then select "Create custom preset" from the menu and enter a name when you are prompted. The new preset can then be found in the "Auto-preset" group and will be added to the MRU list once applied. The function does not include the values for tags that have _name_ semantics, and will set the current value for combo and multi-select fields as the default value.


#### Special auto complete behaviour

If the key of a field is "addr:street" a list of the nearest roads in the downloaded data will be provided.

If the key is "name", selection of a value from the auto complete list will add corresponding tags and if enabled in the settings, apply an appropriate preset. Typical use case: add a name key and start typing "mcd", select "McDonalds's" from the auto completion list and an amenity with the correct tags and name spelling will be added.

If multiple objects with different values for the same tags are being edited, such value fields will display 'Multiple values' and the auto-complete list will contain the existing values with a count. Setting the value to a specific value will set it for all objects, an empty value will leave the values untouched.

### ![Address](../images/address.png) Add address tags

Add address tags for the object. This function will try to predict the house number to suggest. It works best along straight roads with regular numbering schemes. 

### ![Apply preset](../images/preset.png) Apply best preset

Apply the best matching preset to the existing tags.

### ![Apply preset with optional tags](../images/tag_menu_apply_preset.png) Apply best preset with optional tags

Will locate the best matching preset to the existing tags and apply the preset adding further suggested / optional fields.

### ![Paste](../images/ic_menu_paste_holo_light.png) Paste

Paste and merge the tags from the internal clipboard. 

### ![Revert](../images/undolist_undo.png) Revert

Tapping the icon once will reset the tags to the values as they were when the property editor was started.

### Paste from clipboard

Paste and merge multi-line text from the system clipboard. If the lines have the form _key_=_value_ the lines will be split at the = and pasted in to key and values fields, otherwise the complete lines will be copied to the value field. This function can be used for example with OCR apps. 

### ![Surveyed](../images/tag_menu_sourcesurvey.png) Surveyed

Short cut for adding a source key, if a key is present in the current row source:_key_=survey will be added, otherwise source=survey to the object. Such tags are in general deprecated the normal place to add source tags is in the corresponding field on the upload form. The action is only available on the _Details_ tab.

### ![Wiki](../images/tag_menu_mapfeatures.png) Map Features Wiki

Invoke a web browser on the map features page in the OpenStreetMap wiki. *(requires network connectivity)* 

### Reset address prediction

Empty the current address cache and re-seed from downloaded OpenStreetMap data. 

### Reset Presets

Reset the most recently used preset list and remove short cuts from the tag editor screen. 

### ![Help](../images/menu_help.png) Help

Start the on device help browser.

## Relations Tab

Display of the relations the edited object is a member of and the role in the relation.

The following operation can be performed on selected relations.

 * Delete - remove the edited object from this relation.

### ![Revert](../images/undolist_undo.png) Revert

Tapping the icon once will reset the relations to the values as they were when the property editor was started.

### Add to relation

Add the current object to a existing relation in the download.

### ![Help](../images/menu_help.png) Help

Start the on device help browser.

## Members Tab

This tab will only be displayed if you are editing a relation, it displays entries for all the members of the relation.

Members with a dark object field and only a numeric id displayed have not been downloaded, member highlighted in blue can be clicked on and a new property editor will be opened for the element. Note: during such a drill down operation the property editors are stacked, if you close a stacked property editor you will be returned to the previous one and so on.

The following operation can be performed on selected relation members.

 * _Delete_ - remove the object from this (the edited) relation.
 * _Move up_ - move the member up one position.
 * _Move down_ - move the member down one position.
 * _Sort_ - sort ways so that they are connected, nodes and relations retain the order that they have and are placed above the ways.
 * _Reverse order_ - reverse the order of the selected members.
 * _Move to top_ - move the member to the top of the list.
 * _Move to bottom_ - move the member to the bottom of the list.
 * _Download_ - download the selected members.  if all members are selected the members will be downloaded including ways that are parents of nodes that are a relation member, otherwise the individual members are downloaded and for nodes the parent ways are **not** downloaded.

### ![Revert](../images/undolist_undo.png) Revert

Tapping the icon once will reset the members to the values as they were when the property editor was started.

### Scroll to Top

Scroll the screen to the top.

### Scroll to Bottom

Scroll the screen to the bottom.

### ![Help](../images/menu_help.png) Help

Start the on device help browser.

## Preset tab

Find, select and apply a [preset](Presets.md). Groups of presets have a light background, presets a dark one. The on device presets can be searched for terms, if enabled and network connectivity is available you can try and construct additional presets by querying the Taginfo service, when the results from the internal search are displayed.

Presets in the "Auto-presets" group, that is such generated from taginfo or custom presets, can be removed from the configuration by long-pressing the respective icon.

### Top

Go to the top of the preset hierarchy.

### Up

Go up one level.

### ![Help](../images/menu_help.png) Help

Start the on device help browser.

## Split window mode

On devices running Android 7 and later you can enable an __experimental__ split window mode in the [Experimental](Advanced%20preferences.md) section of the preferences. When this is enabled and the screen is split (on Android 12 and later this will happen automatically), the property editor will be displayed in parallel with the map display. If you click on a relation member in the _Member Tab_ the element will be selected and the map display will zoom to it. While this will work both on phones and tablets, the usefulness on the former is limited. 

Changes to the data made in the map display will be reflected as far as possible in the property editor, for example changes if relation membership, deletions and so on, note however that currently later changes will override changes made in earlier property editor instances.
