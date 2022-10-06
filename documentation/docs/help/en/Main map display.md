# Main Vespucci Screen

The main Vespucci map screen is your normal starting point for interacting with the application. It can be in two modes

####  ![Locked](../images/locked.png) locked
In this mode modifying the OpenStreetMap data is disabled, you can however use all the menus and other functions, for example down- or up-load data. The main reason to use this mode is to pan and zoom in high density areas, or to avoid accidental changes when pocketing your device. 

Tapping the lock icon will toggle the lock.

####  ![Unlocked](../images/unlocked.png) unlocked
In this mode you can add and change the geometry and tags of OpenStreetMap data. *You will still need to zoom until editing is enabled.*

If a small "T" is displayed on the lock icon you are in "Tag editing only" mode and will not be able to change geometries, a small "I" indicates indoor mapping mode. A long press on the icon will display a menu allowing you to switch between editing modes, for mode switching purposes it dosen't matter if the display in "locked" or "unlocked".

All the mode options are 

* __Normal__ conventional editing mode.
* __Tag only__ selecting an object will immediately start the property editor.
* __Address__ slightly simplified dedicated mode for surveying addresses.
* __Indoor__ indoor editing with level selector.
* __C-Mode__ "complete" mode, only objects with validation warnings will be shown.

The lock icon will always be located in the upper left corner of your devices screen. The placement of the following controls will depend on the size, orientation and age of your device, and may be in the top or bottom bar, or in a menu. 

### Map and Data display

Once you have downloaded some OSM data you will see it displayed over a configurable background layer. The default background is the standard "Mapnik" style map provided by openstreetmap.org, however you can choose a different background or none from the Preferences. The style of the OSM data displayed on top of the background layer can be fully customized, however the standard "Color round nodes" or for "Pen round nodes" should work for most situations, the latter has smaller "touch areas" mainly for working with a pen. Depending on the device it may however work better for you than the default.

Note: the map tiles are cached on device and only deleted if explicitly flushed. In principle you can pre-download the tiles for the area you want to work on and so spare yourself the need to access them online while outside. 

### Layer control

Vespucci currently supports multiple tiled background imagery layers, multiple tiled overlay layers (both raster and Mapbox vector tiles), a grid/scale layer, a task layer, a photo layer, multiple GeoJSON layers, multiple GPX/GPS layers and, naturally, an OSM data layer. Tapping the layer control (upper right corner) will display the layer dialog).

The layer dialog supports the following actions on the layer entries: 

* Hide/Show button turns drawing of the layer off/on. Hiding a layer doesn't free any resources associated with the layer.
* Zoom to extent. Zooms and pans the screen so that the whole extent of the layer is displayed, if the extent cannot be determined this will zoom out to the full web-mercator coverage. Note: on the data layer this may not be particularly useful if you have downloaded areas that are far apart.
* Menu button.
    * Tile based layers: 
        * __Select imagery__ Show a imagery selection dialog, if multiple layers have been used, a most-recently-used list will be displayed above this menu entry, allowing quick layer switching. 
        * __Flush tile cache__ Flush the on device cache for this layer.
        * __Background properties__ Set contrast of layer.
        * __Info__ Display information on the currently selected imagery.
    * Custom imagery tile based layers (additionally to the above):
        * __Edit custom imagery configuration__ edit the configuration of the imagery, for example the URL.
    * Mapbox Vector Tile layers (additionally to _Tile based layers_):
        * __Change style__ Show the layer styling dialog (disabled if a style has been loaded).
        * __Load style__ Load a mapbox-gl style.
        * __Reset style__ Reset the style to the default.
    * GeoJSON layers: 
        * __Change style__ Show the layer styling dialog.
        * __Info__ Display some information on the contents.
    * GPX layers:
      Starting recording by the "Start GPX track" item in the GPS menu will add a layer for the recording. Layers for GPX files and downloaded tracks from the OSM API can be added via the __+__ button, see below.  
        * __Change style__ Show the layer styling dialog.
        * __Go to start of GPX track__ Center the display on the start of the track. *(not available on the recording layer)*
        * __Go to first waypoint__ Center the display on the first waypoint. *(not available on the recording layer)*
        * __Upload GPX track__ Upload the track to the OSM API.
        * __Export GPX track__ Export the track to a local file.
        * __Start playback__ Playback the current file, this behaves similar to recording tracks, the display will be centered on the current track point, then the following on and so on. *(not available on the recording layer)*
        * __Pause playback__ Temporarily pause the playback. *(not available on the recording layer)*
        * __Stop playback__ Stop the playback. *(not available on the recording layer)*
    * Photo, Grid and Task layers:
        * __Configure...__ (for the Grid and Task layers)
        * __Disable__ Turn this layer off. For the tasks and photo layers this will free resources if the app is exited and re-started.
    * Data layer:
        * __Configure...__ Allows selection and configuration of the current OSM data sources. Same contents as the corresponding preference screen.
        * __Info__ Display some information on the contents.
    * Data and Tasks layers:
        * __Prune__ remove downloaded data from storage that is outside of the current screen and unmodified.
    * All layers:
        * __Discard__ Delete the layer including any saved state.
        * __Up__ move the layer up, potentially obscuring data from lower layers.
        * __Down__ move the layer down.
