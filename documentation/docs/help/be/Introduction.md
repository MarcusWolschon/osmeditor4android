_Before we start: most screens have links in the menu to the on-device help system giving you direct access to information relevant for the current context, you can easily navigate back to this text too. If you have a larger device, for example a tablet, you can open the help system in a separate split window.  All the help texts and more (FAQs, tutorials) can be found on the [Vespucci documentation site](https://vespucci.io/) too. You can further start the help viewer directly on devices that support short cuts with a long press on the app icon and selecting "Help"_

# Vespucci Увядзенне

Vespucci is a full featured OpenStreetMap editor that supports most operations that desktop editors provide. It has been tested successfully on Google's Android 2.3 to 14.0 (versions prior to 4.1 are no longer supported) and various AOSP based variants. A word of caution: while mobile device capabilities have caught up with their desktop rivals, particularly older devices have very limited memory available and tend to be rather slow. You should take this in to account when using Vespucci and keep, for example, the areas you are editing to a reasonable size.

## Рэдагаванне з Vespucci

У залежнасці ад памеру экрана і ўзросту вашай прылады дзеянні рэдагавання могуць быць даступныя непасрэдна праз значкі ў верхняй панэлі, праз выпадальнае меню справа ад верхняй панэлі, з ніжняй панэлі (калі ёсць) або праз клавішу меню.

<a id="download"></a>

### Загрузка дадзеных OSM

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

Самы просты спосаб загрузіць даныя на прыладу - павялічыць і панарамаваць месца, якое вы хочаце адрэдагаваць, а затым выбраць «Спампаваць бягучы выгляд». Вы можаце маштабаваць з дапамогай жэстаў, кнопак маштабавання або кнопак рэгулявання гучнасці на прыладзе. Затым Vespucci павінен загрузіць даныя для бягучага выгляду. Для загрузкі даных на прыладу не патрабуецца аўтэнтыфікацыя.

In unlocked state any non-downloaded areas will be dimmed relative to the downloaded ones if you are zoomed in far enough to enable editing. This is to avoid inadvertently adding duplicate objects in areas that are not being displayed. In the locked state dimming is disabled, this behaviour can be changed in the [Advanced preferences](Advanced%20preferences.md) so that dimming is always active.

If you need to use a non-standard OSM API entry, or use [offline data](https://vespucci.io/tutorials/offline/) in _MapSplit_ format you can add or change entries via the _Configure..._ entry for the data layer in the layer control.

### Рэдагаванне

<a id="lock"></a>

#### Блакаванне, разблакіроўка, пераключэнне рэжымаў

Каб пазбегнуць выпадковых правак, Vespucci запускаецца ў «заблакіраваным» рэжыме, які дазваляе толькі маштабаванне і перамяшчэнне карты. Дакраніцеся да значка ![Заблакіравана](../images/locked.png), каб разблакіраваць экран. 

A long press on the lock icon or the _Modes_ menu in the map display overflow menu will display a menu offering 4 options:

* **Normal** - the default editing mode, new objects can be added, existing ones edited, moved and removed. Simple white lock icon displayed.
* **Tag only** - selecting an existing object will start the Property Editor, new objects can be added via the green "+" button, or long press, but no other geometry operations are enabled. White lock icon with a "T" is displayed.
* **Address** - enables Address mode, a slightly simplified mode with specific actions available from the [Simple mode](../en/Simple%20actions.md) "+" button. White lock icon with an "A" is displayed.
* **Indoor** - enables Indoor mode, see [Indoor mode](#indoor). White lock icon with an "I" is displayed.
* **C-Mode** - enables C-Mode, only objects that have a warning flag set will be displayed, see [C-Mode](#c-mode). White lock icon with a "C" is displayed.

If you are using Vespucci on an Android device that supports short cuts (long press on the app icon) you can start directly to _Address_ and _Indoor_ mode.

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

#### Даданне новага вузла/кропкі або лініі 

Пры першым запуску праграма запускаецца ў «Простым рэжыме», гэта можна змяніць у галоўным меню, зняўшы адпаведны сцяжок.

##### Просты рэжым

Націск на вялікую зялёную плаваючую кнопку на экране карты адкрые меню. Пасля таго, як вы выбралі адзін з элементаў, вам будзе прапанавана націснуць на экран у месцы, дзе вы хочаце стварыць аб'ект, панарамаванне і маштабаванне працягваюць працаваць, калі вам трэба наладзіць выгляд карты. 

See [Creating new objects in simple actions mode](Simple%20actions.md) for more information. Simple mode os the default for new installs.

##### Пашыраны (доўгі націск) рэжым

Доўгі націск на тое месца, дзе вы хочаце размясціць вузел, або пачатак лініі. Вы ўбачыце чорны сімвал «крыжык».
* Калі вы хочаце стварыць новы вузел (не звязаны з аб'ектам), дакраніцеся да існуючых аб'ектаў.
* Калі вы хочаце правіць лінію, дакраніцеся да «зоны допуску» лініі (або вузла на лініі). Зона допуску пазначаецца ўчасткамі вакол вузла або лініі.

Калі вы ўбачыце сімвал крыжыка, у вас ёсць наступныя варыянты:

* _Звычайны націск там жа._
* Калі крыжык знаходзіцца не побач з вузлом, паўторнае дакрананне да таго ж месца стварае новы вузел. Калі вы знаходзіцеся паблізу лініі (але не побач з вузлом), новы вузел будзе на лініі (і падлучаны да лініі).
* Калі перакрыжаванне знаходзіцца побач з вузлом (г.зн. у зоне допуску вузла), дакрананне да таго ж месца проста выбірае вузел (і адкрываецца рэдактар ​​тэгаў). Новы вузел не ствараецца. Дзеянне такое ж, як і пры выбары вышэй).
* _Звычайны дотык у іншым месцы._ Дакрананне да іншага месца (па-за зонай допуску прыцэла) дадае сегмент лініі ад зыходнага становішча да бягучага. Калі прыцэл быў побач з шляхам або вузлом, новы сегмент будзе злучаны з гэтым вузлом або шляхам.

Проста дакраніцеся да экрана, дзе вы хочаце дадаць дадатковыя вузлы шляху. Каб скончыць, двойчы дакраніцеся да апошняга вузла. Калі канчатковы вузел знаходзіцца на шляху або вузле, сегмент будзе падлучаны да шляху або вузла аўтаматычна. 

Вы таксама можаце выкарыстоўваць пункт меню: Глядзіце [Стварэнне новых аб'ектаў](Creating%20new%20objects.md) для атрымання дадатковай інфармацыі.

#### Adding an Area

У цяперашні час OpenStreetMap не мае тыпу аб'екта "вобласць", у адрозненне ад іншых сістэм геаданых. Рэдактар ​​"iD" спрабуе стварыць абстракцыю вобласці з асноўных элементаў OSM, якая працуе добра ў некаторых абставінах, у іншых - не. У цяперашні час Vespucci не спрабуе зрабіць што-небудзь падобнае, таму вам трэба ведаць крыху аб тым, як прадстаўлены вобласці:

* _closed ways (*polygons")_: the simplest and most common area variant, are ways that have a shared first and last node forming a closed "ring" (for example most buildings are of this type). These are very easy to create in Vespucci, simply connect back to the first node when you are finished with drawing the area. Note: the interpretation of the closed way depends on its tagging: for example if a closed way is tagged as a building it will be considered an area, if it is tagged as a roundabout it wont. In some situations in which both interpretations may be valid, an "area" tag can clarify the intended use.
* _multi-polygons_: some areas have multiple parts, holes and rings that can't be represented with just one way. OSM uses a specific type of relation (our general purpose object that can model relations between elements) to get around this, a multi-polygon. A multi-polygon can have multiple "outer" rings, and multiple "inner" rings. Each ring can either be a closed way as described above, or multiple individual ways that have common end nodes. While large multi-polygons are difficult to handle with any tool, small ones are not difficult to create in Vespucci. 
* _coastlines_: for very large objects, continents and islands, even the multi-polygon model doesn't work in a satisfactory way. For natural=coastline ways we assume direction dependent semantics: the land is on the left side of the way, the water on the right side. A side effect of this is that, in general, you shouldn't reverse the direction of a way with coastline tagging. More information can be found on the [OSM wiki](http://wiki.openstreetmap.org/wiki/Tag:natural%3Dcoastline).

#### Выпраўленне геаметрыі ліній

Калі вы дастаткова наблізіце абраную лінію, вы ўбачыце невялікі "х" пасярэдзіне досыць доўгіх адрэзкаў лініі. Перацягванне "x" створыць вузел на лініі ў гэтым месцы. Заўвага: каб пазбегнуць выпадковага стварэння вузлоў, вобласць допуску для гэтай аперацыі даволі малая.

#### Выразаць, капіяваць і ўставіць

You can copy selected nodes and ways, and then paste once or multiple times to a new location. Cutting will retain the osm id and version, thus can only be pasted once. To paste long press the location you want to paste to (you will see a cross hair marking the location). Then select "Paste" from the menu.

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

Vespucci supports OAuth 2, OAuth 1.0a authorization and the classical username and password method. Since July 1st 2024 the standard OpenStreetMap API only supports OAuth 2 and other methods are only available on private installations of the API or other projects that have repurposed OSM software.  

Authorizing Vespucci to access your account on your behalf requires you to one time login with your display name and password. If your Vespucci install isn't authorized when you attempt to upload modified data you will be asked to login to the OSM website (over an encrypted connection). After you have logged on you will be asked to authorize Vespucci to edit using your account. If you want to or need to authorize the OAuth access to your account before editing there is a corresponding item in the "Tools" menu.

If you want to save your work and do not have Internet access, you can save to a JOSM compatible .osm file and either upload later with Vespucci or with JOSM. 

#### Resolving conflicts on uploads

Vespucci has a simple conflict resolver. However if you suspect that there are major issues with your edits, export your changes to a .osc file ("Export" menu item in the "Transfer" menu) and fix and upload them with JOSM. See the detailed help on [conflict resolution](Conflict%20resolution.md).  

### Nearby point-of-interest display

A nearby point-of-interest display can be shown by pulling the handle in the middle and top of the bottom menu bar up. 

More information on this and other available functionality on the main display can be found here [Main map display](Main%20map%display.md).

## Using GPS and GPX tracks

Са стандартнымі наладамі Vespucci паспрабуе ўключыць GPS (і іншыя спадарожнікавыя сістэмы навігацыі) і вернецца да вызначэння месцазнаходжання праз так званае "сеткавае месцазнаходжанне", калі гэта немагчыма. Такія паводзіны мяркуюць, што ваша прылада Android пры звычайным выкарыстанні наладжана на выкарыстанне толькі згенераваных месцазнаходжанняў GPX (каб пазбегнуць адсочвання), гэта значыць у вас выключана эўфемістычна названая опцыя «Палепшыць дакладнасць месцазнаходжання». Калі вы жадаеце ўключыць гэту опцыю, але жадаеце пазбегнуць вяртання Vespucci да «сеткавага месцазнаходжання», вам варта адключыць адпаведную опцыю ў [Дадатковыя налады](Advanced%20preferences.md). 

Touching the ![GPS](../images/menu_gps.png) button (normally on the left hand side of the map display) will center the screen on the current position and as you move the map display will be panned to maintain this.  Moving the screen manually or editing will cause the "follow GPS" mode to be disabled and the blue GPS arrow will change from an outline to a filled arrow. To quickly return to the "follow" mode, simply touch GPS button or re-check the equivalent menu option. If the device doesn't have a current location the location marker/arrow will be displayed in black, if a current location is available the marker will be blue.

To record a GPX track and display it on your device select "Start GPX track" item in the ![GPS](../images/menu_gps.png) menu. This will add layer to the display with the current recorded track, you can upload and export the track from the entry in the [layer control](Main%20map%20display.md). Further layers can be added from local GPX files and tracks downloaded from the OSM API.

Note: by default Vespucci will not record elevation data with your GPX track, this is due to some Android specific issues. To enable elevation recording, either install a gravitational model, or, simpler, go to the [Advanced preferences](Advanced%20preferences.md) and configure NMEA input.

### How to export a GPX track?

Open the layer menu, then click the 3-dots menu next to "GPX recording", then select **Export GPX track...**. Choose in which folder to export the track, then give it a name suffixed with `.gpx` (example: MyTrack.gpx).

## Notes, Bugs and Todos

Vespucci supports downloading, commenting and closing of OSM Notes (formerly OSM Bugs) and the equivalent functionality for "Bugs" produced by the [OSMOSE quality assurance tool](http://osmose.openstreetmap.fr/en/map/). Both have to either be down loaded explicitly or you can use the auto download facility to access the items in your immediate area. Once edited or closed, you can either upload the bug or Note immediately or upload all at once. 

Further we support "Todos" that can either be created from OSM elements, from a GeoJSON layer, or externally to Vespucci. These provide a convenient way to keep track of work that you want to complete. 

On the map the Notes and bugs are represented by a small bug icon ![Bug](../images/bug_open.png), green ones are closed/resolved, blue ones have been created or edited by you, and yellow indicates that it is still active and hasn't been changed. Todos use a yellow checkbox icon.

The OSMOSE bug and Todos display will provide a link to the affected element in blue (in the case of Todos only if an OSM element is associated with it), touching the link will select the object, center the screen on it and down load the area beforehand if necessary. 

### Filtering

Besides globally enabling the notes and bugs display you can set a coarse grain display filter to reduce clutter. The filter configuration can be accessed from the task layer entry in the [layer control](#layers):

* Notes
* Osmose error
* Osmose warning
* Osmose minor issue
* Maproulette
* Todo

<a id="indoor"></a>

## Indoor mode

Mapping indoors is challenging due to the high number of objects that very often will overlay each other. Vespucci has a dedicated indoor mode that allows you to filter out all objects that are not on the same level and which will automatically add the current level to new objects created there.

The mode can be enabled by long pressing on the lock item, see [Lock, unlock, mode switching](#lock) and selecting the corresponding menu entry.

<a id="c-mode"></a>

## C-Mode

In C-Mode only objects are displayed that have a warning flag set, this makes it easy to spot objects that have specific problems or match configurable checks. If an object is selected and the Property Editor started in C-Mode the best matching preset will automatically be applied.

The mode can be enabled by long pressing on the lock item, see [Lock, unlock, mode switching](#lock) and selecting the corresponding menu entry.

### Configuring checks

All validations can be disabled/enabled in the "Validator settings/Enabled validations" in the [preferences](Preferences.md). 

The configuration for "Re-survey" entries allows you to set a time after which a tag combination should be re-surveyed. "Check" entries are tags that should be present on objects as determined by matching presets. Entries can be edited by clicking them, the green menu button allows adding of entries.

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
* Overlay layer - these are semi-transparent layers with additional information, for example quality assurance information. Adding an overlay may cause issues with older devices and such with limited memory. Default: none.
* Notes/Bugs display - Open Notes and bugs will be displayed as a yellow bug icon, closed ones the same in green. Default: on.
* Photo layer - Displays geo-referenced photographs as red camera icons, if direction information is available the icon will be rotated. Default: off.
* Mapillary layer - Displays Mapillary segments with markers where images exist, clicking on a marker will display the image. Default: off.
* GeoJSON layer - Displays the contents of a GeoJSON file, multiple layers can be added from files. Default: none.
* GPX layer - Displays GPX tracks and way points, multiple layers can be added from files, during recording the generate GPX track is displayed in its own one . Default: none.
* Grid - Displays a scale along the sides of the map or a grid. Default: on. 

More information can be found in the section on the [map display](Main%20map%20display.md).

#### Preferences

* Keep screen on. Default: off.
* Large node drag area. Moving nodes on a device with touch input is problematic since your fingers will obscure the current position on the display. Turning this on will provide a large area which can be used for off-center dragging (selection and other operations still use the normal touch tolerance area). Default: off.

The full description can be found here [Preferences](Preferences.md)

#### Advanced preferences

* Full screen mode. On devices without hardware buttons Vespucci can run in full screen mode, that means that "virtual" navigation buttons will be automatically hidden while the map is displayed, providing more space on the screen for the map. Depending on your device this may work well or not,  In _Auto_ mode we try to determine automatically if using full screen mode is sensible or not, setting it to _Force_ or _Never_ skips the automatic check and full screen mode will always be used or always not be used respectively. On devices running Android 11 or higher the _Auto_ mode will never turn full screen mode on as Androids gesture navigation provides a viable alternative to it. Default: _Auto_.  
* Node icons. Default: _on_.
* Always show context menu. When turned on every selection process will show the context menu, turned off the menu is displayed only when no unambiguous selection can be determined. Default: off (used to be on).
* Enable light theme. On modern devices this is turned on by default. While you can enable it for older Android versions the style is likely to be inconsistent. 

The full description can be found here [Advanced preferences](Advanced%20preferences.md)

## Reporting and Resolving Issues

If Vespucci crashes, or it detects an inconsistent state, you will be asked to send in the crash dump. Please do so if that happens, but please only once per specific situation. If you want to give further input or open an issue for a feature request or similar, please do so here: [Vespucci issue tracker](https://github.com/MarcusWolschon/osmeditor4android/issues). The "Provide feedback" function from the main menu will open a new issue and include the relevant app and device information without extra typing.

If you are experiencing difficulties starting the app after a crash, you can try to start it in _Safe_ mode on devices that support short cuts: long press on the app icon and then select _Safe_ from the menu. 

If you want to discuss something related to Vespucci, you can either start a discussion on the [OpenStreetMap forum](https://community.openstreetmap.org).


