# Creating new objects in simple actions mode

Simple action mode replaces the [long click action](../en/Creating%20new%20objects.md) on the screen with a menu driven way of creating new objects. Long clicks are disabled as long as the mode is active.

The mode can be toggled on and off via an item in the main menu. 

Tapping the large green floating button with a "+" mark will show a menu. After you've select one of the items, you will be asked to tap the screen at the location where you want  to create the object, pan and zoom continues to work. 

Available actions depend on the current active overall mode:

## In Normal, Tag only, Indoor and C-Mode

### Add tagged node

Create a new OSM Node at the tapped position, then start the [property editor](../en/Property%20editor.md) with the preset tab open. 

The created Node is not automatically merged with nearby objects.

### Add way

Start a new OSM Way at the tapped position, tapping a further position will create the next node and so on. Behaviour is similar as described in [creating new objects](../en/Creating%20new%20objects.md). __Way Nodes will be merged with nearby objects.__

Besides tapping the "check mark" button to directly start the [property editor](../en/Property%20editor.md), you can

#### ![Snap](../images/snap_on.png)  ![Snap](../images/snap_off.png) Snap

When the snap function is active, newly created nodes near to other nodes and ways will snap on to these automatically creating connections. It the checkbox is not checked 
nodes will be created without merging with other objects. You can toggle this while you are drawing a way.

Note: when the checkbox is not checked you need to manually close closed ways, for example a building outline.

#### ![Preset](../images/tag_menu_preset.png) Add preset

Create the current new object, launch the [property editor](../en/Property%20editor.md) and immediately display the preset tab. In multi-pane mode this will simply start the property editor.

### ![Follow](../images/follow.png) Follow way

If two consecutive nodes overlap with nodes of an existing way you can follow the existing way to one of its nodes after clicking the button.

#### ![Address](../images/address.png) Add address tags

Adds address tags with prediction. If the way is closed this will add the predicted address tags and start the [property editor](../en/Property%20editor.md). If the way is not closed an address interpolation is created and a dedicated editor is started.

### Add map note

Create a new OSM map note at the tapped position, starting the note editor. 

### Add node

Create a new untagged OSM Node at the tapped position, merging with nearby objects. This is for example useful if you simply want to add a Node to an existing Way.

### Paste object

If an OSM object has been copied or cut to the clipboard paste it at the tapped position and then select it. This menu item will only be shown if there is something in the clipboard. 

There are a total of 5 clipboards that can be used, cutting or copying will create a new clipboard that will be used as the default. If the limit of 5 clipboards is reached, the "oldest" one will be deleted. If you want to use a previously created clipboard instead of the most recent one, clicking on the button in the menu will move it to the "top". If a preset can be determined for the items in the clipboard, an icon will be displayed instead of the clipboard number.

### Paste multiple times

If an OSM object has been copied to the clipboard paste it at the tapped position and remain in the same mode allowing to be pasted repeatedly. The mode can then be exited by pressing the back button or the back arrow in the title bar. This menu item will only be shown if there is something in the clipboard. 

See above for clipboard selection.

## In Address mode

### Add address node

Adds a node at the clicked location and adds address tags with prediction. If the node is part of a building way it will further add "entrance=yes" if not present.

### Add address interpolation

Create an address interpolation and start the dedicated editor. 

Notes: 

- closed ways do not make sense for address interpolations.
- the start and end address nodes are added at the end of the way, any intermediate address nodes are ignored.
### Add map note

Create a new OSM map note at the tapped position, starting the note editor. 

