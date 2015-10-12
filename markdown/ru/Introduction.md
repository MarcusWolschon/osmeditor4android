# Введение в Vespucci

Vespucci — это полнофункциональный редактор карт OpenStreetMap, поддерживающий большинство операций, предлагаемых настольными аналогами. Он был успешно протестирован на версиях Android с 2.3 по 5.1. Небольшое предостережение: несмотря на то, что мобильные устройства догоняют своих настольных соперников, особенно старые устройства очень ограничены в объёме памяти, и поэтому они могут быть довольно медленными. Это нужно иметь в виду, работая с Vespucci, и ограничивать, например, размер радактируемых областей разумными рамками. 

## Первое использование

После запуска Vespucci показывает диалог "Скачать другое место"/"Загрузить область". Если у вас отображаются координаты, и вы желаете начать загрузку немедленно, вы можете выбрать соответствующую опцию и установить радиус загружаемой области. На медленных устройствах выбирайте небольшие участки. 

Кроме того, вы можете проигнорировать диалог, нажав на кнопку "Перейти к карте", и при помощи перетаскивания и зума карты переместиться к нужному месту на ней, а затем скачать его для редактирования. (См. ниже: "Редактирование в Vespucci")

## Редактирование в Vespucci

В зависимости от размера экрана и возраста вашего устройства редактировать можно при помощи значков на верхней панели, ниспадающего меню справа на этой панели, при помощи нижней панели (если есть) или посредством кнопки меню.

### Загрузка OSM-данных

Выберите значок передачи ![](../images/menu_transfer.png)  или пункт меню "Передача". Будут показаны семь опций:

* **Скачать текущий вид** - скачать область, видимую на экране и заменить любые существующие данные *(требует  интернет-соединения)*
* **Добавить текущий вид к скачанным** - скачать видимую на экране область и объединить её с уже существующими данными *(требует интернет-соединения)*
* **Скачать другое место** - показывает форму, в которой можно выполнить поиск места или ввести его координаты напрямую, а затем скачать область в указанном местоположении *(требует интернет-соединения)*
* **Передача данных на сервер OSM** - загружает ваши \равки на сервер OpenStreetMap *(требует авторизации)* *(требует интернет-соединения)*
* **Экспорт изменений** - записать файл в формате ".osc", содержащий последние правки. Он, впоследствие, может быть считан JOSM, например
* **Прочитать из файла** - считать (J)OSM-совместимый XML-файл
* **Сохранить в файл** - сохранить в виде JOSM-совместимого XML-файла

Самый простой способ открыть карту - приблизить и центрировать место, которое хотите отредактировать, выбрать "Скачать текущее местоположение". Зуммировать можно жестами, специализированными кнопками на экране или кнопками управления громкостью телефона. Vespucci загрузит данные для области и центрирует карту на вашем текущем местоположении. Для скачивания данных авторизация не требуется.

### Редактирование

Чтобы предотвратить случайные правки Vespucci стартует в режиме "заблокирован", в котором разрешено только зуммировать и перемещать карту. Нажмите на значке ![Заблокирован](../images/locked.png), чтобы разблокировать режим правки.
 
Точки и линии окружены оранжевыми областями, которые указывают, куда приблизительно нужно прикоснуться, чтобы выделить объект. Если вы пытаетесь выделить объект, но Vespucci определяет, что выбор неоднозначен, и потенциально падает на несколько объектов, то будет показано меню с их списком. Выбранные объекты подсвечиваются жёлтым.

Если вы пытаетесь редактировать область с высокой плотностью, то разумно приблизить её побольше.

У Vespucci хорошая система отмены и повторения правок, поэтому не бойтесь экспериментировать на устройстве, однако не загружайте на сервер тестовые данные.

#### Выделение / Снятие выделения

Прикоснитесь к объекту, чтобы выделить его и подсветить. Второе прикосновение к этому же объекту откроет редактор тегов для него. Нажатие на пустой области на экране снимет выделение. Если вы выбрали объект, но требуебуется выделить что-то ещё, то просто прикоснитесь к новому объекту. Не нужно предварительно снимать имеющееся выделение. Двойное нажатие на объекте включает [Множественное выделение](Multiselect.md).

#### Добавление новой точки/узла или линии

Используйте долгое нажатие на том месте, в котором вы хотите разместить точку или начало линии. Вы увидите чёрный крестик. Повторное нажатие в этом же месте создаст точку. Прикосновение за пределами восприимчивой к нажатиям области добавит сегмент линии от начального положения к текущему. 

Просто прикоснитесь к экрану там, где хотите добавить новые точки в продолжение линии. Чтобы завершить, дважды нажмите на последней точке. Если начальная точка находится на линии, она будет включена в эту линию автоматически.

#### Перемещение точки или линии

Объекты можно перетаскивать/перемещать только, когда они выделены. Если в настройках вы выбрали большую область для перетаскивания, то вокруг выбранной точки будет увеличена область, за которую её можно перемещать, что облегчает позиционирование объекта. 

#### Улучшение геометрии линий

If you zoom in far enough you will see a small "x" in the middle of way segments that are long enough. Dragging the "x" will create a node in the way at that location. Note: to avoid accidentally creating nodes, the touch tolerance for this operation is fairly small.

#### Вырезать, копировать и вставлять

