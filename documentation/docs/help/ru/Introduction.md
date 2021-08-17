# Введение в Vespucci

Vespucci это полнофункциональный редактор OpenStreetMap для мобильных устройств, который поддерживает большинство операции доступных редакторам для ПК. Работа редактора протестирована на версиях от 2.3 до 10 Android и различных вариантах этой операционной от Google, основанных на AOSP. В качестве предостережения: в то время, как возможности современных мобильных устройств достигли уровня ПК, старые устройства, как правило, довольно медленны и имеют ограниченный размер памяти. Это нужно учитывать при использовании Vespucci — например, разумно ограничивать размер редактируемой области. 

## Первое использование

Сразу после запуска и запроса всех необходимых разрешений и показа приветственного сообщения Vespucci показывает диалог “загрузить область”. Если программа уже определила ваше местоположение, Вы можете выбрать для загрузки область отображаемую на экране. Не загружайте большие области на медленных устройствах. 

Кроме того, вы можете проигнорировать диалог, нажав на кнопку "Перейти к карте", и при помощи перетаскивания и масштабирования карты переместиться к нужному месту на ней, а затем скачать его для редактирования. (См. ниже: "Редактирование в Vespucci")

## Редактирование в Vespucci

В зависимости от размера экрана и возраста вашего устройства редактировать можно при помощи значков на верхней панели, выпадающего меню справа на этой панели, при помощи нижней панели (если есть) или посредством кнопки меню.

<a id="download"></a>

### Загрузка OSM-данных

Выберите значок передачи ![Transfer](../images/menu_transfer.png) или меню "Передать". Будет предложено семь вариантов:

* **Загрузить текущее место** - загрузить пространство видимое на экране и добавить полученное к уже загруженным данным *(необходимо соединение с интернет или локальный источник данных)*
* **Очистить и загрузить текущее место** - очищает ранее загруженные данные, а затем загружает пространство видимое на экране *(необходимо соединение с интернет)*
* **Передать данные на сервер OSM** - загружает правки в OpenStreetMap *(необходима авторизация)*. *(необходимо соединение с интернет)*
* **Обновить данные** - заново загружает в память данные для всех редактируемых областей *(необходимо соединение с интернет)*
* **Автозагрузка по местоположению** - автоматически загружает область вокруг текущего географического местоположения *(необходимо соединение с интернет или локальный источник данных)* *(необходим доступ к GPS)*
* **Автозагрузка по масштабированию** - автоматически загружает данные в области, которая в данный момент отображается на экране *(необходимо соединение с интернет или локальный источник данных)* *(необходим доступ к GPS)*
* **Файл…** - загрузка и сохранение данных OSM из/в память устройства..
* **Задачи...** - автоматическая или ручная загрузка заметок OSM и “проблем” из инструментов контроля качества (в настоящий момент только из OSMOSE) *(необходимо соединение с интернет)*

Самый простой способ загрузить данные на устройство - найти нужную позицию на карте и выбрать "Загрузить текущее место". Вы можете менять масштаб сведением пальцев, кнопками на экране или кнопками громкости. Vespucci загрузит данные для текущей области на карте. Для загрузки данных не требуется авторизация.

По-умолчанию, незагруженные области будут затемнены относительно загруженных. Это сделано для того, чтобы избежать случайного добавления уже существующих объектов в областях, которые не загружены и не отображаются. Такое поведение программы можно изменить в разделе [Расшиненные настройки](Advanced%20preferences.md).

### Редактирование

<a id="lock"></a>

#### Блокировка, разблокировка, переключение режимов

Для предотвращения случайных изменений Vespucci сначала запускается в "заблокированном" режиме, где разрешено только перемещаться по карте. Нажмите на значок ![Locked](../images/locked.png), чтобы разблокировать экран. 

Долгое нажатие на значке блокировки покажет меню с 4-мя элементами:

