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

#### 鎖定，解鎖，"只有標籤編輯"，室內模式 

要避免不小心編輯的狀況，Vespucci 啟動時處於"鎖定"模式。處於鎖定模式時只允許縮放和移動地圖。點 ![Locked](../images/locked.png)  圖示則會解鎖螢幕。 

對著鎖定圖示長按的話，則會啟動"只有標籤編輯"模式，就不會編輯物件形狀或是移動物件了。而在只有標籤編輯模式下，會出現略為不同的白色鎖定圖示。你仍可以像平常一樣長按新增新的節點或是路徑。

接著長按則會啟用 [室內模式](#indoor)，再長按則會轉回正常編輯模式。

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

開放街圖與其他地理資料系統不同，目前沒有"區域"物件類型。線上編輯器 "iD" 嘗試創建區域來代表特定底層 OSM 元件，有時候可以順暢運作，其他情況則不行。VEspucci 目前並沒有類型的作法，所以你必須知道路徑區域如何表示：

* _closed ways (*polygons")_: the simplest and most common area variant, are ways that have a shared first and last node forming a closed "ring" (for example most buildings are of this type). These are very easy to create in Vespucci, simply connect back to the first node when you are finished with drawing the area. Note: the interpretation of the closed way depends on its tagging: for example if a closed way is tagged as a building it will be considered an area, if it is tagged as a roundabout it wont. In some situations in which both interpretations may be valid, an "area" tag can clarify the intended use.
* _multi-ploygons_: some areas have multiple parts, holes and rings that can't be represented with just one way. OSM uses a specific type of relation (our general purpose object that can model relations between elements) to get around this, a multi-polygon. A multi-polygon can have multiple "outer" rings, and multiple "inner" rings. Each ring can either be a closed way as described above, or multiple individual ways that have common end nodes. While large multi-polygons are difficult to handle with any tool, small ones are not difficult to create in Vespucci. 
* _coastlines_: for very large objects, continents and islands, even the multi-polygon model doesn't work in a satisfactory way. For natural=coastline ways we assume direction dependent semantics: the land is on the left side of the way, the water on the right side. A side effect of this is that, in general, you shouldn't reverse the direction of a way with coastline tagging. More information can be found on the [OSM wiki](http://wiki.openstreetmap.org/wiki/Tag:natural%3Dcoastline).

#### 改善道路的幾何形狀

If you zoom in far enough on a selected way you will see a small "x" in the middle of the way segments that are long enough. Dragging the "x" will create a node in the way at that location. Note: to avoid accidentally creating nodes, the touch tolerance area for this operation is fairly small.

#### 剪下、複製和貼上

您可以複製或剪下選擇的節點和路徑，然後貼上一次或多次到一個新的位置。剪切將保留 OSM ID 和版本。要貼上長按要貼上的位置(您會看到一個十字線標記的位置)。然後從選單中選擇“貼上”。

#### 有效的增加地址

Vespucci has an "add address tags" function that tries to make surveying addresses more efficient. It can be selected:

* after a long press: Vespucci will add a node at the location and make a best guess at the house number and add address tags that you have been lately been using. If the node is on a building outline it will automatically add a "entrance=yes" tag to the node. The tag editor will open for the object in question and let you make any necessary further changes.
* in the node/way selected modes: Vespucci will add address tags as above and start the tag editor.
* in the tag editor.

門牌號碼預測，一般需求要在道路的兩側，至少兩間房屋號碼需要輸入到作業中，更多的號碼存在於資料中越好。

Consider using this with the [Auto-download](#download) mode.  

#### 增加轉​​彎限制

Vespucci has a fast way to add turn restrictions. if necessary it will split ways automatically and ask you to re-select elements. 

* select a way with a highway tag (turn restrictions can only be added to highways, if you need to do this for other ways, please use the generic "create relation" mode)
* select "Add restriction" from the menu
* select the "via" node or way (only possible "via" elements will have the touch area shown)
* select the "to" way (it is possible to double back and set the "to" element to the "from" element, Vespucci will assume that you are adding an no_u_turn restriction)
* set the restriction type

### Vespucci"鎖定"模式

當紅色鎖定圖示顯示時，所有非編輯動作都可以做。除此之外，長按或是接近物件時，如果是OSM物件時會顯示詳細資訊視窗

### 儲存您的變更

*(需要網路連線)*

選擇您在下載時相同的按鈕或選單項目，現在選擇 "上傳資料到 OSM 的伺服器"。

Vespucci 除了支援 OAuth 的授權和標準的使用者名稱與密碼的方式。 而 OAuth 更好，因為它避免了以明文發送密碼。

新版Vespucci安裝之後，OAuth會預設啟用。當你第一次要上傳變動的資料，一頁OSM網頁會載入。登入之後(透過加密連線)，你會被要求認證Vespucci才能用你帳號編輯。如果你想要或是需要在編輯前，認證你帳號的OAuth連線，你可以在"工具"選單選擇對應的項目。

如果您想要儲存您的工作，並且不能連入網際網路，您可以儲存成 JOSM 相容的 .osm 檔案，以後以 Vespucci 或 JOSM 任何一個上載。 

#### 在上傳解決衝突

Vespucci has a simple conflict resolver. However if you suspect that there are major issues with your edits, export your changes to a .osc file ("Export" menu item in the "Transfer" menu) and fix and upload them with JOSM. See the detailed help on [conflict resolution](../en/Conflict%20resolution.md).  

## 使用 GPS

您可用 Vespucci 建立 GPX 軌跡和顯示在您的裝置上。進一步此外，您可顯示目前 GPS 位置，(在 GPS 選單中設定"顯示位置")具有螢幕中心周圍和追隨的位置，(在 GPS 選單中設定"追隨 GPS 位置")。 

If you have the latter set, moving the screen manually or editing will cause the "follow GPS" mode to be disabled and the blue GPS arrow will change from an outline to a filled arrow. To quickly return to the "follow" mode, simply touch GPS button or re-check the menu option.

## 備註和錯誤

Vespucci支援下載、回應或是關閉OSM註解(先前叫做OSM臭蟲)，相當於[OSMOSE 品質管控工具](http://osmose.openstreetmap.fr/en/map/)列出的"臭蟲"。兩者都可以完整下載下來，或者使用自動下載工具看到你的區域內的註解。一旦你回應或是關閉註解，你可以上傳單一臭蟲還是註解，或是一次上傳多個註解。

On the map the Notes and bugs are represented by a small bug icon ![Bug](../images/bug_open.png), green ones are closed/resolved, blue ones have been created or edited by you, and yellow indicates that it is still active and hasn't been changed. 

OSMOSE臭蟲則會將受影響的物件顯示為藍色連結，碰觸連結則會選取物件，並且螢幕置中，如果需要則會下載周邊的區域。 

### 篩選

Besides globally enabling the notes and bugs display you can set a coarse grain display filter to reduce clutter. In the "Advanced preferences" you can individually select:

* Notes
* Osmose error
* Osmose warning
* Osmose minor issue

<a id="indoor"></a>

## 室內模式

Mapping indoors is challenging due to the high number of objects that very often will overlay each other. Vespucci has a dedicated indoor mode that allows you to filter out all objects that are not on the same level and which will automatically add the current level to new objects created their.

The mode can be enabled by long pressing on the lock item, see [Lock, unlock, "tag editing only", indoor mode](#lock).

## 篩選

### 標籤為依據的篩選

The filter can be enabled from the main menu, it can then be changed by tapping the filter icon. More documentation can be found here [Tag filter](../en/Tag%20filter.md).

預置為依據的篩選

An alternative to the above, objects are filtered either on individual presets or on preset groups. Tapping on the filter icon will display a preset selection dialog similar to that used elsewhere in Vespucci. Individual presets can be selected by a normal click, preset groups by a long click (normal click enters the group). More documentation can be found here [Preset filter](../en/Preset%20filter.md).

## 客製化 Vespucci

### 設定，當您可能想要更改

* Background layer
* Overlay layer. Adding an overlay may cause issues with older devices and such with limited memory. Default: none.
* Notes/Bugs display. Open Notes and bugs will be displayed as a yellow bug icon, closed ones the same in green. Default: on.
* Photo layer. Displays georeferenced photographs as red camera icons, if direction information is available the icon will be rotated. Default: off.
* Node icons. Default: on.
* Keep screen on. Default: off.
* Large node drag area. Moving nodes on a device with touch input is problematic since your fingers will obscure the current position on the display. Turning this on will provide a large area which can be used for off-center dragging (selection and other operations still use the normal touch tolerance area). Default: off.

進階參數選項

* Always show context menu. When turned on every selection process will show the context menu, turned off the menu is displayed only when no unambiguous selection can be determined. Default: off (used to be on).
* Enable light theme. On modern devices this is turned on by default. While you can enable it for older Android versions the style is likely to be inconsistent.
* Show statistics. Will show some statistics for debugging, not really useful. Default: off (used to be on).  

## 回報問題

If Vespucci crashes, or it detects an inconsistent state, you will be asked to send in the crash dump. Please do so if that happens, but please only once per specific situation. If you want to give further input or open an issue for a feature request or similar, please do so here: [Vespucci issue tracker](https://github.com/MarcusWolschon/osmeditor4android/issues). If you want to discuss something related to Vespucci, you can either start a discussion on the [Vespucci Google group](https://groups.google.com/forum/#!forum/osmeditor4android) or on the [OpenStreetMap Android forum](http://forum.openstreetmap.org/viewforum.php?id=56)


