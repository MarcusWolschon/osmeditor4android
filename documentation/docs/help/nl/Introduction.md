# Vespucci Introductie

Vespucci is een volledige uitgeruste OpenStreetMap bewerker doe de meeste taken die desktopbewerkers ondersteunen. Het is succesvol getest op Google's Android 2.3 tot 10.0 en verscheidene op AOSP gebaseerde varianten. Een kleine waarschuwing: hoewel de capaciteiten van mobiele apparaten hebben geëvenaard, hebben oudere apparaten beperkt geheugen beschikbaar en zijn vaak redelijk traag. Je moet hier rekening mee houden bij het gebruik van Vespucci, en houd bijvoorbeeld de gebieden die je bewerkt tot een redelijke grootte. 

## Eerste gebruik

Bij het opstarten toont Vespucci het "Download ander locatie"/"Laad gebied" scherm, nadat de benodigde permissies zijn gevraagd en een welkomstbericht is weergegeven. Als je coördinaten weergegeven hebt en direct wil downloaden, kan je de geschikte optie selecteren en de straal rond de locatie die je wilt downloaden. Selecteer geen groot gebied op trage apparaten. 

Je kan ook het scherm verbergen door op "Ga naar kaart" te klikken, te slepen en zoomen naar een locatie die je wilt aanpassen, en de data daarna te downloaden (zie hieronder: "Wijzigingen maken met Vespucci").

## Wijzigingen maken met Vespucci

Afhankelijk van de schermgrootte en de leeftijd van je apparaat, kunnen acties direct beschikbaar zijn via een icoon in de bovenbalk, via een menu rechts van de topbalk, via de onderbalk (wanneer zichtbaar) of via de menu knop. 

<a id="download"></a>

### Downloaden van OSM Data

Selecteer of het Versturen icoon ![Versturen](../images/menu_transfer.png) of het "Versturen" menu item. Dit toont zeven opties:

* **Download current view** - download the area visible on the screen and merge it with existing data *(requires network connectivity or offline data source)*
* **Clear and download current view** - clear any data in memory and then download the area visible on the screen *(requires network connectivity)*
* **Upload data to OSM server** - upload edits to OpenStreetMap *(requires authentication)* *(requires network connectivity)*
* **Update data** - re-download data for all areas and update what is in memory *(requires network connectivity)*
* **Location based auto download** - download an area around the current geographic location automatically *(requires network connectivity or offline data)* *(requires GPS)*
* **Pan and zoom auto download** - download data for the currently displayed map area automatically *(requires network connectivity or offline data)* *(requires GPS)*
* **File...** - saving and loading OSM data to/from on device files.
* **Note/Bugs...** - download (automatically and manually) OSM Notes and "Bugs" from QA tools (currently OSMOSE) *(requires network connectivity)*

De gemakkelijkste manier om data te downloaded is door te slepen en te zoomen naar de locatie die je wilt wijzigen en dan "Download in huidige weergave" te selecteren. Je kan zoomen door schermbewegingen, de zoom knoppen of de volumeknoppen op het apparaat. Vespucci zal dan de data downloaden in de huidige weergave. Er is geen authenticatie vereist voor het downloaden van data naar je apparaat.

With the default settings any non-downloaded areas will be dimmed relative to the downloaded ones, this is to avoid inadvertently adding duplicate objects in areas that are not being displayed. The behaviour can be changed in the [Advanced preferences](Advanced%20preferences.md).

### Wijzigingen maken

<a id="lock"></a>

#### Vergendelen, ontgrendelen, modus wijzigen

Om onbedoelde veranderingen te voorkomen, start Vespucci in "vergendelde" modus, waarmee je alleen kan slepen en zoomen met de kaart. tik op het ![Vergrendeld](../images/locked.png) icoon om het scherm te ontgrendelen. 

Door lang te klikken op het vergrendel-icoon, zal een menu met 4 opties worden getoond:

