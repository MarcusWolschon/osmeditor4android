# Úvod do Vespucci

Vespucci je plně vybavený editor OpenStreetMap, který podporuje většinu operací, které poskytují editory na stolních počítačích. Byl úspěšně testován na platformách Android 2.3 až 10.0 a různých variantách AOSP od společnosti Google. Upozornění: zatímco funkce mobilních zařízení dohnaly své soupeře ve stolních počítačích, zejména starší zařízení mají k dispozici velmi omezenou paměť a bývají spíše pomalé. Měli byste to vzít v úvahu při používání Vespucci a ponechat například oblasti, které upravujete, v přiměřené velikosti. 

## První použití

Při spuštění Vespucci zobrazí dialogové okno "Stáhnout jiné umístění" / "Načíst oblast" po vyžádání požadovaných oprávnění a zobrazení uvítací zprávy. Pokud máte zobrazeny souřadnice a chcete je stáhnout okamžitě, můžete vybrat příslušnou možnost a nastavit poloměr kolem místa, které chcete stáhnout. Nevybírejte velkou oblast na pomalých zařízeních. 

Alternativně lze dialog zrušit stiskem tlačítka "Zobrazit mapu", požadovanou oblast najít na mapě a poté data stáhnout (viz níže "Editování s Vespucci").

## Editování s Vespucci

V závislosti na velikosti obrazovky a stáří vašeho zařízení mohou být možnosti editace dostupné buď přímo skrze ikony na horním panelu, skrze rozbalovací nabídku vpravo na horním panelu, nebo skrze dolní panel (pokud tu je), a nebo skrze klávesu nabídky.

<a id="download"></a>

### Stahování dat OSM

Zvolte buď ikonu ![Přenos](../images/menu_transfer.png) nebo položku "Přenos" z menu. Zobrazí se sedm možností:

* **Download current view** - download the area visible on the screen and merge it with existing data *(requires network connectivity or offline data source)*
* **Clear and download current view** - clear any data in memory and then download the area visible on the screen *(requires network connectivity)*
* **Upload data to OSM server** - upload edits to OpenStreetMap *(requires authentication)* *(requires network connectivity)*
* **Update data** - re-download data for all areas and update what is in memory *(requires network connectivity)*
* **Location based auto download** - download an area around the current geographic location automatically *(requires network connectivity or offline data)* *(requires GPS)*
* **Pan and zoom auto download** - download data for the currently displayed map area automatically *(requires network connectivity or offline data)* *(requires GPS)*
* **File...** - saving and loading OSM data to/from on device files.
* **Note/Bugs...** - download (automatically and manually) OSM Notes and "Bugs" from QA tools (currently OSMOSE) *(requires network connectivity)*

Nejjednodušším způsobem stahování dat je najít požadovanou oblast, kterou chcete editovat, na mapě a poté zvolit "Stáhnout aktuální pohled". Přibližovat můžete pomocí gest, tlačítek přiblížení nebo pomocí tlačítek pro ovládání hlasitosti vašeho zařízení. Vespucci by poté měl stáhnout data pro aktuální pohled. Pro stahování dat do vašeho zařízení není zapotřebí žádného ověření.

With the default settings any non-downloaded areas will be dimmed relative to the downloaded ones, this is to avoid inadvertently adding duplicate objects in areas that are not being displayed. The behaviour can be changed in the [Advanced preferences](Advanced%20preferences.md).

### Editování

<a id="lock"></a>

#### Uzamčení, odemčení, přepínání režimů

K zabránění nechtěných úprav se Vespucci spustí v "uzamčeném" režimu, který dovoluje pouze přibližování a posouvání mapy. Pro odemčení obrazovky klepněte na ikonu ![zámku](../images/locked.png). 

Dlouhým stisknutím ikony zámku se zobrazí nabídka se 4 možnostmi:

