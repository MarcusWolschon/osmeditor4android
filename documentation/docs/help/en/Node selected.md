# Node selected

Node selection is described in the [Introduction](../en/Introduction.md). Selected nodes can be moved by dragging in the touch area, you can enable a large drag area in the preferences. You can edit tags for selected nodes.

## Actions  

Note: some of these actions will only be visible if they can actually be executed on the selected node. For example you can only merge a node with a way or node if the selected node is in their tolerance zone.

### ![Undo](../images/undolist_undo.png) Undo

Tapping the icon once will undo the last operation. A long press will display a list of the operations since the last save, if you have undone anything a corresponding "redo" action will be displayed. If the undo results in the current selected object being removed you will be returned to the main screen. *Some operations consist of multiple simpler actions that will be listed as individual items.*

### ![Properties](../images/tag_menu_tags.png) Properties

Starts the property editor on the current selection.

### ![Append](../images/tag_menu_append.png) Append 

Add to the way this node is part of. The "Append" action is only available if the node in question is an end node of a way.

### ![Unjoin](../images/tag_menu_split.png) Unjoin 

Create an additional node at this location and use it instead of the original node in one of the ways. The "Unjoin" action is only available if the node in question is an end node of two ways.

### ![Merge](../images/tag_menu_merge.png) Merge 

Merge the selected node with a way (as a new way node) or with an existing node. To make this tool appear in toolbar, select a node and move it next to a way.

### ![Extract](../images/extract_node.png) Extract node

Extract the selected node from all ways that it is a member of and replace it with a newly created one. The original node retains all tags and relation memberships and remains selected. After extracting, drag the node to its new position.  

### ![Copy](../images/ic_menu_copy_holo_light.png) Copy

Copy the way to the internal copy and paste buffer.

### ![Cut](../images/ic_menu_cut_holo_light.png) Cut

Move the way to the internal copy and paste buffer removing it fron the data.

### ![Delete](../images/tag_menu_delete.png) Delete

Remove the object from the data.

### ![Extend](../images/extend_selection.png) Extend selection

Start Multi-Select mode with the current selected element.

### ![Relation](../images/relation.png) Create relation

Create a relation and add this object as the first element. Further objects can be added until "Done" is selected, then the property editor will be started on the new relation. 

### ![Position](../images/menu_gps.png) Position

Manually set longitude and latitude values for the coordinates of this node.

### ![Info](../images/tag_menu_mapfeatures.png) Info

Show a screen with some detailed information on the selected object.

### Zoom to selection

Pan and zoom the map to the currently selected object.

### Share position

Share the position of the selected object with other apps on the device.

### ![Preferences](../images/menu_config.png) Preferences

Show the user preference screens. The settings are split into two sets: the first screen contains the more commonly used preferences, the "Advanced preferences" contains the less used ones. 

### ![Help](../images/menu_help.png) Help

Start the Vespucci Help browser
