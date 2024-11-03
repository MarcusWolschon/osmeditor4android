_Before we start: most screens have links in the menu to the on-device help system giving you direct access to information relevant for the current context, you can easily navigate back to this text too. If you have a larger device, for example a tablet, you can open the help system in a separate split window.  All the help texts and more (FAQs, tutorials) can be found on the [Vespucci documentation site](https://vespucci.io/) too. You can further start the help viewer directly on devices that support short cuts with a long press on the app icon and selecting "Help"_

# Einführung in Vespucci

Vespucci is a full featured OpenStreetMap editor that supports most operations that desktop editors provide. It has been tested successfully on Google's Android 2.3 to 14.0 (versions prior to 4.1 are no longer supported) and various AOSP based variants. A word of caution: while mobile device capabilities have caught up with their desktop rivals, particularly older devices have very limited memory available and tend to be rather slow. You should take this in to account when using Vespucci and keep, for example, the areas you are editing to a reasonable size.

## Mit Vespucci OSM-Daten bearbeiten

Abhängig von Bildschirmgröße und Alter des Gerätes sind die Bearbeitungsfunktionen entweder über Icons in einer Leiste am Oberrand, über ein Dropdown-Menü rechts in dieser Leiste, über eine Menüleiste am Unterrand (falls vorhanden) oder über die Menütaste zugänglich.

<a id="download"></a>

### OSM-Daten herunterladen

Select either the transfer icon ![Transfer](../images/menu_transfer.png) or the "Transfer" menu item. This will display eleven options:

* **Upload data to OSM server...** - review and upload changes to OpenStreetMap *(requires authentication)* *(requires network connectivity)*
* **Review changes...** - review current changes
* **Download current view** - download the area visible on the screen and merge it with existing data *(requires network connectivity or offline data source)*
* **Clear and download current view** - clear any data in memory, including pending changes, and then download the area visible on the screen *(requires network connectivity)*
* **Query Overpass...** - run a query against a Overpass API server *(requires network connectivity)*
* **Location based auto download** - download an area around the current geographic location automatically *(requires network connectivity or offline data)* *(requires GPS)*
* **Pan and zoom auto download** - download data for the currently displayed map area automatically *(requires network connectivity or offline data)* *(requires GPS)*
* **Update data** - re-download data for all areas and update what is in memory *(requires network connectivity)*
* **Clear data** - remove any OSM data in memory, including pending changes.
* **File...** - saving and loading OSM data to/from on device files.
* **Tasks...** - download (automatically and manually) OSM Notes and "Bugs" from QA tools (currently OSMOSE) *(requires network connectivity)*

Um Daten auf dem Gerät zu öffnen, ist es am einfachsten, mit Gesten den Bildschirm auf das gewünschte Gebiet zu zentrieren und dann im Menü "Aktuelle Ansicht herunterladen" anzuwählen. Der Zoom kann mit Gesten, den Zoom-Schaltflächen oder den Lautstärketasten bedient werden. Vespucci sollte dann das Gebiet herunterladen. Um Daten herunterzuladen, muss man nicht angemeldet sein.

In unlocked state any non-downloaded areas will be dimmed relative to the downloaded ones if you are zoomed in far enough to enable editing. This is to avoid inadvertently adding duplicate objects in areas that are not being displayed. In the locked state dimming is disabled, this behaviour can be changed in the [Advanced preferences](Advanced%20preferences.md) so that dimming is always active.

If you need to use a non-standard OSM API entry, or use [offline data](https://vespucci.io/tutorials/offline/) in _MapSplit_ format you can add or change entries via the _Configure..._ entry for the data layer in the layer control.

### Bearbeiten

<a id="lock"></a>

#### Sperren, entsperren, Modus wechseln

Um versehentliche Änderungen zu verhindern, startet Vespucci im "gesperrten" Modus, einem Modus, der nur das Zoomen und Verschieben der Karte erlaubt. Um den Schirm zu entsperren, auf das ![Schloss](../images/locked.png)-Icon tippen. 

A long press on the lock icon or the _Modes_ menu in the map display overflow menu will display a menu offering 4 options:

* **Normal** - the default editing mode, new objects can be added, existing ones edited, moved and removed. Simple white lock icon displayed.
* **Tag only** - selecting an existing object will start the Property Editor, new objects can be added via the green "+" button, or long press, but no other geometry operations are enabled. White lock icon with a "T" is displayed.
* **Address** - enables Address mode, a slightly simplified mode with specific actions available from the [Simple mode](../en/Simple%20actions.md) "+" button. White lock icon with an "A" is displayed.
* **Indoor** - enables Indoor mode, see [Indoor mode](#indoor). White lock icon with an "I" is displayed.
* **C-Mode** - enables C-Mode, only objects that have a warning flag set will be displayed, see [C-Mode](#c-mode). White lock icon with a "C" is displayed.

If you are using Vespucci on an Android device that supports short cuts (long press on the app icon) you can start directly to _Address_ and _Indoor_ mode.

#### Einfacher Klick, Doppelklick und langer Klick

In der Standardeinstellung wird um auswählbare Knoten und Wege ein oranger Bereich angezeigt, der angibt, in welchen Bereich man auf den Bildschirm tippen kann, um ein Objekt anzuwählen. Die drei Möglichkeiten sind:

* Einfacher Klick: Objekte auswählen. 
    * Ein einzelner Knoten/Weg wird sofort markiert. 
    * Soll ein Objekt ausgewählt werden und Vespucci stellt allerdings fest, dass die Auswahl mehrere Objekte betreffen kann, zeigt Vespucci ein Auswahlmenü an, aus dem das gewünschte Objekt gewählt werden kann. 
    * Ausgewählte Objekte werden in Gelb hervorgehoben. 
    * Für weitere Informationen siehe [Knoten ausgewählt](Node%20selected.md), [Weg ausgewählt](Way%20selected.md) und [Relation ausgewählt](Relation%20selected.md).
* Doppelklick: Startet den [Mehrfachauswahl-Modus](Multiselect.md)
* Klicken und halten: Erstellt ein "Fadenkreuz" und ermöglicht das Hinzufügen von Knoten, siehe unten und [Erstellen neuer Objekte](Creating%20new%20objects.md). Dies ist nur möglich, wenn der "Einfache Modus" deaktiviert ist.

In einem Gebiet, in dem die OSM-Daten sehr dicht sind, empfiehlt es sich, zum Bearbeiten eine hohe Vergrößerungsstufe zu wählen.

Vespucci hat eine gute Unterstützung von "Undo" und "Redo", weshalb man ohne Bedenken auf seinem Gerät experimentieren kann, bitte aber keine reinen Testdaten auf OSM-Server speichern.

#### Auswählen / Abwählen (einfacher Klick und "Auswahlmenü")

Um ein Objekt anzuwählen und hervorzuheben, tippt man darauf. Ist ein Objekt markiert, kann es durch Tippen in einen leeren Bereich abgewählt werden. Wurde ein Objekt markiert und soll ein anderes ausgewählt werden, tippt man einfach auf das in Frage kommende Element. Es ist nicht nötig, zuerst das aktuell markierte abzuwählen. Ein "Doppeltipp" startet den [Mehrfachauswahl-Modus](../en/Multiselect.md).

Hinweis: falls Vespucci nicht eindeutig feststellen kann, welches Objekt markiert werden soll (zum Beispiel ein Punkt auf einem Weg oder andere sich überlappende Objekte), wird ein Auswahlmenü angezeigt. Dort den Eintrag für das gesuchte Objekt auswählen, worauf es markiert wird. 

Ausgewählte Objekte werden durch einen dünnen, gelben Rand hervorgehoben. Nach der Auswahl erscheint eine Meldung mit einer kurzen Beschreibung des Objekts, die die Auswahl bestätigt.

Sobald die Auswahl stattgefunden hat, werden, entweder als Schaltfläche oder als Menüeintrag, die verfügbaren Operationen für das ausgewählte Objekt angezeigt: Weitere Informationen hierzu unter [Knoten ausgewählt](../en/Node%20selected.md), [Weg ausgewählt](../en/Way%20selected.md) und [Relation ausgewählt](../en/Relation%20selected.md).

#### Ausgewählte Objekte: Tags (Eigenschaften) bearbeiten

Ein zweites Antippen des Objekts öffnet den "Tag-Editor", in dem die mit dem Objekt verknüpften Eigenschaften bearbeitet werden können.

Hinweis: für sich überlappende Objekte (z. B. ein Punkt auf einem Weg) erscheint das Auswahlmenü ein weiteres Mal. Wählt man das schon ausgewählte Objekt nochmals, wird der Tag-Editor gestartet, wählt man ein anderes Objekt, so wird dieses ausgewählt.

#### Ausgewählte Objekte: Einen Knoten oder Weg verschieben

Ist ein Objekt ausgewählt, kann es verschoben werden. Hinweis: Objekte können erst verschoben werden, nachdem sie ausgewählt wurden. Durch Ziehen an der Umgebung (d. h. innerhalb der Toleranzzone) des Objekts, kann es dann bewegt werden. In den Einstellungen lässt sich ein größerer Bereich rund um den markierten Knoten einschalten, der die Verschiebung eines Objekts erleichtert. 

#### Hinzufügen eines Knotens/Punkts oder Weges 

Beim ersten Start der App ist der "Einfache Modus" aktiviert. Dies kann im Hauptmenü geändert werden, indem die entsprechende Auswahlbox deaktiviert wird.

##### Einfacher Modus

Drücken des großen grünen Knopfes auf dem Bildschirm zeigt ein Menü an. Nach der Auswahl eines Befehls wird man aufgefordert, auf dem Bildschirm die Position anzutippen, an der das Objekt erstellt werden soll. Verschieben und Zoomen funktionieren weiterhin, falls der Bildschirmausschnitt angepasst werden muss. 

See [Creating new objects in simple actions mode](Simple%20actions.md) for more information. Simple mode os the default for new installs.

##### Erweiterter Modus (Tippen und halten)
 
Durch langes Drücken an der Stelle, wo ein Knoten oder Weg erstellt werden soll, wird ein schwarzes "Fadenkreuz" angezeigt.
* Wenn man einen neuen (nicht mit einem Objekt verbundenen) Knoten erstellen will, klickt man neben bereits vorhandene Objekte.
* Soll ein vorhandener Weg erweitert werden, klickt man in die Toleranzzone des Weges (oder eines Knotens des Weges). Die Toleranzzone ist der Bereich um einen Knoten oder Weg herum.

Sobald das Positionskreuz erscheint, gibt es die folgenden Möglichkeiten:

* Nochmaliges Berühren an der gleichen Stelle.
    * Ist das Positionskreuz nicht in der Nähe eines Punktes, erstellt das Berühren einen neuen Punkt. Falls die Position in der Nähe eines Weges (aber nicht in der Nähe eines Punktes) ist wird der neue Punkt als Teil des Weges erstellt.
    * Ist das Positionskreuz in der Nähe eines Punktes, so wird kein neuer Punkt erstellt, sondern nur der Eigenschaftseditor gestartet.
* Nochmaliges Berühren an einer anderen Stelle. Erstellt einen Punkt wie oben beschrieben und einen Wegabschnitt zu der neuen Position.

Einfach den Bildschirm dort antippen, wo Wegpunkte ergänzt werden sollen. Den Endpunkt doppelt antippen, um den Weg fertigzustellen. Falls der Endpunkt auf einem anderen Weg oder Punkt liegt, wird er automatisch in diesen integriert. 

Man kann auch einen Menüpunkt dafür verwenden: Weitere Informationen dazu unter [Erstellen neuer Objekte](Creating%20new%20objects.md).

#### Flächen hinzufügen

Aktuell hat OpenStreetMap keinen eigenen Objekttyp für Flächen im Gegensatz zu anderen Geo-Datensystemen. Der Online-Editor "iD" versucht, aus den zugrundeliegenden OSM-Elementen eine Abstraktion der Flächen zu erstellen, was in bestimmten Fällen gut, in anderen nicht richtig funktioniert. Vespucci versucht das in der vorliegenden Version nicht, deshalb hier einige Informationen dazu wie in OSM Flächen abgebildet werden:

* _geschlossene Wege (*Polygone")_: die einfachste und häufigste Flächenvariante sind Wege, die einen gemeinsamen ersten und letzten Knoten haben, der einen geschlossenen "Ring" bildet (zum Beispiel sind die meisten Gebäude von diesem Typ). Diese sind in Vespucci sehr einfach zu erstellen, indem man einfach eine Verbindung zum ersten Knotenpunkt herstellt, wenn man mit dem Zeichnen der Fläche fertig ist. Hinweis: Die Interpretation des geschlossenen Weges hängt vom Tag ab: Wenn ein geschlossener Weg beispielsweise als Gebäude gekennzeichnet ist, wird er als Fläche interpretiert, wenn er als Kreisverkehr gekennzeichnet ist, nicht. In einigen Situationen, in denen beide Interpretationen gültig sein können, kann ein "area" Tag die vorgesehene Verwendung klären.
* _Multipolygone_: Einige Flächen bestehen aus mehreren Teilstücken, Löchern und Ringen, die nicht mit nur einem Weg dargestellt werden können. OSM verwendet eine spezielle Art von Relation (unser Allzweckobjekt, das Beziehungen zwischen Elementen modellieren kann), um dies zu umgehen, ein Multipolygon. Ein Multipolygon kann mehrere "äußere" Ringe und mehrere "innere" Ringe haben. Jeder Ring kann entweder ein geschlossener Weg sein, wie oben beschrieben, oder mehrere individuelle Wege, die gemeinsame Endknoten haben. Während große Multipolygone mit jedem Werkzeug schwer zu handhaben sind, lassen sich kleine in Vespucci problemlos erstellen. 
* _Küstenlinien_: für sehr große Objekte, Kontinente und Inseln, funktioniert selbst das Multipolygon-Modell nicht zufriedenstellend. Für Küstenwege natural=coastline nehmen wir eine richtungsabhängige Semantik an: das Land ist auf der linken Seite des Weges, das Wasser auf der rechten Seite. Ein Nebeneffekt davon ist, dass man im Allgemeinen die Richtung eines Weges mit Küstenlinien-Tagging nicht umkehren sollte. Weitere Informationen hierzu im [OSM Wiki](http://wiki.openstreetmap.org/wiki/Tag:natural%3Dcoastline).

#### Die Geometrie eines Weges verbessern

Zoomt man genügend nah an ein ausgewähltes Wegsegment, wird ein kleines "x" sichtbar. Zieht man daran, wird ein Knoten im Weg erstellt. Hinweis: um das versehentliche Erstellen solcher Punkte zu verhindern, ist der empfindliche Bereich um das "x" ziemlich klein.

#### Kopieren, Ausschneiden & Einfügen

You can copy selected nodes and ways, and then paste once or multiple times to a new location. Cutting will retain the osm id and version, thus can only be pasted once. To paste long press the location you want to paste to (you will see a cross hair marking the location). Then select "Paste" from the menu.

#### Effizient Adressen eintragen

Vespucci unterstützt Funktionen, die die Erfassung von Adressen effizienter machen, indem sie Hausnummern vorhersagen (linke und rechte Straßenseite getrennt) und automatisch _addr:street_ oder _addr:place_ Tags auf Grundlage vom zuletzt verwendeten Wert und der Nähe hinzufügen. Im besten Fall ermöglicht dies das Hinzufügen einer Adresse, ohne dass überhaupt etwas eingegeben werden muss.   

Das Hinzufügen der Tags kann durch Drücken von ![Adresse](../images/address.png) ausgelöst werden: 

* Nach langem Drücken (nur im nicht-einfachen Modus): Vespucci fügt einen Knoten an diesem Ort hinzu und schätzt die Hausnummer und fügt die Adress-Tags hinzu, die Sie in letzter Zeit verwendet haben. Befindet sich der Knoten auf einem Gebäudeumriss, wird dem Knoten automatisch ein "entrance=yes"-Tag hinzugefügt. Der Tag-Editor wird für das betreffende Objekt geöffnet, und Sie können alle weiteren notwendigen Änderungen vornehmen.
* In den Modi Knoten/Weg ausgewählt: Vespucci fügt Adress-Tags wie oben beschrieben hinzu und startet den Tag-Editor.
* im Eigenschaftseditor.

Um einzelne Adressknoten im voreingestellten "Einfachen Modus" direkt hinzuzufügen, wechseln Sie in den Bearbeitungsmodus "Adresse" (drücken Sie lange auf die Schlosstaste), "Adressknoten hinzufügen" fügt dann an der Position einen Adressknoten und, wenn dieser sich auf einer Gebäudekontur befindet, auch ein Tag für den Eingang hinzu, wie oben beschrieben.

Die Hausnummernvorhersage benötigt typischerweise mindestens die Eingabe von je 2 Hausnummern auf jeder Seite der Straße, je mehr Nummern in den Daten vorhanden sind desto besser funktioniert die Vorhersage. 

Diese Funktion kann mit einem der [Auto-Download](#download) Modi verwendet werden.  

#### Abbiegebeschränkungen eintragen

Vespucci erlaubt es schnell Abbiegebeschränkungen hinzuzufügen. Falls dazu Wege aufgetrennt werden müssen, geschieht dies automatisch und gegebenenfalls wird der Benutzer aufgefordert, die Elemente neu auszuwählen. 

* einen Weg mit einem Straßen-Tag ("highway") auswählen (Abbiegebeschränkungen können nur für solche Wege erstellt werden, für andere Wege kann dies mit der allgemeineren Funktion "Relation erstellen" gemacht werden)
* im Menü "Abbiegebeschränkung hinzufügen" wählen
* den "via"-Knoten oder -Weg auswählen (es werden nur die auswählbaren "via"-Elemente mit dem Toleranzbereich angezeigt) 
* den Zielweg ("to") auswählen (es ist auch möglich den ursprünglichen "from" Weg auszuwählen. Vespucci nimmt dann an, dass eine "no_u_turn" Beschränkung erstellt werden soll)
* den Typ der Abbiegebeschränkung festlegen

### Vespucci im "gesperrten" Modus

Während Vespucci "gesperrt" ist, sind alle Funktionen verfügbar, die nicht die Geometrie oder Eigenschaften von Objekten verändern. Zusätzlich können nach einem langen Druck auf oder neben ein Objekt dessen Detailinformationen angezeigt werden, wenn es sich um ein OSM-Objekt handelt.

### Die Bearbeitungen abspeichern

*(benötigt Netzwerkzugang)

Über denselben Knopf oder Menüeintrag, der für das Herunterladen verwendet wurde, kann "Daten zum OSM-Server hochladen" ausgewählt werden.

Vespucci supports OAuth 2, OAuth 1.0a authorization and the classical username and password method. Since July 1st 2024 the standard OpenStreetMap API only supports OAuth 2 and other methods are only available on private installations of the API or other projects that have repurposed OSM software.  

Authorizing Vespucci to access your account on your behalf requires you to one time login with your display name and password. If your Vespucci install isn't authorized when you attempt to upload modified data you will be asked to login to the OSM website (over an encrypted connection). After you have logged on you will be asked to authorize Vespucci to edit using your account. If you want to or need to authorize the OAuth access to your account before editing there is a corresponding item in the "Tools" menu.

Sollen die Änderungen gespeichert werden und es ist kein Internetzugang verfügbar, können sie in einer mit JOSM kompatiblen .osm-Datei gespeichert werden und später entweder mit Vespucci oder mit JOSM hochgeladen werden. 

#### Konfliktbehebung beim Upload

Vespucci hat einen einfachen Konfliktbehebungsmechanismus eingebaut. Sind größere Probleme mit den Änderungen zu erwarten, empfehlen wir, sie in eine .osc-Datei zu speichern ("Transfer" Menü, "Datei...,"  "Änderungen exportieren") und die Konflikte dann mit JOSM zu beheben. Für Details siehe Hilfetext zur [Konfliktauflösung](../en/Conflict%20resolution.md).  

### Nearby point-of-interest display

A nearby point-of-interest display can be shown by pulling the handle in the middle and top of the bottom menu bar up. 

More information on this and other available functionality on the main display can be found here [Main map display](Main%20map%display.md).

## Nutzung von GPS und GPX-Tracks

Mit Standardeinstellungen wird Vespucci versuchen, das GPS (und andere satellitenbasierte Navigstionssysteme) zu aktivieren, und, falls dies nicht möglich ist, auf die Positionsbestimmung über den sogenannten "Netzwerkstandort" zurückgreifen. Dieses Verhalten lässt vermuten, dass Sie für den normalen Gebrauch Ihr Android-Gerät selbst so konfiguriert haben, dass es nur GPX-generierte Standorte verwenden soll (um Tracking zu vermeiden), das heißt, Sie haben die euphemistisch mit "Verbesserte Standortgenauigkeit" bezeichnete Option ausgeschaltet. Wenn Sie die Option aktivieren möchten aber Vespucci daran hindern wollen, auf den "Netzwerkstandort" zurückzugreifen, sollten Sie die dazugehörige Option in den [Erweiterten Einstellungen](Advanced%20preferences.md) ausschalten. 

Touching the ![GPS](../images/menu_gps.png) button (normally on the left hand side of the map display) will center the screen on the current position and as you move the map display will be panned to maintain this.  Moving the screen manually or editing will cause the "follow GPS" mode to be disabled and the blue GPS arrow will change from an outline to a filled arrow. To quickly return to the "follow" mode, simply touch GPS button or re-check the equivalent menu option. If the device doesn't have a current location the location marker/arrow will be displayed in black, if a current location is available the marker will be blue.

Um einen GPX-Track aufzuzeichnen und auf Ihrem Gerät anzuzeigen, wählen Sie "GPX-Track starten" im Menü ![GPS](../images/menu_gps.png). Dies fügt der Anzeige einen Layer mit dem aktuell aufgezeichneten Track hinzu. Sie können den Track über den Eintrag in der [Ebenenkontrolle](Main%20map%20display.md) hochladen und exportieren. Weitere Ebenen können aus lokalen GPX-Dateien und von heruntergeladenen Tracks der OSM-API hinzugefügt werden.

Hinweis: Vespucci zeichnet standardmäßig keine Höhendaten mit Ihrem GPX-Track auf, dies ist auf einige Android-spezifische Probleme zurückzuführen. Um die Höhenaufzeichnung zu aktivieren, installieren Sie entweder ein Gravitationsmodell oder, einfacher, gehen Sie zu den [Erweiterten Voreinstellungen](Advanced%20preferences.md) und konfigurieren Sie den NMEA-Eingang.

### How to export a GPX track?

Open the layer menu, then click the 3-dots menu next to "GPX recording", then select **Export GPX track...**. Choose in which folder to export the track, then give it a name suffixed with `.gpx` (example: MyTrack.gpx).

## Notes, Bugs and Todos

Vespucci unterstützt das Herunterladen, Kommentieren und Schließen von OSM-Notizen (vormals OSM Bugs) und die entsprechende Funktionalität für "Fehler" die vom [OSMOSE Qualitätssicherungswerkzeug](http://osmose.openstreetmap.fr/en/map/) gemeldet werden. Beide müssen entweder explizit heruntergeladen werden oder die Notizen und Fehler in der Nähe können automatisch geladen werden. Geänderte oder geschlossene Notizen und Fehler können entweder sofort hochgeladen werden oder gespeichert und alle zusammen später hochgeladen werden. 

Further we support "Todos" that can either be created from OSM elements, from a GeoJSON layer, or externally to Vespucci. These provide a convenient way to keep track of work that you want to complete. 

On the map the Notes and bugs are represented by a small bug icon ![Bug](../images/bug_open.png), green ones are closed/resolved, blue ones have been created or edited by you, and yellow indicates that it is still active and hasn't been changed. Todos use a yellow checkbox icon.

The OSMOSE bug and Todos display will provide a link to the affected element in blue (in the case of Todos only if an OSM element is associated with it), touching the link will select the object, center the screen on it and down load the area beforehand if necessary. 

### Anzeigefilter

Zusätzlich zum Freischalten der Notiz- und Fehleranzeige kann eine grobe Auswahl der angezeigten Objekte eingestellt werden. In den "Erweiterten Einstellungen" kann folgendes ausgewählt werden:   

* Notes
* Osmose error
* Osmose warning
* Osmose minor issue
* Maproulette
* Todo

<a id="indoor"></a>

## Innenraum-Modus

In Innenräumen Daten zu erfassen ist anspruchsvoll aufgrund der grossen Anzahl von Objekten, die sich häufig überlappen. Vespucci hat einen speziellen Innenraum-Modus, der es ermöglicht, alle Objekte, die nicht auf der gleichen Etage sind, auszufiltern, und der bei neu erstellten Objekten automatisch die richtige Etage in den Objekteigenschaften einträgt. 

In den Modus kann durch einen langen Druck auf das Schlosssymbol gewechselt werden, siehe auch [Sperren, entsperren, Modus wechseln](#lock).

<a id="c-mode"></a>

## C-Modus

Im C-Modus werden nur Objekte angezeigt, die ein Problem haben, was das Finden von Objekten, die spezifische Probleme haben oder zu  konfigurierbaren Qualitätsprüfungen passen, erleichtert. Wird ein solches Objekt angewählt, startet der Eigenschaftseditor und die am besten passende Vorlage wird automatisch angewandt.

In den Modus kann durch einen langen Druck auf das Schlosssymbol gewechselt werden, siehe auch [Sperren, entsperren, Modus wechseln](#lock).

### Prüfungen konfigurieren

All validations can be disabled/enabled in the "Validator settings/Enabled validations" in the [preferences](Preferences.md). 

The configuration for "Re-survey" entries allows you to set a time after which a tag combination should be re-surveyed. "Check" entries are tags that should be present on objects as determined by matching presets. Entries can be edited by clicking them, the green menu button allows adding of entries.

#### Einträge für Elemente die überprüft werden sollten

Überprüfungstest-Einträge haben die folgenden Eigenschaften:

* **Schlüssel** - Schlüssel des Tags.
* **Wert** - Wert des Tags, falls leer wird der Wert des Tags ignoriert..
* **Alter** - wie viele Tage nach der letzten Änderung des Objekts soll es wieder überprüft  werden. Existiert ein "check_date"-Eintrag wird dieses Datum verwendet, ansonsten das der letzten Änderung. Wird der Wert auf Null gesetzt wird das Alter ignoriert.
* **Regulärer Ausdruck** - falls ausgewählt wird angenommen, dass **Wert** ein JAVA regulärer Ausdruck ist.

"Schlüssel" und "Wert" werden mit den _existierenden_ Tags des Objektes verglichen.

Die _Annotations_-Gruppe in der Standard-Vorbelegung beinhaltet ein Element, das automatisch einen "_check_date_"-Tag mit dem aktuellen Datum hinzufügen wird.

#### Tests auf fehlende Tags

Prüfungen auf fehlende Tag-Einträge haben die folgenden zwei Eigenschaften:

* **Schlüssel** - Schlüssel, der gemäß der passenden Vorlage auf dem Objekt vorhanden sein sollte.
* **Optional anfordern** - Den Schlüssel auch dann anfordern, wenn er nur als optionaler Schlüssel in der passenden Vorlage enthalten ist.

Diese Prüfung funktioniert, indem zuerst die zugehörige Voreinstellung bestimmt und dann geprüft wird, ob **Schlüssel** ein gemäß den Voreinstellungen "empfohlener" Schlüssel für dieses Objekt ist. ****Optional erforderlich** erweitert die Prüfung auf Tags, die "optional" für das Objekt sind. Hinweis: aktuell verlinkte Vorlagen werden nicht geprüft.

## Filter

### Auf Tags basierende Filter

Der Filter kann vom Hauptmenü aus eingeschaltet werden, durch Tippen auf die Filter-Schaltfläche kann er dann bearbeitet werden. Mehr Informationen findet man unter [Tag-Filter](../en/Tag%20filter.md).

### Auf Vorlagen basierende Filter

Eine Alternative zu obigem. Objekte werden entweder durch Einzelvorlagen oder Vorlagengruppen gefiltert. Nach einem Tipp auf die Filter-Schaltfläche wird ein Dialog zur  Vorlagenauswahl angezeigt, ähnlich wie er an anderer Stelle in Vespucci benutzt wird. Einzelne Vorlagen können durch einen normalen Klick ausgewählt werden, Vorlagengruppen durch einen langen Klick (normaler Klick öffnet die Gruppe). Mehr Informationen unter [Vorlagenfilter](../en/Preset%20filter.md).

## Vespucci individuell anpassen

Viele Gesichtspunkte der App können angepasst werden. Wenn man nach etwas Bestimmtem sucht und es nicht findet, ist die [Vespucci-Website](https://vespucci.io/) durchsuchbar und enthält zusätzliche Informationen, was auf dem Gerät möglich ist.

<a id="layers"></a>

### Ebeneneigenschaften

Die Ebeneneigenschaften können über den Ebenen-Schalter (obere rechte Ecke) geändert werden, alle anderen Festlegungen sind über die Einstellungen im Hauptmenü zugänglich.

Verfügbare Ebenen:

* Data layer - this is the layer OpenStreetMap data is loaded in to. In normal use you do not need to change anything here. Default: on.
* Background layer - there is a wide range of aerial and satellite background imagery available. The default value for this is the "standard style" map from openstreetmap.org.
* Overlay layer - these are semi-transparent layers with additional information, for example quality assurance information. Adding an overlay may cause issues with older devices and such with limited memory. Default: none.
* Notes/Bugs display - Open Notes and bugs will be displayed as a yellow bug icon, closed ones the same in green. Default: on.
* Photo layer - Displays geo-referenced photographs as red camera icons, if direction information is available the icon will be rotated. Default: off.
* Mapillary layer - Displays Mapillary segments with markers where images exist, clicking on a marker will display the image. Default: off.
* GeoJSON layer - Displays the contents of a GeoJSON file, multiple layers can be added from files. Default: none.
* GPX layer - Displays GPX tracks and way points, multiple layers can be added from files, during recording the generate GPX track is displayed in its own one . Default: none.
* Grid - Displays a scale along the sides of the map or a grid. Default: on. 

Mehr Informationen sind in der Karte verfügbar [map display](Main%20map%20display.md).

#### Einstellungen

* Bildschirm eingeschaltet lassen. Standard: aus.
* Große Knoten-Verschiebefläche. Das Bewegen von Knoten auf einem Gerät mit Touchscreen ist problematisch, da die Finger die aktuelle Position auf dem Display verdecken. Nach Einschalten dieser Funktion steht eine größere Fläche rund um den Knoten zur Verfügung, an der er außerhalb der Mitte angefasst und gezogen werden kann (Die Objektauswahl und andere Operationen verwenden weiterhin den normalen Toleranzbereich für Berührungen). Standard: aus.

Die vollständige Beschreibung findet man hier [Einstellungen] (Preferences.md)

Erweiterte Einstellungen

* Full screen mode. On devices without hardware buttons Vespucci can run in full screen mode, that means that "virtual" navigation buttons will be automatically hidden while the map is displayed, providing more space on the screen for the map. Depending on your device this may work well or not,  In _Auto_ mode we try to determine automatically if using full screen mode is sensible or not, setting it to _Force_ or _Never_ skips the automatic check and full screen mode will always be used or always not be used respectively. On devices running Android 11 or higher the _Auto_ mode will never turn full screen mode on as Androids gesture navigation provides a viable alternative to it. Default: _Auto_.  
* Node icons. Default: _on_.
* Always show context menu. When turned on every selection process will show the context menu, turned off the menu is displayed only when no unambiguous selection can be determined. Default: off (used to be on).
* Enable light theme. On modern devices this is turned on by default. While you can enable it for older Android versions the style is likely to be inconsistent. 

Die vollständige Beschreibung findet man hier [Erweiterte Einstellungen] (Advanced%20Preferences.md)

## Reporting and Resolving Issues

Wenn Vespucci abstürzt oder einen internen Fehler feststellt, fordert die App dazu auf einen Fehlerbericht einzusenden. Dies sollte man in diesen Fällen tun, aber bitte nur ein Mal für jede spezifische Situation. Will man Informationen ergänzen, eine Funktion vorschlagen oder Ähnliches, kann das hier geschehen: [Vespucci-Fehlerberichte] (https://github.com/MarcusWolschon/osmeditor4android/issues). Der "Informationen zur Problembehebung"-Button im Debug-Menü erstellt einen neuen Fehlerbericht und fügt die relevanten App- und Geräteinformationen hinzu - ohne extra Tipparbeit.

If you are experiencing difficulties starting the app after a crash, you can try to start it in _Safe_ mode on devices that support short cuts: long press on the app icon and then select _Safe_ from the menu. 

If you want to discuss something related to Vespucci, you can either start a discussion on the [OpenStreetMap forum](https://community.openstreetmap.org).


