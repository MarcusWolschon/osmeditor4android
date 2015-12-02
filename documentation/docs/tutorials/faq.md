# Frequently Asked Questions #

#### What is Vespucci?
Vespucci is the first [OpenStreetMap](http://www.openstreetmap.org/) editor for Android.
That means: It is a light-weight, easy-to-use mapping tool on mobile Android devices.


#### What is Vespucci NOT? ####
  * Vespucci is not primarily a mobile map-viewer. 
  * Vespucci is not a mobile navigation solution. It does not feature any routing algorithm.
  * Vespucci is not a full replacement for JOSM or other desktop editors, but nearly. It strives for maximum usability on mobile devices which are limited in many regards. 

#### I am already familiar with OSM editor xyz. Why should I use Vespucci? ####
If you are mapping for OSM and already have an Android device (or planning for this) then Vespucci could help in your mapping work. Data is acquired "on site" and uploaded to the OSM server. By entering data in your mobile device "on the road" you can save time because you do not have to upload GPS tracks to your home PC and work your way again through these.
Additionally, obscure OSM data can be verified easily and quickly "on site", data which is already available in OSM is not acquired again.

#### What do I need to get started with Vespucci? ####
You need:

  * An Android device (or the emulator) Release 0.9.X has only been tested on Android 2.3 and later.
  * The Vespucci APK file (available from the Android Market and github)
  * Some familiarity with [OSM Map features](http://wiki.openstreetmap.org/wiki/Map_Features)
  * An [OSM account](http://www.openstreetmap.org/user/new)

#### What is the status of Vespucci development? ####
The current Vespucci version is 0.9.7 nearing release.

#### Is Vespucci available for other mobile platforms? ####
No, with the exception of those that provide an Android compatible environment.

#### How can I obtain Vespucci? ####
See [Obtaing Vespucci](/#obtaining-vespucci)

#### How can I install Vespucci on my Android device? ####
Just download and install like any other app =)


#### How can I install Vespucci on the Android emulator? ####
Installation of the Vespucci APK is like any other APK.
There are plenty descriptions available (e.g. [1 ](http://openhandsetmagazine.com/2008/01/tips-how-to-install-apk-files-on-android-emulator/), [2 ](http://www.androidfreeware.org/tutorials/how-to-install-apk-files-on-android-device-emulator), [3 ](http://www.freeware4android.com/2008/07/30/tutorial-installing-apk-files-on-android-device-emulator.html), [4 ](http://clipmarks.com/clipmark/FB4A2E39-6DA1-4EBC-BBF0-5131E1AC6128/))

#### Running Vespucci on "old and small" devices ####

Modern (0.8 and up) Vespucci versions have been tested and found to work on Android 2.2 and later,
however older devices tend to have very limited memory and correspondingly the apps are allocated very small amounts of heap.
This can be as low as 16MB. If you are trying to run Vespucci on such a device, particularly with 0.9.4 and later, the following hints
should be helpful (ordered in decreasing order of importance):

  * turn off any map overlay
  * only load small map areas and don't excessively use the incremental load facility
  * turn off notes and photo overlay
  * turn off name suggestions
  * don't add large presets

#### What can I do with the editor?

Currently, you can

 * Add and move nodes, ways and relations
 * Rotate ways
 * Edit the tags of nodes, ways and relations
 * Append nodes to existing ways
 * Delete existing nodes, ways and relations
 * Merge nodes
 * Edit existing relation mambers and add new 
 * Add turn restrictions
 * Download area around current location from OSM server
 * Download user-specified areas from OSM server
 * Create, save, upload and import GPS tracks

and much more.

#### Does Vespucci support OSM Notes? ####

Yes, Vespucci supports manual and automatic download of Notes and offline storage of them, further it supports displaying and editing warnings produced by the OSMOSE quality assurance system. OpenStreetBugs is no longer supported.

#### What are the limits of Vespucci? ####

Some things missing at this point in time

  * No validator (however in general Vespucci tries to stop you from shooting yourself in the foot)
  * No connectivity checks for relations
  * No individual relation member download and no functions to change the order of members (planned)

> Remember, Android is intended to be lightweight and easy-to-use.

#### Which languages are supported? ####

The user interdace is currently available in: English, German, Chinese (Taiwan), Spanish, Ukranian, Russian, Turkish, French, Italian, Vienamese, Chinese, Icelandic, Greek, Portugeses and Janpanese. These translations are typically complete or only have a small number of terms missing.

We also have partial translations for a number of other languages (please see link to transifex below for the current status). Any help in this area would be gratefully received. Please see https://www.transifex.com/projects/p/vespucci/

#### How can I download OSM data? ####

On startup, Vespucci requests which area to download. You can choose from the following options:

 * Current location
 * Last known location
 * Coordinates. If you need access to specific geographic coordinates you can specify latitude/longitude.
 * Search for a location (New in 0.9.4, requires network connectivity)

For all download options, the additional parameter "Radius" is used. It specifies how large the downloaded area is. (In detail: Radius is half the side length of the bounding box that is used when  downloading.)

You can alternatively dismiss the dialog, zoom and pan to the area in question and then select the "Download current view" from the transfer menu.

#### How can I upload new/changed data to the OSM server? ####

Choose "Upload data to  OSM server" from the transfer menu.
(With other words: Data is not automatically uploaded!)

#### Which user account is used when uploading data to the OSM server? ####

Vespucci will use OAuth authorization as default for new installs. On your first upload you will be directed to an OSM page that we ask you to authorize your Vespucci install. It is not necessary nor recommended to store username/password on your device (it is however possible if OAuth causes problems for whatever reasons).


#### How can I zoom into an area? ####

 * Use the pinch-to-zoom multi-touch gesture.
 * Use the on-screen zoom controls.
 * Use the volume buttons of your Android device.

#### Conflict resolution ####

vespucci has a built in conflict resolution capability. If you want finer grain control over the resolution process you can export all your changes to a .osc file, open that with JOSM and use JOSMs conflict resolution capabilities.

