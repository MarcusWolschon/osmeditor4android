# Vespucci Introduction

Vespucci is a full featured OpenStreetMap editor that supports most operations that desktop editors provide. It has been tested successfully on Android 2.3 to 4.4. A word of caution: while mobile devices capabilities have caught up with their desktop rivals, particularly older devices have very limited memory available and tend to be rather slow. You should take this in to account when using Vespucci and keep, for example, the size of the areas you are editing to a reasonable size. 

## First time use

On start up Vespucci shows you the "Download other location"/"Load Area" dialog. If you have coordinates displayed and want to download immediately, you can select the appropriate option and set the radius around the location that you want to download, otherwise you can simply go to the map and zoom and pan to the area you want to edit. Do not select a large area on slow devices. 

Alternatively you can dismiss the dialog by pressing the back button and pan and zoom to a location you want to edit and download the data then (see below: "Editing with Vespucci").

## Editing with Vespucci

Depending on screen size and age of your device editing actions may either be accessible directly via icons in the top bar, via a drop down menu on the right of the top bar, or via the menu key.

### Downloading OSM Data

Select either the transfer icon ![](../images/menu_transfer.png)  or the "Transfer" menu item. This will display seven options:

 * **Download current view** - download the area visible on the screen and replace any existing data *(requires network connectivity)*
 * **Add current view to download** - download the area visible on the screen and merge it with existing data *(requires network connectivity)*
 * **Download other location** - shows a form that allows you to enter coordinates, search for a location or enter coordinates directly, and then download an area around that location *(requires network connectivity)*
 * **Upload data to OSM server** - upload edits to OpenStreetMap *(requires authentication)* *(requires network connectivity)*
 * **Export changes** - write a ".osc" format file containing the current edits, this can be read for example by JOSM
 * **Read from file** - read a (J)OSM compatible XML format file
 * **Save to file** - save as a JOSM compatible XML format file

The easiest way to open a map is to zoom and pan to the location you want to edit and then to select "Download current view". You can zoom by using gestures, the zoom buttons or the volume control buttons on the telephone.  Vespucci should download data for the area and center the map on your current location. No authentication is required for downloading data to your device.

### Editing

To avoid accidental edits Vespucci starti in "locked" mode, a mode that only allows zooming and moving the map. Tap the ![Locked](../images/locked.png) icon to unlock the screen.
 
By default, selectable nodes and ways have an orange area around them indicating roughly where you have to touch to select an object. If you try to select an object and Vespucci determines that the selection could mean multiple objects it will present a selection menu. Selected objects are highlighted in yellow.

It is a good strategy to zoom in if you attempt to edit a high density area.

Vespucci has a good "undo/redo" system so don't be afraid of experimenting on your device, however please do not upload and save pure test data.

#### Selecting / De-selecting

Touch an object to select and highlight it, a second touch on the same object opens the tag editor on the element. Touching the screen in an empty region will de-select. If you have selected an object and you need to select something else, simply touch the object in question, there is no need to de-select first. 

#### Adding a new Node/Point or Way

Long press where you want the node to be or the way to start. You will see a black "cross hairs" symbol. Touching the same location again creates a new node, touching a location outside of the touch tolerance zone will add a way segment from the original position to the current position. 

Simply touch the screen where you want to add further nodes of the way. To finish, touch the final node twice. If the initial node is located on a way, the node will be inserted into the way automatically.

#### Moving a Node or Way

Objects can be dragged/moved only when they are selected. If you select the large drag area in the preferences, you get a large area around the selected node that makes it easier to position the object. 

#### Improving Way Geometry

If you zoom in far enough you will see a small "x" in the middle of way segments that are long enough. Dragging the "x" will create a node in the way at that location. Note: to avoid accidentally creating nodes, the touch tolerance for this operation is fairly small.

#### Cut, Copy & Paste

You can copy or cut selected nodes and ways, and then paste once or multiple times to a new location. Cutting will retain the osm id and version. To paste long press the location you want to paste to (you will see a cross hair marking the location). Then select "Paste" from the menu.

#### Efficiently Adding Addresses

Vespucci has an "add address tags" function that tries to make surveying addresses more efficient. It can be selected 

