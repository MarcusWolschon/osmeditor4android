_Before we start: most screens have links in the menu to the on-device help system giving you direct access to information relevant for the current context, you can easily navigate back to this text too. If you have a larger device, for example a tablet, you can open the help system in a separate split window.  All the help texts and more (FAQs, tutorials) can be found on the [Vespucci documentation site](https://vespucci.io/) too. You can further start the help viewer directly on devices that support short cuts with a long press on the app icon and selecting "Help"_

# Vespucci Introductie

Vespucci is a full featured OpenStreetMap editor that supports most operations that desktop editors provide. It has been tested successfully on Google's Android 2.3 to 14.0 (versions prior to 4.1 are no longer supported) and various AOSP based variants. A word of caution: while mobile device capabilities have caught up with their desktop rivals, particularly older devices have very limited memory available and tend to be rather slow. You should take this in to account when using Vespucci and keep, for example, the areas you are editing to a reasonable size.

## Wijzigingen maken met Vespucci

Afhankelijk van de schermgrootte en de leeftijd van je apparaat, kunnen acties direct beschikbaar zijn via een icoon in de bovenbalk, via een menu rechts van de topbalk, via de onderbalk (wanneer zichtbaar) of via de menu knop. 

<a id="download"></a>

### Downloaden van OSM gegevens

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

De gemakkelijkste manier om data te downloaded is door te slepen en te zoomen naar de locatie die je wilt wijzigen en dan "Download in huidige weergave" te selecteren. Je kan zoomen door schermbewegingen, de zoom knoppen of de volumeknoppen op het apparaat. Vespucci zal dan de data downloaden in de huidige weergave. Er is geen authenticatie vereist voor het downloaden van data naar je apparaat.

In unlocked state any non-downloaded areas will be dimmed relative to the downloaded ones if you are zoomed in far enough to enable editing. This is to avoid inadvertently adding duplicate objects in areas that are not being displayed. In the locked state dimming is disabled, this behaviour can be changed in the [Advanced preferences](Advanced%20preferences.md) so that dimming is always active.

If you need to use a non-standard OSM API entry, or use [offline data](https://vespucci.io/tutorials/offline/) in _MapSplit_ format you can add or change entries via the _Configure..._ entry for the data layer in the layer control.

### Wijzigingen maken

<a id="lock"></a>

#### Vergendelen, ontgrendelen, modus wijzigen

Om onbedoelde veranderingen te voorkomen, start Vespucci in "vergendelde" modus, waarmee je alleen kan slepen en zoomen met de kaart. tik op het ![Vergrendeld](../images/locked.png) icoon om het scherm te ontgrendelen. 

Een lange klik op het slot-icoon of het _Modes_ menu in het uitklapmenu in de kaartweergave, toont het menu met 4 opties:

* **Normal** - the default editing mode, new objects can be added, existing ones edited, moved and removed. Simple white lock icon displayed.
* **Tag only** - selecting an existing object will start the Property Editor, new objects can be added via the green "+" button, or long press, but no other geometry operations are enabled. White lock icon with a "T" is displayed.
* **Address** - enables Address mode, a slightly simplified mode with specific actions available from the [Simple mode](../en/Simple%20actions.md) "+" button. White lock icon with an "A" is displayed.
* **Indoor** - enables Indoor mode, see [Indoor mode](#indoor). White lock icon with an "I" is displayed.
* **C-Mode** - enables C-Mode, only objects that have a warning flag set will be displayed, see [C-Mode](#c-mode). White lock icon with a "C" is displayed.

Als je Vespucci gebruikt op een Android apparaat dat shortcuts ondersteunt (druk lang op het app icoon), kan je direct starten in _Adres_ en _Binnen_ modus.

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

Als je een object hebt geselecteerd, dan kan het worden verplaatst. Objecten kunnen alleen worden verplaatst als ze zijn geselecteerd. Sleep dichtbij (dus binnen de tolerantie zone van) het geselecteerde object om het te verplaatsen. Als je een grote sleepoppervlakte selecteert in de [instellingen]](Preferences.md), dan krijg je een grote oppervlakte rond de geselecteerde knoop waarmee het gemakkelijker geplaatst kan worden. 

#### Een nieuwe Knoop/Punt of Weg toevoegen 

De eerste keer start de app in "Simpele modus". Dat kan worden gewijzigd in het hoofdmenu door de bijbehorende selectievakje uit te vinken.

##### Simpele modus

Het klikken op de grote groene kop op het kaartscherm toont een menu. Als je een van de items hebt geselecteerd, wordt gevraagd om het scherm te klikken op de locatie waar je het object wilt maken. Slepen en zoomen blijft werken als je de kaartweergave wilt aanpassen. 

Zie [Nieuwe objecten maken in simpele actiemodus](Simple%20actions.md) voor meer informatie. Simpele modus is standaard voor nieuwe installaties.

##### Geavanceerde (lange klik) modus
 
Klik lang waar je de knoop wilt plaatsen of de weg wilt beginnen. Je ziet een zwart "crosshair" symbool. 
* Om een nieuwe knoop (dat niet is verbonden aan een object) te maken, klik op een gebied zonder bestaande objecten .
* Om een weg te verlengen, klik binnen de "tolerantiezone" van de weg (of een knoop in de weg). De tolerantiezone is aangegeven met een oppervlakte rond de knoop of weg.

Als je het "crosshair" symbool ziet heb je deze opties:

* _Klik op dezelfde plek._
    * Als de crosshair niet dichtbij een knoop is, maakt het klikken op dezelfde locatie een nieuwe knoop. Als je dichtbij een weg bent (maar niet dichtbij een knoop) de nieuwe knoop zal op de weg worden geplaatst (en verbonden aan de weg).
    * Als de crosshair dichtbij een knoop is (d.w.z. binnen de tolerantiezone van de knoop), zal een klik op dezelfde locatie de knoop selecteren en de tag bewerker openen. Er wordt geen nieuwe knoop gemaakt. De actie is hetzelfde als de selectie hierboven.
* _Klik op een andere plek._ Klikken op een andere plek (buiten de tolerantie zone van de crosshair) voegt een wegsegment toe van de originele positie naar de huidige positie. Als de crosshair dichtbij een weg of knoop was, zal het nieuwe segment worden verbonden met die knoop of weg.

Klik op het scherm waar je nieuwe knopen aan de weg wilt toevoegen. Om af te ronden kan tweemaal worden geklikt op de laatste knoop. Als de laatste knoop zich op de locatie van een weg of knoop bevindt, zal het segment automatisch aan de weg of knoop worden verbonden. 

Je kan ook een menu item gebruiken: Zie [Nieuwe objecten maken](Creating%20new%20objects.md) voor meer informatie.

#### Een oppervlakte toevoegen

OpenStreetMap heeft op het moment geen "oppervlakte" object type zoals andere geo-data systemen. De online bewerker "iD" probeert een abstractie te maken van de onderliggende OSM elementen waarmee wordt gewerkt. Dat werkt goed in sommige omstandigheden en minder goed in andere. Vespucci probeert dat niet, dus je zult iets moeten weten over hoe weg-oppervlaktes worden voorgesteld:

* _gesloten wegen (*polygonen")_: de simpelste en meest voorkomende oppervlaktevariant zijn wegen die een gedeelde begin- en eindknoop hebben zodat ze een gesloten "ring" vormen (bijvoorbeeld de meeste gebouwen zijn van dit type). Deze zijn gemakkelijk om te maken in Vespucci: verbind de laatste knoop met de eerste knoop als je klaar bent met het tekenen van een oppervlakte. De interpretatie van een gesloten weg hangt af van de tags: bijvoorbeeld als een gesloten weg als een gebouw is getagd zal het worden beschouwd als een oppervlakte, maar als het een rotonde is niet. In sommige situaties zijn beide interpretaties geldig, en kan een "area" tag worden gebruikt om het bedoelde gebruik aan te  geven.
* _multi-ploygonen_: sommige oppervlaktes hebben meerdere onderdelen, gaten en ringen die niet door een enkele weg worden gerepresenteerd. OSM gebruikt een specifiek type relatie (ons algemene object dat relaties dussen elementen kan modelleren) om hier omheen te werken, een multi-polygoon. Een multi-polygoon kan meerdere "buitenringen" en meerdere "binnenringen" hebben. Elke ring kan een gesloten weg zijn zoals hierboven is beschreven, of meerdere losse wegen met gedeelde eindknopen. Hoewel grote multi-polygonen moeilijk te bewerken zijn in elke tool, zijn kleine goed te maken in in Vespucci. 
* _kustlijnen_: voor hele grote objecten, zoals continenten of eilanden, werkt het multi-polygon model niet meer. Voor natural=coastline wegen wordt een richtingsafhankelijke semantiek aangenomen: het land ligt aan de linkerkant en het water ligt aan de rechterkant. Een bij-effect van dit model is dat je in het algemeen de richting van een kustlijn weg niet moet omkeren. Zie voor meer informatie de [OSM wiki](http://wiki.openstreetmap.org/wiki/Tag:natural%3Dcoastline).

#### Verbeteren van Weg Geometrie

Als je ver genoeg inzoomt op een geselecteerde weg dan zie je een kleine "x" in het midden van de wegsegmenten die lang genoeg zijn. Het slepen van de "x" zal een nieuwe knoop maken in de weg op die locatie. Om te voorkomen dat er per ongeluk kopen worden gemaakt, is de tolerantiezone voor deze operatie redelijk klein.

#### Knippen, Kopiëren & Plakken

Je kan geselecteerde knopen en wegen kopiëren en één of meerdere keren plakken op een nieuwe locatie. Knippen onthoudt de OSM identificatie en versie, en kan dus maar eenmalig worden geplakt. Om te plakken, druk lang op de locatie waar je wilt plakken (je ziet een doel dat de locatie markeert). Selecteer dan "Plakken" in het menu.

#### Efficiënt Adressen Toevoegen

Vespucci ondersteunt functionaliteit om het vastleggen van adressen efficiënter te maken, door huisnummers te voorspellen (linker- en rechterkant van de straat los van elkaar) en automatisch _addr:street_ of _addr:place_ tags toe te voegen op basis van de laatst gebruikte waarden en nabijheid. In het beste geval kunnen adressen worden toegevoegd zonder te typen.   

De tags toevoegen kan worden gedaan door te tikken op ![Adres](../images/address.png): 

* na een lange klik (alleen niet-simpele modus): Vespucci zal een knoop toevegen op de locatie en een schatting maken van het huisnummer en adres tags toevoegen aan de hand van de laatstgebruikte tags. Als de knoop op de buitenkant van een gebouw ligt zal automatisch een "entrance=yes" tag aan de knoop worden toegekend. De tag bewerker zal worden geopend voor het object zodat je verdere wijzigingen kan maken.
* in de knoop/weg geselecteerde modi: Vespucci zal adres tags toevoegen zoals hierboven beschreven en de tag bewerker tonen.
* in de tag bewerker.

Om losse adres-knopen direct in te voeren tijdens het gebruik van de "Simpele modus", wissel naar "Adres modus" (lange tik op het slot-icoon). De "Voeg adres-knoop toe" voegt dan een adres-knoop toe op de locatie. Als de knoop binnen een gebrouw ligt, kan een ingangs-tag worden toegekend zoals hierboven beschreven.

Huisnummervoorspelling heeft normaal gesproken ten minste twee huisnummers aan beide kanten van de weg nodig om goed te werken. Hoe meer huisnummers zijn ingevoerd, hoe beter.

Er wordt geadviseerd om dit samen met een van de [Auto-download](#download) modi te gebruiken.  

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

Vespucci ondersteunt OAuth 2, OAuth 1.0a authorizatie, en de klassieke gebruikersnaam en wachtwoord methode. Sinds 1 juli 2024 ondersteunt de standaard OpenStreetMap API alleen OAuth 2 en andere methodes zijn alleen beschikbaar op privé-installaties van de API of andere projecten die OSM software hebben aangepast.  

Authorizing Vespucci to access your account on your behalf requires you to one time login with your display name and password. If your Vespucci install isn't authorized when you attempt to upload modified data you will be asked to login to the OSM website (over an encrypted connection). After you have logged on you will be asked to authorize Vespucci to edit using your account. If you want to or need to authorize the OAuth access to your account before editing there is a corresponding item in the "Tools" menu.

Om je werk op te slaan zonder internettoegang, kan je naar een JOSM compatible .osm bestand opslaan en het later met Vespucci of JSOM uploaden. 

#### Oplossen van conflicten bij uploaden

Vespucci heeft een simpele conflict-oplosser. Bij een vermoeden van grote problemen men je bewerkingen, exporteer je wijzigingen naar een .osc file ("Exporteren" menu item in het "Overbrengen" menu) en repareer en upload ze met JOSM. Zie de gedetaileerde hulp bij [conflicten oplossen](Conflict%20resolution.md).  

### Weergave van interessante objecten in de omgeving 

De weergave van interessante objecten in de omgeving kan worden getoond door de balk middenin de onderbalk omhoog te slepen. 

Meer informatie hierover en andere functionaliteit op het hoofdscherm kan worden gevonden in [Hoofdscherm weergave](Main%20map%display.md).

## Het gebruik van GPS en GPX routes

Met standaard instellingen zal Vespucci GPS (en andere satellietgebaseerde navigatiesystemen) proberen in te schakelen, en terugvallen op het bepalen van de locatie via "netwerk locatie". Dit gedrag neemt aan dat tijdens normaal gebruik het Android apparaat is geconfigureerd om alleen GPX gegenereerde locaties te gebruiken (om tracking te voorkomen), en dus dat de "Verbeterde Locatiebepaling" optie is uitgeschakeld. Als je deze optie wilt inschakelen, maar niet wilt dat Vespucci terugvalt op "netwerk locatie", kan je de optie uitzetten in de [Geavanceerde instellingen](Advanced%20preferences.md). 

Touching the ![GPS](../images/menu_gps.png) button (normally on the left hand side of the map display) will center the screen on the current position and as you move the map display will be panned to maintain this.  Moving the screen manually or editing will cause the "follow GPS" mode to be disabled and the blue GPS arrow will change from an outline to a filled arrow. To quickly return to the "follow" mode, simply touch GPS button or re-check the equivalent menu option. If the device doesn't have a current location the location marker/arrow will be displayed in black, if a current location is available the marker will be blue.

Om een GPX-route op te nemen en hem op het apparaat weer te geven, selecteer "Start GPX-route" in het ![GPS](../images/menu_gps.png) menu. Dit voegt een laag toe aan de weergave met de huidige opgenomen route. De route kan worden geüpload en geëxporteerd via de menu optie in [lagen configuratie](Main%20map%20display.md). Andere lagen kunnen worden toegevoegd van lokale GPX-bestanden of gedownload van de OSM API.

Let op: standaard zal Vecpucci geen hoogtegegevens opnemen in de GPX-route, wegens Android-specifieke problemen. Om hoogtegegevens toch op te nemen kan een zwaartekrachtmodel worden geïnstalleerd, of simpeler: ga naar de [Geavanceerde instellingen](Advanced%20preferences.md) en configureer NMEA invoer.

### Hoe exporteer ik een GPX track?

Open het lagen menu, klik op het 3-punten menu naast "GPX opnemen", en selecteer **Exporteer GPX track...**. Kies in welke map de track moet worden opgeslagen, en geef het een naam die eindigt met `.gpx` (bijvoorbeeld: MijnTrack.gpx).

## Notities, Problemen en Taken

Vespucci ondersteunt het downloaden, commentaar geven en sluiten van OSM Notes (voorheen OSM Bugs) en de equivalente functionaliteit voor "Bugs" die worden gemaakt door de [OSMOSE kwaliteitsgarantie tool](http://osmose.openstreetmap.fr/en/map/). Beide moeten expliciet worden gedownload of er kan gebruik worden gemaakt van de automatische download functionaliteit om items te vinden in je directe omgeving. Na het aangepassen of sluiten van een Bug of Notitie kan het direct worden geüpload of alles in een keer. 

Verder ondersteunen we "Taken" die kunnen worden aangemaakt van OSM objecten, vanuit een GeoJSON laag of van buiten Vecpucci. Deze taken geven een gemakkelijke manier om werk bij te houden. 

Op de kaart worden Notities en Problemen voorgesteld door een klein insect (bug) icoon ![Bug](../images/bug_open.png). Groene zijn opgelost, blauwe zijn door jou aangemaakt of aangepast, en geel geeft aan dat het probleem nog steeds actief en onaangepast is. Taken gebruiken een geel vinkje als icoon.

De OSMOSE fouten en taken weergave, toont een link naar het element in blauw (voor taken alleen als er een OSM element is gekoppeld). Klikken op de link zal het object selecteren, de kaart centreren en de gegevens in de omgeving van te voren zo nodig downloaden. 

### Filteren

Behalve het globaal activeren van de weergave van Notities en Bugs, kan je een grove weergavefilter instellen om rommel te voorkomen. De filterconfiguratie kan worden gevonden van de takenlaag [laag configuratie](#layers):

* Notities
* Osmose fout
* Osmose waarschuwing
* Osmose klein probleem
* Maproulette
* Taak

<a id="indoor"></a>

## Binnen modus

Binnen dingen in kaart brengen is ingewikkeld door het hoge aantal objecten dat over elkaar heen ligt. Vespucci heeft een speciale binnenmodus die het mogelijk maakt om alle objecten te filteren die niet op dezelfde verdieping zijn en zal automatisch de huidige verdieping aan nieuwe objecten toevoegen.

De modus kan worden aangezet door lang te klikken op de slot knop, zie [Vergendelen, ontgrendelen, modus wijzigen](#lock) en de goede modus te selecteren.

<a id="c-mode"></a>

## C-Modus

In C-Modus worden alleen objecten weergegeven met waarschuwingen. Dit maakt het gemakkelijk om objecten met specifieke problemen te vinden of het koppelen van configureerbare controles. Als een object is geselecteerd en de Eigenschappen Bewerker is gestart in C-Modus dan zal de best passende voorkeuze automatisch worden toegepast.

De modus kan worden aangezet door lang te klikken op de slot knop, zie [Vergendelen, ontgrendelen, modus wijzigen](#lock) en de goede modus te selecteren.

### Configureren van controles

Alle validaties kunnen worden aan of uitgezet in de "Validator instellingen/Ingeschakelde validaties" in de [instellingen](Preferences.md). 

The configuration for "Re-survey" entries allows you to set a time after which a tag combination should be re-surveyed. "Check" entries are tags that should be present on objects as determined by matching presets. Entries can be edited by clicking them, the green menu button allows adding of entries.

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

<a id="layers"></a>

### Lagen instellingen

Lagen instellingen kunnen worden aangepast via de lagen instellingen knop ("hamburger" menu in rechter bovenhoek). Alle andere instellingen zijn bereikbaar via de instellingen in het hoofdmenu. Lagen kunnen worden ingeschakeld, uitgeschakeld of tijdelijk verborgen.

Beschikbare laagtypes:

* Data layer - this is the layer OpenStreetMap data is loaded in to. In normal use you do not need to change anything here. Default: on.
* Background layer - there is a wide range of aerial and satellite background imagery available. The default value for this is the "standard style" map from openstreetmap.org.
* Overlay layer - these are semi-transparent layers with additional information, for example quality assurance information. Adding an overlay may cause issues with older devices and such with limited memory. Default: none.
* Notes/Bugs display - Open Notes and bugs will be displayed as a yellow bug icon, closed ones the same in green. Default: on.
* Photo layer - Displays geo-referenced photographs as red camera icons, if direction information is available the icon will be rotated. Default: off.
* Mapillary layer - Displays Mapillary segments with markers where images exist, clicking on a marker will display the image. Default: off.
* GeoJSON layer - Displays the contents of a GeoJSON file, multiple layers can be added from files. Default: none.
* GPX layer - Displays GPX tracks and way points, multiple layers can be added from files, during recording the generate GPX track is displayed in its own one . Default: none.
* Grid - Displays a scale along the sides of the map or a grid. Default: on. 

Meer informatie kan worden gevonden op de [kaartweergave](Main%20map%20display.md).

#### Instellingen

* Houd scherm aan. Standaard: uit.
* Grote sleepzone. Het slepen van knopen op een apparaat met aanraakbesturing is problematisch omdat je vingers de huidige locatie verbergen op het scherm. Als je ditknoop aanzet kan een grotere oppervlakte worden gebruikt voor slepen naast het midden (selectie en andere acties gebruiken de normale aanraak tolerantiezone). Standaard: uit.

De volledige beschrijving kan worden gevonden in [Instellingen](Preferences.md)

#### Geavanceerde instellingen

* Full screen mode. On devices without hardware buttons Vespucci can run in full screen mode, that means that "virtual" navigation buttons will be automatically hidden while the map is displayed, providing more space on the screen for the map. Depending on your device this may work well or not,  In _Auto_ mode we try to determine automatically if using full screen mode is sensible or not, setting it to _Force_ or _Never_ skips the automatic check and full screen mode will always be used or always not be used respectively. On devices running Android 11 or higher the _Auto_ mode will never turn full screen mode on as Androids gesture navigation provides a viable alternative to it. Default: _Auto_.  
* Node icons. Default: _on_.
* Always show context menu. When turned on every selection process will show the context menu, turned off the menu is displayed only when no unambiguous selection can be determined. Default: off (used to be on).
* Enable light theme. On modern devices this is turned on by default. While you can enable it for older Android versions the style is likely to be inconsistent. 

De volledige beschrijving kan worden gevonden in [Geavanceerde instellingen](Advanced%20preferences.md)

## Problemen rapporteren en oplossen

Als Vespucci crasht, of een inconsitente staat detecteert, dan kan je worden gevraagd om een crash dump te versturen. Doe dat graag, maar maximaal één keer per specifieke situatie. Als je verdere toelichting wilt geven, of een probleemmelding wilt openen voor een vraag of opmerking dan kan dat hier: [Vespucci problemendatabank](https://github.com/MarcusWolschon/osmeditor4android/issues). De "Geef feedback" functie van het hoofdmenu opent een nieuwe probleemelding en voegt automatisch relevante app- en systeeminformatie toe zonder extra typwerk.

Als je problemen hebt om de app op te starten na een crash, kan je proberen hem op te starten in _Veilige_ modus op apparaten die shortcuts ondersteunen: druk lang op het app icoon, en selecteer _Veilig_ in het menu. 

Als je iets wilt discussiëren dat aan Vespucci gerelateerd is, kan je een discussie starten op het [OpenStreetMap forum](https://community.openstreetmap.org).


