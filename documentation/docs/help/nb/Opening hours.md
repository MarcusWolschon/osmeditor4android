# OpenStreetMap åpningstid-redigerer

OpenStreetMap-åpningstidenes spesifikasjoner er ganske kompliserte og gir seg ikke lett til et enkelt og intuitivt brukergrensesnitt.

Men for det meste bruker du sannsynligvis bare en liten del av definisjonen. Redaktøren tar dette med i betraktningen ved å prøve å skjule de mer obskure funksjonene i menyer, og mesteparten av tiden reduserer bruken "på veien" til små tilpasninger av forhåndsdefinerte maler.

_Denne dokumentasjonen er foreløpig og et arbeid som pågår_

## Bruker åpningstid redigerer

I en typisk arbeidsflyt vil objektet du redigerer enten allerede ha en åpningstidsmerke (åpnings_tider, service_tider and hente_tider) , eller du kan bruke forhåndsinnstillingen på nytt for objektet for å få et tomme åpningstidsfelt. Hvis du trenger å legge til feltet manuelt og du bruker Vespucci, kan du skrive inn nøkkelen på informasjonssiden og deretter bytte tilbake til den skjemabaserte fanen for å redigere. Hvis du mener at åpningskoden burde vært en del av forhåndsinnstillingen, kan du åpne et problem for redaktøren.

Hvis du har definert en standardmal (gjør dette via menypunktet "Administrer maler") vil den lastes automatisk når editoren startes med en tom verdi. Med funksjonen "Last inn mal" kan du laste inn hvilken som helst lagret mal og med menyen "Lagre mal" kan du lagre gjeldende verdi som en mal. Du kan definere separate maler og standardinnstillinger for spesifikke nøkkel, for eksempel "opening_hours", "collection_times" og "service_times" eller egendefinerte verdier. Videre kan du begrense anvendeligheten av en mal til en region og en spesifikk identifikator, vanligvis en OSM-tag på toppnivå (for eksempel amenity=restaurant).

Naturligvis kan du bygge en åpningstidsverdi fra bunnen av, men vi anbefaler at du bruker en av de eksisterende malene som utgangspunkt. 

