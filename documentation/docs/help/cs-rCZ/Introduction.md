_Než začneme: většina obrazovek má v nabídce odkazy na systém nápovědy na zařízení, který vám poskytuje přímý přístup k informacím relevantním pro aktuální kontext, můžete se také snadno vrátit k tomuto textu. Pokud máte větší zařízení, například tablet, můžete systém nápovědy otevřít v samostatném rozděleném okně. Všechny texty nápovědy a další (časté dotazy, návody) lze nalézt také na [stránka dokumentace Vespucci](https://vespucci.io/)._

# Úvod do Vespucci

Vespucci je plně vybavený editor OpenStreetMap, který podporuje většinu operací, které poskytují editory na stolních počítačích. Byl úspěšně testován na platformách Android 2.3 až 10.0 a různých variantách AOSP od společnosti Google. Upozornění: zatímco funkce mobilních zařízení dohnaly své soupeře ve stolních počítačích, zejména starší zařízení mají k dispozici velmi omezenou paměť a bývají spíše pomalé. Měli byste to vzít v úvahu při používání Vespucci a ponechat například oblasti, které upravujete, v přiměřené velikosti.

## Editování s Vespucci

V závislosti na velikosti obrazovky a stáří vašeho zařízení mohou být možnosti editace dostupné buď přímo skrze ikony na horním panelu, skrze rozbalovací nabídku vpravo na horním panelu, nebo skrze dolní panel (pokud tu je), a nebo skrze klávesu nabídky.

<a id="download"></a>

### Stahování dat OSM

Zvolte buď ikonu ![Přenos](../images/menu_transfer.png) nebo položku "Přenos" z menu. Zobrazí se sedm možností:

* **Stáhnout aktuální pohled** - stáhne oblast viditelnou na obrazovce a spojí ji s existujícími daty *(vyžaduje připojení k síti nebo offline datový zdroj)*
* **Přepsat stažené aktuálním pohledem** - vymaže všechna data v paměti a poté stáhne oblast viditelnou na obrazovce *(vyžaduje připojení k síti)*
* **Nahrát data na server OSM** - nahraje editace do OpenStreetMap *(vyžaduje přihlášení)* *(vyžaduje připojení k síti)*
* **Aktualizovat data** - znovu stáhne data pro všechny oblasti a aktualizuje to, co je v paměti *(vyžaduje připojení k síti)*
* **Auto. stahování dle polohy** - automaticky stahuje data v okolí vaší aktuální zeměpisné polohy *(vyžaduje připojení k síti nebo offline datový zdroj)* *(vyžaduje GPS)*
* **Auto. stahování při posunu a přiblížení** - automaticky stahuje data pro aktuálně viditelnou oblast mapy *(vyžaduje připojení k síti nebo offline datový zdroj)* *(vyžaduje GPS)*
* **Soubor…** - ukládá nebo načítá data OSM do/ze zařízení.
* **Poznámka/Chyba...** - stahuje (automaticky i manuálně) Poznámky OSM a "chyby" z nástrojů pro kontrolu kvality (momentálně OSMOSE) *(vyžaduje připojení k síti)*

Nejjednodušším způsobem stahování dat je najít požadovanou oblast, kterou chcete editovat, na mapě a poté zvolit "Stáhnout aktuální pohled". Přibližovat můžete pomocí gest, tlačítek přiblížení nebo pomocí tlačítek pro ovládání hlasitosti vašeho zařízení. Vespucci by poté měl stáhnout data pro aktuální pohled. Pro stahování dat do vašeho zařízení není zapotřebí žádného ověření.

Ve výchozím nastavení jsou nestažené oblasti ztmavené, aby se zabránilo neúmyslnému přidávání duplicit v oblastech, které nejsou staženy a zobrazeny. Chování lze změnit v [Rozšířená nastavení](Advanced%20preferences.md).

### Editování

<a id="lock"></a>

#### Uzamčení, odemčení, přepínání režimů

K zabránění nechtěných úprav se Vespucci spustí v "uzamčeném" režimu, který dovoluje pouze přibližování a posouvání mapy. Pro odemčení obrazovky klepněte na ikonu ![zámku](../images/locked.png). 

Dlouhým stisknutím ikony zámku se zobrazí nabídka se 4 možnostmi:

* **Normální** - výchozí režim úprav, lze přidávat nové objekty, upravovat, přesouvat a odstraňovat stávající. Zobrazí se jednoduchá bílá ikona zámku.
* **Pouze označení** - výběrem existujícího objektu se spustí editor vlastností, dlouhým stiskem na hlavní obrazovce se objekty přidají, ale nefungují žádné další operace s geometrií. Zobrazí se bílá ikona zámku s písmenem "T".
* **Adresa** - zapne režim Adresa, mírně zjednodušený režim se specifickými akcemi dostupnými z tlačítka [Jednoduchý režim](../en/Simple%20actions.md) "+". Zobrazí se bílá ikona zámku s písmenem "A".
* **Indoor** - zapne režim Indoor, viz [Indoor mode](#indoor). Zobrazí se bílá ikona zámku s písmenem "I".
* **C-Mode** - zapne režim C-Mode, zobrazí se pouze objekty, které mají nastavený varovný příznak, viz [C-Mode](#c-mode). Zobrazí se bílá ikona zámku s písmenem "C".

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

Více informací viz [Creating new objects in simple actions mode](Creating%20new%20objects%20in%20simple%20actions%20mode.md).

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

Můžete kopírovat nebo odstranit vybrané uzly a cesty a potom je vložit jednou nebo vícekrát do nového umístění. Rozdělení zachová OSM ID a verzi. Chcete-li vložit, dlouze stiskněte místo, do kterého chcete vložit (uvidíte nitkový kříž označující místo). Potom z nabídky vyberte možnost "Vložit".

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

Vespucci podporuje autorizaci OAuth a klasickou metodu uživatelského jména a hesla. OAuth je vhodnější, protože se vyhýbá odesílání hesel.

V nové instalaci Vespucci bude ve výchozím nastavení povolený OAuth. Při prvním pokusu o načtení upravených dat se načte stránka z webu OSM. Po přihlášení (prostřednictvím šifrovaného připojení) budete požádáni, abyste Vespucci povolil úpravu pomocí svého účtu. Pokud chcete, nebo potřebujete povolit přístup k účtu OAuth před úpravou, je v nabídce "Nástroje" příslušná položka.

Pokud chcete uložit svou práci a nemáte přístup k Internetu, můžete ji uložit do souboru .osm kompatibilního s JOSM a nahrát později pomocí Vespucci nebo JOSM. 

#### Řešení konfliktů při nahrávání

Vespucci má jednoduché řešení konfliktů. Pokud však máte podezření, že se jedná o závažné potíže s vašimi úpravami, exportujte změny do souboru .osc (položku nabídky "Exportovat" v nabídce "Přenos") a opravte je a nahrajte je pomocí JOSM. Viz podrobná nápověda k [řešení konfliktů](Conflict%20resolution.md).  

## Používání GPS a GPX stop

Se standardním nastavením se Vespucci pokusí povolit GPS (a další satelitní navigační systémy) a pokud to není možné, vrátí se k určení polohy pomocí tzv. „síťové polohy“. Toto chování předpokládá, že při běžném používání máte samotné zařízení Android nakonfigurováno tak, aby používalo pouze polohy generované GPX (abyste se vyhnuli sledování), to znamená, že máte vypnutou možnost eufemisticky pojmenovanou „Zlepšit přesnost polohy“. Pokud chcete tuto možnost povolit, ale chcete zabránit tomu, aby se Vespucci vrátila zpět do "síťového umístění", měli byste vypnout odpovídající možnost v [Pokročilé předvolby](Advanced%20preferences.md). 

Dotykem tlačítka ![GPS](../images/menu_gps.png) (na levé straně zobrazení mapy) se obrazovka vycentruje na aktuální pozici a při pohybu se zobrazení mapy vyplní, aby se toto zachovalo. Ruční přesun obrazovky nebo úpravy způsobí, že režim „sledovat GPS“ bude deaktivován a modrá šipka GPS se změní z obrysu na plnou šipku. Chcete-li se rychle vrátit do režimu „sledování“, jednoduše se dotkněte tlačítka GPS nebo znovu zaškrtněte ekvivalentní možnost nabídky. Pokud zařízení nemá aktuální polohu, značka/šipka polohy se zobrazí černě, pokud je aktuální poloha dostupná, bude značka modrá.

Chcete-li zaznamenat trasu GPX a zobrazit ji na svém zařízení, vyberte položku „Spustit trasu GPX“ v nabídce ![GPS](../images/menu_gps.png). Tím se na displej přidá vrstva s aktuálně nahranou trasou, trasu můžete nahrát a exportovat ze záznamu v [ovládání vrstvy](Main%20map%20display.md). Další vrstvy lze přidat z místních souborů GPX a stop stažených z OSM API.

Poznámka: Vespucci ve výchozím nastavení nezaznamenává údaje o nadmořské výšce pomocí vaší trasy GPX, je to způsobeno některými specifickými problémy systému Android. Chcete-li povolit záznam nadmořské výšky, buďto nainstalujte gravitační model, nebo jednodušeji přejděte na [Pokročilé předvolby] (Advanced%20preferences.md) a nakonfigurujte vstup NMEA.

## Poznámky a chyby

Vespucci podporuje stahování, komentování a zavírání OSM Notes (dříve OSM Bugs) a ekvivalentní funkcionalitu "Bugs", vytvořenou nástrojem [OSMOSE Quality Assurance Tool](http://osmose.openstreetmap.fr/en/map/). Obě musí být buď načteny explicitně, nebo můžete použít funkci Automatické stahování pro přístup k položkám v bezprostředním okolí. Po úpravě nebo zavření můžete buď nahrát chybu nebo poznámku okamžitě, nebo nahrát vše najednou.

Na mapě jsou poznámky a chyby reprezentovány malou ikonkou chyby ![Bug](../images/bug_open.png), zelené jsou uzavřené / vyřešené, modré byly vytvořeny nebo editovány vámi a žlutá označuje, že je stále aktivní a nebyla změněna. 

Chybová zpráva OSMOSE poskytne odkaz na postižený objekt v modré barvě, dotykem na odkaz vybere objekt, vystředí jej na obrazovku a v případě potřeby načte oblast předem. 

### Filtrování

Kromě globálního zapnutí zobrazení poznámek a chyb můžete nastavit hrubý filtr zobrazení, abyste omezili nepořádek. Konfigurace filtru je přístupná z položky vrstvy úloh v [ovládací prvek vrstvy](#vrstvy):

* Poznámky
* Osmose chyba
* Osmose varování
* Osmose drobná záležitost
* Maproulette
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

V současné době jsou k dispozici dvě konfigurovatelné kontroly (kontrola značek FIXME a test chybějících typových značek u relací, které v současné době nejsou konfigurovatelné), obě lze nakonfigurovat výběrem "Nastavení validátoru" v [preferencích](Preferences.md). 

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

<a id="layers"></a>

### nastavení vrstev

Nastavení vrstvy lze změnit pomocí ovládacího prvku vrstvy (nabídka "hamburger" v pravém horním rohu), všechna ostatní nastavení jsou dostupná pomocí tlačítka předvoleb v hlavní nabídce. Vrstvy lze povolit, zakázat a dočasně skrýt.

Dostupné typy vrstev:

* Datová vrstva – toto je vrstva, do které se načítají data OpenStreetMap. Při běžném používání zde nemusíte nic měnit. Výchozí: zapnuto.
* Vrstva pozadí – k dispozici je široká škála leteckých a satelitních snímků na pozadí. Výchozí hodnotou je mapa „standardního stylu“ z openstreetmap.org.
* Překryvná vrstva – jedná se o poloprůhledné vrstvy s dalšími informacemi, například stopy GPX. Přidání překryvu může způsobit problémy se staršími zařízeními a například s omezenou pamětí. Výchozí: žádný.
* Zobrazení poznámek/chyb - Otevřené poznámky a chyby se zobrazí jako žlutá ikona chyby, zavřené stejně zelené. Výchozí: zapnuto.
* Vrstva fotografie – zobrazuje georeferenční fotografie jako červené ikony fotoaparátu, pokud jsou k dispozici informace o směru, ikona se otočí. Výchozí: vypnuto.
* Vrstva Mapillary – Zobrazuje Mapillary segmenty se značkami tam, kde existují obrázky, kliknutím na značku se obrázek zobrazí. Výchozí: vypnuto.
* Vrstva GeoJSON – Zobrazuje obsah souboru GeoJSON. Výchozí: vypnuto.
* Mřížka – zobrazuje měřítko po stranách mapy nebo mřížku. Výchozí: zapnuto. 

Více informací lze nalézt v sekci na [zobrazení mapy](Main%20map%20display.md).

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


