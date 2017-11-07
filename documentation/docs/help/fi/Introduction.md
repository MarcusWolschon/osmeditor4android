# Johdanto Vespucciin

Vespucci on täysipuolinen OpenStreetMap-muokkain, joka sisältää useimmat pöytäkoneiden tarjoamat toiminnot. Se on testattu toimivaksi Googlen Androidin versioilla 2.3–7.0 sekä eri AOSP-pohjaisilla alustoilla. Pieni varoituksen sana on paikallaan: vaikka mobiililaitteet ovat kuroneet pöytäkoneiden etumatkaa kiinni, varsinkin vanhemmissa malleissa muistia on niukasti ja ne tapaavat olla varsin hitaita. Tämä kannattaa ottaa huomioon Vespuccia käytettäessä, esimerkiksi pitämällä muokattavat alueet kohtuullisen kokoisina. 

## Ensimmäinen käyttökerta

Kun Vespucci käynnistyy, se näyttää dialogin "Lataa (joku muu) alue". Jos näkyvissä on koordinaatit ja haluat aloittaa latauksen heti, voit mainitun toiminnon valittuasi asettaa paikan sekä säteen, miltä kyseisen paikan ympäriltä dataa ladataan. Älä valitse laajaa aluetta, jos laitteesi on hidas. 

Vaihtoehtoisesti voit hylätä dialogin painamalla "Kartalle"-nappia, ja etsiä alueen, jota haluat muokata. Lataa data, kun olet sovitellut karttanäkymän kohdalleen. (Katso alempaa "Muokkaaminen Vespuccilla".)

## Muokkaaminen Vespuccilla

Laitteesi näytön koosta ja iästä riippuen muokkaustoiminnot voivat löytyä joko suoraan yläpalkin kuvakkeista, palkin oikean laidan alasvetovalikosta, mahdollisesta alapalkista tai valikkonäppäimen kautta.

<a id="download"></a>

### OSM-datan lataaminen

Kosketa ensin siirtokuvaketta ![Siirto](../images/menu_transfer.png) tai avaa valikosta "Siirto". Esiin tulee seitsemän valintaa:

* **Lataa nykyinen näkymä** – lataa näytöllä näkyvä alue ja korvaa kaiken jo ladatun datan *(vaatii internetyhteyden)*
* **Lisää nyk. näkymä lataukseen** – lataa näytöllä näkyvän alueen ja yhdistä se jo ladattuun dataan *(vaatii internetyhteyden)*
* **Lataa joku muu alue** – näyttää lomakkeen, johon voit syöttää koordinaatit, etsiä paikkaa tai käyttää nykyistä sijaintia, ja ladata sitten alueen kyseisen paikan ympäriltä *(vaatii internetyhteyden)*
* **Lähetä OSM-palvelimelle** – lähetä muokkaukset OpenStreetMappiin *(vaatii tunnistautumisen)* *(vaatii internetyhteyden)*
* **Automaattilataus** – lataa automaattisesti alue nykyisen maantieteellisen sijainnin ympäriltä *(vaatii internetyhteyden)* *(vaatii GPS:n)*
* **Tiedosto...** – OSM-datan tallennus ja lataus laitteen muistiin/muistista
* **Muistiinpanot/Virheet...** – lataa (automaattisesti tai käsin) OSM-muistiinpanoja tai -virheitä QA-työkaluista (nykyisin OSMOSE) *(vaatii internetyhteyden)*

Helpoin tapa ladata dataa laitteelle on etsiä muokattavaksi aiottu karttanäkymä ja avata valikosta "Lataa nykyinen näkymä". Voit suurentaa ja pienentää sormieleillä, plus- ja miinusnapilla tai laitteen äänenvoimakkuusnappuloilla. Vespuccin pitäisi sitten ladata karttanäkymää vastaava data. Sinun ei tarvitse tunnistautua datan lataamista varten.

### Muokkaaminen

<a id="lock"></a>

#### Lukitus päällä ja pois, vain tägien muokkaus, sisätila-tila 

Tahattomien muokkausten välttämiseksi Vespucci käynnistyy lukitussa tilassa, jossa voi vain siirtää ja suurentaa karttaa. Kosketa ![lukko](../images/locked.png)-kuvaketta niin lukitus menee pois päältä. 

Lukkokuvakkeen pitkä painallus vie "Vain tägien muokkaus" -tilaan, mikä ei salli kohteiden muodon muuttamista eikä niiden siirtämistä. Lukitusta tilasta tämän erottaa kuvakkeen lukon valkoinen väri. Voit kuitenkin lisätä uusia pisteitä ja viivoja pitkällä painalluksella niin kuin normaalisti.

