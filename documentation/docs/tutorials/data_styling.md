## Vespucci Data Styling
_Documentation for Vespucci 22.0 Style file format version 0.3.3_

The data styling configuration is not a work of art, it was created ad hoc (in other words it is an awful hack) to allow slightly more flexible configuration of the rendering.

### Using custom style files in Vespucci

Files need to need have an unique _name_ attribute and the _.xml_ extension and reside in a _styles_ directory in an app specific files directory (for example _Android/data/de.blau.android/files/styles/test.xml_ ). Currently the legacy storage format and location continues to work: unique _name_ attribute and a filename in the format _name_-profile.xml and stored in the _Vespucci_ directory, this is the only location that will work for devices with Android version older than 4.4. 

### Style Elements and Attributes

The styles are for a major part not much more than an external representation of the [Android Paint](https://developer.android.com/reference/android/graphics/Paint) objects. In particular color, style, cap, join and strokeWidth attributes map directly to the Paint fields.

Node styling is limited to the __labelKey__ and __iconPath__ attributes.


|Element                    | Attributes     | Default | Description
|---------------------------|----------------|---------|------------------------------------------------------
|__&lt;profile&gt;__        |                |         | Top level enclosing element
|                           | name           |         | Style name
|                           | format         |         | Format version, currently 0.3.3
|                           | description    |         | Optional description of the style
|                           | version        |         | Optional version string
|__&lt;!--&nbsp;comment&nbsp;--&gt;__ |      |         | A comment
|__&lt;config&gt;__         |                |         | Configuration element
|                           | type           |         | One of "large_drag_area", "marker_scale", "min_handle_len", "icon_zoom_limit"
|                           | touchRadius    |         | 
|                           | scale          |         | Used with marker_scale, increase/decrease size of various markers
|                           | length         |         | User with min_handle_len, defines the minimum length a way segment must have on screen so that the geometry improvement handles are shown
|                           | zoom           |      15 | Used with icon_zoom_limit, minimum zoom level at which POI icons are still displayed
|                           | labelZoomLimit |      20 | Used with icon_zoom_limit, the minimum zoom level for displaying labels with icons
|__&lt;feature&gt;__        |                |         | Feature elements can be nested and each feature can contain one or more other feature elements. Nested elements inherit attributes from their parents. 
|                           | type           |         | "way", "node" or "relation" to match the corresponding OSM elements, or a name
|                           | tags           |         | Tags to use for matching, ignored for named styles, in the format _key_=_value_ or _key_=_*_ for any value. Multiple tags can be added using __&vert;__ as a separator.
|                           | closed         |         | If not present will match all ways, if present will match closed ways if true, or if false open ways, ignored for relations
|                           | area           |         | Use area semantics for rendering if true, note that this will also effect preset matching
|                           | dontrender     |         | Don't render the matching element
|                           | updateWidth    |         | Dynamically update the way width on zoom changes if true
|                           | widthFactor    |         | Determine a way width relative to the extent of the current map shown, ignored if updateWidth is false
|                           | color          |         | A 32bit hex value representing alpha and rgb
|                           | style          |         | One of "FILL", "FILL_AND_STROKE", "STROKE" 
|                           | cap            |         | One of "BUTT", "ROUND", "SQUARE"
|                           | join           |         | One of "BEVEL", "MITER", "ROUND"
|                           | strokeWidth    |         | A float value for the width of the lines, 0 draws a one pixel width line, ignored if updateWidth is true
|                           | offset         |         | Offset in units of the current stroke width
|                           | typefacestyle  |         |
|                           | textsize       |         |
|                           | shadow         |         |
|                           | pathPattern    |         | Reference to a pattern to apply along the path, one of "triangle_left", "triangle_right", "border_left", "border_right"
|                           | minVisibleZoom |      15 | Minimum zoom that has to be reached before the element is rendered
|                           | casingStyle    |         | Reference to a style to use for casing
|                           | arrowStyle     |         | Reference to a style to use for way arrows                          
|                           | oneway         |         | Set this on the referenced arrowStyle if it should have oneway semantics
|                           | labelKey       |         | Tag key to use as label if present, magic value "preset" will use the preset name.
|                           | labelZoomLimit |    none | List for displaying labels on ways, if not set no label will be displayed
|                           | iconPath       |         | Path, relative to the directory in which the style file resides, to a PNG format icon, magic value "preset" (see below) will use the preset icon.
|__&lt;dash&gt;__           |                |         | feature sub-element used to define a dash pattern
|                           | phase          |         | Phase of the dash
|__&lt;interval&gt;__       |                |         | dash sub-element used to define the length of the dash/no-dash phases
|                           | length         |         | Length of the dash as a float

Using _"preset"_ as the value for _iconPath_ will match the objects tags with the presets, just as this is done throughout the application. An _""_ (empty string) value for _iconPath_ will suppress rendering any icon.
                 
### Internal features

Styling for internal features is provided by the app, but they can be overridden by setting them in a style file.

Name                          | Description
------------------------------|----------------------------------------------------------------
gps_track                     | Default style for GPX tracks
infotext                      | 
attribution_text              | Text style for attribution notices on the map
viewbox                       |
way_tolerance                 | Styling for area around a way for touch purposes
way_tolerance_2               | 
way                           | Default way style
selected_way                  | Selected way style
selected_relation_way         | Style for relation member ways when the relation is selected
problem_way                   | Style for a way with an issue
hidden_way                    | Style for faint way rendering (used when filters are active)
node_tolerance                | Styling for area around a node for touch purposes
node_tolerance_2              | 
node_untagged                 | Styling for an untagged node
node_thin                     | 
node_tagged                   | Style for a tagged node without icon
node_drag_radius              | Style for the large drag radius
problem_node                  | Style for a node with an issue
problem_node_thin             | 
problem_node_tagged           | 
selected_node                 | Style for a selected node
selected_node_thin            | 
selected_node_tagged          | 
selected_relation_node        | 
selected_relation_node_thin   | 
selected_relation_node_tagged | 
hidden_node                   | Style for faint node rendering (used when filters are active)
way_direction                 | 
large_drag_area               | 
marker_scale                  | 
gps_pos                       | Styling for the location indicator when not in follow mode 
gps_pos_follow                | Styling for the location indicator when in follow mode
gps_pos_stale                 | Styling for the location indicator when not in follow mode and the location is stale
gps_pos_follow_stale          | Styling for the location indicator when in follow mode and the location is stale
gps_accuracy                  | Styling for the area around the GPS location indicating approximate accuracy
open_note                     | 
closed_note                   | 
crosshairs                    | Styling for the small crosshair display for example when rotating an object
crosshairs_halo               | Style for the halo around the crosshairs
handle                        | Styling for the geometry "improvement" handles on ways
labeltext                     | 
labeltext_normal              | 
labeltext_small               | 
labeltext_normal_selected     | 
labeltext_small_selected      | 
labeltext_normal_problem      | 
labeltext_small_problem       | 
labeltext_background          | 
geojson_default               | Default style for the geojson layer
bookmark_default              | Default style for the bookmark layer
map_background                | Holds the map background colour (from 21.1 on)

### Validation styling

In a limited fashion the default validation styling can be overridden, by adding __feature__ elements with __type__ set to __validation__ and __code__ set to the error value that should be styled. Nodes will only use the colour specified.

#### Error codes ####

These are individual bits that are combined for the final value:

Validation check     | Error value
---------------------|------------
AGE                  | 0x00000002
FIXME                | 0x00000004
MISSING_TAG          | 0x00000008
HIGHWAY_NAME         | 0x00000010
HIGHWAY_ROAD         | 0x00000020
NO_TYPE              | 0x00000040
IMPERIAL_UNITS       | 0x00000080
INVALID_OBJECT       | 0x00000100
UNTAGGED             | 0x00000200
UNCONNECTED_END_NODE | 0x00000400
DEGENERATE_WAY       | 0x00000800
EMPTY_RELATION       | 0x00001000
MISSING_ROLE         | 0x00002000
RELATION_LOOP        | 0x00004000
WRONG_ELEMENT_TYPE   | 0x00008000  

#### Example ####

     <feature type="validation" code="2" updateWidth="true" widthFactor="1.5" color="ffffe000" style="STROKE" cap="BUTT" join="MITER" />


### Complete Example

    <?xml version='1.0' encoding='UTF-8' ?>
    <profile name="Color Round Nodes" format="0.3.0">
        <!-- Assorted config -->
        <config type="min_handle_length" length="200.0" />
        <config type="icon_zoom_limit" zoom="15" />
        
        <!-- Vespucci internal styles -->
        <feature type="gps_accuracy" updateWidth="false" widthFactor="2.0" color="280000ff" style="FILL_AND_STROKE" cap="ROUND" join="ROUND" strokeWidth="0.0" />
        <feature type="problem_node_thin" updateWidth="false" widthFactor="1.0" color="ffff00ff" style="STROKE" cap="BUTT" join="MITER" strokeWidth="1.0" typefacestyle="0" textsize="12.0" />
        <feature type="problem_way" updateWidth="true" widthFactor="1.5" color="ffff00ff" style="STROKE" cap="BUTT" join="MITER" />
        <feature type="way_direction" updateWidth="true" widthFactor="0.8" color="ffb9121b" style="STROKE" cap="SQUARE" join="MITER" />
        <feature type="handle" updateWidth="true" widthFactor="0.8" color="ffb9121b" style="STROKE" cap="SQUARE" join="MITER" />
        <feature type="selected_way" updateWidth="true" widthFactor="2.0" color="fffcfa74" style="STROKE" cap="ROUND" join="ROUND" />
        <feature type="way_tolerance" updateWidth="false" widthFactor="1.0" color="28bd8d46" style="STROKE" cap="BUTT" join="MITER" strokeWidth="40.0" />
        <feature type="way_tolerance_2" updateWidth="false" widthFactor="1.0" color="7fbd8d46" style="STROKE" cap="BUTT" join="MITER" strokeWidth="40.0" />
        <feature type="gps_track" updateWidth="false" widthFactor="1.0" color="ff0000ff" style="STROKE" cap="ROUND" join="ROUND" strokeWidth="2.0" />
        <feature type="node_tolerance" updateWidth="false" widthFactor="1.0" color="28bd8d46" style="FILL" cap="BUTT" join="MITER" strokeWidth="40.0" />
        <feature type="node_tolerance_2" updateWidth="false" widthFactor="1.0" color="7fbd8d46" style="FILL" cap="BUTT" join="MITER" strokeWidth="40.0" />
        <feature type="infotext" updateWidth="false" widthFactor="1.0" color="ff000000" style="FILL" cap="BUTT" join="MITER" strokeWidth="0.0" typefacestyle="0" textsize="12.0" />
        <feature type="viewbox" updateWidth="false" widthFactor="1.0" color="7d0f0f0f" style="FILL" cap="BUTT" join="MITER" strokeWidth="0.0" />
        <feature type="gps_pos" updateWidth="false" widthFactor="2.0" color="ff0000ff" style="FILL" cap="ROUND" join="ROUND" strokeWidth="2.0" />
        <feature type="gps_pos_follow" updateWidth="false" widthFactor="2.0" color="ff0000ff" style="STROKE" cap="ROUND" join="ROUND" strokeWidth="2.0"/>
        <feature type="dontrender_way" dontrender="true" updateWidth="true" widthFactor="1.0" color="00ffffff" style="STROKE" cap="ROUND" join="ROUND" />
        <feature type="map_background" color="ff003153" />```

        <!-- OSM node based features currently internal features -->
        <feature type="node_untagged" updateWidth="true" widthFactor="1.0" color="ffb9121b" style="FILL" cap="ROUND" join="MITER" />
        <feature type="node_tagged" updateWidth="true" widthFactor="1.5" color="ffb9121b" style="FILL" cap="ROUND" join="MITER" />
        <feature type="node_thin" updateWidth="false" widthFactor="1.0" color="ffb9121b" style="STROKE" cap="BUTT" join="MITER" strokeWidth="1.0" typefacestyle="0" textsize="12.0" />
        <feature type="selected_node" updateWidth="true" widthFactor="1.5" color="fff6e497" style="FILL" cap="ROUND" join="MITER" />
        <feature type="selected_node_tagged" updateWidth="true" widthFactor="2.0" color="fff6e497" style="FILL" cap="ROUND" join="MITER" />
        <feature type="selected_node_thin" updateWidth="false" widthFactor="1.0" color="fff6e497" style="STROKE" cap="BUTT" join="MITER" strokeWidth="1.0" typefacestyle="0" textsize="12.0" />
        <feature type="problem_node" updateWidth="true" widthFactor="2.0" color="ffff00ff" style="STROKE" cap="ROUND" join="MITER" />
        <feature type="problem_node_tagged" updateWidth="true" widthFactor="1.5" color="ffff00ff" style="FILL" cap="BUTT" join="MITER" />
        
        <!-- Arrow styles -->
        <feature type="oneway_direction" updateWidth="true" widthFactor="0.5" color="ffb9121b" style="STROKE" cap="SQUARE" join="MITER" oneway="true" />
        <feature type="waterway_direction" updateWidth="true" widthFactor="0.5" color="ff0000bb" style="STROKE" cap="SQUARE" join="MITER" />
    
        <!-- OSM node based features -->
        <feature type="node" />
        <feature type="node" tags="name" labelKey="name" />
        <feature type="node" tags="entrance|addr:housenumber" labelKey="addr:housenumber" />
    
        <!-- OSM way based features -->
        <feature type="way" updateWidth="true" widthFactor="1.0" color="80000000" style="STROKE" cap="BUTT" join="MITER" />
        <feature type="way" tags="boundary" updateWidth="true" widthFactor="0.6" color="ff000000" style="STROKE" cap="BUTT" join="MITER">
            <dash phase="1.0">
                <interval length="6.0" />
                <interval length="2.0" />
            </dash>
        </feature>
        <feature type="way" tags="route" updateWidth="true" widthFactor="0.6" color="880000AA" style="STROKE" cap="BUTT" join="MITER">
            <dash phase="1.0">
                <interval length="3.0" />
                <interval length="3.0" />
            </dash>
        </feature>
        <feature type="way" tags="landuse" area="true" updateWidth="true" widthFactor="2.0" color="8871BE80" style="STROKE" cap="BUTT" join="MITER" pathPattern="border_right">
            <feature type="way" tags="landuse=construction" color="88996633" style="FILL_AND_STROKE" pathPattern="" />
            <feature type="way" tags="landuse=residential" minVisibleZoom="10" color="88888888" />
            <feature type="way" tags="landuse=quarry" color="88d8a2a2" />
            <feature type="way" tags="landuse=military" color="88af2626" />
            <feature type="way" tags="landuse=forest" minVisibleZoom="10" color="8800BE00" />
            <feature type="way" tags="landuse=industrial" minVisibleZoom="10" color="88A3709A" />
            <feature type="way" tags="landuse=commercial" minVisibleZoom="10" color="88A3709A" />
            <feature type="way" tags="landuse=railway" minVisibleZoom="10" color="88A3709A" />
        </feature>
        
        <feature type="way" tags="leisure=pitch" updateWidth="true" widthFactor="1.0" color="8839AC39" style="FILL_AND_STROKE" cap="BUTT" join="MITER" />
        <feature type="way" tags="leisure=swimming_pool" updateWidth="true" widthFactor="1.0" color="880000ff" style="FILL_AND_STROKE" cap="BUTT" join="MITER" />
        <feature type="way" tags="natural" updateWidth="true" widthFactor="0.5" color="ff71BE80" style="STROKE" cap="BUTT" join="MITER"> 
            <feature type="way" tags="natural" closed="true" area="true" pathPattern="border_right" >
                <feature type="way" tags="natural=water" widthFactor="1.0" minVisibleZoom="10" color="ff0000ff" />
            </feature>
            <feature type="way" tags="natural=water" widthFactor="1.0" minVisibleZoom="10" color="ff0000ff" />
            <feature type="way" tags="natural=cliff" widthFactor="2.0" color="ff555555" pathPattern="triangle_right" />
            <feature type="way" tags="natural=coastline" widthFactor="2.0" minVisibleZoom="10" color="ff71BE80" pathPattern="triangle_left" />
            <feature type="way" tags="natural=tree_row" widthFactor="2.0" color="ff4b7a54" cap="ROUND" />
        </feature>
        <feature type="way" tags="waterway" updateWidth="true" widthFactor="1.0" color="ff0000ff" style="STROKE" cap="BUTT" join="MITER" arrowStyle="waterway_direction" >
            <feature type="way" tags="waterway=riverbank" updateWidth="true" widthFactor="1.0" color="ff0000ff" style="STROKE" cap="BUTT" join="MITER" arrowStyle="" />
            <feature type="way" tags="waterway=weir" widthFactor="2.0" color="ff0000ff" pathPattern="triangle_right" />
        </feature>
        <feature type="way" tags="man_made" color="ff585c63" style="STROKE" cap="BUTT" join="MITER" >
            <feature type="way" tags="man_made=embankment" color="ff996633" pathPattern="triangle_right" />
        </feature>
        <feature type="way" tags="building" updateWidth="true" widthFactor="1.0" color="ffcc9999" style="STROKE" cap="BUTT" join="MITER" />
        <feature type="way" tags="building:part" updateWidth="true" widthFactor="0.8" color="ffcc9999" style="STROKE" cap="BUTT" join="MITER" >
            <dash phase="1.0">
                <interval length="2.0" />
                <interval length="2.0" />
            </dash>
        </feature>
        <feature type="motorway_casing" updateWidth="true" widthFactor="2.4" color="FFFFFFFF" style="STROKE" cap="BUTT" join="MITER" />
        <feature type="motorway_bridge_casing" updateWidth="true" widthFactor="2.4" color="FFFFFFFF" style="STROKE" cap="BUTT" join="MITER">
            <dash phase="1.0">
                <interval length="2.0" />
                <interval length="2.0" />
            </dash>
        </feature>
        <feature type="primary_casing" updateWidth="true" widthFactor="1.8" color="FFFFFFFF" style="STROKE" cap="BUTT" join="MITER" />
        <feature type="primary_bridge_casing" updateWidth="true" widthFactor="1.8" color="FFFFFFFF" style="STROKE" cap="BUTT" join="MITER">
            <dash phase="1.0">
                <interval length="2.0" />
                <interval length="2.0" />
            </dash>
        </feature>
        <feature type="secondary_casing" updateWidth="true" widthFactor="1.5" color="FFFFFFFF" style="STROKE" cap="BUTT" join="MITER" />
        <feature type="secondary_bridge_casing" updateWidth="true" widthFactor="1.5" color="FFFFFFFF" style="STROKE" cap="BUTT" join="MITER">
            <dash phase="1.0">
                <interval length="2.0" />
                <interval length="2.0" />
            </dash>
        </feature>
        <feature type="teritary_casing" updateWidth="true" widthFactor="1.3" color="FFFFFFFF" style="STROKE" cap="BUTT" join="MITER" />
        <feature type="tertiary_bridge_casing" updateWidth="true" widthFactor="1.3" color="FFFFFFFF" style="STROKE" cap="BUTT" join="MITER">
            <dash phase="1.0">
                <interval length="2.0" />
                <interval length="2.0" />
            </dash>
        </feature>
        <feature type="unclassified_casing" updateWidth="true" widthFactor="1.2" color="FFFFFFFF" style="STROKE" cap="BUTT" join="MITER" />
        <feature type="unclassified_bridge_casing" updateWidth="true" widthFactor="1.2" color="FFFFFFFF" style="STROKE" cap="BUTT" join="MITER">
            <dash phase="1.0">
                <interval length="2.0" />
                <interval length="2.0" />
            </dash>
        </feature>
        <feature type="residential_casing" updateWidth="true" widthFactor="1.1" color="FFFFFFFF" style="STROKE" cap="BUTT" join="MITER" />
        <feature type="residential_bridge_casing" updateWidth="true" widthFactor="1.1" color="FFFFFFFF" style="STROKE" cap="BUTT" join="MITER">
            <dash phase="1.0">
                <interval length="2.0" />
                <interval length="2.0" />
            </dash>
        </feature>
        <feature type="way" tags="highway" updateWidth="true" widthFactor="1.0" color="88888888" style="STROKE" cap="BUTT" join="MITER" arrowStyle="oneway_direction">
            <feature type="way" tags="highway=construction" widthFactor="1.0" color="88888888" />
            <feature type="way" tags="highway=proposed" widthFactor="1.0" color="88888888" />
            <feature type="way" tags="highway=pedestrian" widthFactor="0.8" color="ff888888" />
            <feature type="way" tags="highway=path" widthFactor="0.6" color="ffc69c49" />
            <feature type="way" tags="highway=cycleway" widthFactor="0.6" color="fff48f42" />
            <feature type="way" tags="highway=footway" widthFactor="0.6" color="ffff4500" />
            <feature type="way" tags="highway=steps"  widthFactor="0.6" color="ffff4500" >
                <dash phase="1.0">
                   <interval length="1.0" />
                   <interval length="1.0" />
                </dash>
            </feature>
            <feature type="way" tags="highway=track" widthFactor="1.0" color="ffc69c49" />
            <feature type="way" tags="highway=motorway" widthFactor="2.0" minVisibleZoom="10" color="ff809BC0" casingStyle="motorway_casing" >
                <feature type="way" tags="bridge=yes" casingStyle="motorway_bridge_casing" />
                <feature type="way" tags="tunnel=yes" casingStyle="">
                    <dash phase="1.0">
                        <interval length="1.0" />
                        <interval length="1.0" />
                    </dash>
                </feature>
            </feature>
            <feature type="way" tags="highway=motorway_link" widthFactor="2.0" minVisibleZoom="10" color="ff809BC0" >
                <feature type="way" tags="tunnel=yes" casingStyle="">
                    <dash phase="1.0">
                        <interval length="1.0" />
                        <interval length="1.0" />
                    </dash>
                </feature>
            </feature>
            <feature type="way" tags="highway=trunk" widthFactor="1.5" minVisibleZoom="10" color="ff7FC97F" casingStyle="trunk_casing" >
                <feature type="way" tags="bridge=yes" casingStyle="trunk_bridge_casing" />
                <feature type="way" tags="tunnel=yes" casingStyle="">
                    <dash phase="1.0">
                        <interval length="1.0" />
                        <interval length="1.0" />
                    </dash>
                </feature>
            </feature>
            <feature type="way" tags="highway=trunk_link" widthFactor="1.5" minVisibleZoom="10" color="ff7FC97F" >
                <feature type="way" tags="tunnel=yes" casingStyle="">
                    <dash phase="1.0">
                        <interval length="1.0" />
                        <interval length="1.0" />
                    </dash>
                </feature>
            </feature>
            <feature type="way" tags="highway=primary" widthFactor="1.5" color="ffE46D71" casingStyle="primary_casing" >
                <feature type="way" tags="bridge=yes" casingStyle="primary_bridge_casing" />
                <feature type="way" tags="tunnel=yes" casingStyle="">
                    <dash phase="1.0">
                        <interval length="1.0" />
                        <interval length="1.0" />
                    </dash>
                </feature>
            </feature>
            <feature type="way" tags="highway=primary_link" widthFactor="1.5" color="ffE46D71" >
                <feature type="way" tags="tunnel=yes" casingStyle="">
                    <dash phase="1.0">
                        <interval length="1.0" />
                        <interval length="1.0" />
                    </dash>
                </feature>
            </feature>
            <feature type="way" tags="highway=secondary" widthFactor="1.2" color="ffFDBF6F" casingStyle="secondary_casing" >
                <feature type="way" tags="bridge=yes" casingStyle="secondary_bridge_casing" />
                <feature type="way" tags="tunnel=yes" casingStyle="">
                    <dash phase="1.0">
                        <interval length="1.0" />
                        <interval length="1.0" />
                    </dash>
                </feature>
            </feature>
            <feature type="way" tags="highway=secondary_link" widthFactor="1.2" color="ffFDBF6F" >
                <feature type="way" tags="tunnel=yes" casingStyle="">
                    <dash phase="1.0">
                        <interval length="1.0" />
                        <interval length="1.0" />
                    </dash>
                </feature>
            </feature>
            <feature type="way" tags="highway=tertiary" widthFactor="1.0" color="ffFCFA74" casingStyle="tertiary_casing" >
                <feature type="way" tags="bridge=yes" casingStyle="tertiary_bridge_casing" />
                <feature type="way" tags="tunnel=yes" casingStyle="">
                    <dash phase="1.0">
                        <interval length="1.0" />
                        <interval length="1.0" />
                    </dash>
                </feature>
            </feature>
            <feature type="way" tags="highway=tertiary_link" widthFactor="1.0" color="ffFCFA74" >
                <feature type="way" tags="tunnel=yes" casingStyle="">
                    <dash phase="1.0">
                        <interval length="1.0" />
                        <interval length="1.0" />
                    </dash>
                </feature>
            </feature>
            <feature type="way" tags="highway=unclassified" widthFactor="1.0" color="ffCCCCCC" casingStyle="unclassified_casing" >
                <feature type="way" tags="bridge=yes" casingStyle="unclassifies_bridge_casing" />
                <feature type="way" tags="tunnel=yes" casingStyle="">
                    <dash phase="1.0">
                        <interval length="1.0" />
                        <interval length="1.0" />
                    </dash>
                </feature>
            </feature>
            <feature type="way" tags="highway=residential" widthFactor="0.9" color="ffCCCCCC" casingStyle="residential_casing" >
                <feature type="way" tags="bridge=yes" casingStyle="residential_bridge_casing" />
            </feature>
            <feature type="way" tags="highway=living_street" widthFactor="0.9" color="ffff4500" />
            <feature type="way" tags="highway=service" widthFactor="0.7" color="ffCCCCCC" />
        </feature>
        <feature type="way" tags="power" updateWidth="true" widthFactor="0.6" color="ffaa0000" style="STROKE" cap="BUTT" join="MITER">
            <dash phase="1.0">
                <interval length="2.0" />
                <interval length="2.0" />
            </dash>
        </feature>
        <feature type="way" tags="railway" updateWidth="true" widthFactor="0.7" color="ff999999" style="STROKE" cap="BUTT" join="MITER">
            <dash phase="1.0">
                <interval length="4.0" />
                <interval length="4.0" />
            </dash>
        </feature>
        <feature type="way" tags="addr:interpolation" updateWidth="true" widthFactor="0.6" color="ff000000" style="STROKE" cap="BUTT" join="MITER">
            <dash phase="1.0">
                <interval length="2.0" />
                <interval length="2.0" />
            </dash>
        </feature>
        <feature type="way" tags="amenity" updateWidth="true" widthFactor="0.8" color="ffCCCC00" style="STROKE" cap="BUTT" join="MITER">
            <dash phase="1.0">
                <interval length="2.0" />
                <interval length="2.0" />
            </dash>
            <feature type="way" tags="amenity=parking" color="ff99CCFF"  />
            <feature type="way" tags="amenity=bicycle_parking"  color="ff99CCFF" />
            <feature type="way" tags="amenity=motorcycle_parking" color="ff99CCFF" />
        </feature>
        
        <feature type="way" tags="leisure=playground" area="true" updateWidth="true" widthFactor="0.8" color="88f4a460" style="STROKE" cap="BUTT" join="MITER" pathPattern="border_right" />
        
        <!-- Indoor features -->
        <feature type="way" tags="indoor" updateWidth="true" widthFactor="0.7" color="99f4a442" style="STROKE" cap="BUTT" join="MITER" >
            <feature type="way" tags="indoor=level" updateWidth="true" widthFactor="0.7" color="99f2c791" style="STROKE" cap="BUTT" join="MITER" />
            <feature type="way" tags="indoor=room" updateWidth="true" widthFactor="0.7" color="99f2c791" style="FILL_AND_STROKE" cap="BUTT" join="MITER" />
            <feature type="way" tags="indoor=corridor" updateWidth="true" widthFactor="0.7" color="99f2c791" style="FILL_AND_STROKE" cap="BUTT" join="MITER" />
        </feature>
        
        <!-- relations (only multipolygons currently -->
        <feature type="relation" updateWidth="true" widthFactor="1.0" color="ff222222" style="STROKE" cap="BUTT" join="MITER" />
        <feature type="relation" tags="type=multipolygon" updateWidth="true" widthFactor="0.8" color="88222222" style="STROKE" cap="BUTT" join="MITER" pathPattern="border_right">
            <feature type="relation" tags="building" widthFactor="1.0" color="ffcc9999"/>
            <feature type="relation" tags="leisure=playground" widthFactor="0.8" color="88f4a460" style="FILL_AND_STROKE" />
            <feature type="relation" tags="natural" color="ff71BE80" >
                <feature type="relation" tags="natural=water" widthFactor="1.0" minVisibleZoom="10" color="ff0000ff" />
                <feature type="relation" tags="natural=cliff" widthFactor="2.0" color="ff555555" pathPattern="triangle_right" />
                <feature type="relation" tags="natural=coastline" widthFactor="2.0" minVisibleZoom="10" color="ff71BE80" pathPattern="triangle_left" />
                <feature type="relation" tags="natural=tree_row" widthFactor="2.0" color="ff4b7a54" cap="ROUND" />
            </feature>
            <feature type="relation" tags="landuse" updateWidth="true" widthFactor="2.0" color="8871BE80" style="STROKE" cap="BUTT" join="BEVEL" pathPattern="border_right" >
                <feature type="relation" tags="landuse=construction" color="88996633" />
                <feature type="relation" tags="landuse=residential" minVisibleZoom="10" color="88888888" />
                <feature type="relation" tags="landuse=quarry" color="88d8a2a2" />
                <feature type="relation" tags="landuse=military" color="88af2626" />
                <feature type="relation" tags="landuse=forest" minVisibleZoom="10" color="8800BE00" />
                <feature type="relation" tags="landuse=industrial" minVisibleZoom="10" color="88A3709A" />
                <feature type="relation" tags="landuse=commercial" minVisibleZoom="10" color="88A3709A" />
                <feature type="relation" tags="landuse=railway" minVisibleZoom="10" color="88A3709A" />
            </feature>
        </feature>
    </profile>
