
# Building Vespucci

We extensively use the androidx libraries. Given that Google randomly makes incompatible changes to these and regularly drops existing functionality, you are on your own if you use any other version than what is configured for gradle.

See [translations document](TRANSLATIONS.md) for information on how Vespucci is translated.

## Proguard

All builds now require proguard to be enabled as we have gone over the limit for the number of references in a normal APK.

## Build flavors

Due to the forced upgrade policy from google from November 1st 2018 onwards we are now supporting two build flavors: _current_ that will target a recent Android SDK and support library and _legacy_ that will support old Android versions as long as practical.

For version 15.0 the legacy flavour has been removed as androidx doesn't support versions older than Android 4 and as versions older than 4.1 do not support TLS 1.2 they would be largely non-functional in any case, however flavours may be reactivated if necessary.

See [https://commonsware.com/blog/2018/01/08/android-version-ratchet.html](https://commonsware.com/blog/2018/01/08/android-version-ratchet.html) for some more information on this.

Currently the split is as follows:

  _current_ Android 14 / 4.0 "Ice Cream Sandwich" and later - androidx version1.0.0
  _legacy_ currently not configured as there is no practical way to do so

## Building with gradle (Eclipse or command line)

This will work with Eclipse Neon and later with buildship or from the command line:

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

The build configuration should work out of the box.

## JOSM Presets

https://github.com/simonpoole/beautified-JOSM-preset

The gradle task ``updatePreset`` will update the icons and preset.xml file in the asset directory from the github repo.

## 3rd Party Configuration Files

Name suggestion index https://github.com/osmlab/name-suggestion-index

Imagery index https://github.com/simonpoole/osm-layer-index (which is a fork of https://github.com/osmlab/editor-imagery-index).

The gradle task ``updateImagery`` will update the imagery.json file in the asset directory from the JOSM wiki.

Tags to be discarded https://github.com/openstreetmap/iD/blob/master/data/discarded.json

Synonyms are retrieved from the iD repository with the grade task ``updateSynonyms``.

## Testing

Automated testing has come relatively late to Vespucci, however we have made large advances in improving the coverage from 2017 onwards. Note: the on-device tests will typically fail the first time if Vespucci was already installed on the device (due to previous state being loaded). Either de-install or simply run the tests twice.

Tests need to be run with the emulator locale set to English and with the "high precision" (aka GPS and network) location option set, currently the only OS versions all tests run on successfully are 8.0 and later. The current expectation is that all tests should pass, if this doesn't happen (for example because default applications and other android app settings have been changed) restarting the emulator should typically help.

On an Intel based emulator the tests currently take something around 45 minutes to complete if run with ``connectedCurrentDebugAndroidTest``.

The time to run the tests can be reduced substantially by running ``spoonCurrentDebugAndroidTest`` spoon will execute the tests sharded over as many emulators that are currently running on the system, the additional bonus is that the test output is much easier to consume and understand.

To make running individual tests simpler refreshing the gradle tasks (assuming there was a prior complete run of the tests) will create individual tasks for the tests, for the failed ones in the "failed tests" group, for successful ones in the "successful tests" group. 

Notes: 

* a number of the tests start with the splash screen activity and then wait for the main activity to be started. Experience shows that if one of these fails to complete in certain ways, the following tests that start via the splash screen will not be able to start the main activity. Reason unknown.
* as the complete set of tests takes a long time to run, if running the tests with spoon you can generate screenshots to debug things instead of trying to catch the test running, see LayerDialogTest.backgroundLayer() for an example of this.