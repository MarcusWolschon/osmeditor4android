# Введение в Vespucci

Vespucci — полнофункциональный редактор OpenStreetMap, который поддерживает большинство операций, доступных в редакторах для ПК. Его работа успешно проверена на Google-версиях ОС Android от 2.3 до 6.0 и различных AOSP-вариантах платформы. Предупредим заранее: хотя по возможностям мобильные устройства уже "догнали" настольные компьютеры, оперативная память старых устройств невелика, и приложение обычно работает на них весьма неспешно. Помните об этом и старайтесь экономить память: например, не стоит загружать в Vespucci слишком большие области. 

## Первое использование

После запуска Vespucci показывает диалог "Скачать другое место"/"Загрузить область". Если у вас отображаются координаты, и вы желаете начать загрузку немедленно, вы можете выбрать соответствующую опцию и установить радиус загружаемой области. На медленных устройствах выбирайте небольшие участки. 

Кроме того, вы можете проигнорировать диалог, нажав на кнопку "Перейти к карте", и при помощи перетаскивания и масштабирования карты переместиться к нужному месту на ней, а затем скачать его для редактирования. (См. ниже: "Редактирование в Vespucci")

## Редактирование в Vespucci

В зависимости от размера экрана и возраста вашего устройства редактировать можно при помощи значков на верхней панели, ниспадающего меню справа на этой панели, при помощи нижней панели (если есть) или посредством кнопки меню.

### Загрузка OSM-данных

Выберите значок передачи ![](../images/menu_transfer.png)  или пункт меню "Передача". Будут показаны семь опций:

* **Download current view** - download the area visible on the screen and replace any existing data *(requires network connectivity)*
* **Add current view to download** - download the area visible on the screen and merge it with existing data *(requires network connectivity)*
* **Download other location** - shows a form that allows you to enter coordinates, search for a location or enter coordinates directly, and then download an area around that location *(requires network connectivity)*
* **Upload data to OSM server** - upload edits to OpenStreetMap *(requires authentication)* *(requires network connectivity)*
* **Auto download** - download an area around the current location automatically *(requires network connectivity)* *(requires GPS)*
* **File...** - saving and loading OSM data to/from on device files.
* **Note/Bugs** - download (automatically and manually) OSM Notes and "Bugs" from QA tools (currently OSMOSE) *(requires network connectivity)*

Самый простой способ открыть карту — приблизить и центрировать место, которое хотите отредактировать, выбрать "Скачать текущее местоположение". Масштабировать можно жестами, специализированными кнопками на экране или кнопками управления громкостью телефона. Vespucci загрузит данные для области и центрирует карту на вашем текущем местоположении. Для скачивания данных авторизация не требуется.

### Редактирование

Чтобы избежать случайных правок, Vespucci запускается в "блокированном" режиме, в котором доступно только масштабирование и перемещение карты. Нажмите значок ![Locked](../images/locked.png), чтобы отключить блокировку. Долгое нажатие на значке включит режим "Только редактировать теги" — в этом режиме вы не сможете создавать объекты или менять их геометрию. Этот режим можно распознать по отличающемуся белому значку замка.

По умолчанию вокруг доступных для редактирования точек и линий есть оранжевые области, отображающие, где нужно нажать, чтобы их выделить. Если вы пытаетесь выбрать объект, а Vespucci обнаружит, что область выделения содержит несколько объектов, откроется меню выбора. Выделенные объекты подсвечены жёлтым.

Если вы пытаетесь редактировать область с высокой плотностью данных, разумно будет приблизить её.

У Vespucci хорошая система отмены и повторения правок, поэтому не бойтесь экспериментировать на устройстве, однако не загружайте на сервер тестовые данные.

#### Выделение / Снятие выделения

Прикоснитесь к объекту, чтобы выделить его и подсветить. Второе прикосновение к этому же объекту откроет редактор тегов для него. Нажатие на пустой области на экране снимет выделение. Если вы выбрали объект, но требуется выделить что-то ещё, то просто прикоснитесь к новому объекту. Не нужно предварительно снимать имеющееся выделение. Двойное нажатие на объекте включает [Множественное выделение](Multiselect.md).

