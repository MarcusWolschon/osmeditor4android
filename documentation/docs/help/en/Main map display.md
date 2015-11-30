# Main Vespucci Screen

The main Vespucci map screen is your normal starting point for interacting with the application. It can be in two modes

####  ![](../images/locked.png) locked
In this mode modifying the OpenStreetMap data is disabled, you can however use all the menus and other functions, for example down- or up-load data. The main reason to use this mode is to pan and zoom in high density areas, or to avoid accidental changes when pocketing your device. 

Tapping the lock icon will toggle the mode.

####  ![](../images/unlocked.png) unlocked
In this mode you can add and change the geometry and tags of OpenStreetMap data. *You will still need to zoom in till editing is enabled.*

If a small "T" is displayed on the lock icon you are in "Tag editing only" mode and will not be able to create new objects of change geometries. A long press on the icon will switch to the normal editing mode.

The lock icon will always be located in the upper left corner of your devices screen. The placement of the following controls will depend on the size, orientation and age of your device, and may be in the top or bottom bar, or in a menu. 

### Map and Data display

Once you have downloaded some OSM data you will see it displayed over a configurable background layer. The default background is the standard "Mapnik" style map provided by openstreetmap.org, however you can choose a different background or none from the Preferences. The style of the OSM data displayed on top of the background layer can be fully customized, however the standard "Color round nodes" or for "Pen round nodes" should work for most situations, the later has smaller "touch areas" mainly for working with a pen. Depending on the device it may however work better for you.

Note: the map tiles are cached on device and only deleted if explicitly flushed. In principle you can pre-download the tiles for the area you want to work on and so pare yourself the need to access them online while outside. 

### Overlay layer

Besides the background layer you can add a "overlay" layer between the background and the OSM data, for example a layer showing buildings without an address.

### Highlighting of data issues

With the standard map style certain data issues will be highlighted in magenta, these are

* objects with 'fixme' and 'todo' tags (case insensitive)
* ways with 'highway=road'
* ways with 'highway' set to one of: motorway, motorway_link, trunk, trunk_link, primary, primary_link, secondary, secondary_link, tertiary, residential, unclassified, living_street and no 'name' or 'ref' tag
* relations with no 'type' tag  

## Available Actions

### ![](../images/undolist_undo.png) Undo

Tapping the icon once will undo the last operation. A long press will display a list of the operations since the last save, if you have undone anything a corresponding "redo" action will be displayed. *Some operations consist of multiple simpler actions that will be listed as individual items.*

### ![](../images/camera.png) Camera

Start a camera app, add the resulting photograph to the photo layer is enabled. The photograph itself should be stored in the Vespucci/Pictures 
directory, however this depends on the specific camera app.

### ![](../images/menu_gps.png) GPS

 * **Show location** - show arrow symbol at current position
 * **Follow GPS position** - pan and center screen to follow the GPS position
 * **Goto GPS position** - go to and zoom in to the current position
 * **Start GPS track** - start recording a GPX track
 * **Pause GPS track** - pause recording the current GPX track
 * **Clear GPS track** - clear the current GPX track
 * **Track management...** - manage existing tracks
    * **Upload GPS track** - upload the current GPX track *(requires network connectivity)*
    * **Export GPS track** - export track to file
    * **Import GPS track** - import track from file
 * **Goto start of track** - center the map on the first track point
 

### ![](../images/menu_transfer.png) Transfer

Select either the transfer icon ![](../images/menu_transfer.png) or the "Transfer" menu item. This will display seven or eight options:

 * **Download current view** - download the area visible on the screen and replace any existing data *(requires network connectivity)*
 * **Add current view to download** - download the area visible on the screen and merge it with existing data *(requires network connectivity)*
 * **Download other location** - shows a form that lets you enter coordinates, search for a location or enter coordinates directly, and then download an area around that location *(requires network connectivity)*
 * **Upload data to OSM server** - upload edits to OpenStreetMap *(requires authentication)* *(requires network connectivity)*
 * **Close current changeset** - manually close the current changeset *(only available if a changeset is open)*
 * **Auto download** - download an area around the current location automatically *(requires network connectivity)* *(requires GPS)*
 * **File...** - saving and loading OSM data to/from on device files.
    * **Export changes** - write a ".osc" format file containing the current edits
    * **Read from file** - read a JOSM compatible XML format file
    * **Save to file** - save as a JOSM compatible XML format file
 * **Note/Bugs...** - down- and uploading Notes/Bugs
    * **Download bugs for current view** - download Notes/Bugs for the area visible on the screen
    * **Upload all** - upload all new or modified Notes/Bugs
    * **Clear** - remove all bugs from storage
    * **Auto download** - download Notes/Bugs around the current location automatically *(requires network connectivity)* *(requires GPS)*

### ![](../images/menu_config.png) Settings

Show the user preference screens. The settings are split into two sets: the first screen contains the more commonly used preferences, the "Advanced preferences" contains the less used ones. 

### ![](../images/menu_tools.png) Tools

 * **Flush background tile cache** - empty the on device cache for the current background *(may take a significant amount of time)*
 * **Flush overlay tile cache** - empty the on device cache for the current overlay *(may take a significant amount of time)*
 * **Align background** - align the current background layer, this can be done manually or from the image alignment database *(requires network connectivity)*
 * **Background properties** - adjust properties of the background layer, currently only a combined contrast/brightness control *(non-persistent)*
 * **Reset OAuth** - reset the OAuth tokens, This will force reauthorisation the next time you upload.
 * **Authorise OAuth** - start authorisation process immediately. *(requires network connectivity)*

### ![](../images/ic_menu_search_holo_light.png) Find

Search for a location and pan to it with the OpenStreetMap Nominatim service *(requires network connectivity)*


### ![](../images/menu_help.png) Help

Start the on device help browser.
