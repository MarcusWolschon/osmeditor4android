_Перш чым мы пачнем: у меню большасці экранаў ёсць спасылкі на сістэму даведак на прыладзе, якая дае вам прамы доступ да інфармацыі, актуальнай для бягучага кантэксту, вы таксама можаце лёгка вярнуцца да гэтага тэксту. Калі ў вас большая прылада, напрыклад, планшэт, вы можаце адкрыць даведачную сістэму ў асобным падзеленым акне. Усе тэксты даведкі і многае іншае (FAQ, навучальныя дапаможнікі) таксама можна знайсці на [сайце дакументацыі Vespucci](https://vespucci.io/)._

# Vespucci Увядзенне

Vespucci - гэта поўнафункцыянальны рэдактар ​​OpenStreetMap, які падтрымлівае большасць аперацый, даступных у настольных рэдактарах. Ён быў паспяхова пратэставаны на Android ад Google ад 2.3 да 10.0 і розных варыянтах на аснове AOSP. Слова засцярогі: у той час як магчымасці мабільных прылад дагналі сваіх настольных канкурэнтаў, асабліва старыя прылады маюць вельмі абмежаваную памяць і, як правіла, працуюць даволі павольна. Вы павінны прыняць гэта да ўвагі пры выкарыстанні Vespucci і захаваць, напрыклад, разумныя памеры абласцей, якія вы рэдагуеце.

## Рэдагаванне з Vespucci

У залежнасці ад памеру экрана і ўзросту вашай прылады дзеянні рэдагавання могуць быць даступныя непасрэдна праз значкі ў верхняй панэлі, праз выпадальнае меню справа ад верхняй панэлі, з ніжняй панэлі (калі ёсць) або праз клавішу меню.

<a id="download"></a>

### Загрузка дадзеных OSM

Выберыце або значок перадачы ![Перадача](../images/menu_transfer.png) або пункт меню «Перадача». Гэта адлюструе некалькі варыянтаў:

* **Спампаваць гэты выгляд** - спампаваць вобласць, бачную на экране, і аб'яднаць яе з існуючымі дадзенымі *(патрабуецца падключэнне да сеткі або аўтаномная крыніца даных)*
* **Ачысціць і загрузіць гэты выгляд** - ачысціць усе дадзеныя ў памяці, а затым загрузіць вобласць, бачную на экране *(патрабуецца падключэнне да сеткі)*
* **Загрузіць дадзеныя на сервер OSM** - загрузіць змены ў OpenStreetMap *(патрабуецца аўтэнтыфікацыя)* *(патрабуецца падключэнне да сеткі)*
* **Абнавіць даныя** - паўторна загрузіць даныя для ўсіх абласцей і абнавіць тое, што ёсць у памяці *(патрабуецца падключэнне да сеткі)*
* **Аўтаматычная загрузка на аснове месцазнаходжання** - аўтаматычна спампоўвайце вобласць вакол бягучага геаграфічнага месцазнаходжання *(патрабуецца падключэнне да сеткі або пазасеткавыя дадзеныя)* *(патрабуецца GPS)*
* **Аўтаматычная загрузка панарамавання і маштабавання** - аўтаматычна спампоўвайце даныя для вобласці карты, якая адлюстроўваецца ў дадзены момант *(патрабуецца падключэнне да сеткі або аўтаномныя дадзеныя)* *(патрабуецца GPS)*
* **Файл...** - захаванне і загрузка дадзеных OSM у/з файлаў прылады.
* **Заўвага/Памылкі...** - загрузка (аўтаматычна і ўручную) OSM-нататак і «Памылак» з інструментаў кантролю якасці (у цяперашні час OSMOSE) *(патрабуецца падключэнне да сеткі)*

Самы просты спосаб загрузіць даныя на прыладу - павялічыць і панарамаваць месца, якое вы хочаце адрэдагаваць, а затым выбраць «Спампаваць бягучы выгляд». Вы можаце маштабаваць з дапамогай жэстаў, кнопак маштабавання або кнопак рэгулявання гучнасці на прыладзе. Затым Vespucci павінен загрузіць даныя для бягучага выгляду. Для загрузкі даных на прыладу не патрабуецца аўтэнтыфікацыя.

Пры наладах па змаўчанні любыя незагружаныя вобласці будуць цьмянымі адносна загружаных, гэта робіцца для таго, каб пазбегнуць ненаўмыснага дадання дублікатаў аб'ектаў у вобласці, якія не адлюстроўваюцца. Паводзіны можна змяніць у [Дадатковыя налады](Advanced%20preferences.md).

### Рэдагаванне

<a id="lock"></a>

#### Блакаванне, разблакіроўка, пераключэнне рэжымаў

Каб пазбегнуць выпадковых правак, Vespucci запускаецца ў «заблакіраваным» рэжыме, які дазваляе толькі маштабаванне і перамяшчэнне карты. Дакраніцеся да значка ![Заблакіравана](../images/locked.png), каб разблакіраваць экран. 

Доўгі націск на значок замка адлюструе меню з 4 варыянтамі:

* **Звычайны** - рэжым рэдагавання па змаўчанні, новыя аб'екты можна дадаваць, існуючыя рэдагаваць, перамяшчаць і выдаляць. Адлюстроўваецца просты белы значок замка.
* **Толькі тэг** - пры выбары існуючага аб'екта запусціцца рэдактар ​​уласцівасцей, доўгае націсканне на галоўным экране дадасць аб'екты, але іншыя геаметрычныя аперацыі працаваць не будуць. Адлюструецца белы значок замка з "Т".
* **Адрас** - уключае рэжым адрасу, крыху спрошчаны рэжым з пэўнымі дзеяннямі, даступнымі з дапамогай кнопкі «+» [Просты рэжым](../en/Simple%20actions.md). Адлюструецца белы значок замка з "А".
* **У памяшканні** - уключае рэжым у памяшканні, гл. [Рэжым у памяшканні](#indoor). Адлюструецца белы значок замка з "I".
* **C-Mode** - уключае C-Mode, будуць адлюстроўвацца толькі тыя аб'екты, якія маюць усталяваны сцяг папярэджання, гл. [C-Mode](#c-mode). Адлюструецца белы значок замка з "C".

#### Адзін націск, двайны націск і доўгае націсканне

Па змаўчанні вузлы і шляхі, якія можна выбраць, маюць аранжавую вобласць вакол іх, якая прыблізна паказвае месца, дзе трэба дакрануцца, каб выбраць аб'ект. У вас ёсць тры варыянты:

* Адно націсканне: выбар аб'екта.
* Ізаляваны вузел/шлях адразу вылучаецца.
* Аднак, калі вы паспрабуеце выбраць аб'ект і Vespucci вызначыць, што выбар можа азначаць некалькі аб'ектаў, ён прадставіць меню выбару, якое дазволіць вам выбраць аб'ект, які вы хочаце выбраць.
* Выбраныя аб'екты вылучаюцца жоўтым колерам.
* Для атрымання дадатковай інфармацыі гл. [Выбраны вузел](Node%20selected.md), [Выбраны шлях](Way%20selected.md) і [Выбраная сувязь](Relation%20selected.md).
* Двойчы націск: запусціце [рэжым множнага выбару] (Multiselect.md)
* Доўгі націск: стварае "крыжык", што дазваляе дадаваць вузлы, глядзіце ніжэй і [Стварэнне новых аб'ектаў](Creating%20new%20objects.md). Гэта ўключана, толькі калі «Просты рэжым» адключаны.

Гэта добрая стратэгія - павялічваць маштаб, калі вы спрабуеце рэдагаваць вобласць з высокай шчыльнасцю.

У Vespucci ёсць добрая сістэма "адмяніць/паўтарыць", так што не бойцеся эксперыментаваць на сваёй прыладзе, аднак, калі ласка, не загружайце і не захоўвайце чыстыя тэставыя дадзеныя.

#### Выбар / адмена выбару (адно націсканне і «меню выбару»)

Дакраніцеся да аб'екта, каб выбраць і вылучыць яго. Дакрананне да экрана ў пустой вобласці здыме выбар. Калі вы выбралі аб'ект і вам трэба выбраць што-небудзь яшчэ, проста дакраніцеся да аб'екта, пра які ідзе гаворка, не трэба спачатку здымаць выбар. Двойчы націск на аб'ект запускае [рэжым Multiselect](Multiselect.md).

Звярніце ўвагу, што калі вы паспрабуеце выбраць аб'ект і Vespucci вызначыць, што выбар можа азначаць некалькі аб'ектаў (напрыклад, вузел на дарозе або іншыя аб'екты, якія перакрываюцца), адкрыецца меню выбару: націсніце на аб'ект, які вы хочаце выбраць, і аб'ект будзе выбраны. 

Выбраныя аб'екты пазначаны тонкай жоўтай рамкай. Жоўтую мяжу можа быць цяжка заўважыць у залежнасці ад фону карты і каэфіцыента маштабавання. Пасля таго, як выбар будзе зроблены, вы ўбачыце апавяшчэнне, якое пацвярджае выбар.

Пасля завяршэння выбару вы ўбачыце (альбо ў выглядзе кнопак, альбо ў выглядзе пунктаў меню) спіс падтрымліваемых аперацый для абранага аб'екта: Для атрымання дадатковай інфармацыі гл.  [Выбраная кропка](Node%20selected.md), Выбраная лінія](Way%20selected.md) і [Выбраныя адносіны](Relation%20selected.md).

#### Выбраныя аб'екты: рэдагаванне тэгаў

Другі дотык да абранага аб'екта адкрывае рэдактар ​​тэгаў, і вы можаце рэдагаваць тэгі, звязаныя з аб'ектам.

Звярніце ўвагу, што для аб'ектаў, якія перакрываюцца (напрыклад, вузел на шляху), меню выбару вяртаецца другі раз. Выбар таго ж аб'екта выклікае рэдактар ​​тэгаў; выбар іншага аб'екта проста выбірае іншы аб'ект.

#### Выбраныя аб'екты: перасоўванне кропкі або лініі

Пасля таго, як вы выбралі аб'ект, яго можна перамяшчаць. Звярніце ўвагу, што аб'екты можна перацягваць/перамяшчаць, толькі калі яны выбраны. Проста перацягніце побач (г.зн. у зоне допуску) выбраны аб'ект, каб перамясціць яго. Калі вы вылучыце вялікую вобласць перацягвання ў [наладах](Preferences.md), вы атрымаеце вялікую вобласць вакол абранага вузла, што палягчае размяшчэнне аб'екта. 

#### Adding a new Node/Point or Way 

Пры першым запуску праграма запускаецца ў «Простым рэжыме», гэта можна змяніць у галоўным меню, зняўшы адпаведны сцяжок.

##### Simple mode

Націск на вялікую зялёную плаваючую кнопку на экране карты адкрые меню. Пасля таго, як вы выбралі адзін з элементаў, вам будзе прапанавана націснуць на экран у месцы, дзе вы хочаце стварыць аб'ект, панарамаванне і маштабаванне працягваюць працаваць, калі вам трэба наладзіць выгляд карты. 

Глядзіце [Стварэнне новых аб'ектаў у рэжыме простых дзеянняў] (Creating%20new%20objects%20in%20simple%20actions%20mode.md) для атрымання дадатковай інфармацыі.

##### Advanced (long press) mode
 
Long press where you want the node to be or the way to start. You will see a black "crosshair" symbol. 
* If you want to create a new node (not connected to an object), touch away from existing objects.
* If you want to extend a way, touch within the "tolerance zone" of the way (or a node on the way). The tolerance zone is indicated by the areas around a node or way.

Once you can see the crosshair symbol, you have these options:

* _Normal press in the same place._
    * If the crosshair is not near a node, touching the same location again creates a new node. If you are near a way (but not near a node), the new node will be on the way (and connected to the way).
    * If the crosshair is near a node (i.e. within the tolerance zone of the node), touching the same location just selects the node (and the tag editor opens. No new node is created. The action is the same as the selection above.
* _Normal touch in another place._ Touching another location (outside of the tolerance zone of the crosshair) adds a way segment from the original position to the current position. If the crosshair was near a way or node, the new segment will be connected to that node or way.

Simply touch the screen where you want to add further nodes of the way. To finish, touch the final node twice. If the final node is located on a way or node, the segment will be connected to the way or node automatically. 

You can also use a menu item: See [Creating new objects](Creating%20new%20objects.md) for more information.

#### Adding an Area

OpenStreetMap currently doesn't have an "area" object type unlike other geo-data systems. The online editor "iD" tries to create an area abstraction from the underlying OSM elements which works well in some circumstances, in others not so. Vespucci currently doesn't try to do anything similar, so you need to know a bit about the way areas are represented:

* _closed ways (*polygons")_: the simplest and most common area variant, are ways that have a shared first and last node forming a closed "ring" (for example most buildings are of this type). These are very easy to create in Vespucci, simply connect back to the first node when you are finished with drawing the area. Note: the interpretation of the closed way depends on its tagging: for example if a closed way is tagged as a building it will be considered an area, if it is tagged as a roundabout it wont. In some situations in which both interpretations may be valid, an "area" tag can clarify the intended use.
* _multi-polygons_: some areas have multiple parts, holes and rings that can't be represented with just one way. OSM uses a specific type of relation (our general purpose object that can model relations between elements) to get around this, a multi-polygon. A multi-polygon can have multiple "outer" rings, and multiple "inner" rings. Each ring can either be a closed way as described above, or multiple individual ways that have common end nodes. While large multi-polygons are difficult to handle with any tool, small ones are not difficult to create in Vespucci. 
* _coastlines_: for very large objects, continents and islands, even the multi-polygon model doesn't work in a satisfactory way. For natural=coastline ways we assume direction dependent semantics: the land is on the left side of the way, the water on the right side. A side effect of this is that, in general, you shouldn't reverse the direction of a way with coastline tagging. More information can be found on the [OSM wiki](http://wiki.openstreetmap.org/wiki/Tag:natural%3Dcoastline).

#### Improving Way Geometry

If you zoom in far enough on a selected way you will see a small "x" in the middle of the way segments that are long enough. Dragging the "x" will create a node in the way at that location. Note: to avoid accidentally creating nodes, the touch tolerance area for this operation is fairly small.

#### Cut, Copy & Paste

You can copy or cut selected nodes and ways, and then paste once or multiple times to a new location. Cutting will retain the osm id and version. To paste long press the location you want to paste to (you will see a cross hair marking the location). Then select "Paste" from the menu.

#### Efficiently Adding Addresses

Vespucci supports functionality that makes surveying addresses more efficient by predicting house numbers (left and right sides of streets separately) and automatically adding _addr:street_ or _addr:place_ tags based on the last used value and proximity. In the best case this allows adding an address without any typing at all.   

Adding the tags can be triggered by pressing ![Address](../images/address.png): 

* after a long press (in non-simple mode only): Vespucci will add a node at the location and make a best guess at the house number and add address tags that you have been lately been using. If the node is on a building outline it will automatically add an "entrance=yes" tag to the node. The tag editor will open for the object in question and let you make any necessary further changes.
* in the node/way selected modes: Vespucci will add address tags as above and start the tag editor.
* in the property editor.

To add individual address nodes directly while in the default "Simple mode" switch to "Address" editing mode (long press on the lock button), "Add address node" will then add an address node at the location and if it is on a building outline add a entrance tag to it as described above.

House number prediction typically requires at least two house numbers on each side of the road to be entered to work, the more numbers present in the data the better.

Consider using this with one of the [Auto-download](#download) modes.  

#### Adding Turn Restrictions

Vespucci has a fast way to add turn restrictions. if necessary it will split ways automatically and ask you to re-select elements. 

* select a way with a highway tag (turn restrictions can only be added to highways, if you need to do this for other ways, please use the generic "create relation" mode)
* select "Add restriction" from the menu
* select the "via" node or way (only possible "via" elements will have the touch area shown)
* select the "to" way (it is possible to double back and set the "to" element to the "from" element, Vespucci will assume that you are adding an no_u_turn restriction)
* set the restriction type

### Vespucci in "locked" mode

When the red lock is displayed all non-editing actions are available. Additionally a long press on or near to an object will display the detail information screen if it is an OSM object.

### Saving Your Changes

*(requires network connectivity)*

Select the same button or menu item you did for the download and now select "Upload data to OSM server".

Vespucci supports OAuth authorization and the classical username and password method. OAuth is preferable since it avoids sending passwords in the clear.

New Vespucci installs will have OAuth enabled by default. On your first attempt to upload modified data, a page from the OSM website loads. After you have logged on (over an encrypted connection) you will be asked to authorize Vespucci to edit using your account. If you want to or need to authorize the OAuth access to your account before editing there is a corresponding item in the "Tools" menu.

If you want to save your work and do not have Internet access, you can save to a JOSM compatible .osm file and either upload later with Vespucci or with JOSM. 

#### Resolving conflicts on uploads

Vespucci has a simple conflict resolver. However if you suspect that there are major issues with your edits, export your changes to a .osc file ("Export" menu item in the "Transfer" menu) and fix and upload them with JOSM. See the detailed help on [conflict resolution](Conflict%20resolution.md).  

## Using GPS and GPX tracks

With standard settings Vespucci will try to enable GPS (and other satellite based navigation systems) and will fallback to determining the position via so called "network location" if this is not possible. This behaviour assumes that you in normal use have your Android device itself configured to only use GPX generated locations (to avoid tracking), that is you have the euphemistically named "Improve Location Accuracy" option turned off. If you want to enable the option but want to avoid Vespucci falling back to "network location", you should turn the corresponding option in the [Advanced preferences](Advanced%20preferences.md) off. 

Touching the ![GPS](../images/menu_gps.png) button (on the left hand side of the map display) will center the screen on the current position and as you move the map display will be padded to maintain this.  Moving the screen manually or editing will cause the "follow GPS" mode to be disabled and the blue GPS arrow will change from an outline to a filled arrow. To quickly return to the "follow" mode, simply touch GPS button or re-check the equivalent menu option. If the device doesn't have a current location the location marker/arrow will be displayed in black, if a current location is available the marker will be blue.

To record a GPX track and display it on your device select "Start GPX track" item in the ![GPS](../images/menu_gps.png) menu. This will add layer to the display with the current recorded track, you can upload and export the track from the entry in the [layer control](Main%20map%20display.md). Further layers can be added from local GPX files and tracks downloaded from the OSM API.

Note: by default Vespucci will not record elevation data with your GPX track, this is due to some Android specific issues. To enable elevation recording, either install a gravitational model, or, simpler, go to the [Advanced preferences](Advanced%20preferences.md) and configure NMEA input.

## Notes and Bugs

Vespucci supports downloading, commenting and closing of OSM Notes (formerly OSM Bugs) and the equivalent functionality for "Bugs" produced by the [OSMOSE quality assurance tool](http://osmose.openstreetmap.fr/en/map/). Both have to either be down loaded explicitly or you can use the auto download facility to access the items in your immediate area. Once edited or closed, you can either upload the bug or Note immediately or upload all at once.

On the map the Notes and bugs are represented by a small bug icon ![Bug](../images/bug_open.png), green ones are closed/resolved, blue ones have been created or edited by you, and yellow indicates that it is still active and hasn't been changed. 

The OSMOSE bug display will provide a link to the affected object in blue, touching the link will select the object, center the screen on it and down load the area beforehand if necessary. 

### Filtering

Besides globally enabling the notes and bugs display you can set a coarse grain display filter to reduce clutter. The filter configuration can be accessed from the task layer entry in the [layer control](#layers):

* Notes
* Osmose error
* Osmose warning
* Osmose minor issue
* Maproulette
* Custom

<a id="indoor"></a>

## Indoor mode

Mapping indoors is challenging due to the high number of objects that very often will overlay each other. Vespucci has a dedicated indoor mode that allows you to filter out all objects that are not on the same level and which will automatically add the current level to new objects created there.

The mode can be enabled by long pressing on the lock item, see [Lock, unlock, mode switching](#lock) and selecting the corresponding menu entry.

<a id="c-mode"></a>

## C-Mode

In C-Mode only objects are displayed that have a warning flag set, this makes it easy to spot objects that have specific problems or match configurable checks. If an object is selected and the Property Editor started in C-Mode the best matching preset will automatically be applied.

The mode can be enabled by long pressing on the lock item, see [Lock, unlock, mode switching](#lock) and selecting the corresponding menu entry.

### Configuring checks

Currently there are two configurable checks (there is a check for FIXME tags and a test for missing type tags on relations that are currently not configurable) both can be configured by selecting "Validator settings" in the [preferences](Preferences.md). 

The list of entries is split in to two, the top half lists "re-survey" entries, the bottom half "check entries". Entries can be edited by clicking them, the green menu button allows adding of entries.

#### Re-survey entries

Re-survey entries have the following properties:

* **Key** - Key of the tag of interest.
* **Value** - Value the tag of interest should have, if empty the tag value will be ignored.
* **Age** - how many days after the element was last changed the element should be re-surveyed, if a _check_date_ tag is present that will be the used, otherwise the date the current version was create. Setting the value to zero will lead to the check simply matching against key and value.
* **Regular expression** - if checked **Value** is assumed to be a JAVA regular expression.

**Key** and **Value** are checked against the _existing_ tags of the object in question.

The _Annotations_ group in the standard presets contain an item that will automatically add a _check_date_ tag with the current date.

#### Check entries

Check entries have the following two properties:

* **Key** - Key that should be present on the object according to the matching preset.
* **Require optional** - Require the key even if the key is in the optional tags of the matching preset.

This check works by first determining the matching preset and then checking if **Key** is a "recommended" key for this object according to the preset, **Require optional** will expand the check to tags that are "optional* on the object. Note: currently linked presets are not checked.

## Filters

### Tag based filter

The filter can be enabled from the main menu, it can then be changed by tapping the filter icon. More documentation can be found here [Tag filter](Tag%20filter.md).

### Preset based filter

An alternative to the above, objects are filtered either on individual presets or on preset groups. Tapping on the filter icon will display a preset selection dialog similar to that used elsewhere in Vespucci. Individual presets can be selected by a normal click, preset groups by a long click (normal click enters the group). More documentation can be found here [Preset filter](Preset%20filter.md).

## Customizing Vespucci

Many aspects of the app can be customized, if you are looking for something specific and can't find it, [the Vespucci website](https://vespucci.io/) is searchable and contains additional information over what is available on device.

<a id="layers"></a>

### Layer settings

Layer settings can be changed via the layer control ("hamburger" menu in the upper right corner), all other setting are reachable via the main menu preferences button. Layers can be enabled, disabled and temporarily hidden.

Available layer types:

* Data layer - this is the layer OpenStreetMap data is loaded in to. In normal use you do not need to change anything here. Default: on.
* Background layer - there is a wide range of aerial and satellite background imagery available. The default value for this is the "standard style" map from openstreetmap.org.
* Overlay layer - these are semi-transparent layers with additional information, for example GPX tracks. Adding an overlay may cause issues with older devices and such with limited memory. Default: none.
* Notes/Bugs display - Open Notes and bugs will be displayed as a yellow bug icon, closed ones the same in green. Default: on.
* Photo layer - Displays geo-referenced photographs as red camera icons, if direction information is available the icon will be rotated. Default: off.
* Mapillary layer - Displays Mapillary segments with markers where images exist, clicking on a marker will display the image. Default: off.
* GeoJSON layer - Displays the contents of a GeoJSON file. Default: off.
* Grid - Displays a scale along the sides of the map or a grid. Default: on. 

More information can be found in the section on the [map display](Main%20map%20display.md).

#### Preferences

* Keep screen on. Default: off.
* Large node drag area. Moving nodes on a device with touch input is problematic since your fingers will obscure the current position on the display. Turning this on will provide a large area which can be used for off-center dragging (selection and other operations still use the normal touch tolerance area). Default: off.

The full description can be found here [Preferences](Preferences.md)

#### Advanced preferences

* Node icons. Default: on.
* Always show context menu. When turned on every selection process will show the context menu, turned off the menu is displayed only when no unambiguous selection can be determined. Default: off (used to be on).
* Enable light theme. On modern devices this is turned on by default. While you can enable it for older Android versions the style is likely to be inconsistent. 

The full description can be found here [Advanced preferences](Advanced%20preferences.md)

## Reporting Problems

If Vespucci crashes, or it detects an inconsistent state, you will be asked to send in the crash dump. Please do so if that happens, but please only once per specific situation. If you want to give further input or open an issue for a feature request or similar, please do so here: [Vespucci issue tracker](https://github.com/MarcusWolschon/osmeditor4android/issues). The "Provide feedback" function from the main menu will open a new issue and include the relevant app and device information without extra typing.

If you want to discuss something related to Vespucci, you can either start a discussion on the [Vespucci Google group](https://groups.google.com/forum/#!forum/osmeditor4android) or on the [OpenStreetMap Android forum](http://forum.openstreetmap.org/viewforum.php?id=56)


