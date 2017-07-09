# Frequently Asked Questions

#### What is Vespucci?
Vespucci is the first [OpenStreetMap](http://www.openstreetmap.org/) editor for Android.
That means: it is a light-weight, easy-to-use mapping tool on mobile Android devices.

#### What is Vespucci NOT?
  * Vespucci is not primarily a mobile map-viewer. 
  * Vespucci is not a mobile navigation solution. It does not feature any routing algorithm.
  * Vespucci is not a full replacement for JOSM or other desktop editors, but nearly. It strives for maximum usability on mobile devices which are limited in many regards. 

#### I am already familiar with OSM editor xyz. Why should I use Vespucci?
If you are mapping for OSM and already have an Android device (or planning for this) then Vespucci could help in your mapping work. Data is acquired "on site" and uploaded to the OSM server. By entering data in your mobile device "on the road" you can save time because you do not have to upload GPS tracks to your home PC and work your way again through these.
Additionally, obscure OSM data can be verified easily and quickly "on site", data which is already available in OSM is not acquired again.

#### What do I need to get started with Vespucci?
You need:

  * An Android device (or the emulator), note that Release 0.9.x has only been tested on Android 2.3 and later
  * The Vespucci APK file (available from Google Play and GitHub)
  * Some familiarity with [OSM Map features](http://wiki.openstreetmap.org/wiki/Map_Features)
  * An [OSM account](http://www.openstreetmap.org/user/new)

#### What is the status of Vespucci development?
The current Vespucci version is 0.9.8, 0.9.9 is currently in beta release.

#### Is Vespucci available for other mobile platforms?
No, with the exception of those that provide an Android compatible environment.

#### How can I obtain Vespucci?
See [Obtaining Vespucci](/#obtaining-vespucci).

#### How can I install Vespucci on my Android device?
Just download and install like any other app =)

#### How can I install Vespucci on the Android emulator?
Installation of the Vespucci APK is like any other APK.
There are plenty of descriptions available (e.g. [1 ](http://openhandsetmagazine.com/2008/01/tips-how-to-install-apk-files-on-android-emulator/), [2 ](http://www.androidfreeware.org/tutorials/how-to-install-apk-files-on-android-device-emulator), [3 ](http://www.freeware4android.com/2008/07/30/tutorial-installing-apk-files-on-android-device-emulator.html), [4 ](http://clipmarks.com/clipmark/FB4A2E39-6DA1-4EBC-BBF0-5131E1AC6128/))

#### Running Vespucci on "old and small" devices

Modern (0.8 and up) Vespucci versions have been tested and found to work on Android 2.2 and later,
however older devices tend to have very limited memory and correspondingly the apps are allocated very small amounts of heap (this can be as low as 16MB). If you are trying to run Vespucci on such a device, particularly with 0.9.4 and later, the following hints should be helpful (ordered in decreasing order of importance):

  * turn off any map overlay
  * only load small map areas and don't excessively use the incremental load facility
  * turn off notes and photo overlay
  * turn off name suggestions
  * don't add large presets
  
#### Can't download data from OpenStreetMap servers 

If it is not a connectivity issue you may be running in to the following problem: current Vespucci versions use HTTPS (encrypted connections) to connect to the OpenStreetMap servers, if you are running on an older Android version this may be failing due to problems the old devices have with more recent certificates. 

Workaround: create a new non-HTTPS API entry (enter "http://api.openstreetmap.org/api/0.6/" as API URL) and select that. 0.9.8 and later versions already have such an entry, you only need to activate it. 

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
 * Create, save, upload and import GPS tracks

... and much more.

#### Does Vespucci support OSM Notes?

Yes, Vespucci supports manual and automatic download of Notes and offline storage of them, further it supports displaying and editing warnings produced by the OSMOSE quality assurance system. OpenStreetBugs is no longer supported.

#### What are the limits of Vespucci?

Some things missing at this point in time:

  * No validator (however in general Vespucci tries to stop you from shooting yourself in the foot)
  * Some operations still missing, for example polygon merging.
  
> Remember, Android is intended to be lightweight and easy-to-use.

#### Which languages are supported?

The user interface is currently available in: English, German, Chinese (Taiwan), Spanish, Ukrainian, Russian, Turkish, French, Italian, Vietnamese, Chinese, Icelandic, Greek, Portuguese and Japanese. These translations are typically complete or only have a small number of terms missing.

We also have partial translations for a number of other languages (please see link to Transifex below for the current status). Any help in this area would be gratefully received. Please see [Vespucci's Transifex page](https://www.transifex.com/projects/p/vespucci/) to help.

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

Note: OAuth will work for both the standard API and the development servers, if you are running your own or need to access a third party site with OAuth you need to add the corresponding secrets to the API configuration and rebuild Vespucci.

#### How can I zoom into an area?

 * Use the pinch-to-zoom multi-touch gesture.
 * Use the on-screen zoom controls.
 * Use the volume buttons of your Android device.

#### Conflict resolution

Vespucci has a built in conflict resolution capability. If you want finer grain control over the resolution process you can export all your changes to a .osm file, open that with JOSM and use JOSM's conflict resolution capabilities.

#### Can't enter key with MessageEase

When word prediction is turned on in MessageEase it causes issues entering text for keys in the "Details" tab, disabling word prediction allows it to work.

#### Why do I have to have GPS turned on?

You don't. We assume that typical use is an on the ground survey and that you will want your current position to be displayed on the map. As a result the standard behaviour is to ask you (once) on start up of the app to turn GPS on, you can however disable this in the "Advanced Preferences" in the GPS section, by setting the "Leave GPS off" preference.

#### The translation for language X is incomplete or wrong!

Vespucci is a community developed, open source, project. While not everybody will be able to contribute to the code, everybody should be able to work on the translations on 
[transifex](https://www.transifex.com/openstreetmap/vespucci/). Translations in the app are updated typically once a month.  

#### Fullscreen mode doesn't work properly

Fullscreen mode on Android is a bit hit and miss (depending on Android version and device) as it is really designed for games and for apps without keyboard use. If it is causing issues simply turn it off in the "Advanced preferences". 

#### Vespucci is getting slower and slower

Adding more data via the auto-download facility and/or the menu item "Add current view to download" increases amount of the data retained in memory. Due to the way Android works this has to be saved and re-loaded in a number of situations, including, naturally, on restart of the app. Further certain operations that need to select a subset of the data in memory can be impacted by this slowdown too. 

Recommended practice, if you no longer need the previously loaded data and have uploaded all edits, is to reset the data in memory by using the "Download current view" action. 
