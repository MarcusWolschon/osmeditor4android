# OpenStreetMap openingstijden bewerker

De OpenStreetMap openingstijden bewerker is redelijk complex en leent zich niet voor een simpele en intuïtieve gebruikersinterface.

Toch zal je meestal maar een klein deel van de definitie gebruiken. De bewerker helpt hiermee door de obscure onderdelen te verbergen in menus en het gebruik "op de straat" meestal te beperken tot kleine aanpassingen van voorgedefinieerde sjablonen.

_Deze documentatie is voorlopig is werk in uitvoering_

## Gebruik van de openingstijdenbewerker

In een typische workflow zal het object dat wordt bewerkt al een bestaande openingstijden tag (opening_hours, service_times en collection_times) hebben, of je kan opnieuw een voorkeuze toepassen op het object om een leeg openingstijden veld te krijgen. Om handmatig het veld toe te voegen in Vespucci kan je in de detailspagina de sleutel toevoegen en dan terug naar de formuliergebaseerde wijzigingstab om te wijzigen. Als je denkt dat de openingstijden tag deel had moeten zijn van de voorkeuze, dan kan je een probleemmelding openen voor jouw bewerker. 

Als je een standaardsjabloon hebt gedefiniëerd (doe dit via het "Beheer sjablonen" menu item) zal het sjabloon automatisch worden geladen als de bewerker wordt geladen met een lege waarde. Met de "Laad sjabloon" kan je een ander opgeslagen sjabloon laden en met de "Sjabloon opslaan" menu kan je de huidige waarde als een sjabloon opslaan. Je kan verschillende standaardsjablonen definiëren voor de "opening_hours", "collection_times" en "service_times" tags. 

Natuurlijk kan je een openingstijden met de hand maken, maar we raden aan om een van de bestaande sjablonen als een startpunt te gebruiken.

