_Перш ніж ми почнемо: більшість екранів мають посилання в меню на вбудовану довідку, що дає вам прямий доступ до інформації, що стосується поточного контексту, ви також можете легко повернутися до цього тексту. Якщо у вас більший пристрій, наприклад планшет, ви можете відкрити довідкову систему в окремому вікні. Усі довідкові матеріали та багато іншого (поширені запитання, навчальні посібники) також можна знайти на [сайті документації Vespucci](https://vespucci.io/)._

# Веспуччі – вступ

Веспуччі – це повноцінний редактор даних OpenStreetMap, що підтримує  більшість операцій, доступних у редакторах настільних комп’ютерів. Він був успішно протестований на Android від 2.3 до 10.0 версії від Google, а також у різних варіантах на основі AOSP (Android Open Source Project). Застереження: в той час, як мобільні пристрої наздогнали своїх настільних суперників, старі мобільні пристрої  мають обмежений обсяг памʼяті та будуть доволі повільними в роботі. Враховуйте це під час роботи з Веспуччі та завантажуйте розмір ділянки для редагування у розумних межах.

## Редагування у Веспуччі

В залежності від розміру екрана вашого пристрою перехід в режим редагування можливий за допомогою значка зверху на панелі інструментів, через меню на панелі інструментів праворуч, з панелі інструментів знизу (якщо є) або за допомогою кнопки меню.

<a id="download"></a>

### Завантаження даних OSM

Оберіть або значок ![Transfer](../images/menu_transfer.png) або меню "Передача". Ви побачите сім пунктів:

* **Download current view** - download the area visible on the screen and merge it with existing data *(requires network connectivity or offline data source)*
* **Clear and download current view** - clear any data in memory and then download the area visible on the screen *(requires network connectivity)*
* **Upload data to OSM server** - upload edits to OpenStreetMap *(requires authentication)* *(requires network connectivity)*
* **Update data** - re-download data for all areas and update what is in memory *(requires network connectivity)*
* **Location based auto download** - download an area around the current geographic location automatically *(requires network connectivity or offline data)* *(requires GPS)*
* **Pan and zoom auto download** - download data for the currently displayed map area automatically *(requires network connectivity or offline data)* *(requires GPS)*
* **File...** - saving and loading OSM data to/from on device files.
* **Note/Bugs...** - download (automatically and manually) OSM Notes and "Bugs" from QA tools (currently OSMOSE) *(requires network connectivity)*

Найпростіший спосіб завантажити дані на пристрій — це масштабування та панорамування до місця, яке потрібно відредагувати, потім виберіть "Звантажити дані для поточного місця". Ви можете масштабувати за допомогою жестів, кнопок масштабування або кнопок регулювання гучності на пристрої. Після цього Веспуччі повинен завантажити дані для поточного місця. Для завантаження даних на пристрій автентифікація не потрібна.

With the default settings any non-downloaded areas will be dimmed relative to the downloaded ones, this is to avoid inadvertently adding duplicate objects in areas that are not being displayed. The behaviour can be changed in the [Advanced preferences](Advanced%20preferences.md).

### Редагування

<a id="lock"></a>

#### Перегляд, розблокування та перемикання режимів

Для того щоб уникнути випадкових змін, Веспуччі запускається в режимі "перегляду", в якому можливе лише пересування мапою та змінення її масштабу. Натисніть на значок ![Locked](../images/locked.png) для розблокування екрану. 

Довге натискання на значок покаже меню, що містить наступні опції:

* **Normal** - the default editing mode, new objects can be added, existing ones edited, moved and removed. Simple white lock icon displayed.
* **Tag only** - selecting an existing object will start the Property Editor, a long press on the main screen will add objects, but no other geometry operations will work. White lock icon with a "T" is displayed.
* **Address** - enables Address mode, a slightly simplified mode with specific actions available from the [Simple mode](../en/Simple%20actions.md) "+" button. White lock icon with an "A" is displayed.
* **Indoor** - enables Indoor mode, see [Indoor mode](#indoor). White lock icon with an "I" is displayed.
* **C-Mode** - enables C-Mode, only objects that have a warning flag set will be displayed, see [C-Mode](#c-mode). White lock icon with a "C" is displayed.

#### Звичайне, подвійне та довге натискання

Типово, точки та лінії мають помаранчеву підсвітку навколо них, що показує де ви можете торкнутись екрану щоб їх виділити. У вас є три варіанти:

* Single tap: Selects object. 
    * An isolated node/way is highlighted immediately. 
    * However, if you try to select an object and Vespucci determines that the selection could mean multiple objects it will present a selection menu, enabling you to choose the object you wish to select. 
    * Selected objects are highlighted in yellow. 
    * For further information see [Node selected](Node%20selected.md), [Way selected](Way%20selected.md) and [Relation selected](Relation%20selected.md).
* Double tap: Start [Multiselect mode](Multiselect.md)
* Long press: Creates a "crosshair", enabling you to add nodes, see below and [Creating new objects](Creating%20new%20objects.md). This is only enabled if "Simple mode" is deactivated.

Кращє наблизитись для вибору об’єкта, якщо ви намагаєтесь редагувати ділянку з великою кількістю об’єктів.

Веспуччі має гарну систему "відміни/повтору"дій, тож не бійтеся експериментувати з вашим пристроєм, але, будь ласка, не надсилайте суто тестові дані, у разі потреби зберігайте їх локально.

#### Selecting / De-selecting (single tap and "selection menu")

Touch an object to select and highlight it. Touching the screen in an empty region will de-select. If you have selected an object and you need to select something else, simply touch the object in question, there is no need to de-select first. A double tap on an object will start [Multiselect mode](Multiselect.md).

Note that if you try to select an object and Vespucci determines that the selection could mean multiple objects (such as a node on a way or other overlapping objects) it will present a selection menu: Tap the object you wish to select and the object is selected. 

Selected objects are indicated through a thin yellow border. The yellow border may be hard to spot, depending on map background and zoom factor. Once a selection has been made, you will see a notification confirming the selection.

Once the selection has completed you will see (either as buttons or as menu items) a list of supported operations for the selected object: For further information see [Node selected](Node%20selected.md), [Way selected](Way%20selected.md) and [Relation selected](Relation%20selected.md).

#### Selected objects: Editing tags

A second touch on the selected object opens the tag editor and you can edit the tags associated with the object.

Note that for overlapping objects (such as a node on a way) the selection menu comes back up for a second time. Selecting the same object brings up the tag editor; selecting another object simply selects the other object.

#### Selected objects: Moving a Node or Way

Once you have selected an object, it can be moved. Note that objects can be dragged/moved only when they are selected. Simply drag near (i.e. within the tolerance zone of) the selected object to move it. If you select the large drag area in the [preferences](Preferences.md), you get a large area around the selected node that makes it easier to position the object. 

#### Додавання нової Точки або Лінії 

При першому запуску програма запускається в «Простому режимі», це можна змінити в головному меню, знявши відповідний прапорець.

##### Простий режим

Tapping the large green floating button on the map screen will show a menu. After you've selected one of the items, you will be asked to tap the screen at the location where you want to create the object, pan and zoom continues to work if you need to adjust the map view. 

See [Creating new objects in simple actions mode](Creating%20new%20objects%20in%20simple%20actions%20mode.md) for more information.

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

#### Покращення геометрії ліній

If you zoom in far enough on a selected way you will see a small "x" in the middle of the way segments that are long enough. Dragging the "x" will create a node in the way at that location. Note: to avoid accidentally creating nodes, the touch tolerance area for this operation is fairly small.

#### Вирізання, Копіювання та Вставка

Ви можете скопіювати або вирізати виділені точки та лінії та вставити їх на нове місце потрібну кількість разів. Під час вирізання версія та ідентифікатор osm зберігаються. Для вставки натисніть та потримайте деякий час потрібне місце (ви побачите "приціл"). Потім оберіть "Вставити" з меню.

#### Швидке додавання адрес

Vespucci supports functionality that makes surveying addresses more efficient by predicting house numbers (left and right sides of streets separately) and automatically adding _addr:street_ or _addr:place_ tags based on the last used value and proximity. In the best case this allows adding an address without any typing at all.   

Adding the tags can be triggered by pressing ![Address](../images/address.png): 

* after a long press (in non-simple mode only): Vespucci will add a node at the location and make a best guess at the house number and add address tags that you have been lately been using. If the node is on a building outline it will automatically add an "entrance=yes" tag to the node. The tag editor will open for the object in question and let you make any necessary further changes.
* in the node/way selected modes: Vespucci will add address tags as above and start the tag editor.
* in the property editor.

To add individual address nodes directly while in the default "Simple mode" switch to "Address" editing mode (long press on the lock button), "Add address node" will then add an address node at the location and if it is on a building outline add a entrance tag to it as described above.

Пропонування номерів будинків потребує наявності не менше двох номерів з кожного боку вулиці, чим більше є даних, тим звісно краще.

Consider using this with one of the [Auto-download](#download) modes.  

#### Додавання обмежень поворотів

Vespucci has a fast way to add turn restrictions. if necessary it will split ways automatically and ask you to re-select elements. 

* select a way with a highway tag (turn restrictions can only be added to highways, if you need to do this for other ways, please use the generic "create relation" mode)
* select "Add restriction" from the menu
* select the "via" node or way (only possible "via" elements will have the touch area shown)
* select the "to" way (it is possible to double back and set the "to" element to the "from" element, Vespucci will assume that you are adding an no_u_turn restriction)
* set the restriction type

### Веспуччі в режимі "перегляду"

Коли показується червоний замок, вам доступні дії не повʼязані з редагуванням. Довге торкання до обʼєкта дозволяє побачити інформацію про нього, якщо він є обʼєктом ОСМ.

### Збереження вашіх змін

*(потрібне зʼєднання з мережею)*

Select the same button or menu item you did for the download and now select "Upload data to OSM server".

Vespucci підтримує авторизацію OAuth і класичний метод з використанням імені користувача та пароля. OAuth є кращим, оскільки дозволяє уникнути надсилання паролів у відкритому вигляді.

New Vespucci installs will have OAuth enabled by default. On your first attempt to upload modified data, a page from the OSM website loads. After you have logged on (over an encrypted connection) you will be asked to authorize Vespucci to edit using your account. If you want to or need to authorize the OAuth access to your account before editing there is a corresponding item in the "Tools" menu.

Якщо ви хочете зберегти свою роботу і не маєте доступу до Інтернету, ви можете зберегти у JOSM-сумісний файл .osm і завантажити пізніше за допомогою Vespucci або JOSM. 

#### Resolving conflicts on uploads

Vespucci has a simple conflict resolver. However if you suspect that there are major issues with your edits, export your changes to a .osc file ("Export" menu item in the "Transfer" menu) and fix and upload them with JOSM. See the detailed help on [conflict resolution](Conflict%20resolution.md).  

## Using GPS and GPX tracks

With standard settings Vespucci will try to enable GPS (and other satellite based navigation systems) and will fallback to determining the position via so called "network location" if this is not possible. This behaviour assumes that you in normal use have your Android device itself configured to only use GPX generated locations (to avoid tracking), that is you have the euphemistically named "Improve Location Accuracy" option turned off. If you want to enable the option but want to avoid Vespucci falling back to "network location", you should turn the corresponding option in the [Advanced preferences](Advanced%20preferences.md) off. 

Touching the ![GPS](../images/menu_gps.png) button (on the left hand side of the map display) will center the screen on the current position and as you move the map display will be padded to maintain this.  Moving the screen manually or editing will cause the "follow GPS" mode to be disabled and the blue GPS arrow will change from an outline to a filled arrow. To quickly return to the "follow" mode, simply touch GPS button or re-check the equivalent menu option. If the device doesn't have a current location the location marker/arrow will be displayed in black, if a current location is available the marker will be blue.

To record a GPX track and display it on your device select "Start GPX track" item in the ![GPS](../images/menu_gps.png) menu. This will add layer to the display with the current recorded track, you can upload and export the track from the entry in the [layer control](Main%20map%20display.md). Further layers can be added from local GPX files and tracks downloaded from the OSM API.

Note: by default Vespucci will not record elevation data with your GPX track, this is due to some Android specific issues. To enable elevation recording, either install a gravitational model, or, simpler, go to the [Advanced preferences](Advanced%20preferences.md) and configure NMEA input.

## Notes and Bugs

Vespucci підтримує завантаження, коментування та закриття нотаток osm (раніше osm bugs) та еквівалентну функціональність для "помилок", створених [інструментом забезпечення якості osmose] (http://osmose.openstreetmap.fr/en/map/). Обидва інструменти потрібно або завантажити окремо, або скористатися функцією автоматичного завантаження, щоб отримати доступ до елементів, які знаходяться у вашій безпосередній близькості. Після редагування або закриття ви можете або негайно завантажити помилку або примітку, або завантажити всі одразу.

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

## Режим редагування приміщень

Редагування планів приміщень завжди є не простим завданням через високу щільність об'єктів, які дуже часто перекривають друг друга. Веспуччі має спеціальний режим для цього, який дозволяє показувати тільки ті об'єкти які знаходяться на одному поверсі та який автоматично додає номер поверху до новостворених об'єктів.

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

#### Уподобання

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