* after a long press: Vespucci will add a node at the location and make a best guess at the house number and add address tags that you have been lately been using. If the node is on a building outline it will automatically add a **entrance=yes"" tag to the node. The tag editor will open for the object in question and let you make any further changes.
* in the node/way selected modes: Vespucci will add address tags as above and start the tag editor.
* in the tag editor.

House number prediction typically requires at least two house numbers on each side of the road to be entered to work, the more numbers present in the data the better.

Consider using this with the "Auto-download" mode.  

#### Adding Turn Restrictions

Vespucci has a fast way to add turn restrictions. Note: if you need to split a way for the restriction you need to do this before starting.

 * select a way with a highway tag (turn restrictions can only be added to highways, if you need to do this for other ways, please use the generic "create relation" mode, if there are no possible "via" elements the menu item will also not display)
 * select "Add restriction" from the menu
 * select the "via" node or way (all possible "via" elements will have the selectable element highlighting)
 * select the "to" way (it is possible to double back and set the "to" element to the "from" element, Vespucci will assume that you are adding an no_u_turn restriction) 
 * set the restriction type in the tag menu

### Saving Your Changes

*(requires network connectivity)*

Select the same button or menu item you did for the download and now select "Upload data to OSM server".

Vespucci supports OAuth authorization besides the classical username and password method. OAuth is preferable, particularly for mobile applications since it avoids sending passwords in the clear.

New Vespucci installs will have OAuth enabled by default. On your first attempt to upload modified data, a page from the OSM website loads. After you have logged on (over an encrypted connection) you will be asked to authorize Vespucci to edit using your account. Once you have done that you will be returned to Vespucci and should retry the upload, which now should succeed.

#### Resolving conflicts on uploads

Vespucci has a simple conflict resolver. However if you suspect that there are major issues with your edits, export your changes to a .osc file ("Export" menu item in the "Transfer" menu) and fix and upload them with JOSM. See the detailed help on [conflict resolution](Conflict Resolution).  

## Using GPS

You can use Vespucci to create a GPX track and display it on your device. Further you can display the current GPS position (set "Show location" in the GPS menu) and/or have the screen center around and follow the position (set "Follow GPS Position" in the GPS menu). 

If you have the later set, moving the screen manually or editing will cause the "follow GPS" mode to be disabled and the blue GPS arrow will change from an outline to a filled arrow. To quickly return to the "follow" mode, simply touch the arrow or re-check the option from the menu.

### Auto-Download

*(requires network connectivity)*

If "Show location" and "Follow GPS Position" are enabled, Vespucci lets you auto download a small area (default 50m radius) around your current position. Just as above if you move the screen manually or change the geometry of an object you will have to re-enable "Follow GPS Position" when you want to continue. 

Notes:

* you need to download an initial area manually
* the function only works below 6km/h (brisk walking speed) to avoid causing issues with the OpenStreetMap API


## Customizing Vespucci

### Settings that you might want to change

 * Background layer
 * Overlay layer. Adding an overlay may cause issues with older devices and such with limited memory. Default: none.
 * Notes display (open Notes will be displayed as a red filled circle, closed Notes the same in blue). Default: off.
 * Node icons. Default: off.
 * Keep screen on. Default: off.
 * Large node drag area. Moving nodes on a device with touch input is problematic since your fingers will obscure the current position on the display. Turning this on will provide a large area which can be used for off-centre dragging (selection and other operations still use the normal touch tolerance area). Default: off.
 
#### Advanced preferences

 * Always show context menu. When turned on every selection process will show the context menu, turned off the menu is displayed only when no unambiguous selection can be determined. Default: off (used to be on).
 * Show statistics. Will show some statistics for debugging, not really useful. Default: off (used to be on).  
 
## Reporting Problems
 
If Vespucci crashes, or it detects an inconsistent state, you will be asked to send in the crash dump. Please do so if that happens, but please only once per specific situation. If you want to give further input or open an issue for a feature request or similar, please do so here: [Vespucci issue tracker](https://code.google.com/p/osmeditor4android/issues/list). If you want to discuss something related to Vespucci, you can either start a discussion on the [Vespucci google group](https://groups.google.com/forum/#!forum/osmeditor4android) or on the [OpenStreetMap Android forum](http://forum.openstreetmap.org/viewforum.php?id=56)


