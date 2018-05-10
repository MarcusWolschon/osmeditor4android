### "network" Location Provider Support

Historically Vespucci has only supported using the on-device GPS location provider, or nothing at all. That meant that you were unable to get a rough location approximation on devices that didn't have onboard GPS, or that had GPS disabled for example to reduce power requirements. The main reason for this is that on the one hand we wanted to avoid location information potentially tainted by your devices Android provider and avoid our users position being tracked by them. 

We now support using so-called "network" location providers, that is location sources that derive your position from the mobile network, WLAN and other signals your device is receiving. If you've enabled such providers on your phone, more on that later, Vespucci will use all available providers for centering the map display on your position and for auto-downloads, tracks will still exclusively be generated from GPS data. 

The change in opinion is mainly due to less and less people caring about such matters and at least google tracking in any case (see for example https://www.theverge.com/2017/11/21/16684818/google-location-tracking-cell-tower-data-android-os-firebase-privacy), further allowing such providers enables better indoor positioning which is a clear advantage.

If Vespucci detects that network positions can at least potentially be used, it will display this icon

![](https://github.com/MarcusWolschon/osmeditor4android/raw/master/src/main/res/drawable-xhdpi/ic_filter_tilt_shift_black_36dp.png) 

instead of the classic GPS icon on the screen and will alert you to which provider it is currently using via toasts (the short on-screen messages). The fallback to network locations can be disabled in the Location Settings in the Advanced Preferences.

Modern devices running a google variant of Android have three location mode setting (besides turning location services completely off):

* __Device only__ - use only the on device GPS location information, does not require sharing your location data with google
* __Battery saving__ - doesn't use GPS, instead uses mobile network, WLAN and other signals to determine your location, requires sharing of your location data with google
* __High accuracy__ - uses GPS and other signals to determine your location, requires sharing of your location data with google, this is typically only more "accurate" than __Device only__ if receiving GPS signals is seriously impaired

Vespucci does not use the Google play servers "fused" location service and remains usable independent of if you are running it in a Google sanctioned environment or not.