_Before we start: most screens have links in the menu to the on-device help system giving you direct access to information relevant for the current context, you can easily navigate back to this text too. If you have a larger device, for example a tablet, you can open the help system in a separate split window.  All the help texts and more (FAQs, tutorials) can be found on the [Vespucci documentation site](https://vespucci.io/) too._

# Einführung in Vespucci

Vespucci ist ein vollwertiger OpenStreetMap-Editor, der die meisten Funktionen von Desktop-Editoren beherrscht. Er wurde erfolgreich auf Googles Android 2.3 bis 10.0 und verschiedenen AOSP-basierten Varianten getestet. Achtung: Während die Leistung mobiler Geräte mit der von Desktop-Konkurrenten aufgeholt hat, haben vor allem ältere Geräte nur sehr begrenzten Speicher zur Verfügung und sind meist recht langsam. Bei der Verwendung von Vespucci sollten Sie dies berücksichtigen und z.B. die zu bearbeitenden Bereiche auf eine angemessene Größe beschränken.

## Mit Vespucci OSM-Daten bearbeiten

Abhängig von der Bildschirmgröße und dem Alter des Gerätes sind die Bearbeitungsfunktionen entweder über Icons in einer Leiste am Oberrand, über ein Dropdown-Menü rechts in dieser Leiste, über eine Menüleiste am Unterrand (falls vorhanden) oder über die Menütaste zugänglich.

<a id="download"></a>

### OSM-Daten herunterladen

Entweder das Icon zur Übertragung ![Transfer](../images/menu_transfer.png) oder den Menüeintrag "Übertragen" wählen. Danach werden sieben Optionen zur Auswahl angezeigt:

* **Aktuelle Ansicht herunterladen** - Gebiet der aktuellen Bildschirmansicht herunterladen und mit den existierenden Daten zusammenführen *(erfordert Netzwerkverbindung)*
* **Löschen und aktuelle Ansicht herunterladen** - Alle Daten im Speicher löschen und dann die aktuelle Bildschirmansicht herunterladen *(erfordert Netzwerkverbindung)*
* **Daten zum OSM-Server hochladen** - Bearbeitungen an OpenStreetMap übertragen *(erfordert Authentifikation)* *(erfordert Netzwerkverbindung)*
* **Auto-Download nach Standort** - Automatisch ein Gebiet rund um den aktuellen geographischen Standort herunterladen *(erfordert Netzwerkverbindung oder Offline-Daten)* *(erfordert GPS)*
* **Auto-Download nach aktueller Ansicht** - Automatisch das momentan auf dem Bildschirm angezeigte Gebiet herunterladen *(erfordert Netzwerkverbindung oder Offline-Daten)* *(erfordert GPS)*
* **Datei...** - OSM-Daten aus Gerätedateien laden oder in sie speichern.
* **Notiz/Fehler...** - OSM-Hinweise und "Fehler" aus der Qualitätssicherung (momentan OSMOSE) herunterladen (automatisch oder manuell) *(erfordert Netzwerkverbindung)*

Um Daten auf dem Gerät zu öffnen, ist es am einfachsten, mit Gesten den Bildschirm auf das gewünschte Gebiet zu zentrieren und dann im Menü "Aktuelle Ansicht herunterladen" anzuwählen. Der Zoom kann mit Gesten, den Zoom-Schaltflächen oder den Lautstärketasten bedient werden. Vespucci sollte dann das Gebiet herunterladen. Um Daten herunterzuladen, muss man nicht angemeldet sein.

Mit den Standardeinstellungen werden alle "nicht-gedownloadete" Bereiche abgedunkelt. Dies verhindert unabsichtliches Editieren und Verdoppelung von Elementen in solchen Gebieten. Das Verhalten kann in den  [Erweiterteneinstellungen](Advanced%20preferences.md) geändert werden.

### Bearbeiten

<a id="lock"></a>

#### Sperren, entsperren, Modus wechseln

Um versehentliche Änderungen zu verhindern, startet Vespucci im "gesperrten" Modus, einem Modus, der nur das Zoomen und Verschieben der Karte erlaubt. Um den Schirm zu entsperren, auf das ![Schloss](../images/locked.png)-Icon tippen. 

Ein langer Druck auf das Schlosssymbol zeigt ein Auswahlmenü mit 4 Optionen:

* **Normal** - der Standardbearbeitungsmodus, neue Objekte können erstellt werden, bestehende bearbeitet, verschoben und gelöscht. Ein einfaches weißes Schlosssymbol wird angezeigt.
* **Nur Tags** - wird ein bestehendes Objekt ausgewählt, startet der Eigenschaftseditor, ein langer Druck auf dem Hauptschirm wird weiterhin neue Objekte hinzufügen, aber keine anderen Geometriebearbeitungen sind möglich. Ein weißes Schlosssymbol mit einem "T" wird angezeigt.
* **Adresse** - schaltet den Adressenmodus ein, einen etwas vereinfachten Modus mit adressspezifschen Aktionen im [Einfachen Modus](../en/Simple%20actions.md) "+" Schaltfläche.  Ein weißes Schlosssymbol mit einem "A" wird angezeigt.
* **Innenraum** - schaltet in den Innenraum-Modus, siehe  [Innenraum-Modus](#indoor). Ein weißes Schlosssymbol mit einem "I" wird angezeigt.
* **C-Modus** - in C-Modus werden nur Objekte angezeigt, die ein Problem haben, siehe [C-Modus](#c-mode).  Ein weißes Schlosssymbol mit einem "C" wird angezeigt.

#### Einfacher Klick, Doppelklick und langer Klick

In der Standardeinstellung wird um auswählbare Knoten und Wege ein oranger Bereich angezeigt, der angibt, in welchen Bereich man auf den Bildschirm tippen kann, um ein Objekt anzuwählen. Die drei Möglichkeiten sind:

* Einfacher Klick: Objekte auswählen. 
    * Ein einzelner Knoten/Weg wird sofort markiert. 
    * Soll ein Objekt ausgewählt werden und Vespucci stellt allerdings fest, dass die Auswahl mehrere Objekte betreffen kann, zeigt Vespucci ein Auswahlmenü an, aus dem das gewünschte Objekt gewählt werden kann. 
    * Ausgewählte Objekte werden in Gelb hervorgehoben. 
    * Für weitere Informationen siehe [Knoten ausgewählt](Node%20selected.md), [Weg ausgewählt](Way%20selected.md) und [Relation ausgewählt](Relation%20selected.md).
* Doppelklick: Startet den [Mehrfachauswahl-Modus](Multiselect.md)
* Klicken und halten: Erstellt ein "Fadenkreuz" und ermöglicht das Hinzufügen von Knoten, siehe unten und [Erstellen neuer Objekte](Creating%20new%20objects.md). Dies ist nur möglich, wenn der "Einfache Modus" deaktiviert ist.

In einem Gebiet, in dem die OSM-Daten sehr dicht sind, empfiehlt es sich, das Gebiet stark zu vergrößern.

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

Siehe [Erstellen neuer Objekte im einfachen Modus](Creating%20new%20objects%20in%20simple%20actions%20mode.md) für mehr Informationen.

##### Erweiterter Modus (Tippen und halten)
 
Durch langes Drücken an der Stelle, wo ein Knoten oder Weg erstellt werden soll, wird ein schwarzes "Fadenkreuz" angezeigt.
* Wenn man einen neuen (nicht mit einem Objekt verbundenen) Knoten erstellen will, klickt man neben bereits vorhandene Objekte.
* Soll ein vorhandener Weg erweitert werden, klickt man in die Toleranzzone des Weges (oder eines Knotens des Weges). Die Toleranzzone ist der Bereich um einen Knoten oder Weg herum.

Sobald das Positionskreuz erscheint, gibt es die folgenden Möglichkeiten:

* Nochmaliges Berühren an der gleichen Stelle.
    * Ist das Positionskreuz nicht in der Nähe eines Punktes, erstellt das Berühren einen neuen Punkt. Falls die Position in der Nähe eines Weges (aber nicht in der Nähe eines Punktes) ist wird der neue Punkt als Teil des Weges erstellt.
    * Ist das Positionskreuz in der Nähe eines Punktes, so wird kein neuer Punkt erstellt, sondern nur der Eigenschaftseditor gestartet.
* Nochmaliges Berühren an einer anderen Stelle. Erstellt einen Punkt wie oben beschrieben und einen Wegabschnitt zu der neuen Position.

Um den Weg zu verlängern, tippe an den Stellen wo du weitere Wegpunkte haben willst. Um den Weg fertigzustellen, tippe nochmals auf den letzten Punkt. Falls der Endpunkt auf einem anderen Weg oder Punkt liegt, wird er automatisch in diesen integriert. 

Man kann auch einen Menüpunkt dafür verwenden: Weitere Informationen dazu unter [Erstellen neuer Objekte](Creating%20new%20objects.md).

#### Flächen hinzufügen

Aktuell hat OpenStreetMap keinen eigenen Objekttyp für Flächen im Gegensatz zu anderen Geo-Datensystemen. Der Online-Editor "iD" versucht, aus den zugrundeliegenden OSM-Elementen eine Abstraktion der Flächen zu erstellen, was in bestimmten Fällen gut, in anderen nicht richtig funktioniert. Vespucci versucht das in der vorliegenden Version nicht, deshalb hier einige Informationen dazu wie in OSM Flächen abgebildet werden:

* _closed ways (*polygons")_: the simplest and most common area variant, are ways that have a shared first and last node forming a closed "ring" (for example most buildings are of this type). These are very easy to create in Vespucci, simply connect back to the first node when you are finished with drawing the area. Note: the interpretation of the closed way depends on its tagging: for example if a closed way is tagged as a building it will be considered an area, if it is tagged as a roundabout it wont. In some situations in which both interpretations may be valid, an "area" tag can clarify the intended use.
* _multi-polygons_: some areas have multiple parts, holes and rings that can't be represented with just one way. OSM uses a specific type of relation (our general purpose object that can model relations between elements) to get around this, a multi-polygon. A multi-polygon can have multiple "outer" rings, and multiple "inner" rings. Each ring can either be a closed way as described above, or multiple individual ways that have common end nodes. While large multi-polygons are difficult to handle with any tool, small ones are not difficult to create in Vespucci. 
* _coastlines_: for very large objects, continents and islands, even the multi-polygon model doesn't work in a satisfactory way. For natural=coastline ways we assume direction dependent semantics: the land is on the left side of the way, the water on the right side. A side effect of this is that, in general, you shouldn't reverse the direction of a way with coastline tagging. More information can be found on the [OSM wiki](http://wiki.openstreetmap.org/wiki/Tag:natural%3Dcoastline).

#### Die Geometrie eines Weges verbessern

Zoomt man genügend nah an ein ausgewähltes Wegsegment, wird ein kleines "x" sichtbar. Zieht man daran, wird ein Knoten im Weg erstellt. Hinweis: um das versehentliche Erstellen solcher Punkte zu verhindern, ist der empfindliche Bereich um das "x" ziemlich klein.

#### Kopieren, Ausschneiden & Einfügen

Ausgewählte Knoten und Wege können kopiert oder ausgeschnitten und dann einmal oder mehrfach wieder eingefügt werden. Beim Ausschneiden bleibt sowohl die OSM-ID als auch die Version erhalten. Ein langer Druck markiert die Position, an der eingefügt werden soll, um die Aktion abzuschließen, danach "Einfügen" aus dem Menü auswählen.

#### Effizient Adressen eintragen

Vespucci supports functionality that makes surveying addresses more efficient by predicting house numbers (left and right sides of streets separately) and automatically adding _addr:street_ or _addr:place_ tags based on the last used value and proximity. In the best case this allows adding an address without any typing at all.   

Adding the tags can be triggered by pressing ![Address](../images/address.png): 

* after a long press (in non-simple mode only): Vespucci will add a node at the location and make a best guess at the house number and add address tags that you have been lately been using. If the node is on a building outline it will automatically add an "entrance=yes" tag to the node. The tag editor will open for the object in question and let you make any necessary further changes.
* in the node/way selected modes: Vespucci will add address tags as above and start the tag editor.
* in the property editor.

To add individual address nodes directly while in the default "Simple mode" switch to "Address" editing mode (long press on the lock button), "Add address node" will then add an address node at the location and if it is on a building outline add a entrance tag to it as described above.

Die Hausnummernvorhersage benötigt typischerweise mindestens die Eingabe von je 2 Hausnummern auf jeder Seite der Straße, je mehr Nummern in den Daten vorhanden sind desto besser funktioniert die Vorhersage. 

Consider using this with one of the [Auto-download](#download) modes.  

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

Vespucci unterstützt sowohl OAuth-Autorisierung (Standardeinstellung) als auch die klassische Methode mit Benutzername und Passwort. Wo möglich sollte OAuth verwendet werden, um die Passwortübertragung im Klartext zu vermeiden.  

Neue Vespucci-Installationen haben OAuth voreingestellt. Beim ersten Versuch, bearbeitete Daten auf den OSM-Server zu speichern, wird eine Seite der OSM-Website geladen. Nach der Anmeldung (über eine verschlüsselte Verbindung) wird um die Autorisierung von Vespucci zum Bearbeiten unter Verwendung des eigenen Kontos gebeten. Soll oder muss schon vor der Bearbeitung der OAuth-Zugriff auf das eigene Konto gestattet werden, gibt es hierfür einen entsprechenden Eintrag im "Werkzeuge"-Menü.

Sollen die Änderungen gespeichert werden und es ist kein Internetzugang verfügbar, können sie in einer mit JOSM kompatiblen .osm-Datei gespeichert werden und später entweder mit Vespucci oder mit JOSM hochgeladen werden. 

#### Konfliktbehebung beim Upload

Vespucci hat einen einfachen Konfliktbehebungsmechanismus eingebaut. Sind größere Probleme mit den Änderungen zu erwarten, empfehlen wir, sie in eine .osc-Datei zu speichern ("Transfer" Menü, "Datei...,"  "Änderungen exportieren") und die Konflikte dann mit JOSM zu beheben. Für Details siehe Hilfetext zur [Konfliktauflösung](../en/Conflict%20resolution.md).  

## Using GPS and GPX tracks

With standard settings Vespucci will try to enable GPS (and other satellite based navigation systems) and will fallback to determining the position via so called "network location" if this is not possible. This behaviour assumes that you in normal use have your Android device itself configured to only use GPX generated locations (to avoid tracking), that is you have the euphemistically named "Improve Location Accuracy" option turned off. If you want to enable the option but want to avoid Vespucci falling back to "network location", you should turn the corresponding option in the [Advanced preferences](Advanced%20preferences.md) off. 

Touching the ![GPS](../images/menu_gps.png) button (on the left hand side of the map display) will center the screen on the current position and as you move the map display will be padded to maintain this.  Moving the screen manually or editing will cause the "follow GPS" mode to be disabled and the blue GPS arrow will change from an outline to a filled arrow. To quickly return to the "follow" mode, simply touch GPS button or re-check the equivalent menu option. If the device doesn't have a current location the location marker/arrow will be displayed in black, if a current location is available the marker will be blue.

To record a GPX track and display it on your device select "Start GPX track" item in the ![GPS](../images/menu_gps.png) menu. This will add layer to the display with the current recorded track, you can upload and export the track from the entry in the [layer control](Main%20map%20display.md). Further layers can be added from local GPX files and tracks downloaded from the OSM API.

Note: by default Vespucci will not record elevation data with your GPX track, this is due to some Android specific issues. To enable elevation recording, either install a gravitational model, or, simpler, go to the [Advanced preferences](Advanced%20preferences.md) and configure NMEA input.

## Notizen und Fehler

Vespucci unterstützt das Herunterladen, Kommentieren und Schließen von OSM-Notizen (vormals OSM Bugs) und die entsprechende Funktionalität für "Fehler" die vom [OSMOSE Qualitätssicherungswerkzeug](http://osmose.openstreetmap.fr/en/map/) gemeldet werden. Beide müssen entweder explizit heruntergeladen werden oder die Notizen und Fehler in der Nähe können automatisch geladen werden. Geänderte oder geschlossene Notizen und Fehler können entweder sofort hochgeladen werden oder gespeichert und alle zusammen später hochgeladen werden.

Auf der Karte werden die Notizen und Fehler mit einem kleinen Käfer-Symbol  ![Bug](../images/bug_open.png) angezeigt, grüne sind behoben, blaue sind neu erstellt oder geändert, und Gelb zeigt an, dass die Notiz respektive der Fehler noch unverändert aktiv ist. 

In der OSMOSE-Fehlerdarstellung wird für die betroffenen Objekte jeweils ein blau hervorgehobener Link angezeigt. Wählt man den Link an, wird das Objekt ausgewählt, der Bildschirm darauf zentriert und, falls nötig, das entsprechende Gebiet heruntergeladen. 

### Anzeigefilter

Zusätzlich zum Freischalten der Notiz- und Fehleranzeige kann eine grobe Auswahl der angezeigten Objekte eingestellt werden. In den "Erweiterten Einstellungen" kann folgendes ausgewählt werden:   

* Notizen
* Osmose-Fehler
* Osmose-Warnung
* Osmose-Hinweis
* Maproulette
* Benutzerdefiniert

<a id="indoor"></a>

## Innenraum-Modus

In Innenräumen Daten zu erfassen ist anspruchsvoll aufgrund der grossen Anzahl von Objekten, die sich häufig überlappen. Vespucci hat einen speziellen Innenraum-Modus, der es ermöglicht, alle Objekte, die nicht auf der gleichen Etage sind, auszufiltern, und der bei neu erstellten Objekten automatisch die richtige Etage in den Objekteigenschaften einträgt. 

In den Modus kann durch einen langen Druck auf das Schlosssymbol gewechselt werden, siehe auch [Sperren, entsperren, Modus wechseln](#lock).

<a id="c-mode"></a>

## C-Modus

Im C-Modus werden nur Objekte angezeigt, die ein Problem haben, was das Finden von Objekten, die spezifische Probleme haben oder zu  konfigurierbaren Qualitätsprüfungen passen, erleichtert. Wird ein solches Objekt angewählt, startet der Eigenschaftseditor und die am besten passende Vorlage wird automatisch angewandt.

In den Modus kann durch einen langen Druck auf das Schlosssymbol gewechselt werden, siehe auch [Sperren, entsperren, Modus wechseln](#lock).

### Prüfungen konfigurieren

Im Augenblick sind zwei der Prüfungen konfigurierbar, beide können durch Auswahl der "Validierungseinstellungen" in den "Einstellungen" geändert werden. (Die Tests für FIXME-Tags und  für fehlende "type"-Tags bei Relationen sind derzeit nicht konfigurierbar.)  

Die Liste ist zweigeteilt, die obere Hälfte enthält die "Überprüfungstests", die untere die Tests auf "Fehlende Tags". Einträge können durch anklicken bearbeitet werden, der grüne Menüknopf erlaubt es weitere Einträge hinzuzufügen.

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

* **Key** - Key that should be present on the object according to the matching preset.
* **Require optional** - Require the key even if the key is in the optional tags of the matching preset.

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

* Datenebene - OpenStreetMap Daten werden in dieses Ebene geladen. Im normalen Gebrauch muss hier nichts geändert werden. Standardeinstellung: eingeschaltet.
* Hintergrundebene - es steht eine große Auswahl an Luftbildern und anderen Hintergründen zur Verfügung. Standardeinstellung ist die "standard style"-Karte von openstreetmap.org.
* Überlagerungsebene - teiltransparente Ebenen mit Zusatzdaten, zum Beispiel GPX-Aufzeichnungen. Auf alten Geräten und solchen mit wenig Hauptspeicher können zusätzliche Ebenen zu Problemen führen. Standardeinstellung: keine Ebene.
* Notizen/Aufgaben/Fehlerebene - offene Notizen und Fehler werden mit einem gelben Icon angezeigt, geschlossene in grün. Standardeinstellung: eingeschaltet.
* Fotoebene - zeigt georeferenzierte Fotos als rote Kamera-Icons an, falls die Aufnahmerichtung mitgespeichert wurde, werden die Icons entsprechend gedreht. Berühren/Klicken auf die Icons zeigt das Bild an. Standardeinstellung: ausgeschaltet.
* Mapillary-Ebene - Zeigt Mapillary-Segmente mit Marker für die Bilder an. Berühren/Klicken auf die Icons zeigt das Bild an. Standardeinstellung: ausgeschaltet.
* GeoJSON-Ebene - zeigt den Inhalt eine GeoJSON Datei an. Standardeinstellung: ausgeschaltet.
* Gitter / Skalen - zeigt eine Skala entlang der Seiten der Karte oder Gitterlinien an. Standardeinstellung: eingeschaltet. 

Mehr Informationen sind in der Karte verfügbar [map display](Main%20map%20display.md).

#### Einstellungen

* Bildschirm eingeschaltet lassen. Standard: aus.
* Große Knoten-Verschiebefläche. Das Bewegen von Knoten auf einem Gerät mit Touchscreen ist problematisch, da die Finger die aktuelle Position auf dem Display verdecken. Nach Einschalten dieser Funktion steht eine größere Fläche rund um den Knoten zur Verfügung, an der er außerhalb der Mitte angefasst und gezogen werden kann (Die Objektauswahl und andere Operationen verwenden weiterhin den normalen Toleranzbereich für Berührungen). Standard: aus.

Die vollständige Beschreibung findet man hier [Einstellungen] (Preferences.md)

Erweiterte Einstellungen

* Knotensymbole. Standard: ein.
* Kontextmenü immer anzeigen. Wenn eingeschaltet, wird bei jedem Auswahlvorgang das Kontextmenü angezeigt, ausgeschaltet wird das Menü nur dann angezeigt, wenn keine eindeutige Auswahl bestimmt werden kann. Voreinstellung: aus (war früher eingeschaltet).
* Helles Thema aktivieren. Bei modernen Geräten ist dies standardmäßig eingeschaltet. Die Aktivierung bei älteren Android-Versionen führt wahrscheinlich zu einem nicht einheitlichen Aussehen. 

Die vollständige Beschreibung findet man hier [Erweiterte Einstellungen] (Advanced%20Preferences.md)

## Fehler melden

Wenn Vespucci abstürzt oder einen internen Fehler feststellt, fordert die App dazu auf einen Fehlerbericht einzusenden. Dies sollte man in diesen Fällen tun, aber bitte nur ein Mal für jede spezifische Situation. Will man Informationen ergänzen, eine Funktion vorschlagen oder Ähnliches, kann das hier geschehen: [Vespucci-Fehlerberichte] (https://github.com/MarcusWolschon/osmeditor4android/issues). Der "Informationen zur Problembehebung"-Button im Debug-Menü erstellt einen neuen Fehlerbericht und fügt die relevanten App- und Geräteinformationen hinzu - ohne extra Tipparbeit.

Wer Themen zu Vespucci diskutieren will, kann eine Diskussion in der [Vespucci-Google-Gruppe](https://groups.google.com/forum/#!forum/osmeditor4android) oder im [OpenStreetMap-Android-Forum](http://forum.openstreetmap.org/viewforum.php?id=56) beginnen.


