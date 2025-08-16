# Tag filter

Vespucci provides a simple mechanism to filter displayed and editable objects based on selecting keys and values with regular expressions. Ways that are not selectable and visible will be drawn with light grey lines. All edits and additions to the filter entries are immediately stored on your device.

The tag filter is enabled by selecting the corresponding menu entry in the overflow menu on the main map display. Once the filter is active the configuration can be changed by touching the filter button (either on the right or left side in the vertical middle of the map display), a long press will show a short cut menu that will allow selection of a filter configuration. 

## Actions  

### + Add

Add a new filter entry.

### ![More](../images/menu_more.png) More

Display a menu with additional actions:

#### Load...

Load filter entries from a different configuration. 

#### New...

Specify a name for a new configuration and switch to it.

#### Clear

Remove all entries from the current configuration.

#### Delete

Delete a configuration completely. Note that the _Default_ configuration cannot be deleted currently. 

### ![Help](../images/menu_help.png) Help

Start the Vespucci Help browser with this content.

## Filter entries

Default, no entries state, is to not include any objects. Each entry can be enabled/disabled with the checkbox and are applied in the top-down order as they are displayed. 

Selecting the "+" mode will include objects that match, "-" will exclude* objects. 

OSM object type can be either *All*, *Nodes*, *Ways*. *Ways+* (automatically include way nodes of matched ways), *Relations* and *Relations+* (automatically include way nodes of matched ways). Included relations will include their member node and way objects, relations will not be automatically included.

Regular expressions can be entered in the Key and Value text fields, an empty field will match all strings. Combination of tags is currently not supported.

Clicking the trash can icon will delete a entry.

&ast;__Limitations:__ currently an entry for a relation that _excludes_ the object will not exclude its members. Excluding a way with *Ways+* set will exclude the way nodes, except if they are tagged (then the relevant filter rules will apply) or if it is a member of other ways that are not excluded with *Ways+*. In the later case the behaviour depends on the order of way processing 
and cannot be determined in advance. 


  
