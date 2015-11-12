# Веспуччі - введення

Веспуччі – повнофункціональний редактор для OpenStreetMap, що виконує більшість операцій, достпних в настільних редакторах. Він був успішно протестований на версіях Android від 2.3 до 6.0 від Google, а також на різних варіантах на онснові AOSP (Android Open Source Project). Застереження: в той час як мобільні присторої наздогнали своїх настільних суперників, більш старі з них мають обмежений обсяг пам’яті і будуть доволі повільні. Ви повинні враховувати це під час роботи з Веспуччі та зберігати розмір ділянки редагування в розумних межах. 

## Початок роботи

Після запуску, Веспуччі показує діалог „Заватажити інше місце“/„Завантажити ділянку“. Якщо ви бачите координати та бажаєте завантажити дані негайно, ви можете обрати потрібний радіус навколо цієї точки, щоб завантажити дані. Не обирайте завелику ділнку на повільних пристроях. 

Або ж ви можете пропустити цей діалог, натисніть кнопку „Перейти до мапи“ та знайдить потрібне вам місце, яке ви бажаете редагувати, та завантажте дані (Див.: „Редагування у Веспуччі“).

## Редагування у Веспуччі

В залежності від розміру екрану вашого пристрою перехід в режим редагування можливий за допомогою значка зверху на панелі інструментів, через випадаюче меню на панелі інструментів праворуч, з панелі інструментів знизу (якщо є) або за допомогою кнопки меню.

### Завантаження даних OSM

Оберіть або значок ![](../images/menu_transfer.png) або меню "Передача". Ви побачите сім пунктів:

* **Звантажити дані для поточного місця** - завантаження даних для місцевості, яка показується на екрані, с заміною потоних даних *(вимагає з’єднання з мережею)*
* **Додати поточний вид для звантаження** - завантаження даних для місцевості, яка показується на екрані,  та їх об’єднання з поточними даними  *(вимагає з’єднання з мережею)*
* **Звантажити інше місце* - показує форму, яка дозволяє зазначити координати, здійснити і потім завантажити це місце та територію навколо*(вимагає з’єднання з мережею)*
* **Надіслати дані на сервер OSM* - надсилає зміни до OpenStreetMap *(вимагає автентифікації)* *(вимагає з’єднання з мережею)*
* **Авто-завантаження** - завантажує ділянку навколо поточного місця автоматично *(вимагає з’єднання з мережею)* *(потрібні дані з GPS)*
* **Файл…** - збереження та заватаження даних OSM у/з фалів на присторої.
* **Нотатки/Помилки** - завантажує (автоматично або вручну) Нотаток OSM та "Помилок" з валідаторів (зараз з OSMOSE) *(вимагає з’єднання з мережею)*

Найпростіший спосіб отрвматв дані – це обрати відповідний масштаб та позиціювати мапу, а потім обрати "Звантажити дані для поточного місця". Ви можете змінити масштаб використовуючи жести, кнопки змінення масштабу або кнопки зміни гучності на телефоні. Веспуччі завантажить дані для ділянки та розташує мапу по центру поточного місця. Для завантаження даних нп ваш пристрій автентифікація на сервері не потрібна.

### Редагування

Для того щоб уникнути випадкових змін, Веспуччі запускається в режимі "перегляду", в якому можливе лише пересування мапою та змінення її масштабу. Натисніть на значок ![Locked](../images/locked.png) для розблокування екрану. Довге натискання на значок вмикає режим "Редагування теґів", який не дозволяє створювати нові об’єкти або змінювати геометрію об’єктів; цей режим показується троши іншим, білим значком.

Типово, точки та лінії, які можна виділити мають помаранчеві контури навколо них, які приблизно показують де ви можете торкатись екрану для виділення об’єктів. Якщо ви намагаєтесь вибрати об’єкт спроміж інших поруч, Веспуччі покаже меню для вибору потрібного об’єкта. Виділений обєкт підсвічеється жовтим кольором.

Кращє наблизитись для вибору об’єкта, якщо ви намагаєтесь редагувати ділянку з великою кількістю об’єктів.

Веспуччі має гарну систему "відміни/повтору"дій, тож не бійтеся експериментувати з вашим пристроєм, але, будь ласка, не надсилайте суто тестові дані, у разі потреби зберігайте їх локально.

#### Selecting / De-selecting

Touch an object to select and highlight it, a second touch on the same object opens the tag editor on the element. Touching the screen in an empty region will de-select. If you have selected an object and you need to select something else, simply touch the object in question, there is no need to de-select first. A double tap on an object will start [Multiselect mode](Multiselect.md).

#### Adding a new Node/Point or Way

Long press where you want the node to be or the way to start. You will see a black "cross hairs" symbol. Touching the same location again creates a new node, touching a location outside of the touch tolerance zone will add a way segment from the original position to the current position. 

Simply touch the screen where you want to add further nodes of the way. To finish, touch the final node twice. If the initial node is located on a way, the node will be inserted into the way automatically.

#### Moving a Node or Way

Objects can be dragged/moved only when they are selected. If you select the large drag area in the preferences, you get a large area around the selected node that makes it easier to position the object. 

#### Improving Way Geometry

If you zoom in far enough you will see a small "x" in the middle of way segments that are long enough. Dragging the "x" will create a node in the way at that location. Note: to avoid accidentally creating nodes, the touch tolerance for this operation is fairly small.

#### Cut, Copy & Paste