* **Normaal** - de standaard wijzigmodus, nieuwe objecten kunnen worden toegevoegd, bestaande objecten kunnen worden gewijzigd, verplaatst en verwijderd. Simpel wit slot icoon wordt weergegeven.
* **Alleen tag** - het selecteren van een object laat de Eigenschappen Bewerker zien, een lange klik op het hoofdscherm voegt objecten toe maar andere geometische opties zijn uitgeschakeld. Een wit slot icoon met een "T" wordt weergegeven.
* **Binnen** - zet de Binnenmodus aan, zie [Binnemmodus](#indoor). Wit slot icoon met een "I" wordt weergegeven.
* **C-Modus** - zet de C-Modus aan, alleen objecten met een waarschuwing worden weergegeven, zie [C-Modus](#c-mode). Wit slot icoon met een "C" wordt weergegeven.

#### Enkele tik, dubbele tik en lange tik

Standaard hebben selecteerbare knopen en wegen een oranje oppervlakte om zich heen waarmee het object geselecteerd kan worden. Je hebt drie opties:

* Enkele tik: Selecteert object. 
    * Een geïsoleerde knoop/weg wordt direct automatisch gemarkeerd. 
    * Als je een object selecteert waarvan Vespucci afleidt dat de selectie om meerdere objecten zou kunnen gaan, wordt een selectiemenu getoond waarmee het object dat je wil selecteren kan worden gekozen. 
    * Geselecteerde objecten worden in het geel gemarkeerd. 
    * Voor meer informatie zie [Geselecteerde knoop](Node%20selected.md), [Geselecteerde weg](Way%20selected.md) en [Geselecteerde relatie](Relation%20selected.md).
* Dubbele tik: Start [Multiselectie modus](Multiselect.md)
* Lange tik: Maakt een "crosshair" waarmee knoop toegevoegd kunnen worden, zie hieronder en [Nieuwe objecten maken](Creating%20new%20objects.md). Dit is alleen mogelijk als "Simpele modus" is gedeactiveerd.

Het is een goed idee om in te zoomen als je wijzigingen wilt maken in een gebied met een hoge dichtheid.

Vespucci heeft een goed "ongedaan maken" en "opnieuw" systeem, dus wees niet bang om te experimenteren op je apparaat. Maar zorg ervoor dat je test data niet opslaat en uploadt.

#### Selecteren / Deselecteren (enkele tik en "selectie menu")

Tik op een object om het te selecteren en te markeren. Tikken in een leeg gebied op het scherm zal deselecteren. Als je een object hebt geselecteerd en je wilt iets anders selecteren, dan kan je er direct op tikken zonder eerst te deselecteren. Een dubbele tik op een object begint de [Multiselectie modus](Multiselect.md).

Als je een object probeert te selecteren en Vespucci bepaalt dat de selectie meerdere objecten zou kunnen betekenen (zoals een knoop in een weg of andere overlappende objecten), dan zal er een selectiemenu worden weergegen: Tik het object dat je wilt selecteren, en het object wordt geselecteerd. 

Geselecteerde objecten worden aangeduidt met een dunne gele rand. De gele rand kan moeilijk zijn om te zien, afhankelijk van de kaart achtergrond en de zoom factor. Als een selectie is gemaakt, zie je een bevestigingsnotificatie.

Als een selectie is gelukt, dan zie je (als knoppen of al menu items) een lijst van beschikbare acties voor het geselecteerde object. Voor meer informatie zie [Geselecteerde knoop](Node%20selected.md), [Geselecteerde weg](Way%20selected.md) en [Geselecteerde relatie](Relation%20selected.md).

#### Geselecteerde objecten: Tags wijzigen

Een tweede klik op een geselecteerd object, opent de tag bewerker waarmee de toegekende tags van het object kunnen worden gewijzigd. 

Voor overlappende objecten (zoals een knoop op een weg) wordt het selectiemenu een tweede keer getoond. Als je hetzelfde object aanklikt wordt de tag bewerker getoond; als je een ander object aanklikt wordt dat object geselecteerd.

#### Geselecteerde objecten: Knoop of weg verplaatsen

Als je een object hebt geselecteed, dan kan het worden verplaatst. Objecten kunnen alleen worden verplaatst als ze zijn geselecteerd. Sleep dichtbij (dus binnen de tolerantie zone van) het geselecteerde object om het te verplaatsen. Als je een grote sleepoppervlakte selecteert in de instellingen, dan krijg je een grote oppervlakte rond de geselecteerde knoop waarmee het gemakkelijker geplaatst kan worden. 

#### Een nieuwe Knoop/Punt of Weg toevoegen 

De eerste keer start de app in "Simpele modus". Dat kan worden gewijzigd in het hoofdmenu door de bijbehorende selectievakje uit te vinken.

##### Simpele modus

Het klikken op de grote groene kop op het kaartscherm toont een menu. Als je een van de items hebt geselecteerd, wordt gevraagd om het scherm te klikken op de locatie waar je het object wilt maken. Slepen en zoomen blijft werken als je de kaartweergave wilt aanpassen. 

Zie [Nieuwe objecten maken in simpele modus](Creating%20new%20objects%20in%20simple%20actions%20mode.md) voor meer informatie.

##### Advanced (long press) mode
 
Long press where you want the node to be or the way to start. You will see a black "crosshair" symbol. 
* If you want to create a new node (not connected to an object), touch away from existing objects.
* If you want to extend a way, touch within the "tolerance zone" of the way (or a node on the way). The tolerance zone is indicated by the areas around a node or way.

Als je het "crosshair" symbool ziet heb je deze opties:

* _Normal press in the same place._
    * If the crosshair is not near a node, touching the same location again creates a new node. If you are near a way (but not near a node), the new node will be on the way (and connected to the way).
    * If the crosshair is near a node (i.e. within the tolerance zone of the node), touching the same location just selects the node (and the tag editor opens. No new node is created. The action is the same as the selection above.
* _Normal touch in another place._ Touching another location (outside of the tolerance zone of the crosshair) adds a way segment from the original position to the current position. If the crosshair was near a way or node, the new segment will be connected to that node or way.

Simply touch the screen where you want to add further nodes of the way. To finish, touch the final node twice. If the final node is located on a way or node, the segment will be connected to the way or node automatically. 

Je kan ook een menu item gebruiken: Zie [Nieuwe objecten maken](Creating%20new%20objects.md) voor meer informatie.

#### Een oppervlakte toevoegen

OpenStreetMap heeft op het moment geen "oppervlakte" object type zoals andere geo-data systemen. De online bewerker "iD" probeert een abstractie te maken van de onderliggende OSM elementen waarmee wordt gewerkt. Dat werkt goed in sommige omstandigheden en minder goed in andere. Vespucci probeert dat niet, dus je zult iets moeten weten over hoe weg-oppervlaktes worden voorgesteld:

* _gesloten wegen (*polygonen")_: de simpelste en meest voorkomende oppervlaktevariant zijn wegen die een gedeelde begin- en eindknoop hebben zodat ze een gesloten "ring" vormen (bijvoorbeeld de meeste gebouwen zijn van dit type). Deze zijn gemakkelijk om te maken in Vespucci: verbind de laatste knoop met de eerste knoop als je klaar bent met het tekenen van een oppervlakte. De interpretatie van een gesloten weg hangt af van de tags: bijvoorbeeld als een gesloten weg als een gebouw is getagd zal het worden beschouwd als een oppervlakte, maar als het een rotonde is niet. In sommige situaties zijn beide interpretaties geldig, en kan een "area" tag worden gebruikt om het bedoelde gebruik aan te  geven.
* _multi-ploygonen_: sommige oppervlaktes hebben meerdere onderdelen, gaten en ringen die niet door een enkele weg worden gerepresenteerd. OSM gebruikt een specifiek type relatie (ons algemene object dat relaties dussen elementen kan modelleren) om hier omheen te werken, een multi-polygoon. Een multi-polygoon kan meerdere "buitenringen" en meerdere "binnenringen" hebben. Elke ring kan een gesloten weg zijn zoals hierboven is beschreven, of meerdere losse wegen met gedeelde eindknopen. Hoewel grote multi-polygonen moeilijk te bewerken zijn in elke tool, zijn kleine goed te maken in in Vespucci. 
* _kustlijnen_: voor hele grote objecten, zoals continenten of eilanden, werkt het multi-polygon model niet meer. Voor natural=coastline wegen wordt een richtingsafhankelijke semantiek aangenomen: het land ligt aan de linkerkant en het water ligt aan de rechterkant. Een bij-effect van dit model is dat je in het algemeen de richting van een kustlijn weg niet moet omkeren. Zie voor meer informatie de [OSM wiki](http://wiki.openstreetmap.org/wiki/Tag:natural%3Dcoastline).

#### Verbeteren van Weg Geometrie

Als je ver genoeg inzoomt op een geselecteerde weg dan zie je een kleine "x" in het midden van de wegsegmenten die lang genoeg zijn. Het slepen van de "x" zal een nieuwe knoop maken in de weg op die locatie. Om te voorkomen dat er per ongeluk kopen worden gemaakt, is de tolerantiezone voor deze operatie redelijk klein.

#### Knippen, Kopiëren & Plakken

Je kan geselecteerde knopen of wegen kopiëren of knippen, en ze daarna eens of meerdere keren plakken op een nieuwe locatie. Knippen behoudt de OSM ID en de versie. Om te plakken kan lang worden geklikt op de locatie waar je wilt plakken (je zal een crosshair op de locatie zien). Klik dan op "Plakken" in het menu.

#### Efficiënt Adressen Toevoegen

Vespucci heeft een ![Adress](../images/address.png) "adres tags toevoegen" functie dat het invoeren van adressen efficiënter maakt door het huidige huisnummer te voorspellen. Het kan worden geselecteerd:

* na een lange klik (_alleen niet-simpele modus_:): Vespucci zal een knoop toevegen op de locatie en een schatting maken van het huisnummer en adres tags toevoegen aan de hand van de laatstgebruikte tags. Als de knoop op de buitenkant van een gebouw ligt zal automatisch een "entrance=yes" tag aan de knoop worden toegekend. De tag bewerker zal worden geopend voor het object zodat je verdere wijzigingen kan maken.
* in de knoop/weg geselecteerde modi: Vespucci zal adres tags toevoegen zoals hierboven beschreven en de tag bewerker tonen.
* in de tag bewerker.

Huisnummervoorspelling heeft normaal gesproken ten minste twee huisnummers aan beide kanten van de weg nodig om goed te werken. Hoe meer huisnummers zijn ingevoerd, hoe beter.

Er wordt geadviseerd om dit samen met de [Auto-download](#download) modus te gebruiken.  

#### Toevoegen van Afslagbeperkingen

Vespucci heeft een snelle manier om afslagbeperkingen toe te voegen. Zo nodig worden wegen opgesplitst en wordt gevraagd om elementen opnieuw te selecteren. 

* selecteer een weg met een highway afslag (afslagbeperkingen kunnen alleen aan highways worden toegekend. Om dit te doen voor andere wegen, gebruik dan de algemene "relatie toevoegen" modus)
* selecteer "Toevoegen afslagbeperking" in het menu
* selecteer de "via" knoop of weg (alleen toegestane "via" elementen worden getoond met een klikgebied)
* selecteer de "naar" weg (het is mogelijk om het "van" element te selecteren voor het "naar" element. In dat geval zal Vespucci aannemen dat je een no_u_turn beperking wilt toevoegen)
* stel het beperkingstype in

### Vespucci in "vergrendelde" modus

Als het rode slot wordt weergegeven dan zijn alle acties die niets wijzigen beschikbaar. Er kan ook lang worden geklikt op of dichtbij een object om het detailscherm weer te geven wanneer het een OSM object betreft.

### Jouw Wijzigingen Opslaan

*(vereist netwerkverbinding)*

Selecteer dezelfde knop of menu item als om te downloaden en selecteer "Upload data naar OSM server".

Vespucci ondersteunt OAuth authorizatie en de klassieke gebruikersnaam en wachtwoord methode. OAuth is wenselijk aangezien daarmee wordt voorkomen dat er wachtwoorden worden verstuurd.

Nieuwe Vespucci installaties zullen OAuth standaard hebben geactiveerd. De eerste keer dat aangepaste data wordt geüpload, wordt een OSM website pagina getoond. Nadat je bent ingelogd (over een versleutelde verbinding) wordt er gevraagd om Vespucci te authoriseren voor jouw account. Om OAuth toegang wilt verlenen voor jouw account voordat er wijzigingen worden gemaakt, kan er in het "Tools" menu een item worden gevonden.

Om je werk op te slaan zonder internettoegang, kan je naar een JOSM compatible .osm bestand opslaan en het later met Vespucci of JSOM uploaden. 

#### Oplossen van conflicten bij uploaden

Vespucci heeft een simpele conflict-oplosser. Bij een vermoeden van grote problemen men je bewerkingen, exporteer je wijzigingen naar een .osc file ("Exporteren" menu item in het "Overbrengen" menu) en repareer en upload ze met JOSM. Zie de gedetaileerde hulp bij [conflicten oplossen](Conflict%20resolution.md).  

## GPS Gebruiken

Je kan Vespucci gebruiken om een GPX track te maken en hem weer te geven op je apparaat. Je kan ook de huidige GPS positie tonen (zet "Toon locatie" aan in het GPS menu) en/of het scherm de positie laten volgen (zet "Volg GPS Positie" in het GPS menu aan). 

Als die laatste optie aan staat en het scherm wordt handmatig bewogen of gewijzigd, dan zal "Volg GPS" modus automatisch worden uitgeschakeld en de blauwe GPS pijl verandert van een omlijning naar een gevulde pijl. 

## Notities and Bugs

Vespucci ondersteunt het downloaden, commentaar geven en sluiten van OSM Notes (voorheen OSM Bugs) en de equivalente functionaliteit voor "Bugs" die worden gemaakt door de [OSMOSE kwaliteitsgarantie tool](http://osmose.openstreetmap.fr/en/map/). Beide moeten expliciet worden gedownload of er kan gebruik worden gemaakt van de automatische download functionaliteit om items te vinden in je directe omgeving. Na het aangepassen of sluiten van een Bug of Notitie kan het direct worden geüpload of alles in een keer.

Op de kaart worden Notities en Bugs weergegeven door een klein insectenicoon ![Bug](../images/bug_open.png), groene zijn gesloten/opgelost, blauwe zijn aangemaakt of gewijzigd door jou en geel geeft aan dat het nog steeds actief en ongewijzigd is.  

De OSMOSE Bug weergave laat een blauwe link zien naar het beïnvloede object. Klikken op de link selecteert het object, centreert het scherm erop en downloadt zo nodig de omgeving eromheen. 

### Filteren

Behalve het globaal activeren van de weergave van Notities en Bugs, kan je een grove weergavefilter instellen om rommel te voorkomen. In de [Geavanceerde instellingen](Advanced%20preferences.md) kan je selecteren:

* Notities
* Osmose fout
* Osmose waarschuwing
* Osmose klein probleem
* Anders

<a id="indoor"></a>

## Binnen modus

Binnen dingen in kaart brengen is ingewikkeld door het hoge aantal objecten dat over elkaar heen ligt. Vespucci heeft een speciale binnenmodus die het mogelijk maakt om alle objecten te filteren die niet op dezelfde verdieping zijn en zal automatisch de huidige verdieping aan nieuwe objecten toevoegen.

De modus kan worden aangezet door lang te klikken op de slot knop, zie [Vergendelen, ontgrendelen, modus wijzigen](#lock) en de goede modus te selecteren.

<a id="c-mode"></a>

## C-Modus

In C-Modus worden alleen objecten weergegeven met waarschuwingen. Dit maakt het gemakkelijk om objecten met specifieke problemen te vinden of het koppelen van configureerbare controles. Als een object is geselecteerd en de Eigenschappen Bewerker is gestart in C-Modus dan zal de best passende voorkeuze automatisch worden toegepast.

De modus kan worden aangezet door lang te klikken op de slot knop, zie [Vergendelen, ontgrendelen, modus wijzigen](#lock) en de goede modus te selecteren.

### Configureren van controles

Op het moment zijn er twee configureerbare controles (een controle voor FIXME en een controle voor missende type tags op relaties die niet configureerbaar zijn). Beide kunnen worden geconfigureerd door "Validator instellingen" te selecteren in de "Instellingen". 

De lijst is in tweën gesplitst. De bovenste helft toont "opniew bekijken" items en de onderkant toont "check" items. Items kunnen worden gewijzigd door erop te klikken. Met de groene menu knop kunnen nieuwe items worden toegevoegd.

#### Opnieuw bekijken items

Opnieuw bekijken items hebben de volgende eigenschappen:

* **Sleutel** - Key van de tag.
* **Waarde** - Waarde van de tag, leeg betekent dat de waarde van de tag wordt genegeerd.
* **Leeftijd** - hoeveel dagen sinds het element is gewijzigd en opnieuw bekeken moet worden. Als een _check_date_ tag aanwezig is zal die worden gebruikt, anders de datum van de huidige versie. Als de waarde op nul wordt gezet zal dat leiden tot het checken op de Sleutel en Waarde.
* **Reguliere expressie** - als aangevinkt zal **Value** worden gebruikt als een Java reguliere expressie.

**Sleutel** en **Waarde** worden gecheckt tegen de _bestaande_ tags in het object.

De _Annotaties_ groep in de standaard voorkeuzes bevatten een item dat automatisch een _check_date_ tag met de huidige datum zal toevoegen.

#### Check items

Check items hebben de volgende eigenschappen:

* **Waarde** - Waarde die aanwezig moet zijn op het object volgens de gekoppelde voorkeuze.
* **Vereis optioneel** - Vereis dat de sleutel aanwezig is in de optionele tags van de voorkeuze.

Deze check werkt door eerst de voorkeuze te achterhalen en dan te checken of **Sleutel** is een "aanbevolen" sleutel voor dit object volgens de voorkeuze, **Vereis optioneel** zal de check uitbreiden naar tags die "optioneel" zijn op het object. Let op: gekoppelde voorkeuzes worden niet gecheckt.

## Filters

### Tag gebaseerd filter

De filter kan worden aangezet via het hoofdmenu. Het kan dan worden gewijzigd door het filtericoon te tikken. Meer documentatie kan worden gevonden op [Tag filter](Tag%20filter.md).

### Voorkeuze gebaseerd filter

Een alternatief op het bovenstaande is het filteren van objecten op basis van losse voorkeuzes of voorkeuzegroepen. Tikken op het filtericoon toont een voorkeuze selectie scherm net als op andere plekken in Vespucci. Losse voorkeuzes kunnen met een normale tik worden selecteerd, voorkeuzegroepen kunnen met een lange tik worden geselecteerd (een normale tik gaat de groep binnen). Meer documentatie kan worden gevonden op [Voorkeuze filter](Preset%20filter.md).

## Vespucci aanpassen

Veel aspecten van de app kunnen worden aangepast. Als je zoekt naar iets specifieks en het niet kan vinden, dan is [de Vespucci website](https://vespucci.io/) zoekbaar en die bevat additionele informatie over wat er beschikbaar is op het apparaat.

### Lagen instellingen

Layer settings can be changed via the layer control ("hamburger" menu in the upper right corner), all other setting are reachable via the main menu preferences button. Layers can be enabled, disabled and temporarily hidden.

Available layer types:

* Data layer - this is the layer OpenStreetMap data is loaded in to. In normal use you do not need to change anything here. Default: on.
* Background layer - there is a wide range of aerial and satellite background imagery available. The default value for this is the "standard style" map from openstreetmap.org.
* Overlay layer - these are semi-transparent layers with additional information, for example GPX tracks. Adding an overlay may cause issues with older devices and such with limited memory. Default: none.
* Notes/Bugs display - Open Notes and bugs will be displayed as a yellow bug icon, closed ones the same in green. Default: on.
* Photo layer - Displays geo-referenced photographs as red camera icons, if direction information is available the icon will be rotated. Default: off.
* Mapillary layer - Displays Mapillary segments with markers where images exist, clicking on a marker will display the image. Default: off.
* GeoJSON layer - Displays the contents of a GeoJSON file. Default: off.
* Grid - Displays a scale alone the sides of the map or a grid. Default: on. 

#### Instellingen

* Houd scherm aan. Standaard: uit.
* Grote sleepzone. Het slepen van knopen op een apparaat met aanraakbesturing is problematisch omdat je vingers de huidige locatie verbergen op het scherm. Als je ditknoop aanzet kan een grotere oppervlakte worden gebruikt voor slepen naast het midden (selectie en andere acties gebruiken de normale aanraak tolerantiezone). Standaard: uit.

De volledige beschrijving kan worden gevonden in [Instellingen](Preferences.md)

#### Geavanceerde instellingen

* Knoop iconen. Standaard: aan.
* Toon altijd het context menu. Als deze optie aan staat zal voor elke selectie het context menu worden getoond. Anders wordt het menu alleen getoond als er geen on-ambigue selectie kan worden bepaald. Standaard: uit (vroeger aan).
* Zet een licht thema aan. Op moderne apparaten kan deze optie standaard aan staan. Ook al kan op oude apparaten kan deze optie worden aangezet, de stijl kan inconsitent zijn. 

De volledige beschrijving kan worden gevonden in [Geavanceerde instellingen](Advanced%20preferences.md)

## Problemen rapporteren

Als Vespucci crasht, of een inconsitente staat detecteert, dan kan je worden gevraagd om een crash dump te versturen. Doe dat graag, maar maximaal één keer per specifieke situatie. Als je verdere toelichting wilt geven, of een probleemmelding wilt openen voor een vraag of opmerking dan kan dat hier: [Vespucci problemendatabank](https://github.com/MarcusWolschon/osmeditor4android/issues). De "Geef feedback" functie van het hoofdmenu opent een nieuwe probleemelding en voegt automatisch relevante app- en systeeminformatie toe zonder extra typwerk.

Om een specifiek discussiepunt te opnenen rerelateerd aan Vespucci, dan kan je een discussie starten in de [Vespucci Google groep](https://groups.google.com/forum/#!forum/osmeditor4android) of op het [OpenStreetMap Android forum](http://forum.openstreetmap.org/viewforum.php?id=56)