#### Добавление новой точки/узла или линии

Используйте долгое нажатие на том месте, в котором вы хотите разместить точку или начало линии. Вы увидите чёрный крестик. Повторное нажатие в этом же месте создаст точку. Прикосновение за пределами восприимчивой к нажатиям области добавит сегмент линии от начального положения к текущему. 

Просто прикоснитесь к экрану там, где хотите добавить новые точки в продолжение линии. Чтобы завершить, дважды нажмите на последней точке. Если начальная точка находится на линии, она будет включена в эту линию автоматически.

#### Перемещение точки или линии

Объекты можно перетаскивать/перемещать только, когда они выделены. Если в настройках вы выбрали большую область для перетаскивания, то вокруг выбранной точки будет увеличена область, за которую её можно перемещать, что облегчает позиционирование объекта. 

#### Улучшение геометрии линий

Если вы приблизите карту, то увидите небольшой крестик в середине сегментов линий, которые на этом масштабе достаточно велики. Потянув за крест, вы создадите в этом месте линии новую точку. Учтите: чтобы не допустить случайного создания точек, область выделения для этой операции довольно маленькая.

#### Вырезать, копировать и вставлять

Вы можете скопировать или вырезать выделенные точки и линии, а затем вставить их один или несколько раз в новое место. Вырезание сохраняет OSM id и версию. Чтобы вставить, длительно нажмите в месте, в которое вы хотите вставить (вы увидите перекрестие, отмечающее расположение). Затем выберите "Вставить" из меню.

#### Эффективное добавление адресов

В Vespucci предусмотрена функция "добавления адресных тегов", которая старается упростить разметку адресов. Её можно выбрать 

* после долгого нажатия: Vespucci добавит в этом месте точку и постарается угадать номер дома и адресные теги на основе тех, что вы недавно использовали. Если новая точка попала на контур дома, к ней по умолчанию добавится тег **entrance=yes"". Для новосозданного объекта откроется редактор тегов, где вы сможете задать прочие свойства.
* в режиме выбора точки/линии: Vespucci добавит адресные теги по тому же принципу и откроет редактор тегов.
* в самом редакторе тегов.

Для того, чтобы интерполяция номеров домов работала, обычно требуется, чтобы на каждой стороне дороги уже существовало хотя бы два номера. Чем больше номеров домов имеется в данных этой области, тем лучше.

Возможно, здесь вам поможет режим "Автоподгрузки области".  

#### Добавление ограничений поворотов

В Vespucci есть быстрый способ добавления ограничений поворотов. Заметьте: если для добавления ограничения требуется разбить линию, сделайте это заранее.

* select a way with a highway tag (turn restrictions can only be added to highways, if you need to do this for other ways, please use the generic "create relation" mode, if there are no possible "via" elements the menu item will also not display)
* select "Add restriction" from the menu
* select the "via" node or way (all possible "via" elements will have the selectable element highlighting)
* select the "to" way (it is possible to double back and set the "to" element to the "from" element, Vespucci will assume that you are adding an no_u_turn restriction)
* set the restriction type in the tag menu
 
### Vespucci in "locked" mode
 
When the red lock is displayed the following all non-editing actions are available. Additionally a long press on or near to an object will display the detail information screen if it is an OSM object.

### Сохранение изменений

*(требует подключения к сети)*

Используйте ту же кнопку или меню, которые вы выбирали для подгрузки данных, но теперь нажмите "Передача данных на сервер OSM".

Vespucci поддерживает и OAuth-авторизацию, и "классический" вход с помощью логина и пароля OSM. Лучше использовать OAuth, поскольку в этом случае вы не будете передавать свой пароль незашифрованным.

Если Vespucci был установлен недавно, OAuth-авторизация в нём уже включёна по умолчанию. При первой вашей попытке передать на сервер изменения в данных откроется страница сайта OSM. После того, как вы войдёте под своим логином (ваши данные передаются по зашифрованному соединению), вас спросят, разрешить ли Vespucci вносить в базу изменения от вашего имени. Если вы захотите разрешить OAuth-доступ к своей учётной записи заранее, до редактирования, в меню "Инструменты" есть соответствующий пункт.

