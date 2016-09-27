# Multiselect

Selected elements can be moved by dragging in the touch area. Individual objects can be selected / de-selected by (single) tapping them. A double tap in an entry area will end the mode.

## Actions  

### ![Undo](../images/undolist_undo.png) Undo

Tapping the icon once will undo the last operation. A long press will display a list of the operations since the last save, if you have undone anything a corresponding "redo" action will be displayed. If the undo results in the current selected object being removed you will be returned to the main screen. *Some operations consist of multiple simpler actions that will be listed as individual items.*

### ![Properties](../images/tag_menu_tags.png) Properties

Starts the property editor on the current selection.

### ![Delete](../images/tag_menu_delete.png) Delete

Remove the object from the data.

### ![Merge](../images/tag_menu_merge.png) Merge ways

Merge multiple selected ways resulting in a single way. Ways will be reversed if necessary. This option will only be available if only ways with common start/end nodes are selected. In pre-merge tag conflicts are detected you will be asked to fix them first and the property editor will be started on the selection, the same post-merge. 

### ![Relation](../images/relation.png) Create relation

Create a relation starting with the current selected elements. Further objects can be added until "Done" is selected, then the tag editor will be started on the new relation. 

### ![Help](../images/menu_help.png) Help

Start the Vespucci Help browser