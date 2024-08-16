# Advanced preferences

The Advanced preferences page is split in to seven topical sections that can be access by clicking on the main entries. Depending on the Android version running on your device some settings may be not available for technical reasons.

## User Interface Settings

### Show node icons

Show icons on nodes if they can be matched with a preset. Default: _on_.

### Show POI icons on buildings

Show icons on buildings if the tags on the building can be matched with a preset. Default: _on_.

### Display tag form

Enable the form based tag editor. Default: _on_.

### Display labels from presets

Display labels from presets in the tag form editor. Default: _on_.

### Show camera action

Show a camera button on the main display (if a camera app is present). Default: _on_.

### Camera app

Select the camera app to use. If your, installed, camera app is not listed, please report this and we will add it to the list. Unluckily google does not allow to automatically determine installed camera apps outside of pre-installed ones. Default: _System default_.

### Use the MediaStore

Additionally use Androids MediaStore for accessing photos. This will add all photographs in JPEG format found via the MediaStore that contain coordinates in their EXIF data to the phto layer. Default: _off_.

### Follow location button layout

Change the side of the display the "Follow location" button is positioned on or remove it completely. Default: _lefthand side_.

### Always dim non-downloaded areas

If on the non-downloaded areas will be dimmed when the screen is locked. Default: _off_.

### Fullscreen mode

On devices without hardware buttons Vespucci can run in full screen mode, that means that "virtual" navigation buttons will be automatically hidden while the map is displayed, providing more space on the screen for the map. Depending on your device this may work well or not.  

In _Auto_ mode we try to determine automatically if using full screen mode is sensible or not, setting it to _Force_ or _Never_ skips the automatic check and full screen mode will always be used or always not be used respectively. _No statusbar_ will additionally try to remove Androids status bar besides forcing full screen mode.

On devices running Android 11 or higher the _Auto_ mode will never turn full screen mode on as Androids gesture navigation provides a viable alternative to it. 

Default on Android 4.4 - 10:0: _Auto_, 4.0 - 4.3 and 11.0 and later: _Never_. 

You need to restart the app for changes to this setting to take effect.

### Map screen orientation

If set to any other value than _Auto_ Vespucci will try to override your device settings for screen rotation.

### Show tolerance

Show halos indicating the size of the "touch areas" around elements. Default: _on_.

### Use back key for Undo

Use the back key for undo. Default: _off_.

### Theme

Select the theme to use. _Follow system_ will follow the setting in the system preferences as far as possible, _Light_ and _Dark_ will fix the setting on the respective theme. _Follow system_ is only available on Android 10 and later. Default: _Follow system_. You need to restart the app for changes to this setting to take effect.

### Enable split action bar

Show the menu buttons at the bottom of the screen. Default: _on_. You need to restart the app for changes to this setting to take effect.

### App language

Select a language for the user interface that is different from the device default, setting the value to _Device language_ will revert to using your preference for the whole device. On devices running Android 13 and later the app language can be changed in the system settings too. Preset translations can be disabled in the preset configurations. 

### Max. number of inline values

Maximum number of values that will directly be displayed in the form based editor for Tags with pre-determined values. Default: _4_.

### Long string limit

Limit from which, instead of an inline text field, a modal will be displayed will be used for text editing in the form based editor. The modal will be displayed if either the existing text is longer than the limit, or if the preset text field length attribute is longer than the limit. Default: _255_.

### Time to wait before auto-locking

How long to wait before auto-locking the map display, setting the value to 0 disables the auto-locking. Default _60 seconds_.

### Enable Anti-Aliasing

Use anto-aliasing when rendering OSM data. Default: _on_.

### Max line width

Maximum width lines will increase to when zooming in. Default: _16 pixels_.

### Pending upload OK limit

If the number of pending object uploads is below this limit the situation is considered unproblematic. Default: _50 objects_.

### Pending upload warning limit

If the number of pending object uploads is below this limit it will have warning status. A pending upload count above this will have danger status. Default: _200 objects_.

### Disable feedback activity

Disable the feedback activity and use an URL instead. Default: _false_.

### Upload reminder interval

Interval for reminders if you have unpublished changes. Default: _6 hours_.
    
### Too much data warning</string>