* __+__ button:
    * for disabled layers that can only be displayed once it will show a corresponding "Enable ..." entry that will turn the layer on.
    * __Add GeoJSON layer__ Loads a GeoJSON layer from a file in to a new GeoJSON layer.
    * __Add background imagery layer__ Adds a tile based imagery layer from the internal configuration, which can be from ELI or JOSM, or a custom imagery layer.
    * __Add overlay imagery layer__ As above but assumes that the layer is partially transparent.
    * __Add layer from GPX file__ Adds a layer from a GPS file on device.
    * __Download GPS track__ Download a GPX file from the OSM API for the user and create a layer from it. Note that only GPX tracks with their starting point in the area currently displayed will be available for selection, this is a limitation of the current OSM API.
    * __Add custom imagery__ Adds a custom imagery configuration, this can then be used just 
      as any tile based imagery source, the entries can be managed in the [preferences](Preferences.md).
    * __Add layer from MVT style__ Load a Mapbox-GL style file that has a "sources" section and create a layer. 
    * __Add imagery from OAM__ Add a layer from the OpenAerialMap catalogue. *(requires network connectivity)*
    * __Add imagery from WMS__ This allows you to add a specific layer from a WMS endpoint. *(requires network connectivity)*

Custom tile layers, including on device MBTile containers can be added in the [preferences](Preferences.md) 
or as decribed above..

### Highlighting of data issues

With the standard map style certain data issues will be highlighted in magenta, these are

* objects with 'fixme' and 'todo' tags (case insensitive)
* ways with 'highway=road'
* ways with 'highway' set to one of: motorway, motorway_link, trunk, trunk_link, primary, primary_link, secondary, secondary_link, tertiary, residential, unclassified, living_street and no 'name' or 'ref' tag
* relations with no 'type' tag
* certain points of interest that haven't been edited or verified lately. Default time that must have past since the last edit is 1 year.  
* untagged ways that are not a member of a relation
* unconnected highway end nodes, if the end nodes a near to a highway object that they could be connected to, the node is highlighted

### Indoor mode
   
In indoor mode the displayed level can be changed with the up/down buttons, tapping the level display button will show suitable objects that doesn’t have a level tag, tapping again will revert to the level selector.

Elements that are not on the current level are drawn in a light grey, this works best on an uncluttered background map, for example the Thunderforest Landscape map. Newly created elements will automatically have the current level assigned in the property editor.

## Available Actions

### ![Undo](../images/undolist_undo.png) Undo

Tapping the icon once will undo the last operation. A long press will display a list of the operations since the last save, if you have undone anything a corresponding "redo" checkpoint will be displayed. *Some operations consist of multiple simpler actions that will be listed as individual items.*

### ![Camera](../images/camera.png) Camera

Start a camera app, add the resulting photograph to the photo layer is enabled. The photograph itself should be stored in the Vespucci/Pictures 
directory, however this depends on the specific camera app.

### ![Location](../images/menu_gps.png) Location

The "on-map" GPS button duplicates the function of the "Follow GPS position" menu entry. When this is activated the GPS arrow will be displayed as an outline. Note that the _Show location_ function will enable location updates from the system, just as the recording and similar options.

 * **Show location** - show arrow symbol at current position
 * **Follow location** - pan and center screen to follow the current position, will enable _Show location_ too.
 * **Add bookmark...** - set a bookmark for the current viewbox
 * **Bookmarks...** - show current viewbox bookmarks
 * **Go to the nearest todo...** - go to the nearest open todo. If you are following the GPS position or it is in the current view this is relative to that position, otherwise it is relative to the center of the current view. If there are more than one active todo list you will be prompted to select one.
 * **Go to current location** - go to and zoom in to the current position. If location updates are not turned on, this will try to get a location without enabling continuous updates.
 * **Go to coordinates...** - go to and zoom in to coordinates or an open location code
 * **Start GPX track** - start recording a GPX track and display a corresponding layer.
 * **Pause GPX track** - pause recording the current GPX track
 * **Clear GPX track** - clear the current GPX track

### ![Transfer](../images/menu_transfer.png) Transfer

