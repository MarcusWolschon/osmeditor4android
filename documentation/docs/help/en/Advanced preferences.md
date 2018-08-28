# Advanced preferences

The Advanced preferences page is split in to seven topical sections that can be access by clicking on the main entries. Depending on the Android version running on your device some settings may be not available for technical reasons.

## User Interface Settings

### Show node icons

Show icons on nodes if they can be matched with a preset. Default: on.

### Show POI icons on buildings

Show icons on buildings if the tags on the building can be matched with a preset. Default: on.

### Display tag form

Enable the form based tag editor. Default: on.

### Show camera action

Show a camera button on the main display (if a camera is present). Default: on.

### Follow location button layout

Change the side of the display the "follow GPS" button is located on. Default: lefthand side.

### Always dim non-downloaded areas

If on the non-downloaded areas will be dimmed when the screen is locked. Default: off.

### Full screen mode

Configure the behaviour on devices with "soft" buttons. Default: on Android 4.4 and later: automatic, 4.0 - 4.3: off, ealier versions: not available.

### Show tolerance

Show halos indicating the size of the "touch areas" around elements. Default: on.

### Use back key for Undo

Use the back key for undo. Default: off.

### Enable light theme

Use a theme with a light background. Default: on.

### Enable split action bar

Show the menu buttons at the bottom of the screen. Default: on.

### Max. number of inline values

Maximum number of values that will directly be displayed in the form based editor for Tags with pre-determined values. Default: 4.

### Time to wait before auto-locking

How long to wait before auto-locking the map display, setting the value to 0 disables the auto-locking. Default 60 seconds.

### Enable Anti-Aliasing

Use anto-aliasing when rendering OSM data. Default: on.

### Max line width

Maximum width lines will increase to when zooming in. Default: 16 pixels.

## Data and Editing Settings

Settings related to editing.

### Always use context menu

Always show the element selection context menu if the selection is ambiguous. If turned off the nearest element will be selected. Default: off.

### Address tags

When using address prediction only add the tags configured here.

### Enable name suggestions

Support special handling of name tags with canonical name suggestions. Default: on.

### Enable name suggestion presets

When using name suggestions, automatically apply the best preset. Default: on.

### Generate notifications

Generate notifications for validator detected issues. Default: on.

### Max. number of displayed notifications

Maximum number of notifications retained in the status bar: 5 (per type).

### Max distances for notifications

Maximum distance a validation issue or note/bug can be from the current positions to trigger a notification. Default: 100 meters.

### Download radius

When auto-downloading, the radius of the area that is attempted to download around the current position. Default: 50 meters.

### Maximum auto-download speed

Maximum speed up to which auto-download is attempted. Default: 6 km/h.

### Filter for Notes/Bugs

Select OSMOSE error leval and Notes to download. Default: everything.

### Notes/Bugs Download radius

When auto-downloading notes and bugs, the radius of the area that is attempted to download around the current position. Default: 200 meters.

### Maximum notes/bugs auto-download speed

Maximum speed up to which auto-download of notes and bugs is attempted. Default: 30 km/h.

### Close changesets

Close the current open changeset after uploading edits. Default: on.

## Location Setting

GPS and Network location settings.

### GPS source

Source if GPS location updates. Default: internal.

### NMEA network source

If GPS source is set to one of the NMEA TCP options, configure on which IP address and port the client or server is.

### Minimum GPS-Interval

Minimum interval between updates for the internal GPS source. Default: 1000 ms.

### Minimum GPS-Distance

Minimum distance between updates for the internal GPS source. Default: 5 meters.

### Leave GPS disabled

If GPS has been disabled by the user, don't ask to turn it on. Default: off.

### Fallback to network location

IF the device is providing "Network" location data use it as a fallback if we haven't received a GPS location for a longer time. Default: on.

## Server Settings

OpenStreetMap API and other servers configuration.

### OSM API URL

Select and add custom OpenStreetMap API servers.

### User Account

Set a user and password for API authentication. This is only needed if the target API server does not support OAuth authentication, and in general should be avoided as unsafe.

### Offset Server

API server for imagery offsets.

### OSMOSE Server

OSMOSE QA API server configuration.

### Configure geocoders

Geocoding service providers. Currently Photon and Nominatim servers are supported.

## Layer download and storage

Download and storage configuration for the tiled imagery layers.

### Max. number of download threads

Maximum number of simultaneous download threads. Default: 4.

### Tile cache size

Total on device size for caching imagery tiles. Default: 100MB.

## Miscellaneous

### Report application crashes

Submit ACRA crash dumps to the Vespucci developers. Default: on.

### Show stats

Show some uninteresting stats on screen. Debugging use only. Default: off.

## Experimental

### Enable JS Console

Turn on the JavaScript console. Default: off.

### Enable voice commands

Enable voice command support: Default: off.