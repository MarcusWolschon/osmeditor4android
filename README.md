[![Dependency Status](https://www.versioneye.com/user/projects/58b9f4f501b5b7003d62093f/badge.svg)](https://www.versioneye.com/user/projects/58b9f4f501b5b7003d62093f)

# Vespucci - An OpenStreetMap editor for Android

This is the first [OpenStreetMap][openstreetmap] editor for
[Android][android], codename "Vespucci".


![Amerigo Vespucci](http://vespucci.io/180px-Amerigo_Vespucci.jpg "Amerigo Vespucci")


## Contributing

If you're interested in this project, you're welcome to help improving it. We
need UI designers, translators, and of course Java programmers. Join now! Join
our [mailing list][mailinglist] or write to marcus@wolschon.biz.


## What is Vespucci?

* An offline (once you have downloaded data) editor for OpenStreetMap
* Runs on mobile devices using the Android platform
* Functionality:
    * Create and edit new Nodes and Ways
    * Move and rotate Ways
    * Append Nodes to existing Ways
    * Delete Nodes
    * Create, edit and delete Tags
    * Edit relations and create new turn restrictions
    * JOSM presets support
    * Download and upload to OSM server
    * Saving and reading of JOSM format OSM data files
    * Highlight unnamed highways, and ways/nodes with TODOs or FIXMEs
    * Add, comment and close OSM Notes
    * Use a variety of background tile layers as reference
    * Show the user's GPS track with accuracy
    * Upload to OSM and local saving of GPS tracks
    * Display the raw data
    * Display geo-referenced photographs


## What is Vespucci NOT?

* a pure map-view or a routing-application

## Getting started with contributing

Here is how you can start developing. 

Currently building is supported with eclipse, android studio and gradle, see [build instructions](BUILDING.md). 

Important note: if you are building your own version, particularly if you are making it available to third parties, please change app_version and app_name_version in res/values/appname.xml to something that makes it clear that this is not an "official" release and clearly identifies your builds. 

## License

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

