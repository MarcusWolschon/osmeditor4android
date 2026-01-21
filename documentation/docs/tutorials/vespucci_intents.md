# Controlling Vespucci from other apps
_by Simon Poole_

Android supports a powerful mechanism for starting and controlling other apps that goes by the name of "[Intents](https://developer.android.com/guide/components/intents-filters.html)". Every app needs to register which Intents it can support and the Android operating system will route requests to handle a specific Intent to appropriate app(s). If there is more than one app available you will see the familiar app chooser. 

## Supported intents

__Load a preset__

vespucci:/preset?preseturl=*url*&presetname=*name*

*url*: URL of a JOSM format preset file or zip archive (including icons)

*name*: optional name of the preset

Load a preset from a website and store it in internal storage, the preset can replace or be used together with the default and other presets.

More information on using presets in Vespucci can be found in the [preset documentation](/presets).  

__Load a style__

vespucci:/style?styleurl=*url*&stylename=*name*

*url*: URL of a Vespucci style file or zip archive (including icons)

*name*: optional name of the style

Load a style from a website and store it in internal storage.

More information on using presets in Vespucci can be found in the [data styling documentation](/data_styling). 

__Zoom to a bounding box__

~~http://127.0.0.1:8111/zoom?left=*left*&bottom=*bottom*&right=*right*&top=*top*~~

~~https://127.0.0.1:8112/zoom?left=*left*&bottom=*bottom*&right=*right*&top=*top*~~

josm:/zoom?left=*left*&bottom=*bottom*&right=*right*&top=*top*

*left*,*bottom*,*right*,*top*: bounding box definition in WGS84 coordinates

JOSM style remote control, zoom to the specified bounding box.

__Load data and optionally select objects__

~~http://127.0.0.1:8111/load\_and\_zoom?left=*left*&bottom=*bottom*&right=*right*&top=*top*&select=*osmobjects*~~

~~https://127.0.0.1:8112/load\_and\_zoom?left=*left*&bottom=*bottom*&right=*right*&top=*top*&select=*osmobjects*~~

josm:/load\_and\_zoom?left=*left*&bottom=*bottom*&right=*right*&top=*top*&select=*osmobjects*

*left*,*bottom*,*right*,*top*: bounding box definition in WGS84 coordinates

*osmobjects*: optional, comma separated list of OSM objects in the form *objectID*, ie node1111111,way1234456. 

Further supported attributes:

*changeset_comment*: a draft changeset comment. (added in version 13.1)
*changeset_source*: a draft changeset source. (added in version 13.1)

__Load objects__

josm:/load\_and\_object?objects=*osmobjects*

*osmobjects*: optional, comma separated list of OSM objects in the form *abbreviationID*, ie n1111111,w1234456. 

Downloads the objects if they are not already present. (added in version 20.0)

__Configure imagery__ (added in version 13.1)

josm:/imagery?title=*title*&type=*type*&min\_zoom=*min\_zoom*&max\_zoom=*max\_zoom*&url=*url*

*title* the title/name of the layer
*type* tms or wms
*min\_zoom* minimum zoom level
*max\_zoom* maximum zoom level
*url* url with place holders (note for WMS servers the proj value in the URL should not be replaced by a place holder)

Configure and activate an imagery layer.

__geo URLs__

geo:*lat*,*lon*?z=*zoom_level*

*lat*,*lon*: WGS84 coordinates
*zoom\_level*: zoom level, not supported

Partial implementation of geo URLs, only WGS84 coordinates are supported, an area of the size of the current auto-download area is downloaded around the specified location.

## Notes on JOSM style remote control

Directly using the local host IP address with a port will not work on modern Android variants. Your application needs to generate URLS like the following for use in a web page/app:

    intent:/load_and_zoomleft=8.3844600&right=8.3879800&top=47.3911300&bottom=47.3892400&changeset_comment=thisisatest&select=node101792984#Intent;scheme=josm;end;

for the __load\_and\_zoom__ command, and        
        
    intent:/imagery?title=osmtest&type=tms&min_zoom=2&max_zoom=19&url=https://a.tile.openstreetmap.org/%7Bzoom%7D/%7Bx%7D/%7By%7D.png#Intent;scheme=josm;end;
    
for the __imagery__ command.

## Supported links

From version 20 on Vespucci supports opening links to the OpenStreetMap website referring to individual OSM elements (hosts _openstreetmap.org_, _www.openstreetmap.org_, 
_osm.org_, _www.osm.org_).

Supported paths are _/node/nnn_, _/way/nnn_, /relation/nnn_ and _/note/nnn_ (trailing map hashes will be ignored).

Examples: 
 
    https://www.openstreetmap.org/node/1 

    https://www.openstreetmap.org/note/100
    
Notes 
- This will not work with links on openstreetmap.org itself, but will with other sites.
- Depending on the browser you are using you may need to explicitly allow opening links in apps.
- On modern Android versions you may have to explicitly allow this to work in the app configuration.