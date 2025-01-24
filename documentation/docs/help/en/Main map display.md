# Main Vespucci Screen

The main Vespucci map screen is your normal starting point for interacting with the application. It can be in two modes

####  ![Locked](../images/locked.png) locked
In this mode modifying the OpenStreetMap data is disabled, you can however use all the menus and other functions, for example down- or up-load data. The main reason to use this mode is to pan and zoom in high density areas, or to avoid accidental changes when pocketing your device. 

Tapping the lock icon will toggle the lock.

####  ![Unlocked](../images/unlocked.png) unlocked
In this mode you can add and change the geometry and tags of OpenStreetMap data. *You will still need to zoom until editing is enabled.*

If a small "T" is displayed on the lock icon you are in "Tag editing only" mode and will not be able to change geometries, a small "I" indicates indoor mapping mode. A long press on the icon will display a menu allowing you to switch between editing modes, for mode switching purposes it dosen't matter if the display is "locked" or "unlocked".

All the mode options are 

* __Normal__ conventional editing mode.
* __Tag only__ selecting an object will immediately start the property editor. See [Tag only mode](Tag%20only%20mode.md) 
* __Address__ slightly simplified dedicated mode for surveying addresses. See [Address mode](Address%20mode.md) 
* __Indoor__ indoor editing with level selector. See [Indoor mode](Indoor%20mode.md) 
* __C-Mode__ "complete" mode, only objects with validation warnings will be shown.

The lock icon will always be located in the upper left corner of your devices screen. The placement of the following controls will depend on the size, orientation and age of your device, and may be in the top or bottom bar, or in a menu. 

### Map and Data display

Once you have downloaded some OSM data you will see it displayed over a background layer. The default background is the standard "Mapnik" style map provided by openstreetmap.org, however you can change the configuration by using the layer control. The styling of the OSM data displayed on top of the background layer can be fully customized, but the standard "Color round nodes" or for "Pen round nodes" should work for most situations, the later has smaller "touch areas" mainly for working with a pen.

