_Before we start: most screens have links in the menu to the on-device help system giving you direct access to information relevant for the current context, you can easily navigate back to this text too. If you have a larger device, for example a tablet, you can open the help system in a separate split window.  All the help texts and more (FAQs, tutorials) can be found on the [Vespucci documentation site](https://vespucci.io/) too. You can further start the help viewer directly on devices that support short cuts with a long press on the app icon and selecting "Help"_

# Vespucci bevezetés

Vespucci is a full featured OpenStreetMap editor that supports most operations that desktop editors provide. It has been tested successfully on Google's Android 2.3 to 14.0 (versions prior to 4.1 are no longer supported) and various AOSP based variants. A word of caution: while mobile device capabilities have caught up with their desktop rivals, particularly older devices have very limited memory available and tend to be rather slow. You should take this in to account when using Vespucci and keep, for example, the areas you are editing to a reasonable size.

## Szerkesztés a Vespuccival

A képernyő méretétől és a készülék életkorától függően a szerkesztési műveletek a következő helyekről érhetők el: közvetlenül a felső sáv ikonjaiból, a felső sáv jobb oldalán található legördülő menüből, az alsó sávból (ha van) vagy a menügombból.

<a id="download"></a>

### OSM-adatok letöltése

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

Az adatok eszközre töltésének legkönnyebb módja a szerkesztendő területre görgetés és nagyítás, aztán a „Jelenlegi nézet letöltése”. Gesztusokkal, a nagyítási gombokkal és a hangerőszabályzó gombokkal nagyíthat. A Vespucci aztán letölti a jelenlegi nézet adatait. Az adatok eszközre letöltéséhez nem szükséges hitelesítés.

In unlocked state any non-downloaded areas will be dimmed relative to the downloaded ones if you are zoomed in far enough to enable editing. This is to avoid inadvertently adding duplicate objects in areas that are not being displayed. In the locked state dimming is disabled, this behaviour can be changed in the [Advanced preferences](Advanced%20preferences.md) so that dimming is always active.

If you need to use a non-standard OSM API entry, or use [offline data](https://vespucci.io/tutorials/offline/) in _MapSplit_ format you can add or change entries via the _Configure..._ entry for the data layer in the layer control.

### Szerkesztés

<a id="lock"></a>

#### Zárolás, feloldás, módváltás

A véletlen szerkesztések elkerülése miatt a Vespucci „zárolt” módban indul, olyan módban, amely csak a nagyítást és a térkép mozgatását engedélyezi. Koppintson a ![Zárolt](../images/locked.png) ikonra a képernyő feloldásához. 

A long press on the lock icon or the _Modes_ menu in the map display overflow menu will display a menu offering 4 options:

* **Normal** - the default editing mode, new objects can be added, existing ones edited, moved and removed. Simple white lock icon displayed.
* **Tag only** - selecting an existing object will start the Property Editor, new objects can be added via the green "+" button, or long press, but no other geometry operations are enabled. White lock icon with a "T" is displayed.
* **Address** - enables Address mode, a slightly simplified mode with specific actions available from the [Simple mode](../en/Simple%20actions.md) "+" button. White lock icon with an "A" is displayed.
* **Indoor** - enables Indoor mode, see [Indoor mode](#indoor). White lock icon with an "I" is displayed.
* **C-Mode** - enables C-Mode, only objects that have a warning flag set will be displayed, see [C-Mode](#c-mode). White lock icon with a "C" is displayed.

If you are using Vespucci on an Android device that supports short cuts (long press on the app icon) you can start directly to _Address_ and _Indoor_ mode.

#### Egyszeres koppintás, dupla koppintás, hosszú nyomás

Alapból a kiválasztható pontok és vonalak körül egy narancssárga terület van, amely azt jelzi, hol kell megérintenie az objektumot a kiválasztásához. Három lehetősége van:

* Egyszeres koppintás: Kiválasztja az objektumot. 
 * Egy izolált pont/út azonnal kiemelésre kerül. 
 * Viszont ha megpróbál kiválasztani egy objektumot, és a Vespucci úgy határoz, hogy több objektumra is gondolhatott, akkor egy választómenüt jelenít meg, így kiválaszthatja a megfelelő objektumot. 
 * A kiválasztott objektumok sárgával lesznek kiemelve. 
 * Tovább információkért lásd [Kiválasztott pont](Node%20selected.md), [Kiválasztott út](Way%20selected.md) and [Kiválasztott kapcsolat](Relation%20selected.md).
* Dupla koppintás: [Többszörös kiválasztási mód](Multiselect.md) indítása
* Hosszú nyomás: Létrehoz egy „célkeresztet”, amellyel új jegyzetet hozhat létre, lásd lent és itt: [Új objektumok létrehozása](Creating%20new%20objects.md). Ez csak akkor engedélyezett, ha az „Egyszerű mód” ki van kapcsolva.

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

Amint kiválasztott egy objektumot, az mozgatható lesz. Ne feledje, hogy csak a kijelölt objektumok mozgathatóak. Egyszerűen húzza (a tolerancia zónán belül) a kiválasztott objektumot a mozgatáshoz. Ha nagy húzási területet választ ki a [beállításokban](Preferences.md), akkor nagy területet kap a kiválasztott pont körül, így könnyebben pozicionálhatja az objektumot. 

#### Új pont vagy vonal létrehozása 

Az alkalmazás első indításakor „Egyszerű módban” indul, ez módosítható a főmenüben, a megfelelő jelölőmező kikapcsolásával.

##### Egyszerű mód

A nagy zöld lebegő gomb a fő térképképernyőn egy menüt jelenít meg. Miután kiválasztotta az egyik elemet, arra lesz kérve, hogy koppintson a képernyő azon helyére, ahol létre akarja hozni az objektumot, a mozgás és a nagyítás továbbra is működik, ha igazítania kell a térképnézeten. 

See [Creating new objects in simple actions mode](Simple%20actions.md) for more information. Simple mode os the default for new installs.

##### Speciális mód (hosszú lenyomás)

Hosszan nyomja meg ott, ahol a pontot vagy a vonal kezdőpontját szeretné elhelyezni. Egy fekete „célkereszt” szimbólumot fog látni.
* Ha új (objektumhoz nem kapcsolódó)pontot szeretne létrehozni, akkor a már meglévő objektumoktól távolabb koppintson a képernyőre.
* Ha egy vonalat szeretne kiegészíteni, akkor a vonal „toleranciazónájára” (vagy a vonal egy pontjára) koppintson. A toleranciazónát a pont vagy vonal körüli terület jelzi.

Ha látja a célkereszt szimbólumot, akkor ezek a lehetőségei:

* _Normális nyomás ugyanott._
* Ha a célkereszt nincs egy pont közelében, akkor ugyanott újból megérintve új pont jön létre. Ha egy vonal közelében van (de nem egy pont közelében), akkor az új pont a vonalon lesz (és csatlakozni fog hozzá).
* Ha a célkereszt egy pont közelében van (azaz a pont toleranciazónáján belül), ugyanott újból megérintve kijelöljük a pontot (és megnyílik a címkeszerkesztő). Nem jön létre új pont. A művelet megegyezik a fenti kijelöléssel.
* _ Normál érintés egy másik helyen._ Egy másik hely megérintése (a célkereszt toleranciazónáján kívül) létrehoz egy vonalszakaszt az eredeti pozíciótól az aktuális pozícióig. Ha a célkereszt egy vonal vagy pont közelében volt, akkor az új szakasz kapcsolódik ahhoz a ponthoz vagy vonalhoz.

Egyszerűen érintse meg a képernyőt ott, ahol a vonalhoz további pontokat szeretne hozzáadni. A befejezéshez érintse meg kétszer az utolsó pontot. Ha az utolsó pont egy vonalon vagy ponton található, akkor a szakasz automatikusan kapcsolódik ehhez a vonalhoz vagy ponthoz. 

Használhatja a menüelemet is: További információkért lásd: [Új objektumok létrehozása](Creating%20new%20objects.md).

#### Terület létrehozása

Más geoadat rendszerekkel ellentétben, az OpenStreetMap jelenleg nem rendelkezik „terület” objektumtípussal. Az „iD” online szerkesztő megpróbál egy terület absztrakciót biztosítani az alacsonyabb szintű OSM elemekből, amely egyes esetekben jól működik, máskor nem. A Vespucci jelenleg meg sem próbál hasonlót, így valamennyit tudnia kell a területek ábrázolásáról:

* _zárt vonal (sokszög)_: a legegyszerűbb és leggyakoribb területváltozat, olyan vonalak, amelyeknek közös első és utolsó csomópontja egy zárt „gyűrűt” alkot (például a legtöbb épület ilyen típusú). Ezeket nagyon könnyű létrehozni a Vespucciban: a terület megrajzolásának végén az utolsó pontot egyszerűen kössük vissza az első ponthoz. Megjegyzés: a zárt vonal értelmezése a címkézésétől függ: ha például egy zárt vonal épületként van címkézve, akkor területnek fog minősülni, ha viszont körforgalomként van címkézve, akkor nem. Bizonyos helyzetekben, amikor mindkét értelmezés érvényes lehet, a „terület” (area) címke tisztázhatja a rendeltetést.
* _multipoligon_: egyes területek több részből, lyukakból és gyűrűkből állnak, amelyek nem ábrázolhatók egyetlen vonallal. Erre az OSM egy speciális kapcsolattípust használ, a multipoligont. (Ez egy általános célú objektum, amely az elemei közötti kapcsolatokat is képes modellezni.) Egy multipoligon egy vagy több „külső” és nulla, egy vagy több „belső” gyűrűből állhat. Egy gyűrű lehet egy zárt vonal, ahogyan azt fentebb leírtuk, vagy több egyedi vonal, amelyek közös végpontjaikkal kapcsolódnak egymáshoz. A nagy multi-poligonokat minden eszközzel nehéz kezelni, a kisebbeket viszont nem nehéz létrehozni a Vespucciban. 
* _partvonal_: a nagyon nagy objektumok, kontinensek és szigetek esetében még a multipoligonos modell sem működik kielégítően. A natural=coastline címkéjű vonalaknál irányfüggő szemantikát feltételezünk: a szárazföld a vonal bal oldalán van, a víz pedig a jobb oldalán. Ennek mellékhatása, hogy általában a partvonalként címkézett vonalak irányát általában nem szabad megfordítani. További információ az [OSM wiki](http://wiki.openstreetmap.org/wiki/Tag:natural%3Dcoastline) oldalon található.

#### Vonalgeometria javítása

Ha eléggé ránagyít a kiválasztott vonalra, akkor egy kis „x”-et fog látni az elég hosszú vonalszakaszok közepén. Az „x” húzása új pontot hoz létre azon a helyen. Megjegyzés: próbálja elkerülni a pontok véletlen létrehozását, az érintési toleranciaterület elég kicsi ennél a műveletnél.

#### Kivágás, másolás és beillesztés

You can copy selected nodes and ways, and then paste once or multiple times to a new location. Cutting will retain the osm id and version, thus can only be pasted once. To paste long press the location you want to paste to (you will see a cross hair marking the location). Then select "Paste" from the menu.

#### Címek hatékony hozzáadása

A Vespucci támogat olyan funkciókat, amelyekkel hatékonyabban lehet a terepen címeket felmérni. Ez a házszámok előrejelzését (az utcák bal és jobb oldalán külön-külön) és – az utoljára használt érték és a közelség alapján – az _addr:street_ vagy _addr:place_ címkék automatikus hozzáadását jelenti. A legjobb esetben ezzel mindenféle gépelés nélkül is megadható egy cím.   

A címkék hozzáadása a [Address](../images/address.png) ikonra koppintva indítható el: 

* hosszú megnyomás után (csak nem egyszerű módban): A Vespucci létrehoz egy pontot az adott helyen, és a lehető legjobban kitalálja a házszámot az utóbbi időben használt címcímkék segítségével. Ha a pont egy épület körvonalán van, akkor automatikusan egy „entrance=yes” (bejárat) címkét hozzáad is a ponthoz. Megnyílik a címkeszerkesztő az adott objektumhoz, és így el lehet végezni a további szükséges módosításokat.
* a kijelölt pont/vonal módokban: A Vespucci a fentiek szerint adja hozzá a címcímkéket, és elindítja a címke szerkesztőt.
* a tulajdonságszerkesztőben.

Az alapértelmezett „Egyszerű módban” egyedi címpont közvetlen létrehozásához válts a „Cím” szerkesztési módra (hosszan nyomd meg a lakatot), és ekkor a „Címpont hozzáadása” létrehoz egy címcsomópontot a helyen. Ha az egy épület körvonalán van, akkor a fentiek szerint egy egy bejárat címkét is létrehoz.

A házszámok becslésének működéséhez jellemzően legalább két házszám szükséges az út két oldalán, minél több szám szerepel az adatokban, annál jobb.

Fontolja meg ennek használatát az [automatikus letöltési](#donwload) módok használatánál.  

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

Vespucci supports OAuth 2, OAuth 1.0a authorization and the classical username and password method. Since July 1st 2024 the standard OpenStreetMap API only supports OAuth 2 and other methods are only available on private installations of the API or other projects that have repurposed OSM software.  

Authorizing Vespucci to access your account on your behalf requires you to one time login with your display name and password. If your Vespucci install isn't authorized when you attempt to upload modified data you will be asked to login to the OSM website (over an encrypted connection). After you have logged on you will be asked to authorize Vespucci to edit using your account. If you want to or need to authorize the OAuth access to your account before editing there is a corresponding item in the "Tools" menu.

Ha menteni akarja a munkáját, és nincs internetkapcsolata, akkor elmentheti egy JOSM kompatibilis .osm fájlba, és felöltheti később a Vespuccival vagy a JOSM-mel. 

#### Ütközések feloldása feltöltéskor

A Vespucci rendelkezik egy egyszerű ütközésfeloldóval. Viszont ha azt gondolja, hogy komoly problémák vannak a szerkesztéseivel, akkor exportálja a módosításokat egy .osc fájlba („Exportálás” menüelem az „Átküldés” menüben), majd javítsa ki és töltse fel a JOSM-mel. Lásd a részletes súgót az [ütközésfeloldásról](Conflict%20resolution.md).  

### Nearby point-of-interest display

A nearby point-of-interest display can be shown by pulling the handle in the middle and top of the bottom menu bar up. 

More information on this and other available functionality on the main display can be found here [Main map display](Main%20map%display.md).

## GPS és GPX nyomvonalak használata

A normál beállításokkal a Vespucci megpróbálja engedélyezni a GPS-t (és más műholdas navigációs rendszereket), és ha ez nem lehetséges, akkor a pozíciót az úgynevezett „hálózati helymeghatározás” segítségével határozza meg. Ez a viselkedés feltételezi, hogy normál használat esetén maga az Android készülék úgy van beállítva, hogy csak a GPX által generált helyeket használja (a nyomon követés elkerülése érdekében), azaz az eufemisztikusan „Helymeghatározási pontosság javítása” névre hallgató opció ki van kapcsolva. Ha engedélyezni szeretné az opciót, de el akarja kerülni, hogy a Vespucci visszaessen a „hálózati helymeghatározásra”, akkor kapcsolja ki a [Speciális beállítások](Advanced%20preferences.md) megfelelő opcióját. 

Touching the ![GPS](../images/menu_gps.png) button (normally on the left hand side of the map display) will center the screen on the current position and as you move the map display will be panned to maintain this.  Moving the screen manually or editing will cause the "follow GPS" mode to be disabled and the blue GPS arrow will change from an outline to a filled arrow. To quickly return to the "follow" mode, simply touch GPS button or re-check the equivalent menu option. If the device doesn't have a current location the location marker/arrow will be displayed in black, if a current location is available the marker will be blue.

GPX-nyomvonal rögzítéséhez és a készüléken való megjelenítéséhez válassza a ![GPS](../images/menu_gps.png) menü „GPX-nyomvonal indítása” pontját. Ezáltal réteg kerül a kijelzőre az aktuálisan felvett nyomvonallal. A [rétegek](Main%20map%20display.md) pontból feltölthet és exportálhat nyomvonalakat. További rétegek adhatók hozzá helyi GPX fájlokból és az OSM API-val letöltött nyomvonalakból.

Megjegyzés: alapértelmezés szerint a Vespucci nem rögzít magassági adatokat a GPX nyomvonallal együtt, ez néhány Android-specifikus probléma miatt van. A magassági adatok rögzítésének engedélyezéséhez telepítsen egy gravitációs modellt, vagy egyszerűbben, menjen a [Speciális beállítások](Advanced%20preferences.md) menüpontba, és konfigurálja az NMEA bemenetet.

### How to export a GPX track?

Open the layer menu, then click the 3-dots menu next to "GPX recording", then select **Export GPX track...**. Choose in which folder to export the track, then give it a name suffixed with `.gpx` (example: MyTrack.gpx).

## Notes, Bugs and Todos

A Vespucci támogatja az OSM jegyzetek (régebben OSM hibák) letöltését, lezárását és a megjegyzések hozzáfűzését, valamint támogatja az [OSMOSE hibaellenőrző eszköz](http://osmose.openstreetmap.fr/en/map/) „hibáit” is. Mind a kettőt vagy le kell tölteni direktben, vagy használhatja az automatikus letöltési lehetőséget, hogy elérje a közeli területen lévő elemeket. Ha már egyszer szerkesztette vagy lezárta őket, akkor egyesével vagy egyben is feltöltheti a hibákat vagy jegyzeteket. 

Further we support "Todos" that can either be created from OSM elements, from a GeoJSON layer, or externally to Vespucci. These provide a convenient way to keep track of work that you want to complete. 

On the map the Notes and bugs are represented by a small bug icon ![Bug](../images/bug_open.png), green ones are closed/resolved, blue ones have been created or edited by you, and yellow indicates that it is still active and hasn't been changed. Todos use a yellow checkbox icon.

The OSMOSE bug and Todos display will provide a link to the affected element in blue (in the case of Todos only if an OSM element is associated with it), touching the link will select the object, center the screen on it and down load the area beforehand if necessary. 

### Szűrés

A jegyzetek és hibák megjelenítésének globális bekapcsolása mellett beállítható egy durva szűrőt, hogy csökkentse a zsúfoltságot. A szűrő beállítási a feladatréteg bejegyzésből érhetők el a [rétegvezérlésből](#layers):

* Notes
* Osmose error
* Osmose warning
* Osmose minor issue
* Maproulette
* Todo

<a id="indoor"></a>

## Beltéri mód

A beltéri térképezés kihívásokkal teli a sok egymást fedő objektum miatt. A Vespucci rendelkezik egy dedikált beltéri móddal, amellyel kiszűrheti az objektumokat, melyek nem ugyanazon a szinten vannak, és automatikusan hozzáadhatja a jelenlegi szintet az újonnan létrehozott objektumokhoz.

A mód a zárolás gomb hosszú megnyomásával, és a megfelelő menüelem kiválasztásával engedélyezhető, lásd: [Zárolás, feloldás, módváltás](#lock).

<a id="c-mode"></a>

## C-mód

C-módban csak azok az objektumok jelennek meg, melyeken figyelmeztetés jelző van, így könnyen kiszúrhatja azokat az objektumokat, melyekkel konkrét probléma van, vagy egyeznek a beállítható ellenőrzésekkel. Ha egy objektum kiválasztásra kerül és elindul a tulajdonságszerkesztő C-módban, akkor a legjobban illeszkedő előbeállítás automatikusan beállításra kerül.

A mód a zárolás gomb hosszú megnyomásával, és a megfelelő menüelem kiválasztásával engedélyezhető, lásd: [Zárolás, feloldás, módváltás](#lock).

### Ellenőrzések beállítása

All validations can be disabled/enabled in the "Validator settings/Enabled validations" in the [preferences](Preferences.md). 

The configuration for "Re-survey" entries allows you to set a time after which a tag combination should be re-surveyed. "Check" entries are tags that should be present on objects as determined by matching presets. Entries can be edited by clicking them, the green menu button allows adding of entries.

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

* **Kulcs** – Az a kulcs, amelynek a megfelelő előbeállítás szerint jelen kell lennie az objektumon.
* **Nem kötelező megkövetelése** - A kulcs megkövetelése akkor is, ha a kulcs a megfelelő előbeállítás nem kötelező címkéi között szerepel.

Ez az ellenőrzés úgy működik, hogy először meghatározza az illeszkedő előbeállítást, majd ellenőrzi, hogy a **Kulcs** egy „ajánlott” kulcs-e ennél az objektumnál, az előbeállítás szerint, a **Nem kötelező elemek megkövetelése** kiterjeszti az ellenőrzést azokra a címkékre is, melyek „nem kötelezőek” az objektumon. Megjegyzés: jelenleg a hivatkozott előbeállítások nem kerülnek ellenőrzésre.

## Szűrők

### Címkealapú szűrő

A szűrő a főmenüből engedélyezhető, és a szűrő ikonra koppintva módosítható. További dokumentáció található itt: [Címkeszűrő](Tag%20filter.md).

### Előbeállítás-alapú szűrő

A fentiek alternatívájaként, az objektumok egyes előbeállítások vagy előbeállítás-csoportok alapján kerülnek szűrésre. A szűrő ikonra koppintva megjelenik egy előbeállítás-választó párbeszédablak, amely hasonlít a Vespucciban máshol előfordulókra. Az egyes előbeállítások normál koppintással válaszhatóak ki, az előbeállítás-csoportok pedig hosszú lenyomással (a normál koppintás belép a csoportba). További dokumentáció található itt: [Előbeállítás-szűrő](Preset%20filter.md).

## A Vespucci testreszabása

Az alkalmazás számos vonatkozása testreszabható. Ha valami konkrét dolgot keres, de nem találja meg, akkor a [Vespucci webhely](https://vespucci.io/) kereshető, és további tájékoztatást ad mindarról, ami az eszközön elérhető.

<a id="layers"></a>

### Rétegbeállítások

A rétegbeállítások a rétegvezérlőn keresztül módosíthatók (a jobb felső sarokban található „hamburgermenüben”), az összes többi beállítás a főmenübeállítások gombbal érhető el. A rétegeket lehet engedélyezni, letiltani és ideiglenesen elrejteni.

Elérhető rétegtípusok

* Data layer - this is the layer OpenStreetMap data is loaded in to. In normal use you do not need to change anything here. Default: on.
* Background layer - there is a wide range of aerial and satellite background imagery available. The default value for this is the "standard style" map from openstreetmap.org.
* Overlay layer - these are semi-transparent layers with additional information, for example quality assurance information. Adding an overlay may cause issues with older devices and such with limited memory. Default: none.
* Notes/Bugs display - Open Notes and bugs will be displayed as a yellow bug icon, closed ones the same in green. Default: on.
* Photo layer - Displays geo-referenced photographs as red camera icons, if direction information is available the icon will be rotated. Default: off.
* Mapillary layer - Displays Mapillary segments with markers where images exist, clicking on a marker will display the image. Default: off.
* GeoJSON layer - Displays the contents of a GeoJSON file, multiple layers can be added from files. Default: none.
* GPX layer - Displays GPX tracks and way points, multiple layers can be added from files, during recording the generate GPX track is displayed in its own one . Default: none.
* Grid - Displays a scale along the sides of the map or a grid. Default: on. 

További információk találhatók a [térkép megjelenítéséről](Main%20map%20display.md) szóló szakaszban.

#### Beállítások

* Kijelző bekapcsolva tartása. Alapértelmezés: kikapcsolva.
* Nagy ponthúzási terület. A pontok mozgatása érintéses bemenettel rendelkező eszközön nehézkes, mivel az ujjai eltakarják a kijelző aktuális helyzetét. Ennek bekapcsolása nagy területet biztosít, amely a középponton kívüli húzásra használható fel (a kijelölés és más műveletek továbbra is a normál érintéstűrési területet használják). Alapértelmezés: kikapcsolva.

A teljes leírás megtalálható a [beállításoknál](Preferences.md).

#### Speciális beállítások

* Full screen mode. On devices without hardware buttons Vespucci can run in full screen mode, that means that "virtual" navigation buttons will be automatically hidden while the map is displayed, providing more space on the screen for the map. Depending on your device this may work well or not,  In _Auto_ mode we try to determine automatically if using full screen mode is sensible or not, setting it to _Force_ or _Never_ skips the automatic check and full screen mode will always be used or always not be used respectively. On devices running Android 11 or higher the _Auto_ mode will never turn full screen mode on as Androids gesture navigation provides a viable alternative to it. Default: _Auto_.  
* Node icons. Default: _on_.
* Always show context menu. When turned on every selection process will show the context menu, turned off the menu is displayed only when no unambiguous selection can be determined. Default: off (used to be on).
* Enable light theme. On modern devices this is turned on by default. While you can enable it for older Android versions the style is likely to be inconsistent. 

A teljes leírás megtalálhat a [speciális beállításoknál](Advanced%20preferences.md).

## Reporting and Resolving Issues

Ha a Vespucci összeomlik vagy nem konzisztens állapotot észlel, akkor megkérjük, hogy küldje el az összeomlás-jelentésben. Tegyen így, ha ez történik, de helyzetenként csak egyszer. Ha további hozzáfűznivalója van, funkciókérés vagy hasonló okból akar jegyet nyitni, akkor itt tegye meg: [Vespucci hibakövető](https://github.com/MarcusWolschon/osmeditor4android/issues). A főmenüben lévő „Visszajelzés küldése” funkció egy új hibajegyet nyit, és további gépelés nélkül beleteszi a releváns alkalmazás- és eszközinformációkat.

If you are experiencing difficulties starting the app after a crash, you can try to start it in _Safe_ mode on devices that support short cuts: long press on the app icon and then select _Safe_ from the menu. 

If you want to discuss something related to Vespucci, you can either start a discussion on the [OpenStreetMap forum](https://community.openstreetmap.org).


