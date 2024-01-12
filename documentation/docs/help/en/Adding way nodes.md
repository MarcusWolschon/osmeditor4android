# Adding way nodes

This mode can be entered via the simple mode __Add way__ action, via a long click otherwise, and via the __Append__ action in way and node selection modes. 

You can then do one of three things

* **touch in the same place again** - this will finish adding nodes to the way and start the property editor. If the location is within the tolerance zone of a way it will be added as a new node to that way.
* **touch some other place on the screen** - this will create a way segment from the previous location to the current location, the way can be extended by adding further nodes. Touching the last location completes the way and starts the tag editor. If the initial location was near a node that will be used as the first node, and if it was on a way, a new node will be inserted in to that way.
* **select a menu item** see below

The complete way can be moved without leaving this mode.

## Further actions  

### ![Undo](../images/undolist_undo.png) Undo

Tapping the icon once will undo the last addition of a node.

### ![Snap](../images/snap_on.png)  ![Snap](../images/snap_off.png) Snap

When the snap function is active, newly created nodes near to other nodes and ways will snap on to these automatically creating connections. It the checkbox is not checked 
nodes will be created without merging with other objects. You can toggle this while you are drawing a way.

Note: when the checkbox is not checked you need to manually close closed ways, for example a building outline.

#### ![Preset](../images/tag_menu_preset.png) Add preset

Create the current new object, launch the [property editor](../en/Property%20editor.md) and immediately display the preset tab. In multi-pane mode this will simply start the property editor.

### ![Follow](../images/follow.png) Follow way

If two consecutive nodes overlap with nodes of an existing way you can follow the existing way to one of its nodes after clicking the button.

### ![Address](../images/address.png) Add address tags

Adds address tags with prediction. If the way is closed this will add the predicted address tags and start the [property editor](../en/Property%20editor.md). If the way is not closed an address interpolation is created and a dedicated editor is started.

### ![Help](../images/menu_help.png) Help

Start the Vespucci Help browser.
