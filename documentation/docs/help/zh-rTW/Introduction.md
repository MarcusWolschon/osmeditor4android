# Vespucci 介紹

Vespucci 是一個完整功能的開放街圖編輯器，支援那些桌面系統編輯器所提供的大部分的操作。它在 googles Android 2.3 至 6.0 以及各種 AOSP 為主轉化的。已經過成功地測試。提醒一句：行動裝置能力已經追上桌面系統與之匹敵，尤其是較舊的裝置，只有非常有限的可用記憶體，並且往往是相當緩慢的。在使用  Vespucci 和持有時，您應該考慮到與接受，例如，您正在編輯的區域尺寸是一個合理的大小。 

## 第一次使用

在啟動 Vespucci 時為您顯示 "下載其它位置"/"載入區域" 的對話框。如果您有座標顯示並且想要立即下載，您可以選擇合適的選項，和設定想要的下載半徑週圍位置，請不要在緩慢的裝置上選擇大的面積。 

除此之外您可以透過按按鈕解除對話框，並平移與放大位置到想要編輯和下載的資料(見下方："編輯于 Vespucci")。

## 編輯于 Vespucci

根據畫面尺寸和您裝置的新舊，編輯操作可以經由在頂端列的圖示可直接進入，通過下拉選單中頂端列的右側，從底部列(如果存在的話) ，或者透過選單鍵。

### 下載 OSM 資料

選擇任何一個的傳輸圖示！[](../images/menu_transfer.png)或“傳輸”選單項目都可以。這將顯示七個選項：

 * **下載現在的檢視** - 下載在螢幕上可見的區域，並取代所有目前的資料*(需要網路連線)* 
* **增加現在的檢視來下載** 下載在螢幕上可見的區域，並合併目前的資料*(需要網路連線)* 
* **下載其它地方** - 顯示一個表單，允許您輸入座標搜尋位置或直接使用現在位置，然後下載該區域週圍的地方*(需要網路連線)* 
* **上傳資料到 OSM 伺服器** - 上傳編輯到 OpenStreetMap *(需要網路連線)*
* **自動下載** - 自動的下載目前周圍位置區域 *(需要網路連線)*  *(需要 GPS)*
* **檔案...** - 儲存和載入在裝置的 OSM 檔案資料。
* **備註/錯誤** -從 QA 工具 (目前的 OSMOSE) 下載 (自動或手動) OSM 備註和"錯誤" *(需要網路連線)*

The easiest way to download data to the device is to zoom and pan to the location you want to edit and then to select "Download current view". You can zoom by using gestures, the zoom buttons or the volume control buttons on the telephone.  Vespucci should then download data for the current view. No authentication is required for downloading data to your device.

### 編輯

To avoid accidental edits Vespucci starts in "locked" mode, a mode that only allows zooming and moving the map. Tap the ![Locked](../images/locked.png) icon to unlock the screen. A long press on the lock icon will enable "Tag editing only" mode which will not allow you to create new objects or edit the geometry of objects, this mode is indicated with a slightly different white lock icon.

By default, selectable nodes and ways have an orange area around them indicating roughly where you have to touch to select an object. If you try to select an object and Vespucci determines that the selection could mean multiple objects it will present a selection menu. Selected objects are highlighted in yellow.

如果您試著編輯高密度區域時進行放大，這是一個很好的對策。

Vespucci 擁有一個良好的"取消/重做"系統，所以不要害怕在您的裝置嘗試，但是請不要上傳和儲存純測試資料。

#### 選擇/取消選擇

Touch an object to select and highlight it, a second touch on the same object opens the tag editor on the element. Touching the screen in an empty region will de-select. If you have selected an object and you need to select something else, simply touch the object in question, there is no need to de-select first. A double tap on an object will start [Multiselect mode](../en/Multiselect.md).

#### 增加新節點/點或是路徑

在您想要的那個節點或開始的道路長按。您將會看到一個黑色的"十字"符號。再次輕觸相同的位置可建立一個新的節點，觸碰位置為容許偏差範圍之外的接觸位置，那將會從原本的地點到目前的位置增加一段道路。 

Simply touch the screen where you want to add further nodes of the way. To finish, touch the final node twice. If the initial and  end nodes are located on a way, they will be inserted into the way automatically.

#### 移動節點或路徑

只有當物件被選中時可以將它們拖曳或移動。如果您在參數中選擇大面積的拖曳區域，您會得到大面積區域周圍的選取節點，使得它更容易到物件的位置。 

#### 改善道路的幾何形狀

如果您放大到足夠遠的程度，在足夠長的道路段中間，您會看到這樣一個小"x"。拖曳“×”，會建立一個在該位置的節點。注意：為了防止意外的建立節點，此操作的觸碰容差是相當小的。

#### 剪下、複製和貼上

您可以複製或剪下選擇的節點和路徑，然後貼上一次或多次到一個新的位置。剪切將保留 OSM ID 和版本。要貼上長按要貼上的位置(您會看到一個十字線標記的位置)。然後從選單中選擇“貼上”。

#### 有效的增加地址

Vespucci 有一個"增加地址標籤"功能，那讓勘察地址更有效率。它是可以選擇的 

* after a long press: Vespucci will add a node at the location and make a best guess at the house number and add address tags that you have been lately been using. If the node is on a building outline it will automatically add a "entrance=yes" tag to the node. The tag editor will open for the object in question and let you make any further changes.
* in the node/way selected modes: Vespucci will add address tags as above and start the tag editor.
* in the tag editor.

