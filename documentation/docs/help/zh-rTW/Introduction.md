# Vespucci 介紹

Vespucci 是全功能的開放街圖編輯器，支援大部分能在桌面版編輯器能做的操作。Vespucci 已經在 Google Android 2.3 到 7.0 等多個以 AOSP 為基礎的版本測試成功。忠告：儘管行動裝置的效能已經追上桌機，但在較老的裝置上面，記憶體並不夠，因此運作速度上會很慢。你應該記住上述的事情，並且可能的話，控制自己編輯的區域大小在合理的大小。 

## 第一次使用

在啟動 Vespucci 時為您顯示 "下載其它位置"/"載入區域" 的對話框。如果您有座標顯示並且想要立即下載，您可以選擇合適的選項，和設定想要的下載半徑週圍位置，請不要在緩慢的裝置上選擇大的面積。 

除此之外您可以透過按按鈕解除對話框，並平移與放大位置到想要編輯和下載的資料(見下方："編輯于 Vespucci")。

## 使用 Vespucci 編輯

根據畫面尺寸和您裝置的新舊，編輯操作可以經由在頂端列的圖示可直接進入，通過下拉選單中頂端列的右側，從底部列(如果存在的話) ，或者透過選單鍵。

<a id="download"></a>

### 下載 OSM 資料

選擇轉移圖示 ![Transfer](../images/menu_transfer.png)，或是轉移選項的項目，這樣會顯示七個選項：

 * **下載當前的檢視** - 下載在螢幕上可見的區域，並取代所有目前的資料*(需要網路連線)* 
* **增加當前的檢視來下載** 下載在螢幕上可見的區域，並合併目前的資料*(需要網路連線)* 
* **下載其它位置** - 顯示一個表單，允許您輸入座標搜尋位置或直接使用現在位置，然後下載該區域週圍的地方*(需要網路連線)* 
* **上傳資料到 OSM 伺服器** - 上傳編輯到 OpenStreetMap *(需要認證)* *(需要網路連線)*
* **自動下載** - 自動的下載目前周圍位置區域 *(需要網路連線)*  *(需要 GPS)*
* **檔案...** - 儲存和載入在裝置的 OSM 檔案資料。
* **備註/錯誤** -下載 (自動或手動) OSM 備註，以及從 QA 工具 (目前是 OSMOSE) 下"錯誤" *(需要網路連線)*

最簡單下載資料到裝置的方式是縮放和平移到你想編輯的地方，接著選擇 "下載當前檢視"。你可以用手勢縮放，用縮放按鈕，或是用裝置的音量鍵。Vespucci 應當下載當前檢視的資料，下載資料到裝置時並不需要認證。

### 編輯

<a id="lock"></a>

#### Lock, unlock, mode switching

要避免不小心編輯的狀況，Vespucci 啟動時處於"鎖定"模式。處於鎖定模式時只允許縮放和移動地圖。點 ![Locked](../images/locked.png)  圖示則會解鎖螢幕。 

A long press on the lock icon will display a menu currently offering 4 options:

* **Normal** - the default editing mode, new objects can be added, existing ones edited, moved and removed. Simple white lock icon displayed.
* **Tag only** - selecting an existing object will start the Property Editor, a long press on the main screen will add objects, but no other geometry operations will work. White lock icon with a "T" is displayed.
* **Indoor** - enables Indoor mode, see [Indoor mode](#indoor). White lock icon with a "I" is displayed.
* **C-Mode** - enables C-Mode, only objects that have a warning flag set will be displayed, see [C-Mode](#c-mode). White lock icon with a "C" is displayed.

#### 單點，雙點和長按

根據預設設定，可選擇的節點或是路徑周圍會出現橘色區域，粗略表示你可以從那裡選碰觸選擇物件。你有三個選擇：

* 單點：選取物件。
  * 孤單的節點/路徑馬上會高亮度顯示。
  * 然後，如果你選取物件，但 Vespucci 覺得選取方式可能意味要多重選擇的話，會顯示選取選單，讓你能決定選那個物件。
  * 選取的物件會以黃色高亮度顯示。
  * 要看更多資訊，請看[節點選擇](../en/Node%20selected.md)， [路徑選擇](../en/Way%20selected.md)和[關係選擇](../en/Relation%20selected.md)
* 雙點：開啟[多重選擇模式](../en/Multiselect.md) 
* 長按：新建"十字準線"，讓你能新增節點，請見下面敘述和[新建新物件](../en/Creating%20new%20objects.md)

如果您試著編輯高密度區域時進行放大，這是一個很好的對策。

Vespucci 擁有一個良好的"取消/重做"系統，所以不要害怕在您的裝置嘗試，但是請不要上傳和儲存純測試資料。

#### 選擇/取消選擇 (單擊和“選擇選單”)

碰觸物件選取和高亮度顯示物件，碰觸螢幕空白處則會取消選取。如果你選取物件後你想再選取其他物件，就輕觸你想選取的物件，不需要先解除選取。在物件雙點會啟用[多重選取模式](../en/Multiselect.md)。

注意如果你嘗試選取物件，而 Vespucci 覺得這次選取重作可能意味多重選取物件 (像是路徑上的節點，或是重疊的物件)，之後會出現選取選單：點你想選取的物件，物件就會被選取。 

選取物件會以黃色細邊線顯示，黃色邊線也許會難以查察，視地圖背景和放大因子而定。一旦選取，你會看到通知確認要選取。

一互選取完成，你會看到 (可能是按鈕或是選單項目) 對選取物件的支援操作清單。要看更多資訊，請見 [物件選取](../en/Node%20selected.md), [路徑選取](../en/Way%20selected.md)和[關係選取](../en/Relation%20selected.md)。

#### 選擇物件：編輯標籤

第二次碰觸選取物件會開啟標籤編輯器，而你可以編輯選取物件的標籤。

注意當有重疊的物件 (像是路徑上的節點)，選取選單會再次顯示。選取相同物件則會出現標籤編輯器，選取其他物件則會轉而選取其他物件。

#### 選擇物件：移動節點或路徑

一旦你選取物件，也可以移動。注意這個物件在選取動況可以被拖動/移動。簡單在選取物件附近拖動 (在容忍範圍內)。如果你在設定中選擇大拖動範圍，你在選擇節點時會有物件附近的大範圍，能讓你有很方便置放物件。 

#### 增加新節點/點或是路徑 (長按)

在你想要放節點或是路徑開始的地方長按，你會看到黑色"準十字星"圖示。
* 如果你想要創建新節點 (並未連到其他物件)，避開既有的物件。
* 如果你想要延伸路徑，在"容忍範圍"內點選路徑 (或是路徑上的節點)。容忍範圍是指節點或路徑週圍的區域。

一旦你看到準星圖示，你有這些選擇：

* 碰觸同一地方。
  * 如果十字準星不在節點附近，碰觸同一地方則會再次創建新的節點。如果附近有路徑 (但不在節點附近)，新的節點會在路徑上面 (並且連到路徑上面)。
＊如果十字準星靠近節點瓶近 (例如在節點容忍範圍)，碰觸相同地方只會選取節點 (標籤編輯器會出現，不會有創建新的節點)。操作的過程與上述敘述一樣。
* 碰觸其他地方。碰觸其他地點 (在十字準星容忍範圍外) 則會從原始地點到現在位置新增路徑。如果十字準星在路徑或節點附近，新的片段會連到該節點或是路徑。　

簡單在你想增加節點的路徑上碰觸螢幕，要完成操作，請在最後一個節點碰觸兩次。如果最後的節點位於路徑或是節點，則片段會自動連到路徑或是節點。 

你也可以使用選單項目：請見[創建新物件](../zh_TW/Creating%20new%20objects.md)得到更多資訊。

#### 增加區域

OpenStreetMap currently doesn't have an "area" object type unlike other geo-data systems. The online editor "iD" tries to create an area abstraction from the underlying OSM elements which works well in some circumstances, in others not so. Vespucci currently doesn't try to do anything similar, so you need to know a bit about the way areas are represented:

* _封閉路徑 (*多邊形")_：最簡單而且最普遍的區域變體，其路徑有共同的第一個節點和最後一個節點，構成封閉的"環狀" (例如大部分的建築都是這種類型)。Vespucci 可以很容易創建封閉路徑，只要畫完區域時最後接回第一個節點。注意：要怎麼解讀區域得看加上去的標籤；舉例來說，如果封閉路徑被標為建築，則會視為區域，如果被標為圓環則不會。有些情況之下，解讀的狀況可能都可以通，被視為"區域"標籤。

* _多重多邊形_：有些區域有多個部件，有空洞區域和環形區域，不能簡單視為單一路徑。OSM 用特定的關係 (我們一般目的物件可以規範物件之間的關係)來完成，一個多重多邊形，一個多重多邊形可以擁有數個"外圈"，以及數個"內圈"。每個圈能以上述的封閉路徑，或是數個共同結束節點的單一路徑。當大的多重多邊形很難以任何工具處理，Vespucci 可以輕易處理小的物件。

* _大陸和島嶼_：對於大型物件，如大陸和島嶼，即便多重多邊形模式也沒辦法以滿意方式處理。如 natural=coastline 路徑我們假設方向相依語意：土地位於路徑左側，水域則是右側。但副作用則是你不能反轉有海岸標籤的路徑。更多資訊則可以到 [OSM wiki](http://wiki.openstreetmap.org/wiki/Tag:natural%3Dcoastline) 閱讀。

#### 改善道路的幾何形狀

如果你放大到夠大的程度，選取夠長的路徑中間會看到小的"x"。拖動"x"會在路徑位置上創建新的節點。注意：要避免不小心創新節點，因此操作碰觸的容忍區域相當小。

#### 剪下、複製和貼上

您可以複製或剪下選擇的節點和路徑，然後貼上一次或多次到一個新的位置。剪切將保留 OSM ID 和版本。要貼上長按要貼上的位置(您會看到一個十字線標記的位置)。然後從選單中選擇“貼上”。

#### 有效的增加地址

Vespucci 擁有"增加地址標籤"功能，讓探察時增加地址更方便。這個功能可以被選擇：

* 長按之後：Vespucci 會在該位置新增節點，並且猜測這裡的門牌號碼和增其他能被新增的地址標籤。如果節點在建築外框上面，則會自動增加 "entrance=yes" 標籤到節點上面。標籤編輯器則會開啟物件，讓你編輯該做的變動。

在節點/路徑選取模式：Vespucci 會增加地址標籤，並且開始標籤編輯器。

* 在標籤編輯器。

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

Vespucci 擁有簡單的衝突解決器。然後如果你懷疑你的編輯有重大問題，你可以匯出你的變動到 .osc 檔案 ("匯出"選單項目在傳輸選單裡)，然後用 JOSM 修正和上傳變動。請見[解決衝突](../en/Conflict%20resolution.md)裡詳盡的說明文件。  

## 使用 GPS

您可用 Vespucci 建立 GPX 軌跡和顯示在您的裝置上。進一步此外，您可顯示目前 GPS 位置，(在 GPS 選單中設定"顯示位置")具有螢幕中心周圍和追隨的位置，(在 GPS 選單中設定"追隨 GPS 位置")。 

如果你有後者的設定，手動移動螢幕或是編輯導致"跟隨 GPS" 模式關閉，而藍色的 GPS 方向箭頭會從邊框變成填充頭。要快速回到"跟隨"模式，簡單碰觸 GPS 按鈕，或是按選單選項。

## 備註和錯誤

Vespucci支援下載、回應或是關閉OSM註解(先前叫做OSM臭蟲)，相當於[OSMOSE 品質管控工具](http://osmose.openstreetmap.fr/en/map/)列出的"臭蟲"。兩者都可以完整下載下來，或者使用自動下載工具看到你的區域內的註解。一旦你回應或是關閉註解，你可以上傳單一臭蟲還是註解，或是一次上傳多個註解。

註解和臭蟲會以小蟲子圖示  ![Bug](../images/bug_open.png) 顯示在地圖上面，綠色代表關閉/解決，藍色代表被新增或是由你編輯過，而黃色表示仍然有效還沒有變動。 

OSMOSE臭蟲則會將受影響的物件顯示為藍色連結，碰觸連結則會選取物件，並且螢幕置中，如果需要則會下載周邊的區域。 

### 篩選

除了全域時啟用顯示註解和臭蟲以外，你可以設定粗略顯示過瀘降低雜亂程度。在"進階設定"裡，你可以單獨選取：

* 註解
* Osmose 錯誤
* Osmose 警告
* Osmose 小問題

<a id="indoor"></a>

## 室內模式

室內繪圖由於有相當多的物件而且常常彼此重疊，因此是相當有挑戰性的事情。Vespucci 發展出室內模式，能夠允許你過濾其他不在同一層的所有物件，並且自動加上目前樓層資訊到新增加的物件上面。

The mode can be enabled by long pressing on the lock item, see [Lock, unlock, mode switching](#lock) and selecting the corresponding menu entry.

<a id="c-mode"></a>

## C-Mode

In C-Mode only objects are displayed that have a warning flag set, this makes it easy to spot objects that have specific problems or match configurable checks. If an object is selected and the Property Editor started in C-Mode the best matching preset will automatically be applied.

A mode that only shows elements that have warnings and validation code that adds user configurable tests for missing tags and makes the re-survey warning time fully configurable. 

The mode can be enabled by long pressing on the lock item, see [Lock, unlock, mode switching](#lock) and selecting the corresponding menu entry.

### Configuring checks

Currently there are two configurable checks (there is a check for FIXME tags and a test for missing type tags on relations that are currently not configurable) both can be configured by selecting "Validator preferences" in the "Preferences". 

The list of entries is split in to two, the top half lists "re-survey" entries, the bottom half check "entries". Entries can be edited by clicking them, the green menu button allows adding of entries.

#### Re-survey entries

Re-survey entries have the following properties:

* **Key** - Key of the tag of interest.
* **Value** - Value the tag of interest should have, if empty the tag value will be ignored.
* **Age** - how many days after the element was last changed the element should be resurveyed, if a check_date field is present that will be the used, otherwise the date the current version was create. Setting the value to zero will lead to the check simply matching against key and value.
* **Regular expression** - if checked **Value** is assumed to be a JAVA regualr expression.

**Key** and **Value** are checked against the _existing_ keys of the object in question.

#### Check entries

Check entries have the following two properties:

* **Key** - Key that should be present on the object according to the matching preset.
* **Check optional** - Check the optional tags of the matching preset.

This check works be first determining the matching preset and then checking if **Key** is a "recommended" key for this object according to the preset, **Check optional** will expand the check to tags that are "optional* on the object. Note: currently linked presets are not checked.

## 篩選

### 標籤為依據的篩選

在主選單能啟用過濾器，可以按過濾器的圖示變更。更多說明文件可以到這邊[標籤過濾器](../en/Tag%20filter.md)。

預置為依據的篩選

上述的替代方式，依據單一預設組合或是預先組合群組過濾物件。按過濾圖示會顯示預設組合選擇選單，類似 Vespucci 裡見到的選單。只要正常按一下就可以選擇單一預設組合，要選擇預設組合群組則長按 (正常按一下之後進入群組)。更多文件說明可到這裡這邊尋找[預設組合過濾器](../en/Preset%20filter.md)。

## 客製化 Vespucci

### 設定，當您可能想要更改

* 背景圖層
* 地圖覆疊層。增加覆疊可能會替較舊或是記憶體較少的裝置造成問題。預設：無。
* 註解/臭蟲顯示。開啟註解和臭蟲，並且以黃色蟲子圖示表示，關閉的則是相同圖示綠色顯示。預設：開啟。
* 圖片圖層。顯示有地理參照的圖片，並且以紅色照相機圖示顯示，如果有方向資訊的話，圖示會旋轉。預設：關閉。
* 節點圖示。預設：開啟。
* 保持螢幕開啟。預設：關閉。
* 大的節點拖曳區域。在觸控裝置上移動節點是相當大的問題，而且你的手指會遮住目前位置。開啟這個功能可以中心外的拖拉相當大的區域 (選擇和其他操作仍然使用相當的觸控容許區域)。預設：關閉。

進階參數選項

* 總是顯示內容選單。當開啟這功能時，每一個選取過程都會顯示內容選單，關閉時則只會顯示無疑問的選取。預設：關閉 (先前是開啟)。
* 啟用輕亮主題。在現代的裝置上為預設開啟。當你在舊的 Android 版本啟用時往往不一致。
* 顯示統計數據。會顯示與除錯相關的數據，並不實用。預設：關閉 (先前是開啟)。  

## 回報問題

如果 Vespucci 當掉，或是偵測不一致的狀態，你會被詢問是否傳送當機報告。如果發生的話請寄送報告，但請一次描述特定狀況。如果你想提供更多資訊，或是開啟 issue請求新功能或其他類似請求，請到這邊開：[Vespucci issue tracker](https://github.com/MarcusWolschon/osmeditor4android/issues)。如果你想討論跟 Vespucci 相關的議題，你可以在 cci Google group](https://groups.google.com/forum/#!forum/osmeditor4android)  開討論，或是到 [OpenStreetMap Android forum](http://forum.openstreetmap.org/viewforum.php?id=56)


