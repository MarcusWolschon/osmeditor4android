# OpenStreetMap Öffnungszeiten-Editor

Die OpenStreetMap Öffnungszeitenspezifikation ist ziemlich komplex und eignet sich nicht ohne weiteres für eine einfache und intuitive Benutzeroberfläche.

Aber meist wird nur ein kleiner Teil der Spezifikation benötigt. Der Editor berücksichtigt dies, lagert selten benötigte Funktionen in Menüs aus und verwendet vordefinierte Vorlagen.

_Diese Dokumentation ist vorläufig und eine laufende Arbeit_

## Verwenden des Editors für die Öffnungszeiten

In einem typischen Arbeitsablauf enthält das Objekt, das Sie bearbeiten, entweder bereits ein Tag für die Öffnungszeiten (opening_hours, service_times und collection_times) oder Sie können die Voreinstellung für das Objekt erneut anwenden, um ein leeres Feld für die Öffnungszeiten zu erhalten. Wenn Sie das Feld manuell hinzufügen müssen und Vespucci verwenden, können Sie den Schlüssel auf der Detailseite eingeben und zur Bearbeitung auf die formularbasierte Registerkarte wechseln. Wenn Sie der Meinung sind, dass das Tag für die Öffnungszeiten Teil des Presets gewesen sein sollte, öffnen Sie eine Fehlermeldung für Ihren Editor.

Wenn Sie eine Standardvorlage definiert haben (tun Sie dies über den Menüpunkt "Vorlagen verwalten"), wird diese automatisch geladen, wenn der Editor mit einem leeren Wert gestartet wird. Mit der Funktion "Vorlage laden" können Sie jede gespeicherte Vorlage laden und mit dem Menü "Vorlage speichern" den aktuellen Wert als Vorlage speichern. Sie können für die Tags "opening_hours", "collection_times" und "service_times" separate Vorlagen und Standardwerte definieren.

Natürlich können Sie einen Wert für die Öffnungszeiten von Grund auf erstellen, wir empfehlen jedoch, eine der vorhandenen Vorlagen als Ausgangspunkt zu verwenden.

