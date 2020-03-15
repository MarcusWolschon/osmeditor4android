# Einführung in Vespucci

Vespucci ist ein vollwertiger OpenStreetMap-Editor, der die meisten Funktionen von Desktop-Editoren beherrscht. Er wurde erfolgreich auf Googles Android 2.3 bis 10.0 und verschiedenen AOSP-basierten Varianten getestet. Achtung: Während die Leistung mobiler Geräte mit der von Desktop-Konkurrenten aufgeholt hat, haben vor allem ältere Geräte nur sehr begrenzten Speicher zur Verfügung und sind meist recht langsam. Bei der Verwendung von Vespucci sollten Sie dies berücksichtigen und z.B. die zu bearbeitenden Bereiche auf eine angemessene Größe beschränken. 

## Erstmaliger Gebrauch

On startup Vespucci shows you the "Download other location"/"Load Area" dialog after asking for the required permissions and displaying a welcome message. If you have coordinates displayed and want to download immediately, you can select the appropriate option and set the radius around the location that you want to download. Do not select a large area on slow devices. 

Alternativ kann das Formular mit "Zur Karte" geschlossen und direkt zur Karte gewechselt werden, hier das Gebiet, das bearbeitet werden soll, herangezoomt und dann die Daten dazu geladen werden (siehe unten "Mit Vespucci OSM-Daten bearbeiten").

## Mit Vespucci OSM-Daten bearbeiten

Abhängig von der Bildschirmgröße und dem Alter des Gerätes sind die Bearbeitungsfunktionen entweder über Icons in einer Leiste am Oberrand, über ein Dropdown-Menü rechts in dieser Leiste, über eine Menüleiste am Unterrand (falls vorhanden) oder über die Menütaste zugänglich.

<a id="download"></a>

### OSM-Daten herunterladen

Entweder das Icon zur Übertragung ![Transfer](../images/menu_transfer.png) oder den Menüeintrag "Übertragen" wählen. Danach werden sieben Optionen zur Auswahl angezeigt:

* **Download current view** - download the area visible on the screen and merge it with existing data *(requires network connectivity)*
* **Clear and download current view** - clear any data in memory and then download the area visible on the screen *(requires network connectivity)*
* **Upload data to OSM server** - upload edits to OpenStreetMap *(requires authentication)* *(requires network connectivity)*
* **Location based auto download** - download an area around the current geographic location automatically *(requires network connectivity or offline data)* *(requires GPS)*
* **Pan and zoom auto download** - download data for the currently displayed map area automatically *(requires network connectivity or offline data)* *(requires GPS)*
* **File...** - saving and loading OSM data to/from on device files.
* **Note/Bugs...** - download (automatically and manually) OSM Notes and "Bugs" from QA tools (currently OSMOSE) *(requires network connectivity)*

Um Daten auf dem Gerät zu öffnen, ist es am einfachsten, mit Gesten den Bildschirm auf das gewünschte Gebiet zu zentrieren und dann im Menü "Aktuelle Ansicht herunterladen" anzuwählen. Der Zoom kann mit Gesten, den Zoom-Schaltflächen oder den Lautstärketasten bedient werden. Vespucci sollte dann das Gebiet herunterladen. Um Daten herunterzuladen, muss man nicht angemeldet sein.

### Bearbeiten

<a id="lock"></a>

#### Sperren, entsperren, Modus wechseln

Um versehentliche Änderungen zu verhindern, startet Vespucci im "gesperrten" Modus, einem Modus, der nur das Zoomen und Verschieben der Karte erlaubt. Um den Schirm zu entsperren, auf das ![Schloss](../images/locked.png) Icon tippen.  

Ein langer Druck auf das Schlosssymbol zeigt ein Auswahlmenü mit 4 Optionen:

