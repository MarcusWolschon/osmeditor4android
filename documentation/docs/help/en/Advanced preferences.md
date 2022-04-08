# Advanced preferences

The Advanced preferences page is split in to seven topical sections that can be access by clicking on the main entries. Depending on the Android version running on your device some settings may be not available for technical reasons.

## User Interface Settings

### Show node icons

Show icons on nodes if they can be matched with a preset. Default: _on_.

### Show POI icons on buildings

Show icons on buildings if the tags on the building can be matched with a preset. Default: _on_.

### Display tag form

Enable the form based tag editor. Default: _on_.

### Show camera action

Show a camera button on the main display (if a camera app is present). Default: _on_.

### Camera app

Select the camera app to use. If your, installed, camera app is not listed, please report this and we will add it to the list. Unluckily google does not allow to automatically determine installed camera apps outside of pre-installed ones. Default: _System default_.

### Follow position button layout

Change the side of the display the "follow GPS" button is located on. Default: _lefthand side_.

### Always dim non-downloaded areas

If on the non-downloaded areas will be dimmed when the screen is locked. Default: _off_.

### Fullscreen mode

Configure the behaviour on devices with "soft" buttons. Default on Android 4.4 and later: _automatic_, 4.0 - 4.3: off, earlier versions: not available. You need to restart the app for changes to this setting to take effect.

### Map screen orientation

If set to any other value than _Auto_ Vespucci will try to override your device settings for screen rotation.

### Show tolerance

Show halos indicating the size of the "touch areas" around elements. Default: _on_.

### Use back key for Undo

Use the back key for undo. Default: _off_.

### Light theme

Use a theme with a light background. Default: _on_. You need to restart the app for changes to this setting to take effect.

### Enable split action bar

Show the menu buttons at the bottom of the screen. Default: _on_. You need to restart the app for changes to this setting to take effect.

### Disable translations

Use English for the user interface. Google does not provide a supported way to switch languages for individual apps, as a result this setting relies on multiple workarounds that may, or may not, work on your device. Preset translations can be disabled in the preset configurations. 

### Max. number of inline values

Maximum number of values that will directly be displayed in the form based editor for Tags with pre-determined values. Default: _4_.

### Time to wait before auto-locking

How long to wait before auto-locking the map display, setting the value to 0 disables the auto-locking. Default _60 seconds_.

### Enable Anti-Aliasing

Use anto-aliasing when rendering OSM data. Default: _on_.

### Max line width

Maximum width lines will increase to when zooming in. Default: _16 pixels_.

### Pending upload OK

Limit below which pending object uploads will be considered OK. Default: _50 objects_.

### Pending upload warning

Limit below which pending object uploads will have warning status. Default: _200 objects_.

### Disable feedback activity

Disable the feedback activity and use an URL instead. Default: _false_.

### Beep volume

Set how load the beep is Vespucci uses when you are pressing unsupported short cut keys on a connected real keyboard. Default: _50_.

### Minimum zoom for Mapillary

Set the minimum zoom level for Mapillary data to be displayed. If set too low the application might crash in areas with very high density Mapillary data. Consider increasing the number if your display if very large or very high resolution. Default: _16_

### Name capitalization

Set the capitalization mode for keyboard input for name-like tag. One of _No capitalization_, _Word capitalization_ and _Sentence capitalization_. Default: _Word capitalization_.

Note: this setting is only applicable when the current locale uses latin script.

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

Select and add custom OpenStreetMap API servers.

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

## Miscellaneous

### Report app crashes

Submit ACRA crash dumps to the Vespucci developers. Default: _on_.

### Show stats

Show some uninteresting stats on screen. Debugging use only. Default: _off_.

## Experimental

### Enable JS Console

Turn on the JavaScript console. Default: _off_.

### Enable voice commands

Enable voice command support: Default: _off_.

### Enable hardware acceleration

Do not use, may cause hangs of the app and other problems.