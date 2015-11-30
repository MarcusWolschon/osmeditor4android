# Version 0.9.5 #

2014-08-01:

  * Auto download
  * Added function to add node at current GPS position (visible in the long press menu).
  * Fast address tags adding with prototype house number prediction
  * Split action bar on newer phones and more top bar buttons on tablets
  * Import and upload of GPS tracks
  * On device help.
  * Basic conflict resolution
  * Substantially better icons for on map display
  * Standard JOSM preset extended to include further object categories
  * Remove unnecessary and unneeded tags automatically
  * JOSM compatible OSM file reading and saving (requires a file explorer or similar for now to select files for reading)
  * Refactored relation code should reduce memory footprint and improve performance
  * Support for external sources of GPS information (for example RTKLIB)

and naturally lots of bug fixes and stability improvements.

# Version 0.9.4 #

2014-03-24: This release contains a lot of .under the hood. improvements and some work on making the UI more consistent and easier to use.

In particular the following changes have been made

  * selectable overlay layer.
  * support for multiple simultaneous presets.
  * added find action to lookup location with nominatim.
  * add per zoom level imagery offsets with support for querying and saving to the imagery offset database or manual entry.
  * added support for name suggestions and auto preset setting.
  * added goto current GPS location.
  * added action to arrange nodes of a closed way in a circle.
  * limited support for geo: URIs and JOSM style remote control.
  * add action to directly set position of node by entering coordinates.
  * major rework of imagery provider configuration, now based on https://github.com/osmlab/editor-imagery-index .
  * make https API default.
  * major refactoring of projection code.
  * lots of bug fixes and stability improvements.


# Version 0.9.3 #

2013-12-15: Improvements centered around making geometry editing easier and more efficient.

  * Lots of issues resolved
  * Added "geometry improvement" handles (small "x"s like in iD or JOSM). Dragging such a handle on a non-selected way will add a node at the location, selection tolerance is that same as for ways.
  * Experimental fix to the "fat finger problem", by providing a large drag area for nodes when selected. This can be turned on in the preferences.
  * Updated presets and icons.
  * Improved map styling
  * Added better handling of the tile cache: total cache size can be limited (see the advanced preferences) default is 100MB, if the cache becomes near full the least recently used tiles (from any provider) are removed. Further the tiles of the current provider can be flushed (resulting in a new download) by selecting on the main screen Menu->Tools->Flush tile cache (note: this currently happens synchronously and can take quite a long time).
  * Updated [Tutorial](Tutorial.md)

# Version 0.9.2 #

2013-10-20: Relation editing nearly feature complete.

  * Lots of issues resolved
  * Preference to use back button for undo
  * Made better profile/map style default
  * Relation creation mode and add member to relation,
  * Preference added to leave screen on as long as Vespucci is app on top.
  * Incremental data download.
  * Split closed way mode.

# Version 0.9.0 #

2013-08-27: Major new features and change of default user interface. See [RelationSupport](RelationSupport.md) for more information.

  * Lots of issues resolved
  * Initial relation support
  * OAuth support
  * Some changes to the rendering were made. landuse and tracks now have separate rendering, house color was changed. The rendering is now user selectable the old "everything black" style is available as "Default", this one as "Color". Further styles can be added, but please be aware that the current implementation is not likely to be long lived.
  * Long press on a preset in the tag editor will remove it from the preset list.
  * Added the trivial changes for basic Samsung multi window support
  * Merging of ways now tries to preserve existing way ids and uses conventional tag merging logic
  * Reversing a way, either explicitly in EasyEdit mode or implicitly by merging ways with different directions, will now reverse direction dependent tags. If a oneway tag is present, the explicit reversing will not change the value of oneway, assuming that this was the point of reversing in the first place.
  * Reversing a "non-reversible" way will display a warning.
  * Replaced OSM bugs support with support for the new Notes API (note the XML version of the Notes API still has some issues which is the reason for the "unknown action" string in the display)
  * Quick way of re-enabling GPS following: The GPS marker will turn to white (default for now) when following and back to the standard blue when following has been turned off. Clicking/touching the marker will turn following back on. Initially the following still has to be turned on via the menu.
  * "old" editing modes can be suppressed via a preference, default is to only show lock/unlocked symbol to switch from "Move" and "EasyEdit" mode
  * Move/drag way
  * Copy, cut and paste of nodes and ways
  * Experimental geo-referenced photo layer. Vespucci will try to find photographs in DCIM, osmtracker, and Vespucci directories on internal and external storage.

# Version 0.8.0 #

2012-10-02: Version 0.8.0 is now available! It's been more than a year since 0.7.0 was released and we have lots of new features:

  * New Honeycomb style Action Bar user interface, even on pre-HC devices.
  * New EasyEdit editing mode that unifies all previous editing modes (which are still available) into one mode.
  * Support Undo/Redo.
  * Show way direction.
  * Support way reversal and deletion.
  * Add JOSM preset support in tag editor, customisations can be added.
  * When tracking position, the direction you're pointing/travelling is shown.
  * Export GPS tracks as GPX files.
  * Transfer/Other Location shows coordinates of last location.
  * Allow installing to SD card.
  * Allow nothing as a background, drop Osmarender.
  * Allow downloaded data to be saved as a JOSM-compatible OSC file.
  * Add support for Croatian, Danish, Japanese, Norwegian, Polish, Portuguese, and Russian languages.
  * Add crash error reporting using ACRA.
  * Support alternate OpenStreetMap APIs.
  * Many other bug fixes and improvements.