Als een bestaande openingstijden waarde is geladen, wordt geprobeerd om het automatisch te corrigeren naar de openingstijden specificatie. Als dat niet mogelijk is zal de locatie van de found worden getoond op de plek in de rauwe OT waarde, en kan worden geprobeerd om het handmatig te herstellen. Ongeveer een kwart van de OT waarden in de OpenStreetMap gegevensbank heeft problemen, maar minder dan 10% kan niet automatisch worden gecorrigeerd, zie [OpeningHoursParser](https://github.com/simonpoole/OpeningHoursParser) voor meer informatie over de afwijkingen in de specificatie die worden getolereerd.

### Hoofdmenu knop

* __Voeg regel toe__: voeg een nieuwe regel toe.
* __Voeg regel toe voor vakanties__: voeg een nieuwe regel toe voor vakanties samen met een staatwijziging.
* __Voeg een regel toe voor 24/7__: voeg een regel toe voor een object dat altijd open is, de openingstijden specification ondersteunt geen andere subwaarden voor 24/7 maar we staan wel toe om hogere niveau selectoren toe te voegen (bijvoorbeeld jaarbereiken).
* __Laad sjabloon__: laad een bestaand sjabloon.
* __Sla op als sjabloon__: sla de huidige openingstijden waarde als sjabloon voor toekomstig gebruik.
* __Beheer sjablonen__: wijzig (bijvoorbeeld de naam) en verwijder bestaande sjablonen.
* __Ververs__: lees opnieuw de openingstijden waarde in.
* __Verwijder alles__: verwijder alle regels.

### Regels

Standaard regels worden toegevoegd als _normale_ rules, dat betekent dat ze waarden van voorgaande regels zullen overschrijven voor dezelfde dagen. Dat kan een probleem zijn als je uitgebreide tijden specificeert, over het algemeen wil je dan de regels wisselen via _Toon regeltype_ menu item naar _additief_.

#### Regel menu

* __Voeg bewerker/commentaar toe__: verander het effect van deze regel en voeg optioneel commentaar toe.
* __Voeg vakantie toe__: voeg een selector toe voor feestdagen of schoolvakanties.
* __Voeg tijdsbereik toe...__
    * __Tijd - tijd__: een starttijd tot een eindtijd op dezefde dag.
    * __Tijd - uitgebreide tijd__: een starttijd tot een eindtijd op de volgende dag (bijvoorbeeld 26:00 is 02:00 (AM) de volgende dag).
    * __Var. tijd - tijd__: van een variabele starttijd (zonsopgang, zonsondergang) tot een eindtijd on the same day.
    * __Var. tijd - tijd__: van een variabele starttijd tot een eindtijd op de volgende dag.
    * __Tijd - var. tijd__: een starttijd tot een variabele eindtijd.
    * __Var. tijd - var. tijd__: van een variabele starttijd tot een variabele eindttijd.
    * __Tijd__: een punt in de tijd.
    * __Tijd-open eind__:  vanaf een startpunt in de tijd.
    * __Variabelet tijd__: op de variabele tijd
    * __Variabele tijd-open eind__: vanaf een variabele starttijd.
* __Voeg dag van de week bereik toe__: voeg een selector toe op basis van de dagen van de week.
* __Voeg datumbereik toe...__
    * __Datum - datum__: vanaf de startdatum (jaar, maand, dag) tot een einddatum.
    * __Variabele datum - datum__: vanaf een variabele startdatum (de specificatie definiëert alleen _Pasen_) tot een einddatum.
    * __Datum - variable datum__: van een startdatum tot een variabele einddatum.
    * __Variabele datum - variabele datum__:  vanaf een variabele startdatum tot een variabele einddatum.
    * __Voorkomen in maand - voorkomen in maand__: van een begindag in de maand tot hetzelfde.
    * __Voorkomen in maand - datum__: van een voorkomen van een dag in de week in de maand tot een einddatum.
    * __Datum - voorkomen in maand__: van een startdatum tot een eind voorkomen van een weekdag in de maand.
 
    * __Voorkomen in maand - variabele datum__: van een voorkomen van een dag in de week in de maand tot een variabele einddatum.
    * __Variabele datum - voorkomen in maand__: van een variabele startdatum tot een eind voorkomen van een weekdag in de maand.
    * __Datum - open eind__: vanaf een startdatum.
    * __Variable datum - open eind__: vanaf een variabele startdatum.
    * __Voorkomen in maand - open einde__: vanafeen voorkomen van een dag in de week in de maand.
    * __Met verplaatsingen...__: hetzelfde als hierboven maar met verplaatsingen gespecificeerd (dit wordt zelden gebruikt).
* __Voeg jaarbereik toe__: voeg een jaargebaseerde selector toe.
* __Voeg weekbereik toe__: voeg een selector voor een weeknummer.
* __Dupliceer__: maak een kopie van deze regel en voeg hem toe achter de huidige positie.
* __Toon regeltype__: toon en wijzig het regeltype tussen _normal_, _additief_ en _terugval_ (niet beschikbaar op de eerste regel).
* __Beweeg naar boven__: beweeg deze regel een positie naar boven (niet beshikbaar voor de eerste regel).
* __Beweeg naar beneden__: beweeg deze regel een positie naar beneden.
* __Verwijder__: verwijder deze regel.

### Tijdsbereiken

Om het wijzigen van tijdbereiken zo gemakkelijk mogelijk te maken, proberen we een optimaal tijdsbereik en granulariteit te selecteren voor de balken wanneer bestaande waarden worden geladen. Voor nieuwe tijdsbereiken beginnen de balken om 6:00 (AM) en hebben ze staggen van 15 minuten, dit kan via het menu worden aangepast.

Klikken (niet op de bolletjes) op de tijdsbalk opent een groter tijdskeuzescherm, want de balken direct gebruiken is te moeilijk. De tijdkeuze gaat door tot de volgende dag, dus het is een gemakkelijke manier om een tijd te verlengen zonder het bereik te hoeven verwijderen en opnieuw toe te voegen. 

#### Tijdsbereik menu

* __Toon tijdskeuze__: toont een groot scherm om start- en einttijd te kiezen, op kleine schermen is dit de voorkeursmanier om tijden te wijzigen.
* __Wissel naar stappen van 15 minuten__: gebruik een granulariteit van 15 minuten voor de balk.
* __Wissel naar stappen van 5 minuten__: gebruik een granulariteit van 5 minuten voor de balk.
* __Wissel naar stappen van 1 minuut__: gebruik een granulariteit van 1 minuut voor de balk, erg moeilijk om te gebruiken op een telefoon.
* __Begin op middernacht__: laat de balk op middernacht beginnen.
* __Toon interval__: toont het interval veld om een interval in minuten op te geven.
* __Verwijder__: verwijder dit tijdsbereik.

