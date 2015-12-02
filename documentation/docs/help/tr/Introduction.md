# Vespucci Giriş

Vespucci is a full featured OpenStreetMap editor that supports most operations that desktop editors provide. It has been tested successfully on googles Android 2.3 to 6.0 and various AOSP based variants. A word of caution: while mobile devices capabilities have caught up with their desktop rivals, particularly older devices have very limited memory available and tend to be rather slow. You should take this in to account when using Vespucci and keep, for example, the size of the areas you are editing to a reasonable size. 

## İlk kullanım

Başlangıçta Vespucci size "Başka konumu indir"/"Alanı Yükle" diyaloğu gösterir. Görütülediğiniz koordinatları hemen indirmek istiyorsanız, uygun seçeneği seçebilir indirmek istediğiniz alanın yarıçapını ayarlayabilirsiniz. Yavaş cihazlarda geniş alanlar seçmeyin. 

Alternatif olarak bu diyaloğu atlayıp "Haritaya git" düğmesine basın ve düzenlemek istediğiniz alanı yaklaştırıp verileri indirin. (aşağıya bkz: "Vespucci ile Düzenleme")

## Vespucci ile düzenleme

Cihazınızın yaşına ve ekran boyutuna bağlı olarak düzenleme menüsüne üst bardaki simgelerden, üst barın sağındaki açılır menüden, (eğer varsa) alttaki bardan veya menü tuşundan erişebilirsiniz.

### OSM Verisini indirme

Transfer simgesini ![](../images/menu_transfer.png) veya transfer menü ögesini seçin . Yedi seçenek sunacaktır:

* **Download current view** - download the area visible on the screen and replace any existing data *(requires network connectivity)*
* **Add current view to download** - download the area visible on the screen and merge it with existing data *(requires network connectivity)*
* **Download other location** - shows a form that allows you to enter coordinates, search for a location or use the current position, and then download an area around that location *(requires network connectivity)*
* **Upload data to OSM server** - upload edits to OpenStreetMap *(requires authentication)* *(requires network connectivity)*
* **Auto download** - download an area around the current location automatically *(requires network connectivity)* *(requires GPS)*
* **File...** - saving and loading OSM data to/from on device files.
* **Note/Bugs...** - download (automatically and manually) OSM Notes and "Bugs" from QA tools (currently OSMOSE) *(requires network connectivity)*

The easiest way to download data to the device is to zoom and pan to the location you want to edit and then to select "Download current view". You can zoom by using gestures, the zoom buttons or the volume control buttons on the telephone.  Vespucci should then download data for the current view. No authentication is required for downloading data to your device.

### Düzenleme

To avoid accidental edits Vespucci starts in "locked" mode, a mode that only allows zooming and moving the map. Tap the ![Locked](../images/locked.png) icon to unlock the screen. A long press on the lock icon will enable "Tag editing only" mode which will not allow you to create new objects or edit the geometry of objects, this mode is indicated with a slightly different white lock icon.

By default, selectable nodes and ways have an orange area around them indicating roughly where you have to touch to select an object. If you try to select an object and Vespucci determines that the selection could mean multiple objects it will present a selection menu. Selected objects are highlighted in yellow.

Yüksek yoğunluklu bir yeri düzenlemek için yaklaştırmak iyi bir yoldur.

Vespucci'nin iyi bir geri al/yinele sistemi vardır cihazınızda bunu denemekten korkmayın, ancak lütfen deneme verilerini kaydedip sunucuya yüklemeyin.

#### Seçme / Bırakma

Touch an object to select and highlight it, a second touch on the same object opens the tag editor on the element. Touching the screen in an empty region will de-select. If you have selected an object and you need to select something else, simply touch the object in question, there is no need to de-select first. A double tap on an object will start [Multiselect mode](../en/Multiselect.md).

#### Yeni Düğüm/Nokta veya yol ekleme

