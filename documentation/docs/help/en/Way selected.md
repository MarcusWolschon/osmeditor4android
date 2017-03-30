# Way selected

Way selection is described in the [Introduction](../en/Introduction.md). Selected ways can be moved by dragging. You can also edit tags for selected ways.

## Actions  

Note: some of these actions will only be visible if they can actually be executed on the selected way. For example you can only split a way that has more than two way nodes.

### ![Undo](../images/undolist_undo.png) Undo

Tapping the icon once will undo the last operation. A long press will display a list of the operations since the last save, if you have undone anything a corresponding "redo" action will be displayed. If the undo results in the current selected object being removed you will be returned to the main screen. *Some operations consist of multiple simpler actions that will be listed as individual items.*

### ![Properties](../images/tag_menu_tags.png) Properties

Starts the property editor on the current selection.

### ![Address](../images/address.png) Add address tags

Adds address tags with prediction. Only visible if way is tagged as building.

### ![Reverse](../images/tag_menu_reverse.png) Reverse way

Reverse the direction of the way. If the way has direction dependent tags it will automatically change them to still have the same effect as before the direction change. oneway tags are not automatically reversed here the assumption is that the intent was to change the effect of the tag, the tag editor will be started automatically and you can change the tag manually if necessary. Direction dependent tags currently handled are:
				
* direction
* incline
* turn:lanes and turn
* \*:left, \*:right, \*:left:\*, \*:right:\*, left and right
* \*:backward, \*:forward, \*:backward:\*, \*:forward:\*, backward and forward
* forward and backward roles in relations the selected way is a member of

### ![Split](../images/tag_menu_split.png) Split

Split the selected way. The nodes at the available locations for splitting will have a visible touch area. The "Split" action is only available if the way has more than two nodes. If the way is closed you will need to select two nodes.

### ![Merge](../images/tag_menu_merge.png) Merge

Join the selected way with one that it shares a node with resulting in a single way. This option will only be available if the ways have compatible tags and relation memberships.

### ![Append](../images/tag_menu_append.png) Append 

Append to the current selected way. Touch areas will be visible around nodes that can be used to extend the way, select "Done" or the same node twice to stop. The "Append" action is only available if the way in question is not closed.

### ![TurnRestriction](../images/no_left_turn_light.png) Add turn restriction 

Start creating a turn restriction with this way as "from" member. This action is only available if at least one "via" node or way and a corresponding "to" way exists.

### ![Orthogonalize](../images/menu_ortho.png) Orthogonalize

Change angles that are near 90° or 180° to 90° resp. 180°.

### ![Rotate](../images/ic_menu_rotate.png) Rotate

Rotate the way around its centroid. The centroid position is marked with a cross.

### Split in to polygons

Available for closed ways with at least four nodes. Will split the way in to two polygons, sharing the common nodes.

### ![Copy](../images/ic_menu_copy_holo_light.png) Copy

Copy the way to the internal copy and paste buffer.

### ![Cut](../images/ic_menu_cut_holo_light.png) Cut

Move the way to the internal copy and paste buffer removing it fron the data.

### ![Delete](../images/tag_menu_delete.png) Delete

Remove the object from the data.

### ![Extend](../images/extend_selection.png) Extend selection

Start Multi-Select mode with the current selected element.

### ![Relation](../images/relation.png) Create relation

Create a relation and add this object as the first element. Further objects can be added until "Done" is selected, then the tag editor will be started on the new relation. 

### ![Info](../images/tag_menu_mapfeatures.png) Info

Show a screen with some detailed information on the selected object.

### ![Help](../images/menu_help.png) Help

Start the Vespucci Help browser
