# OpenStreetMap åpningstid-redigerer

OpenStreetMap-åpningstidenes spesifikasjoner er ganske kompliserte og gir seg ikke lett til et enkelt og intuitivt brukergrensesnitt.

Men for det meste bruker du sannsynligvis bare en liten del av definisjonen. Redaktøren tar dette med i betraktningen ved å prøve å skjule de mer obskure funksjonene i menyer, og mesteparten av tiden reduserer bruken "på veien" til små tilpasninger av forhåndsdefinerte maler.

_Denne dokumentasjonen er foreløpig og et arbeid som pågår_

## Bruker åpningstid redigerer

In a typical workflow the object you are editing will either already have an opening hours tag (opening_hours, service_times and collection_times) or you can re-apply the preset for the object to get an empty opening hours field. If you need to add the field manually and you are using Vespucci you can enter the key on the details page and then switch back to the form based tab to edit. If you believe that the opening hours tag should have been part of the preset, please open an issue for your editor.

If you have defined a default template (do this via the "Manage templates" menu item) it will be loaded automatically when the editor is started with an empty value. With the "Load template" function you can load any saved template and with the "Save template" menu you can save the current value as a template. You can define separate templates and defaults for the "opening_hours", "collection_times" and "service_times" tags. 

Naturally you can build an opening hours value from scratch, but we would recommend using one of the existing templates as a starting point.

If an existing opening hours value is loaded, an attempt is made to auto-correct it to conform to the opening hours specification. If that is not possible the rough location where the error occurred will be highlighted in the display of the raw OH value and you can try and correct it manually. Roughly a quarter of the OH values in the OpenStreetMap database have problems, but less than 10% can't be corrected, see [OpeningHoursParser](https://github.com/simonpoole/OpeningHoursParser) for more information on what deviations from the specification are tolerated.

### Hovedmenyknapp

* __Legg til regel__: legg til en ny regel.
* __Legg til regel for helligdager__: legg til en ny regel for en ferie sammen med en statlig endring.
* __Legg til regel for 24 / 7__: legg til en regel for et objekt som alltid er åpent, åpningstid spesifikasjonen støtter ikke andre underverdier for 24/7, men vi tillater å legge til høyere nivånivåer (for eksempel års områder).
* __Last mal__: last inn en eksisterende mal.
* __ Lagre til mal__: lagre gjeldende åpningstidsverdi som en mal for fremtidig bruk.
* __Håndter maler__: rediger, for eksempel endre navn, og slett eksisterende maler.
* __Refresh__: parse åpningstidsverdien på nytt.
* __Slette alle__: fjern alle regler.

### Regler

Standard regel er lagt til som _normal_ regel, dette innebærer at de vil overstyre verdiene til tidligere regler for de samme dagene. Det kan være en bekymring når du spesifiserer lengre tider, vanligvis vil du da bytte regler via _Vis regel type_ meny innlegg til _additiv_.

#### Regelmeny

* __Legg til modifikasjon / kommentar__: endre effekten av denne regelen og legg til en valgfri kommentar.
* __Legg til ferie__: legg til en velger for helligdager eller skoleferier.
* __Legg til tidsrom ...__
    * __Tid - tid__: en starttid til en sluttid på samme dag.
    * __Time - extended time__: a start time to an end time on the next day (example 26:00 is 02:00 (am) the next day).
    * __Var. time - time__: from a start variable time (dawn, dusk, sunrise and sundown) to an end time on the same day.
    * __Var. time - extended time__: from a start variable time to an end time on the next day.
    * __Time - var. time__: a start time to an end variable time.
    * __Var. time - var. time__: a start variable time to an end variable time.
    * __Tid__: et tidspunkt.
    * __Time-open end__: from a start point in time onwards.
    * __Variable time__: at the variable time
    * __Variable time-open end__: from a start variable time onwards
* __Add week day range__: add a weekday based selector.
* __Legg til datoperiode ...__
    * __Date - date__: from a start date (year, month, day) to an end date.
    * __Variable date - date__: from a start variable date (currently the specification only defines _easter_) to an end date.
    * __Date - variable date__: from a start date to a variable date.
    * __Variable date - variable date__: from a start variable date to an end variable date.
    * __Occurrence in month - occurrence in month__: from a start weekday occurrence in a month to the same.
    * __Occurrence in month - date__: from a start weekday occurrence in a month to a end date.
    * __Date - occurrence in month__: from a start date to an end weekday occurrence in a month.
    * __Occurrence in month - variable date__: from a start weekday occurrence in a month to an end variable date.
    * __Variable date - occurrence in month__: from a start variable date to an end weekday occurrence in a month.
    * __Dato - åpen slutt__: fra en startdato og fremover.
    * __Variabel dato - åpen slutt__: fra en startvariabel dato og fremover.
    * __Forekomst i måned - åpen slutt__: fra en start ukedag hendelse om en måned og utover.
    * __Med forskyvninger ...__: de samme oppføringene som nevnt ovenfor(dette blir sjelden brukt).
* __Legg til årsområde__: legg til en årsbasert velger.
* __Add week range__: add a week number based selector.
* __Duplicate__: create a copy of this rule and insert it after the current position.
* __Show rule type__: display and allow changing of the rule type _normal_, _additive_ and _fallback_ (not available on the first rule).
* __Move up__: move this rule up one position (not available on the first rule).
* __Move down__: move this rule down one position.
* __Delete__: slett denne regelen.

### Tidsrom

To make editing time spans as easy as possible, we try to choose an optimal time range and granularity for the range bars when loading existing values. For new time spans the bars start at 6:00 (am) and have 15 minute increments, this can be changed via the menu.

Clicking (not on the pins) the time bar will open the large time picker, when using the bars directly is too difficult. The time pickers extend in to the next day, so they are a simple way to extend a time range without having to delete and re-add the the range.

#### Tidsrom meny

* __Display time picker__: show a large time picker for selecting start and end time, on very small displays this is the preferred way of changing times.
* __Bryt til 15 minutters tikk__: bruk 15 minutters granularitet for rekkevidde linjen.
* __Switch to 5 minute ticks__: use 5 minute granularity for the range bar.
* __Bytt til 1 minutt tikk__: bruk 1 minutt granularitet for områdelinja, veldig vanskelig å bruke på en telefon.
* __Start i midnatt__: start rekkevidden linje ved midnatt.
* __Vis intervall__: vis intervallfeltet for å spesifisere et intervall i minutter.
* __Delete__: slett dette tidsrommet.