Если вы хотите сохранить свои труды, а интернет-соединения поблизости нет, вы можете сохранить данные в файл формата .osm и передать его на сервер позднее с помощью Vespucci или JOSM.  

#### Разрешение конфликтов данных при передаче на сервер

Vespucci has a simple conflict resolver. However if you suspect that there are major issues with your edits, export your changes to a .osc file ("Export" menu item in the "Transfer" menu) and fix and upload them with JOSM. See the detailed help on [conflict resolution](Conflict resolution.md).  

## Использование GPS

You can use Vespucci to create a GPX track and display it on your device. Further you can display the current GPS position (set "Show location" in the GPS menu) and/or have the screen center around and follow the position (set "Follow GPS Position" in the GPS menu). 

If you have the later set, moving the screen manually or editing will cause the "follow GPS" mode to be disabled and the blue GPS arrow will change from an outline to a filled arrow. To quickly return to the "follow" mode, simply touch the arrow or re-check the option from the menu.

## Notes and Bugs

Vespucci supports downloading, commenting and closing of OSM Notes (formerly OSM Bugs) and the equivalent functionality for "Bugs" produced by the [OSMOSE quality assurance tool](http://osmose.openstreetmap.fr/en/map/). Both have to either be downloaded explicitly or you can use the auto download facility to access the items in your immediate area. Once edited or closed, you can either upload the bug or Note immediately or upload all at once.

On the map the Notes and bugs are represented by a small bug icon ![](../images/bug_open.png), green ones are closed/resolved, blue ones have been created or edited by you, and yellow indicates that it is still active and hasn't been changed. 

## Настройка Vespucci

### Что стоит изменить в настройках

* Фоновый слой
* Слой наложения. Добавление слоя наложения может вызвать проблемы на устаревших устройствах и при недостатке оперативной памяти. По умолчанию: нет.
* Отображение заметок и ошибок. "Незакрытые" заметки и ошибки будут отображаться в виже значка красного жука, "закрытые" - в виде зелёного. По умолчанию: выключено.
* Слой фото. Показывает геопривязанные фотографии в виде красных значков фотоаппарата. Если есть информация о направлении съёмки, значок будет развёрнут. По умолчанию: выключен.
* Значки точек. По умолчанию: выключены.
* Держать экран включённым. По умолчанию: выключено.
* Большая область захвата точек для перемещения. Перемещение точек на устройстве с сенсорным экраном — непростая задача, т.к. ваши пальцы будут заслонять текущее положение точки. Эта функция увеличивает область захвата, чтобы для перетягивания точки не обязательно было попадать ближе к её центру. Выделение точек и другие операции по-прежнему будут использовать общие настройки чувствительности к касаниям. По умолчанию: отключена.

#### Расширенные параметры

* Enable split action bar. On recent phones the action bar will be split in a top and bottom part, with the bottom bar containing the buttons. This typically allows more buttons to be displayed, however does use more of the screen. Turning this off will move the buttons to the top bar. note: you need to restart Vespucci for the change to take effect.
* Always show context menu. When turned on every selection process will show the context menu, turned off the menu is displayed only when no unambiguous selection can be determined. Default: off (used to be on).
* Enable light theme. On modern devices this is turned on by default. While you can enable it for older Android versions the style is likely to be inconsistent.
* Show statistics. Will show some statistics for debugging, not really useful. Default: off (used to be on).  

## Отчёты об ошибках

Если Vespucci "упал" или обнаружил, что работает нестабильно, он предложит вам выслать разработчикам отчёт о сбое программы. Пожалуйста, сделайте это, но не отсылайте отчёт по одному и тому же сбою несколько раз. Если вы хотите описать ошибку подробнее, сообщить о проблеме или предложить новую функцию, это можно сделать здесь: [Баг-трекер Vespucci](https://github.com/MarcusWolschon/osmeditor4android/issues). Если же вы хотите пообщаться с другими пользователями Vespucci, создайте новое обсуждение в [Google-группе Vespucci](https://groups.google.com/forum/#!forum/osmeditor4android) или в [Android-ветке форума OpenStreetMap](http://forum.openstreetmap.org/viewforum.php?id=56)


