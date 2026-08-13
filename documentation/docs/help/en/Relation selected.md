# Relation selected

Selected nodes can be moved by dragging in the touch area, you can enable a large drag area in the preferences.

## Actions  

Note: some of these actions will only be visible if they can actually be executed on the selected relation.

### ![Undo](../images/undolist_undo.png) Undo

Tapping the icon once will undo the last operation. A long press will display a list of the operations since the last save, if you have undone anything a corresponding "redo" action will be displayed. If the undo results in the current selected object being removed you will be returned to the main screen. *Some operations consist of multiple simpler actions that will be listed as individual items.*

### ![Properties](../images/tag_menu_tags.png) Properties

Starts the tag editor on the current selection.

### ![Todos](../images/tag_menu_bug.png) Todos

If the selected element is associated either with a todo or osmose task this menu will be displayed.

Available actions are

#### Close and goto next

Close a todo associated with the element and goto the nearest open todo in the list. If the element is present in multiple todo list a modal will ask which one to choose.

#### Skip and goto next

Skip a todo associated with the element and goto the nearest open todo in the list. If the element is present in multiple todo list a modal will ask which one to choose.

#### Close all

Close all todos and osmose bugs associated with the element. 

### ![Add](../images/relation_add_member.png) Add member 

Add further objects to the relation until "Done" is selected, then the tag editor will be started on the new relation. 

### ![RelationMembers](../images/relation_members.png) Select relation members 

Start Multi-Select mode with all members of the current relation selected. The relation itself will be deselected. 

### ![Rotate](../images/ic_menu_rotate.png) Rotate

If the relation is a multi-polygon, rotate it around its centroid by dragging the display roughly in a circle. The centroid position is marked with a cross.

### ![Copy](../images/ic_menu_copy_holo_light.png) Copy

Copy the selected relation to the internal copy and paste buffer. Only available for multi-polygon relations.

### ![Duplicate](../images/content_duplicate_light.png) Duplicate

Create a copy of the selected relation in the same location. Only available for multi-polygon relations. This does not utilize the copy and paste buffer.

### Shallow duplicate

Create a copy of the selected relation with the same member elements. This does not utilize the copy and paste buffer.

### ![Delete](../images/tag_menu_delete.png) Delete

Remove the object from the data.

### Paste tags

Set tags from the internal tag clipboard and start the property editor.

### ![Extend](../images/extend_selection.png) Extend selection

Start Multi-Select mode with the current selected element.

### ![Relation](../images/relation.png) Create relation

Create a relation and add this object as the first element. The relation type can be selected from a list generated from the available presets. Further objects can be added until the check button is clicked, then the tag editor will be started on the new relation. 

### Add / remove member

Objects can be added to or removed from the relation until the check button is clicked, then the property editor will be started on the relation. 

### ![Info](../images/tag_menu_mapfeatures.png) Info

Show a screen with some detailed information on the selected object. 

### Zoom to selection

Pan and zoom the map to the currently selected object.

### Search for objects

Search for OSM objects in the loaded data using JOSMs search expressions.

### Add to todo list

Add the current selection to an existing or new todo list.

### Add new image

Start the camera app and upload the resulting image, adding an appropriate tag to the OSM element. Currently Panoramax and Wikimedia Commons targets are supported, 
you do need to configure and authorize access to the servers in the [Advanced preferences](Advanced%20preferences.md#image-storage).

### Add existing image

As [Add new image](#add-new-image) but allows you to select an existing image for upload.

### Upload element

Upload the selected element (only available for a new or modified element).

### Share position

Share the position of the selected object with other apps on the device.

### ![Preferences](../images/menu_config.png) Preferences

Show the user preference screens. The settings are split into two sets: the first screen contains the more commonly used preferences, the "Advanced preferences" contains the less used ones. 

### ![Help](../images/menu_help.png) Help

Start the Vespucci Help browser