You can copy or cut selected nodes and ways, and then paste once or multiple times to a new location. Cutting will retain the osm id and version. To paste long press the location you want to paste to (you will see a cross hair marking the location). Then select "Paste" from the menu.

#### Efficiently Adding Addresses

Vespucci has an "add address tags" function that tries to make surveying addresses more efficient. It can be selected 

* after a long press: Vespucci will add a node at the location and make a best guess at the house number and add address tags that you have been lately been using. If the node is on a building outline it will automatically add a **entrance=yes"" tag to the node. The tag editor will open for the object in question and let you make any further changes.
* in the node/way selected modes: Vespucci will add address tags as above and start the tag editor.
* in the tag editor.

House number prediction typically requires at least two house numbers on each side of the road to be entered to work, the more numbers present in the data the better.

Consider using this with the "Auto-download" mode.  

#### Adding Turn Restrictions

Vespucci has a fast way to add turn restrictions. Note: if you need to split a way for the restriction you need to do this before starting.

* select a way with a highway tag (turn restrictions can only be added to highways, if you need to do this for other ways, please use the generic "create relation" mode, if there are no possible "via" elements the menu item will also not display)
* select "Add restriction" from the menu
* select the "via" node or way (all possible "via" elements will have the selectable element highlighting)
* select the "to" way (it is possible to double back and set the "to" element to the "from" element, Vespucci will assume that you are adding an no_u_turn restriction)
* set the restriction type in the tag menu
 
### Vespucci in "locked" mode
 
When the red lock is displayed the following all non-editing actions are available. Additionally a long press on or near to an object will display the detail information screen if it is an OSM object.

### Saving Your Changes

*(requires network connectivity)*

Select the same button or menu item you did for the download and now select "Upload data to OSM server".

Vespucci supports OAuth authorization and the classical username and password method. OAuth is preferable since it avoids sending passwords in the clear.

New Vespucci installs will have OAuth enabled by default. On your first attempt to upload modified data, a page from the OSM website loads. After you have logged on (over an encrypted connection) you will be asked to authorize Vespucci to edit using your account. If you want to or need to authorize the OAuth access to your account before editing there is a corresponding item in the "Tools" menu.

If you want to save your work and do not have Internet access, you can save to a JOSM compatible .osm file and either upload later with Vespucci or with JOSM. 

#### Resolving conflicts on uploads

Vespucci has a simple conflict resolver. However if you suspect that there are major issues with your edits, export your changes to a .osc file ("Export" menu item in the "Transfer" menu) and fix and upload them with JOSM. See the detailed help on [conflict resolution](Conflict resolution.md).  

## Using GPS

You can use Vespucci to create a GPX track and display it on your device. Further you can display the current GPS position (set "Show location" in the GPS menu) and/or have the screen center around and follow the position (set "Follow GPS Position" in the GPS menu). 

If you have the later set, moving the screen manually or editing will cause the "follow GPS" mode to be disabled and the blue GPS arrow will change from an outline to a filled arrow. To quickly return to the "follow" mode, simply touch the arrow or re-check the option from the menu.

## Notes and Bugs

Vespucci supports downloading, commenting and closing of OSM Notes (formerly OSM Bugs) and the equivalent functionality for "Bugs" produced by the [OSMOSE quality assurance tool](http://osmose.openstreetmap.fr/en/map/). Both have to either be downloaded explicitly or you can use the auto download facility to access the items in your immediate area. Once edited or closed, you can either upload the bug or Note immediately or upload all at once.

On the map the Notes and bugs are represented by a small bug icon ![](../images/bug_open.png), green ones are closed/resolved, blue ones have been created or edited by you, and yellow indicates that it is still active and hasn't been changed. 

## Customizing Vespucci

### Settings that you might want to change

* Background layer
* Overlay layer. Adding an overlay may cause issues with older devices and such with limited memory. Default: none.
* Notes/Bugs display. Open Notes and bugs will be displayed as a red bug icon, closed ones the same in green. Default: off.
* Photo layer. Displays georeferenced photographs as red camera icons, if direction information is available the icon will be rotated. Default: off.
* Node icons. Default: off.
* Keep screen on. Default: off.
* Large node drag area. Moving nodes on a device with touch input is problematic since your fingers will obscure the current position on the display. Turning this on will provide a large area which can be used for off-centre dragging (selection and other operations still use the normal touch tolerance area). Default: off.

#### Advanced preferences

* Enable split action bar. On recent phones the action bar will be split in a top and bottom part, with the bottom bar containing the buttons. This typically allows more buttons to be displayed, however does use more of the screen. Turning this off will move the buttons to the top bar. note: you need to restart Vespucci for the change to take effect.
* Always show context menu. When turned on every selection process will show the context menu, turned off the menu is displayed only when no unambiguous selection can be determined. Default: off (used to be on).
* Enable light theme. On modern devices this is turned on by default. While you can enable it for older Android versions the style is likely to be inconsistent.
* Show statistics. Will show some statistics for debugging, not really useful. Default: off (used to be on).  

## Reporting Problems

If Vespucci crashes, or it detects an inconsistent state, you will be asked to send in the crash dump. Please do so if that happens, but please only once per specific situation. If you want to give further input or open an issue for a feature request or similar, please do so here: [Vespucci issue tracker](https://github.com/MarcusWolschon/osmeditor4android/issues). If you want to discuss something related to Vespucci, you can either start a discussion on the [Vespucci google group](https://groups.google.com/forum/#!forum/osmeditor4android) or on the [OpenStreetMap Android forum](http://forum.openstreetmap.org/viewforum.php?id=56)


