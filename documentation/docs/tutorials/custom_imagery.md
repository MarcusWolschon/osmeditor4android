# Custom imagery
_by Simon Poole_

For Vespucci 10.1 we've added four long requested features: a simple UI to add custom imagery sources, support for WMS servers that support the same projection as conventional
OpenStreetMap / google map tiles (EPSG:3857 and EPSG:900913), support for layers from [OAM](https://openaerialmap.org/) and support for imagery layers in [MBTiles](https://github.com/mapbox/mbtiles-spec) format.

## Adding a custom imagery source

 ![custom imagery form](images/custom_imagery_empty.png) 
 
To add a custom layer goto the _Advanced preferences_ screen and select _Custom imagery_, press the _+_ button to add a new layer. In the form you can set

* a __Name__ this is what is displayed in the _Preferences_ selection form. This field is _required_.
* an __URL__ the URL for the source with place holders in the same format as the [Editor Layer Index](https://github.com/osmlab/editor-layer-index) (short ELI) that Vespucci and iD use as source for standard imagery. This field is _required_.
* the__Overlay__ flag, indicating that the layer is not a background, but partially transparent images for displaying over a background layer.
* __Coverage__ left, bottom, right and top coordinates of a coverage bounding box in WGS84 coordinates, if the values are empty it is assumed that the layer covers the whole visible web-mercator area, that is -180°, -85°, 180°, 85°.
* __Zoom__ _Min_ and _Max_ zoom levels, these indicates the minimum and maximum zoom levels available and are important for the app to determine over- and under-zoom correctly.

### Supported placeholders

Placeholders are replaced when the application retrieves imagery files from the source and are replaced by calculated values. There are some variants even between applications that in principle use the same system, which are noted for completeness sake below.

The placeholders have the general format of __{__ _placeholder name_ __}__.

__{zoom}__ the zoom level

__{x}__ the x tile number

__{y}__ the y tile number

__{-y}__ the y tile number for sources using TMS tile numbering. _ELI_, _iD_,  _Vespucci_

__{ty}__ alternative for the y tile number for sources using TMS tile numbering. _iD_, _Vespucci_

__{switch:a,b,c}__ rotating server selection (replace _a,b,c_ by the actual sub-domains). _ELI_, _iD_, _Vespucci_

__{quadkey}__ used for Bing. _Vespucci_

__{u}__ used for Bing. _iD_

__{proj}__ projection for WMS servers. _ELI_, _Vespucci (only in configuration files)_

__{width}__ tile width for WMS servers. _ELI_, _Vespucci_

__{height}__ tile height for WMS servers. _ELI_, _Vespucci_

__{bbox}__ bounding box in _proj_ coordinates for WMS servers. _ELI_, _Vespucci_

__{subdomain}__ reserved, used internally by _Vespucci_

* A valid normal (non-Bing) URL for a tile server must contain at least at least __{zoom}__, __{x}__ and one of __{y}__, __{-y}__ or __{ty}__.
* A valid WMS server entry must contain at least __{width}__, __{height}__ and __{bbox}__ placeholders. Note: do not add a __{proj}__ placeholder when adding such a layer in the "Custom imagery" form in Vespucci (it is supported in the configuration files).

### Examples

Tile server example:

    http://tiles.poole.ch/AGIS/OF2011/{zoom}/{x}/{y}.png

WMS server example:

    https://geodienste.sachsen.de/wms_geosn_dop-rgb/guest?FORMAT=image/jpeg&VERSION=1.1.1&SERVICE=WMS&REQUEST=GetMap&LAYERS=sn_dop_020&STYLES=&SRS=EPSG:3857&WIDTH={width}&HEIGHT={height}&BBOX={bbox}
    
## OAM

You can query the OAM catalog by going to _Tools_ and selecting _Add imagery from OAM_, this will query the OAM servers for layers in the current view:

![oam imagery form](images/custom_imagery_oam.png) 

Unluckily it doesn't seem to be customary to give the imagery meaningful names on OAM, but as the use case is likely mostly use of layers that you have created yourself, it is likely bearable. After you've selected an entry you will again be shown the custom imagery dialog. Note: we try to determine the maximum zoom level from the nominal resolution of imagery as stored in OAM, however we've seen a couple of wacky values for this (sub-milimeter) and you should check that the value is roughly correct before saving. 


## MBTiles

To add a MBTiles layer, down/upload the MBTiles format file to your device, then go to the _Custom imagery_ form as described above. Tap the _SD card_ icon, navigate to the folder where you saved the file and select it. The form should now be filled out with all necessary values.

Notes:

* The MBTiles format does not contain meta-data for min and max zoom levels, the query that determines the values from the actual file contents may take a long time to run if the file is large.
* Vespucci currently supports png and jpg contents.
* You can adjust all values in the form before saving if necessary with exception of the URL field that contains the path to the file.
 
![MBTiles configuration](images/custom_imagery_mbtiles.png)

__Happy mapping!__ 