Düğüm oluşturmak veya yol başlatmak için istediğiniz yere uzun dokunun. "Çapraz kıllar" sembolü göreceksiniz. Aynı yere tekrar dokunmak yeni düğüm oluşturur, dokunma toleransı dışında bir yere dokunmak orjinal konumdan geçerli konuma bir yol bölümü ekler. 

Simply touch the screen where you want to add further nodes of the way. To finish, touch the final node twice. If the initial and  end nodes are located on a way, they will be inserted into the way automatically.

#### Yolu veya Düğümü taşıma

Nesneler sadece seçildiklerinde taşınabilir/sürüklenebilir. Eğer seçeneklerden geniş sürükleme alanını seçerseniz, sürüklemek için daha geniş bir alana sahip olursunuz ve bu işlemi daha kolay gerçekleştirirsiniz. 

#### Yolun Geometrisini Geliştirme

Eğer yeterince yaklaştırırsanız yeterince uzun yolların ortasında ufak bir "x" simgesi göreceksiniz. "x" simgesini sürüklemek o konumda bir
düğüm oluşturacaktır. Not: yanlışlıkla düğüm oluşturmayı önlemek için dokunma toleransı bu işlem için oldukça düşüktür.

#### Kes, Kopyala & Yapıştır

Seçilen düğümleri ve yolları kopyalayabilir veya kesebilir, bir veya daha fazla kez yeni konumlara yapıştırabilirsiniz. Kesme işlemi osm id ve sürümünü koruyacaktır. Yapıştırmak istediğiniz konuma uzun dokunun (konumda çapraz kıl işareti göreceksiniz). Ardından menüden "Yapıştır"'ı seçin.

#### Verimli Adres Ekleme

Vespucci'nin keşfedilen alanları daha verimli eklemek için çalışan bir "adres etiketi ekleme" özelliği vardır. Buradan seçebilirsiniz 

* after a long press: Vespucci will add a node at the location and make a best guess at the house number and add address tags that you have been lately been using. If the node is on a building outline it will automatically add a "entrance=yes" tag to the node. The tag editor will open for the object in question and let you make any further changes.
* in the node/way selected modes: Vespucci will add address tags as above and start the tag editor.
* in the tag editor.

Ev numarası tahmini genelde en az 2 numara gerektirir çalışmaya yolun her iki tarafıda girilebilir, verilerde ne kadar çok numara olursa o kadar iyidir.

Bunu "Oto-indir" modunda kullanmayı düşünün.  

#### Dönüş Kısıtlamaları Ekleme

Vespucci'nin dönüş kısıtlaması eklemek için hızlı bir yöntemi vardır. Not: Eğer kısıtlama için yolu bölmek gerekiyorsa lütfen bunu başlamadan yapın.

* select a way with a highway tag (turn restrictions can only be added to highways, if you need to do this for other ways, please use the generic "create relation" mode, if there are no possible "via" elements the menu item will also not display)
* select "Add restriction" from the menu
* select the "via" node or way (all possible "via" elements will have the selectable element highlighting)
* select the "to" way (it is possible to double back and set the "to" element to the "from" element, Vespucci will assume that you are adding an no_u_turn restriction)
* set the restriction type in the tag menu

### Vespucci in "locked" mode

When the red lock is displayed all non-editing actions are available. Additionally a long press on or near to an object will display the detail information screen if it is an OSM object.

### Değişenleri Kaydetmek

*(ağ bağlantısı gerekir)*

İndirmek için aynı butonu veya menüyü seçin ve ardından "Verileri OSM sunucusuna yükle"'yi seçin

Vespucci supports OAuth authorization and the classical username and password method. OAuth is preferable since it avoids sending passwords in the clear.

New Vespucci installs will have OAuth enabled by default. On your first attempt to upload modified data, a page from the OSM website loads. After you have logged on (over an encrypted connection) you will be asked to authorize Vespucci to edit using your account. If you want to or need to authorize the OAuth access to your account before editing there is a corresponding item in the "Tools" menu.

Eğer çalışmanızı kaydetmek istiyorsanız fakat internet erişiminiz yoksa, JOSM uyumlu .osm dosyasına kaydedebilir daha sonra Vespucci ile veya JOSM ile sunucuya yükleyebilirsiniz. 

