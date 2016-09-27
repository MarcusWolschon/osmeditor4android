# Веспуччі - введення

Веспуччі – повнофункціональний редактор для OpenStreetMap, що виконує більшість операцій, достпних в настільних редакторах. Він був успішно протестований на версіях Android від 2.3 до 6.0 від Google, а також на різних варіантах на онснові AOSP (Android Open Source Project). Застереження: в той час як мобільні присторої наздогнали своїх настільних суперників, більш старі з них мають обмежений обсяг пам’яті і будуть доволі повільні. Ви повинні враховувати це під час роботи з Веспуччі та зберігати розмір ділянки редагування в розумних межах. 

## Початок роботи

Після запуску, Веспуччі показує діалог „Заватажити інше місце“/„Завантажити ділянку“. Якщо ви бачите координати та бажаєте завантажити дані негайно, ви можете обрати потрібний радіус навколо цієї точки, щоб завантажити дані. Не обирайте завелику ділнку на повільних пристроях. 

Або ж ви можете пропустити цей діалог, натисніть кнопку „Перейти до мапи“ та знайдить потрібне вам місце, яке ви бажаете редагувати, та завантажте дані (Див.: „Редагування у Веспуччі“).

## Редагування у Веспуччі

В залежності від розміру екрану вашого пристрою перехід в режим редагування можливий за допомогою значка зверху на панелі інструментів, через випадаюче меню на панелі інструментів праворуч, з панелі інструментів знизу (якщо є) або за допомогою кнопки меню.

### Завантаження даних OSM

Оберіть або значок ![ ](../images/menu_transfer.png) або меню "Передача". Ви побачите сім пунктів:

* **Звантажити дані для поточного місця** - завантажує дані для ділянки, що показується на екрані та замінює наявні дані *(вимагає з’єднання з мережею)*
* **Додати поточний вид для звантаження** - завантажує дані для ділянки, що показується на екрані, та об’єднує їх з наявними даними *(вимагає з’єднання з мережею)*
* **Звантажити інше місце** - показує форму з допомогою, якої можна ввести координати, шукати інше або використовувати поточне місце та завантажити дані навколо вказаного місця *(вимагає з’єднання з мережею)*
* **Надіслати дані на сервер OSM** - надсилає зміни на сервер OpenStreetMap *(потребує автентифікації)* *(вимагає з’єднання з мережею)*
* **Авто-завантаження** - завантажує ділянку навколо поточного місця автоматично *(вимагає з’єднання з мережею)* *(потрібні дані з GPS)*
* **Файл…** - збереження та завантаження даних OSM у/з фалів на присторої.
* **Нотатки/Помилки** - завантажує (автоматично або вручну) Нотатки OSM та "Помилки" з валідаторів (зараз з OSMOSE) *(вимагає з’єднання з мережею)*

Найпростіший спосіб отримати дані – це обрати відповідний масштаб та позиціювати мапу, а потім обрати "Звантажити дані для поточного місця". Ви можете змінити масштаб використовуючи жести, кнопки змінення масштабу або кнопки зміни гучності на телефоні. Веспуччі завантажить дані для ділянки та розташує мапу по центру поточного місця. Для завантаження даних на ваш пристрій, автентифікація на сервері не потрібна.

### Редагування

Для того щоб уникнути випадкових змін, Веспуччі запускається в режимі "перегляду", в якому можливе лише пересування мапою та змінення її масштабу. Натисніть на значок ![Locked](../images/locked.png) для розблокування екрану. Довге натискання на значок вмикає режим "Редагування теґів", який не дозволяє створювати нові об’єкти або змінювати геометрію об’єктів; цей режим показується троши іншим, білим значком.

Типово, точки та лінії, які можна виділити мають помаранчеві контури навколо них, які приблизно показують де ви можете торкатись екрану для виділення об’єктів. Якщо ви намагаєтесь вибрати об’єкт спроміж інших поруч, Веспуччі покаже меню для вибору потрібного об’єкта. Виділений обєкт підсвічеється жовтим кольором.

Кращє наблизитись для вибору об’єкта, якщо ви намагаєтесь редагувати ділянку з великою кількістю об’єктів.