Note: the map images are cached on device and are only deleted if explicitly flushed. If you need to have background images available without network connectivity see [custom imagery with MBtiles](http://vespucci.io/tutorials/custom_imagery_mbtiles/).

### Nearby point-of-interest display

A nearby point-of-interest display can be shown by pulling the handle in the middle and top of the bottom menu bar up.

The view will include a filtered view of all "POI"s displayed on the current map view. If no explicit filter is set this is limited to objects that have a key with one of
_shop_, _amenity_, _leisure_, _tourism_, _craft_, _office_ or _emergency_. If an explicit filter is set, that is a tag filter or a preset filter, or a mode (_Indoor_ and _C_-mode) is selected that sets a filter the display will display objects that are allowed by the filter. For example if _Indoor_ mode is selected, the display will only show POIs on the currently selected level. 

Tapping an entry in the display will center the map on the object and select it, tapping it a second time (just as on the map display) will start the property editor, in _Tag only_ mode the property editor will start directly as expected. The POI entries are highlighted with the same validation indication as map icons, see [validation styling](https://github.com/MarcusWolschon/osmeditor4android/blob/master/src/main/assets/styles/Color-round.xml#L39).

<a id="layer_control"></a>

### Layer control

Vespucci currently supports multiple tiled background imagery layers, multiple tiled overlay layers (both raster and Mapbox vector tiles), a grid/scale layer, a task layer, a photo layer, multiple GeoJSON layers, multiple GPX/GPS layers and, naturally, an OSM data layer. Tapping the layer control (upper right corner) will display the layer dialog).

The layer dialog supports the following actions on the layer entries: 

* Hide/Show button turns drawing of the layer off/on. Hiding a layer doesn't free any resources associated with the layer.
* Zoom to extent. Zooms and pans the screen so that the whole extent of the layer is displayed, if the extent cannot be determined this will zoom out to the full web-mercator coverage. Note: on the data layer this may not be particularly useful if you have downloaded areas that are far apart.
* Drag layer to a new position in the layer stack by a long press and then dragging the layer.
* Menu button.
    * Tile based layers: 
        * __Select imagery__ Show a imagery selection dialog, if multiple layers have been used, a most-recently-used list will be displayed above this menu entry, allowing quick layer switching. 
        * __Flush tile cache__ Flush the on device cache for this layer.
        * __Background properties__ Set contrast of layer.
        * __Info__ Display information on the currently selected imagery.
        * __Align imagery...__ start the imagery alignment mode to adjust this layer. This can be done manually or by querying the "Imagery Offset" database.
        * __Test...__ try to retrieve a tile from the current view and provide some diagnostics.
    * Custom imagery tile based layers (additionally to the above):
        * __Edit custom imagery configuration__ edit the configuration of the imagery, for example the URL.
    * Mapbox Vector Tile layers (additionally to _Tile based layers_):
        * __Change style__ Show the layer styling dialog (disabled if a style has been loaded).
        * __Load style__ Load a mapbox-gl style.
        * __Reset style__ Reset the style to the default.
    * Mapillary and Panoramax layers (additionally to _Mapbox Vector Tile layers_):
        * __Set date range ...__ Select start and end date to filter the displayed data on. Note that this functionality depends on some specifics of the loaded Mapbox GL style.  
    * GeoJSON layers: 
        * __Change style__ Show the layer styling dialog.
        * __Info__ Display some information on the contents.
        * __Create Todos from GeoJSON objects ...__ create Todos from the GeoJSON objects in the layer. The default conversion adds the GeoJSON properties as text to the Todo, Custom conversion allows you to select a script for the conversion, for example a script can include references to OSM elements in the Todos. [Example script](http://vespucci.io/tutorials/other/conversion_exacmple.js)
    * GPX layers:
      Starting recording by the "Start GPX track" item in the GPS menu will add a layer for the recording. Layers for GPX files and downloaded tracks from the OSM API can be added via the __+__ button, see below.  
        * __Change style__ Show the layer styling dialog.
        * __Reset style__ Reset the style to the default.
        * __Info__ Display some summary information on the layer. 
        * __Go to start of GPX track__ Center the display on the start of the track. *(not available on the recording layer)*
        * __Go to first waypoint__ Center the display on the first waypoint. *(not available on the recording layer)*
        * __Upload GPX track__ Upload the track to the OSM API. Note: the OSM API only accepts tracks with valid time stamps for each track point, if they are missing a time stamp corresponding to the UNIX epoch date will be added.
        * __Export GPX track__ Export the track to a local file.
        * __Start playback__ Playback the current file, this behaves similar to recording tracks, the display will be centered on the current track point, then the following on and so on. *(not available on the recording layer)*
        * __Pause playback__ Temporarily pause the playback. *(not available on the recording layer)*
        * __Stop playback__ Stop the playback. *(not available on the recording layer)*
    * Photo layer:
        * __Reindex__ clear the current index data and reindex images on the device.
        * __Discard__ Turn this layer off. This will free resources if the app is exited and re-started.
    * Grid and Task layers:
        * __Configure...__ Change layer settings
        * __Discard__ Turn this layer off. For the task layer this will free resources if the app is exited and re-started.
    * Data layer:
        * __Configure...__ Select the API instance, configure the URLs including read-only sources and authentication method. Basic Authentication, OAuth 1.0a and OAuth 2 are supported, however the API instance on openstreetmap.org only supports OAuth 2 since July 2024.
    * Data and Tasks layers:
        * __Info__ Display some information on the contents.
        * __Prune__ remove downloaded data from storage that is outside of the current screen and unmodified.
    * All layers:
        * __Discard__ Delete the layer including any saved state.
        * __Up__ move the layer up, potentially obscuring data from lower layers.
        * __Down__ move the layer down.
* __+__ button:
    * for disabled layers that can only be displayed once it will show a corresponding "Enable ..." entry that will turn the layer on.
    * __Add GeoJSON layer__ Loads a GeoJSON layer from a file in to a new GeoJSON layer, this will load CVS files with suitable (WGS84) longitude and latitude columns and write a converted file to the Vespucci directory.
    * __Add background imagery layer__ Adds a tile based imagery layer from the internal configuration, which can be from ELI or JOSM, or a custom imagery layer.
    * __Add overlay imagery layer__ As above but assumes that the layer is partially transparent.
    * __Enable photo layer__ Enables the photo layer this will display clickable icons for photos that will start an internal or external viewer. Which photos can be displayed depends strongly on your Android version and settings [Advanced preferences](Advanced%20preferences.md).
    * __Enable bookmark layer__ Enables a layer displaying saved bookmarks.
    * __Enable Mapillary layer__ Enables the Mapillay layer.
    * __Enable Panoramax layer__ Enables the Panoramax layer.
    * __Add layer from GPX file__ Adds a layer from a GPX file on device.
    * __Download GPX track__ Download a GPX [track that you have previously uploaded to the OSM website](https://www.openstreetmap.org/traces/mine), and create a layer from it. Note that only GPX tracks with their starting point in the area currently displayed will be available for selection, this is a limitation of the current OSM API.
    * __Add custom imagery__ Adds a custom imagery configuration, this can then be used just 
      as any tile based imagery source, the entries can be managed in the [preferences](Preferences.md).
    * __Add layer from MVT style__ Load a Mapbox-GL style file that has a "sources" section and create a layer. 
    * __Add imagery from OAM__ Add a layer from the OpenAerialMap catalogue. *(requires network connectivity)*
    * __Add imagery from WMS__ This allows you to add a specific layer from a WMS endpoint. *(requires network connectivity)*

Custom tile layers, including on device MBTile containers can be added in the [preferences](Preferences.md) 
or as decribed above..

### Layer specific information

#### Highlighting of issues on the data layer

The included map styles highlight certain data issues, these are

* objects with 'fixme' and 'todo' tags (case insensitive) _magenta_
* ways with 'highway=road' _magenta_
* ways with 'highway' set to one of: motorway, motorway_link, trunk, trunk_link, primary, primary_link, secondary, secondary_link, tertiary, residential, unclassified, living_street and no 'name' or 'ref' tag _magenta_
* relations with no 'type' tag _magenta_
* certain points of interest that haven't been edited or verified lately. Default time that must have past since the last edit is 1 year. _yellow_
* untagged ways that are not a member of a relation _magenta_
* unconnected highway end nodes, if the end nodes a near to a highway object that they could be connected to, the node is highlighted _magenta_

The colours can be changed in the [data style](http://vespucci.io/tutorials/data_styling/#validation-styling).

#### GeoJSON layer

Clicked GeoJSON objects will display a modal with its details. Further you can copy the attributes from the object to the clipboard and paste them as tags in to an OSM element, or you can directly create an OSM element from the object. 

_Note_ GeoJSON Polygon objects that consist of a single ring will be converted to a OSM closed way if the ring has less than 2000 vertices (the limit for an OSM way element), all other Polygons and Multipolygons will be converted to OSM multipolygon relations. Nodes in GeoJSON GeometryCollections will not be merged with the vertices of linear and area elements in OSM. You might want to do the later manually.


## Available Actions

### ![Undo](../images/undolist_undo.png) Undo

Tapping the icon once will undo the last operation. A long press will display a list of the operations since the last save, if you have undone anything a corresponding "redo" checkpoint will be displayed. *Some operations consist of multiple simpler actions that will be listed as individual items.*

### ![Camera](../images/camera.png) Camera

Start a camera app, and add the resulting photograph to the photo layer if it is enabled. The photograph itself will be stored in the Vespucci/Pictures 
directory, however if this works depends on the specific camera app.  Photographs may also be "shared" from other apps into Vespucci, these can be viewed, just as images taken via the camera button in Vespuccis image viewer.

Notes 
* Because of restrictions in recent versions of Android you will need to configure the target app in the preferences if you are not happy with the standard app on your device. See [16.1 release notes](16.1.0 Release notes.md).
* OpenStreetMap currently does not provide an upload/storage facility for large numbers of images (you can store individual images of interest in the OSM wiki).  

### ![Location](../images/menu_gps.png) Location

The "on-map" GPS button duplicates the function of the "Follow location" menu entry. When this is activated the GPS arrow will be displayed as an outline. Note that the _Show location_ function will enable location updates from the system, just as the recording and similar options.
To reposition or remove the "on-map" GPS button use the "Follow position button layout" Preference.


 * **Show location** - show arrow symbol at current position
 * **Follow location** - pan and center screen to follow the current position, will enable _Show location_ too.
 * **Bookmarks...** - show current viewbox bookmarks
 * **Add bookmark...** - set a bookmark for the current viewbox
 * **Go to the nearest todo...** - go to the nearest open todo. If you are following the GPS position or it is in the current view this is relative to that position, otherwise it is relative to the center of the current view. If there are more than one active todo list you will be prompted to select one.
 * **Go to current location** - go to and zoom in to the current position. If location updates are not turned on, this will try to get a location without enabling continuous updates. Latitude, longitude, and height over the ellipsoid are shown, and you can create a new node at the coordinates including the height over the ellipsoid. You can also choose to share the position.
 * **Go to coordinates...** - go to and zoom in to coordinates (latitude longitude) or an open location code
    Supported formats are the following, with dots or optionally a comma as the decimal marker, variations on the units also accepted e.g. °, d, º, g, o.   
     * `43.63871944444445`
     * `N43°38'19.39"`
     * `43°38'19.39"N`
     * `43°38.3232'N`
     * `43d 38m 19.39s N`
     * `43 38 19.39`
     * `433819N`
 * **Go to last edit** - Pan and zoom to the bounding box of the top most undo checkpoint.
 * **Start GPX track** - start recording a GPX track and display a corresponding layer.
 * **Resume GPX track** - continue recording a GPX track.
 * **Pause GPX track** - pause recording the current GPX track
 * **Clear GPX track** - clear the current GPX track

<a id="transfer"></a>

### ![Transfer](../images/menu_transfer.png) Transfer

Select either the transfer icon ![Transfer](../images/menu_transfer.png) or the "Transfer" menu item. This will display seven or eight options:

 * **Upload data to OSM server...** - review and upload changes to OpenStreetMap, the entry is disabled if you haven't changed anything yet, or there is no network available. See [Uploading your changes](Uploading%20your%20changes.md) for more information *(requires authentication)* *(requires network connectivity)*
 * **Review changes...** - review current changes, and potentially select them for upload.
 * **Download current view** - download the area visible on the screen and merge it with existing data *(requires network connectivity)*
 * **Clear and download current view** - clear any data in memory and then download the area visible on the screen *(requires network connectivity)*
 * **Query Overpass...** - run a query against a Overpass API server, see [Overpass queries](#overpass_queries). *(requires network connectivity)*
 * **Close current changeset** - manually close the current changeset *(only available if a changeset is open)*
 * **Location based auto download** - download an area around the current location automatically *(requires network connectivity)* *(requires GPS)*
 * **Pan and zoom auto download** - download the area shown in the current screen automatically *(requires network connectivity)*
 * **Update data** - re-download data for all areas and update what is in memory *(requires network connectivity)*
 * **Clear data** - remove any OSM data in memory
 * **File...** - saving and loading OSM data to/from on device files. <a id="file"></a>
    * **Export changes to OSC file** - write a ".osc" format file containing the current edits
    * **Apply changes from OSC file** - read a ".osc" format file and apply its contents
    * **Save to JOSM file...** - save as a JOSM compatible XML format file
    * **Read from JOSM file...** - read a JOSM compatible XML format file, this supports JOSM, regular OSM and Overpass API (if metadata is included) format XML files. The files must be node-way-relation ordered.
    * **Read from PBF file...** - read OSM data from a PBF format file
    * **Download data for offline use...** - download and install a mapsplit format data extract *(requires network connectivity)*
 * **Tasks...** - down- and uploading tasks (Notes, OSMOSE bugs, Maproulette and custom tasks)
    * **Upload all** - upload all new or modified tasks
    * **Download tasks for current view** - download Notes/Bugs for the area visible on the screen
    * **Auto download** - download tasks around the current geographic location automatically *(requires network connectivity)* *(requires GPS)*
    * **Clear** - remove all tasks from storage
    * **File...** - save Notes/Bugs data to on device storage
        * **Save all notes...** - write a ".osn" format file containing all downloaded notes (this is the format supported by JOSM)
        * **Save new and changed notes...** - write a ".osn" format file containing all new and changed notes
        * **Load notes...** - load a ".osn" format file containing Notes
        * **Load todos...** - read todos in a simple JSON format
        * **Save todos...** - save todos in a simple JSON format

### ![Preferences](../images/menu_config.png) Preferences

Show the user preference screens. The settings are split into two sets: the first screen contains the more commonly used preferences, the "Advanced preferences" contains the less used ones. 

<a id="tools"></a>

### ![Tools](../images/menu_tools.png) Tools…

 * **Apply stored offset to imagery** - apply stored offset, if it exists, for the current background layer 
 * **More imagery tools**
    * **Update configuration from JOSM** - download a current version of the imagery layer configuration from the JOSM repository. Currently the default configuration is provided by the Editor Layer Index, not JOSM. *(requires network connectivity)*
    * **Update configuration from ELI** - download a current version of the imagery layer configuration from the Editor Layer Index repository. *(requires network connectivity)*
    * **Flush all tile caches** - empty all on device tile caches. 
 * **Reset address prediction** - reset the current address prediction data, will seeded from loaded data if possible.
 * **Reset authorization** - reset the OAuth tokens, this will force re-authorisation the next time you upload.
 * **Authorize with OAuth...** - start authorization process immediately. *(requires network connectivity)*
 * **Set MapRoulette API key** - set the API key for modifying the status of MapRoulette tasks.
 * **Clear clipboard** - remove content from the internal OSM element clipboard.
 * **Calibrate pressure sensor...** - calibrate the conversion from barometric pressure to elevation.
 * **Install EGM** - install a gravitational model of the earth, required for getting height data from the devices GPS. Alternatively you can switch to NMEA input in the "Advanced preferences".
 * **Remove EGM** - remove the EGM.
 * **Import data style...** - import an additional data style from an XML file or from a ZIP archive containing an XML file and icons. This overwrites existing style files with the same name.
 * **Load keys from file...** - load additional keys, for example API keys for background imagery, or other OAuth keys from a file.
 * **JS Console** - start the JavaScript console for scripting the application. Note that this needs to be enabled in the "Advanced preferences".

### ![Find](../images/ic_menu_search_holo_light.png) Find

Search for a location and pan to it with the OpenStreetMap Nominatim or Photon service *(requires network connectivity)*

### Search for objects

Search for OSM objects in the loaded data using JOSMs search/filter expressions. See [JOSM filter documentation](http://vespucci.io/tutorials/object_search/) for more information. Besides searching in the loaded data alternatively you can create a Overpass API query and use that to download data.

<a id="overpass_queries"></a>

#### Overpass queries

The queries will be run against the Overpass server set in the "Advanced preferences". The standard behaviour is to replace the current data with the results of the query, 
if you have unsaved changes the query will not run except if you select the _Merge result_ check box (this will merge the results of the query with the existing data). 
If _Select result_ is checked, any results (with the exception of way nodes) will be selected and the app will zoom to the data.

Note: if you are generating the query from the JOSM query language you need to add an _inview_ term otherwise the query will be executed for the whole world, if you are manually 
creating a query you should add _({{bbox}})_ terms for the same reason.

### Modes...

This menu allows mode selection as via the _lock button_ and enabling/disabling of the _simple mode_.

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
