# Vespucci 介紹

Vespucci 是全功能的開放街圖編輯器，支援大部分能在桌面版編輯器能做的操作。Vespucci 已經在 Google Android 2.3 到 10.0 等多個以 AOSP 為基礎的版本測試成功。忠告：儘管行動裝置的效能已經追上桌機，但在較老的裝置上面，記憶體並不夠，因此運作速度上會很慢。你應該記住上述的事情，並且可能的話，控制自己編輯的區域大小在合理的大小。 

## 第一次使用

當啟動 Vespucci 時在要求需要的權限和顯示歡迎訊息之後，顯示"下載其他區域/戴入區域"對話框。如果你有要顯示的經緯度和想要馬上下載資料，你可以選擇適當的選項和設定下載地點週圍的半徑來下載資料，請不要在慢速的裝置上面載入太大的區域。 

除此之外您可以透過按按鈕解除對話框，並平移與放大位置到想要編輯和下載的資料(見下方："編輯于 Vespucci")。

## 使用 Vespucci 編輯

根據畫面尺寸和您裝置的新舊，編輯操作可以經由在頂端列的圖示可直接進入，通過下拉選單中頂端列的右側，從底部列(如果存在的話) ，或者透過選單鍵。

<a id="download"></a>

### 下載 OSM 資料

選擇轉移圖示 ![Transfer](../images/menu_transfer.png)，或是轉移選項的項目，這樣會顯示七個選項：

 * **下載現有的檢視** - 下載在螢幕上可見的區域，並與所有目前的資料合併*(需要網路連線)* 
* **清除現有資料並下載現有的檢視** 清除所有在記憶體中的資料，之後下載在螢幕上可見的區域*(需要網路連線)* 
* **上傳資料到 OSM 伺服器** - 上傳編輯到 OpenStreetMap *(需要認證)* *(要網路連線)*
* **依據位置自動下載** - 自動的下載目前周圍位置區域 *(需要網路連線或離線資料)*  *(需要 GPS)*
* **拖放和縮放並自動下載** - 自動下載目前地圖現有的檢視 *(需要網路連線或離線資料)  *(需要 GPS)*
* **檔案...** - 儲存和載入在裝置的 OSM 檔案資料。
* **備註/錯誤** -從 QA 工具 (目前的 OSMOSE) 下載 (自動或手動) OSM 備註和"錯誤" *(需要網路連線)*

最簡單下載資料到裝置的方式是縮放和平移到你想編輯的地方，接著選擇 "下載當前檢視"。你可以用手勢縮放，用縮放按鈕，或是用裝置的音量鍵。Vespucci 應當下載當前檢視的資料，下載資料到裝置時並不需要認證。

### 編輯

<a id="lock"></a>

### 鎖定，解鎖，模式切換

要避免不小心編輯的狀況，Vespucci 啟動時處於"鎖定"模式。處於鎖定模式時只允許縮放和移動地圖。點 ![Locked](../images/locked.png)  圖示則會解鎖螢幕。 

在鎖定圖示長按顯示的選單有以下四個選項：