Веспуччі має гарну систему "відміни/повтору"дій, тож не бійтеся експериментувати з вашим пристроєм, але, будь ласка, не надсилайте суто тестові дані, у разі потреби зберігайте їх локально.

#### Виділення/Зняття виділення

Доторкніться до об’єкта щоб його виділити та підсвітити, повторне торкання об’єкта відкриває редактор теґів. Торкання екрану в порожньому місці знімає виділення. Якщо у вас є виділений об’єкт і вам треба виділіти інший, просто доторкніться до потрібного об”єкта та, у разі потреби, оберіть його із запропонованого списку, знімати виділення з попереднього об’єкта не потрібно. Подвійне торкання на об’єкті перемикає в режим [Мультивиділення](../en/Multiselect.md) – виділення кількох об’єктів.

#### Додавання нової Точки або Лінії

Довге натискання на екран в потрібному місці призводить до додавання точки або початку креслення лінії. Ви побачите темний "приціл". Доторкніться до того ж самого місця знов, щоб створити нову точку, торканя екрану по за межами зони чутливості точки призведе до креслення лінії від початкової до поточної точки. 

Просто торкайтесь екрану в потрібних місцях для продовження креслення лінії. Для того, щоб закінчити її кресленя доторкніться до останьої точки двічі. Якщо кінцева точка знаходиться на лінії, вона буде також додана до неї автоматично.

#### Пересування Точки або Лінії

Об’єкти можна пересувати тільки тоді, коли вони виділені. Якщо в налаштуваннях ви  обрали "Велика ділянка навколо точок",  ви матимите велику ділянку навколо точок, що полегшить їх виділення та пересування. 

#### Покращення геометрії ліній

Якщо рівень масштабування є прийнятним, ви можете побачити на достатньо довгих відрізках ліній невеличкі символи "x". Пересування "x" призводить до створення точок на лінії в цьому місці. Примітка: щоб уникнути випадкового створення точок, рівень чутливості для цієї операції є обмеженим.

#### Вирізання, Копіювання та Вставка

Ви можете скопіювати або вирізати виділені точки та лінії та вставити їх на нове місце потрібну кількість разів. Під час вирізання версія та ідентифікатор osm зберігаються. Для вставки натисніть та потримайте деякий час потрібне місце (ви побачите "приціл"). Потім оберіть "Вставити" з меню.

#### Швидке додавання адрес

Веспуччі має функцію для "Додавання адреси" яка намагається зробити внесення арес більш зручним. Її можна викликати 

* після довгого натискання: Веспуччі доає точку та пропонує найбільш вірогідну адресу будинку та додає теґи адреси, які ви недавно використовували. Якщо точка знаходиться на контурі будівлі, до неї автоматично буде доданий теґ `entrance=yes`. Також буде відкритий редактор теґів для подальшого уточнення інформації.
* в режимі виділення точок/ліній: Веспуччі додає адресу так само, і запускає редактор теґів.
* в редакторі теґів.

Пропонування номерів будинків потребує наявності не менше двох номерів з кожного боку вулиці, чим більше є даних, тим звісно краще.

Спробуйте цю функцію в режимі "Автозавантаження".  

#### Додавання обмежень поворотів

Веспуччі дозволяє швидко додавати обмеження поворотів. Примітка: якщо вам треба розділіти лінію для створення обмеження, зробіть це перед тим як розпочати.

* виділіть лінію з теґом highway (обмеження поворотів можуть бути додані лише до доріг, якщо вам потрібно це зробити для інших ліній, будь ласка, скористайтесь загальним режимом „створення звʼязків“, якщо немає можливих елементів "via", елементи меню також не будуть показані)
* оберіть "Додати обмеження" в меню
* оберіть точку або лінію "via" (всі можливі елементи для "via" будуть доступні для виділення та мати відповідне підсвічування)
* оберіть лінію "to" (можливо обрати для цієї ролі лінію "from", Веспуччі зрозуміє, що це заборона розвороту)
* встановіть тип обмеження в меню

### Веспуччі в режимі "перегляду"

Коли показується червоний замок, вам доступні дії не повʼязані з редагуванням. Довге торкання до обʼєкта дозволяє побачити інформацію про нього, якщо він є обʼєктом ОСМ.

