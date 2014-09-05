# Way selected

Selected ways can be moved by dragging. Note dragging the "geometry improvement" marker in the middle will automatically add a node to the way.

## Actions  

### ![](../images/tag_menu_tags.png) Start tag editor

Starts the tag editor on the current selection.

### ![](../images/address.png) Add address tags

Adds address tags with prediction. Only visible if way is tagged as building.

### ![](../images/tag_menu_reverse.png) Reverse way

Reverse the direction of the way. If the way has direction dependent tags it will automatically change them to still have the same effect as before the direction change. oneway tags are not automatically reversed here the assumption is that the intent was to change the effect of the tag, the tag editor will be started automatically and you can change the tag manually if necessary. Direction dependent tags currently handled are:
				
* direction
* incline
* turn:lanes and turn
* \*:left, \*:right, \*:left:\*, \*:right:\*, left and right
* \*:backward, \*:forward, \*:backward:\*, \*:forward:\*, backward and forward

### ![](../images/tag_menu_split.png) Split

Split the selected way, the nodes at the available locations for splitting will have a visible touch area. The "Split" action is only available if the way has more than two nodes. If the way is closed you will need to select two nodes.

### ![](../images/tag_menu_merge.png) Join

Join the selected way with one that it shares a node with resulting in a single way. This option will only be available if the ways have compatible tags and relation memberships.

### ![](../images/tag_menu_append.png) Append 

Append to the current selected way. Touch areas will be visible around nodes that can be used to extend the way, select "Done" or the same node twice to stop. The "Append" action is only available if the way in question is not closed.

### ![](../images/menu_ortho.png) Orthogonalize

Change angles that are near 90� or 180� to 90� resp. 180�.

### ![](../images/ic_menu_rotate.png) Rotate

Rotate the way around its centroid. The centroid position is marked with a cross.

### ![](../images/ic_menu_copy_holo_light.png) Copy

Copy the way to the internal copy and paste buffer.

### ![](../images/ic_menu_cut_holo_light.png) Cut

Move the way to the internal copy and paste buffer removing it fron the data.

### ![](../images/tag_menu_delete.png) Delete

Remove the object from the data.

### ![](../images/relation.png) Create relation

Create a relation and add this object as the first element. Further objects can be added until "Done" is selected, then the tag editor will be started on the new relation. 

### ![](../images/menu_help.png) Help

Start the Vespucci Help browser