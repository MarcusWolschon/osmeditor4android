# Offline Guide
_2024 Tomáš Hnyk_

Vespucci is able to use OSM data stored on your device so you can map in areas without internet coverage and upload your changes later. To do that, follow these steps:

1) If you are lucky and the region you are interested in is included [here](https://mapsplit.poole.ch), you can download the needed data offline through the app. Within reason more areas can be added if there is interest and you ask nicely.

    a) In Vespucci, first tap the two-arrows-in-opposing-directions icon and then select **File...**
<img src="../images/offline_guide_1.jpg" width="250"/>

    b) Select **Download data for offline use...**

<img src="../images/offline_guide_2.jpg" width="250"/>

    c) A view of https://mapsplit.poole.ch will open, just tap on the desired region and Vespucci will download it. Then go to Step 6.

2) If your region is not included, you first need to download the area you are interested in. This service is helpful: https://download.geofabrik.de.
Get the appropriate osb.pbf file and save it to your working directory on your computer.

3) Then you need to use the mapsplit tool that can be downloaded from [Github](https://github.com/simonpoole/mapsplit/tags). Choose the file that has "all" in its name and save it to the same directory where you saved the PBF file.

4) Then run this command on you computer (you need to have a working java installation):

        java -Xmx6G -jar mapsplit-all-0.4.0.jar -tvMm  -i slovenia-latest.osm.pbf -o slovenia.msf -f 2000 -z 14 -O 2000 -s 200000000,20000000,2000000

    In this example, Slovenia PBF file is used. Replace the appropriate filename for your area. Possibly adjust he mapsplit version to the latest. You can adjust the number after `-z` to select the maximal zoom that will be used. It is also possible to use a .poly file to only get a specific subarea like a city. Instructions to do that are on the Mapsplit Github page. For getting the appropriate .poly files, https://osm-boundaries.com/map is recommended. However, in practice, you can usually just use whole countries and do not bother with .poly files. Once you have the .msf file, transfer it to your phone.

5) In Vespucci:

    a) Tap the hamburger menu, then the three vertical dots next to **OpenStreetMap data** and lastly **Configure...**

<img src="../images/offline_guide_3.jpg" width="250"/>

    b) Then tap **Add API**:

<img src="../images/offline_guide_4.jpg" width="250"/>

    c) Choose an appropriate **API name**, under **API URL**, fill in what the other entries use, as of July 2024, it is `https://api.openstreetmap.org/api/0.6/` and finally tap the memory card card icon and choose the .msf file you transferred to your phone in the previous step.

<img src="../images/offline_guide_5.jpg" width="250"/>

    Note that Vespucci copies the file due to [peculiar Android design](https://github.com/MarcusWolschon/osmeditor4android/issues/2455), so you can delete the .msf file afterwards.

6) Now to switch to using offline source instead of downloading OSM data from the internet, choose the tickbox next to your new source, in our case Slovenia:

<img src="../images/offline_guide_6.jpg" width="250"/>

    (to go back to online source, tick "OpenStreetMap")

7) Now, when you want to edit an area coverred by what you downloaded, you can use the "Load current view" function even when you are completely offline.

Of course, when you are offline, you cannot upload your changes to OSM, so use the functions **Export changes to OSC file** and **Save to JOSM file...** (see screenshot from step 1. b)). Due to [Android design](https://github.com/MarcusWolschon/osmeditor4android/issues/2589), the files do not get saved with an extension, so use `.osm` for JOSM and `.osc` for OSC. When you get online again, you can load the changes with **Apply changes from OSC files** or **Read from JOSM file...**. For this to work, you first need to **Load current view** that is bigger than the one used originally. Of course, you could also use these files in JOSM. OSC only saves the changes, JOSM files save the new state after your edit (so the file is much bigger).

Some background on developement of this feature can be found [here](https://www.openstreetmap.org/user/SimonPoole/diary/47275) and [here](https://www.openstreetmap.org/user/SimonPoole/diary/193235).

It is also possible to use [offline background imagery](https://vespucci.io/tutorials/custom_imagery_mbtiles/).
