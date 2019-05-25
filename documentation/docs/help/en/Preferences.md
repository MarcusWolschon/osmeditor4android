# Preferences

The settings for Vespucci are split over two screens: standard and advanced preferences. The more commonly used settings should in general be in the first screen.

## Map background

Select the tiled imagery background layer. Selection will be filtered to layers that cover the current map view.

## Map overlay

Select the tiled imagery overlay layer. Selection will be filtered to layers that cover the current map view.

## Custom layers

Configure a custom imagery layer not included in the standard configuration.

## Scale display

Configure the scale or grid displayed.

## Data style

The style that is used to render OpenStreetmapData. Additional style can be added by including corresponding files in the Vespucci directory.

## Display tasks

Turn on the task layer (currently supporting OSM Notes, OSMOSE bugs, Maproulette and custom tasks).

## Enable Photo layer

Turn on the photo layer. When enabled the Vespucci directory and others will be scanned for geo-referenced photographs that will be displayed with a camera icon and are selectable (selecting will try to start a photo viewer on the device).

## Keep screen on

Disable the Android automatic screen lock.

## Large node drag area

Display a large ring around selected nodes making draging and positioning nodes easier.

## Presets

Start preset management screen. Vespucci supports JOSM style presets that can be downloaded and added from this screen.

## Validator settings

### Validator settings

Configure the standard validator.

There are currently two configurable checks: a check for missing keys and an aged based one for re-surveying.

The missing tag check works on the combination of the keys that should be checked and the preset for element.

Example: the default configuration checks that a "name" tag is present on a object if the matching preset contains a "name" field in the non-optional tags, optionally the check can be extended to require "optional" tags.

### Connected node tolerance

Configure how far away from a highway a highway end node must be to not be highlighted as an error.

## Opening hours templates

The dialog displayed allows to edit the meta-data for individual templates or delete them, save the current templates and load templates from files.

## Advanced preferences

Starts the advanced preferences editor.