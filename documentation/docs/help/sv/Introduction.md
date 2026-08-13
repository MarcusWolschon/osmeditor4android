_Innan vi börjar: de flesta skärmar har länkar i menyn till enhetens hjälpsystem som ger dig direkt tillgång till information som är relevant för det aktuella sammanhanget. Du kan enkelt navigera tillbaka till den här texten också. Om du har en större enhet, till exempel en surfplatta, kan du öppna hjälpsystemet i ett separat delat fönster. Alla hjälptexter och mer (FAQ, handledningar) finns också på [Vespuccis dokumentationswebbplats](https://vespucci.io/). Du kan även starta hjälpvisaren direkt på enheter som stöder genvägar genom att trycka länge på appikonen och välja "Hjälp"._

# Vespucci introduktion

Vespucci är en fullfjädrad OpenStreetMap-redigerare som stöder de flesta funktioner som skrivbordsredigerare erbjuder. Den har testats framgångsrikt på Googles Android 2.3 till 14.0 (versioner före 4.1 stöds inte längre) och olika AOSP-baserade varianter. En varning: även om mobila enheters funktioner har kommit ikapp sina skrivbordsrivaler, har särskilt äldre enheter mycket begränsat tillgängligt minne och tenderar att vara ganska långsamma. Du bör ta hänsyn till detta när du använder Vespucci och hålla till exempel de områden du redigerar till en rimlig storlek.

## Redigering med Vespucci

Beroende på skärmstorlek och ålder på enheten redigering åtgärder kan antingen vara tillgängliga direkt via ikoner i det översta fältet, via en rullgardinsmeny till höger om den översta raden, från den nedre listen (i förekommande fall) eller via menyknappen.

<a id="download"></a>

### Nedladdning av OSM-data

Välj antingen överföringsikonen ![Transfer](../images/menu_transfer.png) eller menyalternativet "Transfer". Detta visar elva alternativ:

* **Ladda upp data till OSM-servern...** - granska och ladda upp ändringar till OpenStreetMap *(kräver autentisering)* *(kräver nätverksanslutning)*
* **Granska ändringar...** - granska aktuella ändringar
* **Ladda ner aktuell vy** - ladda ner området som syns på skärmen och sammanfoga det med befintlig data *(kräver nätverksanslutning eller offline-datakälla)*
* **Rensa och ladda ner aktuell vy** - rensa all data i minnet, inklusive väntande ändringar, och ladda sedan ner området som syns på skärmen *(kräver nätverksanslutning)*
* **Fråga efter Overpass...** - kör en fråga mot en Overpass API-server *(kräver nätverksanslutning)*
* **Platsbaserad automatisk nedladdning** - ladda ner ett område runt den aktuella geografiska platsen automatiskt *(kräver nätverksanslutning eller offline-data)* *(kräver GPS)*
* **Automatisk nedladdning av panorering och zoom** - ladda ner data för det aktuella kartområdet automatiskt *(kräver nätverksanslutning eller offline-data)* *(kräver GPS)*
* **Uppdatera data** - ladda ner data för alla områden igen och uppdatera vad som finns i minne *(kräver nätverksanslutning)*
* **Rensa data** - ta bort all OSM-data i minnet, inklusive väntande ändringar.
* **Fil...** - spara och ladda OSM-data till/från filer på enheten.
* **Uppgifter...** - ladda ner (automatiskt och manuellt) OSM-anteckningar och "buggar" från QA-verktyg (för närvarande OSMOSE) *(kräver nätverksanslutning)*

Det enklaste sättet att ladda ner data till enheten är att zooma och panorera till den plats du vill redigera och sedan välja "Ladda ner aktuell vy". Du kan zooma med hjälp av gester, zoomknapparna eller volymkontrollknapparna på enheten. Vespucci bör då ladda ner data för den aktuella vyn. Ingen autentisering krävs för att ladda ner data till din enhet.

I upplåst läge kommer alla icke-nedladdade områden att vara nedtonade i förhållande till de nedladdade om du zoomar in tillräckligt mycket för att möjliggöra redigering. Detta är för att undvika att oavsiktligt lägga till dubbletter av objekt i områden som inte visas. I låst läge är nedtoning inaktiverad. Detta beteende kan ändras i [Avancerade inställningar](Advanced%20preferences.md) så att nedtoning alltid är aktiv.

Om du behöver använda en OSM API-post som inte är standard, eller använda [offlinedata](https://vespucci.io/tutorials/offline/) i _MapSplit_-format, kan du lägga till eller ändra poster via posten _Configure..._ för datalagret i lagerkontrollen.

### Redigering

<a id="lock"></a>

#### Lås, upplås, lägesväxling

För att undvika oavsiktliga redigeringar startar Vespucci i "låst" läge, ett läge som bara tillåter zoomning och förflyttning av kartan. Tryck på ikonen ![Låst](../images/locked.png) för att låsa upp skärmen. 

Ett långt tryck på låsikonen eller _Lägen_-menyn i kartvisningens överflödesmeny visar en meny med 4 alternativ:

* **Normal** - standardredigeringsläget, nya objekt kan läggas till, befintliga redigeras, flyttas och tas bort. Enkel vit låsikon visas.
* **Endast tagg** - om du väljer ett befintligt objekt startas egenskapsredigeraren. Nya objekt kan läggas till via den gröna "+"-knappen eller lång tryckning, men inga andra geometriska operationer är aktiverade. Vit låsikon med ett "T" visas.
* **Adress** - aktiverar adressläget, ett något förenklat läge med specifika åtgärder tillgängliga från [Enkelt läge](../en/Simple%20actions.md) "+"-knappen. Vit låsikon med ett "A" visas.
* **Inomhus** - aktiverar inomhusläget, se [Inomhusläge](#indoor). Vit låsikon med ett "I" visas.
* **C-läge** - aktiverar C-läge, endast objekt som har en varningsflagga visas, se [C-läge](#c-läge). Vit låsikon med ett "C" visas.

Om du använder Vespucci på en Android-enhet som stöder genvägar (lång tryckning på appikonen) kan du börja direkt till _Adress_ och _Inomhus_ läge.

#### Enkeltryckning, dubbeltryckning och långt tryck

21
Som standard har valbara noder och vägar ett orange område runt sig som ungefär indikerar var du måste röra för att välja ett objekt. Du har tre alternativ:

* Enkelt tryck: Väljer objekt.
* En isolerad nod/väg markeras omedelbart.
* Om du däremot försöker välja ett objekt och Vespucci bestämmer att valet kan innebära flera objekt, visas en urvalsmeny där du kan välja det objekt du vill välja.
* Valda objekt markeras i gult.
* För mer information, se [Node vald](Node%20selected.md), [Väg vald](Way%20selected.md) och [Relation vald](Relation%20selected.md).
* Dubbelt tryck: Starta [Flerval-läge](Multiselect.md)
* Långt tryck: Skapar ett "korshår" som gör att du kan lägga till noder, se nedan och [Skapa nya objekt](Creating%20new%20objects.md). Detta är endast aktiverat om "Simple mode" är inaktiverat.

Det är en bra strategi att zooma in om du försöker redigera ett område med hög täthet.

Vespucci har ett bra "ångra/gör om" system så var inte rädd för att experimentera på din enhet, men vänligen ladda inte upp och spara ren testdata.

#### Markera / Avmarkera (enkelt tryck och "valmeny")

Peka på ett objekt för att välja och markera det. Om du pekar på skärmen i ett tomt område avmarkeras det. Om du har valt ett objekt och behöver välja något annat, peka bara på objektet i fråga. Du behöver inte avmarkera först. Ett dubbeltryck på ett objekt startar [Flervals-läge](Multiselect.md).

Observera att om du försöker markera ett objekt och Vespucci bestämmer att markeringen kan innebära flera objekt (t.ex. en nod på en väg eller andra överlappande objekt) kommer en valmeny att visas: Tryck på objektet du vill markera så markeras objektet. 

Valda objekt markeras med en tunn gul ram. Den gula ramen kan vara svår att upptäcka, beroende på kartans bakgrund och zoomfaktor. När ett val har gjorts ser du ett meddelande som bekräftar valet.

När valet är klart ser du (antingen som knappar eller menyalternativ) en lista över funktioner som stöds för det valda objektet: För mer information, se [Nod vald](Node%20selected.md), [Väg vald](Way%20selected.md) och [Relation vald](Relation%20selected.md).

#### Valda objekt: Redigera taggar

En andra tryckning på det valda objektet öppnar taggredigeraren och du kan redigera taggarna som är associerade med objektet.

Observera att för överlappande objekt (t.ex. en nod på en väg) visas urvalsmenyn igen för andra gången. Om du väljer samma objekt visas taggredigeraren; om du väljer ett annat objekt markeras helt enkelt det andra objektet.

#### Valda objekt: Flytta en nod eller väg

När du väl har valt ett objekt kan det flyttas. Observera att objekt bara kan dras/flyttas när de är markerade. Dra bara nära (dvs. inom toleranszonen för) det markerade objektet för att flytta det. Om du väljer det stora draområdet i [inställningar](Preferences.md) får du ett stort område runt den markerade noden som gör det enklare att placera objektet. 

#### Lägga till en ny nod/punkt eller väg 

Vid första uppstarten startas appen i "Enkelt läge", detta kan ändras i huvudmenyn genom att avmarkera motsvarande kryssruta.

##### Enkelt läge

Om du trycker på den stora gröna flytande knappen på kartskärmen visas en meny. När du har valt ett av alternativen blir du ombedd att trycka på skärmen på den plats där du vill skapa objektet. Panorering och zoom fortsätter att fungera om du behöver justera kartvyn. 

Se [Skapa nya objekt i läget för enkla åtgärder](Simple%20actions.md) för mer information. Enkelt läge är standardläget för nya installationer.

##### Avancerat läge (långt tryck)

Håll länge tryck där du vill att noden ska vara eller vägen ska börja. Du kommer att se ett svart "korshår"-symbol.
* Om du vill skapa en ny nod (som inte är ansluten till ett objekt), tryck bort från befintliga objekt.
* Om du vill förlänga en väg, tryck inom vägens "toleranszon" (eller en nod på vägen). Toleranszonen indikeras av områdena runt en nod eller väg.

När du ser hårkorssymbolen har du följande alternativ:

* _Normal tryckning på samma plats._
 * Om hårkorset inte är nära en nod skapas en ny nod om du trycker på samma plats igen. Om du är nära en väg (men inte nära en nod) kommer den nya noden att vara på vägen (och ansluten till vägen).
 * Om hårkorset är nära en nod (dvs. inom nodens toleranszon) markeras noden om du trycker på samma plats (och taggredigeraren öppnas. Ingen ny nod skapas. Åtgärden är densamma som valet ovan.
* _Normal tryckning på en annan plats._ Om du trycker på en annan plats (utanför hårkorsetets toleranszon) läggs ett vägsegment till från den ursprungliga positionen till den aktuella positionen. Om hårkorset var nära en väg eller nod kommer det nya segmentet att anslutas till den noden eller vägen.

Tryck bara på skärmen där du vill lägga till ytterligare noder på vägen. Avsluta genom att trycka på den sista noden två gånger. Om den sista noden finns på en väg eller nod kommer segmentet automatiskt att anslutas till vägen eller noden. 

Du kan också använda ett menyalternativ: Se [Skapa nya objekt](Creating%20new%20objects.md) för mer information.

#### Lägger till ett område

OpenStreetMap har för närvarande ingen objekttyp för "area" till skillnad från andra geodatasystem. Onlineredigeraren "iD" försöker skapa en areaabstraktion från de underliggande OSM-elementen, vilket fungerar bra under vissa omständigheter, men inte under andra. Vespucci försöker för närvarande inte göra något liknande, så du behöver veta lite om hur områden representeras:

* _stängda vägar (*polygoner")_: den enklaste och vanligaste områdesvarianten är vägar som har en gemensam första och sista nod som bildar en sluten "ring" (till exempel är de flesta byggnader av denna typ). Dessa är mycket enkla att skapa i Vespucci, anslut helt enkelt tillbaka till den första noden när du är klar med att rita området. Obs: tolkningen av den stängda vägen beror på dess taggning: till exempel om en stängd väg är taggad som en byggnad kommer den att betraktas som ett område, om den är taggad som en rondell kommer den inte att göra det. I vissa situationer där båda tolkningarna kan vara giltiga kan en "områdes"-tagg förtydliga den avsedda användningen.
* _multi-polygoner_: vissa områden har flera delar, hål och ringar som inte kan representeras med bara en väg. OSM använder en specifik typ av relation (vårt generella objekt som kan modellera relationer mellan element) för att kringgå detta, en multi-polygon. En multi-polygon kan ha flera "yttre" ringar och flera "inre" ringar. Varje ring kan antingen vara en sluten väg som beskrivits ovan, eller flera individuella vägar som har gemensamma ändnoder. Medan stora multipolygoner är svåra att hantera med vilket verktyg som helst, är små inte svåra att skapa i Vespucci.
* _kustlinjer_: för mycket stora objekt, kontinenter och öar, fungerar inte ens multipolygonmodellen på ett tillfredsställande sätt. För natural=coastline-vägar antar vi riktningsberoende semantik: landet ligger på vänster sida av vägen, vattnet på höger sida. En bieffekt av detta är att man i allmänhet inte bör vända riktningen på en väg med kustlinjemärkning. Mer information finns på [OSM-wikin](http://wiki.openstreetmap.org/wiki/Tag:natural%3Dcoastline).

#### Förbättra väg-geometri

Om du zoomar in tillräckligt långt på en vald väg ser du ett litet "x" mitt i vägsegmenten som är tillräckligt långa. Om du drar "x" skapas en nod i vägen på den platsen. Obs: för att undvika att noder skapas av misstag är beröringstoleransområdet för denna operation ganska litet.

#### Klipp ut, kopiera och klistra in

Du kan kopiera valda noder och vägar och sedan klistra in dem en eller flera gånger på en ny plats. Om du klipper ut dem behålls osm-ID och version, och kan därför bara klistras in en gång. För att klistra in, tryck länge på den plats du vill klistra in på (du kommer att se ett hårkors som markerar platsen). Välj sedan "Klistra in" från menyn.

#### Effektivt lägga till adresser

Vespucci stöder funktioner som effektiviserar adressmätning genom att förutsäga husnummer (vänster och höger sida av gatorna separat) och automatiskt lägga till _addr:street_- eller _addr:place_-taggar baserat på det senast använda värdet och närhet. I bästa fall gör detta det möjligt att lägga till en adress utan att behöva skriva någonting alls.   

Du kan lägga till taggarna genom att trycka på ![Adress](../images/address.png): 

* efter ett långt tryck (endast i icke-enkelt läge): Vespucci lägger till en nod på platsen och gör en bästa möjliga gissning av husnumret och lägger till adresstaggar som du nyligen har använt. Om noden finns på en byggnadskontur lägger den automatiskt till en "entrance=yes"-tagg till noden. Taggredigeraren öppnas för objektet i fråga och låter dig göra eventuella nödvändiga ytterligare ändringar.
* i nod/väg-valda lägen: Vespucci lägger till adresstaggar som ovan och startar taggredigeraren.
* i egenskapsredigeraren.

För att lägga till enskilda adressnoder direkt i standardläget "Enkelt läge", växla till redigeringsläget "Adress" (håll ner låsknappen). "Lägg till adressnod" lägger sedan till en adressnod på platsen och om den finns på en byggnadskontur lägger du till en entrétagg till den enligt beskrivningen ovan.

För att husnummerprediktion ska fungera krävs vanligtvis att minst två husnummer på varje sida av vägen anges. Ju fler siffror som finns i informationen, desto bättre.

Överväg att använda detta med ett av [Automatisk nedladdning](#nedladdning)-lägena.  

#### Lägger till Sväng restriktioner

Vespucci har ett snabbt sätt att lägga till svängbegränsningar. Om det behövs kommer den att dela upp vägar automatiskt och be dig att välja element igen. 

* välj en väg med en huvudväg (svängbegränsningar kan bara läggas till huvudväg, om du behöver göra detta för andra vägar, använd det generiska läget "skapa relation")
* välj "Lägg till begränsning" från menyn
* välj noden eller vägen "via" (endast möjliga "via"-element kommer att få pekområdet visat)
* välj vägen "till" (det är möjligt att gå tillbaka och ställa in "till"-elementet till "från"-elementet, Vespucci antar att du lägger till en begränsning av "ingen_u_sväng")
* ange begränsningstyp

### Vespucci i "låst" läge

När det röda låset visas är alla åtgärder som inte är redigeringsåtgärder tillgängliga. Dessutom visar ett långt tryck på eller nära ett objekt skärmen med detaljerad information om det är ett OSM-objekt.

### Spara dina ändringar

*(kräver nätverksanslutning)*

Välj samma knapp eller menyalternativ du gjorde för nedladdning och välj nu "Skicka data till OSM-servern".

Vespucci stöder OAuth 2, OAuth 1.0a-auktorisering och den klassiska användarnamn- och lösenordsmetoden. Sedan den 1 juli 2024 stöder standard OpenStreetMap API endast OAuth 2 och andra metoder är endast tillgängliga på privata installationer av API:et eller andra projekt som har återanvänt OSM-programvara.  

Att ge Vespucci åtkomst till ditt konto åt dig kräver att du loggar in en gång med ditt visningsnamn och lösenord. Om din Vespucci-installation inte är auktoriserad när du försöker ladda upp ändrade data kommer du att bli ombedd att logga in på OSM-webbplatsen (via en krypterad anslutning). När du har loggat in kommer du att bli ombedd att ge Vespucci auktorisering att redigera med ditt konto. Om du vill eller behöver auktorisera OAuth-åtkomst till ditt konto innan du redigerar finns det ett motsvarande alternativ i menyn "Verktyg".

Om du vill spara ditt arbete och inte har internetåtkomst kan du spara till en JOSM-kompatibel .osm-fil och antingen ladda upp den senare med Vespucci eller med JOSM. 

#### Lös konfliktervid uppladdning

Vespucci har en enkel konfliktlösare. Om du misstänker att det finns större problem med dina redigeringar, exportera dina ändringar till en .osc-fil ("Exportera" i menyn "Överför") och åtgärda och ladda upp dem med JOSM. Se den detaljerade hjälpen på [konfliktlösning](Conflict%20resolution.md).  

### Visning av närliggande intressanta platser

En närliggande sevärdhet kan visas genom att dra upp handtaget i mitten och överst i den nedre menyraden. 

Mer information om detta och andra tillgängliga funktioner på huvudskärmen finns här [Huvudkartvisning](Main%20map%display.md).

## Använder GPS and GPX spår

Med standardinställningarna kommer Vespucci att försöka aktivera GPS (och andra satellitbaserade navigationssystem) och återgå till att bestämma positionen via så kallad "nätverksplats" om detta inte är möjligt. Detta beteende förutsätter att du vid normal användning har din Android-enhet konfigurerad för att endast använda GPX-genererade platser (för att undvika spårning), det vill säga att du har det eufemistiskt namngivna alternativet "Förbättra platsnoggrannhet" avstängt. Om du vill aktivera alternativet men vill undvika att Vespucci återgår till "nätverksplats" bör du stänga av motsvarande alternativ i [Avancerade inställningar](Advanced%20preferences.md). 

Om du trycker på knappen ![GPS](../images/menu_gps.png) (normalt till vänster i kartvisningen) centreras skärmen på den aktuella positionen och när du flyttar dig panoreras kartvisningen för att bibehålla denna. Om du flyttar skärmen manuellt eller redigerar den inaktiveras läget "följ GPS" och den blå GPS-pilen ändras från en kontur till en fylld pil. För att snabbt återgå till "följ"-läget trycker du bara på GPS-knappen eller markerar motsvarande menyalternativ igen. Om enheten inte har en aktuell plats visas platsmarkören/pilen i svart, om en aktuell plats är tillgänglig visas markören i blå.

För att spela in ett GPX-spår och visa det på din enhet, välj "Starta GPX-spår" i menyn ![GPS](../images/menu_gps.png). Detta lägger till ett lager i displayen med det aktuella inspelade spåret. Du kan ladda upp och exportera spåret från posten i [lagerkontroll](Main%20map%20display.md). Ytterligare lager kan läggas till från lokala GPX-filer och spår som laddats ner från OSM API.

Obs: Som standard registrerar Vespucci inte höjddata med ditt GPX-spår, detta beror på vissa Android-specifika problem. För att aktivera höjdregistrering, installera antingen en gravitationsmodell eller, enklare, gå till [Avancerade inställningar](Advanced%20preferences.md) och konfigurera NMEA-ingången.

### Hur exporterar man ett GPX-spår?

Öppna lagermenyn, klicka sedan på 3-punktersmenyn bredvid "GPX-inspelning" och välj sedan **Exportera GPX-spår...**. Välj vilken mapp du vill exportera spåret till och ge det sedan ett namn med suffixet `.gpx` (exempel: MyTrack.gpx).

## Anteckningar, buggar och att-göra-uppgifter

Vespucci stöder nedladdning, kommentering och stängning av OSM-anteckningar (tidigare OSM-buggar) och motsvarande funktionalitet för "buggar" som produceras av [OSMOSE-kvalitetssäkringsverktyget](http://osmose.openstreetmap.fr/en/map/). Båda måste antingen laddas ner explicit eller så kan du använda den automatiska nedladdningsfunktionen för att komma åt objekten i ditt omedelbara område. När du har redigerat eller stängt kan du antingen ladda upp buggen eller anteckningen direkt eller ladda upp allt på en gång. 

Vidare stöder vi "Todos" som antingen kan skapas från OSM-element, från ett GeoJSON-lager eller externt till Vespucci. Dessa ger ett bekvämt sätt att hålla reda på arbete som du vill slutföra. 

På kartan representeras anteckningar och buggar av en liten buggikon ![Bug](../images/bug_open.png), gröna är stängda/lösta, blå har skapats eller redigerats av dig, och gula indikerar att de fortfarande är aktiva och inte har ändrats. Att göra-det-själv-uppgifter använder en gul kryssruteikon.

OSMOSE-buggen och Att-göra-visningen visar en länk till det berörda elementet i blått (i fallet med Att-göra endast om ett OSM-element är associerat med det). Om du trycker på länken markeras objektet, skärmen centreras på det och området laddas ner i förväg om det behövs. 

### Filtrering

Förutom att globalt aktivera visning av anteckningar och buggar kan du ställa in ett filter för grovkornig visning för att minska röran. Filterkonfigurationen kan nås från posten för aktivitetslagret i [lagerkontroll](#lager):

* Anteckningar
* Osmosfel
* Osmosvarning
* Mindre osmosproblem
* Kartroulette
* Att göra

<a id="indoor"></a>

## Inomhus-läge

Att kartlägga inomhus är utmanande på grund av det stora antalet objekt som ofta ligger över varandra. Vespucci har ett dedikerat inomhusläge som låter dig filtrera bort alla objekt som inte är på samma nivå och som automatiskt lägger till den aktuella nivån till nya objekt som skapas där.

Läget kan aktiveras genom att trycka länge på låsalternativet, se [Lås, upplås, lägesväxling] (#lock) och välja motsvarande menyalternativ.

<a id="c-mode"></a>

## C-läge

I C-läge visas endast objekt som har en varningsflagga inställd. Detta gör det enkelt att identifiera objekt som har specifika problem eller matchar konfigurerbara kontroller. Om ett objekt väljs och egenskapsredigeraren startas i C-läge kommer den bäst matchande förinställningen automatiskt att tillämpas.

Läget kan aktiveras genom att trycka länge på låsalternativet, se [Lås, upplås, lägesväxling] (#lock) och välja motsvarande menyalternativ.

### Konfigurera kontroller

Alla valideringar kan inaktiveras/aktiveras i "Valideringsinställningar/Aktiverade valideringar" i [inställningar](Preferences.md). 

Konfigurationen för "Ommätning"-poster låter dig ställa in en tid efter vilken en taggkombination ska mätas om. "Kontroll"-poster är taggar som ska finnas på objekt enligt matchande förinställningar. Poster kan redigeras genom att klicka på dem, den gröna menyknappen gör det möjligt att lägga till poster.

#### Återundersökningsposter

Omundersökningsposter har följande egenskaper:

* **Nyckel** - Nyckeln till den aktuella taggen.
* **Värde** - Värdet som taggen av intresse ska ha. Om det är tomt ignoreras taggvärdet.
* **Ålder** - Hur många dagar efter att elementet senast ändrades ska elementet undersökas igen. Om en _check_date_-tagg finns kommer den att användas, annars datumet då den aktuella versionen skapades. Om värdet ställs in på noll kommer kontrollen helt enkelt att matcha mot nyckel och värde.
* **Regulart uttryck** - om markerat antas **Värde** vara ett reguljärt JAVA-uttryck.

**Nyckel** och **Värde** kontrolleras mot de _existing_-taggarna för objektet i fråga.

Gruppen _Annoteringar_ i standardförinställningarna innehåller ett objekt som automatiskt lägger till en _check_date_-tagg med aktuellt datum.

#### Kontrollera poster

Kontrollposter har följande två egenskaper:

* **Nyckel** - Nyckel som ska finnas på objektet enligt den matchande förinställningen.

* **Kräv valfri** - Kräv nyckeln även om nyckeln finns i de valfria taggarna för den matchande förinställningen.

Den här kontrollen fungerar genom att först fastställa den matchande förinställningen och sedan kontrollera om **Nyckel** är en "rekommenderad" nyckel för detta objekt enligt förinställningen. **Kräv valfri** utökar kontrollen till taggar som är "valfria* på objektet. Obs: för närvarande länkade förinställningar är inte markerade.

## Filter

### Tagbaserat filter

Filtret kan aktiveras från huvudmenyn och sedan ändras genom att trycka på filterikonen. Mer dokumentation finns här [Tagfilter](Tag%20filter.md).

### Förinställt baserat filter

Ett alternativ till ovanstående är att objekt filtreras antingen på individuella förinställningar eller på förinställningsgrupper. Om du trycker på filterikonen visas en dialogruta för val av förinställningar, liknande den som används på andra ställen i Vespucci. Enskilda förinställningar kan väljas med ett vanligt klick, förinställningsgrupper med ett långt klick (vanligt klick öppnar gruppen). Mer dokumentation finns här [Filter för förinställningar](Preset%20filter.md).

## Anpassa Vespucci

Många aspekter av appen kan anpassas. Om du letar efter något specifikt och inte hittar det är [Vespuccis webbplats](https://vespucci.io/) sökbar och innehåller ytterligare information om vad som är tillgängligt på enheten.

<a id="layers"></a>

### Lagerinställningar

Lagerinställningar kan ändras via lagerkontrollen ("hamburger"-menyn i det övre högra hörnet), alla andra inställningar nås via inställningsknappen i huvudmenyn. Lager kan aktiveras, inaktiveras och tillfälligt döljas.

Tillgängliga lagertyper:

* Datalager - detta är lagret som OpenStreetMap-data laddas in i. Vid normal användning behöver du inte ändra något här. Standard: på.
* Bakgrundslager - det finns ett brett utbud av flyg- och satellitbilder tillgängliga som bakgrundsbilder. Standardvärdet för detta är kartan i "standardstil" från openstreetmap.org.
* Överlagringslager - dessa är halvtransparenta lager med ytterligare information, till exempel kvalitetssäkringsinformation. Att lägga till ett överlagringslager kan orsaka problem med äldre enheter och liknande med begränsat minne. Standard: ingen.
* Anteckningar/Buggvisning - Öppna anteckningar och buggar visas som en gul buggikon, stängda likadana i grönt. Standard: på.
* Fotolager - Visar georefererade fotografier som röda kameraikoner. Om riktningsinformation är tillgänglig roteras ikonen. Standard: av.
* Kartbildslager - Visar kartbildssegment med markörer där bilder finns. Om du klickar på en markör visas bilden. Standard: av.
* GeoJSON-lager - Visar innehållet i en GeoJSON-fil. Flera lager kan läggas till från filer. Standard: ingen. * GPX-lager - Visar GPX-spår och vägpunkter. Flera lager kan läggas till från filer. Under inspelning visas det genererade GPX-spåret i ett separat lager. Standard: inget.
* Rutnät - Visar en skala längs kartans sidor eller ett rutnät. Standard: på. 

Mer information finns i avsnittet om [kartvisning](Main%20map%20display.md).

#### Inställningar

* Låt skärmen vara på. Standard: av.
* Stort område för att dra noden. Att flytta noder på en enhet med pekinmatning är problematiskt eftersom dina fingrar kommer att dölja den aktuella positionen på skärmen. Att aktivera detta ger ett stort område som kan användas för att dra utanför mitten (markering och andra åtgärder använder fortfarande det normala området för pekfunktion). Standard: av.

Den fullständiga beskrivningen finns här [Inställningar](Preferences.md)

#### Avancerade inställningar

* Helskärmsläge. På enheter utan hårdvaruknappar kan Vespucci köras i helskärmsläge, det betyder att "virtuella" navigeringsknappar automatiskt döljs medan kartan visas, vilket ger mer utrymme på skärmen för kartan. Beroende på din enhet kan detta fungera bra eller inte. I _Auto_-läget försöker vi automatiskt avgöra om det är klokt att använda helskärmsläge eller inte. Om du ställer in det på _Force_ eller _Never_ hoppas den automatiska kontrollen över och helskärmsläget kommer alltid att användas respektive inte användas. På enheter som kör Android 11 eller senare kommer _Auto_-läget aldrig att aktivera helskärmsläge eftersom Androids gestnavigering erbjuder ett gångbart alternativ till det. Standard: _Auto_.
* Nodikoner. Standard: _on_.
* Visa alltid kontextmeny. När det är aktiverat visas kontextmenyn i varje valprocess. Avaktiverat visas menyn endast när inget entydigt val kan göras. Standard: av (brukade vara på).
* Aktivera ljust tema. På moderna enheter är detta aktiverat som standard. Även om du kan aktivera det för äldre Android-versioner är det troligt att stilen blir inkonsekvent. 

Den fullständiga beskrivningen finns här [Avancerade inställningar](Advanced%20preferences.md)

## Rapportera och lösa problem

Om Vespucci kraschar, eller om den upptäcker ett inkonsekvent tillstånd, kommer du att bli ombedd att skicka in kraschdumpen. Gör det om det händer, men bara en gång per specifik situation. Om du vill ge ytterligare feedback eller öppna ett ärende för en funktionsförfrågan eller liknande, gör det här: [Vespucci issue tracker](https://github.com/MarcusWolschon/osmeditor4android/issues). Funktionen "Ge feedback" från huvudmenyn öppnar ett nytt ärende och inkluderar relevant app- och enhetsinformation utan extra inmatning.

Om du har problem med att starta appen efter en krasch kan du prova att starta den i _Säkert_ läge på enheter som stöder genvägar: tryck länge på appikonen och välj sedan _Säkert_ från menyn. 

Om du vill diskutera något relaterat till Vespucci kan du antingen starta en diskussion på [OpenStreetMap-forumet](https://community.openstreetmap.org).


