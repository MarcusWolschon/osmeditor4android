
# Building Vespucci

We extensively use the androidx libraries. Given that Google randomly makes incompatible changes to these and regularly drops existing functionality, you are on your own if you use any other version than what is configured for gradle.

See [translations document](TRANSLATIONS.md) for information on how Vespucci is translated.

## Java

Building with gradle requires a Java JDK to be installed.

- 19.1 and later requires Java 17
- 16.1 - 19.0 Java 11
- up to 16.0 Java 8

Note that if you are contributing to the code you should restrict yourself only supported Java 8 and Java 11 in Android, in particular the additional de-sugaring provided in later AGP versions doesn't work for Android prior to 5, see https://github.com/MarcusWolschon/osmeditor4android/pull/2131

## Proguard

All builds now require proguard to be enabled as we have gone over the limit for the number of references in a normal APK.

## Build flavors

Due to the forced upgrade policy from google from November 1st 2018 onwards we supported two build flavors: _current_ that will target a recent Android SDK and support library and _legacy_ that will support old Android versions as long as practical.

For version 15.0 the legacy flavour has been removed as androidx doesn't support versions older than Android 4 and as versions older than 4.1 do not support TLS 1.2 they would be largely non-functional in any case, however flavours may be reactivated if necessary.

See [https://commonsware.com/blog/2018/01/08/android-version-ratchet.html](https://commonsware.com/blog/2018/01/08/android-version-ratchet.html) for some more information on this.

Currently the split is as follows:

  _current_ Android 21 / 5.0 and later
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

__IMPORTANT__ : for testing with shrinking, optimization and obfuscation turned on build "fakeRelease" and test manually with it. Anything that uses reflection if particularly endangered to break when these are used, which is the case for release builds. 

Use _installLegacyDebug_ for the legacy flavor if the build has such a flavour. 

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

Automated testing has come relatively late to Vespucci, however we have made large advances in improving the coverage from 2017 onwards.

Unit test coverage is 27%, overall test coverage is currently 70%.

In general if you are writing new tests that do not involve the UI use unit tests and if you need to mock parts of Android use roboelectric. These tests can be completed in far less time than the on device checks, and the unit tests will also be run as part of the CI pipeline.

On device tests need to be run with the emulator locale set to English and with the "high precision" (aka GPS and network) location option set, currently the only OS versions all tests run on successfully are 8.0 and later. The current expectation is that all tests should pass, if this doesn't happen (for example because default applications and other android app settings have been changed) restarting the emulator should typically help. 

On an Intel based emulator the tests currently take something around 90 to 120 minutes to complete if run with the standard ``connectedCurrentDebugAndroidTest``.
To make running individual tests simpler refreshing the gradle tasks (assuming there was a prior complete run of the tests with ``connectedCurrentDebugAndroidTest``) will create individual tasks for the tests, for the failed ones in the "failed tests" group, for successful ones in the "successful tests" group.

For the on device tests the time to run the tests can be reduced substantially by running against multiple emulators with ``marathonCurrentDebugAndroidTest`` marathon can execute the tests sharded according to the configuration and retry failed tests. An additional bonus is that the test output is much easier to consume and understand. marathon will include a video of failed tests (we migrated from ``spoon`` to `` marathon`` for 16.1). For more information see [https://marathonlabs.github.io/marathon/](https://marathonlabs.github.io/marathon/). On a 20 core/174GB machine running 20 emulators this reduces the run time to between 15 and 20 minutes.

Notes: 

* a number of the tests start with the splash screen activity and then wait for the main activity to be started. Experience shows that if one of these fails to complete in certain ways, the following tests that start via the splash screen will not be able to start the main activity. Reason unknown.
* the CameraTest assumes that the emulator has a working camera app of some kind installed.
* some tests assume that a file keys.txt holding imagery API keys is present (otherwise the layers in question are not added), this should be located in ../private_assets/keys2.txt relative to the repo directory. A fake such file is provided in the unit test assets.
* Android 9 / API 28 emulators seem to be very fickle wrt long click timeouts. The build script tries to address this by setting a different value for such devices in setLongClickTimeout, however this may need some tuning on test devices with high load. The typical symptom of this issue is the selectNode and dragNode tests failing.
* if chrome is installed on a device used for testing googles ToS need to be accepted or else some tests will fail, you can disable chrome (which should lead to the pre-installed test browser being used) on emulators by running ``disableChrome``.