* **Normal** - der Standardbearbeitungsmodus, neue Objekte können erstellt, bestehende bearbeitet, verschoben und gelöscht werden. Ein einfaches weißes Schlosssymbol wird angezeigt.
* **Nur Tags** - wird ein bestehendes Objekt ausgewählt, startet der Tag-Editor, ein langer Druck auf dem Hauptschirm wird weiterhin neue Objekte hinzufügen, aber keine anderen Geometriebearbeitungen sind möglich. Ein weißes Schlosssymbol mit einem "T" wird angezeigt.
* **Innenraum** - schaltet in den Innenraum-Modus, siehe  [Innenraum-Modus](#indoor). Ein weißes Schlosssymbol mit einem "I" wird angezeigt.
* **C-Modus** - im C-Modus werden nur Objekte angezeigt, die ein Problem haben, siehe [C-Modus](#c-mode).  Ein weißes Schlosssymbol mit einem "C" wird angezeigt.

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
    * Ist das Positionskreuz nicht in der Nähe eines Knotens, erstellt das Berühren einen neuen Knoten. Falls die Position in der Nähe eines Weges (aber nicht in der Nähe eines Knotens) ist, wird der neue Knoten als Teil des Weges erstellt.
    * Ist das Positionskreuz in der Nähe (also innerhalb der Toleranzzone) eines Knotens, so wird kein neuer Knoten erstellt, sondern nur der vorhandene markiert und der Tag-Editor gestartet.
* Nochmaliges Berühren an einer anderen Stelle (außerhalb der Toleranzzone des Fadenkreuzes) fügt einen Wegabschnitt von der ursprünglichen zur momentanen Position hinzu. Befindet sich das Fadenkreuz in der Nähe eines Weges oder Knotens, wird der neue Abschnitt mit diesem Knoten oder Weg verbunden.

Um einen Weg zu verlängern, tippt man die Stellen an, an denen weitere Wegpunkte ergänzt werden sollen. Ein Doppelklick auf den letzen Knoten stellt den Weg fertig. Falls der Endpunkt auf einem Weg oder Knoten liegt, wird der Abschnitt automatisch mit diesen verbunden. 

Man kann auch einen Menüpunkt dafür verwenden: Weitere Informationen dazu, unter [Erstellen neuer Objekte](Creating%20new%20objects.md).

#### Flächen hinzufügen

Aktuell hat OpenStreetMap keinen eigenen Objekttyp für Flächen im Gegensatz zu anderen Geo-Datensystemen. Der Online-Editor "iD" versucht, aus den zugrundeliegenden OSM-Elementen eine Abstraktion der Flächen zu erstellen, was in bestimmten Fällen gut, in anderen nicht richtig funktioniert. Vespucci versucht das in der vorliegenden Version nicht, deshalb hier einige Informationen dazu wie in OSM Flächen abgebildet werden:

* _geschlossene Wege (*Polygone*)_: die einfachste und häufigste Flächenvariante sind Wege, die einen gemeinsamen Knoten als Anfangs- und Endpunkt besitzen und einen geschlossenen "Ring" bilden (die meisten Gebäude gehören z. B. zu diesem Typ). Solche Flächen sind mit Vespucci einfach zu erstellen. Um den Ring fertigzustellen, muss einfach der letzte Knoten auf dem ersten zu liegen kommen. Hinweis: die Interpretation von geschlossenen Wegen als Fläche hängt von ihren Tags ab. Ist, beispielsweise, ein geschlossener Weg als Gebäude getaggt, so wird es als Fläche interpretiert, ist der Weg als Kreisverkehr getaggt, nicht. In gewissen Fällen, in denen beide Interpretationen möglich wären, kann man dies durch ein "area"-Tag klären.
* _Multi-Polygone_: Es gibt Flächen, die aus mehreren Teilen, Löchern und Ringen bestehen, die  nicht mit nur einem Weg abgebildet werden können. OSM verwendet einen speziellen Typ der Relation (das OSM-Objekt, das Beziehungen zwischen mehreren Objekten abbilden kann), um das Problem zu lösen, ein Multi-Polygon. Ein Multi-Polygon kann mehrere äussere ("outer") und innere ("inner") Ringe besitzen. Jeder Ring kann entweder ein geschlossener Weg sein, wie oben beschrieben, oder mehrere Einzelwege mit gemeinsamen Endpunkten. Während große Multi-Polygone mit jedem Werkzeug schwer zu bearbeiten sind, können kleine einfach mit Vespucci erstellt werden 
* _Küstenlinien_:  bei sehr großen Flächen, Kontinenten und Inseln, versagt auch das Multi-Polygon-Modell. Bei Küstenlinien (natural=coastline) verwenden wir eine von der Ausrichtung abhängige Bedeutung: die Landfläche befindet sich auf der linken Seite des Weges, das Wasser auf der rechten. Als Begleiterscheinung darf demnach die Richtung eines Weges, der die Küstenlinie markiert, nicht umgekehrt werden. Mehr Information dazu findet man im [OSM-Wiki](http://wiki.openstreetmap.org/wiki/Tag:natural%3Dcoastline).

#### Die Geometrie eines Weges verbessern

Zoomt man genügend nah an ein ausgewähltes Wegsegment, wird ein kleines "x" sichtbar. Zieht man daran, wird ein Knoten im Weg erstellt. Hinweis: um das versehentliche Erstellen solcher Punkte zu verhindern, ist der empfindliche Bereich um das "x" ziemlich klein.

#### Kopieren, Ausschneiden & Einfügen

Ausgewählte Knoten und Wege können kopiert oder ausgeschnitten und dann einmal oder mehrfach wieder eingefügt werden. Beim Ausschneiden bleibt sowohl die OSM-ID als auch die Version erhalten. Ein langer Druck markiert die Position, an der eingefügt werden soll, um die Aktion abzuschließen, danach "Einfügen" aus dem Menü auswählen.

#### Effizient Adressen eintragen

Vespucci besitzt eine ![Address](../images/address.png) Funktion "Adresseigenschaften hinzufügen", die durch Vorhersage fortlaufender Hausnummern versucht, die Adresserfassung effizienter zu machen. Sie kann ausgewählt werden:

* nach langem Drücken: Vespucci erstellt einen Knoten an der markierten Stelle, versucht, eine wahrscheinliche Hausnummer vorherzusagen, und schlägt weitere, kürzlich verwendete Adresswerte vor. Falls der Punkt auf einem Gebäudeumriss liegt, wird automatisch ein Knoten mit dem Tag "entrance=yes" erstellt. Dann wird der Eigenschaftseditor gestartet, um allfällige Korrekturen und weitere Änderungen zu ermöglichen. 
*  in den Modi "Knoten/Weg ausgewählt": Vespucci fügt, wie oben beschrieben, Adresswerte hinzu und startet den Eigenschaftseditor.
* im Eigenschaftseditor.

Die Hausnummernvorhersage benötigt typischerweise mindestens die Eingabe von je 2 Hausnummern auf jeder Seite der Straße, je mehr Nummern in den Daten vorhanden sind desto besser funktioniert die Vorhersage. 

Es ist sinnvoll, dafür den [Auto-Download](#download)-Modus zu verwenden.  

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

Vespucci hat einen einfachen Konfliktbehebungsmechanismus eingebaut. Sind grössere Probleme mit den Änderungen zu erwarten, empfehlen wir, sie in eine .osc-Datei zu speichern ("Transfer" Menü, "Datei...,"  "Änderungen exportieren") und die Konflikte dann mit JOSM zu beheben. Für Details siehe Hilfetext zur [Konfliktauflösung](../en/Conflict%20resolution.md).  

## GPS verwenden

Vespucci kann auch einen GPX-Track erstellen und auf dem Schirm anzeigen. Des Weiteren kann auch die aktuelle GPS-Position ("GPS-Position anzeigen" im Menü) angezeigt werden und/oder der Bildschirm darauf zentriert und nachgeführt werden ("GPS-Position folgen").   

Falls Letzteres eingeschaltet ist, wird es beim Verschieben des Schirms oder beim Editieren abgeschaltet und der blaue "GPS-Pfeil" ändert sich vom Umriss in einen ausgefüllten Pfeil. Um wieder in den Nachführmodus zu gelangen, genügt es die "GPS-Schalftfläche" zu berühren oder im Menü wieder die Option auszuwählen.

## Notizen und Fehler

Vespucci unterstützt das Herunterladen, Kommentieren und Schliessen von OSM-Notizen (vormals OSM Bugs) und die entsprechende Funktionalität für "Fehler" die vom [OSMOSE Qualitätssicherungswerkzeug](http://osmose.openstreetmap.fr/en/map/) gemeldet werden. Beide müssen entweder explizit heruntergeladen werden oder die Notizen und Fehler in der Nähe können automatisch geladen werden. Geänderte oder geschlossene Notizen und Fehler können entweder sofort hochgeladen werden oder gespeichert und alle zusammen später hochgeladen werden.

Auf der Karte werden die Notizen und Fehler mit einem kleinen Käfer-Symbol  ![Bug](../images/bug_open.png) angezeigt, grüne sind behoben, blaue sind neu erstellt oder geändert, und Gelb zeigt an, dass die Notiz respektive der Fehler noch unverändert aktiv ist. 

In der OSMOSE-Fehlerdarstellung wird für die betroffenen Objekte jeweils ein blau hervorgehobener Link angezeigt. Wählt man den Link an, wird das Objekt ausgewählt, der Bildschirm darauf zentriert und, falls nötig, das entsprechende Gebiet heruntergeladen. 

### Anzeigefilter

Zusätzlich zum Freischalten der Notiz- und Fehleranzeige kann eine grobe Auswahl der angezeigten Objekte eingestellt werden. In den "Erweiterten Einstellungen" kann Folgendes ausgewählt werden:   

<item>Notizen</item>
<item>Osmose-Fehler</item>
<item>Osmose-Warnung</item>
<item>Osmose-Hinweis</item>
<item>Benutzerdefiniert</item>

<a id="indoor"></a>

## Innenraum-Modus

In Innenräumen Daten zu erfassen ist anspruchsvoll aufgrund der grossen Anzahl von Objekten, die sich häufig überlappen. Vespucci hat einen speziellen Innenraum-Modus, der es ermöglicht, alle Objekte, die nicht auf der gleichen Etage sind, auszufiltern, und der bei neu erstellten Objekten automatisch die richtige Etage in den Objekteigenschaften einträgt. 

In den Modus kann durch einen langen Druck auf das Schlosssymbol gewechselt werden, siehe auch [Sperren, entsperren, Modus wechseln](#lock).

<a id="c-mode"></a>

## C-Modus

Im C-Modus werden nur Objekte angezeigt, die ein Problem haben, was das Finden von Objekten, die spezifische Probleme haben oder zu  konfigurierbaren Qualitätsprüfungen passen, erleichtert. Wird ein solches Objekt angewählt, startet der Eigenschaftseditor und die am besten passende Vorlage wird automatisch angewandt.

In den Modus kann durch einen langen Druck auf das Schlosssymbol gewechselt werden, siehe auch [Sperren, entsperren, Modus wechseln](#lock).

### Prüfungen konfigurieren

Im Augenblick sind zwei der Tests konfigurierbar, beide können durch Auswahl der "Validierungseinstellungen" in den "Einstellungen" geändert werden. (Die Tests für FIXME-Tags und  für fehlende "type"-Tags bei Relationen sind derzeit nicht konfigurierbar.)  

Die Liste ist zweigeteilt, die obere Hälfte enthält die "Überprüfungstests", die untere die Tests auf "Fehlende Tags". Einträge können durch anklicken bearbeitet werden, der grüne Menüknopf erlaubt es weitere Einträge hinzuzufügen.

#### Einträge für Elemente die überprüft werden sollten

Überprüfungstest-Einträge haben die folgenden Eigenschaften:

* **Key** - Key of the tag of interest.
* **Value** - Value the tag of interest should have, if empty the tag value will be ignored.
* **Age** - how many days after the element was last changed the element should be re-surveyed, if a _check_date_ tag is present that will be the used, otherwise the date the current version was create. Setting the value to zero will lead to the check simply matching against key and value.
* **Regular expression** - if checked **Value** is assumed to be a JAVA regular expression.

"Schlüssel" und "Wert" werden mit den _existierenden_ Tags des Objektes verglichen.

The _Annotations_ group in the standard presets contain an item that will automatically add a _check_date_ tag with the current date.

#### Tests auf fehlende Tags

Prüfungen auf fehlende Tag-Einträge haben die folgenden zwei Eigenschaften:

* **Schlüssel** - Schlüssel, den ein Objekt gemäß den Voreinstellungen haben sollte.* **Optional erforderlich** - Erforderlicher Schlüssel, selbst wenn er gemäß den Voreinstellungen zu den optionalen Tags gehört.

Diese Prüfung funktioniert, indem zuerst die zugehörige Voreinstellung bestimmt und dann geprüft wird, ob **Schlüssel** ein gemäß den Voreinstellungen "empfohlener" Schlüssel für dieses Objekt ist. ****Optional erforderlich** erweitert die Prüfung auf Tags, die "optional" für das Objekt sind. Hinweis: aktuell verlinkte Vorlagen werden nicht geprüft.

## Filter

### Auf Tags basierende Filter

Der Filter kann vom Hauptmenü aus eingeschaltet werden, durch Tippen auf die Filter-Schaltfläche kann er dann bearbeitet werden.. Mehr Informationen findet man unter [Tag-Filter](../en/Tag%20filter.md).

### Auf Vorlagen basierende Filter

Eine Alternative zu obigem. Objekte werden entweder durch Einzelvorlagen oder Vorlagengruppen gefiltert. Nach einem Tipp auf die Filter-Schaltfläche wird ein Dialog zur  Vorlagenauswahl angezeigt, ähnlich wie er an anderer Stelle in Vespucci benutzt wird. Einzelne Vorlagen können durch einen normalen Klick ausgewählt werden, Vorlagengruppen durch einen langen Klick (normaler Klick öffnet die Gruppe). Mehr Informationen unter [Vorlagenfilter](../en/Preset%20filter.md).

## Vespucci individuell anpassen

Viele Gesichtspunkte der App können angepasst werden. Wenn man nach etwas Bestimmtem sucht und es nicht findet, die [Vespucci Website](https://vespucci.io/) ist durchsuchbar und enthält zusätzliche Informationen, was auf dem Gerät möglich ist.

### Ebeneneigenschaften

Die Ebeneneigenschaften können über den Ebenen-Schalter (obere rechte Ecke) geändert werden, alle anderen Festlegungen sind über die Einstellungen im Hauptmenü zugänglich.

* Hintergrundebene - Es steht eine große Anzahl von Luft- und Satellitenaufnahmen zur Verfügung, die Standardeinstellung hierfür ist die Karte von openstreetmap.org im "Standard Style".
* Überlagerungsebene - Dies sind halbtransparente Ebenen mit zusätzlichen Informationen wie z. B. GPX-Tracks. Das Hinzufügen kann auf älteren Geräten und solchen mit wenig Speicher zu Problemen führen. Standard: keine.
* Anzeige von Hinweisen/Fehlern - Offene Hinweise und Fehler werden durch einen gelben, geschlossene durch einen grünen Käfer angezeigt. Standard: ein.
* Fotoebene - Zeigt geo-referenzierte Fotos als rotes Kamera-Symbol an. Falls Informationen zur Ausrichtung enthalten sind, wird das Symbol entsprechend gedreht. Standard: aus.

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

Wenn Vespucci abstürzt oder einen internen Fehler feststellt, fordert die App dazu auf einen Fehlerbericht einzusenden. Dies sollte man in diesen Fällen tun, aber bitte nur einmal für jede spezifische Situation. Will man Informationen ergänzen, eine Funktion vorschlagen oder Ähnliches, kann das hier geschehen: [Vespucci Fehlerberichte] (https://github.com/MarcusWolschon/osmeditor4android/issues). Der "Informationen zur Problembehebung"- Button im Debug-Menü erstellt einen neuen Fehlerbericht und fügt die relevanten App- und Geräteinformationen hinzu - ohne extra Tipparbeit.

Wer Themen zu Vespucci diskutieren will, kann  eine Diskussion in der [Vespucci Google Gruppe](https://groups.google.com/forum/#!forum/osmeditor4android) oder im [OpenStreetMap Android Forum](http://forum.openstreetmap.org/viewforum.php?id=56) beginnen.


