# Einführung in Vespucci

Vespucci ist ein Editor für OpenStreetMap, der die meisten Funktionen unterstützt, die in ähnlichen Programme auf normalen Rechnern vorhanden sind. Es ist erfolgreich auf Googles Android 2.3 bis 7.0 und verschiedene auf AOSP basierende Varianten getestet worden. Wichtig: während die Leistung von Handys und Tablets ihre stationären Konkurrenten auf vielen Gebieten eingeholt hat, muss, vor allem bei älteren Geräten, auch nicht vergessen gehen, dass sie eher wenig Hauptspeicher zur Verfügung haben und auch deutlich langsamer  bei bestimmten Operationen sein können. Deshalb sollte man beim Bearbeiten dies im Auge behalten und zum Beispiel die Größe der bearbeiteten Gebiete in vernünftige Größen halten.   

## Erstmaliger Gebrauch

Beim erstmaligen Starten zeigt Vespucci das "Herunterladen einer anderen Position"/"Bereich laden" Formular. Falls Koordinaten angezeigt werden und sofort heruntergeladen werden sollen, kann die entsprechende Option gewählt und der Radius um den Punkt herum gesetzt werden. Auf langsamen Geräten sollte nur ein kleiner Bereich ausgewählt werden. 

Alternativ kann mit dem "Zur Karte" direkt zur Karte gewechselt werden, auf das Gebiet das bearbeitet werden soll gezoomt werden und dann Daten für das Gebiet geladen werden (siehe unten "Mit Vespucci OSM Daten bearbeiten").


## Mit Vespucci OSM Daten bearbeiten

Abhängig der Bildschirmgrösse und Alter des Gerätes können die Bearbeitungsfunktionen über Icons in der obersten Menüzeile, via ein Menü oben rechts, von der unteren Menüzeile (falls vorhanden) oder mittels der Menütaste zugänglich sein.

### OSM Daten herunterladen

Wähle entweder das Übertragungs-Icon ![Transfer](../images/menu_transfer.png) oder den "Transfer" Menüeintrag. Dies wird sieben Optionen zur Auswahl anzeigen:

* **Aktuelle Ansicht herunterladen** - den aktuell sichtbaren Bildschirmbereich herunterladen und allfällig vorhandene Daten ersetzen *(benötigt Netzzugang)*
* **Diese Ansicht zu dn Daten dazu laden** - den aktuell sichtbaren Bildschirmbereich auf dem Schirm herunterladen und mit vorhandene Daten  zusammenführen *(benötigt Netzzugang)*
* **Herunterladen einer anderen Position** - zeigt ein Formular an, dass es erlaubt nach einem Ort zu suchen, Koordinaten einzugeben. oder direkt um die aktuelle Position einen Bereich herunterzuladen  *(benötigt Netzzugang)*
* **Daten zum OSM-Server hochladen** - lädt die Änderungen zum OSM-Server hoch *(Konto benötigt)*  *(benötigt Netzzugang)*
* **Automatischer Download** - lädt automatisch einen Bereich um die aktuelle Position herunter  *(benötigt Netzzugang)* *(benötigt GPS)*
* **Datei..** - Speichern und Laden von OSM Daten zu Dateien auf dem Gerä.
* **Notizen/Fehler...** - herunterlanden (automatisch und manuell) von OSM Notizen und "Fehlern" von QA Werkzeugen (aktuell OSMOSE) *(benötigt Netzzugang)*

The easiest way to download data to the device is to zoom and pan to the location you want to edit and then to select "Download current view". You can zoom by using gestures, the zoom buttons or the volume control buttons on the device.  Vespucci should then download data for the current view. No authentication is required for downloading data to your device.

### Bearbeiten

#### Sperren, entsperren, "nur Eigenschaften bearbeiten" Modus

Um versehentliche Änderungen zu verhindern startet Vespucci im "gesperrten" Modus, einen Modus der nur das Zoomen und Verschieben der Karte erlaubt. Tippe auf das ![Schloss](../images/locked.png) Icon um den Schirm zu entsperren.  

