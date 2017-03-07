# Controlling Vespucci from other apps
_by Simon Poole_

Android supports a powerful mechanism for starting and controlling other apps that goes be the names of ["Intents"](https://developer.android.com/guide/components/intents-filters.html). Every app needs to register which Intents it can support and the Android operating system will route requests to handle a specific Intent to appropriate app(s), if there is more than one app available you will get the well known app chooser. 

##Supported intents

__Load a preset__

vespucci:/preset?preseturl=*url*&presetname=*name*

*url*: URL of a JOSM format preset file or zip archive (including icons)

*name*: optional name of the preset

Load a preset from a website and store it in internal storage, the preset can replace or be used together with the default and other presets.

__Zoom to a bounding box__

http://127.0.0.1:8111/zoom?left=*left*&bottom=*bottom*&right=*right*&top=*top*

https://127.0.0.1:8112/zoom?left=*left*&bottom=*bottom*&right=*right*&top=*top*

josm:/zoom?left=*left*&bottom=*bottom*&right=*right*&top=*top*

*left*,*bottom*,*right*,*top*: bounding box definition in WGS84 coordinates

JOSM style remote control, zoom to the specified bounding box.

__Load data and optionally select objects__

http://127.0.0.1:8111/load\_and\_zoom?left=*left*&bottom=*bottom*&right=*right*&top=*top*&select=*osmobjects*

https://127.0.0.1:8112/load\_and\_zoom?left=*left*&bottom=*bottom*&right=*right*&top=*top*&select=*osmobjects*

josm:/load\_and\_zoom?left=*left*&bottom=*bottom*&right=*right*&top=*top*&select=*osmobjects*

*left*,*bottom*,*right*,*top*: bounding box definition in WGS84 coordinates

*osmobjects*: optional, comma separated list of OSM objects in the form *objectID*, ie node1111111,way1234456. 

For more information see http://wiki.openstreetmap.org/wiki/JOSM/Plugins/RemoteControl, note: this has a different format than the JOSM object selection command. The "josm:" scheme Intents are supported from version 0.9.8, build 1242 on.

__geo URLs__

geo:*lat*,*lon*?z=*zoom_level*

*lat*,*lon*: WGS84 coordinates
*zoom\_level*: zoom level, not supported

Partial implementation of geo Urls, only WGS84 coordinates are supported, an area of the size of the current auto-download area is downloaded around the specified location.

__Happy mapping!__ 
