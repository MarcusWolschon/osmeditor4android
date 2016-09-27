# Einführung in Vespucci

Vespucci ist ein Editor für OpenStreetMap der die meisten Funktionen unterstützt die ähnliche Programme auf normalen Rechnern vorhanden sind und es ist erfolgreich auf googles Android 2.3 bis 6.0 und verschiedene auf AOSP basierende Varianten getestet worden. Wichtig: während die Leistung von Handys und Tablets Ihre stationären Konkurrenten auf vielen Gebieten eingeholt hat muss man, vor allem bei älteren Geräten, auch nicht vergessen, dass sie eher wenig Hauptspeicher zur Verfügung haben und auch deutlich langsamer sein können bei bestimmten Operationen. Deshalb sollte man beim Editieren dies im Auge behalten und, zum Beispiel, die Grösse der editierten Gebiete in vernünftige Grössen halten.   

## Erstmaliger Gebrauch

Beim erstmaligen Starten zeigt Vespucci das "Herunterladen einer anderen Position"/"Bereich laden" Formular. Falls Koordinaten angezeigt werden und sofort heruntergeladen werden soll, kann die entsprechende Option gewählt und den Radius um den Punkt herum gesetzt werden. Auf langsamen Geräten sollte nur ein kleiner Bereich ausgewählt werden. 

Alternativ kann mit dem "Zur Karte" direkt zur Karte gewechselt werden, auf das Gebiet das bearbeitet werden soll gezoomt werden und dann Daten für das Gebiet geladen werden (siehe unten "Mit Vespucci OSM Daten bearbeiten").


## Mit Vespucci OSM Daten bearbeiten

Abhängig der Bildschirmgrösse und Alter des Gerätes können die Bearbeitungsfunktionen über Icons in der obersten Menüzeile, via ein Menü oben rechts, von der unteren Menüzeile (falls vorhanden) oder mittels der Menütaste zugänglich sein.

### OSM Daten herunterladen

Wähle entweder das Übertragungs-Icon ![ ](../images/menu_transfer.png) oder den "Transfer" Menüeintrag Dies wird sieben Optionen zur Auswahl anzeigen:

* **Aktuelle Ansicht herunterladen** - den aktuell sichtbaren Bildschirmbereich herunterladen und allfällig vorhandene Daten ersetzen *(benötigt Netzzugang)*
* **Diese Ansicht zu dn Daten dazu laden** - den aktuell sichtbaren Bildschirmbereich auf dem Schirm herunterladen und mit vorhandene Daten  zusammenführen *(benötigt Netzzugang)*
* **Herunterladen einer anderen Position** - zeigt ein Formular an, dass es erlaubt nach einem Ort zu suchen, Koordinaten einzugeben. oder direkt um die aktuelle Position einen Bereich herunterzuladen  *(benötigt Netzzugang)*
* **Daten zum OSM-Server hochladen** - lädt die Änderungen zum OSM-Server hoch *(Konto benötigt)*  *(benötigt Netzzugang)*
* **Automatischer Download** - lädt automatisch einen Bereich um die aktuelle Position herunter  *(benötigt Netzzugang)* *(benötigt GPS)*
* **Datei...** - Speichern und Laden von OSM Daten zu Dateien auf dem Gerät.
* **Notizen/Fehler...** - herunterladen (automatisch und manuell) von OSM Notizen un "Fehlern" von QA Werkzeugen (aktuell OSMOSE) *(benötigt Netzzugang)*

Der einfachste Weg um Daten auf dem Gerät zu öffnen ist mit Gesten den Bildschirm auf das gewünschte Gebiet zu zentrieren und dann "Aktuelle Ansicht herunterladen" im Menü anzuwählen. Mit Gesten, den "- | +" Knöpfen oder den Laut-/Leisetasten kann gezoomt werden. Vespucci sollte dann das Gebiet herunterladen. Um Daten herunterzuladen muss man nicht angemeldet sein.

### Bearbeiten

Um versehentliche Änderungen zu verhindern startet Vespucci im "gesperrten" Modus, einen Modus der nur das Zoomen und Verschieben der Karte erlaubt. Tippe auf das ![Schloss](../images/locked.png) Icon um den Schirm zu entsperren. Ein langer Druck auf das Schloss schaltet in einen Modus um in dem nur die Eigenschaften der Elemente geändert werden kann, aber keine neuen Objekte erstellt oder die Geometrien geändert werden können. Dieser Modus wird mit einem weissen Schloss mit kleinem "T" angezeigt. 

In der Standardeinstellung wird um auswählbare Punkte und Wege ein oranger Bereich angezeigt, der angibt in welchen Bereich man um den Bildschirm tippen kann um ein Objekt anzuwählen. Ist die Auswahl nicht eindeutig zeigt Vespucci ein Menü mit den auswählbaren Objekte in der Nähe. Einmal ausgewählte Objekte werden mit Gelb hervorgehoben.

