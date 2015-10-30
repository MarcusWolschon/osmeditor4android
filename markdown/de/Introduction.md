# Einführung in Vespucci

Vespucci ist ein Editor für OpenStreetMap der die meisten Funktionen unterstützt die ähnliche Programme auf normalen Rechnern vorhanden sind und es ist erfolgreich auf googles Android 2.3 bis 6.0 und verschiedene auf AOSP basierende Varianten getestet worden. Wichtig: während die Leistung von Handys und Tablets Ihre stationären Konkurrenten auf vielen Gebieten eingeholt hat muss man, vorallem bei älteren Geräten, auch nicht vergessen, dass sie eher wenig Hauptspeicher zur verfügung haben und auch deutlich langsamer sein können bei bestimmten Operationen. Deshalb sollte man beim Editieren dies im Auge behalten und, zum Beispiel, die Grösse der editierten Gebiete in vernünftige Grössen halten.   

## Erstmaliger Gebrauch

Beim erstmaligen Starten zeigt Vespucci das "Herunterladen einer anderen Position"/"Bereich laden" Formular. Falls Koordinaten angezeigt werden und sofort heruntergeladen werden soll, kann die entsprechende Option gewählt und den Radius um den Punkt herum gesetzt werden. Auf langsamen Geräten sollte nur ein kleiner Bereich ausgewählt werden. 

Alternativ kann mit dem "Zur Karte" direket zur Karte gewechselt werden, auf das Gebiet das bearbeitet werden soll gezoomt werden und dann Daten für das Gebiet geladen werden (siehe unten "Mit Vespucci OSM Daten bearbeiten").


## Mit Vespucci OSM Daten bearbeiten

Abhängig der Bildschirmgrösse und Alter des Gerätes können die Bearbeitungsfunktionen über Icons in der obersten Menuzeile, via ein Menu oben rechts, von der unteren Menuzeile (falls vorhanden) oder mittels der Menutaste zugänglich sein.

### OSM Daten herunterladen

Wähle entweder das Übertragungs-Icon ![](../images/menu_transfer.png) oder den "Transfer" Menueintrag Dies wird sieben Optionen zur Auswahl anzeigen:

* **Download current view** - download the area visible on the screen and replace any existing data *(requires network connectivity)*
* **Add current view to download** - download the area visible on the screen and merge it with existing data *(requires network connectivity)*
* **Download other location** - shows a form that allows you to enter coordinates, search for a location or enter coordinates directly, and then download an area around that location *(requires network connectivity)*
* **Upload data to OSM server** - upload edits to OpenStreetMap *(requires authentication)* *(requires network connectivity)*
* **Auto download** - download an area around the current location automatically *(requires network connectivity)* *(requires GPS)*
* **File...** - saving and loading OSM data to/from on device files.
* **Note/Bugs** - download (automatically and manually) OSM Notes and "Bugs" from QA tools (currently OSMOSE) *(requires network connectivity)*

The easiest way to open a map is to zoom and pan to the location you want to edit and then to select "Download current view". You can zoom by using gestures, the zoom buttons or the volume control buttons on the telephone.  Vespucci should download data for the area and center the map on your current location. No authentication is required for downloading data to your device.

### Bearbeiten

To avoid accidental edits Vespucci start in "locked" mode, a mode that only allows zooming and moving the map. Tap the ![Locked](../images/locked.png) icon to unlock the screen. A long press on the lock icon will enable "Tag editing only" mode which will not allow you to create new objects or edit the geometry of objects, this mode is indicated with a slightly different white lock icon.
 
By default, selectable nodes and ways have an orange area around them indicating roughly where you have to touch to select an object. If you try to select an object and Vespucci determines that the selection could mean multiple objects it will present a selection menu. Selected objects are highlighted in yellow.

In Gebieten in denen die OSM Daten sehr dicht sind ist es sinnvoll vor dem Bearbeiten weit hineinzuzoomen.

Vespucci has a good "undo/redo" system so don't be afraid of experimenting on your device, however please do not upload and save pure test data.

#### Auswählen / Abwählen

Touch an object to select and highlight it, a second touch on the same object opens the tag editor on the element. Touching the screen in an empty region will de-select. If you have selected an object and you need to select something else, simply touch the object in question, there is no need to de-select first. A double tap on an object will start [Multiselect mode](Multiselect.md).

#### Einen neuen Knoten oder Weg erstellen

Long press where you want the node to be or the way to start. You will see a black "cross hairs" symbol. Touching the same location again creates a new node, touching a location outside of the touch tolerance zone will add a way segment from the original position to the current position. 

Simply touch the screen where you want to add further nodes of the way. To finish, touch the final node twice. If the initial node is located on a way, the node will be inserted into the way automatically.

#### Einen Knoten oder Weg verschieben

Objects can be dragged/moved only when they are selected. If you select the large drag area in the preferences, you get a large area around the selected node that makes it easier to position the object. 

#### Die Geometrie eines Weges verbessern

If you zoom in far enough you will see a small "x" in the middle of way segments that are long enough. Dragging the "x" will create a node in the way at that location. Note: to avoid accidentally creating nodes, the touch tolerance for this operation is fairly small.

#### Kopiern, Ausschneiden & EInfügen

You can copy or cut selected nodes and ways, and then paste once or multiple times to a new location. Cutting will retain the osm id and version. To paste long press the location you want to paste to (you will see a cross hair marking the location). Then select "Paste" from the menu.

