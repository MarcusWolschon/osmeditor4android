# Node selected

Node selection is described in the [Introduction](../en/Introduction.md). Selected nodes can be moved by dragging in the touch area, you can enable a large drag area in the preferences. You can edit tags for selected nodes.

## Actions  

Note: some of these actions will only be visible if they can actually be executed on the selected node. For example you can only merge a node with a way or node if the selected node is in their tolerance zone.

### ![Undo](../images/undolist_undo.png) Undo

Tapping the icon once will undo the last operation. A long press will display a list of the operations since the last save, if you have undone anything a corresponding "redo" action will be displayed. If the undo results in the current selected object being removed you will be returned to the main screen. *Some operations consist of multiple simpler actions that will be listed as individual items.*

### ![Properties](../images/tag_menu_tags.png) Properties

Starts the property editor on the current selection.

### ![Todos](../images/tag_menu_bug.png) Todos

If the selected element is associated either with a todo or osmose task this menu will be displayed.

Available actions are

#### Close and goto next

Close a todo associated with the element and goto the nearest open todo in the list. If the element is present in multiple todo list a modal will ask which one to choose.

#### Skip and goto next

Skip a todo associated with the element and goto the nearest open todo in the list. If the element is present in multiple todo list a modal will ask which one to choose.

#### Close all

Close all todos and osmose bugs associated with the element. 

### ![Address](../images/address.png) Add address tags

Adds address tags with prediction. If the node is part of a building way it will further add "entrance=yes" if not present.

### ![Append](../images/tag_menu_append.png) Append 

Add to the way this node is part of. The "Append" action is only available if the node in question is an end node of a way.

### ![Unjoin](../images/tag_menu_split.png) Unjoin 

If a way node is selected that is a member of multiple ways this will create additional nodes at the location and use them instead of the original node in the ways. 

### ![Merge](../images/tag_menu_merge.png) Merge 

Merge the selected node with a way (as a new way node) or with an existing node. To make this tool appear in the toolbar, select a node and move it next to a way. If there are multiple possible target nodes or ways a disambiguation context menu will be shown.

### ![Extract](../images/extract_node.png) Extract node

Extract the selected node from all ways that it is a member of and replace it with a newly created one without any tags at all. The original node retains all tags and relation memberships and remains selected. After extracting, drag the original node to its new position.
The above might seem very confusing but in the end the new mode is in the original place and the original node is in the new place!

### ![TurnRestriction](../images/no_left_turn_light.png) Add turn restriction 

Start creating a turn restriction with this node as "via" member. This action is only available if the node in question is a potential via member, that is it is a member of at least two ways that have a "highway" tag. Ways will be split automatically during the process.

### ![Rotate](../images/ic_menu_rotate.png) Rotate

If the node has a direction tag with a degree value, rotate the node by dragging the display roughly in a circle.

### ![Copy](../images/ic_menu_copy_holo_light.png) Copy

Copy the node to the internal copy and paste buffer.

### ![Duplicate](../images/content_duplicate_light.png) Duplicate

Create a copy of the selected node in the same location. This does not utilize the copy and paste buffer.

### ![Cut](../images/ic_menu_cut_holo_light.png) Cut

Move the node to the internal copy and paste buffer removing it from the data.

### Paste tags

Set tags from the internal tag clipboard and start the property editor.

### ![Delete](../images/tag_menu_delete.png) Delete

Remove the object from the data.

### ![Extend](../images/extend_selection.png) Extend selection

Start Multi-Select mode with the current selected element.

### ![Relation](../images/relation.png) Create relation

Create a relation and add this object as the first element. The relation type can be selected from a list generated from the available presets. Further objects can be added until the check button is clicked, then the tag editor will be started on the new relation. 

### Add to relation

Select a relation and add this object as a member. Further objects can be added until the check button is clicked, then the tag editor will be started on the relation. 

### ![Position](../images/menu_gps.png) Position

Manually set longitude and latitude values for the coordinates of this node.

### ![Info](../images/tag_menu_mapfeatures.png) Info

Show a screen with some detailed information on the selected object.

### Zoom to selection

Pan and zoom the map to the currently selected object.

### Search for objects

Search for OSM objects in the loaded data using JOSMs search expressions.

### Add to todo list

Add the current selection to an existing or new todo list.

### Replace geometry

Move the tags of current node to a (in a second step) selected way and merge the current node in to one of the target ways to preserve history.

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