Toinen pitkä painallus avaa [sisätila-tilan](#indoor), ja seuraava painallus päättää syklin tavalliseen muokkaustilaan.

#### Napautus, tuplanapautus ja pitkä painallus

Valittavissa olevia pisteitä ja viivoja ympäröi oletuksena oranssi alue, joka näyttää suurinpiirtein mihin pitää napauttaa kohteen valitsemiseksi. Vaihtoehtoja on kolme:

* Yksittäinen napautus: Valitsee kohteen. 
    * Erillinen piste/viiva korostetaan heti. 
    * Kuitenkin jos yrität valitat kohteen, ja Vespucci arvioi, että valinta voisi tarkoittaa useaa eri kohdetta, se näyttää ne listana, josta voi valita haluamansa kohteen. 
    * Valitut kohteet korostetaan keltaisella. 
    * Lisätietoa saat kohdista [Piste valittuna](../fi/Piste%20valittuna.md), [Viiva valittuna](../fi/Viiva%20valittuna.md) ja [Relaatio valittuna](../fi/Relaatio%20valittuna.md).
* Tuplanapautus: Aloittaa [monivalintatilan](../fi/Monivalinta.md)
* Pitkä painallus: Tekee "hiusristikon", jonka avulla voit lisätä pisteen, ks. alempaa ja [Uusien kohteiden luominen](../fi/Uusien%20kohteiden%20luominen.md)

Kannattaa lähentää näkymää tiheään kartoitettua aluetta muokattaessa.

Vespuccissa on hyvä peruutus- ja palautustoiminnot, joten voit huoletta tehdä kokeiluja laitteellasi. Älä kuitenkaan lähetä tai tallenna puhdasta testidataa.

#### Valitseminen ja valinnan poisto (yksi napautus ja "valintavalikko")

Kohde valitaan ja korostetaan, kun sitä koskettaa. Kosketus ruudun tyhjään osaan poistaa valinnan. Jos jokin kohde on valittuna, ja pitää valita jokin toinen kohde, ei valintaa tarvitse ensin poistaa, vaan voit koskettaa suoraan uutta kohdetta. Tuplanapautus kohteeseen aloittaa [Monivalinnan](../en/Multiselect.md).

Huomaa että jos Vespucci ei valintatilanteessa ole varma mitä kohdetta yrität valita (vaikkapa viivan pistettä tai muita päällekkäisiä kohteita), sinulle näytetään valintavalikko: kosketa kohdetta, jonka haluat valita, ja valinta on valmis. 

Valitut kohteet ilmaistaan ohuen keltaisen reunuksen avulla. Keltaista reunusta voi olla vaikea huomata taustakartasta ja suurennuksesta riippuen. Valinnan toteuduttua näytetään siitä vahvistukseksi ilmoitus.

Kun valinta on valmis, näytetään sinulle (joko nappeina tai valikkokohteina) lista toiminnoista, jotka kohteelle voidaan suorittaa: Lisätietoa saat kohdista [Piste valittuna](../fi/Piste%20valittuna.md), [Viiva valittuna](../fi/Viiva%20valittuna.md) ja [Relaatio valittuna](../fi/Relaatio%20valittuna.md).

#### Valitut kohteet: tägien muokkaaminen

Uusi kosketus valittuun kohteeseen avaa tägimuokkaimen, jossa voit muokata kohteelle kuuluvia tägejä.

Huomaa että päällekkäisten kohteiden tapauksessa (kuten piste viivalla) valintavalikko näytetään uudelleen. Saman kohteen valitseminen avaa tägimuokkaimen, mutta jos valitset jonkun toisen kohteen, tämä yksinkertaisesti valitaan.

#### Valitut kohteet: Pisteen tai viivan siirtäminen

Kun jokin kohde on valittuna, sitä voi siirtää. Huomaa että kohteita voi siirtää vain, jos ne on valittu. Siirto tehdään yksinkertaisesti vetämällä kohdetta sen läheltä (toleranssialueen sisältä). Asetuksista voit valita laajan vetoalueen valitun kohteen ympärille, jolloin sen siirtäminen tarkasti tiettyyn kohtaan on helpompaa. 

#### Uuden pisteen tai viivan lisääminen (pitkä painallus)

Paina pitkään siihen, mihin haluat uuden pisteen, tai mistä haluat aloittaa uuden viivan. Paikkaan ilmestyy "hiusristikko".
* Jos haluat luoda uuden pisteen (ei viivalle), paina kauas olemassa olevissa kohteista.
* Jos haluat jatkaa viivaa, paina viivan (tai viivan pisteen) valinta-alueen sisään. Toleranssialue näytetään värillisenä alueena pisteen tai viivan ymäpärillä.

Kun hiusristikko on asetettu, on valittavana seuraavat toimenpiteet:

* Kosketus samaan paikkaan.
    * Ellei hiusristikko ole lähellä pistettä, saman kohdan koskettaminen lisää uuden pisteen. Jos se on lähellä viivaa (mutta ei pistettä), uusi piste lisätään viivaan (ja on siis siinä kiinni).
    * Jos hiusristikko on lähellä pistettä (eli pisteen toleranssialueen sisällä), saman kohdan koskettaminen vain valitsee pisteen (ja tägimuokkain aukeaa). Uutta pistettä ei lisätä. Toiminto on sama kuin edellä kuvattu valitseminen.
* Kosketus toiseen paikkaan. Toisen kohdan (hiusristikon toleranssialueen ulkopuolella) koskettaminen lisää viivanpätkän ensimmäisestä kohdasta jälkimmäiseen. Jos hiusristikko oli lähellä viivaa tai pistettä, uusi pätkä on kiinni tässä pisteessä tai viivassa.

Voit lisätä viivaan lisää pisteitä koskettamalla näyttöä haluamiisi kohtiin. Lopetus tapahtuu koskettamalla viimeistä pistettä kahdesti. Jos viimeinen piste on jonkin viivan tai pisteen kohdalla, uusi viiva kiinnittyy siihen automaattisesti. 

Toiminto löytyy myös valikosta: Katso lisätietoa kohdasta [Uusien kohteiden luominen](../fi/Uusien%20kohteiden%20luominen.md).

#### Alueen lisääminen

OpenStreetMapissä ei nykyisellään ole elementtiä, jonka tyyppi olisi "alue", toisin kuin muissa geodata-järjestelmissä. Verkossa toimiva muokkain "iD" koettaa luoda alue-abstraktion pohjana olevista OSM:n elementeistä – joissain tilanteissa se toimii hyvin, toisissa huonommin. Vespucci ei toistaiseksi yritä mitään samantapaista, joten sinun tulee tietää hiukan siitä miten viiva-alueet esitetään: 

* _suljetut viivat (*polygonit")_: yksinkertaisin ja yleisin aluetyyppi on viiva, jolla on sama alku- ja loppupiste, muodostaen suljetun "renkaan" (esimerkiksi useimmat rakennukset ovat tälläisiä). Tämä on erittäin helppo tehdä Vespuccilla: riittää että liität viimeisen pätkän takaisin viivan ensimmäiseen pisteeseen, kun olet saanut alueen piirrettyä. Huom: suljetun viivan tulkinta riippuu sen tägeistä; jos suljettu viiva on tägätty vaikkapa rakennukseksi, se käsitetään alueeksi, mutta jos se on tägätty kiertoliittymäksi, niin näin ei ole. Joissain tilanteissa, missä kumpikin tulkinta on mahdollinen, area-tägillä voi selventää viivan käyttötarkoituksen.
* _moni-monikulmio_: joissain alueissa on useita osia, reikiä ja renkaita, joita ei kertakaikkiaan voi esittää yhdellä viivalla. Ongelman kiertämiseksi OSM käyttää tietyntyyppistä relaatiota (yleiskäyttöinen elementti, jolla voi mallintaa elementtien välisiä suhteita), nimittäin "moni-monikulmiota". Moni-monikulmiossa voi olla monta "ulkorengasta" ja monta "sisärengasta". Joka rengas voi olla edellä kuvattu suljettu viiva tai muodostua monesta yksittäisestä viivasta, joilla on yhteiset päätepisteet. Vaikka laajoja moni-monikulmioita on hankala käsitellä millä tahansa työkalulla, ei pienten tekeminen Vespuccilla ole vaikeaa. 
* _rantaviivat_: todella laajojen kohteiden kuten mannerten ja saarten mallinnuksessa edes moni-monikulmio ei toimi tyydyttävällä tavalla. Viivoilla, joilla on tägi natural=coastline, oletetaan olevan suunnasta riippuva merkitys: maa on viivan vasemmalla ja vesi oikealla puolella. Tämän sivuvaikutus on se, että rantaviivaksi tägätyn viivan suuntaa ei (yleisesti ottaen) pidä kääntää. Lisätietoa on saatavilla [OSM-wikistä](http://wiki.openstreetmap.org/wiki/Tag:natural%3Dcoastline).

#### Viivan muodon parantaminen

Jos lähennät valittua viivaa tarpeeksi, voit nähdä pienen x-merkin riittävän pitkien viivanpätkien keskikohdassa. Pikku-x:n vetäminen lisää viivaan kyseiseen kohtaan pisteen. Huom: jottei pisteitä tulisi luotua vahingossa, tämän toiminnon toleranssialue on verrattain pieni.

#### Leikkaa, kopioi & liitä

Voit kopioida ja leikata sekä pisteitä että viivoja, ja liittää ne sitten kerran tai monesti uuteen paikkaan. Leikkaaminen säilyttää OSM-tunnuksen ja -version. Liittäminen aloitetaan painamalla pitkään kohtaa, johon halutaan liittää (merkiksi ilmestyy hiusristikko). Sitten valikosta valitaan "Liitä".

#### Osoitteiden lisääminen kätevästi

Vespuccissa on toiminto "lisää osoitetägit", jonka tarkoitus on tehdä osoitteiden syöttämisestä kätevämpää. Se voidaan suorittaa:

* pitkän painalluksen jälkeen: Vespucci lisää ko. paikkaan pisteen ja tekee parhaansa osoitenumeron ja muiden lähiaikoina käyttämiesi osoitetägien arvaamiseksi. Jos piste on rakennuksen ääriviivalla, lisätään siihen automaattisesti tägi "entrance=yes". Piste avataan tägimuokkaimeen, jotta voit tarpeen vaatiessa tehdä muutoksia.
* tiloissa piste/viiva valittuna: Vespucci lisää osoitetägit kuten yllä ja avaa tägimuokkaimen.
* tägimuokkaimessa.

Tyypillisesti osoitenumeron arvaus vaatii toimiakseen vähintään kahden osoitenumeron syöttämisen kadun molemmin puolin. Mitä enemmän numeroita on valmiina, sen parempi.

Kannattaa harkita käyttöä yhdessä [automaattilatauksen](#download) kanssa.  

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

Vespuccilla on yksinkertainen ristiriitojen selvitystyökalu. Jos kuitenkin epäilet, että muokkauksissasi on vakavia virheitä, niin vie muutokset .osc-tiedostoon ("Siirto"-valikon toiminto "Vie") ja korjaa ja lähetä ne JOSMilla. Katso tarkemmat ohjeet kohdasta [ristiriitojen selvittäminen](../en/Conflict%20resolution.md).  

## GPS:n käyttäminen

Vespuccilla voit luoda GPX-jälkiä ja katsella niitä laitteellasi. Lisäksi voit näyttää nykyisen GPS-sijaintisi (aseta "Näytä sijainti" GPS-valikosta) ja/tai keskittää näkymän sijaintiin ja seurata sitä (aseta "Seuraa GPS-sijaintia" GPS-valikosta). 

Jos jälkimmäinen asetus on päällä, näkymän liikuttaminen käsin tai sen muokkaaminen poistaa GPS:n seurannan, ja sininen GPS-nuoli muuttuu ääriviivasta täytetyksi nuoleksi. Pääset nopeasti takaisin seurantatilaan koskettamalla GPS-nappia, tai uudelleen valikon kautta.

## Muistiinpanot ja virheet

Vespucci tukee OSM:n muistiinpanojen (aiemmin OSM:n virheet) lataamista, kommentoimista ja sulkemista sekä vastaavia toimintoja [OSMOSE-laadunvarmistustyökalun](http://osmose.openstreetmap.fr/en/map/) "virheille". Kummatkin pitää joko ladata käsin tai käyttäen automaattista latausta lähistöllä sijaitsevien kohteiden hakemiseen. Muokkauksen tai sulkemisen jälkeen virheet tai muistiinpanot voi lähettää saman tien yksitellen tai myöhemmin kaikki samalla kertaa.

Kartalla muistiinpanot ja virheet näytetään pienellä ötökkäkuvakkeella ![Bug](../images/bug_open.png): vihreät ovat suljettuja/selvitettyjä, siniset ovat käyttäjän luomia tai muokkaamia ja keltainen väri osoittaa, ettei sitä ole vielä korjattu eikä muutettu. 

OSMOSE-virheiden tiedoissa on sininen linkki kyseessä olevaan kohteeseen, ja linkin koskettaminen valitsee kohteen, keskittää näkymän siihen ja lataa tarvittaessa alueen valmiiksi. 

### Suodattaminen

Muistiinpanojen ja virheiden päälle laittamisen lisäksi voit asettaa karkean suodatuksen, ettei näkymä täyty niistä. Lisävalinnoista voit valita yksitellen:

* Muistiinpanot
* Osmosen virheet
* Osmosen varoitukset
* Osmosen pikkuongelmat

<a id="indoor"></a>

# Sisätila-tila

Sisätilojen kartoittaminen on haastavaa kohteiden suuren määrän vuoksi, ja koska ne usein ovat päällekkäin. Vespuccissa on erityinen sisätila-tila, jonka avulla voit piilottaa muilla tasoilla olevat kohteet ja lisätä automaattisesti nykyisen tason uusin kohteisiin.

Tämän tilan saa käyttöön lukkokuvakkeen pitkällä painalluksella, katso [Lukitus päällä ja pois, vain tägien muokkaus, sisätila-tila](#lock).

## Suodattimet

### Tägipohjainen suodatin

Suodattimen voi ottaa käyttöön päävalikosta, jonka jälkeen sitä voi muokata napauttamalla suodatinkuvaketta. Lisätietoa saa täältä: [Tägisuodatin](../en/Tag%20filter.md).

### Esivalintapohjainen suodatin

Vaihtoehtona edelliselle kohteet suodatetaan joko yksittäisten esivalintojen tai esivalintaryhmien perusteella. Suodatinkuvakkeen napautus tuo esiin samanlaisen esivalintojen valintaikkunan mitä käytetään Vespuccissa muuallakin. Yksittäisiä esivalintoja voi valita normaalilla painalluksella, ja esivalintaryhmiä pitkällä painalluksella (normaali painallus avaa ryhmän). Lisätietoa on saatavilla täältä: [Esivalintasuodatin](../en/Preset%20filter.md).

## Vespuccin räätälöiminen

### Asetuksia jotka ehkä haluat muuttaa

* Taustataso
* Peittotaso. Peittotason käyttö saattaa aiheuttaa ongelmia vanhojen ja vähämuististen laitteiden kanssa. Oletus: ei tasoa.
* Muistiinpanojen/Virheiden näyttö. Avoimet muistiinpanot ja virheet näytetään keltaisina ötökkäkuvakkeina, suljetut vihreinä. Oletus: käytössä.
* Valokuvataso. Näyttää georeferoidut kuvat punaisina kamerakuvakkeina; jos kuvassa on myös suuntatieto, kuvaketta pyöräytetään sen mukaisesti. Oletus: ei käytössä.
* Pistekuvakkeet. Oletus: käytössä.
* Pidä näyttö päällä. Oletus: ei käytössä.
* Laaja vetoalue pisteille. Pisteiden liikuttelu kosketusnäytöllä on ongelmallista, sillä sormet peittävät kosketettavan kohdan näytöllä. Kun tämä asetus on käytössä, voidaan pistettä vetää laajemmalta alueelta pisteen vierestä. Valitseminen ja muut toiminnot käyttävät silti tavallista toleranssialuetta. Oletus: ei käytössä.

#### Lisäasetukset

* Näytä kontekstivalikko aina. Kun tämä on käytössä, kontekstivalikko näytetään aina kun kohteita ollaan valitsemassa, muutoin vain kun valinta ei ole yksiselitteinen. Oletus: pois käytöstä (aiemmin käytössä).
* Käytä vaaleaa teemaa. Uusissa laitteissa tämä on käytössä oletuksena. Vaikka teeman voi ottaa käyttöön vanhoissakin Android-versioissa, tyyli on todennäköisesti epäjohdonmukainen.
* Näytä tilastotietoa. Tämä näyttää vähän tietoja virheenkorjausta varten. Ei kovin hyödyllinen. Oletus: pois päältä (aiemmin päällä).  

## Ongelmista ilmoittaminen

Jos Vespucci kaatuu, tai se havaitsee epäjohdonmukaisen tilan, sinua pyydetään lähettämään kaatumispaketti. Pyydämme tekemään niin, jos näin tapahtuu, mutta mielellään vain kerran aina tiettyä tilannetta kohden. Jos haluat antaa lisää tietoa tai avata ominaisuuspyynnön tai muuta vastaavaa, voit tehdä sen täällä: [Vespucci issue tracker](https://github.com/MarcusWolschon/osmeditor4android/issues) (englanniksi). Jos haluat keskustella jostain Vespucciin liittyvästä, voit joko aloittaa keskustelun [Vespuccin Google-ryhmässä](https://groups.google.com/forum/#!forum/osmeditor4android) tai [OpenStreetMapin Android-foorumilla](http://forum.openstreetmap.org/viewforum.php?id=56).