Ein langer Druck auf das Schloss schaltet in einen Modus um in dem nur die Eigenschaften der Elemente geändert werden kann, aber keine neuen Objekte erstellt oder die Geometrien geändert werden können. Dieser Modus wird mit einem weissen Schloss mit kleinem "T" angezeigt. 

#### Einfacher Klick, Doppelklick und langer Klick

In der Standardeinstellung wird um auswählbare Punkte und Wege ein oranger Bereich angezeigt, der angibt in welchen Bereich man um den Bildschirm tippen kann um ein Objekt anzuwählen. Die drei Möglichkeiten sind:

* Single tap: Selects object. 
    * An isolated node/way is highlighted immediately. 
    * However, if you try to select an object and Vespucci determines that the selection could mean multiple objects it will present a selection menu, enabling you to choose the object you wish to select. 
    * Selected objects are highlighted in yellow. 
    * For further information see [Node selected](../en/Node%20selected.md) and [Way selected](../en/Way%20selected.md).
* Double tap: Start [Multiselect mode](../en/Multiselect.md)
* Long press: Creates a "crosshair", enabling you to add nodes, see below and [Creating new objects](../en/Creating new objects.md)

In Gebieten in denen die OSM Daten sehr dicht sind ist es sinnvoll vor dem Bearbeiten weit hineinzuzoomen.

Vespucci hat eine gute Unterstützung von "Undo" und "Redo" deshalb kann man angstfrei auf seinem Gerät experimentieren, bitte aber keine reinen Testdaten auf den OSM Server speichern.

#### Selecting / De-selecting (single tap and "selection menu")

Touch an object to select and highlight it. Touching the screen in an empty region will de-select. If you have selected an object and you need to select something else, simply touch the object in question, there is no need to de-select first. A double tap on an object will start [Multiselect mode](../en/Multiselect.md).

Note that if you try to select an object and Vespucci determines that the selection could mean multiple objects (such as a node on a way or other overlapping objects) it will present a selection menu: Tap the object you wish to select and the object is selected. 

Selected objects are indicated through a thin yellow border. The yellow border may be hard to spot, depending on map background and zoom factor. Once a selection has been made, you will see a notification confirming the selection.

You can also use menu items: For further information see [Node selected](../en/Node%20selected.md) and [Way selected](../en/Way%20selected.md).

#### Selected objects: Editing tags

A second touch on the selected object opens the tag editor and you can edit the tags associated with the object.

Note that for overlapping objects (such as a node on a way) the selection menu comes back up for a second time. Selecting the same object brings up the tag editor; selecting another object simply selects the other object.

#### Selected objects: Moving a Node or Way

Once you have selected an object, it can be moved. Note that objects can be dragged/moved only when they are selected. Simply drag near (i.e. within the tolerance zone of) the selected object to move it. If you select the large drag area in the preferences, you get a large area around the selected node that makes it easier to position the object. 

#### Adding a new Node/Point or Way (long press)

Long press where you want the node to be or the way to start. You will see a black "crosshair" symbol. 
* If you want to create a new node (not connected to an object), click away from existing objects.
* If you want to extend a way, click within the "tolerance zone" of the way (or a node on the way). The tolerance zone is indicated by the areas around a node or way.

Once you can see the crosshair symbol, you have these options:

