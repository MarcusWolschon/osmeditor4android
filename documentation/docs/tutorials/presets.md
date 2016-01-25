## Vespucci Preset System
_Preliminary documentation for Vespucci 0.9.8_

As explained in the [help documentation](../help/en/Presets.md) Vespucci uses JOSM compatible presets, currently any preset used in JOSM should simply work with Vespucci however that doesn't mean that there are no differences. Particularly with the new preset driven tagging interface presets have become even more important and if you are writing presets yourself and want them to work well in Vespucci please keep on reading.

### Preset-Driven Tag-Editing in Vespucci

![](images/form_editor_small.png)

For the preset based editing to work Vespucci has to match the existing tags, including incomplete, key-only ones. with the available presets. This is done in an iterative fashion: the best matching preset is found (weighting presets with fixed key-value tuples higher than such with variable values), then all tags that can be found in linked presets are added (no separate header displayed) and this process is repeated until their are either no tags left or no preset match can be found.  

### Supported JOSM Preset Elements and Attributes

Note: this is loosely based on what [JOSM claims](https://josm.openstreetmap.de/wiki/TaggingPresets) to works, this may, and actually likely is, different from the actual implementation. Language specific attributes are ignored see [Translation](#Translation)


Element            | Attributes                     | Support   | Notes
-------------------|-------------------------------|-----------|----------------------------------------------------------------
__&lt;presets&gt;__          |                               | ignored   |
__&lt;!-- comment --&gt;__   |                               | ignored   |
__&lt;group&gt;__            |                               | supported |
                   | name                          | supported | required
                   | name_context                  | supported |
                   | icon                          | supported | you really should add one for Vespucci
__&lt;item&gt;__             |                               | supported |
                   | name                          | supported | required
                   | name_context                  | supported |
                   | icon                          | supported | you really should add one for Vespucci
                   | type                          | supported |
                   | name_template                 | ignored   |
                   | preset_name_label             | ignored   |
__&lt;chunk&gt;__            |                               | supported | 
                   | id                            | supported | required
__&lt;reference&gt;__        |                               | supported |
                   | ref                           | supported | required
__&lt;key&gt;__              |                               | supported |
                   | value                         | supported | required
                   | match                         | partial   | "key" value is supported, "none" will demote the tag from a "fixed" to a recommended key, all other values are ignored
__&lt;text&gt;__             |                               | supported |
                   | key                           | supported | required
                   | text                          | supported |
                   | match                         | partial   | only the "key" value is supported, all other values are ignored
                   | default                       | supported | 
                   | use_last_as_default           | ignored   | 
                   | auto_increment                | ignored   |
                   | length                        | ignored   |
                   | alternative_autocomplete_keys | ignored   |
__&lt;combo&gt;__            |                               | supported |
                   | key                           | supported | required
                   | text                          | supported |
                   | text_context                  | supported |
                   | values                        | supported |
                   | values_sort                   | supported |
                   | delimiter                     | supported |
                   | default                       | supported |
                   | match                         | partial   | only the "key" value is supported, all other values are ignored
                   | display_values                | supported |
                   | short_descriptions            | partial   | will only be used if display_values is not present
                   | values_context                | supported |
                   | editable                      | ignored   |
                   | use_last_as_default           | ignored   |
                   | values_searchable             | ignored   | all values are currently added to the index
                   | length                        | ignored   |
                   | values_no_i18n                | ignored   |
__&lt;multiselect&gt;__      |                               | supported |
                   | key                           | supported | required
                   | text                          | supported |
                   | text_context                  | supported |
                   | values                        | supported |
                   | values_sort                   | supported |
                   | delimiter                     | supported |
                   | default                       | supported |
                   | match                         | partial   | only the "key" value is supported, all other values are ignored
                   | display_values                | supported |
                   | short_descriptions            | partial   | will only be used if display_values is not present
                   | values_context                | supported |
                   | editable                      | ignored   |
                   | use_last_as_default           | ignored   |
                   | values_searchable             | ignored   | all values are currently added to the index
                   | length                        | ignored   |
                   | values_no_i18n                | ignored   |
                   | rows                          | ignored   |
__&lt;list_entry&gt;__       |                               | supported |   
                   | display_value                 | supported |
                   | short_description             | supported |
                   | icon                          | ignored   |
                   | icon_size                     | ignored   |
__&lt;checkgroup&gt;__       |                               | ignored   | but not the included <check> elements
__&lt;check&gt;__            |                               | supported |
                   | key                           | supported | required
                   | text                          | supported |
                   | text_context                  | supported |
                   | value_on                      | supported |
                   | value_off                     | supported | 
                   | disable_off                   | supported |
                   | default                       | supported |
                   | match                         | partial   | only the "key" value is supported, all other values are ignored
__&lt;label&gt;__            |                               | ignored   |
__&lt;space/&gt;__          |                               | ignored   |
__&lt;optional&gt;__         |                               | supported | doesn't display anything
                   | text                          | ignored   |
__&lt;separator/&gt;__       |                               | supported | starts a new row in the preset selection display
__&lt;item_separator/&gt;__  |                               | ignored   |
__&lt;link&gt;__             |                               | supported |
                   | href                          | partial   | language variants are currently not supported
__&lt;roles&gt;__            |                               | ignored   | but not the included <role> elements
__&lt;role&gt;__             |                               | supported |
                   | key                           | supported | required
                   | text                          | supported |
                   | text_context                  | supported | 
                   | requisite                     | ignored   |
                   | count                         | ignored   |
                   | type                          | ignored   |
                   | member_expression             | ignored   | 


                 
### Translation <a name="Translation"></a>

While the preset specification includes language specific text by prefixing the keys with the two letter ISO code, for example _de.text_, in practical terms this doesn't really work. Particularly if lots of languages are in use, the preset file itself would explode in size, become essentially illegible and force the application to parse many 100's if not 1000's of attributes for languages it is not interested in. 

For Vespucci I've chosen a different approach based on [GNU gettext](https://www.gnu.org/software/gettext/manual/gettext.html) format files, the simple [Preset2Pot](https://github.com/simonpoole/beautified-JOSM-preset/tree/master/src/ch/poole/presetutils) utility will generate a .pot file from a preset which you can use with one of the translation services to produce GNU gettext compatible translations of all the strings in the preset (excluding the actual key and value strings).
                 
