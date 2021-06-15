# Vespucci bevezetés

A Vespucci egy teljes funkcionalitású OpenStreetMap-szerkesztő, amely az asztali szerkesztők által biztosított legtöbb műveletet támogatja. Sikeresen tesztelték a Google Android 2.3–10.0 és különböző AOSP-alapú változataira. Vigyázat: bár a mobileszközök képességei felzárkóztak asztali vetélytársaikéihoz, különösen a régebbi eszközök memóriája korlátozott, és általában meglehetősen lassúak. Ezt vegye figyelembe a Vespucci használatakor, és például ésszerű méretű területeket próbáljon szerkeszteni. 

## Első használat

Indításkor a Vespucci – miután megkérte a szükséges engedélyeket, és megjelenített egy üdvözlő üzenetet – az „Egyéb hely letöltése” / „Terület betöltése” párbeszédpanelt mutatja. Ha a koordináták megjelennek, és azonnal szeretné letölteni, kiválaszthatja a megfelelő opciót, és beállíthatja a letölthető hely körüli sugarat. Lassú eszközön ne jelöljön ki nagy területet. 

Másrészt be is zárhatja a párbeszédablakot az „Ugrás a térképre” gomb megnyomásával, majd a térképen ránagyíthat a szerkesztendő területre, és letöltheti az adatokat (lásd lejjebb: „Szerkesztés a Vespuccival”).

## Szerkesztés a Vespuccival

A képernyő méretétől és a készülék életkorától függően a szerkesztési műveletek a következő helyekről érhetők el: közvetlenül a felső sáv ikonjaiból, a felső sáv jobb oldalán található legördülő menüből, az alsó sávból (ha van) vagy a menügombból.

<a id="download"></a>

### OSM-adatok letöltése

Jelölje ki vagy az átvitel ikont ![Transfer](../images/menu_transfer.png) vagy az „Átvitel” menüpontot. Ez hét lehetőséget fog megjeleníteni:

* **Aktuális nézet letöltése** - a képernyőn látható terület letöltése és egyesítése a meglévő adatokkal *(hálózati kapcsolatot vagy offline adatforrást igényel)*
* **Aktuális nézet törlése és letöltése** - az összes adat törlése a memóriából, majd a képernyőn látható terület letöltése *(hálózati kapcsolatot igényel)*
* **Adatok feltöltése az OSM szerverre** - szerkesztések feltöltése az OpenStreetMap-kiszolgálóra *(hitelesítést és hálózati kapcsolatot igényel)*
* **Adatok frissítése** - az összes terület adatainak ismételt letöltése, és a memóriában tárolt adatok frissítése *(hálózati kapcsolatot igényel)*
* **Helyalapú automatikus letöltés** - az aktuális földrajzi hely körüli terület automatikus letöltése *(hálózati kapcsolatot vagy offline adatokat igényel)* *(GPS-t igényel)*
* **Automatikus letöltés és nagyítás** - az aktuálisan megjelenített térképterület adatainak automatikus letöltése *(hálózati kapcsolatot vagy offline adatokat igényel)* *(GPS-t igényel)*
* **Fájl…** - OSM-adatok mentése és betöltése az eszközön tárolt fájlokba /-ból.
* **Jegyzet / hibák…** - OSM-jegyzetek és „hibák” (automatikus és kézi) letöltése minőségbiztosítási eszközökből (jelenleg Osmose) *(hálózati kapcsolatot igényel)*

Az adatok eszközre töltésének legkönnyebb módja a szerkesztendő területre görgetés és nagyítás, aztán a „Jelenlegi nézet letöltése”. Gesztusokkal, a nagyítási gombokkal és a hangerőszabályzó gombokkal nagyíthat. A Vespucci aztán letölti a jelenlegi nézet adatait. Az adatok eszközre letöltéséhez nem szükséges hitelesítés.

Az alapértelmezett beállításokkal a nem letöltött területek a letöltöttekhez képest halványabbak lesznek, ezzel elkerülhető a duplikált objektumok véletlen hozzáadása a nem megjelenített területeken. Ez a viselkedés megváltoztatható a [Speciális beállítások](Advanced%20preferences.md) részben.

### Szerkesztés

<a id="lock"></a>

#### Zárolás, feloldás, módváltás

A véletlen szerkesztések elkerülése miatt a Vespucci „zárolt” módban indul, olyan módban, amely csak a nagyítást és a térkép mozgatását engedélyezi. Koppintson a ![Zárolt](../images/locked.png) ikonra a képernyő feloldásához. 

