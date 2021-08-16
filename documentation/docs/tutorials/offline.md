# Going completely offline with Vespucci

_This is based on two OpenStreetMap diary posts from late 2018_

<p align="center">
  <img src="https://upload.wikimedia.org/wikipedia/commons/thumb/4/43/Caf%C3%A9_Wall_Illusion.svg/500px-Caf%C3%A9_Wall_Illusion.svg.png" />
</p>

Vespucci has supported unconnected mapping since day one, more than a decade ago, and that has improved over the years with addition of support for reading and saving files in OSM and JOSM xml format (the JOSM format stores information on your changes).

In version 10.1, in early 2018, we added support for MBTiles files for local on device background imagery sources, supporting building an imagery source while you are online on your desktop and avoiding having to download to the Vespucci imagery cache manually, a rather tiresome undertaking.

But despite all of this, the main problem remained that you had to keep OSM data files on device reasonably small because the contents would be read in total in to memory and while such areas can be quite large on a modern phone, things do tend to get slow. And as we all know when you  make a selection in advance, Murphy comes in to play and you are surely are going to miss exactly the area that you suddenly stumbled in to. 

To get around all of that I've been investigating a compact, indexed in one way or the other, on device format for raw OSM data for quite a while, and now have a solution that works even better than originally envisioned.

The format is a MBTiles format sqlite database containing tiled OSM data in PBF format. The format goes back to 2011 and the application [mapsplit](https://github.com/simonpoole/mapsplit) created by well known OSMer Peter Barth, at the time used to prepare input for [OSM2World](http://osm2world.org/). The slight rework that I started in late 2018 adds support for producing output in MBTile format (instead of copying a couple of 100'000 or so files to your mobile), higher and variable maximum zoom level for the tiles and an optimisation pass. 

When generated from OSM source data that has referentially complete ways, the individual tiles are referentially complete too. That means a tile will contain all nodes referenced by ways included in the tile, and all relations that have members in the tile (but not all members of those relations). If input data contains metadata fields (that is OSM id, version and date) the resulting files can be used by OSM editors just as data from the API. Naturally the data will be at least a bit stale and that needs to be taken in to account when using it in an editor.

Having all of Switzerland (not exactly an empty country in OSM) currently will use 1.1GB, which compares quite well to the original PBF file it was generated from (421MB). Some of the overhead is due to having to duplicate ways crossing tile boundaries and including relations in every tile that has a member element and tiles with little content not compressing well.

To update the tiles it is currently necessary to re-tile the data (after either downloading a new extract or keeping the extract up to date from the diffs). This is not a big deal, generating the tiles for Switzerland from scratch only takes a couple of minutes, but naturally the holy grail is to update in situ on device, doing away with the need for a desktop completely. There are some hurdles that need to be taken before we are there, but it seems to be completely possible. Even if that turns out to be not practical there are some potentially interesting intermediate steps that I'm exploring, for example updating the tiles with the local edits (after they have been uploaded and received definitive IDs).

While you can simply download the [current release](https://github.com/simonpoole/mapsplit/releases) and generate files yourself, to make life easier for user that just want to quickly try things out, I'm generating some files for some regions on a daily basis on [https://mapsplit.poole.ch/](https://mapsplit.poole.ch/). Within reason I can add more if there is interest.

To configure MapSplit files as a data source in Vespucci, see the [13.0 release notes](http://vespucci.io/help/en/13.0.0%20Release%20notes/#add-support-for-mapsplit-tiled-osm-data).