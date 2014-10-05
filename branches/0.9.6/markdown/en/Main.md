# Main Vespucci Screen

The main Vespucci screen is your normal starting point for interacting with the application. It can be in two modes

####  ![](../images/locked.png) locked
In this mode modifying the OpenStreetMap data is disabled, you can however use all the menus and other functions, for example down- or up-load data. The main reason to use this mode is to pan and zoom in high density areas, or to avoid accidental changes when pocketing your device. 

Tapping the lock icon will toggle the mode.

####  ![](../images/unlocked.png) unlocked
In this mode you can add and change the geometry and tags of OpenStreetMap data. *You will still need to zoom in till editing is enabled.*

The lock icon will always be located in the upper left corner of your devices screen. The placement of the following controls will depend on size, orientation and age of your device, and may be in the top or bottom bar, or in a menu. 

## ![](../images/undolist_undo.png) Undo

Tapping the icon once will undo the last operation. A long press will display a list of the operations since the last save, if you have undone anything a corresponding "redo" action will be displayed. *Some operations consist of multiple simpler actions that will be listed as individual items.*

## ![](../images/camera.png) Camera

Start a camera app, add the resulting photograph to the photo layer is enabled. The photograph itself should be stored in the Vespucci/Pictures 
directory, however this depends on the specific camera app.

## ![](../images/menu_gps.png) GPS

 * **Show location** - show arrow symbol at current position
 * **Follow GPS position** - pan and center screen to follow the GPS position
 * **Goto GPS position** - goto and zoom in to the current position
 * **Start GPS track** - start recording a GPX track
 * **Pause GPS track** - pause recording the current GPX track
 * **Clear GPS track** - clear the current GPX track
 * **Upload GPS track** - upload the current GPX track *(currently not implemented)*
 * **Export GPS track** - export track to file
 * **Auto download** - automatically download the area around the current position (speed needs to be below 6 km/h) *(requires network connectivity)*

## ![](../images/menu_transfer.png) Transfer

Select either the transfer icon ![](../images/menu_transfer.png) or the "Transfer" menu item. This will display seven options:

 * **Download current view** - download the area visible on the screen and replace any existing data *(requires network connectivity)*
 * **Add current view to download** - download the area visible on the screen and merge it with existing data *(requires network connectivity)*
 * **Download other location** - shows a form that allows you to enter coordinates, search for a location or enter coordinates directly, and then download an area around that location *requires network connectivity*
 * **Upload data to OSM server** - upload edits to OpenStreetMap *(requires authentication)* *(requires network connectivity)*
 * **Close current changeset** - manually close the current changeset *(only available if a changeset is open)*
 * **Export changes** - write a ".osc" format file containing the current edits
 * **Read from file** - read a a JOSM compatible XML format file
 * **Save to file** - save as a JOSM compatible XML format file

## ![](../images/menu_config.png) Settings

Show the user preference screens. The settings are split in to two sets the first screen contains the more commonly used, the "Advanced preferences" the less used ones. 

## ![](../images/menu_tools.png) Tools

 * **Flush background tile cache** - empty the on device cache for the current background *(may take a significant amount of time)*
 * **Flush overlay tile cache** - empty the on device cache for the current overlay *(may take a significant amount of time)*
 * **Align background** - align the current background layer, this can be done manually or from the image alignment database *(requires network connectivity)*
 * **Background properties** - adjust properties of the background layer, currently only a combined contrast/brightness control *(non-persistent)*

## ![](../images/ic_menu_search_holo_light.png) Find

Search for a location and pan to it with the OpenStreetMap Nominatim service *(requires network connectivity)*


## ![](../images/menu_help.png) Help

Start the on device help browser.
