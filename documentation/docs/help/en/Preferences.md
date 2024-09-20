# Preferences

The settings for Vespucci are split over two screens: standard and advanced preferences. The more commonly used settings should in general be in the first screen.

## Data style

The style that is used to render OpenStreetmap data. Additional styles can be added by including corresponding files in the Vespucci directory. Documentation on the style file format can be found online here [Vespucci Data Styling](http://vespucci.io/tutorials/data_styling/).

## Custom layers

Manage custom imagery layers not included in the standard configuration. This supports configuring both remote tile sources with a URL or on device MBTiles.

See [custom imagery help](Custom%20imagery.md) for more information.

## Keep screen on

Disable the Android automatic screen lock.

## Large node drag area

Display a large ring around selected nodes making draging and positioning nodes easier.

## Enable way node dragging

Enables dragging of individual way nodes of a selected way without selecting the node first, this can make geometry editing substantially faster in some circumstances.

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

#### Enabled validations

By default all validations are enabled, if you want to disable individual checks, you can do that here. 

 - __Object age__ re-survey validation
 - __FIXME tags__ check for fixme tags
 - __Missing tags__ preset required missing tags check
 - __Highway road__ report was with highway=road
 - __No relation type__ missing type tag on a relation
 - __Imperial units missing__ in the US and UK missing units for values that are likely imperial measurement values
 - __Invalid object__ OSM element that isn't valid, for example a way without nodes
 - __Untagged object__ untagged object that is not a child object (aka a way node) or member of a relation
 - __Unconnected end node__ report end nodes of ways tagged with highway that should likely be joined to a nearby highway
 - __Degenerate way__ way with just one node
 - __Empty relation__ relation without members
 - __Missing relation member role__ report relation members that should have a role, but don't
 - __Relation loop__ relations that refer to eachother in a loop
 - __Non-standard element type__ report if the element type of an object isn't one of the preset required ones
        
### Connected node tolerance

Configure how far away from a highway a highway end node must be to not be highlighted as an error.

## Opening hours templates

The dialog displayed allows to edit the meta-data for individual templates or delete them, save the current templates and load templates from files.

## Nearby POI display

Configure the keys used for the nearby POI display.

## Advanced preferences

Starts the advanced preferences editor.