Вы можете скопировать или вырезать выделенные точки и линии, а затем вставить их один или несколько раз в новое место. Вырезание сохраняет OSM id и версию. Чтобы вставить длительно нажмите в месте, в которое вы хотите вставить (вы увидите перекрестие отмечающее расположение). Затем выберите "Вставить" из меню.

#### Эффективное добавление адресов

Vespucci has an "add address tags" function that tries to make surveying addresses more efficient. It can be selected 

* after a long press: Vespucci will add a node at the location and make a best guess at the house number and add address tags that you have been lately been using. If the node is on a building outline it will automatically add a **entrance=yes"" tag to the node. The tag editor will open for the object in question and let you make any further changes.
* in the node/way selected modes: Vespucci will add address tags as above and start the tag editor.
* in the tag editor.

House number prediction typically requires at least two house numbers on each side of the road to be entered to work, the more numbers present in the data the better.

Consider using this with the "Auto-download" mode.  

#### Добавление ограничений поворотов

Vespucci has a fast way to add turn restrictions. Note: if you need to split a way for the restriction you need to do this before starting.

* select a way with a highway tag (turn restrictions can only be added to highways, if you need to do this for other ways, please use the generic "create relation" mode, if there are no possible "via" elements the menu item will also not display)
* select "Add restriction" from the menu
* select the "via" node or way (all possible "via" elements will have the selectable element highlighting)
* select the "to" way (it is possible to double back and set the "to" element to the "from" element, Vespucci will assume that you are adding an no_u_turn restriction)
* set the restriction type in the tag menu
 
### Vespucci in "locked" mode
 
When the red lock is displayed the following all non-editing actions are available. Additionally a long press on or near to an object will display the detail information screen if it is an OSM object.

### Сохранение изменений

*(требует подключения к сети)*

Select the same button or menu item you did for the download and now select "Upload data to OSM server".

Vespucci supports OAuth authorization besides the classical username and password method. OAuth is preferable, particularly for mobile applications since it avoids sending passwords in the clear.

New Vespucci installs will have OAuth enabled by default. On your first attempt to upload modified data, a page from the OSM website loads. After you have logged on (over an encrypted connection) you will be asked to authorize Vespucci to edit using your account. Once you have done that you will be returned to Vespucci and should retry the upload, which now should succeed.

If you want to save your work and do not have Internet access, you can save to a JOSM compatible .osm file and either upload later with Vespucci or with JOSM. 

#### Resolving conflicts on uploads

Vespucci has a simple conflict resolver. However if you suspect that there are major issues with your edits, export your changes to a .osc file ("Export" menu item in the "Transfer" menu) and fix and upload them with JOSM. See the detailed help on [conflict resolution](Conflict resolution.md).  

## Использование GPS

You can use Vespucci to create a GPX track and display it on your device. Further you can display the current GPS position (set "Show location" in the GPS menu) and/or have the screen center around and follow the position (set "Follow GPS Position" in the GPS menu). 

If you have the later set, moving the screen manually or editing will cause the "follow GPS" mode to be disabled and the blue GPS arrow will change from an outline to a filled arrow. To quickly return to the "follow" mode, simply touch the arrow or re-check the option from the menu.

### Auto-Download

*(требует подключения к сети)*

If "Show location" and "Follow GPS Position" are enabled, Vespucci lets you auto download a small area (default 50m radius) around your current position. Just as above if you move the screen manually or change the geometry of an object you will have to re-enable "Follow GPS Position" when you want to continue. 

Заметки:

* you need to download an initial area manually
* the function only works below 6km/h (brisk walking speed) to avoid causing issues with the OpenStreetMap API

## Настройка Vespucci

### Settings that you might want to change

* Background layer
* Overlay layer. Adding an overlay may cause issues with older devices and such with limited memory. Default: none.
* Notes display. Open Notes will be displayed as a red filled circle, closed Notes the same in blue. Default: off.
* Photo layer. Displays georeferenced photographs as red camera icons, if direction information is available the icon will be rotated. Default: off.
* Node icons. Default: off.
* Keep screen on. Default: off.
* Large node drag area. Moving nodes on a device with touch input is problematic since your fingers will obscure the current position on the display. Turning this on will provide a large area which can be used for off-centre dragging (selection and other operations still use the normal touch tolerance area). Default: off.

#### Расширенные параметры

* Enable split action bar. On recent phones the action bar will be split in a top and bottom part, with the bottom bar containing the buttons. This typically allows more buttons to be displayed, however does use more of the screen. Turning this off will move the buttons to the top bar. note: you need to restart Vespucci for the change to take effect.
* Always show context menu. When turned on every selection process will show the context menu, turned off the menu is displayed only when no unambiguous selection can be determined. Default: off (used to be on).
* Enable light theme. On modern devices this is turned on by default. While you can enable it for older Android versions the style is likely to be inconsistent.
* Show statistics. Will show some statistics for debugging, not really useful. Default: off (used to be on).  

## Reporting Problems

If Vespucci crashes, or it detects an inconsistent state, you will be asked to send in the crash dump. Please do so if that happens, but please only once per specific situation. If you want to give further input or open an issue for a feature request or similar, please do so here: [Vespucci issue tracker](https://github.com/MarcusWolschon/osmeditor4android/issues). If you want to discuss something related to Vespucci, you can either start a discussion on the [Vespucci google group](https://groups.google.com/forum/#!forum/osmeditor4android) or on the [OpenStreetMap Android forum](http://forum.openstreetmap.org/viewforum.php?id=56)