In Gebieten in denen die OSM Daten sehr dicht sind ist es sinnvoll vor dem Bearbeiten weit hineinzuzoomen.

Vespucci hat eine gute Unterstützung von "Undo" und "Redo" deshalb kann man angstfrei auf seinem Gerät experimentieren, bitte aber keine reinen Testdaten auf den OSM Server speichern.

#### Auswählen / Abwählen

Tippe auf ein Objekt um es anzuwählen und hervorzuheben, ein zweites mal Anwählen öffnet den "Eigenschaftseditor" für das Element. Um ein Objekt abzuwählen tippe einfach in ein leeren Bereich. Um ein anderes Element anzuwählen tippe einfach darauf, es ist nicht nötig zuerst das aktuell angewählte abzuwählen. Ein "Doppeltipp" startet den [Mehrfachauswahl Modus](../en/Multiselect.md).

#### Einen neuen Knoten oder Weg erstellen

Ein langer Druck markiert die Position mit einem schwarzen Kreuz wo der Weg beginnt oder ein Punkt erstellt werden soll. Nochmaliges Berühren an der gleichen Stelle erstellt einen Punkt, berührt man den Schirm etwas weiter weg, wird ein Wegsegment dorthin erstellt. 

Um den Weg zu verlängern tippe an den Stellen wo du weitere Wegpunkte haben willst. Um den Weg fertigzustellen, tippe nochmals auf den letzten Punkt. Falls die Anfangs- und Endpunkte auf einem anderen Weg liegen werden sie automatisch in diesen integriert.

#### Einen Knoten oder Weg verschieben

Objekte können erst verschoben werden nachdem sie ausgewählt wurden. In den Einstellungen kann für Punkte einen grossen Bereich anzeigen lassen mit dem der Punkt leichter verschoben werden kann als mit der Standardeinstellung. 

#### Die Geometrie eines Weges verbessern

Zoomt man genügend nah an ein Wegsegment wird ein kleines "x" sichtbar. Zieht man dran wird ein Knoten im Weg erstellt. Hinweis: um das versehentliche Erstellen solcher Punkte zu verhindern ist der empfindliche Bereich um das "x" ziemlich klein.

#### Kopieren, Ausschneiden & Einfügen

Ausgewählte Knoten und Wege können kopiert oder ausgeschnitten werden, und dann ein- oder mehrmals wieder eingefügt werden. Ausschneiden erhält sowohl die OSM Id wie auch die Version. Ein langer Druck markiert die Position an der eingefügt werden soll, um die Aktion auszulösen danach "Einfügen" aus dem Menü auswählen.

#### Effizient Adressen eintragen

Vespucci hat eine "Adresseigenschaften hinzufügen" Funktion, die versucht Adresserfassung schneller und effizienter zu machen. Die Funktion kann ausgewählt werden 

* nach einem langen Druck: Vespucci erstellt dann einen Knoten an der markierten Stelle, versucht eine wahrscheinliche Hausnummer vorherzusagen  und schlägt weitere aktuell verwendete Adresswerte vor.  Falls der Punkt auf einem Gebäudeumriss liegt wird automatisch ein Eingang erstellt. Der Eigenschaftseditor wird dann gestartet um allfällige Korrekturen und andere Änderungen zu ermöglichen. 
*  in den Knoten/Weg ausgewählt Modi:: Vespucci fügt Eigenschaften wie oben beschrieben hinzu und startet den Eigenschaftseditor.
* im Eigenschaftseditor.

Die Hausnummernvorhersage benötigt typischerweise mindestens die Eingabe von je 2 Hausnummern auf jede Seite der Strasse, je mehr Nummern in den Daten vorhanden sind desto besser funktioniert die Vorhersage. 

Es ist sinnvoll dies mit dem "Automatischen Download" zu verwenden.  

#### Abbiegebeschränkungen eintragen

Vespucci erlaubt es schnell Abbiegebeschränkungen hinzuzufügen. Hinweis: falls dazu Wege aufgetrennt werden müssen, muss dass vorher geschehen.

* wähle ein Weg mit "highway" Eigenschaft aus (Abbiegebeschränkungen können nur mit solchen Wegen erstellt werden, falls dies für andere Wege gemacht werden soll kann das mit der allgemeineren "Relation erstellen" Funktion gemacht werden)
* wähle "Abbiegebeschränkung hinzufügen" im Menü (falls es keins möglichen "via" Elemente gibt, wird der Menüeintrag auch nicht angezeigt)
* wähle den "via" Knoten oder Weg aus (nur die möglichen "via" Elemente werden den Touchbereich angezeigt haben) 
* wähle den "to" Weg aus (es ist auch möglich den ursprünglichen "from" Weg auszuwählen. Vespucci nimmt dann an, dass eine "no_u_turn" Beschränkung erstellt werden soll)
* setzte den Typ der Abbiegebeschränkung im Eigenschaftseditor

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