Select either the transfer icon ![Transfer](../images/menu_transfer.png) or the "Transfer" menu item. This will display seven or eight options:

 * **Download current view** - download the area visible on the screen and merge it with existing data *(requires network connectivity)*
 * **Clear and download current view** - clear any data in memory and then download the area visible on the screen *(requires network connectivity)*
 * **Upload data to OSM server** - upload edits to OpenStreetMap, the entry is disabled if you haven't changed anything yet, or there is no network available *(requires authentication)* *(requires network connectivity)*
 * **Update data** - re-download data for all areas and update what is in memory *(requires network connectivity)*
 * **Close current changeset** - manually close the current changeset *(only available if a changeset is open)*
 * **Location based auto download** - download an area around the current location automatically *(requires network connectivity)* *(requires GPS)*
 * **Pan and zoom auto download** - download the area shown in the current screen automatically *(requires network connectivity)*
 * **File...** - saving and loading OSM data to/from on device files.
    * **Export changes to OSC file** - write a ".osc" format file containing the current edits
    * **Apply changes from OSC file** - read a ".osc" format file and apply its contents
    * **Read from JOSM file...** - read a JOSM compatible XML format file, this supports JOSM, regular OSM and Overpass API (if metadata is included) format XML files. The files must be node-way-relation ordered.
    * **Read from PBF file...** - read OSM data from a PBF format file
    * **Save to JOSM file...** - save as a JOSM compatible XML format file
    * **Download data for offline use...** - download and install a mapsplit format data extract *(requires network connectivity)*
 * **Tasks...** - down- and uploading tasks (Notes, OSMOSE bugs, Maproulette and custom tasks)
    * **Download tasks for current view** - download Notes/Bugs for the area visible on the screen
    * **Upload all** - upload all new or modified tasks
    * **Clear** - remove all tasks from storage
    * **Auto download** - download tasks around the current geographic location automatically *(requires network connectivity)* *(requires GPS)*
    * **File...** - save Notes/Bugs data to on device storage
        * **Save all notes...** - write a ".osn" format file containing all downloaded notes (this is the format supported by JOSM)
        * **Save new and changed notes...** - write a ".osn" format file containing all new and changed notes
        * **Load notes...** - load a ".osn" format file containing Notes
        * **Load todos...** - read todos in a simple JSON format
        * **Save todos...** - save todos in a simple JSON format

### ![Preferences](../images/menu_config.png) Preferences

Show the user preference screens. The settings are split into two sets: the first screen contains the more commonly used preferences, the "Advanced preferences" contains the less used ones. 

### ![Tools](../images/menu_tools.png) Tools

 * **Align background** - align the current background layer, this can be done manually or from the image alignment database *(requires network connectivity)*
 * **Apply stored offset to imagery** - apply stored offset, if it exists, for the current background layer 
 * **More imagery tools**
    * **Update imagery layer configuration** - download a current version of the imagery layer configuration. *(requires network connectivity)*
    * **Flush all tile caches** - empty all on device tile caches. 
 * **Reset address prediction** - reset the current address prediction data, will seeded from loaded data if possible.
 * **Reset OAuth** - reset the OAuth tokens, this will force re-authorisation the next time you upload.
 * **Authorise OAuth** - start authorisation process immediately. *(requires network connectivity)*
 * **Set MapRoulette API key** - set the API key for modifying the status of MapRoulette tasks.
 * **Clear clipboard** - remove content from the internal OSM element clipboard.
 * **Calibrate pressure sensor...** - calibrate the conversion from barometric pressure to elevation.
 * **Install EGM** - install a gravitational model of the earth, required for getting height data from the devices GPS. Alternatively you can switch to NMEA input in the "Advanced preferences".
 * **Remove EGM** - remove the EGM.
 * **Import data style...** - import an additional data style from an XML file or from a ZIP archive containing an XML file and icons. This overwrites existing style files with the same name.
 * **Load keys from file...** - load additional keys, for example API keys for background imagery, or other OAuth keys from a file.
 * **JS Console** - start the JavaScript console for scripting the application. Note that this needs to be anabled in the "Advaced preferences":

### ![Find](../images/ic_menu_search_holo_light.png) Find

Search for a location and pan to it with the OpenStreetMap Nominatim or Photon service *(requires network connectivity)*

### Search for objects

Search for OSM objects in the loaded data using JOSMs search/filter expressions. See [JOSM filter documentation](http://vespucci.io/tutorials/object_search/) for more information.

### Tag-Filter *(checkbox)*

Enable the tag based filter, the filter can be configured by tapping the filter button on the map display.

### Preset-Filter *(checkbox)*

Enable the preset based filter, the filter can be configured by tapping the filter button on the map display.

### Share position

Share the current position (center of the displayed map) with other apps on the device.

### ![Help](../images/menu_help.png) Help

Start the on device help browser.

### Authors and licenses

Some information on the licence of Vespucci itself, specific components and a list of all known contributors to the app.

### Debug

Internal information on the app and a button to force submission of a crash dump.