Wenn ein existierender Öffnungszeiten-Wert geladen wird, wird versucht diesen automatisch an die Öffnungszeiten-Spezifikationen anzupassen. Wenn das nicht möglich ist, wird die ungefähre Stelle des Fehlers angezeigt und Sie können versuchen den Fehler manuell zu korrigieren. Ungefähr ein Viertel der Öffnungszeiten-Werte hat Fehler, aber weniger als 10% können nicht korrigiert werden. Siehe [OpeningHoursParser](https://github.com/simonpoole/OpeningHoursParser) für weitere Informationen welche Abweichungen von den Spezifikationen toleriert werden.

### Hauptmenü-Schaltfläche

* __Regel hinzufügen__: eine neue Regel hinzufügen.
* __Regel für Urlaub hinzufügen__: Eine neue Regel für Urlaub hinzufügen mit einem Statuswechsel.
* __Regel für 24/7 hinzufügen__: eine Regel für ein Objekt hinzufügen das Rund-um-die Uhr geöffnet ist. Die Öffnungszeiten-Regeln erlauben keine weiteren Regeln auf niedrigerer Ebene, aber es sind Regeln auf höherer Ebene möglich (z.B. Jahresbereiche)
* __Vorlage laden__: eine Vorlage laden.
* __Speichern als Vorlage__: Speichern der aktuellen Öffnungszeiten-Werte als Vorlage zur späteren Verwendung.
* __Vorlagen verwalten__: Bearbeiten, z.B. den Namen ändern oder Löschen von vorhandenen Vorlagen.
* __Aktualisieren__: Neuanalyse des Öffnungszeiten-Wertes.
* __Alle löschen__: Löschen aller Regeln.

### Regeln

Standard-Regeln werden als _normale_ Regeln hinzugefügt. Dies bedeutet, dass diese vorherige Regeln für den gleichen Tag überschreiben. Dies kann ein Problem sein beim Anlegen von erweiterten Zeiten, dann sollte die Regel über das _Zeige Regel-Type_ Menü auf _Hinzufügen_ geändert werden.

#### Regelmenü

* __Modifizierer/Kommentar hinzufügen__: Verändern des Effekts dieser Regel und Hinzufügen eines optionalen Kommentars.
* __Ferien hinzufügen__: Hinzufügen eines Auswahlschalters für öffentliche Ferien oder Schulferien.
* __Zeitspanne hinzufügen...__
    * __Zeit - Zeit__: eine Start- und Endzeit am selben Tag.
    * __Zeit - erweiterte Zeit__: eine Startzeit und am nächsten Tag eine Endzeit (Beispiel 26:00 ist 02:00 Uhr am nächsten Tag).
    * __variable Zeit - Zeit__: eine variable Startzeit (Morgen-/Abendämmerung, Sonnenauf-/untergang) und eine Endzeit am selben Tag.
    * __variable Zeit - erweiterte Zeit__: eine variable Startzeit und am nächsten Tag eine Endzeit.
    * __Zeit - variable Zeit__: eine Startzeit und eine variable Endzeit.
    * __variable Zeit - variable Zeit__: eine variable Startzeit und eine variable Endzeit.
    * __Zeit__: ein Zeitpunkt.
    * __Zeit - offene Endzeit__: eine Startzeit und eine offene Endzeit.
    * __variable Zeit__: zu einer variablen Zeit
    * __variable Zeit - offenes Ende__: eine variable Startzeit und eine offene Endzeit.
* __Wochtag-Auswahlschalter hinzufügen__: Hinzufügen eines Wochentag-Auswahlschalters.
* __Datumsbereich hinzufügen...__
    * __Datum - Datum__: ein Startdatum (Jahr, Monat, Tag) und ein Enddatum.
    * __variables Datum - Datum__: ein variables Startdatum (die aktuellen Spezifikationen definieren nur _Ostern_) und ein Enddatum.
    * __Datum - variables Datum__: ein Startdatum und ein variables Enddatum.
    * __variables Datum - variables Datum__: ein variables Startdatum und ein variables Enddatum.
    * __Vorkommen im Monat - Vorkommen im Monat__: ein Startvorkommen im Monat und ein Endvorkommen im Monat.
    * __Vorkommen im Monat - Datum__: ein Startvorkommen im Monat und ein Enddatum.
    * __Datum - Vorkommen im Monat__: ein Startdatum und ein Endvorkommen im Monat.
    * __Vorkommen im Monat - variables Datum__: ein Startvorkommen im Monat und ein variables Enddatum.
    * __Variables Datum - Vorkommen im Monat__: ein variables Startdatum und ein Endvorkommen im Monat.
    * __Datum - offenes Ende__: ein Startdatum und ein offenes Enddatum.
    * __variables Datum - offenes Ende__: ein variables Startdatum und ein offenes Ende.
    * __Vorkommen im Monat - offenes Ende__: ein Startvorkommen im Monat und ein offenes Ende.
    * __Mit Versatz...__: Die gleichen Angaben wie vorher aber mit angegebenen Versatz (wird nur selten verwendet).
* __Jahresbereich hinzufügen__: Jahres-Auswahlschalter hinzufügen.
* __Wochenbereich hinzufügen__: Wochen-Auswahlschalter hinzufügen.
* __Kopieren__: Kopieren einer Regel und Einfügen der Kopie nach der aktuellen Position.
* __Regeltype anzeigen__: Anzeigen der Regel und die Möglichkeit den Regeltype zu ändern _Normal_, _Zusätzlich_ und _Rückfall_ (nicht verfügbar für die erste Regel).
* __Nach oben bewegen__: Bewegt die Regel eine Position nach oben (nicht verfügbar für die erste Regel).
* __Nach unten bewegen__: Bewegt die Regel eine Position nach unten.
* __Löschen__: diese Regel löschen.

### Zeitspannen

Um das Eintragen von Zeitspannen so einfach wie möglich zu machen, haben wir für existierende Öffnungszeiten versucht einen optimalen Zeitraum und -einteilung für den Zeitschieber zu finden. Neue Zeitschieber beginnen um 6:00 Uhr und haben eine 15-Minuten Einteilung, welche im Menü geändert werden können.

Klicken auf die Zeitleiste (nicht auf die Nadeln) öffnet den grossen Zeitauswähler falls es zu schwierig ist die Leiten direkt zu brauchen. Da der Zeitauswähler Zeiten bis in den nächsten Tag anzeigt, ist dies auch ein einfacher Weg einen Zeitbereich zu erweitern, ohne den Bereich zu löschen und wieder einzufügen. 

#### Zeitspannenmenü

* __Anzeige des Zeitauswählers__: Zeigt einen großen Zeitauswählern um Start- und Endzeit auszuwählen. Auf sehr kleinen Displays kann dies die bevorzugte Eingabemethode für Zeiten sein.
* __Wechsel zu 15-Minuten Einteilung__: Verwenden einer 15-Minuten Einteilung für den Zeitschieber.
* __Wechsel zu 5-Minuten Einteilung__: Verwenden einer 5-Minuten Einteilung für den Zeitschieber.
* __Wechsel zu 1-Minuten Einteilung__: Verwenden einer 1-Minuten Einteilung für den Zeitschieber, sehr schwer zu verwenden auf einem Smartphone.
* __Start um Mitternacht__: Start des Zeitschiebers um Mitternacht.
* __Intervall anzeigen__: Intervallfeld zum Festlegen eines Intervalls in Minuten anzeigen.
* __Löschen__: diese Zeitspanne löschen.

