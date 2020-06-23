# Vespucci bevezetés

Vespucci is a full featured OpenStreetMap editor that supports most operations that desktop editors provide. It has been tested successfully on Google's Android 2.3 to 10.0 and various AOSP based variants. A word of caution: while mobile device capabilities have caught up with their desktop rivals, particularly older devices have very limited memory available and tend to be rather slow. You should take this in to account when using Vespucci and keep, for example, the areas you are editing to a reasonable size. 

## Első használat

On startup Vespucci shows you the "Download other location"/"Load Area" dialog after asking for the required permissions and displaying a welcome message. If you have coordinates displayed and want to download immediately, you can select the appropriate option and set the radius around the location that you want to download. Do not select a large area on slow devices. 

Másrészt be is zárhatja a párbeszédet az „Ugrás a térképre” gomb megnyomásával, majd a térképen a szerkesztendő területre nagyíthat, és letöltheti az adatokat (lásd lejjebb: „Szerkesztés a Vespuccival”).

## Szerkesztés a Vespuccival

A képernyő méretétől és a készülék életkorától függően a szerkesztési műveletek a következő helyekről érhetők el: közvetlenül a felső sáv ikonjaiból, a felső sáv jobb oldalán található legördülő menüből, az alsó sávból (ha van) vagy a menügombból.

<a id="download"></a>

### OSM adatok letöltése

Jelölje ki vagy az átvitel ikont ![Transfer](../images/menu_transfer.png) vagy az „Átvitel” menüpontot. Ez hét lehetőséget fog megjeleníteni:

* **Download current view** - download the area visible on the screen and merge it with existing data *(requires network connectivity)*
* **Clear and download current view** - clear any data in memory and then download the area visible on the screen *(requires network connectivity)*
* **Upload data to OSM server** - upload edits to OpenStreetMap *(requires authentication)* *(requires network connectivity)*
* **Location based auto download** - download an area around the current geographic location automatically *(requires network connectivity or offline data)* *(requires GPS)*
* **Pan and zoom auto download** - download data for the currently displayed map area automatically *(requires network connectivity or offline data)* *(requires GPS)*
* **File...** - saving and loading OSM data to/from on device files.
* **Note/Bugs...** - download (automatically and manually) OSM Notes and "Bugs" from QA tools (currently OSMOSE) *(requires network connectivity)*

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

A Vespucci rendelkezik egy ![Address](../images/address.png) „címcímkék hozzáadása” funkcióval, amely az adott házszám kitalálásával megpróbálja hatékonyabbá tenni a címek felmérést. Így jelölhető ki:

* hosszú nyomás után (_egyszerű módban nem): a Vespucci elhelyez egy pontot az adott helyen, megpróbálja a lehető legjobban kitalálni a házszámot, és hozzáadja az utóbbi időben használ címcímkéket. Ha a pont egy épület kontúrján van, akkor automatikusan kap egy „entrance=yes” címkét is. Megnyílik a címkeszerkesztő és így lehetővé válik a további módosítások elvégzése.
* a kijelölt pont/vonal módban: a Vespucci a fent leírt módon hozzáadja a címcímkéket és elindítja a címkeszerkesztőt
* a tulajdonságszerkesztőben.

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

Jelenleg két konfigurálható ellenőrzés van (nem konfigurálható a FIXME címkék ellenőrzése és a kapcsolatokról hiányzó type címkék ellenőrzése), mindkettő a Beállítások > Érvényesítő beállításai menöben állítható be. 

A bejegyzések listája két részre van osztva, a felső része az „újbóli felmérési” bejegyzéseket tartalmazza, az alsó rész pedig az „ellenőrzési bejegyzéseket”. A bejegyzések koppintással szerkeszthetőek, és a zöld menügombbal adhatóak hozzá új bejegyzések.

#### Újbóli felmérési bejegyzések

Az újbóli felmérési bejegyzések a következő tulajdonságokkal rendelkeznek:

* **Key** - Key of the tag of interest.
* **Value** - Value the tag of interest should have, if empty the tag value will be ignored.
* **Age** - how many days after the element was last changed the element should be re-surveyed, if a _check_date_ tag is present that will be the used, otherwise the date the current version was create. Setting the value to zero will lead to the check simply matching against key and value.
* **Regular expression** - if checked **Value** is assumed to be a JAVA regular expression.

A **Kulcs** és az **Érték** a _meglévő_ címkékkel lesz összehasonlítva a kérdéses objektumon.