#### Effizient Adressen eintragen

Vespucci has an "add address tags" function that tries to make surveying addresses more efficient. It can be selected 

* after a long press: Vespucci will add a node at the location and make a best guess at the house number and add address tags that you have been lately been using. If the node is on a building outline it will automatically add a **entrance=yes"" tag to the node. The tag editor will open for the object in question and let you make any further changes.
* in the node/way selected modes: Vespucci will add address tags as above and start the tag editor.
* in the tag editor.

House number prediction typically requires at least two house numbers on each side of the road to be entered to work, the more numbers present in the data the better.

Consider using this with the "Auto-download" mode.  

#### Abbiegebeschränkungen eintragen

Vespucci has a fast way to add turn restrictions. Note: if you need to split a way for the restriction you need to do this before starting.

* select a way with a highway tag (turn restrictions can only be added to highways, if you need to do this for other ways, please use the generic "create relation" mode, if there are no possible "via" elements the menu item will also not display)
* select "Add restriction" from the menu
* select the "via" node or way (all possible "via" elements will have the selectable element highlighting)
* select the "to" way (it is possible to double back and set the "to" element to the "from" element, Vespucci will assume that you are adding an no_u_turn restriction)
* set the restriction type in the tag menu
 
### Vespucci in "locked" mode
 
When the red lock is displayed the following all non-editing actions are available. Additionally a long press on or near to an object will display the detail information screen if it is an OSM object.

### Die Bearbeitungen abspeichern

*(benötigt Netzwerkzugang)

Select the same button or menu item you did for the download and now select "Upload data to OSM server".

Vespucci supports OAuth authorization and the classical username and password method. OAuth is preferable since it avoids sending passwords in the clear.

New Vespucci installs will have OAuth enabled by default. On your first attempt to upload modified data, a page from the OSM website loads. After you have logged on (over an encrypted connection) you will be asked to authorize Vespucci to edit using your account. If you want to or need to authorize the OAuth access to your account before editing there is a corresponding item in the "Tools" menu.

If you want to save your work and do not have Internet access, you can save to a JOSM compatible .osm file and either upload later with Vespucci or with JOSM. 

#### Konfliktbehebung beim Upload

Vespucci has a simple conflict resolver. However if you suspect that there are major issues with your edits, export your changes to a .osc file ("Export" menu item in the "Transfer" menu) and fix and upload them with JOSM. See the detailed help on [conflict resolution](Conflict resolution.md).  

## GPS verwenden

You can use Vespucci to create a GPX track and display it on your device. Further you can display the current GPS position (set "Show location" in the GPS menu) and/or have the screen center around and follow the position (set "Follow GPS Position" in the GPS menu). 

If you have the later set, moving the screen manually or editing will cause the "follow GPS" mode to be disabled and the blue GPS arrow will change from an outline to a filled arrow. To quickly return to the "follow" mode, simply touch the arrow or re-check the option from the menu.

## Notizen und Fehler

Vespucci supports downloading, commenting and closing of OSM Notes (formerly OSM Bugs) and the equivalent functionality for "Bugs" produced by the [OSMOSE quality assurance tool](http://osmose.openstreetmap.fr/en/map/). Both have to either be downloaded explicitly or you can use the auto download facility to access the items in your immediate area. Once edited or closed, you can either upload the bug or Note immediately or upload all at once.

On the map the Notes and bugs are represented by a small bug icon ![](../images/bug_open.png), green ones are closed/resolved, blue ones have been created or edited by you, and yellow indicates that it is still active and hasn't been changed. 

## Vespucci individuell anpsasen

### Häufig geänderte Einstellungen

* Background layer
* Overlay layer. Adding an overlay may cause issues with older devices and such with limited memory. Default: none.
* Notes/Bugs display. Open Notes and bugs will be displayed as a red bug icon, closed ones the same in green. Default: off.
* Photo layer. Displays georeferenced photographs as red camera icons, if direction information is available the icon will be rotated. Default: off.
* Node icons. Default: off.
* Keep screen on. Default: off.
* Large node drag area. Moving nodes on a device with touch input is problematic since your fingers will obscure the current position on the display. Turning this on will provide a large area which can be used for off-centre dragging (selection and other operations still use the normal touch tolerance area). Default: off.

Erweiterte Einstellungen

* Enable split action bar. On recent phones the action bar will be split in a top and bottom part, with the bottom bar containing the buttons. This typically allows more buttons to be displayed, however does use more of the screen. Turning this off will move the buttons to the top bar. note: you need to restart Vespucci for the change to take effect.
* Always show context menu. When turned on every selection process will show the context menu, turned off the menu is displayed only when no unambiguous selection can be determined. Default: off (used to be on).
* Enable light theme. On modern devices this is turned on by default. While you can enable it for older Android versions the style is likely to be inconsistent.
* Show statistics. Will show some statistics for debugging, not really useful. Default: off (used to be on).  

## Fehler melden

If Vespucci crashes, or it detects an inconsistent state, you will be asked to send in the crash dump. Please do so if that happens, but please only once per specific situation. If you want to give further input or open an issue for a feature request or similar, please do so here: [Vespucci issue tracker](https://github.com/MarcusWolschon/osmeditor4android/issues). If you want to discuss something related to Vespucci, you can either start a discussion on the [Vespucci google group](https://groups.google.com/forum/#!forum/osmeditor4android) or on the [OpenStreetMap Android forum](http://forum.openstreetmap.org/viewforum.php?id=56)


