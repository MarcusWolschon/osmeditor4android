_Before we start: most screens have links in the menu to the on-device help system giving you direct access to information relevant for the current context, you can easily navigate back to this text too. If you have a larger device, for example a tablet, you can open the help system in a separate split window.  All the help texts and more (FAQs, tutorials) can be found on the [Vespucci documentation site](https://vespucci.io/) too. You can further start the help viewer directly on devices that support short cuts with a long press on the app icon and selecting "Help"_

# Úvod do Vespucci

Vespucci is a full featured OpenStreetMap editor that supports most operations that desktop editors provide. It has been tested successfully on Google's Android 2.3 to 14.0 (versions prior to 4.1 are no longer supported) and various AOSP based variants. A word of caution: while mobile device capabilities have caught up with their desktop rivals, particularly older devices have very limited memory available and tend to be rather slow. You should take this in to account when using Vespucci and keep, for example, the areas you are editing to a reasonable size.

## Editování s Vespucci

V závislosti na velikosti obrazovky a stáří vašeho zařízení mohou být možnosti editace dostupné buď přímo skrze ikony na horním panelu, skrze rozbalovací nabídku vpravo na horním panelu, nebo skrze dolní panel (pokud tu je), a nebo skrze klávesu nabídky.

<a id="download"></a>

### Stahování dat OSM

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

Nejjednodušším způsobem stahování dat je najít požadovanou oblast, kterou chcete editovat, na mapě a poté zvolit "Stáhnout aktuální pohled". Přibližovat můžete pomocí gest, tlačítek přiblížení nebo pomocí tlačítek pro ovládání hlasitosti vašeho zařízení. Vespucci by poté měl stáhnout data pro aktuální pohled. Pro stahování dat do vašeho zařízení není zapotřebí žádného ověření.

In unlocked state any non-downloaded areas will be dimmed relative to the downloaded ones if you are zoomed in far enough to enable editing. This is to avoid inadvertently adding duplicate objects in areas that are not being displayed. In the locked state dimming is disabled, this behaviour can be changed in the [Advanced preferences](Advanced%20preferences.md) so that dimming is always active.

If you need to use a non-standard OSM API entry, or use [offline data](https://vespucci.io/tutorials/offline/) in _MapSplit_ format you can add or change entries via the _Configure..._ entry for the data layer in the layer control.

### Editování

<a id="lock"></a>

#### Uzamčení, odemčení, přepínání režimů

K zabránění nechtěných úprav se Vespucci spustí v "uzamčeném" režimu, který dovoluje pouze přibližování a posouvání mapy. Pro odemčení obrazovky klepněte na ikonu ![zámku](../images/locked.png). 

A long press on the lock icon or the _Modes_ menu in the map display overflow menu will display a menu offering 4 options:

* **Normal** - the default editing mode, new objects can be added, existing ones edited, moved and removed. Simple white lock icon displayed.
* **Tag only** - selecting an existing object will start the Property Editor, new objects can be added via the green "+" button, or long press, but no other geometry operations are enabled. White lock icon with a "T" is displayed.
* **Address** - enables Address mode, a slightly simplified mode with specific actions available from the [Simple mode](../en/Simple%20actions.md) "+" button. White lock icon with an "A" is displayed.
* **Indoor** - enables Indoor mode, see [Indoor mode](#indoor). White lock icon with an "I" is displayed.
* **C-Mode** - enables C-Mode, only objects that have a warning flag set will be displayed, see [C-Mode](#c-mode). White lock icon with a "C" is displayed.

If you are using Vespucci on an Android device that supports short cuts (long press on the app icon) you can start directly to _Address_ and _Indoor_ mode.

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

Zvolené objekty můžete posouvat. Všimněte si, že objekty lze přetahovat/posouvat pouze pokud jsou zvolené. Jednoduše uchopte vybraný objekt (v oblasti tolerance) a posuňte jej. Pokud vyberete velkou oblast tažení v [preferences](Preferences.md), získáte kolem vybraného uzlu velkou oblast, která usnadňuje umístění objektu. 

#### Přidání nového Uzlu/Bodu nebo Cesty 

Při prvním spuštění je aplikace v „Jednoduchém režimu“, to lze změnit v hlavní nabídce zrušením zatržení odpovídající volby.

##### Jednoduchý režim

Stisknutí velkého zeleného tlačítka na obrazovce s mapou zobrazí nabídku. Po zvolení jedné z nabízených možností určíte klepnutím do mapy místo, na které chcete objekt umístit. Během volby umístění lze mapu v případě potřeby přibližovat či oddalovat. 

See [Creating new objects in simple actions mode](Simple%20actions.md) for more information. Simple mode os the default for new installs.

##### Pokročilý režim (s dlouhým stiskem)
 
Dlouze stiskněte na místo, kde chcete vytvořit uzel nebo počátek cesty. Zobrazí se černý symbol "zaměřovače". 
* Pokud chcete vytvořit nový uzel (nenapojený na objekt), klikněte mimo existující objekty.
* Pokud chcete rozšířit cestu, klikněte do oblasti tolerance výběru cesty (nebo uzlu cesty). Tolerance výběru je vyznačena oranžovou oblastí okolo uzlu nebo cesty.

Jakmile se zobrazí symbol zaměřovače, máte tři možnosti:

* _Normální stisknutí na stejném místě._
* Pokud se nitkový kříž nenachází v blízkosti uzlu, opětovný dotek stejného místa vytvoří nový uzel. Pokud jste blízko cesty (ale ne poblíž uzlu), bude nový uzel na cestě (a připojen k cestě).
* Pokud je nitkový kříž v blízkosti uzlu (tj. Uvnitř toleranční zóny uzlu), dotýká se stejného místa, vybírá uzel (a otevře se editor tagů.) Není vytvořen žádný nový uzel. Akce je stejná jako v předchozím výběru.
* _Normálně se dotkněte jiného místa._ Dotyk jiného místa (mimo toleranční zónu nitkového kříže) přidá další segment cesty od původní pozice k aktuální pozici. Pokud je nitkový kříž blízko cesty nebo uzlu, nový segment bude připojen k tomuto uzlu nebo cestě.

Jednoduše se dotkněte obrazovky v místě, kam chcete přidat další uzly cesty. Poslední uzel dokončíte tak, že se ho dvakrát dotknete. Pokud se koncový uzel nachází na cestě nebo uzlu, segment se k cestě nebo uzlu připojí automaticky. 

Můžete také použít položku nabídky: Viz [Vytvoření nového objektu](Creating%20new%20objects.md) pro víc informací.

#### Přidání oblasti

OpenStreetMap momentálně nemá typ objektu "oblast" na rozdíl od jiných geo-datových systémů. Editor online "iD" se pokouší vytvořit abstrakci od základních prvků OSM, která v některých případech funguje dobře, v jiných tolik ne. Vespucci se momentálně nepokouší dělat něco podobného, takže potřebujete vědět trochu o tom, jak jsou reprezentovány oblasti:

* _closed ways (*polygons")_: nejjednodušší a nejběžnější varianty oblastí jsou cesty, které mají sdílený první a poslední uzel tvořící uzavřený "kruh" (například většina budov tohoto typu). To se dělá ve Vespucci jednoduše, prostě se připojte zpět k prvnímu uzlu, až skončíte kreslením oblasti. Poznámka: interpretace uzavřené cesty závisí na jejím označení: například pokud je uzavřená cesta označena jako budova, bude považován za plochu, pokud je označen jako kruhový objezd, pak obvykle ne. V některých situacích, ve kterých mohou být obě interpretace platné, značka "oblast" může objasnit zamýšlené použití.
* _multi-polygons_: některé oblasti mají více částí, díry a kruhy, OSM používá specifický typ vztahu (náš obecný účelový objekt, který může modelovat vztahy mezi prvky), aby se dostal kolem toho, vícestranného polygonu. Multi-polygon může mít několik "vnějších" kruhů a více "vnitřních" kruhů. Každý kruh může být buď uzavřený, jak je popsáno, nebo více různých způsobů, které mají uzly společného konce. Zatímco velké multi-polygony jsou obtížně zvládnutelné s jakýmkoliv nástrojem, ve Vespucci není obtížné vytvářet malé. 
* _coastlines_: u velmi velkých objektů, kontinentů a ostrovů dokonce i multi-polygonový model nefunguje uspokojivým způsobem. Pro přírodní cesty = pobřežní linie předpokládáme směrovou sémantiku: země je na levé straně cesty, voda na pravé straně. Vedlejším efektem je, že obecně byste neměli zvrátit směr cesty s označením pobřeží. Další informace naleznete na [OSM wiki](http://wiki.openstreetmap.org/wiki/Tag:natural%3Dcoastline).

#### Zlepšení geometrie cesty

Pokud zvětšíte dostatečně daleko zvolený způsob, uvidíte malé "x" uprostřed cesty, které jsou dostatečně dlouhé. Přetahováním "x" vytvoříte uzel způsobem, který se nachází na daném místě. Poznámka: Aby nedošlo k náhodnému vytvoření uzlů, je tato toleranční oblast pro tuto operaci poměrně malá.

#### Vyjmout, kopírovat a vložit

You can copy selected nodes and ways, and then paste once or multiple times to a new location. Cutting will retain the osm id and version, thus can only be pasted once. To paste long press the location you want to paste to (you will see a cross hair marking the location). Then select "Paste" from the menu.

#### Efektivně přidávat adresy

Vespucci podporuje funkcionalitu, která zefektivňuje měření adres předpovídáním čísel domů (levé a pravé strany ulic samostatně) a automatickým přidáváním značek _addr:street_ nebo _addr:place_ na základě naposledy použité hodnoty a blízkosti. V nejlepším případě to umožňuje přidání adresy bez jakéhokoli psaní.   

Přidání značek lze spustit stisknutím ![Adresa](../images/address.png): 

* po dlouhém stisknutí (pouze v pokročilém režimu): Vespucci přidá na místě uzel a nejlépe odhadne číslo domu a přidá adresy, které jste v poslední době používali. Pokud je uzel na obrysu budovy, automaticky přidá na uzel značku „entrance=yes“. Editor tagů se otevře pro dotyčný objekt a umožní vám provést další potřebné změny.
* ve vybraných režimech uzel / cesta: Vespucci přidá tagy adres, jak je uvedeno výše, a spustí editor tagů.
* v editoru vlastností.

Chcete-li přidat jednotlivé uzly adresy přímo, zatímco ve výchozím „Jednoduchém režimu“, přepněte do režimu úprav „Adresa“ (dlouhým stisknutím tlačítka zámku), „Přidat uzel adresy“ poté přidá uzel adresy na místo a pokud je na obrys budovy přidejte k němu vstupní štítek, jak je popsáno výše.

Předpověď čísla domu obvykle vyžaduje, aby byly na každé straně silnice zadány alespoň dvě čísla domů, čím je více dat v data, tím lépe.

Zvažte použití s jedním z režimů [Auto-download](#download).  

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

Vespucci supports OAuth 2, OAuth 1.0a authorization and the classical username and password method. Since July 1st 2024 the standard OpenStreetMap API only supports OAuth 2 and other methods are only available on private installations of the API or other projects that have repurposed OSM software.  

Authorizing Vespucci to access your account on your behalf requires you to one time login with your display name and password. If your Vespucci install isn't authorized when you attempt to upload modified data you will be asked to login to the OSM website (over an encrypted connection). After you have logged on you will be asked to authorize Vespucci to edit using your account. If you want to or need to authorize the OAuth access to your account before editing there is a corresponding item in the "Tools" menu.

Pokud chcete uložit svou práci a nemáte přístup k Internetu, můžete ji uložit do souboru .osm kompatibilního s JOSM a nahrát později pomocí Vespucci nebo JOSM. 

#### Řešení konfliktů při nahrávání

Vespucci má jednoduché řešení konfliktů. Pokud však máte podezření, že se jedná o závažné potíže s vašimi úpravami, exportujte změny do souboru .osc (položku nabídky "Exportovat" v nabídce "Přenos") a opravte je a nahrajte je pomocí JOSM. Viz podrobná nápověda k [řešení konfliktů](Conflict%20resolution.md).  

### Nearby point-of-interest display

A nearby point-of-interest display can be shown by pulling the handle in the middle and top of the bottom menu bar up. 

More information on this and other available functionality on the main display can be found here [Main map display](Main%20map%display.md).

## Používání GPS a GPX stop

Se standardním nastavením se Vespucci pokusí povolit GPS (a další satelitní navigační systémy) a pokud to není možné, vrátí se k určení polohy pomocí tzv. „síťové polohy“. Toto chování předpokládá, že při běžném používání máte samotné zařízení Android nakonfigurováno tak, aby používalo pouze polohy generované GPX (abyste se vyhnuli sledování), to znamená, že máte vypnutou možnost eufemisticky pojmenovanou „Zlepšit přesnost polohy“. Pokud chcete tuto možnost povolit, ale chcete zabránit tomu, aby se Vespucci vrátila zpět do "síťového umístění", měli byste vypnout odpovídající možnost v [Pokročilé předvolby](Advanced%20preferences.md). 

Touching the ![GPS](../images/menu_gps.png) button (normally on the left hand side of the map display) will center the screen on the current position and as you move the map display will be panned to maintain this.  Moving the screen manually or editing will cause the "follow GPS" mode to be disabled and the blue GPS arrow will change from an outline to a filled arrow. To quickly return to the "follow" mode, simply touch GPS button or re-check the equivalent menu option. If the device doesn't have a current location the location marker/arrow will be displayed in black, if a current location is available the marker will be blue.

Chcete-li zaznamenat trasu GPX a zobrazit ji na svém zařízení, vyberte položku „Spustit trasu GPX“ v nabídce ![GPS](../images/menu_gps.png). Tím se na displej přidá vrstva s aktuálně nahranou trasou, trasu můžete nahrát a exportovat ze záznamu v [ovládání vrstvy](Main%20map%20display.md). Další vrstvy lze přidat z místních souborů GPX a stop stažených z OSM API.

Poznámka: Vespucci ve výchozím nastavení nezaznamenává údaje o nadmořské výšce pomocí vaší trasy GPX, je to způsobeno některými specifickými problémy systému Android. Chcete-li povolit záznam nadmořské výšky, buďto nainstalujte gravitační model, nebo jednodušeji přejděte na [Pokročilé předvolby] (Advanced%20preferences.md) a nakonfigurujte vstup NMEA.

### How to export a GPX track?

Open the layer menu, then click the 3-dots menu next to "GPX recording", then select **Export GPX track...**. Choose in which folder to export the track, then give it a name suffixed with `.gpx` (example: MyTrack.gpx).

## Notes, Bugs and Todos

Vespucci podporuje stahování, komentování a zavírání OSM Notes (dříve OSM Bugs) a ekvivalentní funkcionalitu "Bugs", vytvořenou nástrojem [OSMOSE Quality Assurance Tool](http://osmose.openstreetmap.fr/en/map/). Obě musí být buď načteny explicitně, nebo můžete použít funkci Automatické stahování pro přístup k položkám v bezprostředním okolí. Po úpravě nebo zavření můžete buď nahrát chybu nebo poznámku okamžitě, nebo nahrát vše najednou. 

Further we support "Todos" that can either be created from OSM elements, from a GeoJSON layer, or externally to Vespucci. These provide a convenient way to keep track of work that you want to complete. 

On the map the Notes and bugs are represented by a small bug icon ![Bug](../images/bug_open.png), green ones are closed/resolved, blue ones have been created or edited by you, and yellow indicates that it is still active and hasn't been changed. Todos use a yellow checkbox icon.

The OSMOSE bug and Todos display will provide a link to the affected element in blue (in the case of Todos only if an OSM element is associated with it), touching the link will select the object, center the screen on it and down load the area beforehand if necessary. 

### Filtrování

Kromě globálního zapnutí zobrazení poznámek a chyb můžete nastavit hrubý filtr zobrazení, abyste omezili nepořádek. Konfigurace filtru je přístupná z položky vrstvy úloh v [ovládací prvek vrstvy](#vrstvy):

* Notes
* Osmose error
* Osmose warning
* Osmose minor issue
* Maproulette
* Todo

<a id="indoor"></a>

## Interierový režim

Mapování v interiéru je náročné kvůli vysokému počtu objektů, které se velmi často překrývají. Vespucci má vyhrazený vnitřní režim, který vám umožní odfiltrovat všechny objekty, které nejsou na stejné úrovni a které automaticky přidávají aktuální úroveň nově vytvořeným objektům.

Režim lze aktivovat dlouhým stisknutím tlačítka zámku, viz [Zamknout, odemknout, přepnout režim](#lock) a vybrat odpovídající položku nabídky.

<a id="c-mode"></a>

## C-Mód

V režimu C-Mód jsou zobrazeny pouze objekty, které mají nastavenou varovnou vlajku, což usnadňuje detekci objektů, které mají specifické problémy nebo odpovídají konfigurovatelným kontrolám. Pokud je vybrán objekt a editor vlastností je spuštěn v režimu C-Mód, automaticky se použije nejlépe odpovídající předvolba.

Režim lze aktivovat dlouhým stisknutím tlačítka zámku, viz [Zamknout, odemknout, přepnout režim](#lock) a vybrat odpovídající položku nabídky.

### Konfigurace kontrol

All validations can be disabled/enabled in the "Validator settings/Enabled validations" in the [preferences](Preferences.md). 

The configuration for "Re-survey" entries allows you to set a time after which a tag combination should be re-surveyed. "Check" entries are tags that should be present on objects as determined by matching presets. Entries can be edited by clicking them, the green menu button allows adding of entries.

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

<a id="layers"></a>

### nastavení vrstev

Nastavení vrstvy lze změnit pomocí ovládacího prvku vrstvy (nabídka "hamburger" v pravém horním rohu), všechna ostatní nastavení jsou dostupná pomocí tlačítka předvoleb v hlavní nabídce. Vrstvy lze povolit, zakázat a dočasně skrýt.

Dostupné typy vrstev:

* Data layer - this is the layer OpenStreetMap data is loaded in to. In normal use you do not need to change anything here. Default: on.
* Background layer - there is a wide range of aerial and satellite background imagery available. The default value for this is the "standard style" map from openstreetmap.org.
* Overlay layer - these are semi-transparent layers with additional information, for example quality assurance information. Adding an overlay may cause issues with older devices and such with limited memory. Default: none.
* Notes/Bugs display - Open Notes and bugs will be displayed as a yellow bug icon, closed ones the same in green. Default: on.
* Photo layer - Displays geo-referenced photographs as red camera icons, if direction information is available the icon will be rotated. Default: off.
* Mapillary layer - Displays Mapillary segments with markers where images exist, clicking on a marker will display the image. Default: off.
* GeoJSON layer - Displays the contents of a GeoJSON file, multiple layers can be added from files. Default: none.
* GPX layer - Displays GPX tracks and way points, multiple layers can be added from files, during recording the generate GPX track is displayed in its own one . Default: none.
* Grid - Displays a scale along the sides of the map or a grid. Default: on. 

Více informací lze nalézt v sekci na [zobrazení mapy](Main%20map%20display.md).

#### Předvolby

* Nechte obrazovku zapnutou. Výchozí: vypnuto.
* Velká oblast přetažení uzlu. Přesunutí uzlů na zařízení s dotykovým vstupem je problematické, protože vaše prsty zakrývají aktuální polohu na displeji. Zapnutím této funkce získáte velkou oblast, kterou lze použít pro přetažení mimo střed (výběr a další operace stále používají normální oblast tolerance dotyku). Výchozí: vypnuto.

Úplný popis naleznete zde [Preferences](Preferences.md)

#### Rozšířené předvolby

* Full screen mode. On devices without hardware buttons Vespucci can run in full screen mode, that means that "virtual" navigation buttons will be automatically hidden while the map is displayed, providing more space on the screen for the map. Depending on your device this may work well or not,  In _Auto_ mode we try to determine automatically if using full screen mode is sensible or not, setting it to _Force_ or _Never_ skips the automatic check and full screen mode will always be used or always not be used respectively. On devices running Android 11 or higher the _Auto_ mode will never turn full screen mode on as Androids gesture navigation provides a viable alternative to it. Default: _Auto_.  
* Node icons. Default: _on_.
* Always show context menu. When turned on every selection process will show the context menu, turned off the menu is displayed only when no unambiguous selection can be determined. Default: off (used to be on).
* Enable light theme. On modern devices this is turned on by default. While you can enable it for older Android versions the style is likely to be inconsistent. 

Úplný popis naleznete zde [Advanced preferences](Advanced%20preferences.md)

## Reporting and Resolving Issues

Pokud dojde k selhání Vespucci, nebo zjistí nekonzistentní stav, budete požádáni o odeslání výpisu havárie. Pokud tak učiníte, postupujte prosím, ale pouze jednou za konkrétní situaci. Chcete-li zadat další informace nebo otevřít problém pro požadavek na funkci nebo podobné, proveďte to prosím zde: [Vespucci issue tracker](https://github.com/MarcusWolschon/osmeditor4android/issues). Funkce „Zpětná vazba“ z hlavní nabídky otevře nový úkol a automaticky přiloží informace o aplikaci a zařížení.

If you are experiencing difficulties starting the app after a crash, you can try to start it in _Safe_ mode on devices that support short cuts: long press on the app icon and then select _Safe_ from the menu. 

If you want to discuss something related to Vespucci, you can either start a discussion on the [OpenStreetMap forum](https://community.openstreetmap.org).