* Touch in the same place.
    * If the crosshair is not near a node, touching the same location again creates a new node. If you are near a way (but not near a node), the new node will be on the way (and connected to the way).
    * If the crosshair is near a node (i.e. within the tolerance zone of the node), touching the same location just selects the node (and the tag editor opens. No new node is created. The action is the same as the selection above.
* Touch another place. Touching another location (outside of the tolerance zone of the crosshair) adds a way segment from the original position to the current position. If the crosshair was near a way or node, the new segment will be connected to that node or way.

Simply touch the screen where you want to add further nodes of the way. To finish, touch the final node twice. If the final node is  located on a way or node, the segment will be connected to the way or node automatically. 

You can also use a menu item: See [Creating new objects](../en/Creating new objects.md) for more information.

#### Die Geometrie eines Weges verbessern

Zoomt man genügend nah an ein Wegsegment wird ein kleines "x" sichtbar. Zieht man dran wird ein Knoten im Weg erstellt. Hinweis: um das versehentliche Erstellen solcher Punkte zu verhindern ist der empfindliche Bereich um das "x" ziemlich klein.

#### Kopieren, Ausschneiden & Einfügen

Ausgewählte Knoten und Wege können kopiert oder ausgeschnitten werden, und dann ein- oder mehrmals wieder eingefügt werden. Ausschneiden erhält sowohl die OSM Id wie auch die Version. Ein langer Druck markiert die Position an der eingefügt werden soll, um die Aktion auszulösen danach "Einfügen" aus dem Menü auswählen.

#### Effizient Adressen eintragen

Vespucci hat eine "Adresseigenschaften hinzufügen" Funktion, die versucht Adresserfassung schneller und effizienter zu machen. Die Funktion kann ausgewählt werden

* nach einem langen Druck: Vespucci erstellt dann einen Knoten an der markierten Stelle, versucht eine wahrscheinliche Hausnummer vorherzusagen  und schlägt weitere aktuell verwendete Adresswerte vor.  Falls der Punkt auf einem Gebäudeumriss liegt wird automatisch ein Eingang erstellt. Der Eigenschaftseditor wird dann gestartet um allfällige Korrekturen und andere Änderungen zu ermöglichen. 
*  in den "Knoten/Weg ausgewählt" Modi:: Vespucci fügt Eigenschaften wie oben beschrieben hinzu und startet den Eigenschaftseditor.
* im Eigenschaftseditor.

Die Hausnummernvorhersage benötigt typischerweise mindestens die Eingabe von je 2 Hausnummern auf jede Seite der Strasse, je mehr Nummern in den Daten vorhanden sind desto besser funktioniert die Vorhersage. 

Es ist sinnvoll dies mit dem "Automatischen Download" zu verwenden.  

#### Abbiegebeschränkungen eintragen

Vespucci has a fast way to add turn restrictions, if necessary it will split ways automatically and, if necessary, ask you to re-select elements. 

* select a way with a highway tag (turn restrictions can only be added to highways, if you need to do this for other ways, please use the generic "create relation" mode)
* select "Add restriction" from the menu
* select the "via" node or way (only possible "via" elements will have the touch area shown)
* select the "to" way (it is possible to double back and set the "to" element to the "from" element, Vespucci will assume that you are adding an no_u_turn restriction)
* set the restriction type in the property editor

### Vespucci im "gesperrten" Modus

Während dem Vespucci "gesperrt" ist sind alle Funktionen verfügbar die nicht die Geometrie oder Eigenschaften von Objekten verändern. Des weiteren kann nach einem langen Druck auf dem Schirm die Eigenschaften von Objekten in der Nähe angezeigt werden.

### Die Bearbeitungen abspeichern

*(benötigt Netzwerkzugang)

Wähle den gleichen Icon oder Menüeintrag wie für das Herunterladen und wähle jetzt "Daten zum OSM-Server hochladen".

Vespucci unterstützt sowohl OAuth Autorisierung (Standardeinstellung) wie auch Benutzername und Passwort. Wo möglich sollte OAuth verwendet werden um die Übertragung von Passworten zu vermeiden.  

Neue Vespucci Installationen haben OAuth voreingestellt. Beim ersten Versuch Daten auf den OSM Server zu speichern wird eine Seite der OSM Website geladen (über eine verschlüsselte Verbindung). Nach der erfolgreichen Authentisierung mit Username und Passwort muss den Zugriff mit OAuth zugelassen werden. Falls dieser Vorgang vor dem ersten Hochladen ausgelöst werden soll, gibt es einen entsprechende Auswahlmöglichkeit im "Werkzeuge" Menü.

Sollen die Änderungen gespeichert werden und es ist kein Internetzugang verfügbar, können sie in einer JOSM kompatible .osm Datei gespeichert werden und später entweder mit Vespucci oder mit JOSM hochgeladen werden. 

#### Konfliktbehebung beim Upload

Vespucci hat einen einfachen Konfliktbehebungsmechanismus eingebaut. Sind grössere Probleme mit den Änderungen zu erwarten, empfehlen wir sie in ein .osc Datei zu speichern ("Transfer" Menü, "Datei...,"  "Änderungen exportieren") und die Konflikte dann mit JOSM zu beheben. Für Details siehe den [conflict resolution](../en/Conflict resolution.md) Hilfetext.  

## GPS verwenden

Vespucci kann auch einen GPX Track erstellen und auf dem Schirm anzeigen. Weiter kann auch die aktuelle GPS-Position ("GPS-Position anzeigen" im Menü) angezeigt werden und/oder der Bildschirm darauf zentriert und nachgeführt werden ("GPS-Position folgen").   

If you have the latter set, moving the screen manually or editing will cause the "follow GPS" mode to be disabled and the blue GPS arrow will change from an outline to a filled arrow. To quickly return to the "follow" mode, simply touch the arrow or re-check the option from the menu.

## Notizen und Fehler

Vespucci unterstützt das Herunterladen, Kommentieren und Schliessen von OSM Notizen (vormals OSM Bugs) und die entsprechende Funktionalität für "Fehler" die vom [OSMOSE Qualitätssicherungswerkzeug](http://osmose.openstreetmap.fr/en/map/) gemeldet werden. Beide müssen entweder explizit heruntergeladen werden oder die Notizen und Fehler in der Nähe können automatisch geladen werden. Geänderte oder geschlossene Notizen und Fehler können entweder sofort hochgeladen werden oder gespeichert und alle zusammen später hochgeladen werden.

Auf der Karte werden die Notizen und Fehler werden mit einem kleinen Käfer Icon  ![Bug](../images/bug_open.png) angezeigt, grüne sind behoben, blaue sind neu erstellt oder geändert, und Gelb zeigt an, dass die Notiz respektive der Fehler noch unverändert aktiv ist. 

In der Anzeige von OSMOSE Fehler wird jeweils für die betroffenen Objekte ein blau hervorgehobener Link angezeigt, wählt man den Link an, wird das Objekt ausgewählt, der Bildschirm darauf zentriert und, falls nötig, das entsprechende Gebiet heruntergeladen. 

### Filtering

Besides globally enabling the notes and bugs display you can set a coarse grain display filter to reduce clutter. In the "Advanced preferences" you can individually select:

<item>Notes</item>
<item>Osmose Fehler</item>
<item>Osmose Warnung</item>
<item>Osmose nebensächliche Warnung</item>


## Vespucci individuell anpassen

### Häufig geänderte Einstellungen

* Background layer
* Overlay layer. Adding an overlay may cause issues with older devices and such with limited memory. Default: none.
* Notes/Bugs display. Open Notes and bugs will be displayed as a yellow bug icon, closed ones the same in green. Default: on.
* Photo layer. Displays georeferenced photographs as red camera icons, if direction information is available the icon will be rotated. Default: off.
* Node icons. Default: on.
* Keep screen on. Default: off.
* Large node drag area. Moving nodes on a device with touch input is problematic since your fingers will obscure the current position on the display. Turning this on will provide a large area which can be used for off-center dragging (selection and other operations still use the normal touch tolerance area). Default: off.

Erweiterte Einstellungen

* Always show context menu. When turned on every selection process will show the context menu, turned off the menu is displayed only when no unambiguous selection can be determined. Default: off (used to be on).
* Enable light theme. On modern devices this is turned on by default. While you can enable it for older Android versions the style is likely to be inconsistent.
* Show statistics. Will show some statistics for debugging, not really useful. Default: off (used to be on).  

## Fehler melden

Falls Vespucci abstürzt oder einen inkonsistenten Zustand entdeckt wird, erscheint eine Aufforderung eine Fehlermeldung ein zuschicken. Bitte komme der Aufforderung nach, aber nur einmal per spezifischen Ereignis.  Falls du mehr Input geben willst oder einen Verbesserungsvorschlag hast, erstelle bitte hier: [Vespucci Issue Tracker](https://github.com/MarcusWolschon/osmeditor4android/issues) einen neuen Eintrag. Falls du zu Vespucci eine Diskussion beginnen willst, kannst du dies entweder auf der [Vespucci google group](https://groups.google.com/forum/#!forum/osmeditor4android) oder dem [OpenStreetMap Android Forum](http://forum.openstreetmap.org/viewforum.php?id=56) machen.


