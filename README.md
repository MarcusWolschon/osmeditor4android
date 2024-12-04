[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=osmeditor4android&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=osmeditor4android) [![Coverage](https://sonarcloud.io/api/project_badges/measure?project=osmeditor4android&metric=coverage)](https://sonarcloud.io/summary/new_code?id=osmeditor4android) [![sonarcloud bugs](https://sonarcloud.io/api/project_badges/measure?project=osmeditor4android&metric=bugs)](https://sonarcloud.io/component_measures?id=osmeditor4android&metric=bugs) [![sonarcould maintainability](https://sonarcloud.io/api/project_badges/measure?project=osmeditor4android&metric=sqale_rating)](https://sonarcloud.io/component_measures?id=osmeditor4android&metric=Maintainability) [![sonarcloud security](https://sonarcloud.io/api/project_badges/measure?project=osmeditor4android&metric=security_rating)](https://sonarcloud.io/component_measures?id=osmeditor4android&metric=Security) [![sonarcloud reliability](https://sonarcloud.io/api/project_badges/measure?project=osmeditor4android&metric=reliability_rating)](https://sonarcloud.io/component_measures?id=osmeditor4android&metric=Reliability)

# Vespucci - An OpenStreetMap editor for Android

This is the first [OpenStreetMap][openstreetmap] editor for
[Android][android], codename "Vespucci".


![Amerigo Vespucci](http://vespucci.io/180px-Amerigo_Vespucci.jpg "Amerigo Vespucci")

## Installing

[GooglePlay](https://play.google.com/store/apps/details?id=de.blau.android)

[Latest release from Github](https://github.com/MarcusWolschon/osmeditor4android/releases/latest)

## Contributing

If you're interested in this project, you're welcome to help improving it. We
need UI designers, [translators](TRANSLATIONS.md), and of course Java programmers. 
See the [build documentation](BUILDING.md) for more information.

## What is Vespucci?

* An on- and offline editor for OpenStreetMap
* Runs on mobile devices using the Android platform
* Functionality:
    * Create and edit new nodes and ways
    * Move and rotate ways
    * Append nodes to existing ways
    * Delete nodes
    * Create, edit and delete tags
    * Edit relations and create new turn restrictions and routes
    * JOSM presets support
    * Thematic mapping with no programming
    * Download and upload to OSM server
    * Offline data support
    * Saving and reading of JOSM format OSM data files
    * Highlight objects with missing tags like unnamed roads
    * Highlight ways/nodes with TODOs or FIXMEs
    * Highlight very old objects that are likely to be out dated
    * Add, comment and close OSM Notes
    * OSMOSE and Maproulette support
    * Use a variety of background tile layers as reference
    * Show the user's GPS track with accuracy
    * Upload to OSM and local saving of GPS tracks
    * Display the raw data
    * Display geo-referenced photographs and Mapillary images
    * GeoJSON layers with support for importing objects
    * ... and lots more ...
    
Detailed documentation can be found on the [Vespucci Website](https://vespucci.io/)

## What is Vespucci NOT?

* a pure map-view or a routing-application

## Getting started with contributing

Here is how you can start developing.

Currently building is supported with gradle in eclipse, android studio and naturally gradle on the command line, see [build instructions](BUILDING.md).

If you are interested in adding a feature or other development work, please reach out to us before embarking on something that might be already in work or a fools errand. 

Important note: if you are building your own version, particularly if you are making it available to third parties, please change app_version and app_name_version in res/values/appname.xml to something that makes it clear that this is not an "official" release and clearly identifies your builds.

## Related projects

Vespucci utilizes a number of independent, separately maintained, projects. The most relevant of these are

* [Editor Layer Index (ELI)](https://github.com/osmlab/editor-layer-index) background and overlay layers configuration
* [JOSM](https://josm.openstreetmap.de/wiki/Maps) alternative background and overlay layers configuration
* [Beautified JOSM Preset](https://github.com/simonpoole/beautified-JOSM-preset) independently maintained presets in JOSM format (can be utilized in JOSM too)
* [OpeningHoursFragment](https://github.com/simonpoole/OpeningHoursFragment) opening hours user interface
* [Name Suggestion Index](https://github.com/osmlab/name-suggestion-index) name/brand-related tag suggestions database
* [iD tagging schema](https://github.com/openstreetmap/id-tagging-schema) for synonyms used for searching presets
* [geocontext](https://github.com/simonpoole/geocontext) country/region specific speed limits and similar
* [osm-area-tags](https://github.com/simonpoole/osm-area-tags) OSM tags that imply area semantics

You can update both the imagery and preset configuration from within the app, the other configuration files are updated when necessary in the APK. 

## License and trademarks

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.


[openstreetmap]: http://www.openstreetmap.org
[android]: http://developer.android.com
[mailinglist]: http://groups.google.de/group/osmeditor4android
[josm]: http://wiki.openstreetmap.org/wiki/JOSM 

__OpenStreetMap__ and the magnifying glass logo are trademarks of the OpenStreetMap Foundation. The Vespucci app is not endorsed by or affiliated with the OpenStreetMap Foundation.

__Mapillary__ is a trademark of Mapillary AB, Sweden. The Vespucci app is not endorsed by or affiliated with Mapillary AB. 


