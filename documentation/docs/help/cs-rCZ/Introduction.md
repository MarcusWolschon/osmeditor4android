# Úvod do Vespucci

Vespucci je plně vybavený editor OpenStreetMap, podporující většinu operací, které poskytují desktopové editory. Aplikace byla otestována pro Android 2.3 až 7.0 a také pro různé varianty založené na AOSP. Jedno upozornění: zatímco schopnosti mobilních zařízení dostihly desktopové stroje, zejména starší zařízení mají velmi omezenou dostupnou paměť a bývají spíše pomalejší. Měli byste to při používání Vespucci vzít v potaz a pamatovat, že například oblasti, které upravujete, by měli být rozumného rozsahu. 

## První použití

Po spuštění Vespucci se zobrazí dialog "Stáhnout jinou oblast"/"Načíst oblast". Pokud se vám zobrazují souřadnice a chcete zahájit stahování okamžitě, můžete zvolit odpovídající možnost a vybrat poloměr oblasti, kterou chcete stáhnout. Na pomalých zařízeních nevolte rozsáhlé oblasti. 

Alternativně lze dialog zrušit stiskem tlačítka "Zobrazit mapu", požadovanou oblast najít na mapě a poté data stáhnout (viz níže "Editování s Vespucci").

## Editování s Vespucci

V závislosti na velikosti obrazovky a stáří vašeho zařízení mohou být možnosti editace dostupné buď přímo skrze ikony na horním panelu, skrze rozbalovací nabídku vpravo na horním panelu, nebo skrze dolní panel (pokud tu je), a nebo skrze klávesu nabídky.

<a id="download"></a>

###Stahování OSM dat

Zvolte buď ikonu ![Přenos](../images/menu_transfer.png) nebo položku "Přenos" z menu. Zobrazí se sedm možností:

* **Stáhnout aktuální pohled** - stáhne oblast viditelnou na obrazovce a nahradí jakákoliv existující data *(vyžaduje připojení k síti)*
* **Přidat aktuální pohled ke stažení** - stáhne oblast viditelnou na obrazovce a sloučí ji s existujícími daty *(vyžaduje připojení k síti)*
* **Stáhnout jinou oblast** - zobrazí formulář, který umožňuje zadat souřadnice, vyhledat místo nebo použít aktuální pozici a poté stáhnout oblast kolem daného místa *(vyžaduje připojení k síti)*
* **Nahrát data na OSM server** - nahraje úpravy na OpenStreetMap *(vyžaduje ověření)* *(vyžaduje připojení k síti)*
* **Stahovat automaticky** - automaticky stahuje oblast okolo aktuální pozice *(vyžaduje připojení k síti)* *(vyžaduje GPS)*
* **Soubor...** - ukládání a načítání OSM dat do/ze souborů v zařízení
* **Poznámky/Chyby...** - stahování (automatické i manuální) OSM Poznámek a "Chyb" z QA nástrojů (aktuálně z OSMOSE) *(vyžaduje připojení k síti)*

Nejjednodušším způsobem stahování dat je najít požadovanou oblast, kterou chcete editovat, na mapě a poté zvolit "Stáhnout aktuální pohled". Přibližovat můžete pomocí gest, tlačítek přiblížení nebo pomocí tlačítek pro ovládání hlasitosti vašeho zařízení. Vespucci by poté měl stáhnout data pro aktuální pohled. Pro stahování dat do vašeho zařízení není zapotřebí žádného ověření.

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

* Single tap: Selects object. 
    * An isolated node/way is highlighted immediately. 
    * However, if you try to select an object and Vespucci determines that the selection could mean multiple objects it will present a selection menu, enabling you to choose the object you wish to select. 
    * Selected objects are highlighted in yellow. 
    * For further information see [Node selected](Node%20selected.md), [Way selected](Way%20selected.md) and [Relation selected](Relation%20selected.md).
* Double tap: Start [Multiselect mode](Multiselect.md)
* Long press: Creates a "crosshair", enabling you to add nodes, see below and [Creating new objects](Creating%20new%20objects.md). This is only enabled if "Simple mode" is deactivated.

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