Hvis en eksisterende åpningstidsverdi lastes inn, blir det forsøkt å korrigere den automatisk for å være i samsvar med spesifikasjonen for åpningstider. Hvis dette ikke er mulig, blir den grove plasseringen der feilen oppstod, uthevet i visningen av den rå OH-verdien, og du kan prøve å rette den manuelt. Omtrent en fjerdedel av OH-verdiene i OpenStreetMap-databasen har problemer, men mindre enn 10% kan ikke korrigeres. Se [OpeningHoursParser] (https://github.com/simonpoole/OpeningHoursParser) for mer informasjon om hvilke avvik fra spesifikasjon tolereres. 

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
    * __Tid - utvidet tid__: en starttid til en sluttid neste dag (eksempel 26:00 er 02:00 (am) neste dag). 
    * __Var. tid - tid__: fra en startvariabel tid (soloppgang, skumring, soloppgang og solnedgang) til sluttid på samme dag. 
    * __Var. tid - utvidet tid__: fra en startvariabel tid til en sluttid neste dag. 
    * __Tid - var. tid__: en start tid til en sluttvariabel tid.
    * __Var. tid - var. tid__: en startvariabel tid til en sluttvariabel tid.
    * __Tid__: et tidspunkt.
    * __Tid-åpen slutt__: fra et startpunkt fremover i tid.
    * __Variert tid__: på variert tid
    * __Variert Tid-åpen slut__: fra en start variert fremover i tid
* __Legg til uke dag rekkevidde__: og en ukedag basert velger.
* __Legg til datoperiode ...__
    * __Dato - dato__: fra en start dato (år, måned, dag) til en sluttdato.
    * __Variert dato - dato __: fra en start variert dato (for tiden spesifikasjonen  kun defineres _påske_) til en sluttdato .
    * __Dato - variert dato__: fra en start dato til en variert dato.
    * __Variert dato- variert dato__: fra en start variert dato til en slutt variert dato.
    * __Oppkomst i måned - forekomst i måned__: fra en start ukedag hendelse  i en måned til den samme. 
    * __Hendelse i måned - dato__: fra en start ukedag i en måned til en sluttdato.
    * __Date - hendelse i måned__: fra en startdato til en slutt på ukedagen i en måned. 
    * __Hendelse i måned - variabel dato__: fra en start ukedag hendelse i en måned til en slutt variabel dato.
    * __Variabel dato - hendelse i måned__: fra en start variabel dato til en slutt på ukedag i en måned. 
    * __Dato - åpen slutt__: fra en startdato og fremover.
    * __Variabel dato - åpen slutt__: fra en startvariabel dato og fremover.
    * __Forekomst i måned - åpen slutt__: fra en start ukedag hendelse om en måned og utover.
    * __Med forskyvninger ...__: de samme oppføringene som nevnt ovenfor(dette blir sjelden brukt).
* __Legg til årsområde...__
    * __Legg til årsområde__: legg til en årsbasert velger.
    * __Legg til startår__: legg til et åpent årsområde.
* __Legg til ukes rekkevidde __: legg til et uke nummer basert velger.
* __Dupliser__: opprett en kopi av denne regelen og legg den in etter the nåværende posisjon.
* __Vis regeltype__: vis og tillat endring av regeltypen _normal_, _additiv_ og _tilbakefall_ (ikke tilgjengelig på den første regelen).
* __Flytt opp__: flytt denne regelen opp en posisjon (ikke tilgjengelig på den første regelen). 
* __Flytt ned__: flytt denne regelen ned en posisjon.
* __Delete__: slett denne regelen.

### Tidsrom

For å gjøre redigeringstiden så enkel som mulig, vi prøver å velge et optimalt tidsområde og granularitet for områdelinjene når vi laster inn eksisterende verdier.  For nye tidsperioder begynner stolpene klokka 6:00 (am) og har trinn på 15 minutter, dette kan endres via menyen. 

Ved å klikke (ikke på pinnene) åpner tidslinjen den store tidsvelgeren, når det er vanskelig å bruke linjene direkte. Tidsvelgerne utvides til neste dag, så de er en enkel måte å utvide et tidsintervall uten å måtte slette og legge til området på nytt. 

#### Tidsrom meny

* __VIs tidsvelger __: vis  en stor tidsvelger for valg av start og sluttid, på en veldig liten skjermer dette er den foretrukne måten av skiftende tider.
* __Bryt til 15 minutters tikk__: bruk 15 minutters granularitet for rekkevidde linjen.
* __Bytt til 5 minutt tikk__: bruk 5 minutt granularitet for rekkevidde linje. 
* __Bytt til 1 minutt tikk__: bruk 1 minutt granularitet for områdelinja, veldig vanskelig å bruke på en telefon.
* __Start i midnatt__: start rekkevidden linje ved midnatt.
* __Vis intervall__: vis intervallfeltet for å spesifisere et intervall i minutter.
* __Delete__: slett dette tidsrommet.

### Administrer maler 

Malbehandlingsdialoggen lar deg legge til, redigere og slette maler.

I Android 4.4 og nyere er følgende tilleggsfunksjonalitet tilgjengelig fra menyknappen.

* __Vis alle__: vis alle maler i databasen.
* __Lagre til fil__: skriv innholdet i maldatabasen til en fil.
* __Last fra fil (erstatt)__: last inn maler fra en fil som erstatter gjeldende innhold i databasen.
* __Last fra fil__: last inn maler fra en fil som beholder det gjeldende innholdet.

#### Lagre og rediger maldialogger

Dialogen lar deg stille inn

* __Navn__ et beskrivende navn for malen.
* __Standard__ hvis det er merket av, vil dette bli vurdert som en standardmal (vanligvis ytterligere begrenset av de andre feltene).
* __Key__ nøkkelen denne malen er relevant for, hvis satt til _Egendefinert nøkkel_ kan du legge til en ikke-standard verdi i feltet nedenfor. Nøkkelverdiene støtter SQL-jokertegn, det vil si at _%_ samsvarer med null eller flere tegn, *_* samsvarer med et enkelt tegn. Begge jokertegnene kan rømte med _\\_ for bokstavelige treff.
* __Region__ regionen malen gjelder for.
* __Objekt__ en applikasjonsspesifikk streng som skal brukes for matching.

