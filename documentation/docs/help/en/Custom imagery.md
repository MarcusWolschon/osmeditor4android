# Custom imagery

## Supported layer types

* google/OSM type tile servers (raster tiles and to a limited extent Mapbox Vector Tiles)
* imagery layers in [MBTiles](https://github.com/mapbox/mbtiles-spec) and [PMtiles V3](https://github.com/protomaps/PMTiles/blob/main/spec/v3/spec.md) format on device
* WMS servers supporting images in EPSG:3857 (including alternative names like EPSG:900913 etc) and EPSG:4326 projections

Besides manually adding layers you can add WMS layers by querying a WMS server for a list of supported layers see [layer control](Main%20map%20display.md#layer_control), or by querying the Open Aerial Map (OAM) service.

__NOTE__ even though the URLs may look similar to those for a WMS layer, we do not support requesting data from ESRI MapServer with their native protocol.

## Adding a custom imagery source

__NOTE__ only a subset of all possible attributes can be set in the form, if you need access to more parameters, see [adding custom imagery from a file](https://vespucci.io/tutorials/custom_imagery/#adding-custom-imagery-sources-configuration-from-a-file).

To add a custom layer goto the _Preferences_ screen and select _Custom imagery_, press the _+_ button to add a new layer, or you can select the same from the layer control, "+" menu. In the form you can set

* a __Name__ this is what is displayed in the _Layer control_ background or overlay selection form. This field is _required_.
* an __URL__ the URL for the source with place holders. This field is _required_.
* the __Overlay__ flag, indicating that the layer is not a background, but partially transparent images for displaying over a background layer. If this flag is _not_ set, layers that are below this one will be considered to be invisible and will not be rendered.
* __Coverage__ left, bottom, right and top coordinates of a coverage bounding box in WGS84 coordinates, if the values are empty it is assumed that the layer covers the whole visible web-mercator area, that is -180°, -85°, 180°, 85°.
* __Zoom__ _Min_ and _Max_ zoom levels, these indicates the minimum and maximum zoom levels available and are important for the app to determine over- and under-zoom correctly.
* __Tile size__ side length in pixels for the tiles, default 256. _Available from version 16.0 and later_

Selecting the _Save_ button will add the source to the configuration, _Save and set_ will additionally add the source to the active layers as the current background for background imagery sources.

### Remote PMTiles source

To add a remote PMTiles source add the URL and then select _Save_. Vespucci will attempt to retrieve all necessary information for the entry from the remote source if the URL ends with _.pmtiles_.

### Supported place holders

Place holders are replaced when the application retrieves imagery files from the source and are replaced by calculated values. There are some variants even between applications that in principle use the same system, which are noted for completeness sake below.

The place holders have the general format of __{__ _place holder name_ __}__.

__{apikey}__ api key for sources that require it. _JOSM_, _iD_, _Vespucci (only in configuration files)_ see [key configuration example](https://github.com/MarcusWolschon/osmeditor4android/blob/master/src/main/assets/keys2-default.txt) for more information.

__{zoom}__ the zoom level

__{x}__ the x tile number

__{y}__ the y tile number

__{-y}__ the y tile number for sources using TMS tile numbering. _JOSM_, _iD_,  _Vespucci_

__{ty}__ alternative for the y tile number for sources using TMS tile numbering. _iD_, _Vespucci_

__{switch:a,b,c}__ rotating server selection (replace _a,b,c_ by the actual sub-domains). _JOSM_, _iD_, _Vespucci_

__{quadkey}__ used for Bing. _Vespucci_

__{proj}__ projection for WMS servers. _JOSM_, _Vespucci (only in configuration files)_

__{wkid}__ projection for proprietary ESRI servers. _JOSM_, _Vespucci (only in configuration files)_

__{width}__ tile width for WMS servers. _JOSM_, _Vespucci_

__{height}__ tile height for WMS servers. _JOSM_, _Vespucci_

__{bbox}__ bounding box in _proj_ coordinates for WMS servers. _JOSM_, _Vespucci_

__{subdomain}__ reserved, used internally by _Vespucci_

##### Required placeholders

Note: remote PMTiles sources do not require any placeholders.

* A valid normal (non-Bing) URL for a tile server must contain at least at least __{zoom}__, __{x}__ and one of __{y}__, __{-y}__ or __{ty}__.
* A valid WMS entry must be a legal WMS URL for a layer containing at least __{width}__, __{height}__ and __{bbox}__ place holders. Note: do not add a __{proj}__ place holder when adding such a layer in the "Custom imagery" form in Vespucci (it is supported in the configuration files), simply leave the SRS or CRS attribute in the URL as is with the desired projection value.

### Examples

Tile server example:

    http://tiles.poole.ch/AGIS/OF2011/{zoom}/{x}/{y}.png

WMS server example:

    https://geodienste.sachsen.de/wms_geosn_dop-rgb/guest?FORMAT=image/jpeg&VERSION=1.1.1&SERVICE=WMS&REQUEST=GetMap&LAYERS=sn_dop_020&STYLES=&SRS=EPSG:3857&WIDTH={width}&HEIGHT={height}&BBOX={bbox}
    
PMTiles example:

    https://r2-public.protomaps.com/protomaps-sample-datasets/overture-pois.pmtiles

## MBTiles and PMTiles

To add a MBTiles or PMTIles layer, down/upload the file to your device, then go to the _Custom imagery_ form as described above. Tap the _SD card_ icon, navigate to the folder where you saved the file and select it. The form should now be filled out with all necessary values.

Notes:

* Older MBTiles formats do not contain meta-data for min and max zoom levels, the query that determines the values from the actual file contents may take a long time to run if the file is large.
* Vespucci currently supports png, jpg and mvt contents.
* You can adjust all values in the form before saving if necessary with exception of the URL field that contains the path to the file.
* You can create MBTiles files for example with [MOBAC](https://sourceforge.net/projects/mobac/) and many other tools, some more information can be found on the [HOT toolbox site](https://github.com/hotosm/toolbox/wiki/4.5-Creating-.mbtiles) and the [separate tutorial about the generation of custom MBTiles files](custom_imagery_mbtiles.md). 