#### Yüklemelerdeki çatışmaları çözme

Vespucci has a simple conflict resolver. However if you suspect that there are major issues with your edits, export your changes to a .osc file ("Export" menu item in the "Transfer" menu) and fix and upload them with JOSM. See the detailed help on [conflict resolution](../en/Conflict resolution.md).  

## GPS kullanma

Bir GPX izi oluşturmak ve cihazınızda görüntülemek için Vespucci'yi kullanabilirsiniz. Dahası GPS konumunuzu görüntüleyin (menüden "Konumu göster" seçin) ve/veya ekran merkezinin etrafını görüntüleyin ya da (GPS menüsünden "GPS Konumu İzle" seçip) konumu takip edin. 

Eğer daha sonra ayarlarsanız, ekranı elle hareket ettirmek ve düzenleme "GPS takip et" modunu iptal eder, GPS ok'u dolu bir ok'a dönüşür. Hızlıca "takip" moduna dönmek için, ok'a tekrar dokunun ve menüden seçeneği tekrar seçin.

## Notes and Bugs

Vespucci supports downloading, commenting and closing of OSM Notes (formerly OSM Bugs) and the equivalent functionality for "Bugs" produced by the [OSMOSE quality assurance tool](http://osmose.openstreetmap.fr/en/map/). Both have to either be downloaded explicitly or you can use the auto download facility to access the items in your immediate area. Once edited or closed, you can either upload the bug or Note immediately or upload all at once.

On the map the Notes and bugs are represented by a small bug icon ![](../images/bug_open.png), green ones are closed/resolved, blue ones have been created or edited by you, and yellow indicates that it is still active and hasn't been changed. 

The OSMOSE bug display will provide a link to the affected object in blue, touching the link will select the object, center the screen on it and down load the area beforehand if necessary. 

## Vespucci'yi Özelleştirme

### Değiştirmek isteyebileceğiniz ayarlar

* Background layer
* Overlay layer. Adding an overlay may cause issues with older devices and such with limited memory. Default: none.
* Notes/Bugs display. Open Notes and bugs will be displayed as a yellow bug icon, closed ones the same in green. Default: on.
* Photo layer. Displays georeferenced photographs as red camera icons, if direction information is available the icon will be rotated. Default: off.
* Node icons. Default: on.
* Keep screen on. Default: off.
* Large node drag area. Moving nodes on a device with touch input is problematic since your fingers will obscure the current position on the display. Turning this on will provide a large area which can be used for off-centre dragging (selection and other operations still use the normal touch tolerance area). Default: off.

#### Gelişmiş tercihler

* Enable split action bar. On recent phones the action bar will be split in a top and bottom part, with the bottom bar containing the buttons. This typically allows more buttons to be displayed, however does use more of the screen. Turning this off will move the buttons to the top bar. note: you need to restart Vespucci for the change to take effect.
* Always show context menu. When turned on every selection process will show the context menu, turned off the menu is displayed only when no unambiguous selection can be determined. Default: off (used to be on).
* Enable light theme. On modern devices this is turned on by default. While you can enable it for older Android versions the style is likely to be inconsistent.
* Show statistics. Will show some statistics for debugging, not really useful. Default: off (used to be on).  

## Sorunları Bildirme

Vespucci çökerse, tutarsız bir durum algılanırsa, kilitlenme bilgi dökümü göndermek için size soracaktır. Böyle bir şey olursa lütfen bildirin, ve lütfen tek seferde tek sorun bildirin. Eğer daha fazla bilgi vermek, ya da bir özellik isteğinde bulunmak istiyorsanız bkz: [Vespucci issue tracker](https://github.com/MarcusWolschon/osmeditor4android/issues). Bir tartışma başlatmak ve Vespucci ile ilgili tartışmak istiyorsanız  [Vespucci google group](https://groups.google.com/forum/#!forum/osmeditor4android) veya [OpenStreetMap Android forum](http://forum.openstreetmap.org/viewforum.php?id=56)