* **Normální** - výchozí režim úprav, lze vytvářet nové objekty, existující upravovat, přesouvat a odstraňovat. Symbolizován jednoduchou bílou ikonou zámku.
* **Editování tagů** - výběr existujících objektů vyvolá Editor vlastností, dlouhý stisk na hlavní obrazovce vytvoří objekt, nicméně měnit pozice objektů není možné. Symbolizován bílou ikonou zámku s "T".
* **Vnitřní prostory** - aktivuje režim pro Editaci vnitřních prostor, viz [režim Vnitřní prostory](#indoor). Symbolizován bílou ikonou zámku s "I".
* **Režim kontroly** - aktivuje Režim kontroly, zobrazeny jsou pouze objekty splňující nastavená kritéria, viz [Režim kontroly](#c-mode). Symbolizován bílou ikonou zámku s "C".

#### Jedno klepnutí, dvě klepnutí a dlouhé stisknutí

Ve výchozím nastavení mají uzly a cesty, které lze zvolit, oranžově vyznačenou oblast do které musíte přibližně klepnout pro zvolení objektu. Existují tři možnosti:

* Jedno klepnutí: Vybírá objekt. 
    * Osamocený uzel/cesta je označena ihned. 
    * Nicméně pokud se pokusíte vybrat objekt a Vespucci uzná, že je výběr nejednoznačný, zobrazí se nabídka s výběrem umožňující vybrat objekt, který jste si přáli zvolit. 
    * Zvolené objekty jsou zvýrazněny žlutě. 
    * Více informací viz [Zvolený uzel](Node%20selected.md), [Zvolená cesta](Way%20selected.md) a [Zvolená relace](Relation%20selected.md).
* Dvě klepnutí: Zahájí [Režim vícenásobného výběru](Multiselect.md)
* Dlouhé stisknutí: Vytvoří zaměřovač "+" umožňující přidat uzly, viz níže, a [Vytváření nových objektů](Creating%20new%20objects.md). Tento způsob je možný při vypnutém „Jednoduchém režimu“.

Vyplatí se přiblížit mapu, pokud plánujete upravovat v oblasti s vysokou hustotou.

Vespucci má kvalitní systém "zpět/vpřed", takže se nemusíte bát experimentovat s vaším zařízením, nicméně prosíme nenahrávejte a neukládejte čistě testovací data.

#### Výběr / Zrušení výběru (jedno klepnutí a "nabídka výběru")

Klepněte na objekt, který chcete zvolit a označit. Klepnutím na prázdnou oblast výběr zrušíte. Pokud máte označený objekt a chcete označit jiný, jednoduše klepněte na dotyčný objekt, není nutné rušit označení původního objektu. Dvě klepnutí na objekt zahájí [Multiselect](Multiselect.md).

Všimněte si, že pokud se pokusíte vybrat objekt a Vespucci nazná, že je výběr nejednoznačný (např. uzel ležící na cestě nebo jiné překrývající se objekty), zobrazí se nabídka výběru - klepněte na objekt, který chcete vybrat. 

Vybrané objekty jsou odlišeny tenkým žlutým okrajem. Žlutý okraj může být obtížné zpozorovat v závislosti na mapovém podkladu a přiblížení. Po provedení výběru se objeví oznámení potvrzující výběr.

Po provedení výběru se zobrazí seznam podporovaných operací (buď jako tlačítka nebo jako položky menu) pro zvolený objekt: Pro více informací viz [Uzel vybránl](Node%20selected.md), [Cesta vybrána](Way%20selected.md) a [Relace vybrána](Relation%20selected.md).

#### Zvolené objekty: Úprava tagů

Druhé klepnutí na zvolený objekt otevře editor tagů, ve kterém můžete upravovat tagy přiřazené k objektu.

Všimněte si, že pro překrývající se objekty (např. uzel na cestě) se podruhé zobrazí menu s výběrem. Zvolení stejného objektu zobrazí editor tagů; zvolení jiného objektu jednoduše zvolí daný objekt.

#### Zvolené objekty: Posun Uzlu nebo Cesty

Zvolené objekty můžete posouvat. Všimněte si, že objekty lze přetahovat/posouvat pouze pokud jsou zvolené. Jednoduše uchopte vybraný objekt (v oblasti tolerance) a posuňte jej. V nastavení můžete zvolit Větší oblast pro výběr uzlu, díky které je snazší objekt uchopit a umístit. 

#### Přidání nového Uzlu/Bodu nebo Cesty 

Při prvním spuštění je aplikace v „Jednoduchém režimu“, to lze změnit v hlavní nabídce zrušením zatržení odpovídající volby.

##### Jednoduchý režim

Stisknutí velkého zeleného tlačítka na obrazovce s mapou zobrazí nabídku. Po zvolení jedné z nabízených možností určíte klepnutím do mapy místo, na které chcete objekt umístit. Během volby umístění lze mapu v případě potřeby přibližovat či oddalovat. 

Více informací viz [Creating new objects in simple actions mode](Creating%20new%20objects%20in%20simple%20actions%20mode.md).

##### Advanced (long press) mode
 
Long press where you want the node to be or the way to start. You will see a black "crosshair" symbol. 
* If you want to create a new node (not connected to an object), touch away from existing objects.
* If you want to extend a way, touch within the "tolerance zone" of the way (or a node on the way). The tolerance zone is indicated by the areas around a node or way.

Jakmile se zobrazí symbol zaměřovače, máte tři možnosti:

* _Normal press in the same place._
    * If the crosshair is not near a node, touching the same location again creates a new node. If you are near a way (but not near a node), the new node will be on the way (and connected to the way).
    * If the crosshair is near a node (i.e. within the tolerance zone of the node), touching the same location just selects the node (and the tag editor opens. No new node is created. The action is the same as the selection above.
* _Normal touch in another place._ Touching another location (outside of the tolerance zone of the crosshair) adds a way segment from the original position to the current position. If the crosshair was near a way or node, the new segment will be connected to that node or way.

Simply touch the screen where you want to add further nodes of the way. To finish, touch the final node twice. If the final node is located on a way or node, the segment will be connected to the way or node automatically. 

Můžete také použít položku nabídky: Viz [Vytvoření nového objektu](Creating%20new%20objects.md) pro víc informací.

#### Přidání oblasti

OpenStreetMap momentálně nemá typ objektu "oblast" na rozdíl od jiných geo-datových systémů. Editor online "iD" se pokouší vytvořit abstrakci od základních prvků OSM, která v některých případech funguje dobře, v jiných tolik ne. Vespucci se momentálně nepokouší dělat něco podobného, takže potřebujete vědět trochu o tom, jak jsou reprezentovány oblasti:

* _closed ways (*polygons")_: nejjednodušší a nejběžnější varianty oblastí jsou cesty, které mají sdílený první a poslední uzel tvořící uzavřený "prsten" (například většina budov tohoto typu). To se dělá ve Vespucci jednoduše, prostě se připojte zpět k prvnímu uzlu, až skončíte kreslením oblasti. Poznámka: interpretace uzavřené cesty závisí na jejím označení: například pokud je uzavřená cesta označena jako budova, bude považován za plochu, pokud je označen jako kruhový objezd, pak obvykle ne. V některých situacích, ve kterých mohou být obě interpretace platné, značka "oblast" může objasnit zamýšlené použití.
* _multi-ploygons_: některé oblasti mají více částí, díry a kroužky, OSM používá specifický typ vztahu (náš obecný účelový objekt, který může modelovat vztahy mezi prvky), aby se dostal kolem toho, vícestranného polygonu. Multi-polygon může mít několik "vnějších" kroužků a více "vnitřních" kroužků. Každý kruh může být buď uzavřený, jak je popsáno nebo více různých způsobů, které mají uzly společného konce. Zatímco velké multi-polygony jsou obtížně zvládnutelné s jakýmkoliv nástrojem, v Vespucci není obtížné vytvářet malé. 
* _coastlines_: u velmi velkých objektů, kontinentů a ostrovů dokonce i multi-polygonový model nefunguje uspokojivým způsobem. Pro přírodní cesty = pobřežní linie předpokládáme směrovou sémantiku: země je na levé straně cesty, voda na pravé straně. Vedlejším efektem je, že obecně byste neměli zvrátit směr cesty s označením pobřeží. Další informace naleznete na [OSM wiki](http://wiki.openstreetmap.org/wiki/Tag:natural%3Dcoastline).

#### Zlepšení geometrie cesty

Pokud zvětšíte dostatečně daleko zvolený způsob, uvidíte malé "x" uprostřed cesty, které jsou dostatečně dlouhé. Přetahováním "x" vytvoříte uzel způsobem, který se nachází na daném místě. Poznámka: Aby nedošlo k náhodnému vytvoření uzlů, je tato toleranční oblast pro tuto operaci poměrně malá.

#### Vyjmout, kopírovat a vložit

Můžete kopírovat nebo odstranit vybrané uzly a cesty a potom je vložit jednou nebo vícekrát do nového umístění. Rozdělení zachová OSM ID a verzi. Chcete-li vložit, dlouze stiskněte místo, do kterého chcete vložit (uvidíte nitkový kříž označující místo). Potom z nabídky vyberte možnost "Vložit".

#### Efektivně přidávat adresy

Vespucci má funkci ![Address](../images/address.png) "přidat číslo domu" , která se snaží předpovědět aktuální číslo domu a zefektivnit mapování adres. Lze vybrat:

* po dlouhém stisknutí (pouze v pokročilém režimu :): Vespucci přidá na místě uzel a nejlépe odhadne číslo domu a přidá adresy, které jste v poslední době používali. Pokud je uzel na obrysu budovy, automaticky přidá na uzel značku „entrance=yes“. Editor tagů se otevře pro dotyčný objekt a umožní vám provést další potřebné změny.
* ve vybraných režimech uzel / cesta: Vespucci přidá tagy adres, jak je uvedeno výše, a spustí editor tagů.
* v editoru vlastností.

Předpověď čísla domu obvykle vyžaduje, aby byly na každé straně silnice zadány alespoň dvě čísla domů, čím je více dat v data, tím lépe.

Zvažte použití tohoto režimu v režimu [Auto-download](#download).  

#### Přidání omezení odbočení

Vespucci má rychlý způsob, jak přidat omezení odbočení. V případě potřeby automaticky rozdělí způsoby a vyzve vás k opětovnému výběru prvků. 

* vyberte cestu s označením dálnice (pro omezení cesty je možné přidat pouze na dálnicích, pokud to potřebujete pro jiné způsoby, použijte obecný režim "vytvořit vztah") 
* z nabídky vyberte "Přidat omezení" 
* vyberte uzel "přes" nebo cestu (pouze dotyková plocha se zobrazí pomocí dotykové plochy) 
* vyberte cestu "do" (je možné zdvojnásobit zpět a nastavit prvek "to" na " z "elementu, Vespucci předpokládá, že přidáváte omezení no_u_turn) 
* nastavte typ omezení

### Vespucci je v "uzamčeném" režimu

Když je zobrazen červený zámek, jsou k dispozici všechny akce bez úpravy. Dlouhým stisknutím na objektu nebo v jeho blízkosti se zobrazí obrazovka s detailními informacemi, pokud jde o objekt OSM.

### Uložení změn

*(vyžaduje připojení k síti)*

Vyberte stejné tlačítko nebo položku nabídky, kterou jste provedli při stahování, a nyní vyberte možnost "Odeslat data na server OSM".

Vespucci podporuje autorizaci OAuth a klasickou metodu uživatelského jména a hesla. OAuth je vhodnější, protože se vyhýbá odesílání hesel.

V nové instalaci Vespucci bude ve výchozím nastavení povolený OAuth. Při prvním pokusu o načtení upravených dat se načte stránka z webu OSM. Po přihlášení (prostřednictvím šifrovaného připojení) budete požádáni, abyste Vespucci povolil úpravu pomocí svého účtu. Pokud chcete, nebo potřebujete povolit přístup k účtu OAuth před úpravou, je v nabídce "Nástroje" příslušná položka.

Pokud chcete uložit svou práci a nemáte přístup k Internetu, můžete ji uložit do souboru .osm kompatibilního s JOSM a nahrát později pomocí Vespucci nebo JOSM. 

#### Řešení konfliktů při nahrávání

Vespucci má jednoduché řešení konfliktů. Pokud však máte podezření, že se jedná o závažné potíže s vašimi úpravami, exportujte změny do souboru .osc (položku nabídky "Exportovat" v nabídce "Přenos") a opravte je a nahrajte je pomocí JOSM. Viz podrobná nápověda k [řešení konfliktů](Conflict%20resolution.md).  

## Používání GPS

Vespucci můžete použít k vytvoření stopy GPX a zobrazení v zařízení. Dále můžete zobrazit aktuální polohu GPS (nastavte možnost "Zobrazit polohu" v nabídce GPS) a / nebo nechte polohu na středu obrazovky a postupujte podle polohy (v nabídce GPS nastavte "Sledujte polohu GPS"). 

Chcete-li tuto možnost nastavit, ruční posouvání obrazovky nebo úpravy způsobí deaktivaci režimu "následovat GPS" a změnu modré šipky GPS z obrysu na plnou šipku. Chcete-li se rychle vrátit do "sledovacího" režimu, jednoduše stiskněte tlačítko GPS nebo znovu označte nabídkové menu.

## Poznámky a chyby

Vespucci podporuje stahování, komentování a zavírání OSM Notes (dříve OSM Bugs) a ekvivalentní funkcionalitu "Bugs", vytvořenou nástrojem [OSMOSE Quality Assurance Tool](http://osmose.openstreetmap.fr/en/map/). Obě musí být buď načteny explicitně, nebo můžete použít funkci Automatické stahování pro přístup k položkám v bezprostředním okolí. Po úpravě nebo zavření můžete buď nahrát chybu nebo poznámku okamžitě, nebo nahrát vše najednou.

Na mapě jsou poznámky a chyby reprezentovány malou ikonkou chyby ![Bug](../images/bug_open.png), zelené jsou uzavřené / vyřešené, modré byly vytvořeny nebo editovány vámi a žlutá označuje, že je stále aktivní a nebyla změněna. 

Chybová zpráva OSMOSE poskytne odkaz na postižený objekt v modré barvě, dotykem na odkaz vybere objekt, vystředí jej na obrazovku a v případě potřeby načte oblast předem. 

### Filtrování

Kromě toho, že globálně umožňuje zobrazování poznámek a chyb, můžete nastavit filtr hrubého zrnitého displeje pro snížení chaosu. Ve složce [Pokročilé preference](Advanced%20preferences.md) to můžete individuálně zvolit:

* Poznámky 
* Osmose chyba
* Osmose varování
* Osmose drobná záležitost 
* Vlastní

<a id="indoor"></a>

## Interierový režim

Mapování v interiéru je náročné kvůli vysokému počtu objektů, které se velmi často překrývají. Vespucci má vyhrazený vnitřní režim, který vám umožní odfiltrovat všechny objekty, které nejsou na stejné úrovni a které automaticky přidávají aktuální úroveň nově vytvořeným objektům.

Režim lze aktivovat dlouhým stisknutím tlačítka zámku, viz [Zamknout, odemknout, přepnout režim](#lock) a vybrat odpovídající položku nabídky.

<a id="c-mode"></a>

## C-Mód

V režimu C-Mód jsou zobrazeny pouze objekty, které mají nastavenou varovnou vlajku, což usnadňuje detekci objektů, které mají specifické problémy nebo odpovídají konfigurovatelným kontrolám. Pokud je vybrán objekt a editor vlastností je spuštěn v režimu C-Mód, automaticky se použije nejlépe odpovídající předvolba.

Režim lze aktivovat dlouhým stisknutím tlačítka zámku, viz [Zamknout, odemknout, přepnout režim](#lock) a vybrat odpovídající položku nabídky.

### Konfigurace kontrol

V současné době existují dvě konfigurovatelné kontroly (existuje kontrola pro značky FIXME a test na chybějící typové značky ve vztazích, které momentálně nelze konfigurovat), oba lze nakonfigurovat výběrem "Nastavení ověřovatele" v "Předvolbách". 

Seznam záznamů je rozdělen na dvě části, políčka nahoře vypisují "ověřit záznamy", dolní polovina "zkontrolované záznamy". Záznamy lze editovat kliknutím na ně, zelené tlačítko nabídky umožňuje přidání položek.

#### Ověření záznamů

Položky opětovného Ověření mají následující vlastnosti:

* **Key** - Klíč značky zájmu.
* **Hodnota** - Hodnota, kterou by měla mít požadovaná značka, pokud bude prázdná, bude ignorována.
* **Čas** - kolik dní po poslední změně prvku by měl být prvek znovu prozkoumán, pokud je použita značka _check_date_, která bude použita, jinak datum vytvoření aktuální verze. Nastavení hodnoty na nulu povede ke kontrole, která se jednoduše shoduje s klíčem a hodnotou.
* **Regulární výraz** - pokud je zaškrtnuto **Hodnota** se považuje za regulární výraz jazyka JAVA.

**Klíč** a **Hodnota** jsou kontrolovány proti značkám _existing_ daného objektu.

Skupina _Annotations_ ve standardních předvolbách obsahuje položku, která automaticky přidá značku _check_date_ s aktuálním datem.

#### Kontrola položek

Kontrolní položky mají následující dvě vlastnosti:

* **Klíč** - Klíč, který by měl být přítomen na objektu podle příslušné předvolby.
 * **Kontrola volitelných** - Zkontrolujte volitelná označení odpovídajících předvoleb.

Tato kontrola funguje nejdříve při určování přednastavení a následném zkontrolování, zda **Klíč** je pro tento objekt "doporučen" podle předvoleb, **Kontrola volitelných** rozšiřuje kontrolu na značky, které jsou na objektu "volitelné". Upozornění: Aktuální přednastavené položky nejsou zaškrtnuty.

## Filtry

### Filtr na základě tagů

Filtr lze aktivovat z hlavního menu, poté jej lze změnit klepnutím na ikonu filtru. Další dokumentaci naleznete zde [Tag filter](Tag%20filter.md).

### Přednastavený filtr

Alternativou k výše uvedeným objektům jsou objekty filtrovány buď na základě individuálních předvoleb nebo na přednastavených skupinách. Klepnutím na ikonu filtru se zobrazí přednastavené dialogové okno pro výběr, které se používá ve Vespucci. Jednotlivé předvolby lze zvolit pomocí normálního kliknutí, přednastavených skupin dlouhým kliknutím (normální kliknutí se dostane do skupiny). Další dokumentaci naleznete zde [Přednastavený filtr](Preset%20filter.md).

## Přizpůsobení Vespucci

Mnoho aspektů aplikace lze přizpůsobit, pokud hledáte něco konkrétního a nemůžete jej najít, [web Vespucci](https://vespucci.io/) je prohledávatelný a obsahuje další informace o tom, co je k dispozici na zařízení.

### nastavení vrstev

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

#### Předvolby

* Nechte obrazovku zapnutou. Výchozí: vypnuto.
* Velká oblast přetažení uzlu. Přesunutí uzlů na zařízení s dotykovým vstupem je problematické, protože vaše prsty zakrývají aktuální polohu na displeji. Zapnutím této funkce získáte velkou oblast, kterou lze použít pro přetažení mimo střed (výběr a další operace stále používají normální oblast tolerance dotyku). Výchozí: vypnuto.

Úplný popis naleznete zde [Preferences](Preferences.md)

#### Rozšířené předvolby

* Ikony uzel. Výchozí: zapnuto.
* Vždy zobrazovat kontextové menu. Když je zapnutý, každý proces výběru zobrazí kontextové menu, vypnuté menu se zobrazí, pouze pokud nelze určit jednoznačný výběr. Výchozí: vypnuto (dříve zapnuto).
* Povolit odlehčený styl. U moderních zařízení je ve výchozím nastavení zapnuto. I když ji můžete povolit pro starší verze Android, styl bude pravděpodobně nekonzistentní. 

Úplný popis naleznete zde [Advanced preferences](Advanced%20preferences.md)

## Nahlašování problémů

Pokud dojde k selhání Vespucci, nebo zjistí nekonzistentní stav, budete požádáni o odeslání výpisu havárie. Pokud tak učiníte, postupujte prosím, ale pouze jednou za konkrétní situaci. Chcete-li zadat další informace nebo otevřít problém pro požadavek na funkci nebo podobné, proveďte to prosím zde: [Vespucci issue tracker](https://github.com/MarcusWolschon/osmeditor4android/issues). Funkce „Zpětná vazba“ z hlavní nabídky otevře nový úkol a automaticky přiloží informace o aplikaci a zařížení.

Chcete-li projednat něco souvisejícího s Vespucci, můžete zahájit diskusi buď ve skupině [Vespucci Google](https://groups.google.com/forum/#!forum/osmeditor4android), nebo v [OpenStreetMap Android forum](http://forum.openstreetmap.org/viewforum.php?id=56).


