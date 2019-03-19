# Frequently Asked Questions

#### What is Vespucci?

Vespucci is the first [OpenStreetMap](http://www.openstreetmap.org/) editor for Android.
That means: it is a light-weight, easy-to-use mapping tool on mobile Android devices.

#### What is Vespucci NOT?

  * Vespucci is not primarily a mobile map-viewer. 
  * Vespucci is not a mobile navigation solution. It does not feature any routing support.
  * Vespucci is not a full replacement for JOSM or other desktop editors, but nearly. It strives for maximum usability on mobile devices which are limited in many regards. 

#### I am already familiar with OSM editor xyz. Why should I use Vespucci?
If you are mapping for OSM and already have an Android device (or planning for this) then Vespucci could help in your mapping work. Data is acquired "on site" and uploaded to the OSM server. By entering data in your mobile device "on the road" you can save time because you do not have to upload GPX tracks to your home PC and work your way again through these.
Additionally, obscure OSM data can be verified easily and quickly "on site", data which is already available in OSM is not acquired again.

#### What do I need to get started with Vespucci?

You need:

  * An Android device (or the emulator), note that Release 0.9.x has only been tested on Android 2.3 and later
  * The Vespucci APK file (available from Google Play and GitHub)
  * Some familiarity with [OSM Map features](http://wiki.openstreetmap.org/wiki/Map_Features)
  * An [OSM account](http://www.openstreetmap.org/user/new)

#### What is the status of Vespucci development?

The current Vespucci release version is 12.1. We typically provide monthly updates to the released version (bug fixes and updated presets and imagery configurations), minor releases roughly every quarter and major release once to twice a year.

#### Is Vespucci available for other mobile platforms?

No, with the exception of those that provide an Android compatible environment.

#### How can I obtain Vespucci?

See [Obtaining Vespucci](/#obtaining-vespucci).

#### How can I install Vespucci on my Android device?

Just download and install like any other app =)

#### Which special permissions does Vespucci require?

   * fine location (GPS): to be able to pan the map to your current position and generate GPX format tracks
   * write to external storage: background imagery tiles are stored, if present, on external storage, removing this permission will make the background process that downloads tiles non-functional
   
#### Vespucci warning that it cannot write to an external SD card

Vespucci preferentially attempts to store aerial imagery data on an external SD card if present, this is slower but leaves expensive internal storage free. On some devices this seems to cause persistent issues that are not resolved by giving the app permissions to write to external storage. A potential workaround is to remove the SD card (if this works in your setup), and then run vespucci. If that works and background imagery is displayed you can reinsert the SD card and vespucci should continue to use the internal storage.

#### How can I install Vespucci on the Android emulator?

Installation of the Vespucci APK is like any other APK.
There are plenty of descriptions available (e.g. [1 ](http://openhandsetmagazine.com/2008/01/tips-how-to-install-apk-files-on-android-emulator/), [2 ](http://www.androidfreeware.org/tutorials/how-to-install-apk-files-on-android-device-emulator), [3 ](http://www.freeware4android.com/2008/07/30/tutorial-installing-apk-files-on-android-device-emulator.html), [4 ](http://clipmarks.com/clipmark/FB4A2E39-6DA1-4EBC-BBF0-5131E1AC6128/))

#### Running Vespucci on "old and small" devices

Modern (0.8 and up) Vespucci versions have been tested and found to work on Android 2.3 (2.2 was last supported in version 0.9.8) and later,
however older devices tend to have very limited memory and correspondingly the apps are allocated very small amounts of heap (this can be as low as 16MB). If you are trying to run Vespucci on such a device, particularly with 0.9.4 and later, the following hints should be helpful (ordered in decreasing order of importance):

  * turn off any map overlay
  * only load small map areas and don't excessively use the incremental load facility
  * turn off notes and photo overlay
  * turn off name suggestions
  * don't add large presets
  * use the <a href="vespucci://preset/?preseturl=https://github.com/simonpoole/beautified-JOSM-preset/blob/master/gen/vespucci_zip_no_translations.zip%3fraw=true&presetname=Default preset without translations">Default preset without translations</a> instead of the one packaged with the app.
  
The location dependent tagging and validation feature is not available on such devices as it alone requires 4MB of heap. 
  
#### Can't download data from OpenStreetMap servers 

If it is not a connectivity issue you may be running in to the following problem: current Vespucci versions use HTTPS (encrypted connections) to connect to the OpenStreetMap servers, if you are running on an older Android version this may be failing due to problems the old devices have with more recent certificates. 

Workaround: create a new non-HTTPS API entry (enter "http://api.openstreetmap.org/api/0.6/" as API URL) and select that. 0.9.8 and later versions already have such an entry, you only need to activate it. 

Note that the same issue may apply to certain background and overlay layers.

#### "301 Moved Permanently" error when trying to download

The OpenStreetMap API server you are using is likely redirecting HTTP (non-encrypted) to HTTPS (encrypted) connections. Try changing the API configuration to use HTTPS.

#### What can I do with the editor?

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

... and much more.

#### Does Vespucci support OSM Notes?

Yes, Vespucci supports manual and automatic download of Notes and offline storage of them, further it supports displaying and editing warnings produced by the OSMOSE quality assurance system. OpenStreetBugs is no longer supported.

#### What are the limits of Vespucci?

Some things missing at this point in time:

  * No validator (however in general Vespucci tries to stop you from shooting yourself in the foot)
  * Some operations still missing, for example polygon merging.
  
> Remember, Android is intended to be lightweight and easy-to-use.

#### Which languages are supported?

The user interface is currently available in: English, German, Chinese (Taiwan), Spanish, Ukrainian, Russian, Turkish, French, Italian, Vietnamese, Chinese, Icelandic, Greek, Portuguese, Japanese, Hebrew, Hungarian, Polish and Czech. These translations are typically complete or only have a small number of terms missing.

We also have partial translations for a number of other languages (please see link to Transifex below for the current status). If the translation for your language is incomplete or missing, please feel free to request the language on Transifex and start translating. Any help in this area would be gratefully received. Please see [Vespucci's Transifex page](https://www.transifex.com/projects/p/vespucci/) to help.

#### How can I download OSM data?

On the first time startup, Vespucci requests which area to download. You can choose from the following options:

 * Current location
 * Last known location
 * Coordinates. If you need access to specific geographic coordinates you can specify latitude/longitude.
 * Search for a location (New in 0.9.4, requires network connectivity)

For all download options, the additional parameter "Radius" is used. It specifies how large the downloaded area is. (In detail: Radius is half the side length of the bounding box that is used when downloading.)

You can alternatively dismiss the dialog, zoom and pan to the area in question and then select the "Download current view" from the transfer menu.

#### How can I upload new/changed data to the OSM server?

Choose "Upload data to  OSM server" from the transfer menu.
(With other words: Data is not automatically uploaded!)

#### Which user account is used when uploading data to the OSM server?

Vespucci uses OAuth authorization by default for new installations. On your first upload you will be directed to an OSM page where you will need to grant Vespucci permission to use your OpenStreetMap account. It is not necessary nor recommended to store username/password on your device (it is however possible if OAuth causes problems for whatever reasons).

Note: OAuth will work for both the OSMF-operated standard API and the development servers, if you are running your own or need to access a third party site with OAuth you need to add the corresponding secrets to the API configuration and rebuild Vespucci.

#### OAuth authorization fails

If OAuth authorization fails, for example with a blank screen, you may need to disable any ad blocker you have installed during the authorization process. For example Blockada is known to cause issues, but any blocker that uses Easylist will likely have similar problems, see [discussion on Easylist repo](https://github.com/easylist/easylist/pull/906) for some background.  

#### How can I zoom into an area?

 * Use the pinch-to-zoom multi-touch gesture.
 * Use the on-screen zoom controls.
 * Use the volume buttons of your Android device.

#### Conflict resolution

Vespucci has a built in conflict resolution capability. If you want finer grain control over the resolution process you can export all your changes to a .osm file, open that with JOSM and use JOSM's conflict resolution capabilities.

#### Can't enter key with MessageEase

When word prediction is turned on in MessageEase it causes issues entering text for keys in the "Details" tab, disabling word prediction allows it to work.

#### Why do I have to have GPS turned on?

You don't. You can return to the app and continue working by pressing the back button when the system asks you to turn on GPS (location based functions will not work). 

The reasoning behind this is that we assume that typical use is an on the ground survey and that you will want your current position to be displayed on the map. As a result the standard behaviour is to ask you (once) on start up of the app to turn GPS on, you can however disable this in the "Advanced Preferences" in the GPS section, by setting the "Leave GPS off" preference. 

#### The translation for language X is incomplete or wrong!

Vespucci is a community developed, open source, project. While not everybody will be able to contribute to the code, everybody should be able to work on the translations on 
[transifex](https://www.transifex.com/openstreetmap/vespucci/). Translations in the app are updated typically once a month.  

#### The translation for the presets is broken!

The preset translations tend to be a work in progress and given that a large number (over 2'300) of terms have to be translated, many are incomplete. From Vespucci 12.0 on you can disable the use of translations per preset, a long-click on the respective preset entry in the preset preference screen will allow you to change the behaviour.

#### Presets for military objects are missing!

Due to the touchy situation having pre-defined presets for military objects on device could create you need to download them and install them yourself. You can find a link at the bottom of the preset help page.

#### Fullscreen mode doesn't work properly

Fullscreen mode on Android is a bit hit and miss (depending on Android version and device) as it is really designed for games and for apps without keyboard use. If the Android buttons are hidden, the standard Android way to show them, at least in recent Android versions, is a swipe from the bottom edge. 

If fullscreen mode is causing issues for you, the best solution is to simply turn it off in the "Advanced preferences". 

#### Vespucci is getting slower and slower

Adding more data via the auto-download facility and/or the menu item "Add current view to download" increases the amount of the data retained in memory. Due to the way Android works this has to be saved and re-loaded in a number of situations, including, naturally, on restart of the app. Further certain operations that need to select a subset of the data in memory can be impacted by this slow down too. 

Recommended practice, if you no longer need the previously loaded data and have uploaded all edits, is to reset the data in memory by using the "Download current view" action.

#### How can I add a custom (aerial/satellite) imagery layer

The preferred method is to add your imagery source to [the editor layer index](https://github.com/osmlab/editor-layer-index) so that it can be used by all OpenStreetMap editors. 

From 10.1 on, the "Advanced preferences" contain an option to add custom imagery via a form. However adding to the editor layer index is still preferred, in 10.1 you can update the complete configuration at any time by selecting the corresponding menu entry in the "Tools" menu.


#### Why can't you provide technical support on the google play store review section

The google play store allows only one reply to a review with a maximum length of just 350 characters. If you believe that what you are experiencing is a clear malfunction please open a new issue on [github](https://github.com/MarcusWolschon/osmeditor4android/issues), if you have questions or can't find how do a specific operation you can ask on the [OpenStreetMap help site](http://help.openstreetmap.org) or one of the other OpenStreetMap communication channels. 

350 characters is 100 characters less than the text above.

#### On startup Vespucci complains that it can't write to a directory

Vespucci stores downloaded background and overlay tiles in a database that it tries to create on removable storage if present, this to preserve space on your device at the expense of access potentially being a bit slower.

If the app can't write to the database it will show a message asking you to submit a crash dump and will not display any background layer. The exact reasons why this happens, even if the installation was able to create and write to the database previously and for example you just rebooted, are not clear. However in some cases is seems to be linked to Vespucci being moved to removable storage. If this happens to you try moving the app back to the regular on device storage. 

To recover from the directory containing the tile database not being writable you do not need to re-install Vespucci, simply clearing the apps data in your application manager is enough. Alternatively you can eject the SD card and restart Vespucci, that will then create the directory on your device and you should be able to delete the directory on the SD card (typically: Android/data/de.blau.android).

#### Other tile database related issues

While rare, now and then the database holding the background tiles can become corrupted, for example by a sudden power loss. 

To resolve you can clear all the data for the app (upload any pending changes before that), or you can try to simply remove the database file,. The location is typically (on removable storage or on the internal "sdcard") Android/data/de.blau.android/files/databases/osmaptilefscache_db.db, remove the accompanying journal file too.

#### Can't save MapRoulette API Key

If you have an existing Vespucci installation you may have to re-authorize the application by going to "Tools" - "Authorize OAuth" menu. This is due to a change in the OSM API that occured after the release of the support for MapRoulette. 