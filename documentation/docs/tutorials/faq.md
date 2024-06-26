
# Frequently Asked Questions

## General

### What is Vespucci?

Vespucci is the first [OpenStreetMap](http://www.openstreetmap.org/) editor for Android.
That means: it is a light-weight, easy-to-use mapping tool on mobile Android devices.

### What is Vespucci NOT?

  * Vespucci is not primarily a mobile map-viewer or map designer.
  * Vespucci is not a mobile navigation solution. It does not feature any routing support.
  * Vespucci is not a full replacement for JOSM or other desktop editors, but nearly. It strives for maximum usability on mobile devices which are limited in many regards. 

### I am already familiar with OSM editor xyz. Why should I use Vespucci?
If you are mapping for OSM and already have an Android device (or planning for this) then Vespucci could help in your mapping work. Data is acquired "on site" and uploaded to the OSM server. By entering data in your mobile device "on the road" you can save time because you do not have to upload GPX tracks to your home PC and work your way again through these.
Additionally, obscure OSM data can be verified easily and quickly "on site", data which is already available in OSM is not acquired again.

### What do I need to get started with Vespucci?

You need:

  * An Android device (or the emulator), note that Release 0.9.x has only been tested on Android 2.3 and later
  * The Vespucci APK file (available from Google Play and GitHub)
  * Some familiarity with [OSM Map features](http://wiki.openstreetmap.org/wiki/Map_Features)
  * An [OSM account](http://www.openstreetmap.org/user/new)

### What can I do with the editor?

Currently, you can:

 * Add and move nodes, ways and relations
 * Rotate ways
 * Edit the tags of nodes, ways and relations
 * Append nodes to existing ways
 * Delete existing nodes, ways and relations
 * Merge nodes
 * Edit existing relation members and add new 
 * Add turn restrictions
 * Download area around current location from OSM server
 * Download user-specified areas from OSM server
 * Create, save, upload and import GPX tracks

... see [Vespucci feature list](features.md) for more.

### Does Vespucci support OSM Notes?

Yes, Vespucci supports manual and automatic download of Notes and offline storage of them, further it supports displaying and editing warnings produced by the OSMOSE quality assurance system. OpenStreetBugs is no longer supported.

### What are the limits of Vespucci?

Some things missing at this point in time:

  * No extensive validation (however in general Vespucci tries to stop you from shooting yourself in the foot)
  * Property (tag) editor does not run concurrently with the main map and object display (note that there is the experimental [Enable split window property editor](../help/en/Advanced%20preferences.md) setting that will support this, however this tends to work best on tablets and is quite cramped on phones).

### Which languages are supported?

The user interface is currently available in: English, German, Chinese (Taiwan), Spanish, Ukrainian, Russian, Turkish, French, Italian, Vietnamese, Chinese, Icelandic, Greek, Portuguese, Japanese, Hebrew, Hungarian, Polish and Czech. These translations are typically complete or only have a small number of terms missing.

We also have partial translations for a number of other languages (please see link to Transifex below for the current status). If the translation for your language is incomplete or missing, please feel free to request the language on Transifex and start translating. Any help in this area would be gratefully received. Please see [Vespucci's Transifex page](https://www.transifex.com/projects/p/vespucci/) to help.

### What is the status of Vespucci development?

We typically provide monthly updates to the released version (bug fixes, updated presets and imagery configurations), minor releases roughly every quarter and major release once to twice a year.

### Is Vespucci available for other mobile platforms?

No, with the exception of those that provide an Android compatible environment.

### Supported devices

Current Vespucci builds support Android 4.1 and later. Pre-release testing has been done on a limited number of physical devices, but with the exception of manufacturer specific quirks there is no reason to believe that it isn't compatible with all devices with a current Android version. Note that pre-Android 5 devices are not recommended due both to the limited resources they typically have and the age of the operating system.

Physical devices:

    5.1.1 Amazon Fire (5th gen) tablet
    6.0 HTC One M8
    6.0.1 Samsung S5
    8.0 Samsung S7
    9.0 Samsung Note 8 
    11.0 Samsung A40
    11.0 Samsung S5e tablet
    13.0 Samsung S23 Ultra
    
Emulators:
    
    4.1.2
    4.4.2
    7.1.1
    8.1 (full test suite)
    9.0 (full test suite)
    11.0 (full test suite)
    12.0 (full test suite)
    13.0 
    
## Installation

### How can I obtain Vespucci?

See [Obtaining Vespucci](/#obtaining-vespucci).

### How can I install Vespucci on my Android device?

Just download and install like any other app =)

### Which special permissions does Vespucci require?

   * fine location (GPS): to be able to pan the map to your current position and generate GPX format tracks
   * write to external storage: background imagery tiles are stored, if present, on external storage, removing this permission will make the background process that downloads tiles non-functional
   
### How can I install Vespucci on the Android emulator?

Installation of the Vespucci APK is like any other APK.
There are plenty of descriptions available (e.g. [1 ](http://openhandsetmagazine.com/2008/01/tips-how-to-install-apk-files-on-android-emulator/), [2 ](http://www.androidfreeware.org/tutorials/how-to-install-apk-files-on-android-device-emulator), [3 ](http://www.freeware4android.com/2008/07/30/tutorial-installing-apk-files-on-android-device-emulator.html), [4 ](http://clipmarks.com/clipmark/FB4A2E39-6DA1-4EBC-BBF0-5131E1AC6128/))

### Using third party builds of Vespucci

If you have a third party build of Vespucci installed (for example from F-Droid), that is you haven't obtained the app from google, Amazon or directly from our github repository, you need to be aware that certain functionality will be disabled because of missing API keys, in particular:

   * displaying Mapillary data,
   * sending feedback to us via Github,
   * and numerous background layers, for example Bing aerial imagery
   
will not be available. If you obtain your own keys, you can install them via the _Load keys from file..._ mechanism.

### Removing the Bing imagery layer

Due to an implementation detail, once per session a metadata call will be made to the Bing servers, potentially leaking some minimal information (IP address, and the fact that you are using Vespucci to edit OSM). You can remove the Bing API key which will disable the Bing source and the behaviour with the following steps:

   * download and save [remove_bing_key.txt](other/remove_bing_key.txt)
   * in the tools menu select _Load keys from file..._ and load the saved file
   * update the imagery configuration from the _More imagery tools..._ _Update configuration from ELI_ (or if you are using the JOSM configuration from the other similar entry)
   * restart the app

Note that this can only be reversed by either you providing a new Bing API key, or completely removing Vespuccis data directory.

## Using Vespucci

### How can I upload new/changed data to the OSM server?

Choose "Upload data to  OSM server" from the transfer menu.
(With other words: Data is not automatically uploaded!)

### Which user account is used when uploading data to the OSM server?

Vespucci uses OAuth authorization by default for new installations. On your first upload you will be directed to an OSM page where you will need to grant Vespucci permission to use your OpenStreetMap account. It is not necessary nor recommended to store username/password on your device (it is however possible if OAuth causes problems for whatever reasons).

Note: OAuth will work for both the OSMF-operated standard API and the development servers, if you are running your own or need to access a third party site with OAuth you need to add the corresponding secrets to the API configuration and rebuild Vespucci.

### How can I zoom into an area?

 * Use the pinch-to-zoom multi-touch gesture.
 * Use the on-screen zoom controls.
 * Use the volume buttons of your Android device.

### How can I add a custom (aerial/satellite) imagery layer

The preferred method is to add your imagery source to the [Editor Layer Index](https://github.com/osmlab/editor-layer-index) so that it can be used by all OpenStreetMap editors. 

From 10.1 on, the "Advanced preferences" contain an option to add custom imagery via a form, however adding to the [ELI](https://github.com/osmlab/editor-layer-index) is still preferred. You can update the complete configuration at any time by selecting the corresponding menu entry in the _Tools/More imagery tools..._ menu. Note if you prefer to use JOSMs imagery configuration you can update from that source in the same place.

### Conflict resolution

Vespucci has a built in conflict resolution capability. If you want finer grain control over the resolution process you can export all your changes to a .osm file, open that with JOSM and use JOSM's conflict resolution capabilities.
  
## Support and help

### Where to get help

Most content from this web site is available on your device. Many of the menus contain a _Help_ entry that will display useful information for the current context. You can start the help browser stand alone (long press on the Vespucci icon in your launcher) and on modern Android versions use split-screen mode to display the contents in parallel with the editor.

If you cannot find an answer in the help system, feel free to open an issue on our github repository via the _Provide feedback_ item in the main menu.   

### Why can't you provide technical support on the google play store review section

The google play store allows only one reply to a review with a maximum length of just 350 characters. If you believe that what you are experiencing is a clear malfunction please open a new issue on [github](https://github.com/MarcusWolschon/osmeditor4android/issues), if you have questions or can't find how do a specific operation you can ask on the [OpenStreetMap help site](http://help.openstreetmap.org) or one of the other OpenStreetMap communication channels. 

350 characters is 100 characters less than the text above.  
  
## Error messages and other issues

### App hangs

With better rendering support for OSM objects that have "sides", for example embankments and cliffs, there have been more reports of the app hanging on startup after the "Loading presets" message appears.

In our analysis this is due to issues with [Skia]( https://skia.org/), the low level graphics library that Android utilizes, on devices running Android 8 and older. It seems as if Skia
creates persistent state when [PathDashPathEffects](https://developer.android.com/reference/android/graphics/PathDashPathEffect) is used that is is some form corrupted when the app is forced
closed. Starting Vespucci in such a situation will lead to the app hanging or crashing.

As a workaround version 19.0.2 and later provide a shortcut that will start the app in "Safe" mode that will set the data rendering style to the minimal built-in style. In testing this allows startup to succeed and the style can then be (manually) changed back. As shortcuts are only available from Android 7.1.1 on, we've further added a data style that avoids the critical style elements that can be used on older devices.

It should be noted that "force closing" any Android app is a bad idea and should only be done when absolutely necessary.

### Vespucci warning that it cannot write to an external SD card

Vespucci preferentially attempts to store aerial imagery data on an external SD card if present, this is slower but leaves expensive internal storage free. On some devices this seems to cause persistent issues that are not resolved by giving the app permissions to write to external storage. A potential workaround is to remove the SD card (if this works in your setup), and then run Vespucci. If that works and background imagery is displayed you can reinsert the SD card and Vespucci should continue to use the internal storage.

### Can't download data from OpenStreetMap servers 

If it is not a connectivity issue you may be running in to the following problem: current Vespucci versions use HTTPS (encrypted connections) to connect to the OpenStreetMap servers, if you are running on an older Android version this may be failing due to problems the old devices have with more recent certificates. 

Workaround: create a new non-HTTPS API entry (enter "http://api.openstreetmap.org/api/0.6/" as API URL) and select that. 0.9.8 and later versions already have such an entry, you only need to activate it. 

Note that the same issue may apply to certain background and overlay layers.

### "301 Moved Permanently" error when trying to download

The OpenStreetMap API server you are using is likely redirecting HTTP (non-encrypted) to HTTPS (encrypted) connections. Try changing the API configuration to use HTTPS.

### I've downloaded data, but the screen remains blank

There are a number of potential reasons for this, all of them harmless:

  * preset or tag filter turned on, check the main 'overflow' menu (three dots).
  * You are not in the "Normal" editing mode. A long press on the lock button will display the mode menu, switch to "Normal" mode and check if things are better.
  * data layer disabled or hidden by an other layer, check the layer dialog ('hamburger' menu in the upper right corner). If this is the issue you can move the layers via the menu associated with each layer. Note that the Layer stack is as displayed in the dialog and that non-transparent layers will obscure any layer beneath them.
  * API sandbox selected instead of normal API, check the configuration of the data layer in the layer dialog. The sandbox API has no data in most places, you would have to strike it very lucky to see something.

### OAuth authorization fails

If OAuth authorization fails, for example with a blank screen, you may need to disable any ad blocker you have installed during the authorization process. For example Blockada is known to cause issues, but any blocker that uses Easylist will likely have similar problems, see [discussion on Easylist repo](https://github.com/easylist/easylist/pull/906) for some background.  

### Can't enter key with MessageEase

When word prediction is turned on in MessageEase it causes issues entering text for keys in the "Details" tab, disabling word prediction allows it to work.

### Why do I have to have GPS turned on?

You don't. You can return to the app and continue working by pressing the back button when the system asks you to turn on GPS (location based functions will not work). 

The reasoning behind this is that we assume that typical use is an on the ground survey and that you will want your current position to be displayed on the map. As a result the standard behaviour is to ask you (once) on start up of the app to turn GPS on, you can however disable this in the "Advanced Preferences" in the GPS section, by setting the "Leave GPS off" preference. 

### Location unstable

Vespucci will generally prefer location information from satellite based position systems aka GPS or GNSS. If it hasn't received a location update for a longer time (20 x the _Minimum GPS interval_ up to version 19.0, the _Stale location after_ value from 19.1 onwards), it will fallback to a "network" location provider if one is enabled and _Fallback to network location_ is turned on. As "network" services derived locations can differ substantially from GNSS derived ones this can lead to the impression of a jittery location if the the GNSS location is updated rarely, to avoid this turn _Fallback to network location_ off. Track recording and similar functions in Vespucci all only work with positions from GNSS services. All location related settings can be found in the _Advanced preferences_/_Location Settings_

Note that any setting on your device that indicates better local accuracy typically uses network derived location services and leaks your position information to google. 

### The translation for language X is incomplete or wrong!

Vespucci is a community developed, open source, project. While not everybody will be able to contribute to the code, everybody should be able to work on the translations on 
[transifex](https://www.transifex.com/openstreetmap/vespucci/). Translations in the app are updated typically once a month.  

### The translation for the presets is broken!

The preset translations tend to be a work in progress and given that a large number (over 2'300) of terms have to be translated, many are incomplete. From Vespucci 12.0 on you can disable the use of translations per preset, a long-click on the respective preset entry in the preset preference screen will allow you to change the behaviour.

### Presets for military objects are missing!

Due to the touchy situation having pre-defined presets for military objects on device could create you need to download them and install them yourself. You can find a link at the bottom of the preset help page.

### Fullscreen mode doesn't work properly

Fullscreen mode on Android is a bit hit and miss (depending on Android version and device) as it is really designed for games and for apps without keyboard use. If the Android buttons are hidden, the standard Android way to show them, at least in recent Android versions, is a swipe from the bottom edge. 

If fullscreen mode is causing issues for you, the best solution is to simply turn it off in the [Advanced preferences](../help/en/Advanced%20preferences.md). 

### The aerial or satellite imagery is out of date

Vespucci uses the [Editor Layer Index](https://github.com/osmlab/editor-layer-index) for configuring the available backgrounds. The contents cover essentially all imagery that can legitimately be used for OpenStreetMap purposes. If you believe the imagery for a specific region is out of date or there are better sources you should preferably update the ELI or open an issue there.

### On startup Vespucci complains that it can't write to a directory or reports an error

Vespucci stores downloaded background and overlay tiles in a database that it tries to create on removable storage if present, this to preserve space on your device at the expense of access potentially being a bit slower.

If the app can't write to the database it will show a message and will not display any background layer. 

To recover from the directory containing the tile database not being writable you do not need to re-install Vespucci, simply clearing the apps data in your application manager is enough. If 
the problem is related to access to a removable storages, disabling the "Prefer removable storage" in the [Advanced preferences](../help/en/Advanced%20preferences.md) "Layer download and storage" section may help.

### Other tile database related issues

While rare, now and then the database holding the background tiles can become corrupted, for example by a sudden power loss. 

To resolve you can clear all the data for the app (upload any pending changes before that), or you can try to simply remove the database file,. The location is typically (on removable storage or on the internal "sdcard") in `Android/data/de.blau.android/files/databases/` remove `osmaptilefscache_db.db` and the accompanying journal file too.

### Background imagery cache

Background imagery tiles are cached on device in a fixed sized (configurable in the [Advanced preferences](../help/en/Advanced%20preferences.md) cache for all sources, they remain there until they are either explicitly flushed or they are removed when the cache is full. 

Cached tiles are available offline with the exception of "Bing" imagery. Due to the way the "Bing" API works cached tiles currently can't be accessed without a network connection.

### Can't save MapRoulette API Key

If you have an existing Vespucci installation you may have to re-authorize the application by going to "Tools" - "Authorize OAuth" menu. This is due to a change in the OSM API that occured after the release of the support for MapRoulette. 

### Navigation gestures on Android 9 and later

Android 9 introduced the option to replace the navigation bar at the bottom of the screen with gestures, these were further refined with the release of Android 10. If you have this enabled you will notice that this conflicts with the menu bar at the bottom of the screen used by Vespucci.

There is no completely satisfactory solution to this, but if you don't want to disable the gestures the best setup is to change the following settings in the [Advanced preferences](../help/en/Advanced%20preferences.md) user interface settings:

* turn off __Enable split action bar__, this will move the menu bar to the top of the screen.
* set __Fullscreen mode__ to _never_ in the _Advanced preferences_ 

Vespucci 15.0 and higher will detect that gestures have been enabled and will try to configure itself appropriately.

### Long press on Undo button doesn't do anything

This most likely due to you having navigation gestures configured, but the app not being able to detect this. For example this has been the case with devices running the /e/ variant of Android. Switching __Fullscreen mode__ to _never_ in the _Advanced preferences_ should allow the Undo button to work as intended.

### Can't (re-)authenticate - TLS 1.0 / 1.1 issues

Many sites have turned off support for TLS 1.0 / 1.1 secured connections, this includes the API on openstreetmap.org. This renders Vespucci on devices with an Android version older than 4.1 essentially useless. 

On Android versions between 4.1 and 4.4 the app can be used without issues, except that authorization with OAuth will potentially not work. If you are unable to use OAuth you can still simply authenticate with login and password. To enable this:

* go to _Preferences_ -> _Advanced preferences_ -> _Server settings_ and set login and password under the _User account_ entry.
* in the layer control select the _Configure_ menu entry for the data layer entry, edit the active entry, and uncheck OAuth support.   

### Resuming the app doesn't bring me back to the property editor

Depending on the device, Android version and the phase of the moon, some Android launchers will restart the app instead of resuming it if you click on the icon in the app draw instead of selecting the app from the current application list. This will lead to any state/edit in the property editor being lost if you had navigated away from it, for example recent Samsung devices exhibit this behaviour. 

If you are experiencing this issue, setting _Use "new task" mode for property editor_ in the experimental section of the _Advanced preferences_ may help.

## Performance

### Vespucci is getting slower and slower

Adding more data via the auto-download facility and/or the menu item "Add current view to download" increases the amount of the data retained in memory. Due to the way Android works this has to be saved and re-loaded in a number of situations, including, naturally, on restart of the app. Further certain operations that need to select a subset of the data in memory can be impacted by this slow down too. In current versions of Vespucci data downloaded by the auto-download facilities will be automatically pruned if possible, but particularly zooming far out will make this less effective. 

Recommended practice if you no longer need the previously loaded data and have uploaded all edits, is to reset the data in memory by using the "Clear and download current view" action, or manually (by using the corresponding entry in the menu for the data layer) start the prune process.

### Running Vespucci on "old and small" devices

Modern (0.8 and up) Vespucci versions have been tested and found to work on Android 2.3 (2.2 was last supported in version 0.9.8) and later,
however older devices tend to have very limited memory and correspondingly the apps are allocated very small amounts of heap (this can be as low as 16MB). If you are trying to run Vespucci on such a device, particularly with 0.9.4 and later, the following hints should be helpful (ordered in decreasing order of importance):

  * turn off any map overlay
  * only load small map areas and don't excessively use the incremental load facility
  * turn off notes and photo overlay
  * turn off name suggestions
  * don't add large presets
  * use the <a href="vespucci://preset/?preseturl=https://github.com/simonpoole/beautified-JOSM-preset/blob/master/gen/vespucci_zip_no_translations.zip%3fraw=true&presetname=Default preset without translations">Default preset without translations</a> instead of the one packaged with the app.
  
The location dependent tagging and validation feature is not available on such devices as it alone requires 4MB of heap. 

### Rendering is very slow

Rendering multi-polygons is quite complex, and the default style for these is quite computationally expensive and will be slow on older devices. Try switching to the data style without multi-polygon to improve this. See [preferences](../help/en/Preferences.md).