Limit (in number of Nodes) at which we start warning about too much data being loaded. Default: _50'000 objects_.

### Beep volume

Set how load the beep is Vespucci uses when you are pressing unsupported short cut keys on a connected real keyboard. Default: _50_.

### Minimum zoom for Mapillary

Set the minimum zoom level for Mapillary data to be displayed. If set too low the application might crash in areas with very high density Mapillary data. Consider increasing the number if your display if very large or very high resolution. Default: _16_

### Name capitalization

Set the capitalization mode for keyboard input for name-like tag. One of _No capitalization_, _Word capitalization_ and _Sentence capitalization_. Default: _Word capitalization_.

Note: this setting is only applicable when the current locale uses latin script.

### Use volume keys for zooming

Allow the use of the volume keys for zooming in and out. Default: _false_.

## Data and Editing Settings

Settings related to editing.

### Always use contextual menu

Always show the element selection context menu if the selection is ambiguous. If turned off the nearest element will be selected. Default: _off_.

### Override address tag default 
    
Use our address tag configuration over any region specific values that we may have found. Default: _off_.

### Address tags

When using address prediction only add the tags configured here.

### Enable auto-apply of preset

Automatically apply the best matching preset when the property editor is invoked. This will add keys for all non-optional tags. Default: _on_.

### Distance between neighbour addresses

The distance in meters up to which two addresses are considered to be potential neighbours, this is useful for fine tuning how the increment used for predicting addresses is calculated. Default: _50 meters_.

### Enable name suggestions

Support special handling of name tags with canonical name suggestions. Default: _on_.

### Enable name suggestion presets

When using name suggestions, automatically apply the best preset. Default: _on_.

### Generate notifications

Generate notifications for validator detected issues. Default: _on_.

### Group alerts only

Generate notifications (audio) only once per notification group. Default: _off_.

### Max. number of displayed notifications

Maximum number of notifications retained in the status bar: _5_ (per type).

### Max distances for notifications

Maximum distance a validation issue or note/bug can be from the current positions to trigger a notification. Default: _100 meters_.

### Close changesets

Close the current open changeset after uploading edits. Default: _on_.

### Squaring threshold

The threshold in ° over which angles are ignored for squaring and straightening. Default: _15°_.

### Auto-format phone numbers

This allows to control if phone numbers should be auto-formatted. Default: _on_. 

### Max distance for stored imagery offsets
  
The farthest distance a locally stored layer offset will be automatically be applied. Default: _100 meters_.

### Use imperial units for measuring

Use imperial units when measuring in countries that customarily use them. Default: _off_.

### Minimum number of nodes in circle

The target minimum number of nodes that should be aimed for in a circle. Default: _6_.

### Maximum circle segment length

Maximum distance between two circle nodes. Default: _2m_.

### Minimum circle segment length
   
Minimum distance between two circle nodes. Default: _0.5m_.
    
## Auto-download Settings

### Download radius

When auto-downloading, the radius of the area that is attempted to download around the current position. In pan and zoom auto-download mode, this is the minimum download size. Default: _50 meters_.

### Maximum auto-download speed

Maximum speed up to which auto-download is attempted. Default: _10 km/h_.

### Auto-prune limit

Limit at, when reached, an automatic prune of the data in memory is attempted. Default: _5000 Nodes_.

### Zoom limit

Minimum zoom for pan and zoom auto-download to work. In high data density areas this should be set to higher values to avoid loading very large amounts of data when zooming out. 
This is used both for data and tasks. Default: _17_. 

### Task download radius

When auto-downloading notes and bugs, the radius of the area that is attempted to download around the current position. Default: _200 meters_.

### Maximum task auto-download speed

Maximum speed up to which auto-download of notes and bugs is attempted. Default: _30 km/h_.

### Task auto-prune limit

Limit at, when reached, an automatic prune of the tasks in memory is attempted. Default: _10000 Tasks_.

## Location Settings

GPS and Network location settings.

### GPS/GNSS source

Source of GPS/GNSS location updates. 

Possible values:

__Internal__ use the internal GNSS location provider.

__Internal NMEA__ use the internal GNSS location provider with NMEA output, this is only useful for testing.

