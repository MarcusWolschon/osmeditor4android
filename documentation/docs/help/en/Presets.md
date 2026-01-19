# Presets

Vespucci supports JOSM compatible presets and the default preset is derived from the original JOSM one and kept in sync as far as possible. Just as with JOSM you can add further presets or use a different one than the default. 

The default preset is maintained at [https://github.com/simonpoole/beautified-JOSM-preset](https://github.com/simonpoole/beautified-JOSM-preset). You can install an additional, updated version by selecting <a href="vespucci://preset/?preseturl=https://github.com/simonpoole/beautified-JOSM-preset/releases/latest/download/vespucci_v4_zip.zip&presetname=Updated default preset">current Vespucci preset</a> *(this link will only work on your device)* after the download you will have to explicitly enable it and disable the built-in version.

## ![Preset](../images/tag_menu_preset.png) Property editor preset tab

The _Presets_ tab (on tablets a continuously displayed pane) in the [Property editor](Property editor.md) displays a screen which allows you to navigate through the currently active presets and select one to be applied to the current object. A shortcut button will be added to the _Properties_ tab below the tags. The recently used preset display can be completely emptied with the corresponding menu item, individual presets can be removed by a long press.

The light grey buttons in the preset dialog indicate groups of presets, dark grey are presets. Navigating back to the previous screen is accomplished with the ''Up'' button, the ''Top'' button will navigate back to the initial view.

# Managing presets

To manage the presets go to the "Preferences" screen, selecting "Presets" will give you a list of the currently available presets. Clicking in the check box will enable/disable the corresponding entry, a long press on the entry or a press on the menu icon will give you a choice of:


* **Edit** - change the name or/and URL of the preset, enable/disable translations.
* **Delete** - remove this entry
* **Update** - re-download the preset *(requires network connectivity)*
* **Move up** - move the entry up.
* **Move down** - move the entry down.

Note that the "Built-in preset" cannot be deleted, but it can be disabled.

The **+** button will add a new preset, you will need to supply an URL for the file containing the preset and a name or select a preset file (click the SD-card icon)  that is available on your device. Vespucci will download the preset and any icons referenced by URL in the file, presets contained in a ZIP-archive are supported too. *(requires network connectivity for downloading)*

**Note: presets are evaluated top to bottom in this list, identically named items in earlier entries will override the later ones.**

## Useful presets for download

Most current JOSM presets can be found in [JOSM preset list](http://josm.openstreetmap.de/wiki/Presets#JOSMAvailablepresets). If you are viewing this on your mobile device with Vespucci the preset download urls are modified for direct download and install.

Some further potentially useful presets:

 * <a href="vespucci://preset/?preseturl=https://github.com/simonpoole/beautified-JOSM-preset/releases/latest/download/vespucci_v5.zip&presetname=Updated default preset">Update of default preset</a> *
 * <a href="vespucci://preset/?preseturl=http://josm.openstreetmap.de/josmfile%3fpage=Presets/LaneAttributes%26zip=1&presetname=Lane tagging">Presets for lane tagging</a>
 * <a href="vespucci://preset/?preseturl=https://github.com/kendzi/Simple3dBuildingsPreset/releases/download/0.9_2018-05-08/s3db-preset.zip&presetname=Simple 3D building properties">Presets for Simple 3D building properties</a>
 * <a href="vespucci://preset/?preseturl=http://josm.openstreetmap.de/josmfile%3Fpage=Presets/Simple_Indoor_Tagging%26zip=1&presetname=Simple Indoor Tagging">Presets for Simple Indoor Tagging</a>
 * <a href="vespucci://preset/?preseturl=https://github.com/simonpoole/military-preset/releases/latest/download/military.zip&presetname=Military objects">Presets for military objects</a>
 * <a href="vespucci://preset/?preseturl=https://github.com/simonpoole/US-MUTCD-preset/raw/gh-pages/gen/MUTCD.zip&presetname=MUTCD Access Preset">Presets for US MUTCD access signs</a>
 * <a href="vespucci://preset/?preseturl=https://github.com/simonpoole/xmas-preset/releases/latest/download/xmas.zip&presetname=Xmas Preset">Presets for xmas schema tagging</a>
 * <a href="vespucci:/preset?preseturl=https://github.com/simonpoole/preset-scripting-examples/raw/gh-pages/gen/script_examples.zip&presetname=javascript in Vespucci presets examples">Scripting examples (check date, disused life cycle tag)</a>
 * <a href="vespucci://preset/?preseturl=https://github.com/simonpoole/bicycle-parking-preset/releases/latest/download/bicycle_parking.zip&presetname=Bicycle parking">Bicycle parking with large images</a>

&ast; if you want to replace the default preset with the downloaded version you will need to disable it in the preferences after installing the replacement.


