# Generating MBTiles files with custom imagery
_by Manfred Stock_

Current Vespucci versions [support the addition of custom imagery sources](custom_imagery.md), including [sources based on MBTiles](custom_imagery.md#mbtiles). This tutorial will show a few approaches to create your own [MBTiles](https://github.com/mapbox/mbtiles-spec) files that can then be used with Vespucci.

All approaches support either regular WMS or tile servers as imagery sources, some of them also both or local files in e.g. GeoTIFF format. The [editor layer index](https://github.com/osmlab/editor-layer-index), which is also used by several editors including Vespucci, provides many imagery sources that can be used for OpenStreetMap. The GeoJSON files in the [`sources` directory](https://github.com/osmlab/editor-layer-index/tree/gh-pages/sources) usually contain all the information required to access these imagery sources.

Other services that are useful in this context are the following:

* [OSM Admin Boundaries Map](https://wambachers-osm.website/boundaries/): This allows to retrieve boundaries in various formats which can be useful to limit the extent of the resulting MBTiles file to e.g. a single town.
* [Tile Calculator](https://tools.geofabrik.de/calc/#type=geofabrik_standard&tab=1&proj=EPSG:4326&places=4): This allows you to select rectangular bounding boxes and get the coordinates, which is also useful to select a smaller area.

*A word of warning*: Using these tools can result in a _large_ number of requests to the services that provide the imagery, even for small areas (like smaller cities) when using the higher zoom levels (e.g. 19 and up). So please try to download only small areas, configure and/or use caching and don't run multiple instances of the tools in parallel. Otherwise, those who provide the services may block you or even consider to stop providing the public service if they for example think that it is being abused. Using smaller areas also results in smaller files so they will take up less space on your mobile phone or tablet.

## Mobile Atlas Creator

[MOBAC](https://mobac.sourceforge.io/) is a Java-based GUI application which can also generate MBTiles files.

## GDAL

[GDAL (Geospatial Data Abstraction Library)](http://www.gdal.org/) is a library for reading and writing geospatial data formats.

## MapProxy

[MapProxy](http://mapproxy.org/) is an open source proxy for geospatial data.

## Other tools and resources

* [TileHuria](https://github.com/HumanitarianStuff/tilehuria) is a set of minimal utilities to download imagery and create MBtiles files.
* [gdal2mbtiles](https://github.com/ecometrica/gdal2mbtiles) can convert GDAL-readable datasets into an MBTiles file.
* [Exporting rasters to Mbtiles using GDAL](https://pvanb.wordpress.com/2017/03/06/raster2mbtiles/)
* [Creating OSM Offline tiles using Maperitive](https://osedok.wordpress.com/2015/02/25/creating-osm-offline-tiles-using-maperitive/)
* [Creating .mbtiles (hotosm/toolbox)](https://github.com/hotosm/toolbox/wiki/4.5-Creating-.mbtiles)