The _Annotations_ group in the standard presets contain an item that will automatically add a _check_date_ tag with the current date.

#### Bejegyzések ellenőrzése

Az elemek ellenőrzése a következő két tulajdonsággal rendelkezik:

* **Kulcs** – Kulcs, amelynek jelen kell lennie az objektumon az előbeállítás szerint.
* **Nem kötelező elemek megkövetelése** – A kulcs megkövetelése akkor is, ha a kulcs az előbeállítás nem kötelező címkéi között található.

Ez az ellenőrzés úgy működik, hogy először meghatározza az illeszkedő előbeállítást, majd ellenőrzi, hogy a **Kulcs** egy „ajánlott” kulcs-e ennél az objektumnál, az előbeállítás szerint, a **Nem kötelező elemek megkövetelése** kiterjeszti az ellenőrzést azokra a címkékre is, melyek „nem kötelezőek” az objektumon. Megjegyzés: jelenleg a hivatkozott előbeállítások nem kerülnek ellenőrzésre.

## Szűrők

### Címkealapú szűrő

A szűrő a főmenüből engedélyezhető, és a szűrő ikonra koppintva módosítható. További dokumentáció található itt: [Címkeszűrő](Tag%20filter.md).

### Előbeállítás-alapú szűrő

A fentiek alternatívájaként, az objektumok egyes előbeállítások vagy előbeállítás-csoportok alapján kerülnek szűrésre. A szűrő ikonra koppintva megjelenik egy előbeállítás-választó párbeszédablak, amely hasonlít a Vespucciban máshol előfordulókra. Az egyes előbeállítások normál koppintással válaszhatóak ki, az előbeállítás-csoportok pedig hosszú lenyomással (a normál koppintás belép a csoportba). További dokumentáció található itt: [Előbeállítás-szűrő](Preset%20filter.md).

## A Vespucci testreszabása

Many aspects of the app can be customized, if you are looking for something specific and can't find it, [the Vespucci website](https://vespucci.io/) is searchable and contains additional information over what is available on device.

### Layer settings

Layer settings can be changed via the layer control (upper right corner), all other setting are reachable via the main menu preferences button.

* Background layer - there is a wide range of aerial and satellite background imagery available, , the default value for this is the "standard style" map from openstreetmap.org.
* Overlay layer - these are semi-transparent layers with additional information, for example GPX tracks. Adding an overlay may cause issues with older devices and such with limited memory. Default: none.
* Notes/Bugs display. Open Notes and bugs will be displayed as a yellow bug icon, closed ones the same in green. Default: on.
* Photo layer. Displays geo-referenced photographs as red camera icons, if direction information is available the icon will be rotated. Default: off.

#### Preferences

* Keep screen on. Default: off.
* Large node drag area. Moving nodes on a device with touch input is problematic since your fingers will obscure the current position on the display. Turning this on will provide a large area which can be used for off-center dragging (selection and other operations still use the normal touch tolerance area). Default: off.

The full description can be found here [Preferences](Preferences.md)

#### Speciális beállítások

* Node icons. Default: on.
* Always show context menu. When turned on every selection process will show the context menu, turned off the menu is displayed only when no unambiguous selection can be determined. Default: off (used to be on).
* Enable light theme. On modern devices this is turned on by default. While you can enable it for older Android versions the style is likely to be inconsistent. 

The full description can be found here [Advanced preferences](Advanced%20preferences.md)

## Problémák jelentése

Ha a Vespucci összeomlik vagy nem konzisztens állapotot észlel, akkor megkérjük, hogy küldje el az összeomlás-jelentésben. Tegyen így, ha ez történik, de helyzetenként csak egyszer. Ha további hozzáfűznivalója van, funkciókérés vagy hasonló okból akar jegyet nyitni, akkor itt tegye meg: [Vespucci hibakövető](https://github.com/MarcusWolschon/osmeditor4android/issues). A főmenüben lévő „Visszajelzés küldése” funkció egy új hibajegyet nyit, és további gépelés nélkül beleteszi a releváns alkalmazás- és eszközinformációkat.

Ha valamilyen Vespuccival kapcsolatos dolgot szeretne megbeszélni, akkor kezdjen egy beszélgetést a [Vespucci Google csoportban](https://groups.google.com/forum/#!forum/osmeditor4android) vagy az [OpenStreetMap Android fórumban](http://forum.openstreetmap.org/viewforum.php?id=56)