門牌號碼預測，一般需求要在道路的兩側，至少兩間房屋號碼需要輸入到作業中，更多的號碼存在於資料中越好。

考量使用這種"自動下載"模式。  

#### 增加轉​​彎限制

Vespucci 有個快速的增加轉向限制。注意：如果您為了限制而需要分割一條道路，您須要在開始之前做到這點。

* select a way with a highway tag (turn restrictions can only be added to highways, if you need to do this for other ways, please use the generic "create relation" mode, if there are no possible "via" elements the menu item will also not display)
* select "Add restriction" from the menu
* select the "via" node or way (all possible "via" elements will have the selectable element highlighting)
* select the "to" way (it is possible to double back and set the "to" element to the "from" element, Vespucci will assume that you are adding an no_u_turn restriction)
* set the restriction type in the tag menu

### Vespucci in "locked" mode

When the red lock is displayed all non-editing actions are available. Additionally a long press on or near to an object will display the detail information screen if it is an OSM object.

### 儲存您的變更

*(需要網路連線)*

選擇您在下載時相同的按鈕或選單項目，現在選擇 "上傳資料到 OSM 的伺服器"。

Vespucci 除了支援 OAuth 的授權和標準的使用者名稱與密碼的方式。 而 OAuth 更好，因為它避免了以明文發送密碼。

New Vespucci installs will have OAuth enabled by default. On your first attempt to upload modified data, a page from the OSM website loads. After you have logged on (over an encrypted connection) you will be asked to authorize Vespucci to edit using your account. If you want to or need to authorize the OAuth access to your account before editing there is a corresponding item in the "Tools" menu.

如果您想要儲存您的工作，並且不能連入網際網路，您可以儲存成 JOSM 相容的 .osm 檔案，以後以 Vespucci 或 JOSM 任何一個上載。 

#### 在上傳解決衝突

Vespucci has a simple conflict resolver. However if you suspect that there are major issues with your edits, export your changes to a .osc file ("Export" menu item in the "Transfer" menu) and fix and upload them with JOSM. See the detailed help on [conflict resolution](../en/Conflict resolution.md).  

## 使用 GPS

您可用 Vespucci 建立 GPX 軌跡和顯示在您的裝置上。進一步此外，您可顯示目前 GPS 位置，(在 GPS 選單中設定"顯示位置")具有螢幕中心周圍和追隨的位置，(在 GPS 選單中設定"追隨 GPS 位置")。 

如果您有設定，當用手移動螢幕或編輯將會導致停用"追隨 GPS" 模式，藍色的 GPS 箭頭將從空心變為實心箭頭。要快速的返回到"追隨"模式，只需觸碰箭頭或重新從選單中確認選項。

## 備註和錯誤

Vespucci supports downloading, commenting and closing of OSM Notes (formerly OSM Bugs) and the equivalent functionality for "Bugs" produced by the [OSMOSE quality assurance tool](http://osmose.openstreetmap.fr/en/map/). Both have to either be downloaded explicitly or you can use the auto download facility to access the items in your immediate area. Once edited or closed, you can either upload the bug or Note immediately or upload all at once.

On the map the Notes and bugs are represented by a small bug icon ![](../images/bug_open.png), green ones are closed/resolved, blue ones have been created or edited by you, and yellow indicates that it is still active and hasn't been changed. 

The OSMOSE bug display will provide a link to the affected object in blue, touching the link will select the object, center the screen on it and down load the area beforehand if necessary. 

## 定制 Vespucci

### 設定，當您可能想要更改

* Background layer
* Overlay layer. Adding an overlay may cause issues with older devices and such with limited memory. Default: none.
* Notes/Bugs display. Open Notes and bugs will be displayed as a yellow bug icon, closed ones the same in green. Default: on.
* Photo layer. Displays georeferenced photographs as red camera icons, if direction information is available the icon will be rotated. Default: off.
* Node icons. Default: on.
* Keep screen on. Default: off.
* Large node drag area. Moving nodes on a device with touch input is problematic since your fingers will obscure the current position on the display. Turning this on will provide a large area which can be used for off-centre dragging (selection and other operations still use the normal touch tolerance area). Default: off.

進階參數選項

* Enable split action bar. On recent phones the action bar will be split in a top and bottom part, with the bottom bar containing the buttons. This typically allows more buttons to be displayed, however does use more of the screen. Turning this off will move the buttons to the top bar. note: you need to restart Vespucci for the change to take effect.
* Always show context menu. When turned on every selection process will show the context menu, turned off the menu is displayed only when no unambiguous selection can be determined. Default: off (used to be on).
* Enable light theme. On modern devices this is turned on by default. While you can enable it for older Android versions the style is likely to be inconsistent.
* Show statistics. Will show some statistics for debugging, not really useful. Default: off (used to be on).  

## 回報問題

如果 Vespucci 崩潰，或檢測到不一致的狀態，將會要求您發送失敗傾印。如果出現這種情況，請送出失敗傾印，但是請在每個特定的情況只要一次。如果您希望進一步的投入或對功能要求的開放議題或著類似的，請在這裡這樣做：[ Vespucci 問題追蹤](https://github.com/MarcusWolschon/osmeditor4android/issues)。如果您想討論與 Vespucci 相關的東西，您也可以在 [Vespucci google 群組](https://groups.google.com/forum/#!forum/osmeditor4android) 或是在[OpenStreetMap Android 論壇](http://forum.openstreetmap.org/viewforum.php?id=56)任何一個裡開始討論。


