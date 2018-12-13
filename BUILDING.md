
# Building Vespucci 0.9.9 and later

The code has been re-factored to use the compatibility libraries from Google instead of the ActionBarSherlock library.

Given that Google randomly makes incompatible changes to these and regularly drops existing functionality, you are on your own if you use any other version than what is configured for gradle.

## Build flavors (11.2 and later)

Due to the forced upgrade policy from google from November 1st 2018 onwards we are now supporting two build flavors: _current_ that will target a recent Android SDK and support library and _legacy_ that will support old Android versions as long as practical.

See [https://commonsware.com/blog/2018/01/08/android-version-ratchet.html](https://commonsware.com/blog/2018/01/08/android-version-ratchet.html) for some more information on this.

Currently the split is as follows:

  _current_ Android 14 / 4.0 "Ice Cream Sandwich" and later - support library version 27
  _legacy_ Android 9 / 2.3 "Gingerbread" and later - support library version 25

## Building with gradle (Eclipse or command line)

This will work with Eclipse Neon with buildship or from the command line:

To install the application to your device run the following task on Unix:

```bash
$ ./gradlew installCurrentDebug
```

or this command on Windows:

```bash
$ gradlew.bat installCurrentDebug
```

Use _installLegacyDebug_ for the legacy flavor.

If you are using Eclipse you need to import the project as a gradle project, and then generate the .classpath file initially and on any configuration change or else the IDE is essentially useless. Run the gradle "eclipse" task to do this. It should be noted that this is a constant fight against changes in googles build tools and involves rather hackish workarounds

## Building with gradle and Android Studio

The build configuration has been updated for AppCompat and should work out of the box.

## Building with Eclipse

This is likely no longer possible since we have changed the directory layout to be in line with the standard Android Studio and gradle layout with 0.9.9, but you can naturally give it a try.

You can find the required Android support libs in your SDK folder in:

extras/android/support/v7

and

extras/android/support/design

You need to import _appcompat_, _preference_, _recyclerview_ and the _design_ libraries into Eclipse as library projects, since the build requires access to the resource files in the libraries.

You will further need the libraries listed in build.gradle for a successful build.

## JOSM Presets

https://github.com/simonpoole/beautified-JOSM-preset

The gradle task ``updatePreset`` will update the icons and preset.xml file in the asset directory from the github repo.

## 3rd Party Configuration Files

Name suggestion index https://github.com/osmlab/name-suggestion-index

Imagery index https://github.com/simonpoole/osm-layer-index (which is a fork of https://github.com/osmlab/editor-imagery-index).

The gradle task ``updateImagery`` will update the imagery.json file in the asset directory from the github repo.

Tags to be discarded https://github.com/openstreetmap/iD/blob/master/data/discarded.json

## Testing

Automated testing has come relatively late to Vespucci, however we have made large advances in improving the coverage in 2017. Note: the on-device tests will typically fail the first time if Vespucci was already installed on the device (due to previous state being loaded). Either de-install or simply run the tests twice.

Tests need to be run with the emulator set to English and with the "high precision" (aka GPS and network) location option set, currently the only OS version all tests run on successfully is 8.0 and probably later.

On an Intel based emulator the tests currently take something around 25 minutes to complete.
