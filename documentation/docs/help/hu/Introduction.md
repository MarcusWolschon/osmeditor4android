# Vespucci bevezetés

A Vespucci egy teljes körű OpenStreetMap-szerkesztő, amely támogatja a legtöbb olyan műveletet, amelyet asztali gépen futó szerkesztőkkel el lehet végezni. Sikeresen tesztelték a Google Android 2.3–7.0 verzióin és különféle AOSP változatokon. Egy kis figyelmeztetés: a mobileszközök képességei ugyan utolérték az asztali versenytársaikéit, ám elsősorban a régebbi eszközök rendelkezésre álló memóriája korlátozott, ezért előfordulhat, hogy lassúak. A Vespucci használatánál ezt figyelembe kell venni, és például célszerű a szerkesztendő területet észszerű méretek között tartani. 

## Első használat

Indításkor a Vespucci a „Más helyszín letöltése” / „Terület betöltése” párbeszédet mutatja. Ha megjelentek a koordináták, és azonnal szeretné letölteni, kiválaszthatja a megfelelő opciót, és kijelölheti a helyszín körüli letöltendő terület sugarát. Lassú eszközön ne jelöljön ki nagy területet. 

Másrészt be is zárhatja a párbeszédet az „Ugrás a térképre” gomb megnyomásával, majd a térképen a szerkesztendő területre nagyíthat, és letöltheti az adatokat (lásd lejjebb: „Szerkesztés a Vespuccival”).

## Szerkesztés a Vespuccival

A képernyő méretétől és a készülék életkorától függően a szerkesztési műveletek a következő helyekről érhetők el: közvetlenül a felső sáv ikonjaiból, a felső sáv jobb oldalán található legördülő menüből, az alsó sávból (ha van) vagy a menügombból.

<a id="download"></a>

### OSM adatok letöltése

Jelölje ki vagy az átvitel ikont ![Transfer](../images/menu_transfer.png) vagy az „Átvitel” menüpontot. Ez hét lehetőséget fog megjeleníteni:

* **Jelenlegi nézet letöltése** – letölti a képernyőn látható területet és felvált minden meglévő adatot *(hálózati kapcsolatot igényel)*
* **Jelenlegi nézet hozzáadása a letöltéshez** – letölti a képernyőn látható területet, és egyesíti a már meglévő adatokkal *(hálózati kapcsolatot igényel)*
* **Más helyszín letöltése** – megjelenít egy űrlapot, amelybe koordinátákat írhat, helyet kereshet vagy felhasználhatja a jelenlegi pozícióját, majd így letöltheti az így megadott hely körüli területet *(hálózati kapcsolatot igényel)*
* **Adatok feltöltése az OSM-kiszolgálóra** – feltölti a szerkesztéseket az OpenStreetMapre *(hitelesítést igényel)* *(hálózati kapcsolatot igényel)*
* **Automatikus letöltés** – automatikusan letölti a jelenlegi földrajzi hely körüli területet *(hálózati kapcsolatot igényel)* *(GPS-t igényel)*
* **Fájl…** – OSM-adatok mentése fájlba, és betöltés a készüléken található fájlokból.
* **Megjegyzések / hibák…** – (automatikusan és kézileg) letölti az OSM megjegyzéseket és hibákat egyes minőségbiztosítási eszközökről (jelenleg az OSMOSE-ról) *(hálózati kapcsolatot igényel)*

Az adatok eszközre töltésének legkönnyebb módja a szerkesztendő területre görgetés és nagyítás, aztán a „Jelenlegi nézet letöltése”. Gesztusokkal, a nagyítási gombokkal és a hangerőszabályzó gombokkal nagyíthat. A Vespucci aztán letölti a jelenlegi nézet adatait. Az adatok eszközre letöltéséhez nem szükséges hitelesítés.

### Szerkesztés

<a id="lock"></a>

#### Zárolás, feloldás, módváltás