__NMEA from TCP client__ connect to a source of NMEA sentences over TCP, if the source is gpsd, the client will attempt to switch it to NMEA output (from 14.0.11 on).

__NMEA from TCP server__ this will start a server on a specified port and listen for a client providing NMEA sentences, this is mainly useful with rtklib.

Default: _internal_.

### NMEA network source

If GPS source is set to one of the NMEA TCP options, configure on which IP address and port the client or server is. In __NMEA from TCP server__ mode the host part is currently ignored.

### Minimum GPS/GNSS interval

Minimum interval between updates for the internal GPS/GNSS source. Default: _1000 ms_.

### Minimum GPS/GNSS-distance

Minimum distance between updates for the internal GPS/GNSS source. Default: _5 meters_.

### Leave GPS/GNSS turned off

If GPS has been disabled by the user, don't ask to turn it on. Default: _off_.

### Fallback to network location

If the device is providing "Network" location data use it as a fallback if we haven't received a GPS location for a longer time. Default: _on_.

### Stale location after

Time, in seconds, after which a location will be considered stale. Default: _60 s_

## Server Settings

OpenStreetMap API and other servers configuration.

### OSM API URL

Select the API instance, configure the URLs including read-only sources and authentication method. Basic Authentication, OAuth 1.0a and OAuth 2 are supported, however the API instance on openstreetmap.org only supports OAuth 2 since June 2024. These preferences can also be changed by selecting _Configure_ for the OpenStreetMap Data layer in the layers modal.

### User account

Set a user and password for API authentication. This is only needed if the target API server does not support OAuth authentication, and in general should be avoided as unsafe.

### Offset server

API server for imagery offsets.

### OSMOSE server

OSMOSE QA API server configuration.

### Configure geocoders

Geocoding service providers. Currently Photon and Nominatim servers are supported.

### Taginfo server

Configure the taginfo server used for the "online" preset search/construction. 

### Overpass server

Configure the overpass API server used online object search. 

### OpenAerialMap server

Configure the OpenAerialMap server used for providing additional imagery for backgrounds.

## Layer download and storage

Download and storage configuration for the tiled imagery layers.

### Max. number of download threads

Maximum number of simultaneous download threads. Default: _4_.

### Tile cache size

Total on device size for caching imagery tiles. Default: _100MB_.

### Prefer removable storage

Prefer removable storage for storing tiles. Requires restart of app to take effect, you will need to manually remove the existing tile cache if you change this. Default: _true_.

### Mapillary cache size

Total on device size for caching Mapillary images. Default: _100MB_.

## Auto-save configuration

Settings for the automatic saving of edits.

### Save state
    
Save the complete state including unchanged data and undo information. This is used to restore the app to its previous state and is the same information saved as when the app is paused or stopped. The saved state is stored in an app private location and is not accessible by the end user. Default: _enabled_.

### Save changes

Save changes as OSC file. This saves the current changes as a .osc file in the "Autosave" directory in the public Vespucci directory. Default: _enabled_.

### Minimum interval

The minimum interval in minutes between saves. Default: _5 minutes_.

### Minimum number of changes

Minimum number of changes required for a save to occur. Default: _1_.

### Maximum number of OSC files to retain

The maximum number of OSC files the app will retain, when this limit is reached the oldest files will be deleted. Default: _5_.

### Minimum GPX saving interval

Minimum interval in minutes between saves of GPX recordings. Default: _5 minutes_.

## Miscellaneous

### Report app crashes

Submit ACRA crash dumps to the Vespucci developers. You will still be asked on a per event basis if you want to submit a specific crash dump, if you set this to _off_ the functionalitiy is completely disabled. Default: _on_.

### Show stats

Show some uninteresting stats on screen. Debugging use only. Default: _off_.

## Experimental

### Enable JS Console

Turn on the JavaScript console. Default: _off_.

### Enable voice commands

Enable voice command support: Default: _off_.

### Enable hardware acceleration

Turn on use of hardware rendering on Android 10 and later. Default: _off_

### Enable split window property editor

Enable displaying the property editor in a separate window if available. Default: _off_

### Use "new task" mode for property editor

Set FLAG_ACTIVITY_NEW_TASK when starting the property editor, this may improve the behaviour of certain launchers when resuming the app. Default: _off_