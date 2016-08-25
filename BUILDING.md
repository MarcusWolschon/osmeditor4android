
# Building vespucci 0.9.8 and later

The code has been re-factored to use the compatibility libraries from google instead of 
the ActionBarSherlock library. This has been tested with the google v7 support library 23.2.X, 
given that google randomly makes incompatible changes to these and regularly drops existing 
functionality, you are on your own if you use any other version. 


## Building with gradle (Eclipse or commandline)

This will work with Eclipse Neon with buildship or from the commandline: 

To install the application to your device run the following task on Unix:

```bash
$ ./gradlew installDebug
```

or this command on Windows:

```bash
$ gradlew.bat installDebug
```

If you are using eclipse you need to generate the .classpath file initially and on any configuration change or else the IDE is essentially useless, run the gradle "eclipse" task to do this.

## Building with gradle and Android Studio

The build configuration has been updated for AppCompat and should work out of the box.

Note we currently don't use a standard project layout

## Build with eclipse

This will likely no longer be possible post version 0.9.8 since we are will change the directory layout to be in line with standard Android Studio and gradle layout. 

You can find the required android support libs in your SDK folder in:

extras/android/support/v7

and

extras/android/support/design

You need to import _appcompat_, _preference_, _recyclerview_ and the _design_ libraries into eclipse as library projects, since the build requires access to the resource files in the libraries.

You will further need the the libraries listed in build.gradle for a successful build.

## JOSM Presets

Reworked version:
https://github.com/simonpoole/beautified-JOSM-preset

Original:
http://josm.openstreetmap.de/svn/trunk/data/defaultpresets.xml

The gradle task ``updatePreset`` will update the icons and preset.xml file in the asset directory from the github repo. 

## 3rd Party Configuration Files

Name suggestion index https://github.com/osmlab/name-suggestion-index

Imagery index https://github.com/simonpoole/osm-layer-index (which is a fork of https://github.com/osmlab/editor-imagery-index).

The gradle task ``updateImagery`` will update the imagery.json file in the asset directory from the github repo. 


Tags to be discarded https://github.com/openstreetmap/iD/blob/master/data/discarded.json

