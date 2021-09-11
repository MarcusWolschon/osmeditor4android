# Preferences

The settings for Vespucci are split over two screens: standard and advanced preferences. The more commonly used settings should in general be in the first screen.

## Data style

The style that is used to render OpenStreetmap data. Additional styles can be added by including corresponding files in the Vespucci directory.

## Custom layers

Configure a custom imagery layer not included in the standard configuration. This supports configuring both remote tile sources with a URL or on device MBTiles.

Most important supported url place holders

- {x} tile x number                      
- {y} tile y number
- {z} {zoom} zoom level
- {ty} {-y} tile y number OGC TMS numbering
                    
WMS server specific place holders

- {proj} EPSG projection number, one of 3857, 900913 and 4326.
- {width} image width.
- {height} image height.
- {bbox} WMS bounding box.

## Keep screen on

Disable the Android automatic screen lock.

## Large node drag area

Display a large ring around selected nodes making draging and positioning nodes easier.

## Presets

Start preset management screen. Vespucci supports JOSM style presets that can be downloaded and added from this screen.

## Validator settings

### Validator settings

Configure the standard validator.

There are currently two configurable checks: a check for missing keys and an age based one for re-surveying.

#### Re-survey check

For objects with the configured tags the validator will check if they have been modified in the number of days set in the rule and if they have not, highlight the objects. *check_date* and *check_date:...* keys with last modified dates are taken in to consideration if they are older than the modification date of the object.

#### Missing tags check

The missing tag check works on the combination of the keys that should be checked and the preset for element.

Example: the default configuration checks that a _name_ tag is present on a object if the matching preset contains a _name_ field in the non-optional tags (the check rules can be configured to require "optional" tags).

Alternative tags can be indicated by separating them with a vertical bar "|". 

Example: "name|ref" will fail only if neither a _name_ or a _ref_ tag is present. Note that is a tag is present that leads to the absence of a tag being ignored (currently supported: _noname=yes_, _validate:no_name=yes_, _noref=yes_ for _name_ and _ref_ tags), the check will terminate with the corresponding missing tag.  

### Connected node tolerance

Configure how far away from a highway a highway end node must be to not be highlighted as an error.

## Opening hours templates

The dialog displayed allows to edit the meta-data for individual templates or delete them, save the current templates and load templates from files.

## Advanced preferences

Starts the advanced preferences editor.