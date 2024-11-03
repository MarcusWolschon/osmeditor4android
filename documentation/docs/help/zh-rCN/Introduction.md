_Before we start: most screens have links in the menu to the on-device help system giving you direct access to information relevant for the current context, you can easily navigate back to this text too. If you have a larger device, for example a tablet, you can open the help system in a separate split window.  All the help texts and more (FAQs, tutorials) can be found on the [Vespucci documentation site](https://vespucci.io/) too. You can further start the help viewer directly on devices that support short cuts with a long press on the app icon and selecting "Help"_

# Vespucci 介绍

Vespucci is a full featured OpenStreetMap editor that supports most operations that desktop editors provide. It has been tested successfully on Google's Android 2.3 to 14.0 (versions prior to 4.1 are no longer supported) and various AOSP based variants. A word of caution: while mobile device capabilities have caught up with their desktop rivals, particularly older devices have very limited memory available and tend to be rather slow. You should take this in to account when using Vespucci and keep, for example, the areas you are editing to a reasonable size.

## 使用 Vespucci 编辑

根据屏幕大小和设备的新旧，编辑操作可以使用顶部栏的图标、顶部栏右侧的下拉菜单、底部栏（如果有的话）或通过菜单进行。

<a id="download"></a>

### 下载 OSM 数据

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

下载数据到设备，最简单方法是拉到要编辑的地方，然后选择“下载当前视图”。 缩放的话，可以使用两只手指、缩放按钮或音量键。 Vespucci 会下载当前看到这片区域的数据。 下载数据不需要身份验证。

In unlocked state any non-downloaded areas will be dimmed relative to the downloaded ones if you are zoomed in far enough to enable editing. This is to avoid inadvertently adding duplicate objects in areas that are not being displayed. In the locked state dimming is disabled, this behaviour can be changed in the [Advanced preferences](Advanced%20preferences.md) so that dimming is always active.

If you need to use a non-standard OSM API entry, or use [offline data](https://vespucci.io/tutorials/offline/) in _MapSplit_ format you can add or change entries via the _Configure..._ entry for the data layer in the layer control.

### 编辑

<a id="lock"></a>

#### 锁定、解锁，模式切换

为避免手滑，Vespucci 启动的时候是“锁定”模式，该模式仅允许缩放和移动地图。 点击 ![Locked](../images/locked.png) 锁定图标解锁编辑。 

A long press on the lock icon or the _Modes_ menu in the map display overflow menu will display a menu offering 4 options:

* **Normal** - the default editing mode, new objects can be added, existing ones edited, moved and removed. Simple white lock icon displayed.
* **Tag only** - selecting an existing object will start the Property Editor, new objects can be added via the green "+" button, or long press, but no other geometry operations are enabled. White lock icon with a "T" is displayed.
* **Address** - enables Address mode, a slightly simplified mode with specific actions available from the [Simple mode](../en/Simple%20actions.md) "+" button. White lock icon with an "A" is displayed.
* **Indoor** - enables Indoor mode, see [Indoor mode](#indoor). White lock icon with an "I" is displayed.
* **C-Mode** - enables C-Mode, only objects that have a warning flag set will be displayed, see [C-Mode](#c-mode). White lock icon with a "C" is displayed.

If you are using Vespucci on an Android device that supports short cuts (long press on the app icon) you can start directly to _Address_ and _Indoor_ mode.

#### 单击，双击和长按

默认情况下，可选择的节点和路径周围有一个橙色区域，大致指示出一个范围，点击这个范围都可以选择到这个物件。三个选择：

* 单击：选择物件。
    * 一个单独的节点或路径会高亮显示。
    * 但是，如果你尝试选择一个物件，而这个范围里有多个物件，软件会显示一个菜单，你再从菜单里选择那个物件。
    * 所选物件以黄色高亮显示。
    * 选了之后，根据你选的东西，请参阅[已选择节点](Node%20selected.md)、[已选择路径](Way%20selected.md)以及[已选择关系](Relation%20selected.md)。
* 双击：启动[多选模式](Multiselect.md)
* 长按：创建一个“十字准线”，使您能够添加节点（见下文）和[创建新物件](Creating%20new%20objects.md)。这仅在“简单模式”被禁用时才可以用。

如果你要编辑的地方很密，放大地图来编辑，或者换大屏幕。

Vespucci 有撤销和重做的功能（类似电脑上的 Ctrl + Z 和 Ctrl + Y），编辑的时候尽管随便尝试，但不要把你练习的数据 po 上网。

#### 选择与取消选择（单击与“选择菜单”）

点了一个物件，它高亮显示了，这个时候是“选择”，再点一下物件以外的空白区域，那就是“取消选择”。 如果要多选物件，先点一个物件，然后双击这个物件，就能连续点后面的物件了。双击一个物件将启动[多选模式](Multiselect.md)。

请注意，如果你尝试选择一个物件，而这个范围里有多个物件（例如路径上的一个节点或其他重叠对象），软件会显示一个菜单，你再从菜单里选择你要的那个物件。 

你选了一个物件，它周围会有细黄色边框。这个黄色边框不一定看得清，要看你的地图背景和放得够不够大。 选择了之后，会弹出一个通知，你要确认选择。

选完了，会有一个这个物件可以用的操作列表（以按钮或菜单项的形式）：有关详细信息，请参阅[已选择节点](Node%20selected.md)、[已选择路径](Way%20selected.md)以及[已选择关系](Relation%20selected.md)。

#### 选择对象：编辑标签

选择了一个物件，再点它一次，会打开标签编辑器，可以编辑与该物件关联的标签。

请注意，对于重叠的物件（例如路径上的节点），再点它一次，又会出现菜单，这个时候你要再选一次这个物件，才会打开标签编辑器。如果你在第二次出现菜单的时候，选择了另一个对象，那就变成是首次选中另一个对象，不会打开另一个对象的标签编辑器。

#### 选择物件：移动节点或路径

如果选择了一个物件，它就可以被移动。 请注意，物件只有在被选择时，才能移动。你不用很精准按住，只需在所选物件附近（以其为中心，一个圆形区域都是拖动区域）拖动屏幕，即可移动物件的位置。在[首选项](Preferences.md)中调大拖动区域，所选节点周围的可拖动区域就会变大，更容易定位物件。 

#### 添加新节点或路径 

首次以“简单模式”启动应用程序时，可以在主菜单取消选中相应的复选框进行更改。

##### 简单模式

按住地图画面上的绿色大浮动按钮，会弹出一个菜单。 选择其中一项后，再点一下地图上对应的位置即可创建物件。在点击之前，不会影响你调整地图视图、平移和缩放地图。 

See [Creating new objects in simple actions mode](Simple%20actions.md) for more information. Simple mode os the default for new installs.

##### 高级（长按）模式
 
长按你希望节点所在的位置或路径开始的位置。 屏幕上会出现一个黑色的“十字准线”符号，十字准线出现之后，手不要放开，可以拖到你要瞄准的地方。这个时候可以松开手。
* 如果要创建新节点（不连接到物件），长按的时候远离现有物件，在空的地方按。
* 如果要延长路径，请在路径的“容差区”内（或路径的节点）触摸，这个容差区显示在节点或路径周围。

看到十字准线符号后，有几个选项：

* _点一下十字准线瞄准的地方（大概就行，会有一个容差区），会有两种情况。_
    * 1. 如果十字准线不在节点附近，会创建一个新节点。 如果在路径附近（但不在节点附近），则会给这个路径增加一个新节点。
    * 2. 如果十字准线在节点附近（在节点的容差范围内），会打开这个节点的标签编辑器，不会创建新节点。
* _点一下十字准线没有瞄准的地方。_ 在十字准线的容差区之外，会添加一条从瞄准的位置到你点的位置的路径。 如果十字准线靠近路径或节点，一个新片段会连接到路径或节点。

你可以连续点屏幕，来一直添加路径的节点。添加完了，请双击最后一个节点。如果最后一个节点位于已有的路径或节点上，那就不用双击了，这个片段会自动连接到已有的路径或节点。 

你也可以使用菜单项目：请参阅[创建新物件](Creating%20new%20objects.md)了解更多信息。

#### 添加区域

与其他地理数据系统不同，OpenStreetMap 目前没有“区域”这种物件类型。 网页编辑器“iD”它是从底层 OSM 元素中制造一个抽象的区域，这在某些情况下效果很好，在其他情况下则不然。 Vespucci 不打算这样子搞，所以你需要了解一下区域的表示方式：

* _闭合路径（多边形）_：这是最简单和最常见的区域变体，它是一个路径，但它第一个和最后一个节点是在一起的，形成一个封闭的“环”（大多数建筑物都是这种类型）。这样的闭环很容易在 Vespucci 中创建，画完之后，只需连接回第一个节点。注意：一个封闭路径它具体是什么，看的是它的标签：例如，如果这个闭合路径的标签是建筑物，那它是一个区域；如果它的标签是转圈圈的路口，那就不是区域了。在某些情况下，这两种例子会同时生效，可以用“area”（区域）标签来区分。
* _多重多边形_：某些区域有多个部分、孔、环，它不是一条路径就能画出来的。OSM 用一种特别类型的关系（多个物件可以将它们关联起来，形成一个组）来解决这个问题，即多重多边形。多重多边形可以有多个“外”环和多个“内”环（想象一个奥运五环形状的建筑物），每个环都是一个单独的闭合路径，也可以是具有公共节点的多个单独的路径（蜘蛛一样）。复杂的多边形搞不定，但这个小小的手机软件还是能应付简单的多边形的。 
* _海岸线_：对于非常大的物体、大陆和岛屿，即使是多重多边形也很难搞定。对于使用 natural=coastline（自然=海岸线）标签的路径，我们假设它是一条依赖方向的片段，意思就是，陆地在路径的左侧，水面在路径的右侧。如此，一个大岛屿的海岸线就能分成多个不是闭合的路径了，这些非闭合的路径，最终又能头尾相接起来，而稍作修改的时候，只需要处理眼前的一小段路径，不需要整个环形海岸线都下载下来。不过这样会有个问题，绝大多数情况，不要去把作为海岸线的路径反转，不要把陆地和水面换位置。与此有关的更多信息可以在[OpenStreetMap 的官方 wiki](http://wiki.openstreetmap.org/wiki/Tag:natural%3Dcoastline) 上找到。

#### 改善路径的几何形状

选择一条路径之后，如果你放大得足够大，可以看到路径中间有个小叉。拖动那个小叉就能在小叉的位置增加一个节点。这样子就能很方便让一条路径的形状变得更加正确。注意了，为避免平时意外创建节点，那个小叉能点到的区域很小，你没那么容易按到。

#### 剪切、复制和粘贴

You can copy selected nodes and ways, and then paste once or multiple times to a new location. Cutting will retain the osm id and version, thus can only be pasted once. To paste long press the location you want to paste to (you will see a cross hair marking the location). Then select "Paste" from the menu.

#### 高效地添加地址

Vespucci 能高效地预测门牌号（街道左右两侧是单独的，不会联合预测），并根据上次使用的值以及它们有多近，自动添加 _addr:street_ 或者 _addr:place_ 的标签，通过这个功能可以提高测量地址的效率。在最好的情况下，不用打字就能添加地址。   

长按![Address](../images/address.png)即可触发添加标签的功能： 

* 长按后（仅限非简单模式）：Vespucci 将在该位置添加一个节点，并对门牌号进行最佳猜测，并添加您最近使用的地址标签。 如果节点位于建筑物轮廓上，它将自动向节点添加“entrance=yes”（入口=是）标签。 标签编辑器会打开，可以进一步更改。
* 在节点及路径选择模式下：Vespucci 会像上面一样添加地址标签并启动标签编辑器。
* 在属性编辑器中。

直接添加单个地址节点，可以从默认的“简单模式”切换到“地址”编辑模式（长按锁定按钮）。“添加地址节点”将在该位置添加一个地址节点。如上所述，如果它在建筑轮廓上，则会添加入口标签。

门牌号预测的功能，通常需要在道路的两个单侧各输入至少两个门牌号才能生效，能输入的门牌号越多则越好。这段的意思是，比如一条街，你站在街头（相对街尾），左手门牌 001，右手门牌 002，整条街 100 个门，左手全是单数，右手全是双数。没有要求你准确找到街头街尾的位置，但是你至少找到单数侧任意两个门牌的准确位置以及双数侧任意两个门牌的准确位置（同侧门牌越远越好）。比如说你找到 15 号和 65 号，软件就能自动等距补齐 001 到 015 的门牌号、015 至 065、065 至 099。但因为是等距，肯定是不准确的，如果之后你又能发现 085 的准确位置，那么 085 附近的就能更准确了。这个功能意义在于，你不用重复登记一百次门牌号，你只需要等差数列去登记，让自动补齐的门牌基本对应实际，你就可以收工了。

用这个功能的时候，你最好打开[自动下载](#download)模式。  

#### 添加转向限制

Vespucci 有一种快速添加转弯限制的方法。如有必要，它会自动拆分路径，并要求你重新选择元素。转弯限制，就是某个路口不允许右转的这种情况。 

* 选择带有“highway”（高速公路）标签的路径（转弯限制只能添加到高速公路，如果你需要为其他路径添加限制，请使用一般的“创建关系”模式）
* 从菜单中选择“添加限制”
* 选择“via”（途径）标签节点或路径（只有可能的“via”（途径）元素才会显示触摸区域）
* 选择“to”（到）标签路径（可能要返回两次，并将“to”（到）元素设置为“from”（从）元素，Vespucci 将假定你正在添加 no_u_turn（不允许调头）限制）
* 设置限制类型

### Vespucci "锁定" 模式

当显示红色锁定标志时，不能编辑。 这时长按 OpenStreetMap 物件或周围，将显示其详细信息。

### 保存您的变更

“（需要网络连接）”

下载数据的时候点了哪些菜单，现在就怎么点，然后选择“将数据上传到 OSM 服务器”。

Vespucci supports OAuth 2, OAuth 1.0a authorization and the classical username and password method. Since July 1st 2024 the standard OpenStreetMap API only supports OAuth 2 and other methods are only available on private installations of the API or other projects that have repurposed OSM software.  

Authorizing Vespucci to access your account on your behalf requires you to one time login with your display name and password. If your Vespucci install isn't authorized when you attempt to upload modified data you will be asked to login to the OSM website (over an encrypted connection). After you have logged on you will be asked to authorize Vespucci to edit using your account. If you want to or need to authorize the OAuth access to your account before editing there is a corresponding item in the "Tools" menu.

如果要保存作业，但这时不能上网，可以保存到 .osm 文件中，这种文件格式与 JOSM 兼容。有网之后，可以使用 Vespucci 或 JOSM 上传。 

#### 上传时解决冲突

Vespucci 有一个简单的冲突解决器。 但如果你怀疑你的编辑存在重大问题，请将你的更改导出到 .osc 文件（“传输”菜单里有个“导出”），并在电脑上使用 JOSM 修复，然后上传。 请参阅[冲突解决](Conflict%20resolution.md)的详细帮助。  

### Nearby point-of-interest display

A nearby point-of-interest display can be shown by pulling the handle in the middle and top of the bottom menu bar up. 

More information on this and other available functionality on the main display can be found here [Main map display](Main%20map%display.md).

## 使用 GPS 与 GPX 轨迹

在标准设置下，Vespucci 将尝试启用设备上的 GPS（以及其他基于卫星的导航系统）。如果不行，则会用基站位置来定位。 此行为假定您在正常使用中将您的 Android 设备本身配置为仅使用 GPX 生成的位置（以避免跟踪），即您已关闭委婉命名的“提高位置准确性”选项。 如果您想启用该选项但又想避免 Vespucci 回退到“网络位置”，您应该关闭[高级首选项](Advanced%20preferences.md)中的相应选项。（太长不看版：这段可以跳过不看的，第二句开始就谷歌翻译了，我完全没理解什么意思，又想关 GPS 避免政府跟踪，又想软件不要只用模糊定位？咋那么矫情呢？） 

Touching the ![GPS](../images/menu_gps.png) button (normally on the left hand side of the map display) will center the screen on the current position and as you move the map display will be panned to maintain this.  Moving the screen manually or editing will cause the "follow GPS" mode to be disabled and the blue GPS arrow will change from an outline to a filled arrow. To quickly return to the "follow" mode, simply touch GPS button or re-check the equivalent menu option. If the device doesn't have a current location the location marker/arrow will be displayed in black, if a current location is available the marker will be blue.

要记录 GPX 轨迹，并将其显示在您的设备上，请选择 ![GPS](../images/menu_gps.png) 菜单中的“开始 GPX 轨迹”项。 当前已记录的轨迹会作为一个图层显示出来，你可以从[图层控制]（Main％20map％20display.md）中上传和导出轨迹。 可以将本地 GPX 文件以及从 OSM API 下载的轨迹添加为图层。

注意：默认情况下，Vespucci 不会把高程数据记录进 GPX 轨迹，这是由于某些 Android 特定问题造成的。要启用高程记录，请安装重力模型，或者更简单地，转到 [高级首选项](Advanced%20preferences.md) 并配置 NMEA 输入。（感觉像甩锅，隔壁 OpenCamera 都能在录像中持续记录海拔高度，虽然安卓手机的 GPS　海拔高度误差大得几乎不可用就是了）

### How to export a GPX track?

Open the layer menu, then click the 3-dots menu next to "GPX recording", then select **Export GPX track...**. Choose in which folder to export the track, then give it a name suffixed with `.gpx` (example: MyTrack.gpx).

## Notes, Bugs and Todos

Vespucci 支持下载、评论和关闭 OSM 注释（以前称为 OSM bugs）以及由 [OSMOSE 质量保证工具](http://osmose.openstreetmap.fr/en/map/) 生成的“错误”。这两样都能完整下载，或者你可以使用自动下载工具访问你附近区域中的项目。编辑或关闭后，你可以立即上传错误或注释，也可以一次上传所有内容。 

Further we support "Todos" that can either be created from OSM elements, from a GeoJSON layer, or externally to Vespucci. These provide a convenient way to keep track of work that you want to complete. 

On the map the Notes and bugs are represented by a small bug icon ![Bug](../images/bug_open.png), green ones are closed/resolved, blue ones have been created or edited by you, and yellow indicates that it is still active and hasn't been changed. Todos use a yellow checkbox icon.

The OSMOSE bug and Todos display will provide a link to the affected element in blue (in the case of Todos only if an OSM element is associated with it), touching the link will select the object, center the screen on it and down load the area beforehand if necessary. 

### 筛选

除了全局启用注释和错误显示之外，你还可以设置过滤的程度以减少混乱。过滤器配置可以从任务图层的[图层控制](#layers)里面去配置：

* Notes
* Osmose error
* Osmose warning
* Osmose minor issue
* Maproulette
* Todo

<a id="indoor"></a>

## 室内模式

在室内画地图很难，因为经常会有很多物件重叠。Vespucci 有一个专用的室内模式，可以过滤出只在同一级别的物件，并自动把当前楼层添加到新创建的物件属性里。

该模式可以通过长按锁定按钮来启用，参见[锁定、解锁、模式切换](#lock)并选择相应的菜单项。

<a id="c-mode"></a>

## C 模式

在 C 模式下，仅显示设置了警告标志的物件，很容易发现具有特定问题或匹配可配置检查的物件。 如果选择了一个物件，并且在 C 模式下启动了属性编辑器，则将自动应用最佳匹配预设。

该模式可以通过长按锁定按钮来启用，参见[锁定、解锁、模式切换](#lock)并选择相应的菜单项。

### 设置检查

All validations can be disabled/enabled in the "Validator settings/Enabled validations" in the [preferences](Preferences.md). 

The configuration for "Re-survey" entries allows you to set a time after which a tag combination should be re-surveyed. "Check" entries are tags that should be present on objects as determined by matching presets. Entries can be edited by clicking them, the green menu button allows adding of entries.

#### 重新调查条目

重新调查含有以下属性的条目：

* **Key（关键字）** - 感兴趣的标签的关键字。
* **Value（值）** - 感兴趣的标签应该有的值，如果为空，标签值将被忽略。
* **Age（年龄）** - 元素最后一次更改后过了多少天，如果存在将使用的 _check_date_ 标签，则应重新调查元素，否则创建当前版本的日期。 将值设置为零将导致检查仅匹配关键字和值。
* **正则表达式** - 如果选中 **Value**　则假定为 JAVA 正则表达式。

**Key** 和 **Value** 会根据相关对象的 _existing_ 标签进行检查。

标准预设中的 _Annotations_ 组里面包含了一个项目，这个项目可以自动添加带有当前日期的 _check_date_ 的标签。

#### 检查条目

检查条目具有以下两个属性：

* **关键字** - 根据已匹配上的预设，关键字应该存在于物件上。
* **必选项** - 即使关键字在已匹配上预设的可选标签中，也需要关键字。

此检查首先确定已匹配上的预设，然后根据预设检查 **关键字** 是否是此物件的“推荐”关键字，**必选项** 将检查扩展到物件的*选项*。注意：当前链接的预设未检查。

## 筛选

### 基于标签的过滤器

过滤器可以从主菜单启用，然后点击过滤器图标修改过滤的配置。更多文档可以在这里找到[标签过滤器](Tag%20filter.md)。

### 基于预设的过滤器

上述的替代方法，是根据单个预设或预设组来过滤物件。点击过滤器图标将显示一个预设选择对话框，类似于 Vespucci 其他地方使用的对话框。单个预设可以通过正常单击选择，预设组要长按才能选择（单击则是进入组）。 更多文档可以在这里找到[预设过滤器](Preset%20filter.md)。

## 自定义 Vespucci

可以自定义应用程序的许多方面，如果您正在寻找特定的东西但找不到它，可以在[Vespucci 网站](https://vespucci.io/) 搜索，网站上还包含有关设备上可用内容的附加信息。

<a id="layers"></a>

### 图层设置

图层设置可以通过图层控件（右上角的“汉堡”菜单）进行更改，所有其他设置都可以通过主菜单首选项按钮进行访问。 图层可以启用、禁用和暂时隐藏。

可用的图层类型：

* Data layer - this is the layer OpenStreetMap data is loaded in to. In normal use you do not need to change anything here. Default: on.
* Background layer - there is a wide range of aerial and satellite background imagery available. The default value for this is the "standard style" map from openstreetmap.org.
* Overlay layer - these are semi-transparent layers with additional information, for example quality assurance information. Adding an overlay may cause issues with older devices and such with limited memory. Default: none.
* Notes/Bugs display - Open Notes and bugs will be displayed as a yellow bug icon, closed ones the same in green. Default: on.
* Photo layer - Displays geo-referenced photographs as red camera icons, if direction information is available the icon will be rotated. Default: off.
* Mapillary layer - Displays Mapillary segments with markers where images exist, clicking on a marker will display the image. Default: off.
* GeoJSON layer - Displays the contents of a GeoJSON file, multiple layers can be added from files. Default: none.
* GPX layer - Displays GPX tracks and way points, multiple layers can be added from files, during recording the generate GPX track is displayed in its own one . Default: none.
* Grid - Displays a scale along the sides of the map or a grid. Default: on. 

更多信息可以在[地图显示](Main%20map%20display.md)部分找到。

#### 首选项

* 保持屏幕开启。 默认值：关闭。
* 大一点的节点拖动区域。 在具有触摸输入的设备上移动节点是有问题的，因为您的手指会遮挡显示屏上的当前位置。 开启此项将提供一个大区域，可用于偏心拖动（选择和其他操作仍使用正常的触摸容差区域）。默认值：关闭。

完整的描述可以在这里找到[首选项](Preferences.md)。

#### 高级首选项

* Full screen mode. On devices without hardware buttons Vespucci can run in full screen mode, that means that "virtual" navigation buttons will be automatically hidden while the map is displayed, providing more space on the screen for the map. Depending on your device this may work well or not,  In _Auto_ mode we try to determine automatically if using full screen mode is sensible or not, setting it to _Force_ or _Never_ skips the automatic check and full screen mode will always be used or always not be used respectively. On devices running Android 11 or higher the _Auto_ mode will never turn full screen mode on as Androids gesture navigation provides a viable alternative to it. Default: _Auto_.  
* Node icons. Default: _on_.
* Always show context menu. When turned on every selection process will show the context menu, turned off the menu is displayed only when no unambiguous selection can be determined. Default: off (used to be on).
* Enable light theme. On modern devices this is turned on by default. While you can enable it for older Android versions the style is likely to be inconsistent. 

完整的描述可以在这里找到[高级首选项](Advanced%20preferences.md)。

## Reporting and Resolving Issues

如果 Vespucci 出现崩溃，或者它检测到不一致的状态，您将被要求发送崩溃转储文件。如果发生这种情况，请按下面说的来做，每种情况只做一次就行。如果你想为功能请求或类似内容提供进一步的打字说话或开启一个 Github 的 issue，请在此处进行：[Vespucci 问题跟踪器]（https://github.com/MarcusWolschon/osmeditor4android/issues）。主菜单中的“提供反馈”功能将开启一个 Github 的 issue ，并包含相关的应用程序和设备信息，而无需额外输入。

If you are experiencing difficulties starting the app after a crash, you can try to start it in _Safe_ mode on devices that support short cuts: long press on the app icon and then select _Safe_ from the menu. 

If you want to discuss something related to Vespucci, you can either start a discussion on the [OpenStreetMap forum](https://community.openstreetmap.org).


