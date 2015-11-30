# Presets

Vespucci supports JOSM compatible presets and the default preset is derived from the original JOSM one and kept in sync as far as possible. Just as with JOSM you can add further presets or use a different one than the default. 

The default preset is maintained at [https://github.com/simonpoole/beautified-JOSM-preset](https://github.com/simonpoole/beautified-JOSM-preset). You can install an additional, updated version by selecting [current Vespucci preset](vespucci://preset/?preseturl=https://github.com/simonpoole/beautified-JOSM-preset/raw/master/gen/vespucci_zip.zip) *(this link will only work on your device)* after the download you will have to explicitly enable it and disable the built-in version.

## ![](../images/tag_menu_preset.png) Preset pane

Opens a screen which allows you to navigate through the currently active presets and select one to be applied to the current object. A shortcut button will be added to the Property Editor screen below the tags. The recently used preset screen can be completely emptied with the corresponding menu item, individual presets can be removed by a long press.

The light grey buttons in the preset dialog indicate groups of presets, dark grey are presets. Navigating back to the previous screen is accomplished with the ''Up'' button, the ''Top'' button will navigate back to the initial view.

## Managing presets

To manage the presets go to the "Preferences" screen and then to the "Advanced Preferences", selecting "Presets" will give you a list of the currently available presets. Clicking in the check box will enable/disable the corresponding entry, a long press on the entry (except for the default "OpenStreetMap") will give you a choice of 


* **Edit** - change the name or/and URl of the preset
* **Delete** - remove this entry
* **Update** - re-download the preset *(requires network connectivity)*

**Add preset** will add a new preset, you need to supply an URL and a name. Vespucci will download the preset file and any icons referenced by URL, presets contained in a ZIP-archive are supported too. *(requires network connectivity)*

Most current JOSM presets can be found in this list [http://josm.openstreetmap.de/wiki/Presets](http://josm.openstreetmap.de/wiki/Presets).

Some potentially useful ones for direct download:

 * [Presets for lane tagging](vespucci://preset/?preseturl=http://josm.openstreetmap.de/josmfile%3fpage=Presets/LaneAttributes%26zip=1&presetname=Lane tagging) *(this link will only work on your device)*
 * [Presets for Simple 3D building properties](vespucci://preset/?preseturl=http://zibi.openstreetmap.org.pl/kendzi/k/Simple3dPreset/s3db-preset.zip&presetname=Simple 3D building properties) *(this link will only work on your device)*
 * [Presets for Simple Indoor Tagging](vespucci://preset/?preseturl=http://josm.openstreetmap.de/josmfile%3Fpage=Presets/Simple_Indoor_Tagging%26zip=1&presetname=Simple Indoor Tagging) *(this link will only work on your device)*



