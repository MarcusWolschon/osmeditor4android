_Aluksi pieni huomio: useimpien näyttöjen valikossa on linkkejä sovelluksen sisäisille ohjesivuille, joten pääset suoraan senhetkiseen tilanteeseen liittyviin tietoihin, ja voit myös helposti palata takaisin tähän tekstiin. Jos sinulla on isohko laite – esimerkiksi tabletti – voit avata ohjesivut erilliseen ikkunaan. Kaikki ohjeet ja muutakin (FAQ, oppaat) löytyvät myös [Vespuccin ohjesivustolta](https://vespucci.io/)._

# Vespuccin esittely

Vespucci on monipuolinen OpenStreetMap-muokkain, joka sisältää useimmat tietokonemuokkainten tarjoamat toiminnot. Se on läpäissyt testit Googlen Androidin versioissa 2.3–10.0 ja eri AOSP-pohjaisissa varianteissa. Pieni varoituksen sana: vaikka mobiililaitteiden suorituskyky on saanut kiinni eroa tietokoneisiin, niin varsinkin vanhemmissa laitteissa on niukasti muistia ja ne tuppaavat olemaan varsin hitaita. Sinun kannattaa ottaa tämä huomioon, kun käytät Vespuccia, ja esimerkiksi pitää muokattavat alueet järkevissä rajoissa. 

## Muokkaaminen Vespuccilla

Laitteesi näytön koosta ja sen iästä riippuen muokkaustoiminnot voivat löytyä joko suoraan yläpalkin kuvakkeista, palkin oikean laidan alasvetovalikosta, alapalkista (jos sellainen on) tai valikkonäppäimen kautta.

<a id="download"></a>

### OSM-datan lataaminen

Kosketa ensin datansiirtokuvaketta ![Siirrä dataa](../images/menu_transfer.png) tai valitse valikosta "Siirrä dataa". Avatuvassa valikossa on kahdeksan vaihtoehtoa:

* **Lataa nykyinen näkymä** – lataa näytöllä näkyvän alueen ja yhdistää sen aiempaan dataan *(vaatii internetyhteyden tai paikallisen datalähteen)*
* **Tyhjennä ja lataa nykyinen näkymä** – tyhjentää muistissa olevan datan ja lataa näytöllä näkyvän alueen *(vaatii internetyhteyden)*
* **Lähetä data OSM-palvelimelle** – lähetä muokkaukset OpenStreetMappiin *(vaatii tunnistautumisen)* *(vaatii internetyhteyden)*
* **Päivitä data** – lataa uudelleen kaikkien jo ladattujen aluiden data ja päivitä muistissa olevat tiedot *(vaatii internetyhteyden)*
**Sijaintipohjainen automaattilataus** – lataa automaattisesti alueen nykyisen maantieteellisen sijainnin ympäriltä *(vaatii internetyhteyden tai paikallisen datalähteen)* *(vaatii GPS:n)*
**Automaattilataus liikutettaessa** – lataa automaattisesti näytöllä näkyvän alueen *(vaatii internetyhteyden tai paikallisen datalähteen)* *(vaatii GPS:n)*
* **Tiedosto...** – OSM-datan tallennus laitteelle tai lataus laitteelta
* **Muistiinpanot/Virheet...** – lataa (automaattisesti tai käsin) OSM-muistiinpanoja tai -virheitä QA-työkaluista (nykyisin OSMOSE) *(vaatii internetyhteyden)*

Helpoin tapa ladata dataa laitteelle on etsiä muokattavaksi aiottu karttanäkymä ja avata valikosta "Lataa nykyinen näkymä". Voit suurentaa ja pienentää sormieleillä, plus- ja miinusnapilla tai laitteen äänenvoimakkuuspainikkeilla. Vespuccin pitäisi sitten ladata karttanäkymää vastaava data. Sinun ei tarvitse tunnistautua datan lataamista varten.

Oletusasetuksilla lataamattomat alueet näkyvät ladattuja alueita himmeämpinä. Tällä pyritään välttämään olemassa olevien kohteiden lisääminen epähuomiossa uudelleen alueille, jotka eivät ole näkyvissä. Käyttäytymisen voi muuttaa [Lisäasetuksista](../en/Advanced%20preferences.md).

### Muokkaaminen

<a id="lock"></a>

#### Lukitus ja tilan vaihtaminen

Tahattomien muokkausten välttämiseksi Vespucci käynnistyy lukitussa tilassa, jossa karttaa voi vain siirtää ja suurentaa. Kosketa ![lukko](../images/locked.png)-kuvaketta niin lukitus avataan. 

Pitkä painallus lukkokuvakkeeseen avaa valikon, jossa tällä hetkellä on 5 vaihtoehtoa:

* **Tavallinen** - oletusmuokkaustila, uusia kohteita voi lisätä ja olemassaolevia muokata, liikuttaa ja poistaa. Kuvakkeena valkoinen avoin lukko.
* **Vain tägi** - olemassaolevan kohteen valitseminen avaa ominaisuusmuokkaimen; pitkä painallus lisää uuden kohteen, mutta muut muototoiminnot eivät toimi. Kuvakkeena T-kirjain valkoisen lukon vieressä.
* **Osoite** - osoitetila on hieman yksinkertaistettu tila, jossa on tiettyjä toimintoja [Perustilan](../en/Simple%20actions.md) "+"-painikkeesta. Kuvakkeena A-kirjain valkoisen lukon vieressä.
* **Sisätila** - katso [Sisätila](#indoor). Kuvakkeena I-kirjain valkoisen lukon vieressä.
* **C-tila** - C-tilassa näytetään vain kohteet, joilla on varoitusmerkintä; katso [C-tila](#c-mode). Kuvakkeena C-kirjain valkoisen lukon vieressä.

#### Napautus, tuplanapautus ja pitkä painallus

Valittavissa olevia pisteitä ja viivoja ympäröi oletuksena oranssi varjostus, joka näyttää mihin pitää napauttaa, jos haluaa valita kohteen. Vaihtoehtoja on kolme:

* Yksittäinen napautus: Valitsee kohteen. 
    * Erillinen piste tai viiva korostetaan heti.
    * Mutta jos napautuskohdan lähellä on useita kohteita eikä Vespucci ole varma minkä niistä haluat valita, se näyttää listan, josta voi valita haluamansa kohteen.
    * Valitut kohteet korostetaan keltaisella.
    * Lisätietoa saat kohdista [Piste valittuna](../fi/Piste%20valittuna.md), [Viiva valittuna](../fi/Viiva%20valittuna.md) ja [Relaatio valittuna](../fi/Relaatio%20valittuna.md).
* Tuplanapautus: Aloittaa [monivalintatilan](../fi/Monivalinta.md)
* Pitkä painallus: Piirtää "hiusristikon", jonka avulla voit lisätä pisteen; katso alempaa ja [Uusien kohteiden luominen](../fi/Uusien%20kohteiden%20luominen.md). Käytössä vain, jos perustila on pois päältä.

Kannattaa lähentää näkymää tiheään kartoitettua aluetta muokattaessa.

Vespuccissa on hyvä perumis- ja toistojärjestelmä, joten älä arkaile tehdä kokeiluja laitteellasi. Älä kuitenkaan lähetä tai tallenna puhdasta testidataa.

#### Valitseminen ja valinnan poisto (yksittäinen napautus ja "valintavalikko")

Kohteen koskettaminen valitsee ja korostaa sen. Kosketus tyhjään kohtaan ruutua poistaa valinnan. Jos jokin kohde on jo valittuna, kun sinun pitää valita jokin muu kohde, ei aiempaa valintaa tarvitse ensin poistaa, vaan riittää että vain kosketat uutta kohdetta. Tuplanapautus kohteeseen aloittaa [Monivalinnan](../en/Multiselect.md).

Huomaa että jos Vespucci ei valintatilanteessa ole varma mitä kohdetta yrität valita (vaikkapa viivan pistettä tai muita päällekkäisiä kohteita), sinulle näytetään valintavalikko: kosketa kohdetta, jonka haluat valita, ja valinta on valmis. 

Valitut kohteet ilmaistaan ohuen keltaisen reunuksen avulla. Keltaista reunusta voi olla vaikea huomata taustakartasta ja suurennuksesta riippuen. Valinnan toteuduttua näytetään siitä vahvistukseksi ilmoitus.

Once the selection has completed you will see (either as buttons or as menu items) a list of supported operations for the selected object: For further information see [Node selected](Node%20selected.md), [Way selected](Way%20selected.md) and [Relation selected](Relation%20selected.md).

#### Valitut kohteet: tägien muokkaaminen

Uusi kosketus valittuun kohteeseen avaa tägimuokkaimen, jossa voit muokata kohteelle kuuluvia tägejä.

Huomaa että päällekkäisten kohteiden tapauksessa (kuten piste viivalla) valintavalikko näytetään uudelleen. Saman kohteen valitseminen avaa tägimuokkaimen, mutta jos valitset jonkun toisen kohteen, tämä yksinkertaisesti valitaan.

#### Valitut kohteet: Pisteen tai viivan siirtäminen

Once you have selected an object, it can be moved. Note that objects can be dragged/moved only when they are selected. Simply drag near (i.e. within the tolerance zone of) the selected object to move it. If you select the large drag area in the [preferences](Preferences.md), you get a large area around the selected node that makes it easier to position the object. 

#### Adding a new Node/Point or Way 

On first start the app launches in "Simple mode", this can be changed in the main menu by un-checking the corresponding checkbox.

##### Simple mode

Tapping the large green floating button on the map screen will show a menu. After you've selected one of the items, you will be asked to tap the screen at the location where you want to create the object, pan and zoom continues to work if you need to adjust the map view. 

See [Creating new objects in simple actions mode](Creating%20new%20objects%20in%20simple%20actions%20mode.md) for more information.

##### Advanced (long press) mode
 
Long press where you want the node to be or the way to start. You will see a black "crosshair" symbol. 
* If you want to create a new node (not connected to an object), touch away from existing objects.
* If you want to extend a way, touch within the "tolerance zone" of the way (or a node on the way). The tolerance zone is indicated by the areas around a node or way.

Kun hiusristikko on asetettu, on valittavana seuraavat toimenpiteet:

* _Normal press in the same place._
    * If the crosshair is not near a node, touching the same location again creates a new node. If you are near a way (but not near a node), the new node will be on the way (and connected to the way).
    * If the crosshair is near a node (i.e. within the tolerance zone of the node), touching the same location just selects the node (and the tag editor opens. No new node is created. The action is the same as the selection above.
* _Normal touch in another place._ Touching another location (outside of the tolerance zone of the crosshair) adds a way segment from the original position to the current position. If the crosshair was near a way or node, the new segment will be connected to that node or way.

Simply touch the screen where you want to add further nodes of the way. To finish, touch the final node twice. If the final node is located on a way or node, the segment will be connected to the way or node automatically. 

You can also use a menu item: See [Creating new objects](Creating%20new%20objects.md) for more information.

#### Alueen lisääminen

OpenStreetMap currently doesn't have an "area" object type unlike other geo-data systems. The online editor "iD" tries to create an area abstraction from the underlying OSM elements which works well in some circumstances, in others not so. Vespucci currently doesn't try to do anything similar, so you need to know a bit about the way areas are represented:

* _closed ways (*polygons")_: the simplest and most common area variant, are ways that have a shared first and last node forming a closed "ring" (for example most buildings are of this type). These are very easy to create in Vespucci, simply connect back to the first node when you are finished with drawing the area. Note: the interpretation of the closed way depends on its tagging: for example if a closed way is tagged as a building it will be considered an area, if it is tagged as a roundabout it wont. In some situations in which both interpretations may be valid, an "area" tag can clarify the intended use.
* _multi-polygons_: some areas have multiple parts, holes and rings that can't be represented with just one way. OSM uses a specific type of relation (our general purpose object that can model relations between elements) to get around this, a multi-polygon. A multi-polygon can have multiple "outer" rings, and multiple "inner" rings. Each ring can either be a closed way as described above, or multiple individual ways that have common end nodes. While large multi-polygons are difficult to handle with any tool, small ones are not difficult to create in Vespucci. 
* _coastlines_: for very large objects, continents and islands, even the multi-polygon model doesn't work in a satisfactory way. For natural=coastline ways we assume direction dependent semantics: the land is on the left side of the way, the water on the right side. A side effect of this is that, in general, you shouldn't reverse the direction of a way with coastline tagging. More information can be found on the [OSM wiki](http://wiki.openstreetmap.org/wiki/Tag:natural%3Dcoastline).

#### Viivan muodon parantaminen

Jos lähennät valittua viivaa tarpeeksi, voit nähdä pienen x-merkin riittävän pitkien viivanpätkien keskikohdassa. Pikku-x:n vetäminen lisää viivaan kyseiseen kohtaan pisteen. Huom: jottei pisteitä tulisi luotua vahingossa, tämän toiminnon toleranssialue on verrattain pieni.

#### Leikkaa, kopioi & liitä

Voit kopioida ja leikata sekä pisteitä että viivoja, ja liittää ne sitten kerran tai monesti uuteen paikkaan. Leikkaaminen säilyttää OSM-tunnuksen ja -version. Liittäminen aloitetaan painamalla pitkään kohtaa, johon halutaan liittää (merkiksi ilmestyy hiusristikko). Sitten valikosta valitaan "Liitä".

#### Osoitteiden lisääminen kätevästi

Vespucci supports functionality that makes surveying addresses more efficient by predicting house numbers (left and right sides of streets separately) and automatically adding _addr:street_ or _addr:place_ tags based on the last used value and proximity. In the best case this allows adding an address without any typing at all.   

Adding the tags can be triggered by pressing ![Address](../images/address.png): 

* after a long press (in non-simple mode only): Vespucci will add a node at the location and make a best guess at the house number and add address tags that you have been lately been using. If the node is on a building outline it will automatically add an "entrance=yes" tag to the node. The tag editor will open for the object in question and let you make any necessary further changes.
* in the node/way selected modes: Vespucci will add address tags as above and start the tag editor.
* in the property editor.

To add individual address nodes directly while in the default "Simple mode" switch to "Address" editing mode (long press on the lock button), "Add address node" will then add an address node at the location and if it is on a building outline add a entrance tag to it as described above.

Tyypillisesti osoitenumeron arvaus vaatii toimiakseen vähintään kahden osoitenumeron syöttämisen kadun molemmin puolin. Mitä enemmän numeroita on valmiina, sen parempi.

Consider using this with one of the [Auto-download](#download) modes.  

#### Kääntymisrajoitusten lisääminen

Vespuccissa on nopea tapa lisätä kääntymisrajoituksia. Tarvittaessa viivat jaetaan automaattisesti kahtia, ja sinua pyydetään valitsemaan kohteet uudelleen. 

* valitse viiva, jossa on highway-tägi (kääntymisrajoituksia voi lisätä vain maanteille; jos tarvitset rajoitusta muille viivoille, käytä yleisluontoista toimintoa "luo relaatio")
* valitse valikosta "Lisää rajoitus"
* valitse "kautta"-piste tai viiva (kosketusalue näytetään vain mahdollisille "kautta"-elementeille)
* valitse "mihin"-viiva (on mahdollista asettaa "mihin"-elementti samaksi kuin "mistä"-elementti, jolloin Vespucci olettaa sinun olevan lisäämässä "ei U-käännöstä" -rajoitusta (no_u_turn))
* aseta rajoitustyyppi

### Vespucci lukitustilassa

Kun näkyvissä on punainen lukko, voidaan käyttää kaikkia epämuokkaavia toimintoja. Lisäksi pitkä painallus kohteen päällä tai lähellä näyttää kohteesta yksityiskohtaisia tietoja, jos se on OSM-elementti.

### Muutosten tallentaminen

*(vaatii internetyhteyden)*

Valitse sama nappi tai valikkokohta kuin ladattaessa, mutta valitse nyt "Lähetä data OSM-palvelimelle".

Vespucci tukee OAuth-todennusta sekä perinteisiä käyttäjätunnusta ja salasanaa. Suositeltu tapa on OAuth, koska sen avulla vältetään salasanojen lähettäminen selkokielisinä.

Uusissa Vespucci-asennuksissa on oletuksena käytössä OAuth. Lähettäessäsi muokattua dataa ensimmäistä kertaa, avautuu näytölle sivu OSM:n verkkosivustolta. Kirjauduttuasi sisään (salattua yhteyttä pitkin) sinua pyydetään antamaan Vespuccille lupa tehdä muokkauksia tilisi kautta. Jos haluat tai sinun täytyy antaa lupa OAuth-kirjautumiseen jo ennen muokkaamista, on työkaluvalikossa toiminto tätä varten.

Jos haluat tallentaa työsi tulokset, mutta sinulla ei ole internetyhteyttä, voit tallentaa ne JOSMin ymmärtämäksi .osm-tiedostoksi, jonka voi myöhemmin lähettää Vespuccilla tai JOSMilla. 

#### Lähetyksen ristiriitojen selvittäminen

Vespucci has a simple conflict resolver. However if you suspect that there are major issues with your edits, export your changes to a .osc file ("Export" menu item in the "Transfer" menu) and fix and upload them with JOSM. See the detailed help on [conflict resolution](Conflict%20resolution.md).  

## Using GPS and GPX tracks

With standard settings Vespucci will try to enable GPS (and other satellite based navigation systems) and will fallback to determining the position via so called "network location" if this is not possible. This behaviour assumes that you in normal use have your Android device itself configured to only use GPX generated locations (to avoid tracking), that is you have the euphemistically named "Improve Location Accuracy" option turned off. If you want to enable the option but want to avoid Vespucci falling back to "network location", you should turn the corresponding option in the [Advanced preferences](Advanced%20preferences.md) off. 

Touching the ![GPS](../images/menu_gps.png) button (on the left hand side of the map display) will center the screen on the current position and as you move the map display will be padded to maintain this.  Moving the screen manually or editing will cause the "follow GPS" mode to be disabled and the blue GPS arrow will change from an outline to a filled arrow. To quickly return to the "follow" mode, simply touch GPS button or re-check the equivalent menu option. If the device doesn't have a current location the location marker/arrow will be displayed in black, if a current location is available the marker will be blue.

To record a GPX track and display it on your device select "Start GPX track" item in the ![GPS](../images/menu_gps.png) menu. This will add layer to the display with the current recorded track, you can upload and export the track from the entry in the [layer control](Main%20map%20display.md). Further layers can be added from local GPX files and tracks downloaded from the OSM API.

Note: by default Vespucci will not record elevation data with your GPX track, this is due to some Android specific issues. To enable elevation recording, either install a gravitational model, or, simpler, go to the [Advanced preferences](Advanced%20preferences.md) and configure NMEA input.

## Muistiinpanot ja virheet

Vespucci supports downloading, commenting and closing of OSM Notes (formerly OSM Bugs) and the equivalent functionality for "Bugs" produced by the [OSMOSE quality assurance tool](http://osmose.openstreetmap.fr/en/map/). Both have to either be down loaded explicitly or you can use the auto download facility to access the items in your immediate area. Once edited or closed, you can either upload the bug or Note immediately or upload all at once.

Kartalla muistiinpanot ja virheet näytetään pienellä ötökkäkuvakkeella ![Bug](../images/bug_open.png): vihreät ovat suljettuja/selvitettyjä, siniset ovat käyttäjän luomia tai muokkaamia ja keltainen väri osoittaa, ettei sitä ole vielä korjattu eikä muutettu. 

OSMOSE-virheiden tiedoissa on sininen linkki kyseessä olevaan kohteeseen, ja linkin koskettaminen valitsee kohteen, keskittää näkymän siihen ja lataa tarvittaessa alueen valmiiksi. 

### Suodattaminen

Besides globally enabling the notes and bugs display you can set a coarse grain display filter to reduce clutter. The filter configuration can be accessed from the task layer entry in the [layer control](#layers):

* Notes
* Osmose error
* Osmose warning
* Osmose minor issue
* Maproulette
* Custom

<a id="indoor"></a>

# Sisätila-tila

Mapping indoors is challenging due to the high number of objects that very often will overlay each other. Vespucci has a dedicated indoor mode that allows you to filter out all objects that are not on the same level and which will automatically add the current level to new objects created there.

The mode can be enabled by long pressing on the lock item, see [Lock, unlock, mode switching](#lock) and selecting the corresponding menu entry.

<a id="c-mode"></a>

## C-tila

In C-Mode only objects are displayed that have a warning flag set, this makes it easy to spot objects that have specific problems or match configurable checks. If an object is selected and the Property Editor started in C-Mode the best matching preset will automatically be applied.

The mode can be enabled by long pressing on the lock item, see [Lock, unlock, mode switching](#lock) and selecting the corresponding menu entry.

### Configuring checks

Currently there are two configurable checks (there is a check for FIXME tags and a test for missing type tags on relations that are currently not configurable) both can be configured by selecting "Validator settings" in the [preferences](Preferences.md). 

The list of entries is split in to two, the top half lists "re-survey" entries, the bottom half "check entries". Entries can be edited by clicking them, the green menu button allows adding of entries.

#### Re-survey entries

Re-survey entries have the following properties:

* **Key** - Key of the tag of interest.
* **Value** - Value the tag of interest should have, if empty the tag value will be ignored.
* **Age** - how many days after the element was last changed the element should be re-surveyed, if a _check_date_ tag is present that will be the used, otherwise the date the current version was create. Setting the value to zero will lead to the check simply matching against key and value.
* **Regular expression** - if checked **Value** is assumed to be a JAVA regular expression.

**Key** and **Value** are checked against the _existing_ tags of the object in question.

The _Annotations_ group in the standard presets contain an item that will automatically add a _check_date_ tag with the current date.

#### Check entries

Check entries have the following two properties:

* **Key** - Key that should be present on the object according to the matching preset.
* **Require optional** - Require the key even if the key is in the optional tags of the matching preset.

This check works by first determining the matching preset and then checking if **Key** is a "recommended" key for this object according to the preset, **Require optional** will expand the check to tags that are "optional* on the object. Note: currently linked presets are not checked.

## Suodattimet

### Tägipohjainen suodatin

The filter can be enabled from the main menu, it can then be changed by tapping the filter icon. More documentation can be found here [Tag filter](Tag%20filter.md).

### Esivalintapohjainen suodatin

An alternative to the above, objects are filtered either on individual presets or on preset groups. Tapping on the filter icon will display a preset selection dialog similar to that used elsewhere in Vespucci. Individual presets can be selected by a normal click, preset groups by a long click (normal click enters the group). More documentation can be found here [Preset filter](Preset%20filter.md).

## Vespuccin räätälöiminen

Many aspects of the app can be customized, if you are looking for something specific and can't find it, [the Vespucci website](https://vespucci.io/) is searchable and contains additional information over what is available on device.

<a id="layers"></a>

### Tason asetukset

Layer settings can be changed via the layer control ("hamburger" menu in the upper right corner), all other setting are reachable via the main menu preferences button. Layers can be enabled, disabled and temporarily hidden.

Available layer types:

* Data layer - this is the layer OpenStreetMap data is loaded in to. In normal use you do not need to change anything here. Default: on.
* Background layer - there is a wide range of aerial and satellite background imagery available. The default value for this is the "standard style" map from openstreetmap.org.
* Overlay layer - these are semi-transparent layers with additional information, for example GPX tracks. Adding an overlay may cause issues with older devices and such with limited memory. Default: none.
* Notes/Bugs display - Open Notes and bugs will be displayed as a yellow bug icon, closed ones the same in green. Default: on.
* Photo layer - Displays geo-referenced photographs as red camera icons, if direction information is available the icon will be rotated. Default: off.
* Mapillary layer - Displays Mapillary segments with markers where images exist, clicking on a marker will display the image. Default: off.
* GeoJSON layer - Displays the contents of a GeoJSON file. Default: off.
* Grid - Displays a scale along the sides of the map or a grid. Default: on. 

More information can be found in the section on the [map display](Main%20map%20display.md).

#### Preferences

* Keep screen on. Default: off.
* Large node drag area. Moving nodes on a device with touch input is problematic since your fingers will obscure the current position on the display. Turning this on will provide a large area which can be used for off-center dragging (selection and other operations still use the normal touch tolerance area). Default: off.

The full description can be found here [Preferences](Preferences.md)

#### Lisäasetukset

* Node icons. Default: on.
* Always show context menu. When turned on every selection process will show the context menu, turned off the menu is displayed only when no unambiguous selection can be determined. Default: off (used to be on).
* Enable light theme. On modern devices this is turned on by default. While you can enable it for older Android versions the style is likely to be inconsistent. 

The full description can be found here [Advanced preferences](Advanced%20preferences.md)

## Ongelmista ilmoittaminen

If Vespucci crashes, or it detects an inconsistent state, you will be asked to send in the crash dump. Please do so if that happens, but please only once per specific situation. If you want to give further input or open an issue for a feature request or similar, please do so here: [Vespucci issue tracker](https://github.com/MarcusWolschon/osmeditor4android/issues). The "Provide feedback" function from the main menu will open a new issue and include the relevant app and device information without extra typing.

If you want to discuss something related to Vespucci, you can either start a discussion on the [Vespucci Google group](https://groups.google.com/forum/#!forum/osmeditor4android) or on the [OpenStreetMap Android forum](http://forum.openstreetmap.org/viewforum.php?id=56)