A zárolás ikonra hosszan nyomva egy menü jelenik meg, amely jelenleg 4 lehetőséget kínál:

* **Normál** - az alapértelmezett szerkesztési mód, új elemek adhatóak hozzá, a létezők szerkeszhetőek, mozgathatóak és törölhetőek. Egy egyszerű fehér lakatikon lesz megjelenítve.
* **Cím** - lehetővé tezi a cím módot, amely az [egyszerű mód](../en/Simple%20actions.md) „+” gombjával elérhető kicsit egyszerűsített mód néhány specifikus funkcióval. Egy „A” betűvel ellátott fehér lakatikon jelzi.
* **Csak címkézés** - egy létező objektum kiválasztása a Tulajdonságszerkesztőt jeleníti meg, a hosszú nyomás a főképernyőn objektumokat ad hozzá, de más geometriai műveletek nem működnek. Egy fehér lakatikon fog megjelenni, egy „T” betűvel.
* **Beltéri** - engedélyezi a beltéri módot, lásd [Beltéri mód](#indoor). Egy fehér lakatikon lesz megjelenítve, egy „I” betűvel.
* **C-mód** - engedélyezi a C-módot, csak a figyelmeztetés jelzővel megjelölt elemek lesznek megjelenítve, lásd [C-mód](#c-mode). Egy fehér lakatikon lesz megjelenítve, egy „C” betűvel.

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

##### Speciális mód (hosszú lenyomás)

Hosszan nyomja meg ott, ahol a pontot vagy a vonal kezdőpontját szeretné elhelyezni. Egy fekete „célkereszt” szimbólumot fog látni.
* Ha új (objektumhoz nem kapcsolódó)pontot szeretne létrehozni, akkor a már meglévő objektumoktól távolabb koppintson a képernyőre.
* Ha egy vonalat szeretne kiegészíteni, akkor a vonal „toleranciazónájára” (vagy a vonal egy pontjára) koppintson. A toleranciazónát a pont vagy vonal körüli terület jelzi.

Ha látja a célkereszt szimbólumot, akkor ezek a lehetőségei:

* _Normális nyomás ugyanott._
* Ha a célkereszt nincs egy pont közelében, akkor ugyanott újból megérintve új pont jön létre. Ha egy vonal közelében van (de nem egy pont közelében), akkor az új pont a vonalon lesz (és csatlakozni fog hozzá).
* Ha a célkereszt egy pont közelében van (azaz a pont toleranciazónáján belül), ugyanott újból megérintve kijelöljük a pontot (és megnyílik a címkeszerkesztő). Nem jön létre új pont. A művelet megegyezik a fenti kijelöléssel.
* _ Normál érintés egy másik helyen._ Egy másik hely megérintése (a célkereszt toleranciazónáján kívül) hozzáad egy vonalszakaszt az eredeti pozíciótól az aktuális pozícióig. Ha a célkereszt egy vonal vagy pont közelében volt, akkor az új szakasz kapcsolódik ahhoz a ponthoz vagy vonalhoz.

Egyszerűen érintse meg a képernyőt ott, ahol a vonalhoz további pontokat szeretne hozzáadni. A befejezéshez érintse meg kétszer az utolsó pontot. Ha az utolsó pont egy vonalon vagy ponton található, akkor a szakasz automatikusan kapcsolódik ehhez a vonalhoz vagy ponthoz. 

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

Ha a piros lakat látszik, akkor az összes nem szerkesztési művelet elérhető. Továbbá egy objektumon vagy a közelében történő hosszú nyomás megjeleníti a részletes információs képernyőt, ha az egy OSM objektum.

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

* **Kulcs** – Az érintett címke kulcsa.
* **Érték** – Az érintett címke értéke, ha üres, a címke értéke figyelmen kívül marad.
* **Kor** – legutóbbi módosítása után hány nappal kell megvizsgálni az elemet, ha van _check_date_ címke. Különben az aktuális verzió létrehozásának dátuma. Az érték nullára állítása azt eredményezi, hogy az ellenőrzés egyszerűen megfelel a kulcsnak és az értéknek.
* **Reguláris kifejezés** – ha be van jelölve, az **érték** JAVA reguláris kifejezésnek lesz tekintve.

A **Kulcs** és az **Érték** a _meglévő_ címkékkel lesz összehasonlítva a kérdéses objektumon.

A standard előbeállításokban a _Magyarázó jegyzetek_ csoport tartalmaz egy elemet, amely automatikusan hozzáad egy _check_date_ címkét az aktuális dátummal.

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

Az alkalmazás számos vonatkozása testreszabható. Ha valami konkrét dolgot keres, de nem találja meg, akkor a [Vespucci webhely](https://vespucci.io/) kereshető, és további tájékoztatást ad mindarról, ami az eszközön elérhető.

### Rétegbeállítások

A rétegbeállítások a rétegvezérlőn keresztül módosíthatók (a jobb felső sarokban található „hamburgermenüben”), az összes többi beállítás a főmenübeállítások gombbal érhető el. A rétegeket lehet engedélyezni, letiltani és ideiglenesen elrejteni.

Elérhető rétegtípusok

* Adatréteg - ez az a réteg, amelyre az OpenStreetMap-adatok betöltődnek. Normál használat esetén itt semmit sem kell megváltoztatnia. Alapértelmezés: bekapcsolva
* Háttérréteg - légi és műholdas háttérképek széles választéka áll rendelkezésre. Ennek alapértelmezett értéke az openstreetmap.org webhely „standard stílusú” térképe
* Fedőréteg - ezek félig átlátszó rétegek további információkkal, például GPX-nyomvonalakkal. Régebbi, korlátozott memóriával rendelkező készülékeknél a fedőréteg hozzáadása problémákat okozhat. Alapértelmezés: nincs.
* Jegyzetek/hibák megjelenítése - A nyitott jegyzetek és hibák sárga hibaikonként jelennek meg, a lezártak zölddel. Alapértelmezés: bekapcsolva
* Fényképréteg - A georeferált fényképek piros kameraikonként jelennek meg; ha rendelkezésre állnak irányinformációk, akkor az ikon elfordul. Alapértelmezés: kikapcsolva
* Mapillary-réteg - Mapillary szakaszokat jelenít meg jelölőkkel, ahol képek vannak, a jelölőre kattintva megjelenik a kép. Alapértelmezés: kikapcsolva
* GeoJSON-réteg - Megjeleníti egy GeoJSON fájl tartalmát. Alapértelmezés: kikapcsolva
* Rács - megjeleníti a méretarányt a térkép vagy a rács oldalán. Alapértelmezés: bekapcsolva. 

#### Beállítások

* Hagyja bekapcsolva a kijelzőt. Alapértelmezés: kikapcsolva.
* Nagy ponthúzási terület. A pontok mozgatása érintéses bemenettel rendelkező eszközön nehézkes, mivel az ujjai eltakarják a kijelző aktuális helyzetét. Ennek bekapcsolása nagy területet biztosít, amely a középponton kívüli húzásra használható fel (a kijelölés és más műveletek továbbra is a normál érintéstűrési területet használják). Alapértelmezés: kikapcsolva.

A teljes leírás megtalálható a [beállításoknál](Preferences.md).

#### Speciális beállítások

* Pont ikonok. Alapértelmezés: bekapcsolva.
* Mindig mutassa a helyi menüt. Bekapcsolva minden kijelölési folyamat megmutatja a helyi menüt, kikapcsolva a menü csak akkor jelenik meg, ha nem lehet egyértelmű kijelölést meghatározni. Alapértelmezés: kikapcsolva (korábban be volt kapcsolva).
* Könnyű téma engedélyezése. Modern eszközökön ez alapértelmezés szerint be van kapcsolva. Régebbi Android verziók esetében is engedélyezhető, a stílus azonban valószínűleg következetlen lesz. 

A teljes leírás megtalálhat a [speciális beállításoknál](Advanced%20preferences.md).

## Problémák jelentése

Ha a Vespucci összeomlik vagy nem konzisztens állapotot észlel, akkor megkérjük, hogy küldje el az összeomlás-jelentésben. Tegyen így, ha ez történik, de helyzetenként csak egyszer. Ha további hozzáfűznivalója van, funkciókérés vagy hasonló okból akar jegyet nyitni, akkor itt tegye meg: [Vespucci hibakövető](https://github.com/MarcusWolschon/osmeditor4android/issues). A főmenüben lévő „Visszajelzés küldése” funkció egy új hibajegyet nyit, és további gépelés nélkül beleteszi a releváns alkalmazás- és eszközinformációkat.

Ha valamilyen Vespuccival kapcsolatos dolgot szeretne megbeszélni, akkor kezdjen egy beszélgetést a [Vespucci Google csoportban](https://groups.google.com/forum/#!forum/osmeditor4android) vagy az [OpenStreetMap Android fórumban](http://forum.openstreetmap.org/viewforum.php?id=56)