* **Normal** - the default editing mode, new objects can be added, existing ones edited, moved and removed. Simple white lock icon displayed.
* **Tag only** - selecting an existing object will start the Property Editor, a long press on the main screen will add objects, but no other geometry operations will work. White lock icon with a "T" is displayed.
* **Address** - enables Address mode, a slightly simplified mode with specific actions available from the [Simple mode](../en/Simple%20actions.md) "+" button. White lock icon with an "A" is displayed.
* **Indoor** - enables Indoor mode, see [Indoor mode](#indoor). White lock icon with an "I" is displayed.
* **C-Mode** - enables C-Mode, only objects that have a warning flag set will be displayed, see [C-Mode](#c-mode). White lock icon with a "C" is displayed.

#### Простое, двойное и долгое нажатие

По умолчанию, выделяемые линии и точки имеют вокруг себя оранжевую область, означающую, где нужно прикоснуться, чтобы выбрать объект. У Вас есть три возможности:

* Простое нажатие: выделяет объект. 
    * При этом отдельная точка или линия подсвечиваются. 
    * Однако если вы попытаетесь выделить объект и vespucci определит что можно выделить несколько объектов, появится диалог со списком объектов для выбора.
    * Выделенные объекты подсвечиваются желтым. 
    * Дополнительная информация содержится в главах [Выделена точка](Node%20selected.md), [Выделена линия](Way%20selected.md) и [Выделено отношение](Relation%20selected.md).
* Двойное нажатие: включает режим одновременного выбора объектов [Множественное выделение](Multiselect.md)
* Долгое нажатие: включает "прицел", режим создания точек, который описан, в разделе [Создание новых объектов](Creating%20new%20objects.md). Эти действия доступны, когда отключен "Простой режим" редактора.

Если вы пытаетесь редактировать область с высокой плотностью данных, разумно будет приблизить её.

У Vespucci хорошая система отмены и повторения правок, поэтому не бойтесь экспериментировать на устройстве, однако не загружайте на сервер тестовые данные.

#### Выделение / Снятие выделения (простое нажатие и меню выделения)

Нажмите на объект, чтобы выделить его и выбрать. Касание экрана в пустом месте отменит выбор. Если Вы выбрали объект, а вам нужно выбрать другой, просто нажмите на нужный объект: нет необходимости сначала отменять выбор выделенного объекта. Двойное нажатие на объекте включит режим [Множественное выделение](Multiselect.md). 

Обратите внимание, когда Вы пытаетесь выделить объект и Vespucci посчитает, что может быть выделено несколько объектов (таких, как точка на линии или другие перекрывающиеся объекты), будет показано меню выделения: нажмите на объект, который Вы хотели выбрать и он будет выделен. 

Выбранные объекты отображаются с дополнительным тонким жёлтым бордюром. Он может быть плохо заметен на некоторых цветах фона карты или при некоторых масштабах. Когда выделение сделано, Вы увидите уведомление, подтверждающее выбор.

Once the selection has completed you will see (either as buttons or as menu items) a list of supported operations for the selected object: For further information see [Node selected](Node%20selected.md), [Way selected](Way%20selected.md) and [Relation selected](Relation%20selected.md).

#### Выделенные объекты: Редактирование тэгов

Двойной тап по выбранному обьекту открывает редактор тегов, вы сможете редактировать теги связанные с обьектом.

Note that for overlapping objects (such as a node on a way) the selection menu comes back up for a second time. Selecting the same object brings up the tag editor; selecting another object simply selects the other object.

#### Selected objects: Moving a Node or Way

Once you have selected an object, it can be moved. Note that objects can be dragged/moved only when they are selected. Simply drag near (i.e. within the tolerance zone of) the selected object to move it. If you select the large drag area in the [preferences](Preferences.md), you get a large area around the selected node that makes it easier to position the object. 

#### Добавление новой точки/узла или линии 

On first start the app launches in "Simple mode", this can be changed in the main menu by un-checking the corresponding checkbox.

##### Простой режим

Tapping the large green floating button on the map screen will show a menu. After you've selected one of the items, you will be asked to tap the screen at the location where you want to create the object, pan and zoom continues to work if you need to adjust the map view. 

See [Creating new objects in simple actions mode](Creating%20new%20objects%20in%20simple%20actions%20mode.md) for more information.

##### Advanced (long press) mode
 
Long press where you want the node to be or the way to start. You will see a black "crosshair" symbol. 
* If you want to create a new node (not connected to an object), touch away from existing objects.
* If you want to extend a way, touch within the "tolerance zone" of the way (or a node on the way). The tolerance zone is indicated by the areas around a node or way.

Когда вы увидете символ перекрестия, у вас появятся следующие опции:

* _Normal press in the same place._
    * If the crosshair is not near a node, touching the same location again creates a new node. If you are near a way (but not near a node), the new node will be on the way (and connected to the way).
    * If the crosshair is near a node (i.e. within the tolerance zone of the node), touching the same location just selects the node (and the tag editor opens. No new node is created. The action is the same as the selection above.
* _Normal touch in another place._ Touching another location (outside of the tolerance zone of the crosshair) adds a way segment from the original position to the current position. If the crosshair was near a way or node, the new segment will be connected to that node or way.

Simply touch the screen where you want to add further nodes of the way. To finish, touch the final node twice. If the final node is located on a way or node, the segment will be connected to the way or node automatically. 

You can also use a menu item: See [Creating new objects](Creating%20new%20objects.md) for more information.

#### Добавить область

OpenStreetMap currently doesn't have an "area" object type unlike other geo-data systems. The online editor "iD" tries to create an area abstraction from the underlying OSM elements which works well in some circumstances, in others not so. Vespucci currently doesn't try to do anything similar, so you need to know a bit about the way areas are represented:

* _closed ways (*polygons")_: the simplest and most common area variant, are ways that have a shared first and last node forming a closed "ring" (for example most buildings are of this type). These are very easy to create in Vespucci, simply connect back to the first node when you are finished with drawing the area. Note: the interpretation of the closed way depends on its tagging: for example if a closed way is tagged as a building it will be considered an area, if it is tagged as a roundabout it wont. In some situations in which both interpretations may be valid, an "area" tag can clarify the intended use.
* _multi-ploygons_: some areas have multiple parts, holes and rings that can't be represented with just one way. OSM uses a specific type of relation (our general purpose object that can model relations between elements) to get around this, a multi-polygon. A multi-polygon can have multiple "outer" rings, and multiple "inner" rings. Each ring can either be a closed way as described above, or multiple individual ways that have common end nodes. While large multi-polygons are difficult to handle with any tool, small ones are not difficult to create in Vespucci. 
* _coastlines_: for very large objects, continents and islands, even the multi-polygon model doesn't work in a satisfactory way. For natural=coastline ways we assume direction dependent semantics: the land is on the left side of the way, the water on the right side. A side effect of this is that, in general, you shouldn't reverse the direction of a way with coastline tagging. More information can be found on the [OSM wiki](http://wiki.openstreetmap.org/wiki/Tag:natural%3Dcoastline).

#### Улучшение геометрии линий

If you zoom in far enough on a selected way you will see a small "x" in the middle of the way segments that are long enough. Dragging the "x" will create a node in the way at that location. Note: to avoid accidentally creating nodes, the touch tolerance area for this operation is fairly small.

#### Вырезание, копирование и вставка

Вы можете скопировать или вырезать выделенные точки и линии, а затем вставить их один или несколько раз в новое место. Вырезание сохраняет OSM id и версию. Чтобы вставить, длительно нажмите в месте, в которое вы хотите вставить (вы увидите перекрестие, отмечающее расположение). Затем выберите "Вставить" из меню.

#### Эффективное добавление адресов

Vespucci has an ![Address](../images/address.png) "add address tags" function that tries to make surveying addresses more efficient by predicting the current house number. It can be selected:

* after a long press (_non-simple mode only:): Vespucci will add a node at the location and make a best guess at the house number and add address tags that you have been lately been using. If the node is on a building outline it will automatically add a "entrance=yes" tag to the node. The tag editor will open for the object in question and let you make any necessary further changes.
* in the node/way selected modes: Vespucci will add address tags as above and start the tag editor.
* in the property editor.

Для того, чтобы интерполяция номеров домов работала, обычно требуется, чтобы на каждой стороне дороги уже существовало хотя бы два номера. Чем больше номеров домов имеется в данных этой области, тем лучше.

Consider using this with the [Auto-download](#download) mode.  

#### Добавление ограничений поворотов

Vespucci has a fast way to add turn restrictions. if necessary it will split ways automatically and ask you to re-select elements. 

* select a way with a highway tag (turn restrictions can only be added to highways, if you need to do this for other ways, please use the generic "create relation" mode)
* select "Add restriction" from the menu
* select the "via" node or way (only possible "via" elements will have the touch area shown)
* select the "to" way (it is possible to double back and set the "to" element to the "from" element, Vespucci will assume that you are adding an no_u_turn restriction)
* set the restriction type

### Vespucci в "locked" режиме

Когда показывается красный замок, то все не изменяющие действия активны. Дополнительно, долгий тап по объекту покажет детальную информацию по OSM объекту.

### Сохранение изменений

*(требуется доступ в интернет)*

Используйте ту же кнопку или меню, которые вы выбирали для подгрузки данных, но теперь нажмите "Передача данных на сервер OSM".

Vespucci поддерживает и OAuth-авторизацию, и "классический" вход с помощью логина и пароля OSM. Лучше использовать OAuth, поскольку в этом случае вы не будете передавать свой пароль незашифрованным.

Если Vespucci был установлен недавно, OAuth-авторизация в нём уже включёна по умолчанию. При первой вашей попытке передать на сервер изменения в данных откроется страница сайта OSM. После того, как вы войдёте под своим логином (ваши данные передаются по зашифрованному соединению), вас спросят, разрешить ли Vespucci вносить в базу изменения от вашего имени. Если вы захотите разрешить OAuth-доступ к своей учётной записи заранее, до редактирования, в меню "Инструменты" есть соответствующий пункт.

Если вы хотите сохранить свои труды, а интернет-соединения поблизости нет, вы можете сохранить данные в файл формата .osm и передать его на сервер позднее с помощью Vespucci или JOSM.  

#### Разрешение конфликтов данных при передаче на сервер

Vespucci has a simple conflict resolver. However if you suspect that there are major issues with your edits, export your changes to a .osc file ("Export" menu item in the "Transfer" menu) and fix and upload them with JOSM. See the detailed help on [conflict resolution](Conflict%20resolution.md).  

## Использование GPS

Вы можете использовать Vespucci для создания GPX треков и их просмотра на вашем устройстве. Также вы можете отобразить текущее положение по GPS в центре экрана (отметьте "Показывать местоположение" в меню GPS) и/или следовать за ним (отметьте "Следовать за GPS" в меню GPS). 

If you have the latter set, moving the screen manually or editing will cause the "follow GPS" mode to be disabled and the blue GPS arrow will change from an outline to a filled arrow. To quickly return to the "follow" mode, simply touch GPS button or re-check the menu option.

## Заметки и ошибки

Vespucci supports downloading, commenting and closing of OSM Notes (formerly OSM Bugs) and the equivalent functionality for "Bugs" produced by the [OSMOSE quality assurance tool](http://osmose.openstreetmap.fr/en/map/). Both have to either be down loaded explicitly or you can use the auto download facility to access the items in your immediate area. Once edited or closed, you can either upload the bug or Note immediately or upload all at once.

On the map the Notes and bugs are represented by a small bug icon ![Bug](../images/bug_open.png), green ones are closed/resolved, blue ones have been created or edited by you, and yellow indicates that it is still active and hasn't been changed. 

Дисплей ошибок OSMOSE предоставит ссылку на затронутый объект синим цветом; касание по ссылке выберет объект, центрирует экран на нем и, если необходимо, предварительно загрузит область. 

### Filtering

Besides globally enabling the notes and bugs display you can set a coarse grain display filter to reduce clutter. The filter configuration can be accessed from the task layer entry in the [layer control](#layers):

* Notes
* Osmose error
* Osmose warning
* Osmose minor issue
* Maproulette
* Custom

<a id="indoor"></a>

## План помещения

Mapping indoors is challenging due to the high number of objects that very often will overlay each other. Vespucci has a dedicated indoor mode that allows you to filter out all objects that are not on the same level and which will automatically add the current level to new objects created there.

Режим может быть включен долгим нажатием на заблокированном объекте, см. [Блокировка, разблокировка, переключение режимов](#lock) и выбором соответствующего пункта меню.

<a id="c-mode"></a>

## "Только конфликты" (C-Mode)

In C-Mode only objects are displayed that have a warning flag set, this makes it easy to spot objects that have specific problems or match configurable checks. If an object is selected and the Property Editor started in C-Mode the best matching preset will automatically be applied.

Режим может быть включен долгим нажатием на заблокированном объекте, см. [Блокировка, разблокировка, переключение режимов](#lock) и выбором соответствующего пункта меню.

### Настройка проверок

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
* **Require optional** - Require the key even if the key is in the optional tags of the matching preset .

This check works by first determining the matching preset and then checking if **Key** is a "recommended" key for this object according to the preset, **Require optional** will expand the check to tags that are "optional* on the object. Note: currently linked presets are not checked.

## Фильтры

### Tag based filter

The filter can be enabled from the main menu, it can then be changed by tapping the filter icon. More documentation can be found here [Tag filter](Tag%20filter.md).

### Preset based filter

An alternative to the above, objects are filtered either on individual presets or on preset groups. Tapping on the filter icon will display a preset selection dialog similar to that used elsewhere in Vespucci. Individual presets can be selected by a normal click, preset groups by a long click (normal click enters the group). More documentation can be found here [Preset filter](Preset%20filter.md).

## Настройка Vespucci

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

#### Расширенные параметры

* Node icons. Default: on.
* Always show context menu. When turned on every selection process will show the context menu, turned off the menu is displayed only when no unambiguous selection can be determined. Default: off (used to be on).
* Enable light theme. On modern devices this is turned on by default. While you can enable it for older Android versions the style is likely to be inconsistent. 

The full description can be found here [Advanced preferences](Advanced%20preferences.md)

## Отчёты об ошибках

If Vespucci crashes, or it detects an inconsistent state, you will be asked to send in the crash dump. Please do so if that happens, but please only once per specific situation. If you want to give further input or open an issue for a feature request or similar, please do so here: [Vespucci issue tracker](https://github.com/MarcusWolschon/osmeditor4android/issues). The "Provide feedback" function from the main menu will open a new issue and include the relevant app and device information without extra typing.

If you want to discuss something related to Vespucci, you can either start a discussion on the [Vespucci Google group](https://groups.google.com/forum/#!forum/osmeditor4android) or on the [OpenStreetMap Android forum](http://forum.openstreetmap.org/viewforum.php?id=56)


