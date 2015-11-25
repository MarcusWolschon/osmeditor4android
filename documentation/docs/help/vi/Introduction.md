# Giới thiệu về Vespucci

Vespucci là một chương trình sửa đổi OpenStreetMap có chức năng đầy đủ, kể cả các tác vụ đuợc hỗ trợ bởi các chương trình sửa đổi dành cho máy tính để bàn. Nó đã được kiểm thử thành công trong Android của Google từ 2.3 cho tới 6.0 cũng như một số biến thể gốc AOSP. Xin cẩn thận: tuy các thiết bị di động có khả năng gần như bằng với các máy tính để bàn, các thiết bị di động, nhất là các thiết bị cũ hơn, có bộ nhớ hạn chế và chạy tương đối chậm chạp. Bạn nên nghĩ đến điều này trong việc sử dụng Vespucci và chẳng hạn cố gắng sửa đổi những khu vực có kích thước hợp lý. 

## Lần sử dụng đầu tiên

On startup Vespucci shows you the "Download other location"/"Load Area" dialog. If you have coordinates displayed and want to download immediately, you can select the appropriate option and set the radius around the location that you want to download. Do not select a large area on slow devices. 

Alternatively you can dismiss the dialog by pressing the "Go to map" button and pan and zoom to a location you want to edit and download the data then (see below: "Editing with Vespucci").

## Sửa đổi bằng Vespucci

Tùy theo kích thước màn hình và thế hệ của thiết bị, các tác vụ sửa đổi có thể được truy cập trực tiếp qua các hình tượng trên thanh trên, qua một trình đơn thả xuống vào bên phải của thanh trên, từ thanh dưới (nếu có), hoặc bằng cách bấm phím trình đơn.

### Tải về dữ liệu OSM

Chọn hình tượng truyền ![](../images/menu_transfer.png) hoặc mục “Truyền” trong trình đơn. Bảy mục sẽ xuất hiện:

* **Download current view** - download the area visible on the screen and replace any existing data *(requires network connectivity)*
* **Add current view to download** - download the area visible on the screen and merge it with existing data *(requires network connectivity)*
* **Download other location** - shows a form that allows you to enter coordinates, search for a location or use the current position, and then download an area around that location *(requires network connectivity)*
* **Upload data to OSM server** - upload edits to OpenStreetMap *(requires authentication)* *(requires network connectivity)*
* **Auto download** - download an area around the current location automatically *(requires network connectivity)* *(requires GPS)*
* **File...** - saving and loading OSM data to/from on device files.
* **Note/Bugs...** - download (automatically and manually) OSM Notes and "Bugs" from QA tools (currently OSMOSE) *(requires network connectivity)*

The easiest way to download data to the device is to zoom and pan to the location you want to edit and then to select "Download current view". You can zoom by using gestures, the zoom buttons or the volume control buttons on the telephone.  Vespucci should then download data for the current view. No authentication is required for downloading data to your device.

### Sửa đổi

To avoid accidental edits Vespucci starts in "locked" mode, a mode that only allows zooming and moving the map. Tap the ![Locked](../images/locked.png) icon to unlock the screen. A long press on the lock icon will enable "Tag editing only" mode which will not allow you to create new objects or edit the geometry of objects, this mode is indicated with a slightly different white lock icon.

By default, selectable nodes and ways have an orange area around them indicating roughly where you have to touch to select an object. If you try to select an object and Vespucci determines that the selection could mean multiple objects it will present a selection menu. Selected objects are highlighted in yellow.

It is a good strategy to zoom in if you attempt to edit a high density area.

Vespucci có đầy đủ chức năng hoàn tác/làm lại – đừng có sợ thử nghiệm trên thiết bị của bạn. Tuy nhiên, xin vui lòng đừng tải lên và lưu dữ liệu thử nghiệm.

#### Chọn / bỏ chọn

Touch an object to select and highlight it, a second touch on the same object opens the tag editor on the element. Touching the screen in an empty region will de-select. If you have selected an object and you need to select something else, simply touch the object in question, there is no need to de-select first. A double tap on an object will start [Multiselect mode](../en/Multiselect.md).

#### Thêm nốt/địa điểm hoặc lối

Long press where you want the node to be or the way to start. You will see a black "cross hairs" symbol. Touching the same location again creates a new node, touching a location outside of the touch tolerance zone will add a way segment from the original position to the current position. 

Simply touch the screen where you want to add further nodes of the way. To finish, touch the final node twice. If the initial and  end nodes are located on a way, they will be inserted into the way automatically.

#### Di chuyển nốt hoặc lối

Objects can be dragged/moved only when they are selected. If you select the large drag area in the preferences, you get a large area around the selected node that makes it easier to position the object. 

#### Cải thiện hình dạng lối

If you zoom in far enough you will see a small "x" in the middle of way segments that are long enough. Dragging the "x" will create a node in the way at that location. Note: to avoid accidentally creating nodes, the touch tolerance for this operation is fairly small.

#### Cắt, sao chép, dán

You can copy or cut selected nodes and ways, and then paste once or multiple times to a new location. Cutting will retain the osm id and version. To paste long press the location you want to paste to (you will see a cross hair marking the location). Then select "Paste" from the menu.

#### Ghi địa chỉ một cách tiện lợi

Vespucci có chức năng “thêm thẻ địa chỉ” để làm tiện việc lấy các địa chỉ. Để sử dụng chức năng này: 