### Збереження вашіх змін

*(потрібне зʼєднання з мережею)*

Select the same button or menu item you did for the download and now select "Upload data to OSM server".

Vespucci supports OAuth authorization and the classical username and password method. OAuth is preferable since it avoids sending passwords in the clear.

New Vespucci installs will have OAuth enabled by default. On your first attempt to upload modified data, a page from the OSM website loads. After you have logged on (over an encrypted connection) you will be asked to authorize Vespucci to edit using your account. If you want to or need to authorize the OAuth access to your account before editing there is a corresponding item in the "Tools" menu.

If you want to save your work and do not have Internet access, you can save to a JOSM compatible .osm file and either upload later with Vespucci or with JOSM. 

#### Resolving conflicts on uploads

Vespucci has a simple conflict resolver. However if you suspect that there are major issues with your edits, export your changes to a .osc file ("Export" menu item in the "Transfer" menu) and fix and upload them with JOSM. See the detailed help on [conflict resolution](../en/Conflict resolution.md).  

## Using GPS

You can use Vespucci to create a GPX track and display it on your device. Further you can display the current GPS position (set "Show location" in the GPS menu) and/or have the screen center around and follow the position (set "Follow GPS Position" in the GPS menu). 

If you have the later set, moving the screen manually or editing will cause the "follow GPS" mode to be disabled and the blue GPS arrow will change from an outline to a filled arrow. To quickly return to the "follow" mode, simply touch the arrow or re-check the option from the menu.

## Notes and Bugs

Vespucci supports downloading, commenting and closing of OSM Notes (formerly OSM Bugs) and the equivalent functionality for "Bugs" produced by the [OSMOSE quality assurance tool](http://osmose.openstreetmap.fr/en/map/). Both have to either be downloaded explicitly or you can use the auto download facility to access the items in your immediate area. Once edited or closed, you can either upload the bug or Note immediately or upload all at once.

On the map the Notes and bugs are represented by a small bug icon ![ ](../images/bug_open.png), green ones are closed/resolved, blue ones have been created or edited by you, and yellow indicates that it is still active and hasn't been changed. 

The OSMOSE bug display will provide a link to the affected object in blue, touching the link will select the object, center the screen on it and down load the area beforehand if necessary. 

## Customizing Vespucci

### Settings that you might want to change

* Background layer
* Overlay layer. Adding an overlay may cause issues with older devices and such with limited memory. Default: none.
* Notes/Bugs display. Open Notes and bugs will be displayed as a yellow bug icon, closed ones the same in green. Default: on.
* Photo layer. Displays georeferenced photographs as red camera icons, if direction information is available the icon will be rotated. Default: off.
* Node icons. Default: on.
* Keep screen on. Default: off.
* Large node drag area. Moving nodes on a device with touch input is problematic since your fingers will obscure the current position on the display. Turning this on will provide a large area which can be used for off-centre dragging (selection and other operations still use the normal touch tolerance area). Default: off.

#### Advanced preferences

* Enable split action bar. On recent phones the action bar will be split in a top and bottom part, with the bottom bar containing the buttons. This typically allows more buttons to be displayed, however does use more of the screen. Turning this off will move the buttons to the top bar. note: you need to restart Vespucci for the change to take effect.
* Always show context menu. When turned on every selection process will show the context menu, turned off the menu is displayed only when no unambiguous selection can be determined. Default: off (used to be on).
* Enable light theme. On modern devices this is turned on by default. While you can enable it for older Android versions the style is likely to be inconsistent.
* Show statistics. Will show some statistics for debugging, not really useful. Default: off (used to be on).  

## Reporting Problems

If Vespucci crashes, or it detects an inconsistent state, you will be asked to send in the crash dump. Please do so if that happens, but please only once per specific situation. If you want to give further input or open an issue for a feature request or similar, please do so here: [Vespucci issue tracker](https://github.com/MarcusWolschon/osmeditor4android/issues). If you want to discuss something related to Vespucci, you can either start a discussion on the [Vespucci google group](https://groups.google.com/forum/#!forum/osmeditor4android) or on the [OpenStreetMap Android forum](http://forum.openstreetmap.org/viewforum.php?id=56)


