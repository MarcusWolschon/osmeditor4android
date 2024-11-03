_Before we start: most screens have links in the menu to the on-device help system giving you direct access to information relevant for the current context, you can easily navigate back to this text too. If you have a larger device, for example a tablet, you can open the help system in a separate split window.  All the help texts and more (FAQs, tutorials) can be found on the [Vespucci documentation site](https://vespucci.io/) too. You can further start the help viewer directly on devices that support short cuts with a long press on the app icon and selecting "Help"_

# Vespucci 介紹

Vespucci is a full featured OpenStreetMap editor that supports most operations that desktop editors provide. It has been tested successfully on Google's Android 2.3 to 14.0 (versions prior to 4.1 are no longer supported) and various AOSP based variants. A word of caution: while mobile device capabilities have caught up with their desktop rivals, particularly older devices have very limited memory available and tend to be rather slow. You should take this in to account when using Vespucci and keep, for example, the areas you are editing to a reasonable size.

## 使用 Vespucci 編輯

根據畫面尺寸和您裝置的新舊，編輯操作可以經由在頂端列的圖示可直接進入，通過下拉選單中頂端列的右側，從底部列(如果存在的話) ，或者透過選單鍵。

<a id="download"></a>

### 下載 OSM 資料

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

最簡單下載資料到裝置的方式是縮放和平移到你想編輯的地方，接著選擇 "下載當前檢視"。你可以用手勢縮放，用縮放按鈕，或是用裝置的音量鍵。Vespucci 應當下載當前檢視的資料，下載資料到裝置時並不需要認證。

In unlocked state any non-downloaded areas will be dimmed relative to the downloaded ones if you are zoomed in far enough to enable editing. This is to avoid inadvertently adding duplicate objects in areas that are not being displayed. In the locked state dimming is disabled, this behaviour can be changed in the [Advanced preferences](Advanced%20preferences.md) so that dimming is always active.

If you need to use a non-standard OSM API entry, or use [offline data](https://vespucci.io/tutorials/offline/) in _MapSplit_ format you can add or change entries via the _Configure..._ entry for the data layer in the layer control.

### 編輯

<a id="lock"></a>

### 鎖定，解鎖，模式切換

要避免不小心編輯的狀況，Vespucci 啟動時處於"鎖定"模式。處於鎖定模式時只允許縮放和移動地圖。點 ![Locked](../images/locked.png)  圖示則會解鎖螢幕。 

A long press on the lock icon or the _Modes_ menu in the map display overflow menu will display a menu offering 4 options:

* **Normal** - the default editing mode, new objects can be added, existing ones edited, moved and removed. Simple white lock icon displayed.
* **Tag only** - selecting an existing object will start the Property Editor, new objects can be added via the green "+" button, or long press, but no other geometry operations are enabled. White lock icon with a "T" is displayed.
* **Address** - enables Address mode, a slightly simplified mode with specific actions available from the [Simple mode](../en/Simple%20actions.md) "+" button. White lock icon with an "A" is displayed.
* **Indoor** - enables Indoor mode, see [Indoor mode](#indoor). White lock icon with an "I" is displayed.
* **C-Mode** - enables C-Mode, only objects that have a warning flag set will be displayed, see [C-Mode](#c-mode). White lock icon with a "C" is displayed.

If you are using Vespucci on an Android device that supports short cuts (long press on the app icon) you can start directly to _Address_ and _Indoor_ mode.

#### 單點，雙點和長按

根據預設設定，可選擇的節點或是路徑周圍會出現橘色區域，粗略表示你可以從那裡選碰觸選擇物件。你有三個選擇：

* 單點：選取物件。
  * 孤單的節點/路徑馬上會高亮度顯示。
  * 然後，如果你選取物件，但 Vespucci 覺得選取方式可能意味要多重選擇的話，會顯示選取選單，讓你能決定選那個物件。
  * 選取的物件會以黃色高亮度顯示。
  * 要看更多資訊，請看[節點選擇](Node%20selected.md)， [路徑選擇](Way%20selected.md)和[關係選擇](../en/Relation%20selected.md)
* 雙點：開啟[多重選擇模式](Multiselect.md) 
* 長按：新建"十字準線"，讓你能新增節點，請見下面敘述和[新建新物件](Creating%20new%20objects.md)。這只有在"簡單模式"未啟用才能用。

如果您試著編輯高密度區域時進行放大，這是一個很好的對策。

Vespucci 擁有一個良好的"取消/重做"系統，所以不要害怕在您的裝置嘗試，但是請不要上傳和儲存純測試資料。

#### 選擇/取消選擇 (單擊和“選擇選單”)

碰觸物件選取和高亮度顯示物件，碰觸螢幕空白處則會取消選取。如果你選取物件後你想再選取其他物件，就輕觸你想選取的物件，不需要先解除選取。在物件雙點會啟用[多重選取模式](Multiselect.md)。

注意如果你嘗試選取物件，而 Vespucci 覺得這次選取重作可能意味多重選取物件 (像是路徑上的節點，或是重疊的物件)，之後會出現選取選單：點你想選取的物件，物件就會被選取。 

選取物件會以黃色細邊線顯示，黃色邊線也許會難以查察，視地圖背景和放大因子而定。一旦選取，你會看到通知確認要選取。

一旦選取完成，你會看到 (可能是按鈕或是選單項目) 對選取物件的支援操作清單。要看更多資訊，請見 [物件選取](Node%20selected.md), [路徑選取](Way%20selected.md)和[關係選取](Relation%20selected.md)。

#### 選擇物件：編輯標籤

第二次碰觸選取物件會開啟標籤編輯器，而你可以編輯選取物件的標籤。

注意當有重疊的物件 (像是路徑上的節點)，選取選單會再次顯示。選取相同物件則會出現標籤編輯器，選取其他物件則會轉而選取其他物件。

#### 選擇物件：移動節點或路徑

一旦你選取物件，也可以移動。注意這個物件在選取動況可以被拖動/移動。簡單在選取物件附近拖動 (在容忍範圍內)。如果你在[設定](Preferences.md)中選擇大拖動範圍，你在選擇節點時會有物件附近的大範圍，能讓你有很方便置放物件。 

#### 增加新節點/點或是路徑 

啟動 App 時就啟用"簡單模式"，這個設定能透過取消選取相對應的核取方塊，在主要選單中更動。

##### 簡易模式

按著地圖畫面上的綠色浮動按鈕，能顯示選單。當你選擇其中一個項目時，你會被問到想在那個位置新建物件，持續平移和縮放到你想要的地圖畫面為止。 

See [Creating new objects in simple actions mode](Simple%20actions.md) for more information. Simple mode os the default for new installs.

##### 進階 (長按) 模式

在你創建節點或是路徑開始的地方長按，你會看到黑色"十字"圖樣。
* 如果你想要創建新的節點 (並沒有連到其他物件)，請避開按到其他已有的物件。
* 如果你想延伸路徑，按在路徑的"容許區域" (或是路徑上的節點)內。容許區域會顯示在節點或是路徑周邊。

一旦你看到準星圖示，你有這些選擇：

* _正常按同一地方。_
  * 如果十字準星不在節點附近，碰觸同一地方則會再次創建新的節點。如果附近有路徑 (但不在節點附近)，新的節點會在路徑上面 (並且連到路徑上面)。
  * 如果十字準星靠近節點瓶近 (例如在節點容忍範圍)，碰觸相同地方只會選取節點 (標籤編輯器會出現，不會有創建新的節點)。操作的過程與上述敘述一樣。
* _正常碰觸其他地方。_碰觸其他地點 (在十字準星容忍範圍外) 則會從原始地點到現在位置新增路徑。如果十字準星在路徑或節點附近，新的片段會連到該節點或是路徑。　

簡單在你想增加節點的路徑上碰觸螢幕，要完成操作，請在最後一個節點碰觸兩次。如果最後的節點位於路徑或是節點，則片段會自動連到路徑或是節點。 

你也可以使用選單項目：請見[創建新物件](Creating%20new%20objects.md) 來獲得更多資訊。

#### 增加區域

目前開放街圖並沒有如其他地理資訊系統一樣，有"區域"物件類型。線上編輯器 "iD" 編輯嘗試在現有 OSM 架構下，建立區域類型，目前看來在特定狀況下運作相當好，也有不能好好運作的時候。Vespucci 目前並沒有計畫像 iD 一樣做類似的事情，所以你必須知道路徑區域是怎麼構成的：

* _封閉路徑 ("多邊形*)_：最簡單與最常見的區域類型，共享第一個節點與最後個節點的路徑來形成封閉的"環形" (例如說大部分的建築都是屬於這類)。封閉路徑相當容易在 Vespucci 新建，只要畫區域時連回第一個節點就可以了。注意：如何解讀封閉路徑端看加上去的標籤：例如說，如果封閉路徑標為建築，則會視為區域，如果標為圓環則不會。在一些情境當中，兩種解讀則都有效，加上 "area" 標籤能夠清楚區別用途。
* _多重多邊形_：有些區域有多個部分，穿洞與環形，無法只有一個路徑表示。OSM 採用特定型態的關聯 (我們一般用途物件能用關聯模型來連結) 來處理上述狀況，也就是多重多邊形。多重多邊形有"外圍"環形與"內部"環形。每個環形可以是上述提及的封閉路徑，或是多個分享結束節點的獨立路徑。當大型多重多邊形很難被任何工具處理，但小型的多邊形能輕易用 Vespucci 來處理。
* _海岸線_：對於相當大的物件、大陸與島嶼，即便是多重多邊形也無法圓滿來處理。對於 natural=coastline 路徑我們假設是方向相關的片段：陸地是在路徑的左側，而水域則在路徑的右側。然而這樣定義有個副作用，一般來說，你不應該反轉有海岸線標籤的路徑方向。你可以在  [OSM wiki](http://wiki.openstreetmap.org/wiki/Tag:natural%3Dcoastline) 閱讀更多資訊。

#### 改善道路的幾何形狀

如果你放大到夠大的程度，選取夠長的路徑中間會看到小的"x"。拖動"x"會在路徑位置上創建新的節點。注意：要避免不小心創新節點，因此操作碰觸的容忍區域相當小。

#### 剪下、複製和貼上

You can copy selected nodes and ways, and then paste once or multiple times to a new location. Cutting will retain the osm id and version, thus can only be pasted once. To paste long press the location you want to paste to (you will see a cross hair marking the location). Then select "Paste" from the menu.

#### 有效的增加地址

Vespucci 支援能更有效率在現地調查地址的功能，那就是預測門牌號碼功能 (分別在道路左側與右側作用)，以及依據位置與上次使用的值自動添加 _addr:street_ 或者 _addr:place_。在最佳情境下，能夠允許添加地址時不用輸入任何資訊。   

可以長按 ![Address](../images/address.png) 來啟動新增標籤： 

* 長按之後 (只有在非簡單模式下)：Vespucci 會在該位置新增節點，然後依據最近狀況猜測這邊的門牌號碼與地址標籤。如果節點在建築外緣則會自動在該節點加上 "entrance=yes" 標籤。標籤編輯器將會對有疑問的物件開啟，然後可以做進一步的編輯。
* 在節點/路徑選取模式：Vespucci 會像上述那樣添加地址標籤，然後啟動標籤編輯器。
* 在屬性編輯器。

要直接新增單獨的地址節點的話，則是在預設的"簡單模式"切換到"地址"編輯模式(長按鎖按鈕)，"新增地址節點"則會在該位置新增地址節點，而如果在建築外緣則會像前述那樣加出入口標籤。

門牌號碼預測，一般需求要在道路的兩側，至少兩間房屋號碼需要輸入到作業中，更多的號碼存在於資料中越好。

考慮用這個功能搭配[自動下載](#download)模式。  

#### 增加轉​​彎限制

Vespucci 有個快速增加轉彎限制的功能。如果需要則可以自動切割路徑，並且詢問後重新選擇物件。 

* 選取有道路標籤的路徑(轉彎限制只能加道路。如果你需要用其他方式做的話，請使用一般的"創建關係"模式)
* 從選單選取"增加限制"
* 選取"經由"節點或路徑 (只有"經由"元件有觸控區域顯示才有可能)
* 選取"到"路徑 (雙重後退和設定"到"元件變成"從"元件才有可能，Vespucci 會假設你增加不可迴轉限制)
* 設定限制種類

### Vespucci"鎖定"模式

當紅色鎖定圖示顯示時，所有非編輯動作都可以做。除此之外，長按或是接近物件時，如果是OSM物件時會顯示詳細資訊視窗

### 儲存您的變更

*(需要網路連線)*

選擇您在下載時相同的按鈕或選單項目，現在選擇 "上傳資料到 OSM 的伺服器"。

Vespucci supports OAuth 2, OAuth 1.0a authorization and the classical username and password method. Since July 1st 2024 the standard OpenStreetMap API only supports OAuth 2 and other methods are only available on private installations of the API or other projects that have repurposed OSM software.  

Authorizing Vespucci to access your account on your behalf requires you to one time login with your display name and password. If your Vespucci install isn't authorized when you attempt to upload modified data you will be asked to login to the OSM website (over an encrypted connection). After you have logged on you will be asked to authorize Vespucci to edit using your account. If you want to or need to authorize the OAuth access to your account before editing there is a corresponding item in the "Tools" menu.

如果您想要儲存您的工作，並且不能連入網際網路，您可以儲存成 JOSM 相容的 .osm 檔案，以後以 Vespucci 或 JOSM 任何一個上載。 

#### 在上傳解決衝突

Vespucci 有個簡單的衝突解決。不管怎樣，如果您于您的編輯察覺到有重要事件，將您的更改匯出到 .osc  檔案 (在"傳輸"選單中"匯出"的選單項目) 並且修復和上傳給 JOSM。請參閱有關詳細的説明 [衝突解決](Conflict%20resolution.md)。  

### Nearby point-of-interest display

A nearby point-of-interest display can be shown by pulling the handle in the middle and top of the bottom menu bar up. 

More information on this and other available functionality on the main display can be found here [Main map display](Main%20map%display.md).

## 使用 GPS 與 GPX 軌跡

當在標準設定時，Vespucci 會嘗試打開 GPS (以及其他依據衛星定位的導航系統)，如果不行才會退下來用"電信網路"來定位。這項行為預設你是在正常狀態使用你的 Android 裝置，只採用 GPX 產生的位置 (避免遭到追蹤)，就是委婉寫做"改進定位準確度"選項關閉。如果你想啟用這選項，但想避免 Vespucci 退下來採用"電信網路位置"，你應該在 [進階設定](Advanced%20preferences.md)關閉該選項。 

Touching the ![GPS](../images/menu_gps.png) button (normally on the left hand side of the map display) will center the screen on the current position and as you move the map display will be panned to maintain this.  Moving the screen manually or editing will cause the "follow GPS" mode to be disabled and the blue GPS arrow will change from an outline to a filled arrow. To quickly return to the "follow" mode, simply touch GPS button or re-check the equivalent menu option. If the device doesn't have a current location the location marker/arrow will be displayed in black, if a current location is available the marker will be blue.

要錄製 GPS 軌跡並且在裝置上顯示，你要在![GPS](../images/menu_gps.png) 選單選擇"開始錄製GPX軌跡"。這會在螢幕上新增目前錄製軌跡圖層，你可以在 [圖層控制](Main%20map%20display.md)上傳或是匯出軌跡。你可以新增本地 GPX 檔案與從 OSM API 下載軌跡變成其他圖層。

注意：預設 Vespucci 不會在錄製 GPX 軌跡時記錄高度，這是因為一些 Android 裝置的特殊問題。要啟用高度記錄，需要啟用高度記錄，或是安裝重力模組，或是更簡單到[進階設定](Advanced%20preferences.md) 設定 NMEA 輸入。

### How to export a GPX track?

Open the layer menu, then click the 3-dots menu next to "GPX recording", then select **Export GPX track...**. Choose in which folder to export the track, then give it a name suffixed with `.gpx` (example: MyTrack.gpx).

## Notes, Bugs and Todos

Vespucci 支援下載、回應和關閉 OSM 註解 (先前的 OSM 臭蟲)，以及相等功能由 [OSMOSE 品質監控工具](http://osmose.openstreetmap.fr/en/map/)產生的"臭蟲"。兩者都能完整下載下來，或者你可以使用自動下載功能，馬下輔助你取得所在區域的物件。一旦編輯或關閉，你可以馬上上傳臭蟲或是註解，或是全部一次上傳。 

Further we support "Todos" that can either be created from OSM elements, from a GeoJSON layer, or externally to Vespucci. These provide a convenient way to keep track of work that you want to complete. 

On the map the Notes and bugs are represented by a small bug icon ![Bug](../images/bug_open.png), green ones are closed/resolved, blue ones have been created or edited by you, and yellow indicates that it is still active and hasn't been changed. Todos use a yellow checkbox icon.

The OSMOSE bug and Todos display will provide a link to the affected element in blue (in the case of Todos only if an OSM element is associated with it), touching the link will select the object, center the screen on it and down load the area beforehand if necessary. 

### 篩選

除了全域時啟用顯示註解和臭蟲以外，你可以設定粗略顯示過瀘降低雜亂程度。你可以從任務圖層當中的[圖層控制](#layers)當中，設定過濾選項：

* Notes
* Osmose error
* Osmose warning
* Osmose minor issue
* Maproulette
* Todo

<a id="indoor"></a>

## 室內模式

室內繪圖由於有相當多的物件而且常常彼此重疊，因此是相當有挑戰性的事情。Vespucci 發展出室內模式，能夠允許你過濾其他不在同一層的所有物件，並且自動加上目前樓層資訊到新增加的物件上面。

這個模式可以透過長按鎖定鈕，請見[鎖定、解鎖、切換模式](#lock)，然後選擇對應的選單選項。

<a id="c-mode"></a>

## C-模式

在 C-模式下，只有擁有警告標示的物件才會顯示，讓檢視有特定問題或符合設定檢查的物件變得更容易。C-模式下啟動的內容編輯器，最符合的預設組合會自動套用。

這個模式可以透過長按鎖定鈕，請見[鎖定、解鎖、切換模式](#lock)，然後選擇對應的選單選項。

### 設定檢查

All validations can be disabled/enabled in the "Validator settings/Enabled validations" in the [preferences](Preferences.md). 

The configuration for "Re-survey" entries allows you to set a time after which a tag combination should be re-surveyed. "Check" entries are tags that should be present on objects as determined by matching presets. Entries can be edited by clicking them, the green menu button allows adding of entries.

#### 重新踏察選項

重新踏察列表擁有以下內容：

* **鍵** - 感興趣標籤的鍵。
* **值** - 感興趣標籤應該要的值，如果是空白的話則標籤的值會被忽略。
* **年齡** - 離上次元素變動的時間過了多少天了，代表可能需要重新踏察。如果有 _check_date_ 標籤則會用到，不然就是依據現在版本是那一天創建的。設為零則會簡單檢查鍵和值。
* **正規表示式** - 如果檢查 **值**則會假定是 JAVA 的正規表示式。 

**鍵** 和 **值** 會與問題物件 _existing_ tags 相比檢查。

在標準預設組合當中的_Annotations_ 群組中 ，含有物件能夠自動增加現在時間的 _check_date_ 標籤。

#### 檢查選項

檢查列表有兩個內容：

* **鍵** - 鍵應當依據符合的預置出現在相對的物件。
* **需要的選填** - 即便是符合的預置的選填標籤，也會需要鍵。

這次檢查會先決定符合的預置，接著檢查**鍵**是否是預置推薦的"建議"鍵值。**必須的選擇性**則會擴大檢查物件標籤中的"選擇性"標籤。注意：目前連結的預置並不會檢查。

## 篩選

### 標籤為依據的篩選

在主選單能啟用過濾器，可以按過濾器的圖示變更。更多說明文件可以到這邊[標籤過濾器](Tag%20filter.md)。

預置為依據的篩選

上述的替代方式，依據單一預設組合或是預先組合群組過濾物件。按過濾圖示會顯示預設組合選擇選單，類似 Vespucci 裡見到的選單。只要正常按一下就可以選擇單一預設組合，要選擇預設組合群組則長按 (正常按一下之後進入群組)。更多文件說明可到這裡這邊尋找[預設組合過濾器](Preset%20filter.md)。

## 客製化 Vespucci

這款 app 很多方面都能自訂，如果你想要特別的功能但找不到的話，可以到 [Vespucci 網站](https://vespucci.io/)搜尋，尋找裝置上的額外資訊。

<a id="layers"></a>

### 圖層設定

圖層設定可以藉由圖層控制來改變 (在右上角"漢堡"選項)，所有其他設定可以透過主選項設定按鈕設定進入。可以啟動、關闢或暫時隱藏圖層。

可用的圖層種類：

* Data layer - this is the layer OpenStreetMap data is loaded in to. In normal use you do not need to change anything here. Default: on.
* Background layer - there is a wide range of aerial and satellite background imagery available. The default value for this is the "standard style" map from openstreetmap.org.
* Overlay layer - these are semi-transparent layers with additional information, for example quality assurance information. Adding an overlay may cause issues with older devices and such with limited memory. Default: none.
* Notes/Bugs display - Open Notes and bugs will be displayed as a yellow bug icon, closed ones the same in green. Default: on.
* Photo layer - Displays geo-referenced photographs as red camera icons, if direction information is available the icon will be rotated. Default: off.
* Mapillary layer - Displays Mapillary segments with markers where images exist, clicking on a marker will display the image. Default: off.
* GeoJSON layer - Displays the contents of a GeoJSON file, multiple layers can be added from files. Default: none.
* GPX layer - Displays GPX tracks and way points, multiple layers can be added from files, during recording the generate GPX track is displayed in its own one . Default: none.
* Grid - Displays a scale along the sides of the map or a grid. Default: on. 

更多資訊能在[地圖顯示](Main%20map%20display.md)取得。

#### 參數選項 

* 保持螢幕開啟。預設：關閉。
* 巨大節點拖曳區，當裝置觸控輸入導致移動節點有問題時，手指會在顯示目前位置採模糊顯示。開啟這個選項會提供大片區域，來用在非中心的拖拉上 (選擇和其他操作仍會使用正常的觸控容許範圍)。預設：關閉。

整個敘述能在這裡[設定](Preferences.md)找到

進階參數選項

* Full screen mode. On devices without hardware buttons Vespucci can run in full screen mode, that means that "virtual" navigation buttons will be automatically hidden while the map is displayed, providing more space on the screen for the map. Depending on your device this may work well or not,  In _Auto_ mode we try to determine automatically if using full screen mode is sensible or not, setting it to _Force_ or _Never_ skips the automatic check and full screen mode will always be used or always not be used respectively. On devices running Android 11 or higher the _Auto_ mode will never turn full screen mode on as Androids gesture navigation provides a viable alternative to it. Default: _Auto_.  
* Node icons. Default: _on_.
* Always show context menu. When turned on every selection process will show the context menu, turned off the menu is displayed only when no unambiguous selection can be determined. Default: off (used to be on).
* Enable light theme. On modern devices this is turned on by default. While you can enable it for older Android versions the style is likely to be inconsistent. 

整個敘述能在這裡[進階設定](Advanced%20preferences.md)找到

## Reporting and Resolving Issues

如果 Vespucci 當掉，或是偵測不一致的狀態，你會被詢問是否傳送當機報告。如果發生的話請寄送報告，但請一次描述特定狀況。如果你想提供更多資訊，或是開啟 issue 請求新功能或其他類似請求，請到這邊開：[Vespucci issue tracker](https://github.com/MarcusWolschon/osmeditor4android/issues)。主選單的"提供回饋"功能會開放新的 issue，不用額外輸入就有包含相關的 app 和裝置資訊。

If you are experiencing difficulties starting the app after a crash, you can try to start it in _Safe_ mode on devices that support short cuts: long press on the app icon and then select _Safe_ from the menu. 

If you want to discuss something related to Vespucci, you can either start a discussion on the [OpenStreetMap forum](https://community.openstreetmap.org).


