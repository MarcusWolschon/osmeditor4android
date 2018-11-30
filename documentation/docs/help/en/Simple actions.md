# Creating new objects in simple actions mode

Simple action mode replaces the [long click action](../en/Creating%20new%20objects.md) on the screen with a menu driven way of creating new objects. Long clicks are disabled as long as the mode is active.

The mode can be toggled on and off via an item in the main menu.

Tapping the large green floating button will show a menu. After you've select one of the items, you will be asked to tap the screen at the location where you want  to create the object, pan and zoom continues to work. The possible actions are: 

### Add tagged node

Create a new OSM Node at the tapped position, then start the [property editor](../en/Property%20editor.md) with the preset tab open. 

The created Node is not automatically merged with nearby objects.

### Add way

Start a new OSM Way at the tapped position, tapping a further position will create the next node and so on. Behaviour is the same as described in [creating new objects](../en/Creating%20new%20objects.md). __Way Nodes will be merged with nearby objects.__

### Add map note

Create a new OSM map note at the tapped position, starting the note editor. 

### Add node

Create a new untagged OSM Node at the tapped position, merging with nearby objects. This is for example useful if you simply want to add a Node to an existing Way.

### Paste object

If an OSM object has been copied or cut to the clipboard paste it at the tapped position and then select it. This menu item will only be shown if there is something in the clipboard.

### Paste multiple times

If an OSM object has been copied or cut to the clipboard paste it at the tapped position and remain in the same mode allowing to be pasted repeatedly. The mode can then be exited by pressing the back button or the back arrow in the title bar.. This menu item will only be shown if there is something in the clipboard.