* **正常** - 預設的編輯模式，新物件能被添加，既有的物件可以被編輯、移動和移除。用簡單的白色鎖定圖示表示。
* **只有標籤** - 選擇既有的物件會跳進內容編輯器，主畫面長按則會新增物件，但不會有任何幾何動作。用白色鎖定圖示加上"T"的方式表示。
* **室內** - 啟動室內模式，請見[室內模式](#indoor)。用白色鎖定圖示加上"I"的方式表示。
* C-模式** - 啟動 C-模式，只有上面有警告標示的物件會顯示，請見 [C-模式](#c-mode)。用白色鎖定圖示加上"C"的方式表示。

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

一旦你選取物件，也可以移動。注意這個物件在選取動況可以被拖動/移動。簡單在選取物件附近拖動 (在容忍範圍內)。如果你在設定中選擇大拖動範圍，你在選擇節點時會有物件附近的大範圍，能讓你有很方便置放物件。 

#### 增加新節點/點或是路徑 

啟動 App 時就啟用"簡單模式"，這個設定能透過取消選取相對應的核取方塊，在主要選單中更動。

##### 簡易模式

按著地圖畫面上的綠色浮動按鈕，能顯示選單。當你選擇其中一個項目時，你會被問到想在那個位置新建物件，持續平移和縮放到你想要的地圖畫面為止。 

請見 [簡單模式下新建物件](Creating%20new%20objects%20in%20simple%20actions%20mode.md) 來得到更多資訊。

##### 進階 (長按) 模式

在你創建節點或是路徑開始的地方長按，你會看到黑色"十字"圖樣。
* 如果你想要創建新的節點 (並沒有連到其他物件)，請避開其他已有的物件。
* 如果你想延伸路徑，點選路徑的"容許區域" (或是路徑上的節點)。容許區域會顯示在節點或是路徑周邊。

一旦你看到準星圖示，你有這些選擇：

* 碰觸同一地方。
  * 如果十字準星不在節點附近，碰觸同一地方則會再次創建新的節點。如果附近有路徑 (但不在節點附近)，新的節點會在路徑上面 (並且連到路徑上面)。
＊如果十字準星靠近節點瓶近 (例如在節點容忍範圍)，碰觸相同地方只會選取節點 (標籤編輯器會出現，不會有創建新的節點)。操作的過程與上述敘述一樣。
* 碰觸其他地方。碰觸其他地點 (在十字準星容忍範圍外) 則會從原始地點到現在位置新增路徑。如果十字準星在路徑或節點附近，新的片段會連到該節點或是路徑。　

簡單在你想增加節點的路徑上碰觸螢幕，要完成操作，請在最後一個節點碰觸兩次。如果最後的節點位於路徑或是節點，則片段會自動連到路徑或是節點。 

你也可以使用選單項目：請見[創建新物件](Creating%20new%20objects.md) 來獲得更多資訊。

#### 增加區域

目前開放街圖並沒有如其他地理資訊系統一樣，有"區域"物件類型。線上編輯器 "iD" 編輯嘗試在現有 OSM 架構下，建立區域類型，目前看來在特定狀況下運作相當好，也有不能好好運作的時候。Vespucci 目前並沒有計畫像 iD 一樣做類似的事情，所以你必須知道路徑區域是怎麼構成的：

* _封閉路徑 (*多邊形")_：最簡單而且最普遍的區域變體，其路徑有共同的第一個節點和最後一個節點，構成封閉的"環狀" (例如大部分的建築都是這種類型)。Vespucci 可以很容易創建封閉路徑，只要畫完區域時最後接回第一個節點。注意：要怎麼解讀區域得看加上去的標籤；舉例來說，如果封閉路徑被標為建築，則會視為區域，如果被標為圓環則不會。有些情況之下，解讀的狀況可能都可以通，被視為"區域"標籤。

* _多重多邊形_：有些區域有多個部件，有空洞區域和環形區域，不能簡單視為單一路徑。OSM 用特定的關係 (我們一般目的物件可以規範物件之間的關係)來完成，一個多重多邊形，一個多重多邊形可以擁有數個"外圈"，以及數個"內圈"。每個圈能以上述的封閉路徑，或是數個共同結束節點的單一路徑。當大的多重多邊形很難以任何工具處理，Vespucci 可以輕易處理小的物件。

* _大陸和島嶼_：對於大型物件，如大陸和島嶼，即便多重多邊形模式也沒辦法以滿意方式處理。如 natural=coastline 路徑我們假設方向相依語意：土地位於路徑左側，水域則是右側。但副作用則是你不能反轉有海岸標籤的路徑。更多資訊則可以到 [OSM wiki](http://wiki.openstreetmap.org/wiki/Tag:natural%3Dcoastline) 閱讀。

#### 改善道路的幾何形狀

如果你放大到夠大的程度，選取夠長的路徑中間會看到小的"x"。拖動"x"會在路徑位置上創建新的節點。注意：要避免不小心創新節點，因此操作碰觸的容忍區域相當小。

#### 剪下、複製和貼上

您可以複製或剪下選擇的節點和路徑，然後貼上一次或多次到一個新的位置。剪切將保留 OSM ID 和版本。要貼上長按要貼上的位置(您會看到一個十字線標記的位置)。然後從選單中選擇“貼上”。

#### 有效的增加地址

Vespucci 擁有"![地址](../images/address.png) 增加地址標籤"功能，讓探察時藉由自動預測目前門牌號碼添加地址時更為方便。這個功能可以被選擇：

* after a long press (_non-simple mode only:): Vespucci will add a node at the location and make a best guess at the house number and add address tags that you have been lately been using. If the node is on a building outline it will automatically add a "entrance=yes" tag to the node. The tag editor will open for the object in question and let you make any necessary further changes.
* in the node/way selected modes: Vespucci will add address tags as above and start the tag editor.
* in the property editor.

門牌號碼預測，一般需求要在道路的兩側，至少兩間房屋號碼需要輸入到作業中，更多的號碼存在於資料中越好。

考慮使用[自動下載](#download)模式。  

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

Vespucci 除了支援 OAuth 的授權和標準的使用者名稱與密碼的方式。 而 OAuth 更好，因為它避免了以明文發送密碼。

新版Vespucci安裝之後，OAuth會預設啟用。當你第一次要上傳變動的資料，一頁OSM網頁會載入。登入之後(透過加密連線)，你會被要求認證Vespucci才能用你帳號編輯。如果你想要或是需要在編輯前，認證你帳號的OAuth連線，你可以在"工具"選單選擇對應的項目。

如果您想要儲存您的工作，並且不能連入網際網路，您可以儲存成 JOSM 相容的 .osm 檔案，以後以 Vespucci 或 JOSM 任何一個上載。 

#### 在上傳解決衝突

Vespucci 有個簡單的衝突解決。不管怎樣，如果您于您的編輯察覺到有重要事件，將您的更改匯出到 .osc  檔案 (在"傳輸"選單中"匯出"的選單項目) 並且修復和上傳給 JOSM。請參閱有關詳細的説明 [衝突解決](Conflict%20resolution.md)。  

## 使用 GPS

您可用 Vespucci 建立 GPX 軌跡和顯示在您的裝置上。進一步此外，您可顯示目前 GPS 位置，(在 GPS 選單中設定"顯示位置")具有螢幕中心周圍和追隨的位置，(在 GPS 選單中設定"追隨 GPS 位置")。 

如果你有後者的設定，手動移動螢幕或是編輯導致"跟隨 GPS" 模式關閉，而藍色的 GPS 方向箭頭會從邊框變成填充頭。要快速回到"跟隨"模式，簡單碰觸 GPS 按鈕，或是按選單選項。

## 備註和錯誤

Vespucci 支援下載、回應和關閉 OSM 註解 (先前的 OSM 臭蟲)，以及相等功能由 [OSMOSE 品質監控工具](http://osmose.openstreetmap.fr/en/map/)產生的"臭蟲"。兩者都能完整下載下來，或者你可以使用自動下載功能，馬下輔助你取得所在區域的物件。一旦編輯或關閉，你可以馬上上傳臭蟲或是註解，或是全部一次上傳。

註解和臭蟲會以小蟲子圖示  ![Bug](../images/bug_open.png) 顯示在地圖上面，綠色代表關閉/解決，藍色代表被新增或是由你編輯過，而黃色表示仍然有效還沒有變動。 

OSMOSE臭蟲則會將受影響的物件顯示為藍色連結，碰觸連結則會選取物件，並且螢幕置中，如果需要則會下載周邊的區域。 

### 篩選

除了全域時啟用顯示註解和臭蟲以外，你可以設定粗略顯示過瀘降低雜亂程度。在[進階設定](Advanced%20preferences.md)裡，你可以單獨選取：

* 註解
* Osmose 錯誤
* Osmose 警告
* Osmose 小問題
* 客製

<a id="indoor"></a>

## 室內模式

室內繪圖由於有相當多的物件而且常常彼此重疊，因此是相當有挑戰性的事情。Vespucci 發展出室內模式，能夠允許你過濾其他不在同一層的所有物件，並且自動加上目前樓層資訊到新增加的物件上面。

這個模式可以透過長按鎖定鈕，請見[鎖定、解鎖、切換模式](#lock)，然後選擇對應的選單選項。

<a id="c-mode"></a>

## C-模式

在 C-模式下，只有擁有警告標示的物件才會顯示，讓檢視有特定問題或符合設定檢查的物件變得更容易。C-模式下啟動的內容編輯器，最符合的預設組合會自動套用。

這個模式可以透過長按鎖定鈕，請見[鎖定、解鎖、切換模式](#lock)，然後選擇對應的選單選項。

### 設定檢查

Currently there are two configurable checks (there is a check for FIXME tags and a test for missing type tags on relations that are currently not configurable) both can be configured by selecting "Validator settings" in the "Preferences". 

列表清單分成兩部分，上半部列出"重新踏察"列表，下半部列出檢查"列表列表"。列表點了之後就可以編輯了，綠色選單按鍵則允許增加列表。

#### 重新踏察選項

重新踏察列表擁有以下內容：

* **Key** - Key of the tag of interest.
* **Value** - Value the tag of interest should have, if empty the tag value will be ignored.
* **Age** - how many days after the element was last changed the element should be re-surveyed, if a _check_date_ tag is present that will be the used, otherwise the date the current version was create. Setting the value to zero will lead to the check simply matching against key and value.
* **Regular expression** - if checked **Value** is assumed to be a JAVA regular expression.

**鍵** 和 **值** 會與問題物件 _existing_ tags 相比檢查。

The _Annotations_ group in the standard presets contain an item that will automatically add a _check_date_ tag with the current date.

#### 檢查選項

檢查列表有兩個內容：

* **鍵** - 物件的鍵，應該有相對應的預置。
* **必須的選擇性** - 即便鍵相對應的預置是選擇性的標籤，仍然需要該鍵。

這次檢查會先決定符合的預置，接著檢查**鍵**是否是預置推薦的"建議"鍵值。**必須的選擇性**則會擴大檢查物件標籤中的"選擇性"標籤。注意：目前連結的預置並不會檢查。

## 篩選

### 標籤為依據的篩選

在主選單能啟用過濾器，可以按過濾器的圖示變更。更多說明文件可以到這邊[標籤過濾器](Tag%20filter.md)。

預置為依據的篩選

上述的替代方式，依據單一預設組合或是預先組合群組過濾物件。按過濾圖示會顯示預設組合選擇選單，類似 Vespucci 裡見到的選單。只要正常按一下就可以選擇單一預設組合，要選擇預設組合群組則長按 (正常按一下之後進入群組)。更多文件說明可到這裡這邊尋找[預設組合過濾器](Preset%20filter.md)。

## 客製化 Vespucci

Many aspects of the app can be customized, if you are looking for something specific and can't find it, [the Vespucci website](https://vespucci.io/) is searchable and contains additional information over what is available on device.

### Layer settings

Layer settings can be changed via the layer control (upper right corner), all other setting are reachable via the main menu preferences button.

* Background layer - there is a wide range of aerial and satellite background imagery available, , the default value for this is the "standard style" map from openstreetmap.org.
* Overlay layer - these are semi-transparent layers with additional information, for example GPX tracks. Adding an overlay may cause issues with older devices and such with limited memory. Default: none.
* Notes/Bugs display. Open Notes and bugs will be displayed as a yellow bug icon, closed ones the same in green. Default: on.
* Photo layer. Displays geo-referenced photographs as red camera icons, if direction information is available the icon will be rotated. Default: off.

#### 參數選項 

* Keep screen on. Default: off.
* Large node drag area. Moving nodes on a device with touch input is problematic since your fingers will obscure the current position on the display. Turning this on will provide a large area which can be used for off-center dragging (selection and other operations still use the normal touch tolerance area). Default: off.

The full description can be found here [Preferences](Preferences.md)

進階參數選項

* Node icons. Default: on.
* Always show context menu. When turned on every selection process will show the context menu, turned off the menu is displayed only when no unambiguous selection can be determined. Default: off (used to be on).
* Enable light theme. On modern devices this is turned on by default. While you can enable it for older Android versions the style is likely to be inconsistent. 

The full description can be found here [Advanced preferences](Advanced%20preferences.md)

## 回報問題

如果 Vespucci 當掉，或是偵測不一致的狀態，你會被詢問是否傳送當機報告。如果發生的話請寄送報告，但請一次描述特定狀況。如果你想提供更多資訊，或是開啟 issue 請求新功能或其他類似請求，請到這邊開：[Vespucci issue tracker](https://github.com/MarcusWolschon/osmeditor4android/issues)。主選單的"提供回饋"功能會開放新的 issue，不用額外輸入就有包含相關的 app 和裝置資訊。

如果你想討論跟 Vespucci 相關的議題，你可以在 [Vespucci Google group](https://groups.google.com/forum/#!forum/osmeditor4android) 開討論，或是到 [OpenStreetMap Android forum](http://forum.openstreetmap.org/viewforum.php?id=56)


