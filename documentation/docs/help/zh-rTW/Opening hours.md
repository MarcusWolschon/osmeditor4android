# 開放街圖營業時間編輯器

OpenStreetMap 營業時間規範相當複雜，並不易於一個簡單直觀的使用者介面。

然而大部分的時候，你應該只會用到一小部分的定議。編輯器嘗試在選單中隱藏更加模糊的功能，來達成這一點，大部分的時候減少路上設定，儘可能用預先設定的模版，加以小量修改。

_這份文件是初步的並且正在進展_

## 使用營業時間編輯器

在一般的工作流程中，你編輯的物件一般已經有開放時間的標籤 (opening_hour、service_times 和 colllection_times)，或是你可以重新設定物件的預設組合，用空白的開放時間欄位。如果你需要手動輸入欄位，像是用 Vespucci，你可以在詳情頁面輸入鍵值，之後切換回欄位頁籤繼續編輯。如果你相信開放時間標籤必須扅於預設組合的話，請在你的編輯器開啟 issue。

If you have defined a default template (do this via the "Manage templates" menu item) it will be loaded automatically when the editor is started with an empty value. With the "Load template" function you can load any saved template and with the "Save template" menu you can save the current value as a template. You can define separate templates and defaults for the "opening_hours", "collection_times" and "service_times" tags. Further you can limit applicability of a template to a region and a specific identifier, typically an OSM top-level tap (for example amenity=restaurant). 

當然，您可以從頭開始構建營業時間的值，但我們建議使用現有的模板之一作為起點。

如果已經有營業時間的數值則會載入，另外會自動修正以符合營業時間規範。如果無法修正，則會在大概的位置顯示錯誤訊息，原始的營業時間數值，等待人工修正。開放街圖資料庫中有大約1/4的營業時間數值有問題，但只有少於10%的狀況是無法修正。詳見 [OpeningHoursParser](https://github.com/simonpoole/OpeningHoursParser)，並且看有那些允許的規範變體。

### 主選單按鈕

* __增加規則__：增加一個新規則。
* __增加假日規則__：國家改變時，為假日增加新的規則。
* __為 24/7 增加規則__：為總是開放的物件增加規則，開放時間規範不支持任何其他子值為24/7，但我們允許增加更高階的選擇器 (例如年份範圍)。
* __載入模板__：載入先有的模板。
* __儲存至模板__：將現有的營業時間數值儲存為模板留待日後使用。
* __管理模板__：編輯（如變更名稱）或刪除現有模板
* __重新整理__：重新解析營業時間的數值。
* __全部刪除__：刪除全部規則。

### 規則

預設規則會依 _normal_ rules 增加，意味將直接套用並且覆寫同一天原先規則的數值。這可能會為套用延伸時間規則帶來困擾，特別是你希望透過 _顯示規則類別_ 選單  _追加_ 轉換規則。

#### 規則選單

* __增加修飾符/註釋__：更改此規則的效果並增加可選擇的註釋。
* __增加假日__：為公衆假期或學校假期增加選取器。
* __增加時間跨度 ...__
    * __時間 - 時間__：同一天的開始時間到結束時間。
    * __時間 - 延長時間__：開始時間跨到隔天的結束時間 (例如 26:00 是隔天的 02:00 (am))。
    * __變化時間 - 時間__：從開始變化時間 (黎明，黃昏，日出和日落) 到同一天的結束時間。
    * __變化時間 -  延長時間__：從第二天起始變化時間到隔天的結束時間。
    * __時間 - 變化時間__：起啟時間到結束的變化時間
    * __變化時間 - 變化時間__：從起始變化時間到結束變化時間。
    * __時間__：特定的時間。
    * __時間-一直持續__：從起始時間一直持續。
    * __變化時間__：特定的變化時間
    * __變化時間 - 一直持續__：從變化時間到一直持續
* __增加週間範圍__：增加以週間為依據的選擇器。
* __增加日期範圍...__
    * __日期 - 日期__：從開始日期 (年、月、日) 到結束日期。
    * __變化日期 - 日期__：從起始變化日期 (目前規範僅定義_復活節_) 到結束日期。
    * __日期 - 變化日期__：從開始日期到變化日期。
    * __變化日期 - 變化日期__：從起始變化日期到結束變化日期。
    * __發生在月中-發生在月中__：一個月中從開始的週間是相同的。
    * __發生在月中 - 日期__：一個月中週間發生，到特定結束日期。
    * __日期 - 發生在月中__：從開始日舒到一個月中的週間結束。
    * __發生在月中 - 變動日期__：從一個月開始週間發生，到變動日期。
    * __變動日期 - 發生在月中__：從開始的變動日期，到一個月中的週間。
    * __日期 - 開放結束__：從開始日期起。
    * __日期變化 - 開端__：從起始變化日期開始。
    * __發生在月中 - 一直持續__：從一個月中開始週間發生。
    * __使用偏差量...__：與上述相同的內容，但是指定了偏差量 (這很少使用)。
* __Add year range...__    
    * __增加年範圍__：增加以年為依據的選擇器。
    * __Add starting year__: add an open ended year range.
* __增加週範圍__：增加以週為依據的選擇器。
* __複製__：複製此規則，並且插入目前位置之後。 
* __顯示規則類型__：顯示並允許更改規則類型_正常_，_增加_和_倒退_(位於第一條規則中不可用)。
* __向上移動__：將此規則向上移動一個位置(位於第一條規則時不可用)。
* __向下移動__：將此規則向下移動一個位置。
* __刪除__：刪除此一規則。

### 時間跨度

要讓編輯時間刻度變得更簡單可行，我們嘗試選擇最佳時間範圍，加載現有值時範圍欄的刻度。對新的時間刻度來說，欄從6:00(am)開始，然後每15分鐘增加。上述的設定可以在選單更改。

當你直接用時間軸太困難時，點 (不是在別針上) 在時間軸上，會打開比較大的時間檢取器。時間檢取器會延伸到下一天，所有簡單的方式延長時間區間，但不用刪除再加區間。

#### 時間跨度選單

* __顯示時間檢取器__：顯示更大的時間檢取器來開始和結束時間，在非常小的裝置上，則有更好的方式改變時間。
* __切換到15分鐘刻度__：對範圍欄使用15分鐘刻度。
* __切換到5分鐘刻度__：對範圍欄使用5分鐘的刻度。
* __切換到1分鐘刻度__：對範圍欄使用1分鐘的刻度，很難在手機上使用。
* __從午夜開始__：午夜開始範圍欄。
* __顯示間隔__：顯示指定間隔（以分鐘為單位）的間隔字段。
* __刪除__：刪除此一時間跨度。

### 管理模版

The template management dialog allows you to add, edit and delete templates.

In Android 4.4 and later the following additional functionality is available from the menu button. 

* __Show all__: display all templates in the database.
* __Save to file__: write the contents of the template database to a file.
* __Load from file (replace)__: load templates from a file replacing the current contents of the database.
* __Load from file__: load templates from a file retaining the current contents.