#### Adding a new Node/Point or Way 

On first start the app launches in "Simple mode", this can be changed in the main menu by un-checking the corresponding checkbox.

##### Jednoduchý režim

Tapping the large green floating button on the map screen will show a menu. After you've selected one of the items, you will be asked to tap the screen at the location where you want to create the object, pan and zoom continues to work if you need to adjust the map view. 

See [Creating new objects in simple actions mode](Creating%20new%20objects%20in%20simple%20actions%20mode.md) for more information.

##### Advanced (long press) mode
 
Long press where you want the node to be or the way to start. You will see a black "crosshair" symbol. 
* If you want to create a new node (not connected to an object), click away from existing objects.
* If you want to extend a way, click within the "tolerance zone" of the way (or a node on the way). The tolerance zone is indicated by the areas around a node or way.

Jakmile se zobrazí symbol zaměřovače, máte tři možnosti:

* Dotek na stejném místě. 
* Pokud se nitkový kříž nenachází v blízkosti uzlu, opětovný dotek stejného místa vytvoří nový uzel. Pokud jste blízko cesty (ale ne poblíž uzlu), bude nový uzel na cestě (a připojen k cestě). 
* Pokud je nitkový kříž v blízkosti uzlu (tj. Uvnitř toleranční zóny uzlu) , dotýká se stejného místa, vybírá uzel (a otevře se editor tagů.) Není vytvořen žádný nový uzel Akce je stejná jako v předchozím výběru.
* Dotkněte se jiného místa. Dotyk jiného místa (mimo toleranční zónu nitkového kříže) přidá další segment cesty od původní pozice k aktuální pozici. Pokud je nitkový kříž blízko cesty nebo uzlu, nový segment bude připojen k tomuto uzlu nebo cestě.

Jednoduše klepněte na místo, kam chcete přidat další uzel cesty. Pro dokončení cesty klepněte na koncový uzel podruhé. Pokud se koncový uzel nachází na cestě nebo existujícím uzlu, tak se na ni vytvářený segment automaticky napojí. 

You can also use a menu item: See [Creating new objects](Creating%20new%20objects.md) for more information.

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

Vespucci má funkci "přidat adresní tagy", která se pokouší efektivněji mapovat adresy. Lze vybrat:

* po dlouhém stisknutí: Vespucci přidá uzel v místě a udělá nejlepší odhad na číslo domu a přidá adresní značky, které jste v poslední době používali. Pokud je uzel na obrysu budovy, automaticky přidá do uzlu značku "entrance = yes". Editor značek se otevře pro daný objekt a umožní vám provádět další nezbytné změny. 
* V uzlu / způsobu výběru režimů: Vespucci přidá adresové značky jako výše a spustí editor značek.
* v editoru značek.

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

V současné době existují dvě konfigurovatelné kontroly (existuje kontrola značek FIXME a test chybějících typových značek na relacích, které nejsou aktuálně konfigurovatelné). Obě je možné nakonfigurovat tak, že vyberete "Předvolby validátora" v části "Předvolby". 

Seznam záznamů je rozdělen na dvě části, políčka nahoře vypisují "ověřit záznamy", dolní polovina "zkontrolované záznamy". Záznamy lze editovat kliknutím na ně, zelené tlačítko nabídky umožňuje přidání položek.

#### Ověření záznamů

Položky opětovného Ověření mají následující vlastnosti:

* **Klíč** - Klíč značky, která vás zajímá. 
* **Hodnota** - Hodnota, kterou má mít značka, která by měla mít, pokud bude prázdná hodnota značky ignorována. 
* **Stáří** kolik dní poté, co byl prvek naposledy změněn, by měl  být prvek opětovně prozkoumán, pokud je k dispozici pole check_date, které bude použito, jinak bude datum vytvoření aktuální verze. Nastavení hodnoty na nulu vede ke kontrole, která se jednoduše shoduje s klíčem a hodnotou. 
* **Regulární výraz** - je-li zaškrtnuto, pak **Hodnota** se považuje za JAVA regulární výraz.

