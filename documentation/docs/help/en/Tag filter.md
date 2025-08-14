# Tag filter

Vespucci provides a simple mechanism to filter displayed and editable objects based on selecting keys and values with regular expressions. Elements that are not selectable and visible will be drawn with light grey lines. All edits and additions to the filter entries are immediately stored on your device.

The preset filter is enabled by selecting the corresponding menu entry in the menu on the main map display.

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

Start the Vespucci Help browser on this page.

## Filter entries

Default, no entries state, is to not include any objects. Each entry can be enabled/disabled with the checkbox and are applied in the top-down order as they are displayed. Selecting the "+" node will include objects that match, "-" will exclude objects. OSM object type can be either *All*, *Nodes*, *Ways*. *Ways+* (automatically include way nodes of matched ways), *Relations* and *Relations+* (automatically include way nodes of matched ways). Included ways and relations will include their member objects.

Regular expressions can be entered in the Key and Value text fields, an empty field will match all strings. Combination of tags is currently not supported.

Clicking the trash can icon will delete the entry.
  
