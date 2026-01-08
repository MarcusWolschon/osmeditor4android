_Bevor wir anfangen: Die meisten Bildschirme haben im Menü Links zum integrierten Hilfesystem, das dir direkten Zugang zu relevanten Informationen für den aktuellen Kontext gibt. Du kannst auch leicht zu diesem Text zurücknavigieren. Wenn du ein größeres Gerät wie ein Tablet hast, kannst du das Hilfesystem in einem separaten geteilten Fenster öffnen. Alle Hilfetexte und mehr (FAQs, Tutorials) findest du auch auf der [Vespucci-Dokumentationsseite](https://vespucci.io/). Auf Geräten, die Shortcuts unterstützen, kannst du den Hilfe-Viewer auch direkt starten, indem du das App-Symbol lange drückst und "Hilfe" auswählst._

# Einführung in Vespucci

Vespucci ist ein voll ausgestatteter OpenStreetMap-Editor, der die meisten Funktionen bietet, die auch Desktop-Editoren unterstützen. Er wurde erfolgreich auf Googles Android-Versionen 2.3 bis 14.0 getestet (Versionen vor 4.1 werden nicht mehr unterstützt) sowie auf verschiedenen AOSP-basierten Varianten. Ein Wort der Vorsicht: Auch wenn mobile Geräte inzwischen mit ihren Desktop-Pendants mithalten können, haben insbesondere ältere Geräte nur begrenzt Speicher zur Verfügung und neigen dazu, eher langsam zu sein. Das solltest du bei der Nutzung von Vespucci berücksichtigen und beispielsweise die Größe der Bereiche, die du bearbeitest, in einem vernünftigen Rahmen halten.

## Mit Vespucci OSM-Daten bearbeiten

Abhängig von Bildschirmgröße und Alter des Gerätes sind die Bearbeitungsfunktionen entweder über Icons in einer Leiste am Oberrand, über ein Dropdown-Menü rechts in dieser Leiste, über eine Menüleiste am Unterrand (falls vorhanden) oder über die Menütaste zugänglich.

<a id="download"></a>

### OSM-Daten herunterladen

Wähle entweder das Übertragungssymbol ![Übertragung](../images/menu_transfer.png) oder den Menüpunkt "Übertragung". Dadurch werden elf Optionen angezeigt:

* **Daten auf OSM-Server hochladen...** - Änderungen überprüfen und auf OpenStreetMap hochladen *(erfordert Authentifizierung)* *(erfordert Netzwerkverbindung)*
* **Änderungen überprüfen...** - aktuelle Änderungen überprüfen
* **Aktuellen Ausschnitt herunterladen** - den auf dem Bildschirm sichtbaren Bereich herunterladen und mit vorhandenen Daten zusammenführen *(erfordert Netzwerkverbindung oder eine Offline-Datenquelle)*
* **Speicher leeren und aktuellen Ausschnitt herunterladen** - alle Daten im Speicher, einschließlich ausstehender Änderungen, löschen und den auf dem Bildschirm sichtbaren Bereich herunterladen *(erfordert Netzwerkverbindung)*
* **Über Overpass abfragen...** - eine Abfrage gegen einen Overpass-API-Server ausführen *(erfordert Netzwerkverbindung)*
* **Automatischer Download basierend auf Standort** - ein Gebiet um den aktuellen geografischen Standort automatisch herunterladen *(erfordert Netzwerkverbindung oder Offline-Daten)* *(erfordert GPS)*
* **Automatischer Download bei Schwenken und Zoomen** - Daten für den aktuell angezeigten Kartenausschnitt automatisch herunterladen *(erfordert Netzwerkverbindung oder Offline-Daten)* *(erfordert GPS)*
* **Daten aktualisieren** - Daten für alle Bereiche erneut herunterladen und den Speicherinhalt aktualisieren *(erfordert Netzwerkverbindung)*
* **Daten löschen** - alle OSM-Daten im Speicher, einschließlich ausstehender Änderungen, entfernen.
* **Datei...** - OSM-Daten auf dem Gerät speichern und laden.
* **Aufgaben...** - OSM-Notizen und „Bugs“ von QA-Tools (derzeit OSMOSE) automatisch und manuell herunterladen *(erfordert Netzwerkverbindung)*.

Um Daten auf dem Gerät zu öffnen, ist es am einfachsten, mit Gesten den Bildschirm auf das gewünschte Gebiet zu zentrieren und dann im Menü "Aktuelle Ansicht herunterladen" anzuwählen. Der Zoom kann mit Gesten, den Zoom-Schaltflächen oder den Lautstärketasten bedient werden. Vespucci sollte dann das Gebiet herunterladen. Um Daten herunterzuladen, muss man nicht angemeldet sein.

Im entsperrten Zustand werden alle nicht heruntergeladenen Bereiche im Vergleich zu den heruntergeladenen Bereichen abgeblendet, wenn Du weit genug hineingezoomt hast, um das Bearbeiten zu ermöglichen. Dies dient dazu, das versehentliche Hinzufügen doppelter Objekte in Bereichen zu vermeiden, die nicht angezeigt werden. Im gesperrten Zustand ist das Abblenden deaktiviert. Dieses Verhalten kann in den [Erweiterten Einstellungen](Advanced%20preferences.md) so geändert werden, dass das Abblenden immer aktiv ist.

Falls Du einen nicht standardmäßigen OSM-API-Eintrag verwenden oder [Offline-Daten](https://vespucci.io/tutorials/offline/) im _MapSplit_-Format nutzen musst, kannst Du Einträge über den Eintrag _Konfigurieren..._ für die Datenebene in der Ebenensteuerung hinzufügen oder ändern.

### Bearbeiten

<a id="lock"></a>

#### Sperren, entsperren, Modus wechseln

Um versehentliche Änderungen zu verhindern, startet Vespucci im "gesperrten" Modus, einem Modus, der nur das Zoomen und Verschieben der Karte erlaubt. Um den Schirm zu entsperren, auf das ![Schloss](../images/locked.png)-Icon tippen. 

Ein langer Druck auf das Sperrsymbol oder das _Modi_-Menü im Überlaufmenü der Kartenanzeige zeigt ein Menü mit 4 Optionen an:

* **Normal** – der Standard-Bearbeitungsmodus, neue Objekte können hinzugefügt, vorhandene bearbeitet, verschoben und entfernt werden. Einfaches weißes Sperrsymbol wird angezeigt.
* **Nur Tags** – das Auswählen eines vorhandenen Objekts öffnet den Eigenschaften-Editor. Neue Objekte können über den grünen „+“-Button oder durch langes Drücken hinzugefügt werden, aber keine anderen Geometrieoperationen sind möglich. Weißes Sperrsymbol mit einem „T“ wird angezeigt.
* **Adresse** – aktiviert den Adressmodus, einen leicht vereinfachten Modus mit speziellen Aktionen, die über den [Einfach-Modus](../en/Simple%20actions.md) „+“-Button verfügbar sind. Weißes Sperrsymbol mit einem „A“ wird angezeigt.
* **Indoor** – aktiviert den Indoor-Modus, siehe [Indoor-Modus](#indoor). Weißes Sperrsymbol mit einem „I“ wird angezeigt.
* **C-Modus** – aktiviert den C-Modus, nur Objekte mit einem Warnhinweis werden angezeigt, siehe [C-Modus](#c-mode). Weißes Sperrsymbol mit einem „C“ wird angezeigt.

Wenn Du Vespucci auf einem Android-Gerät verwendest, das Kurzbefehle unterstützt (langes Drücken auf das App-Symbol), kannst Du direkt im _Adress_- und _Indoor_-Modus starten.

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

Siehe [Erstellen neuer Objekte im Einfach-Modus](Simple%20actions.md) für mehr Informationen. Der Einfach-Modus ist die Voreinstellung bei neuen Installationen.

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

Sie können ausgewählte Knoten und Wege kopieren und dann einmal oder mehrmals an einer neuen Stelle einfügen. Beim Ausschneiden bleiben die Osm-ID und die Version erhalten, so dass sie nur einmal eingefügt werden können. Zum Einfügen drücken Sie lange auf die Stelle, an der Sie einfügen möchten (Sie sehen ein Fadenkreuz, das die Stelle markiert). Wählen Sie dann „Einfügen“ aus dem Menü.

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

Vespucci unterstützt OAuth 2, OAuth 1.0a-Autorisierung und die klassische Methode mit Benutzername und Passwort. Seit dem 1. Juli 2024 unterstützt die Standard-OpenStreetMap-API nur noch OAuth 2. Andere Methoden sind nur noch bei privaten Installationen der API oder bei anderen Projekten verfügbar, die OSM-Software für andere Zwecke nutzen.  

Um Vespucci zu autorisieren, in Ihrem Namen auf Ihr Konto zuzugreifen, müssen Sie sich einmalig mit Ihrem Anzeigenamen und Passwort anmelden. Wenn Ihre Vespucci-Installation nicht autorisiert ist und Sie versuchen, geänderte Daten hochzuladen, werden Sie aufgefordert, sich auf der OSM-Website (über eine verschlüsselte Verbindung) anzumelden. Nach der Anmeldung werden Sie aufgefordert, Vespucci die Berechtigung zur Bearbeitung Ihres Kontos zu erteilen. Wenn Sie den OAuth-Zugriff auf Ihr Konto vor der Bearbeitung autorisieren möchten oder müssen, finden Sie einen entsprechenden Eintrag im Menü „Tools“.

Sollen die Änderungen gespeichert werden und es ist kein Internetzugang verfügbar, können sie in einer mit JOSM kompatiblen .osm-Datei gespeichert werden und später entweder mit Vespucci oder mit JOSM hochgeladen werden. 

#### Konfliktbehebung beim Upload

Vespucci hat einen einfachen Konfliktbehebungsmechanismus eingebaut. Sind größere Probleme mit den Änderungen zu erwarten, empfehlen wir, sie in eine .osc-Datei zu speichern ("Transfer" Menü, "Datei...,"  "Änderungen exportieren") und die Konflikte dann mit JOSM zu beheben. Für Details siehe Hilfetext zur [Konfliktauflösung](../en/Conflict%20resolution.md).  

### Anzeige eines nahegelegenen Point-of-Interest

Ein nahegelegenes Point-of-Interest-Display kann angezeigt werden, indem der Griff in der Mitte und oben in der unteren Menüleiste nach oben gezogen wird. 

Weitere Informationen zu dieser und anderen verfügbaren Funktionen der Hauptanzeige finden Sie hier [Hauptkartenanzeige](Main%20map%display.md).

## Nutzung von GPS und GPX-Tracks

Mit Standardeinstellungen wird Vespucci versuchen, das GPS (und andere satellitenbasierte Navigstionssysteme) zu aktivieren, und, falls dies nicht möglich ist, auf die Positionsbestimmung über den sogenannten "Netzwerkstandort" zurückgreifen. Dieses Verhalten lässt vermuten, dass Sie für den normalen Gebrauch Ihr Android-Gerät selbst so konfiguriert haben, dass es nur GPX-generierte Standorte verwenden soll (um Tracking zu vermeiden), das heißt, Sie haben die euphemistisch mit "Verbesserte Standortgenauigkeit" bezeichnete Option ausgeschaltet. Wenn Sie die Option aktivieren möchten aber Vespucci daran hindern wollen, auf den "Netzwerkstandort" zurückzugreifen, sollten Sie die dazugehörige Option in den [Erweiterten Einstellungen](Advanced%20preferences.md) ausschalten. 

Durch Berühren der Schaltfläche ![GPS](../images/menu_gps.png) (normalerweise auf der linken Seite der Kartenanzeige) wird der Bildschirm auf die aktuelle Position zentriert, und wenn Sie sich bewegen, wird die Kartenanzeige geschwenkt, um dies beizubehalten. Wenn Sie den Bildschirm manuell verschieben oder bearbeiten, wird der Modus „GPS folgen“ deaktiviert und der blaue GPS-Pfeil ändert sich von einem Umriss zu einem ausgefüllten Pfeil. Um schnell zum „Follow“-Modus zurückzukehren, berühren Sie einfach die GPS-Taste oder aktivieren Sie die entsprechende Menüoption erneut. Wenn das Gerät keinen aktuellen Standort hat, wird die Standortmarkierung/der Pfeil schwarz angezeigt, wenn ein aktueller Standort verfügbar ist, ist die Markierung blau.

Um einen GPX-Track aufzuzeichnen und auf Ihrem Gerät anzuzeigen, wählen Sie "GPX-Track starten" im Menü ![GPS](../images/menu_gps.png). Dies fügt der Anzeige einen Layer mit dem aktuell aufgezeichneten Track hinzu. Sie können den Track über den Eintrag in der [Ebenenkontrolle](Main%20map%20display.md) hochladen und exportieren. Weitere Ebenen können aus lokalen GPX-Dateien und von heruntergeladenen Tracks der OSM-API hinzugefügt werden.

Hinweis: Vespucci zeichnet standardmäßig keine Höhendaten mit Ihrem GPX-Track auf, dies ist auf einige Android-spezifische Probleme zurückzuführen. Um die Höhenaufzeichnung zu aktivieren, installieren Sie entweder ein Gravitationsmodell oder, einfacher, gehen Sie zu den [Erweiterten Voreinstellungen](Advanced%20preferences.md) und konfigurieren Sie den NMEA-Eingang.

### Wie exportiert man einen GPX-Track?

Öffnen Sie das Ebenenmenü, klicken Sie dann auf das 3-Punkte-Menü neben „GPX-Aufzeichnung“ und wählen Sie **GPX-Track exportieren...**. Wählen Sie aus, in welchen Ordner Sie den Track exportieren möchten, und geben Sie ihm einen Namen mit der Endung `.gpx` (Beispiel: MeinTrack.gpx).

## Notizen, Bugs und Todos

Vespucci unterstützt das Herunterladen, Kommentieren und Schließen von OSM-Notizen (vormals OSM Bugs) und die entsprechende Funktionalität für "Fehler" die vom [OSMOSE Qualitätssicherungswerkzeug](http://osmose.openstreetmap.fr/en/map/) gemeldet werden. Beide müssen entweder explizit heruntergeladen werden oder die Notizen und Fehler in der Nähe können automatisch geladen werden. Geänderte oder geschlossene Notizen und Fehler können entweder sofort hochgeladen werden oder gespeichert und alle zusammen später hochgeladen werden. 

Außerdem unterstützen wir „Todos“, die entweder aus OSM-Elementen, aus einer GeoJSON-Ebene oder extern in Vespucci erstellt werden können. Diese bieten eine bequeme Möglichkeit, den Überblick über die Arbeit zu behalten, die Sie erledigen möchten. 

Auf der Karte werden die Notizen und Fehler durch ein kleines Fehlersymbol ![Bug](../images/bug_open.png) dargestellt, grüne sind geschlossen/gelöst, blaue wurden von Ihnen erstellt oder bearbeitet, und gelb zeigt an, dass sie noch aktiv sind und nicht geändert wurden. Todos verwenden ein gelbes Kästchensymbol.

Die OSMOSE-Fehler- und Todos-Anzeige zeigt einen blauen Link zu dem betroffenen Element an (im Fall von Todos nur, wenn ein OSM-Element damit verknüpft ist); durch Berühren des Links wird das Objekt ausgewählt, der Bildschirm darauf zentriert und der Bereich gegebenenfalls vorher heruntergeladen. 

### Anzeigefilter

Zusätzlich zum Freischalten der Notiz- und Fehleranzeige kann eine grobe Auswahl der angezeigten Objekte eingestellt werden. In den "Erweiterten Einstellungen" kann folgendes ausgewählt werden:   

* Anmerkungen
* Osmose-Fehler
* Osmose-Anmerkung
* Osmose kleineres Problem
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

Alle Validierungen können unter "Validator Einstellungen/Aktivierte Validierungen" in den [preferences](Preferences.md) deaktiviert/aktiviert werden. 

In der Konfiguration für „Re-survey“-Einträge können Sie eine Zeit festlegen, nach der eine Tag-Kombination erneut erfasst werden soll. „Check"-Einträge sind Markierungen, die auf Objekten vorhanden sein sollten, wie durch übereinstimmende Voreinstellungen bestimmt. Einträge können durch Anklicken bearbeitet werden, die grüne Menüschaltfläche ermöglicht das Hinzufügen von Einträgen.

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

* Datenebene – dies ist die Ebene, in der OpenStreetMap-Daten geladen werden. Im normalen Gebrauch musst Du hier nichts ändern. Standard: ein.
* Hintergrundebene – es steht eine große Auswahl an Luft- und Satellitenbildern als Hintergrund zur Verfügung. Der Standardwert ist die „Standardstil“-Karte von openstreetmap.org.
* Überlagerungsebene – das sind halbtransparente Ebenen mit zusätzlichen Informationen, zum Beispiel zur Qualitätssicherung. Das Hinzufügen einer Überlagerung kann bei älteren Geräten oder solchen mit begrenztem Speicher zu Problemen führen. Standard: keine.
* Anzeige von Notizen/Fehlern – offene Notizen und Fehler werden als gelbes Käfer-Symbol angezeigt, geschlossene als dasselbe Symbol in Grün. Standard: ein.
* Foto-Ebene – zeigt georeferenzierte Fotos als rote Kamera-Symbole an; wenn Richtungsinformationen verfügbar sind, wird das Symbol entsprechend gedreht. Standard: aus.
* Mapillary-Ebene – zeigt Mapillary-Segmente mit Markierungen, wo Bilder vorhanden sind; ein Klick auf eine Markierung zeigt das Bild an. Standard: aus.
* GeoJSON-Ebene – zeigt den Inhalt einer GeoJSON-Datei an; mehrere Ebenen können aus Dateien hinzugefügt werden. Standard: keine.
* GPX-Ebene – zeigt GPX-Tracks und Wegpunkte an; mehrere Ebenen können aus Dateien hinzugefügt werden, während der Aufzeichnung wird der erstellte GPX-Track in einer eigenen Ebene angezeigt. Standard: keine.
* Gitter – zeigt eine Skala entlang der Kartenränder oder ein Gitter an. Standard: ein. 

Mehr Informationen sind in der Karte verfügbar [map display](Main%20map%20display.md).

#### Einstellungen

* Bildschirm eingeschaltet lassen. Standard: aus.
* Große Knoten-Verschiebefläche. Das Bewegen von Knoten auf einem Gerät mit Touchscreen ist problematisch, da die Finger die aktuelle Position auf dem Display verdecken. Nach Einschalten dieser Funktion steht eine größere Fläche rund um den Knoten zur Verfügung, an der er außerhalb der Mitte angefasst und gezogen werden kann (Die Objektauswahl und andere Operationen verwenden weiterhin den normalen Toleranzbereich für Berührungen). Standard: aus.

Die vollständige Beschreibung findet man hier [Einstellungen] (Preferences.md)

Erweiterte Einstellungen

* Vollbildmodus. Auf Geräten ohne Hardware-Tasten kann Vespucci im Vollbildmodus laufen, das bedeutet, dass die „virtuellen“ Navigationstasten automatisch ausgeblendet werden, während die Karte angezeigt wird, was mehr Platz auf dem Bildschirm für die Karte schafft. Je nach Gerät kann dies gut oder weniger gut funktionieren. Im _Auto_-Modus versuchen wir automatisch festzustellen, ob die Verwendung des Vollbildmodus sinnvoll ist oder nicht. Die Einstellung _Erzwingen_ oder _Nie_ überspringt die automatische Prüfung, und der Vollbildmodus wird immer oder niemals verwendet. Auf Geräten mit Android 11 oder höher wird der _Auto_-Modus den Vollbildmodus nie aktivieren, da die Gestennavigation von Android eine brauchbare Alternative darstellt. Standard: _Auto_.
* Knoten-Symbole. Standard: _ein_.
* Kontextmenü immer anzeigen. Wenn aktiviert, wird bei jedem Auswahlvorgang das Kontextmenü angezeigt; wenn deaktiviert, wird das Menü nur angezeigt, wenn keine eindeutige Auswahl bestimmt werden kann. Standard: aus (früher war es aktiviert).
* Helles Design aktivieren. Auf modernen Geräten ist dies standardmäßig aktiviert. Während Du es auch für ältere Android-Versionen aktivieren kannst, ist der Stil möglicherweise uneinheitlich. 

Die vollständige Beschreibung findet man hier [Erweiterte Einstellungen] (Advanced%20Preferences.md)

## Melden und Lösen von Problemen

Wenn Vespucci abstürzt oder einen internen Fehler feststellt, fordert die App dazu auf einen Fehlerbericht einzusenden. Dies sollte man in diesen Fällen tun, aber bitte nur ein Mal für jede spezifische Situation. Will man Informationen ergänzen, eine Funktion vorschlagen oder Ähnliches, kann das hier geschehen: [Vespucci-Fehlerberichte] (https://github.com/MarcusWolschon/osmeditor4android/issues). Der "Informationen zur Problembehebung"-Button im Debug-Menü erstellt einen neuen Fehlerbericht und fügt die relevanten App- und Geräteinformationen hinzu - ohne extra Tipparbeit.

Wenn du Schwierigkeiten hast, die App nach einem Absturz zu starten, kannst du versuchen, sie im _Sicheren Modus_ zu öffnen, sofern dein Gerät Shortcuts unterstützt: Halte das App-Symbol lange gedrückt und wähle dann _Sicherer Modus_ aus dem Menü. 

Wenn du etwas im Zusammenhang mit Vespucci besprechen möchtest, kannst du eine Diskussion im [OpenStreetMap-Forum](https://community.openstreetmap.org) starten.