Falls letzteres eingeschaltet ist, wird es beim Verschieben des Schirms oder beim Editieren abgeschaltet und der blaue "GPS-Pfeil" ändert sich vom Umriss auf einen ausgefüllten Pfeil. Um wieder in den Nachführmodus zu gelangen genügt es den Pfeil zu berühren, oder die Option im Menü wieder auszuwählen.

## Notizen und Fehler

Vespucci unterstützt das Herunterladen, Kommentieren und Schliessen von OSM Notizen (vormals OSM Bugs) und die entsprechende Funktionalität für "Fehler" die vom [OSMOSE Qualitätssicherungswerkzeug](http://osmose.openstreetmap.fr/en/map/) gemeldet werden. Beide müssen entweder explizit heruntergeladen werden oder die Notizen und Fehler in der Nähe können automatisch geladen werden. Geänderte oder geschlossene Notizen und Fehler können entweder sofort hochgeladen werden oder gespeichert und alle zusammen später hochgeladen werden.

Auf der Karte werden die Notizen und Fehler werden mit einem kleinen Käfer Icon  ![ ](../images/bug_open.png) angezeigt, grüne sind behoben, blaue sind neu erstellt oder geändert, und Gelb zeigt an, dass die Notiz respektive der Fehler noch unverändert aktiv ist. 

In der Anzeige von OSMOSE Fehler wird jeweils für die betroffenen Objekte ein blau hervorgehobener Link angezeigt, wählt man den Link an, wird das Objekt ausgewählt, der Bildschirm darauf zentriert und, falls nötig, das entsprechende Gebiet heruntergeladen. 

## Vespucci individuell anpassen

### Häufig geänderte Einstellungen

* Kartenhintergrund
* Karten Overlay. Dies kann Probleme mit älteren Geräten und solchen mit wenig Hauptspeicher verursachen. Standardwert: kein Overlay.
* Fehler und Notizen anzeigen. Offene Notizen und Fehler werden mit einem gelben Käfer angezeigt, geschlossene in grün. Standardwert: eingeschaltet.
* Foto Layer. Zeigt verortete Fotos die auf dem Gerät gefunden werden als Kamera Icon an, falls Richtungsinformationen vorhanden sind wird die Kamera entsprechend gedreht. Standardwert: ausgeschaltet.
* Icons für Knoten mit Eigenschaften. Standardwert: eingeschaltet.
* Bildschirm nicht abschalten. Standardwert: ausgeschaltet.
* Grosser Bereich um Punkte zu bewegen. Auf Geräten mit Touchscreen ist das Verschieben von Knoten schwierig da die Finger die Sicht auf den Knoten versperren. Eingeschaltet zeigt diese Option einen grossen Bereich um ausgewählte Knoten herum der für das Verschieben verwendet werden kann. Den normale kleine Bereich wird weiterhin für die Objektauswahl und andere Funktionen verwendet. Standardwert: ausgeschaltet.

Erweiterte Einstellungen

* Geteilte Action Bar erlauben. Auf aktuelleren Telefonen wird der "Action Bar" in einen oberen und unteren Teil geteilt, dabei sind to Icons im unteren Teil. Typischerweise erlaubt dies etwas mehr Icons anzuzeigen, allerdings auf Kosten der verfügbaren Bildschirmfläche. Wird die Option abgewählt werden die Icons im oberen Balken angezeigt. Hinweis: Vespucci muss neugestartet werden so das die Änderung aktiv wird.
* Immer Kontext Menü benutzen. Eingeschaltet wird bei jedem Auswahlprozess das Kontext Menü angezeigt, ausgeschaltet nur dann wenn keine eindeutige Auswahl bestimmt werden kann. Standardwert: aus (früher: ein).
* Helles Thema verwenden. Auf aktuellen Geräten standardmässig eingeschaltet. Kann auch auf älteren Geräten eingeschaltet werden ist aber eventuell nicht vollständig konsistent.
* Zeige Statistiken. Zeigt einige Werte für die Fehlerbehebung an, nicht wirklich nützlich. Standardwert: ausgeschaltet (früher: eingeschaltet).  

## Fehler melden

Falls Vespucci abstürzt oder einen inkonsistenten Zustand entdeckt wird erscheint eine Aufforderung eine Fehlermeldung einzuschicken. Bitte komme der Aufforderung nach, aber nur einmal per spezifischen Ereignis.  Falls du mehr Input geben willst oder einen Verbesserungsvorschlag hast, erstelle bitte hier: [Vespucci Issue Tracker](https://github.com/MarcusWolschon/osmeditor4android/issues) einen neuen Eintrag. Falls du zu Vespucci eine Diskussion beginnen willst, kannst du dies entweder auf der [Vespucci google group](https://groups.google.com/forum/#!forum/osmeditor4android) oder dem [OpenStreetMap Android Forum](http://forum.openstreetmap.org/viewforum.php?id=56) machen.