A véletlen szerkesztések elkerülése miatt a Vespucci „zárolt” módban indul, olyan módban, amely csak a nagyítást és a térkép mozgatását engedélyezi. Koppintson a ![Zárolt](../images/locked.png) ikonra a képernyő feloldásához. 

A zárolás ikonra hosszan nyomva egy menü jelenik meg, amely jelenleg 4 lehetőséget kínál:

* **Normál** - az alapértelmezett szerkesztési mód, új elemek adhatóak hozzá, a létezők szerkeszhetőek, mozgathatóak és törölhetőek. Egy egyszerű fehér zár ikon lesz megjelenítve.
* **Csak címkézés** - egy létező objektum kiválasztása a Tulajdonságszerkesztőt jeleníti meg, a hosszú nyomás a főképernyőn objektumokat ad hozzá, de más geometriai műveletek nem működnek. Egy fehér zár ikon lesz megjelenítve, egy „T” betűvel.
* **Beltéri** - engedélyezi a beltéri módot, lásd [Beltéri mód](#indoor). Egy fehér zár ikon lesz megjelenítve, egy „I” betűvel.
* **C-mód** - engedélyezi a C-módot, csak a figyelmeztetés jelzővel megjelölt elemek lesznek megjelenítve, lásd [C-mód](#c-mode). Egy fehér zár ikon lesz megjelenítve, egy „C” betűvel.

#### Egyszeres koppintás, dupla koppintás, hosszú nyomás

Alapból a kiválasztható pontok és vonalak körül egy narancssárga terület van, amely azt jelzi, hol kell megérintenie az objektumot a kiválasztásához. Három lehetősége van:

* Egyszeres koppintás: Kiválasztja az objektumot. 
 * Egy izolált pont/út azonnal kiemelésre kerül. 
 * Viszont ha megpróbál kiválasztani egy objektumot, és a Vespucci úgy határoz, hogy több objektumra is gondolhatott, akkor egy választómenüt jelenít meg, így kiválaszthatja a megfelelő objektumot. 
 * A kiválasztott objektumok sárgával lesznek kiemelve. 
 * Tovább információkért lásd [Kiválasztott pont](Node%20selected.md), [Kiválasztott út](Way%20selected.md) and [Kiválasztott kapcsolat](Relation%20selected.md).
* Dupla koppintás: [Többszörös kiválasztási mód](Multiselect.md) indítása
* Hosszú nyomás: Létrehoz egy „célkeresztet”, amellyel új jegyzetetek hozhat létre, lásd lent és itt: [Új objektumok létrehozása](Creating%20new%20objects.md). Ez csak akkor engedélyezett, ha az „Egyszerű mód” ki van kapcsolva.

Jó stratégia ha belenagyít, ha nagy sűrűségű területet akar szerkeszteni.

A Vespucci jó „visszavonás/mégis” rendszerrel rendelkezik, így ne féljen kísérletezni az eszközén, viszont kérjük ne töltsön fel tesztadatokat.

### Kiválasztás / kiválasztás megszüntetése (egyetlen koppintás a „kiválasztás menüben”)

Érintsen meg egy objektumot és emelje ki. A képernyő egy üres területének megérintése megszünteti a kijelölést. Ha már kiválasztott egy objektumot, és valami mást kell kiválasztania, akkor érintse meg a kérdéses objektumot, nem kell előtte megszüntetnie a kijelölést. Az objektumon történő dupla koppintás elindítja a [Többszörös kiválasztás módot](Multiselect.md).

Ne feledje, hogy ha megpróbál kijelölni egy objektumot, és a Vespucci úgy dönt, hogy a kijelölés több objektumot jelent (például egy pont a vonalon, vagy egy másik átfedő objektumot), akkor egy kiválasztási menüt jelenít meg: Koppintson a kiválasztandó objektumra, és az kiválasztásra kerül. 

A kiválasztott objektumokat egy vékony sárga keret jelzi. A sárga keret a térképháttértől és a nagyítási szinttől függően nehezen észrevehető lehet. Ha kiválasztás történt, akkor értesítést kap a kiválasztás megerősítéséről.

Amint a kiválasztás megtörtént, a kiválasztott objektum támogatott műveletei fognak megjelenni (gombként vagy menüelemként): További információkért lásd: [Kiválasztott pont](Node%20selected.md), [Kiválasztott vonal](Way%20selected.md) és [Kiválasztott kapcsolat](Relation%20selected.md).

#### Kiválasztott objektumok: Címkék szerkesztése

A kiválasztott objektum másodszori megérintése megnyitja a címkeszerkesztőt, és így szerkesztheti az objektumhoz rendelt címkéket.

Ne feledje, hogy az átfedő objektumok esetén (mint a vonalon lévő pontok) a kiválasztási menü még egyszer megjelenik. Az ugyanazon objektum kiválasztása előhozza a címkeszerkesztőt; egy másik objektum kiválasztása egyszerűen kiválasztja a másik objektumot.

#### Kijelölt objektumok: pont vagy vonal mozgatása

Amint kiválasztott egy objektumot, az mozgatható lesz. Ne feledje, hogy csak a kijelölt objektumok mozgathatóak. Egyszerűen húzza (a tolerancia zónán belül) a kiválasztott objektumot a mozgatáshoz. Ha nagy húzási területet választ ki a beállításokban, akkor nagy területet kap a kiválasztott pont körül, így könnyebben pozicionálhatja az objektumot. 

#### Új pont vagy vonal hozzáadása 

Az alkalmazás első indításakor „Egyszerű módban” indul, ez módosítható a főmenüben, a megfelelő jelölőmező kikapcsolásával.

##### Egyszerű mód

A nagy zöld lebegő gomb a fő térképképernyőn egy menüt jelenít meg. Miután kiválasztotta az egyik elemet, arra lesz kérve, hogy koppintson a képernyő azon helyére, ahol létre akarja hozni az objektumot, a mozgás és a nagyítás továbbra is működik, ha igazítania kell a térképnézeten. 

További információkért lásd: [Új objektumok létrehozása az egyszerű műveletek módban](Creating%20new%20objects%20in%20simple%20actions%20mode.md).

##### Speciális (hosszú nyomás) mód
 
Nyomja hosszan ott, ahová a pontot vagy a vonal kezdetét szeretné tenni. Egy fekete „célkereszt” ikont fog látni. 
* Ha új pontot akar létrehozni (amely nem kapcsolódik objektumhoz), akkor koppintson félre a létező objektumtól.
* Ha bővíteni akar egy vonalat, akkor kattintson a vonal „tolerancia zónájába” (egy egy pontra a vonalon). A tolerancia zónát a pont vagy vonal körüli terület jelzi.

Ha látja a célkereszt szimbólumot, akkor ezek a lehetőségei:

* Érintse meg ugyanazon a helyen.
    * Ha a célkereszt nincs pont közelében, akkor az ugyanazon helyen történő érintés egy új pontot hoz létre. Ha egy vonal közelében van (de nincs a közelben pont), akkor az új pont a vonalon lesz (és hozzá lesz kapcsolva a vonalhoz).
    * Ha a célkereszt pont közelében van (tehát a pont toleranciaterületén), akkor az ugyanazon hely kiválasztása kiválasztja a pontot (és megnyílik a címkeszerkesztő). Nem lesz új pont létrehozva. A művelet ugyanaz mint a fenti kijelölésnél.
    * Érintsen meg egy másik helyet. Egy másik hely megérintése (a célkereszt toleranciazónáján kívül) egy új szakaszt ad hozzá az eredeti pozíciótól a jelenlegi pozícióig. Ha a célkereszt egy ponthoz vagy vonalhoz van közel, akkor az új szakasz ahhoz a ponthoz vagy vonalhoz lesz kapcsolva.

Egyszerűen érintse meg a képernyőt, ahová a további pontokat akarja hozzáadni a vonalon. A befejezéshez érintse meg kétszer az utolsó pontot. Ha az utolsó pont egy vonalon vagy ponton van, akkor a szakasz automatikusan a ponthoz vagy vonalhoz lesz kötve. 

Használhatja a menüelemet is: További információkért lásd: [Új objektumok létrehozása](Creating%20new%20objects.md).

#### Terület hozzáadása

Más geoadat rendszerekkel ellentétben, az OpenStreetMap jelenleg nem rendelkezik „terület” objektumtípussal. Az „iD” online szerkesztő megpróbál egy terület absztrakciót biztosítani az alacsonyabb szintű OSM elemekből, amely egyes esetekben jól működik, máskor nem. A Vespucci jelenleg meg sem próbál hasonlót, így valamennyit tudnia kell a területek ábrázolásáról:

* _zárt vonalak („poligonok”)_: a legegyszerűbb és leggyakoribb területtípus, olyan vonalat jelent, amelynek a közös első és utolsó pontja zárt „gyűrűt” alkot (például a legtöbb épület ilyen típusú). Ezek nagyon könnyen létrehozhatóak a Vespuccival, egyszerűen kapcsoljon vissza az első ponthoz a terület megrajzolásakor. Megjegyzés: A zárt vonalak értelmezése a címkézésen múlik: például ha a zárt vonal épületként van címkézve, akkor területként lesz kezelve, ha körforgalomként, akkor nem. Egyes esetekben mindkét értelmezés érvényes lehet, ilyenkor egy „area” címke pontosíthatja a szándékolt használatot.
* _multipoligonok_: egyes területek több részből is állhatnak, lyukakat és gyűrűket tartalmazhatnak, és így nem ábrázolhatóak egy vonallal. Az OSM egy konkrét kapcsolattípust használ ennek a feloldására, a multipoligont (a kapcsolat egy általános elem, amely elem közti kapcsolatokat tud leírni). Egy multipoligonnak több „külső (outer)” gyűrűje, és több „belső (inner)” gyűrűje is lehet. Ezek a gyűrűk lehetnek zártak, mint ahogy fentebb szerepel, vagy lehetnek különálló vonalak közös végpontokkal. Ugyan a nagy multipoligonok kezelése minden eszközzel bonyolult, a kisebbek létrehozása nem túl nehéz a Vespuccival.
* _partvonalak_: nagyon nagy objektumok esetén, mint a kontinensek vagy a szigetek, még a multipoligon modell sem működik kielégítően. A natural=coastline vonalak esetén irányfüggő szemantikát alkalmazunk: a szárazföld a vonal bal oldalán van, a víz a jobb oldalán. Ennek mellékhatása, hogy általánosságban nem szabad megfordítani a partvonal címkézésű vonalakat. További információk az [OSM wikiben](http://wiki.openstreetmap.org/wiki/Tag:natural%3Dcoastline).

#### Vonalgeometria javítása

Ha eléggé ránagyít a kiválasztott vonalra, akkor egy kis „x”-et fog látni az elég hosszú vonalszakaszok közepén. Az „x” húzása új pontot hoz létre azon a helyen. Megjegyzés: próbálja elkerülni a pontok véletlen létrehozását, az érintési toleranciaterület elég kicsi ennél a műveletnél.

#### Kivágás, másolás és beillesztés

Másolhatja és kivághatja a kiválasztott pontokat és vonalakat, aztán egyszer vagy többször beillesztheti egy új helyen. A kivágás megtartja az OSM azonosítót és verziót. A beillesztéshez nyomja hosszan a területet, ahová be akarja illeszteni (egy célkereszt fogja jelölni a helyet). Aztán válassza ki a „Beillesztést” a menüből.

#### Címek hatékony hozzáadása

A Vespucci rendelkezik egy „cím címkék hozzáadása” funkcióval, amely megpróbálja hatékonyabbá tenni a felmérést. Így választható ki:

* hosszú nyomás után: a Vespucci egy pontot ad hozzá a helyen, és becslést ad a házszámra és hozzáadja a legutóbb használt cím címkéket. Ha a pont egy épület körvonalán van, akkor automatikus hozzáadja az „entrance=yes” címkét a ponthoz. A címkeszerkesztő megnyitja a kérdéses objektumot, és további változtatásokat tehet.
* a pont/vonal kiválasztási módokban: a Vespucci hozzáadja a cím címkéket ahogy fent, és elindítja a címkeszerkesztőt.
* a címkeszerkesztőben.

A házszámok becslésének működéséhez jellemzően legalább két házszám szükséges az út két oldalán, minél több szám szerepel az adatokban, annál jobb.

Fontolja meg, hogy ezt az [Automatikus letöltés](#download) móddal használja.  

#### Kanyarodási korlátozások hozzáadása

A Vespucci rendelkezik egy gyors módszerrel a kanyarodási korlátozások hozzáadásához, ha szükséges, akkor automatikusan felosztja a vonalakat, és megkéri. hogy válassza ki újra az elemeket. 

* válasszon ki egy „highway” címkéjű vonalat (kanyarodási korlátozások csak ezekhez adhatóak, ha más vonalaknál akarja használni, akkor használja az általános „kapcsolat létrehozása” módot)
* válassza ki a „Korlátozás hozzáadása” lehetőséget a menüből
* válassza ki a „via” pontot vagy vonalat (csak a lehetséges „via” elemeknél lesz megjelenítve az érintési terület)
* válassza ki a „to” vonalat (lehetséges visszalépni, és megadni a „to” elemet a „from” elemből, ekkor a Vespucci azt fogja feltételezni, hogy a „no_u_turn” korlátozást akarja hozzáadni)
* állítsa be a korlátozás típusát

### Vespucci „zárolt” módban

Ha a piros zár látszik, akkor az összes nem szerkesztési művelet elérhető. Továbbá egy objektumon vagy a közelében történő hosszú nyomás megjeleníti a részletes információs képernyőt, ha az egy OSM objektum.

### Módosítások mentése

*(hálózati kapcsolatot igényel)*

Válassza ugyanazt a gombot vagy menüelemet, melyet a letöltésnél használt, és most válassza az „Adatok feltöltése az OSM kiszolgálóra” lehetőséget.

A Vespucci támogatja az OAuth engedélyezést és a klasszikus felhasználónév és jelszó módszert. Az OAuth a javasolt, mivel így nem kell jelszót küldeni.

Az új Vespucci telepítésekben az OAuth automatikusan engedélyezett. Az első feltöltési kísérletkor az OSM weboldal egy lapja töltődik be. Ha bejelentkezett (titkosított kapcsolaton keresztül), akkor megkérésre kerül, hogy engedélyezze a Vespuccinak, hogy szerkessze a fiókját. Ha a szerkesztés előtt akarja engedélyezni az OAuth hozzáférést, akkor ezt megteheti az „Eszközök” menüben.

Ha menteni akarja a munkáját, és nincs internetkapcsolata, akkor elmentheti egy JOSM kompatibilis .osm fájlba, és felöltheti később a Vespuccival vagy a JOSM-mel. 

#### Ütközések feloldása feltöltéskor

A Vespucci rendelkezik egy egyszerű ütközésfeloldóval. Viszont ha azt gondolja, hogy komoly problémák vannak a szerkesztéseivel, akkor exportálja a módosításokat egy .osc fájlba („Exportálás” menüelem az „Átküldés” menüben), majd javítsa ki és töltse fel a JOSM-mel. Lásd a részletes súgót az [ütközésfeloldásról](Conflict%20resolution.md).  

## GPS használata

Használhatja a Vespuccit GPX nyomvonalak létrehozására, és azok megjelenítésére az eszközén. Továbbá megjelenítheti a jelenlegi GPS pozíciót (lásd a „Pozíció megjelenítése” lehetőséget a GPS menüben). 

Ha az utóbbi van beállítva, akkor a képernyő kézi mozgatása vagy a szerkesztés letiltja „GPS követése” módot, és a kék GPS nyíl körvonal helyett kitöltött nyílra vált. A „követés” módhoz történő gyors visszatéréshez egyszerűen nyomja meg a GPS gombot, vagy kapcsolja be újra a menü lehetőséget.

## Jegyzetek és hibák

A Vespucci támogatja az OSM jegyzetek (régebben OSM hibák) letöltését, lezárását és a megjegyzések hozzáfűzését, valamint támogatja az [OSMOSE hibaellenőrző eszköz](http://osmose.openstreetmap.fr/en/map/) „hibáit” is. Mind a kettőt vagy le kell tölteni direktben, vagy használhatja az automatikus letöltési lehetőséget, hogy elérje a közeli területen lévő elemeket. Ha már egyszer szerkesztette vagy lezárta őket, akkor egyesével vagy egyben is feltöltheti a hibákat vagy jegyzeteket.

A térképen a jegyzeteket vagy hibákat kis hiba ikonok ![Hiba](../images/bug_open.png) jelzik, a zöldek lezártak/megoldottak, a kékeket Ön hozta létre vagy szerkesztette, a sárgák pedig még mindig aktívak, és Ön nem változtatott rajtuk. 

Az OSMOSE hibák kék hivatkozást jelenítenek meg az érintett objektumhoz, a hivatkozás megnyomása kiválasztja az objektumot, a képernyő közepére teszi azt, és letölti a területet, ha az szükséges. 

### Szűrés

A jegyzetek és hibák megjelenítésének globális engedélyezése mellett beállíthat egy durva szűrést, hogy csökkentse a zsúfoltságot. A [Speciális beállításokban](Advanced%20preferences.md) egyenként kiválaszthatja:

* Jegyzetek
* Osmose szerinti hiba
* Osmose szerinti figyelmeztetés
* Osmose szerinti kisebb probléma
* Egyéni

<a id="indoor"></a>

## Beltéri mód

A beltéri térképezés kihívásokkal teli a sok egymást fedő objektum miatt. A Vespucci rendelkezik egy dedikált beltéri móddal, amellyel kiszűrheti az objektumokat, melyek nem ugyanazon a szinten vannak, és automatikusan hozzáadhatja a jelenlegi szintet az újonnan létrehozott objektumokhoz.

A mód a zárolás gomb hosszú megnyomásával, és a megfelelő menüelem kiválasztásával engedélyezhető, lásd: [Zárolás, feloldás, módváltás](#lock).

<a id="c-mode"></a>

## C-mód

C-módban csak azok az objektumok jelennek meg, melyeken figyelmeztetés jelző van, így könnyen kiszúrhatja azokat az objektumokat, melyekkel konkrét probléma van, vagy egyeznek a beállítható ellenőrzésekkel. Ha egy objektum kiválasztásra kerül és elindul a tulajdonságszerkesztő C-módban, akkor a legjobban illeszkedő előbeállítás automatikusan beállításra kerül.

A mód a zárolás gomb hosszú megnyomásával, és a megfelelő menüelem kiválasztásával engedélyezhető, lásd: [Zárolás, feloldás, módváltás](#lock).

### Ellenőrzések beállítása

Jelenleg két beállítható ellenőrzés van (van egy ellenőrzés a FIXME címkékhez, és egy teszt a hiányzó kapcsolattípus címkékhez, melyek jelenleg nem konfigurálhatóak), és mind a kettő beállítható az „Ellenőrző beállításai” kiválasztásával a „Beállításokban”. 

A bejegyzések listája két részre van osztva, a felső része az „újbóli felmérési” bejegyzéseket tartalmazza, az alsó rész pedig az „ellenőrzési bejegyzéseket”. A bejegyzések koppintással szerkeszthetőek, és a zöld menügombbal adhatóak hozzá új bejegyzések.

#### Újbóli felmérési bejegyzések

Az újbóli felmérési bejegyzések a következő tulajdonságokkal rendelkeznek:

* **Kulcs** – A kérdéses címke kulcs.
* **Érték** – Az érték, amellyel a kérdéses címkének rendelkeznie kell, ha üres, akkor figyelmen kívül lesz hagyva.
* **Kor** – az elem hány napi változatlansága esetén kell újra felmérni, ha a check_date mező meg van adva, akkor az lesz használva, egyébként a jelenlegi verzió létrehozási dátuma. Az érték nullára állítása esetén egy egyszerű kulcs és érték összehasonlítást eredményez.
* **Reguláris kifejezés** – ha be van kapcsolva, akkor az **Érték** JAVA reguláris kifejezésnek lesz tekintve.

A **Kulcs** és az **Érték** a _meglévő_ címkékkel lesz összehasonlítva a kérdéses objektumon.

#### Bejegyzések ellenőrzése

Az elemek ellenőrzése a következő két tulajdonsággal rendelkezik:

* **Kulcs** – Kulcs, amelynek jelen kell lennie az objektumon az előbeállítás szerint.
* **Nem kötelező elemek megkövetelése** – A kulcs megkövetelése akkor is, ha a kulcs az előbeállítás nem kötelező címkéi között található.

Ez az ellenőrzés úgy működik, hogy először meghatározza az illeszkedő előbeállítást, majd ellenőrzi, hogy a **Kulcs** egy „ajánlott” kulcs-e ennél az objektumnál, az előbeállítás szerint, a **Nem kötelező elemek megkövetelése** kiterjeszti az ellenőrzést azokra a címkékre is, melyek „nem kötelezőek” az objektumon. Megjegyzés: jelenleg a hivatkozott előbeállítások nem kerülnek ellenőrzésre.

## Szűrők

### Címkealapú szűrő

A szűrő a főmenüből engedélyezhető, és a szűrő ikonra koppintva módosítható. További dokumentáció található itt: [Címkeszűrő](Tag%20filter.md).

### Előbeállítás-alapú szűrő

An alternative to the above, objects are filtered either on individual presets or on preset groups. Tapping on the filter icon will display a preset selection dialog similar to that used elsewhere in Vespucci. Individual presets can be selected by a normal click, preset groups by a long click (normal click enters the group). More documentation can be found here [Preset filter](Preset%20filter.md).

## A Vespucci testreszabása

### Beállítások, melyeket módosíthat

* Background layer
* Overlay layer. Adding an overlay may cause issues with older devices and such with limited memory. Default: none.
* Notes/Bugs display. Open Notes and bugs will be displayed as a yellow bug icon, closed ones the same in green. Default: on.
* Photo layer. Displays geo-referenced photographs as red camera icons, if direction information is available the icon will be rotated. Default: off.
* Keep screen on. Default: off.
* Large node drag area. Moving nodes on a device with touch input is problematic since your fingers will obscure the current position on the display. Turning this on will provide a large area which can be used for off-center dragging (selection and other operations still use the normal touch tolerance area). Default: off.

#### Speciális beállítások

* Node icons. Default: on.
* Always show context menu. When turned on every selection process will show the context menu, turned off the menu is displayed only when no unambiguous selection can be determined. Default: off (used to be on).
* Enable light theme. On modern devices this is turned on by default. While you can enable it for older Android versions the style is likely to be inconsistent.
* Show statistics. Will show some statistics for debugging, not really useful. Default: off (used to be on).  

## Problémák jelentése

If Vespucci crashes, or it detects an inconsistent state, you will be asked to send in the crash dump. Please do so if that happens, but please only once per specific situation. If you want to give further input or open an issue for a feature request or similar, please do so here: [Vespucci issue tracker](https://github.com/MarcusWolschon/osmeditor4android/issues). The "Provide feedback" function from the main menu will open a new issue and include the relevant app and device information without extra typing.

If you want to discuss something related to Vespucci, you can either start a discussion on the [Vespucci Google group](https://groups.google.com/forum/#!forum/osmeditor4android) or on the [OpenStreetMap Android forum](http://forum.openstreetmap.org/viewforum.php?id=56)


