
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

## Building with gradle and Android Studio

The build configuration has been updated for AppCompat and should work out of the box.

Note we currently don't use a standard project layout

## Build with AppCompat and eclipse

This will likely no longer be possible post version 0.9.8 since we are will change the directory layout to be in line with standard Android Studio and gradle layout. 

You can find the required android support libs in your SDK folder in:

extras/android/support/v7

and

extras/android/support/design

You need to import _appcompat_, _preference_, _recyclerview_ and the _design_ libraries into eclipse as library projects, since the build requires access to the resource files in the libraries.

You will further need the the following libraries:

ACRA crash reporting http://www.acra.ch/

OAuth support library https://github.com/mttkay/signpost 


## Required 3rd Party Libraries

The following libraries are included in this repository in the libs directory that are currently not available from a maven compatible site 

EXIF metadata extractor library https://github.com/drewnoakes/metadata-extractor 

The parser for .PO files https://github.com/simonpoole/PoParser .

## JOSM Presets

Reworked version:
https://github.com/simonpoole/beautified-JOSM-preset

Original:
http://josm.openstreetmap.de/svn/trunk/data/defaultpresets.xml

## 3rd Party Configuration Files
Name suggestion index https://github.com/osmlab/name-suggestion-index

Imagery index https://github.com/osmlab/editor-imagery-index

Tags to be discarded https://github.com/openstreetmap/iD/blob/master/data/discarded.json