**Klíč** a **Hodnota** jsou kontrolovány proti značkám _existing_ daného objektu.

#### Kontrola položek

Kontrolní položky mají následující dvě vlastnosti:

* **Key** - Key that should be present on the object according to the matching preset.
* **Require optional** - Require the key even if the key is in the optional tags of the matching preset .

This check works by first determining the matching preset and then checking if **Key** is a "recommended" key for this object according to the preset, **Require optional** will expand the check to tags that are "optional* on the object. Note: currently linked presets are not checked.

## Filtry

### Filtr na základě tagů

Filtr lze aktivovat z hlavního menu, poté jej lze změnit klepnutím na ikonu filtru. Další dokumentaci naleznete zde [Tag filter](Tag%20filter.md).

### Přednastavený filtr

Alternativou k výše uvedeným objektům jsou objekty filtrovány buď na základě individuálních předvoleb nebo na přednastavených skupinách. Klepnutím na ikonu filtru se zobrazí přednastavené dialogové okno pro výběr, které se používá ve Vespucci. Jednotlivé předvolby lze zvolit pomocí normálního kliknutí, přednastavených skupin dlouhým kliknutím (normální kliknutí se dostane do skupiny). Další dokumentaci naleznete zde [Přednastavený filtr](Preset%20filter.md).

## Přizpůsobení Vespucci

### Nastavení, která můžete chtít změnit

* Vrstva pozadí 
* Překryvná vrstva. Přidání překryvné vrstvy může způsobit problémy se staršími zařízeními a problémy s omezenou pamětí. Výchozí stav: žádný. 
* Zobrazit poznámky / chyby. Otevřené poznámky a chyby se zobrazí jako žlutá ikona chyby, uzavřené tytéž zelené. Výchozí hodnota: zapnuto. 
* Fotografická vrstva. Zobrazuje geo-odkazované fotografie jako červené ikony fotoaparátu, pokud jsou k dispozici informace o směru, bude ikona otočena. Výchozí: vypnuto.  
* Zachovat obrazovku zapnutou. Výchozí stav: vypnuto. 
* Velký prostor pro tažení uzlu. Přesunutí uzlů na zařízení s dotykovým vstupem je problematické, protože vaše prsty zakryjí aktuální polohu na displeji. Zapnutím této funkce se vytvoří velká oblast, která může být použita pro tažení mimo centrum (volba a další operace stále používají normální oblast tolerance dotyku). Výchozí stav: vypnuto.

#### Rozšířené předvolby

* Ikony uzlu. Výchozí nastavení: zapnuto. 
* Vždy zobrazovat kontextovou nabídku. Po zapnutí každého výběrového procesu se zobrazí kontextové menu, vypnuté menu se zobrazí pouze v případě, že nelze určit jednoznačnou volbu. Výchozí nastavení: vypnuto (používá se k zapnutí). 
* Aktivovat motiv světla. U moderních zařízení je tato funkce zapnuta ve výchozím nastavení. I když ji můžete povolit pro starší verze systému Android, pravděpodobně bude styl nekonzistentní.
* Zobrazit statistiky. Zobrazí některé statistiky pro ladění, které nejsou zrovna užitečné. Výchozí nastavení: vypnuto (používá se k zapnutí).  

## Nahlašování problémů

If Vespucci crashes, or it detects an inconsistent state, you will be asked to send in the crash dump. Please do so if that happens, but please only once per specific situation. If you want to give further input or open an issue for a feature request or similar, please do so here: [Vespucci issue tracker](https://github.com/MarcusWolschon/osmeditor4android/issues). The "Provide feedback" function from the main menu will open a new issue and include the relevant app and device information without extra typing.

If you want to discuss something related to Vespucci, you can either start a discussion on the [Vespucci Google group](https://groups.google.com/forum/#!forum/osmeditor4android) or on the [OpenStreetMap Android forum](http://forum.openstreetmap.org/viewforum.php?id=56)