* nhấn giữ: Vespucci sẽ đặt một nốt vào vị trí, cố gắng đoán ra số nhà, và thêm các thẻ địa chỉ mà bạn đã sử dụng nhiều gần đây. Nếu nốt nằm trên đường nét tòa nhà, thẻ “entrance=yes” sẽ được tự động thêm vào nốt. Trình sửa đổi thẻ sẽ mở lên để cho bạn chỉnh lại đối tượng.
* chọn nốt hoặc lối: Vespucci sẽ thêm thẻ địa chỉ như bên trên và mở trình sửa đổi thẻ.
* mở trình sửa đổi thẻ.

Để đoán ra số nhà, thường phải có mỗi bên đường là ít nhất hai số nhà, càng thêm số nhà càng chính xác hơn.

Hãy thử sử dụng chức năng này trong chế độ “Tự động tải về”.  

#### Thêm hạn chế rẽ

Vespucci cho phép ghi hạn chế rẽ một cách nhanh nhẹn. Lưu ý: nếu cần cắt đôi một lối được hạn chế, bạn cần phải cắt đôi nó trước khi ghi hạn chế.

* select a way with a highway tag (turn restrictions can only be added to highways, if you need to do this for other ways, please use the generic "create relation" mode, if there are no possible "via" elements the menu item will also not display)
* select "Add restriction" from the menu
* select the "via" node or way (all possible "via" elements will have the selectable element highlighting)
* select the "to" way (it is possible to double back and set the "to" element to the "from" element, Vespucci will assume that you are adding an no_u_turn restriction)
* set the restriction type in the tag menu

### Vespucci trong chế độ “khóa”

When the red lock is displayed all non-editing actions are available. Additionally a long press on or near to an object will display the detail information screen if it is an OSM object.

### Lưu các thay đổi của bạn

*(cần kết nối mạng)*

Select the same button or menu item you did for the download and now select "Upload data to OSM server".

Vespucci supports OAuth authorization and the classical username and password method. OAuth is preferable since it avoids sending passwords in the clear.

New Vespucci installs will have OAuth enabled by default. On your first attempt to upload modified data, a page from the OSM website loads. After you have logged on (over an encrypted connection) you will be asked to authorize Vespucci to edit using your account. If you want to or need to authorize the OAuth access to your account before editing there is a corresponding item in the "Tools" menu.

If you want to save your work and do not have Internet access, you can save to a JOSM compatible .osm file and either upload later with Vespucci or with JOSM. 

#### Giải quyết mâu thuẫn khi tải lên

Vespucci has a simple conflict resolver. However if you suspect that there are major issues with your edits, export your changes to a .osc file ("Export" menu item in the "Transfer" menu) and fix and upload them with JOSM. See the detailed help on [conflict resolution](../en/Conflict resolution.md).  

## Sử dụng GPS

You can use Vespucci to create a GPX track and display it on your device. Further you can display the current GPS position (set "Show location" in the GPS menu) and/or have the screen center around and follow the position (set "Follow GPS Position" in the GPS menu). 

If you have the later set, moving the screen manually or editing will cause the "follow GPS" mode to be disabled and the blue GPS arrow will change from an outline to a filled arrow. To quickly return to the "follow" mode, simply touch the arrow or re-check the option from the menu.

## Ghi chú và lỗi

Vespucci supports downloading, commenting and closing of OSM Notes (formerly OSM Bugs) and the equivalent functionality for "Bugs" produced by the [OSMOSE quality assurance tool](http://osmose.openstreetmap.fr/en/map/). Both have to either be downloaded explicitly or you can use the auto download facility to access the items in your immediate area. Once edited or closed, you can either upload the bug or Note immediately or upload all at once.

On the map the Notes and bugs are represented by a small bug icon ![](../images/bug_open.png), green ones are closed/resolved, blue ones have been created or edited by you, and yellow indicates that it is still active and hasn't been changed. 

The OSMOSE bug display will provide a link to the affected object in blue, touching the link will select the object, center the screen on it and down load the area beforehand if necessary. 

## Tùy chỉnh Vespucci

### Các thiết lập có thể muốn thay đổi

* Background layer
* Overlay layer. Adding an overlay may cause issues with older devices and such with limited memory. Default: none.
* Notes/Bugs display. Open Notes and bugs will be displayed as a yellow bug icon, closed ones the same in green. Default: on.
* Photo layer. Displays georeferenced photographs as red camera icons, if direction information is available the icon will be rotated. Default: off.
* Node icons. Default: on.
* Keep screen on. Default: off.
* Large node drag area. Moving nodes on a device with touch input is problematic since your fingers will obscure the current position on the display. Turning this on will provide a large area which can be used for off-centre dragging (selection and other operations still use the normal touch tolerance area). Default: off.

#### Tùy chỉnh nâng cao

* Enable split action bar. On recent phones the action bar will be split in a top and bottom part, with the bottom bar containing the buttons. This typically allows more buttons to be displayed, however does use more of the screen. Turning this off will move the buttons to the top bar. note: you need to restart Vespucci for the change to take effect.
* Always show context menu. When turned on every selection process will show the context menu, turned off the menu is displayed only when no unambiguous selection can be determined. Default: off (used to be on).
* Enable light theme. On modern devices this is turned on by default. While you can enable it for older Android versions the style is likely to be inconsistent.
* Show statistics. Will show some statistics for debugging, not really useful. Default: off (used to be on).  

## Báo cáo lỗi

If Vespucci crashes, or it detects an inconsistent state, you will be asked to send in the crash dump. Please do so if that happens, but please only once per specific situation. If you want to give further input or open an issue for a feature request or similar, please do so here: [Vespucci issue tracker](https://github.com/MarcusWolschon/osmeditor4android/issues). If you want to discuss something related to Vespucci, you can either start a discussion on the [Vespucci google group](https://groups.google.com/forum/#!forum/osmeditor4android) or on the [OpenStreetMap Android forum](http://forum.openstreetmap.org/viewforum.php?id=56)


