# Vespucci Preset System
_Documentation for Vespucci 20.2_

As explained in the [help documentation](../help/en/Presets.md) Vespucci uses JOSM compatible presets, currently any preset used in JOSM should simply work with Vespucci, however there can be differences. Particularly with the new preset driven tagging interface presets have become even more important and if you are writing presets yourself and want them to work well in Vespucci please keep on reading.

## Preset-Driven Tag-Editing in Vespucci

![ ](images/form_editor_small.png)

For the preset based editing to work Vespucci has to match the existing tags, including incomplete, key-only ones with the available presets. This is done in an iterative fashion: the best matching preset is found (weighting presets with fixed key-value tuples higher than such with variable values), then all tags that can be found in linked presets are added (no separate header displayed) and this process is repeated until their are either no tags left or no preset match can be found.

## Supported JOSM Preset Elements and Attributes

Note: this is loosely based on what [JOSM claims](https://josm.openstreetmap.de/wiki/TaggingPresets) that works, this may be, and likely is, different from the actual implementation. Language specific attributes are ignored (see [Translation](#Translation)). "supported" doesn't necessarily imply the same behaviour as JOSM, simply that Vespucci will do something useful with the value. Icons currently can be in PNG or SVG format.


|Element            | Attributes                     | Support   | Notes
|-------------------|-------------------------------|-----------|----------------------------------------------------------------
|__&lt;presets&gt;__          |                     |           |
|                             | author              | ignored   |
|                             | version             | supported |
|                             | description         | partial   |
|                             | shortdescription    | supported |
|                             | link                | ignored   |
|                             | icon                | ignored   |
|                             | baselanguage        | ignored   |
|                             | object_keys         | extension | comma separated list of top-level keys
|__&lt;!-- comment --&gt;__   |                     | ignored   |
|__&lt;group&gt;__  |                               | supported |
|                   | name                          | supported | required
|                   | name_context                  | supported |
|                   | icon                          | supported | you really should add one for Vespucci
|                   | items_sort                    | extension | sort the items alphabetically, default "yes"
|                   | regions                       | extension | comma separated list of countries this preset group is applicable for
|                   | exclude_regions               | extension | if true, invert the meaning of regions
|__&lt;item&gt;__   |                               | supported |
|                   | name                          | supported | required
|                   | name_context                  | supported |
|                   | icon                          | supported | you really should add one for Vespucci
|                   | type                          | supported | extension: "area" is supported as a replacement for "multipolygon" and matches closed ways with an area=yes tag besides multipolygons
|                   | name_template                 | supported | if set will always be used 
|                   | name_template_filter          | ignored   |
|                   | preset_name_label             | ignored   |
|                   | match_expression              | supported |
|                   | deprecated                    | extension | only use the preset for matching and map icon display
|                   | regions                       | extension | comma separated list of countries this preset item is applicable for
|                   | exclude_regions               | extension | if true, invert the meaning of regions
|                   | autoapply                     | extension | if true (default), allow auto-applying of the item
|                   | min_match                     | extension | an integer indicating the minimum number of fixed tags that need to match (default is all fixed tags)
|__&lt;chunk&gt;__  |                               | supported | chunks can either contain complete fields just as items can, or a sequence of list_entry elements
|                   | id                            | supported | required
|__&lt;reference&gt;__ |                            | supported |
|                   | ref                           | supported | required
|__&lt;key&gt;__    |                               | supported |
|                   | value                         | supported | required
|                   | match                         | partial   | "none" is supported
|                   | match_expression              | extension |
|                   | text                          | extension | Something to display
|                   | values_context                | extension | Translation context
|                   | object                        | extension | If present and true or false the tag will be considered as defining/not-defining a top level object, overriding the normal rules
|                   | regions                       | extension | comma separated list of countries this preset group is applicable for
|                   | exclude_regions               | extension | if true, invert the meaning of regions 
|__&lt;text&gt;__   |                               | supported |
|                   | key                           | supported | required
|                   | text                          | supported |
|                   | match                         | partial   | only the "key", "key!" and "none" values are supported, all other values are ignored
|                   | match_expression              | extension |
|                   | default                       | supported |
|                   | use_last_as_default           | partial   | "force" has the same effect as "true"
|                   | auto_increment                | ignored   |
|                   | length                        | partial   | depending on the settings a modal will be displayed for editing instead of an inline text field
|                   | alternative_autocomplete_keys | ignored   |
|                   | value_template                | ignored   |
|                   | javascript                    | extension | if value is not set, execute the JS script
|                   | i18n                          | extension | if set to true this tag has i18n variants
|                   | value_type                    | extension | indicate the kind of value this tag should have
|                   | regions                       | supported | comma separated list of countries this preset group is applicable for
|                   | exclude_regions               | supported | if true, invert the meaning of regions
|__&lt;combo&gt;__  |                               | supported | combo elements can contain a sequence of list_entry elements, or references to chunks that contain the same
|                   | key                           | supported | required
|                   | text                          | supported |
|                   | text_context                  | supported |
|                   | values                        | supported |
|                   | values_sort                   | supported |
|                   | delimiter                     | supported |
|                   | default                       | supported |
|                   | match                         | supported |
|                   | match_expression              | extension |
|                   | display_values                | supported |
|                   | short_descriptions            | partial   | will only be used if display_values is not present
|                   | values_context                | supported |
|                   | editable                      | supported | default is "false"
|                   | use_last_as_default           | partial   | "force" has the same effect as "true"
|                   | values_searchable             | supported | subtype values are added regardless of the setting
|                   | length                        | ignored   |
|                   | values_no_i18n                | ignored   |
|                   | values_from                   | supported | extension: if the target method supports a String argument the current key will be passed
|                   | regions                       | supported | comma separated list of countries this preset group is applicable for
|                   | exclude_regions               | supported | if true, invert the meaning of regions
|__&lt;multiselect&gt;__ |                          | supported | multiselect elements can contain a sequence of list_entry elements, or references to chunks that contain the same
|                   | key                           | supported | required
|                   | text                          | supported |
|                   | text_context                  | supported |
|                   | values                        | supported |
|                   | values_sort                   | supported |
|                   | delimiter                     | supported |
|                   | default                       | supported |
|                   | match                         | supported |
|                   | match_expression              | extension |
|                   | display_values                | supported |
|                   | short_descriptions            | partial   | will only be used if display_values is not present
|                   | values_context                | supported |
|                   | editable                      | supported | default is "false"
|                   | use_last_as_default           | partial   | "force" has the same effect as "true"
|                   | values_searchable             | supported | subtype values are added regardless of the setting
|                   | length                        | ignored   |
|                   | values_no_i18n                | ignored   |
|                   | values_from                   | supported | extension: if the target method supports a String argument the current key will be passed
|                   | rows                          | ignored   |
|                   | value_count_key               | extension | reference to a tag that will hold the number of values this multiselect should contain
|                   | regions                       | supported | comma separated list of countries this preset group is applicable for
|                   | exclude_regions               | supported | if true, invert the meaning of regions
|__&lt;list_entry&gt;__       |                     | supported |
|                   | value                         | supported | required
|                   | display_value                 | supported |
|                   | short_description             | supported |
|                   | icon                          | supported |
|                   | icon_size                     | ignored   |
|                   | image                         | extension |
|                   | regions                       | supported | comma separated list of countries this preset group is applicable for
|                   | exclude_regions               | supported | if true, invert the meaning of regions
|__&lt;checkgroup&gt;__       |                     | supported |
|                   | columns                       | ignored   |
|                   | text                          | extension |
|                   | text_context                  | extension |
|                   | regions                       | supported | comma separated list of countries this preset group is applicable for
|                   | exclude_regions               | supported | if true, invert the meaning of regions
|__&lt;check&gt;__  |                               | supported |
|                   | key                           | supported | required
|                   | text                          | supported |
|                   | text_context                  | supported |
|                   | value_on                      | supported |
|                   | value_off                     | supported |
|                   | disable_off                   | supported |
|                   | default                       | supported |
|                   | match                         | supported |
|                   | match_expression              | extension |
|                   | use_last_as_default           | extension | "force" has the same effect as "true"
|                   | regions                       | supported | comma separated list of countries this preset group is applicable for
|                   | exclude_regions               | supported | if true, invert the meaning of regions
|__&lt;label&gt;__  |                               | supported |
|                   | regions                       | extension | comma separated list of countries this preset group is applicable for
|                   | exclude_regions               | extension | if true, invert the meaning of regions
|__&lt;space/&gt;__ |                               | supported | suppressed immediately after a header or after another space element
|                   | regions                       | extension | comma separated list of countries this preset group is applicable for
|                   | exclude_regions               | extension | if true, invert the meaning of regions
|__&lt;optional&gt;__ |                             | supported | contained fields are not displayed the in "Properties" tab when a preset is applied except if applying with optional fields is used, further matching ignores any optional fields
|                   | text                          | supported |
|__&lt;separator/&gt;__ |                           | supported | starts a new row in the preset selection display
|__&lt;item_separator/&gt;__ |                      | supported |
|                   | regions                       | extension | comma separated list of countries this preset group is applicable for
|                   | exclude_regions               | extension | if true, invert the meaning of regions
|__&lt;link&gt;__   |                               | supported | not legal inside &lt;chunk&gt; elements
|                   | href                          | supported | including language specific variants
|                   | wiki                          | supported | this will be used with lower preference than href entries
|__&lt;roles&gt;__  |                               | ignored   | but not the included &lt;role&gt; elements
|__&lt;role&gt;__   |                               | supported |
|                   | key                           | supported | required
|                   | text                          | supported |
|                   | text_context                  | supported |
|                   | requisite                     | ignored   |
|                   | count                         | ignored   |
|                   | type                          | supported |
|                   | member_expression             | supported | See [search language documentation](object_search.md)
|                   | regexp                        | ignored   |
|                   | regions                       | extension | comma separated list of countries this preset group is applicable for
|                   | exclude_regions               | extension | if true, invert the meaning of regions
|__&lt;preset_link&gt;__ |                          | supported | Used for tag matching and added to preset MRU list for easy access
|                        | text                     | ignored   |
|                        | text_context             | ignored   |
|                        | alternative              | extension | If "true", the linked to preset is an alternative tagging for the object in question

## Name templates

The Vespucci name templates differ slightly from the JOSM implementation, in particular nested quotes need to be escaped, however any template that works with Vespucci should
work with JOSM.

## Translation <a name="Translation"></a>

While the preset specification includes language specific text by prefixing the keys with the two letter ISO code, for example _de.text_, in practical terms this doesn't really work. Particularly if lots of languages are in use, the preset file itself would explode in size, become essentially illegible and force the application to parse many 100's if not 1000's of attributes for languages it is not interested in.

For Vespucci I've chosen a different approach based on [GNU gettext](https://www.gnu.org/software/gettext/manual/gettext.html) format files, the simple [Preset2Pot](https://github.com/simonpoole/preset-utils) utility will generate a .pot file from a preset which you can use with one of the translation services to produce GNU gettext compatible translations of all the strings in the preset (excluding the actual key and value strings).

## Extensions

* _deprecated_ only use this preset for matching, do not offer it for new use and do not find it when searched, it will work analogous for deprecated tags and values.
* _javascript_ execute the script if the value of this tag is empty, the script has access to and can modify the current tags and use the specified default value if any.
* _i18n_ the tag can have i18n variants, for example name and name:de.
* _object\_keys_ comma separated list of top level keys, examples would be highway, amenity etc., use this if your preset is introducing new such keys, see the [Christmas preset](https://github.com/simonpoole/xmas-preset) for an example.
* _object_ on fixed value tags. If present and true or false the tag will be considered as defining/not-defining a top level object, overriding the normal rules (see above).
* _value\_type_ type of the value. This avoids having to hardcode such properties in applications, see the [Christmas preset](https://github.com/simonpoole/xmas-preset) for an example.

    * _opening_hours_ a normal opening hours syntax tag
    * _opening_hours_mixed_ a tag with both text values and opening hours, examples: _fee_, _supervised_ and _lit_
    * ~~_conditional_ conditional access tag~~
    * _integer_ a integer numeric value
    * _website_ an URL for a website
    * _phone_ a phone number
    * _wikipedia_ a wikipedia page
    * _wikidata_  a wikidata Q ref
    * _speed_ a speed value (reserved)

* _items\_sort_ this controls sorting of items in groups, the default is "yes", any other value will disable sorting, the sorting preserves structure. Note: the preset root groups are currently not sortable.
* _regions_ comma separated list of countries this preset group or item is applicable for. Note that from JOSM 18918 on the only difference is that we support the region related tags on combo, multiselect (and not just the values), and the formatting tags.
* _exclude_regions_ if true, invert the meaning of regions. See above for details.
* _autoapply_ if true (default), allow auto-applying of the item.
* _min_match_ modifies Vespuccis matching algorithm to use the specified number of fixed tag matches instead of expecting all to match.
* _value_count_key_ reference to a tag that will hold the number of values this multiselect should contain. Example for the destination:lanes tag this would be value_count_key="lanes".
* _alternative_ indicates that the preset_link points to an alternative tagging of the object.
* _image_ optional reference to a large image in an _list_entry_. See [Bicycle parking preset](https://github.com/simonpoole/bicycle-parking-preset) for an example of using this tag.
* _match_expression_ on text, combo, multiselect and check elements. Note that having a field with the same key multiple times in an item leads to undefined behaviour. 
