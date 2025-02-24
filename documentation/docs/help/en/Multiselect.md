# Multiselect

Multiple elements can be selected by either selecting an element and then using the _Extend selection_ entry from the menu, or by double-clicking an element. Selected elements can be moved by dragging in the touch area. Individual objects can be selected / de-selected by (single) tapping them. A double tap in an empty area will end the mode.

## Actions  

### ![Undo](../images/undolist_undo.png) Undo

Tapping the icon once will undo the last operation. A long press will display a list of the operations since the last save, if you have undone anything a corresponding "redo" action will be displayed. If the undo results in the current selected object being removed you will be returned to the main screen. *Some operations consist of multiple simpler actions that will be listed as individual items.*

### ![Properties](../images/tag_menu_tags.png) Properties

Starts the property editor on the current selection.

### ![Rotate](../images/ic_menu_rotate.png) Rotate

Rotate the selection around its centroid by dragging the display roughly in a circle. The centroid position is marked with a cross.

### Paste tags

Set tags from the internal tag clipboard and start the property editor.

### ![Copy](../images/ic_menu_copy_holo_light.png) Copy

Copy the selected objects  to the internal copy and paste buffer.

### ![Cut](../images/ic_menu_cut_holo_light.png) Cut

Move the the selected objects to the internal copy and paste buffer effectively deleting them from the data.

### ![Delete](../images/tag_menu_delete.png) Delete

Remove the objects from the data.

### ![Merge](../images/tag_menu_merge.png) Merge ways

Merge multiple selected ways resulting in a single way. Ways will be reversed if necessary. This option will only be available if only ways with common start/end nodes are selected, or the selection is two closed ways (polygons), in the later case if the polygons do not have common nodes a multi-polygon relation will be created and the ways added as members. If post-merge tag conflicts are detected you will be alerted. 

### Extract segment

If you have selected exactly two nodes on the same way, you can extract the segment of the way between the two nodes. If the way is closed the segment extracted will between the first and 2nd node selected in the winding direction (clockwise or counterclockwise) of the way.

If the way has _highway_ or _waterway_ tagging a number of shortcuts will be displayed, for example to change a _footway_ in to _steps_.   

### Add node at intersectionn

If two or more ways are selected and they intersect without a common node, a new node will be added at the first intersection found.

### Create circle

Creates a circle if at least three nodes are selected. If you select the nodes in clockwise/counterclockwise direction the resulting way will turn clockwise/counterclockwise. Nodes are spaced roughly 2 meters apart for larger circles. If additional nodes are too near to the original ones they will not be added. Note that if the selected nodes do not form a valid polygon, the behaviour of the function is undefined.

### ![Relation](../images/relation.png) Create relation

Create a relation starting with the current selected elements. The relation type can be selected from a list generated from the available presets. Further objects can be added until the check button is clicked, then the tag editor will be started on the new relation. 

### Add to relation

Select a relation and add the selected objects as members. Further objects can be added until the check button is clicked, then the tag editor will be started on the relation. 

### Zoom to selection

Pan and zoom the map to the currently selected object.

### Search for objects

Search for OSM objects in the loaded data using JOSMs search expressions.

### Add to todo list

Add the current selection to an existing or new todo list.

### Upload elements

Upload the selected elements (only available for new or modified elements).

### ![Help](../images/menu_help.png) Help

Start the Vespucci Help browser