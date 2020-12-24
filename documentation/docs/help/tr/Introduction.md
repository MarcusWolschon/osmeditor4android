# Vespucci Giriş

Vespucci tam donanımlı bir OpenStreetMap düzenleyicisidir masaüstünün sağladığı özelliklerin çoğunu sağlar. Anroid 2.3 den 10.0'a kadar başarıyla test edilmiştir. Bir uyarı: mobil cihazlar neredeyse masaüstü rakiplerini yakalamış durumda, özellikle eski cihazlarda çok az bellek vardır ve bu yüzden oldukça yavaş çalışma eğilimi gösterebilirler. Ör, makul düzeyde alan düzenlerken bile, Vespucci kullanırken bunu hesaba katmalısınız. 

## İlk defa kullanım

KonumBaşlangıçta Vespucci size "Başka konumu indir"/"Alanı Yükle" diyaloğu gösterir. Görüntülediğiniz koordinatları hemen indirmek istiyorsanız, uygun seçeneği seçebilir indirmek istediğiniz yerin yarıçapını ayarlayabilirsiniz. Yavaş cihazlarda geniş alanlar seçmeyin. 

Alternatif olarak bu diyaloğu atlayıp "Haritaya git" düğmesine basın ve düzenlemek istediğiniz alanı yaklaştırıp verileri indirin. (aşağıya bkz: "Vespucci ile Düzenleme")

## Vespucci ile düzenleme

Cihazınızın yaşına ve ekran boyutuna bağlı olarak düzenleme menüsüne üst bardaki simgelerden, üst barın sağındaki açılır menüden, (eğer varsa) alttaki bardan veya menü tuşundan erişebilirsiniz.

<a id="download"></a>

### OSM Verisini indirme

Transfer simgesini ![Transfer](../images/menu_transfer.png) veya "Transfer" menü öğesini seçin. Yedi seçenek görüntülenecektir:

* **Download current view** - download the area visible on the screen and merge it with existing data *(requires network connectivity or offline data source)*
* **Clear and download current view** - clear any data in memory and then download the area visible on the screen *(requires network connectivity)*
* **Upload data to OSM server** - upload edits to OpenStreetMap *(requires authentication)* *(requires network connectivity)*
* **Update data** - re-download data for all areas and update what is in memory *(requires network connectivity)*
* **Location based auto download** - download an area around the current geographic location automatically *(requires network connectivity or offline data)* *(requires GPS)*
* **Pan and zoom auto download** - download data for the currently displayed map area automatically *(requires network connectivity or offline data)* *(requires GPS)*
* **File...** - saving and loading OSM data to/from on device files.
* **Note/Bugs...** - download (automatically and manually) OSM Notes and "Bugs" from QA tools (currently OSMOSE) *(requires network connectivity)*

Cihazınıza verileri indirmenin en kolay yolu, düzenlemek istediğiniz konumu yakınlaştırıp "Geçerli görüntüyü indir" i seçmektir. Jestleri, cihazınızdaki yakınlaştırma düğmelerini veya ses kontrol düğmelerini kullanarak yakınlaştırma yapabilirsiniz. Vespucci artık geçerli görüntü için veriyi indirebilir. Verileri cihazınıza indirmek için kimlik doğrulama gerekmemektedir.

With the default settings any non-downloaded areas will be dimmed relative to the downloaded ones, this is to avoid inadvertently adding duplicate objects in areas that are not being displayed. The behaviour can be changed in the [Advanced preferences](Advanced%20preferences.md).

### Düzenleme

<a id="lock"></a>

#### Kilitli, kilitsiz, kip anahtarlama

Kazara yapılan düzenlemeleri önlemek için Vespucci "kilitli" modda başlatılır, bu mod sadece yakınlaştırmaya ve haritayı hareket ettirmeye izin verir. Ekran kilidini açmak için  ![Locked](../images/locked.png) simgesine tıklatın. 

Kilit simgesine uzun bir basıldığı anda halihazırda 4 seçenek sunan bir menü görüntülenecektir:

* **Normal** - the default editing mode, new objects can be added, existing ones edited, moved and removed. Simple white lock icon displayed.
* **Tag only** - selecting an existing object will start the Property Editor, a long press on the main screen will add objects, but no other geometry operations will work. White lock icon with a "T" is displayed.
* **Indoor** - enables Indoor mode, see [Indoor mode](#indoor). White lock icon with a "I" is displayed.
* **C-Mode** - enables C-Mode, only objects that have a warning flag set will be displayed, see [C-Mode](#c-mode). White lock icon with a "C" is displayed.

#### Tek dokunuş, çift dokunuş, ve uzun basış

Varsayılan olarak, seçilebilir düğümler ve yolların etrafında, bir nesneyi seçmek için kabaca nereye dokunmanız gerektiğini gösteren turuncu bir alan vardır. Üç seçeneğiniz var:

* Single tap: Selects object. 
    * An isolated node/way is highlighted immediately. 
    * However, if you try to select an object and Vespucci determines that the selection could mean multiple objects it will present a selection menu, enabling you to choose the object you wish to select. 
    * Selected objects are highlighted in yellow. 
    * For further information see [Node selected](Node%20selected.md), [Way selected](Way%20selected.md) and [Relation selected](Relation%20selected.md).
* Double tap: Start [Multiselect mode](Multiselect.md)
* Long press: Creates a "crosshair", enabling you to add nodes, see below and [Creating new objects](Creating%20new%20objects.md). This is only enabled if "Simple mode" is deactivated.

Yüksek yoğunluklu bir yeri düzenlemek için yaklaştırmak iyi bir yoldur.

Vespucci'nin iyi bir geri al/yinele sistemi vardır cihazınızda bunu denemekten korkmayın, ancak lütfen deneme verilerini kaydedip sunucuya yüklemeyin.

#### Seçme / Seçimi kaldırma (tek dokunuş ve "seçim menüsü")

Touch an object to select and highlight it. Touching the screen in an empty region will de-select. If you have selected an object and you need to select something else, simply touch the object in question, there is no need to de-select first. A double tap on an object will start [Multiselect mode](Multiselect.md).

Eğer bir nesneyi seçmeye çalışıyorsanız ve Vespucci bu seçimin birden fazla nesne (yoldaki bir düğüm veya diğer üst üste gelen nesneler gibi) içerdiğini belirlerse size bir seçenekler menüsü gösterecektir: Seçmek istediğiniz nesneye tıklayın ve işte oldu. 

Selected objects are indicated through a thin yellow border. The yellow border may be hard to spot, depending on map background and zoom factor. Once a selection has been made, you will see a notification confirming the selection.

Seçim tamamlandığında, seçilmiş öğeler için desteklenen işlemlerin listesini (düğme ya da menü öğesi olarak) göreceksiniz: Daha fazla bilgi için [Düğüm seçimi] (Node%20selected.md), [Yol seçimi] (Way%20selected.md) ve [İlişki seçimi] (Relation%20selected.md).

#### Seçilen nesneler: Yaftaları düzenleme

Seçilen nesneye ikinci kez dokunulduğunda etiket düzenleyici açılır ve nesne ile ilişkili etiketleri düzenleyebilirsiniz.

Note that for overlapping objects (such as a node on a way) the selection menu comes back up for a second time. Selecting the same object brings up the tag editor; selecting another object simply selects the other object.

#### Seçilen nesneler: bir Düğümü veya Yolu Taşıma

Once you have selected an object, it can be moved. Note that objects can be dragged/moved only when they are selected. Simply drag near (i.e. within the tolerance zone of) the selected object to move it. If you select the large drag area in the preferences, you get a large area around the selected node that makes it easier to position the object. 

#### Yeni bir Düğüm/Nokta veya Yol ekleme 

Uygulama ilk çalıştırılmasında "Basit mod" da başlatılır, bu ana menüdeki ilgili onay kutusunun işaretini kaldırarak değiştirilebilir.

##### Basit kip

Tapping the large green floating button on the map screen will show a menu. After you've selected one of the items, you will be asked to tap the screen at the location where you want to create the object, pan and zoom continues to work if you need to adjust the map view. 

See [Creating new objects in simple actions mode](Creating%20new%20objects%20in%20simple%20actions%20mode.md) for more information.

##### Advanced (long press) mode
 
Long press where you want the node to be or the way to start. You will see a black "crosshair" symbol. 
* If you want to create a new node (not connected to an object), touch away from existing objects.
* If you want to extend a way, touch within the "tolerance zone" of the way (or a node on the way). The tolerance zone is indicated by the areas around a node or way.

İnceartı gösterge simgesini gördüğünüzde, şu seçeneklere sahipsiniz:

* _Normal press in the same place._
    * If the crosshair is not near a node, touching the same location again creates a new node. If you are near a way (but not near a node), the new node will be on the way (and connected to the way).
    * If the crosshair is near a node (i.e. within the tolerance zone of the node), touching the same location just selects the node (and the tag editor opens. No new node is created. The action is the same as the selection above.
* _Normal touch in another place._ Touching another location (outside of the tolerance zone of the crosshair) adds a way segment from the original position to the current position. If the crosshair was near a way or node, the new segment will be connected to that node or way.

Yol üzerinde başka nereye düğüm eklemek istiyorsanız sadece ekrana dokunmanız yeterli. Bitirmek için son düğüme iki kez dokunun. Eğer son düğüm bir yol veya başka bir düğüm üzerine denk geliyorsa, bu bölüm o yola veya düğüme otomatik olarak bağlanacaktır. 

Bir menü öğesi de kullanabilirsiniz: Daha fazla bilgi için şunu seçin [Yeni nesne oluşturma](Creating%20new%20objects.md) 

#### Alan Ekleme

OpenStreetMap currently doesn't have an "area" object type unlike other geo-data systems. The online editor "iD" tries to create an area abstraction from the underlying OSM elements which works well in some circumstances, in others not so. Vespucci currently doesn't try to do anything similar, so you need to know a bit about the way areas are represented:

* _closed ways (*polygons")_: the simplest and most common area variant, are ways that have a shared first and last node forming a closed "ring" (for example most buildings are of this type). These are very easy to create in Vespucci, simply connect back to the first node when you are finished with drawing the area. Note: the interpretation of the closed way depends on its tagging: for example if a closed way is tagged as a building it will be considered an area, if it is tagged as a roundabout it wont. In some situations in which both interpretations may be valid, an "area" tag can clarify the intended use.
* _multi-ploygons_: some areas have multiple parts, holes and rings that can't be represented with just one way. OSM uses a specific type of relation (our general purpose object that can model relations between elements) to get around this, a multi-polygon. A multi-polygon can have multiple "outer" rings, and multiple "inner" rings. Each ring can either be a closed way as described above, or multiple individual ways that have common end nodes. While large multi-polygons are difficult to handle with any tool, small ones are not difficult to create in Vespucci. 
* _coastlines_: for very large objects, continents and islands, even the multi-polygon model doesn't work in a satisfactory way. For natural=coastline ways we assume direction dependent semantics: the land is on the left side of the way, the water on the right side. A side effect of this is that, in general, you shouldn't reverse the direction of a way with coastline tagging. More information can be found on the [OSM wiki](http://wiki.openstreetmap.org/wiki/Tag:natural%3Dcoastline).

#### Yolun Geometrisini Geliştirme

If you zoom in far enough on a selected way you will see a small "x" in the middle of the way segments that are long enough. Dragging the "x" will create a node in the way at that location. Note: to avoid accidentally creating nodes, the touch tolerance area for this operation is fairly small.

#### Kes, Kopyala & Yapıştır

Seçilen düğümleri ve yolları kopyalayabilir veya kesebilir, bir veya daha fazla kez yeni konumlara yapıştırabilirsiniz. Kesme işlemi osm id ve sürümünü koruyacaktır. Yapıştırmak istediğiniz konuma uzun dokunun (konumda çapraz kıl işareti göreceksiniz). Ardından menüden "Yapıştır"'ı seçin.

#### Etkin Adresler Ekleme

Vespucci has an ![Address](../images/address.png) "add address tags" function that tries to make surveying addresses more efficient by predicting the current house number. It can be selected:

* after a long press (_non-simple mode only:): Vespucci will add a node at the location and make a best guess at the house number and add address tags that you have been lately been using. If the node is on a building outline it will automatically add a "entrance=yes" tag to the node. The tag editor will open for the object in question and let you make any necessary further changes.
* in the node/way selected modes: Vespucci will add address tags as above and start the tag editor.
* in the property editor.

Ev numarası tahmini genelde en az 2 numara gerektirir çalışmaya yolun her iki tarafıda girilebilir, verilerde ne kadar çok numara olursa o kadar iyidir.

Bunu [Otomatik indirme] (# indirme) kipiyle kullanmayı düşünün.  

#### Dönüş Kısıtlamaları Ekleme

Vespucci'nin dönüş kısıtlamaları eklemek için hızlı bir yolu var. Eğer gerekirse otomatik olarak yolları bölecek ve elemanları tekrar seçmek için size soracaktır. 

* select a way with a highway tag (turn restrictions can only be added to highways, if you need to do this for other ways, please use the generic "create relation" mode)
* select "Add restriction" from the menu
* select the "via" node or way (only possible "via" elements will have the touch area shown)
* select the "to" way (it is possible to double back and set the "to" element to the "from" element, Vespucci will assume that you are adding an no_u_turn restriction)
* set the restriction type

### Vespucci "kilitli" kipte

Kırmızı kilit gözüktüğünde tüm düzenlenemez eylem uygun olur. Ek olarak, eğer bir OSM nesnesi ise, uzun bir basış ya da bir nesneye yakınlaşma ile detaylı bilgi gösterilir.

### Değişenleri Kaydetmek

*(ağ bağlantısı gerekir)*

İndirmek için aynı butonu veya menüyü seçin ve ardından "Verileri OSM sunucusuna yükle"'yi seçin

Vespucci OAuth doğrulamasını, ve klasik kullanıcı adı-şifre yöntemini destekler. Şifreleri açıktan göndermeyi önlediği için OAuth daha iyidir.

New Vespucci installs will have OAuth enabled by default. On your first attempt to upload modified data, a page from the OSM website loads. After you have logged on (over an encrypted connection) you will be asked to authorize Vespucci to edit using your account. If you want to or need to authorize the OAuth access to your account before editing there is a corresponding item in the "Tools" menu.

Eğer çalışmanızı kaydetmek istiyorsanız fakat internet erişiminiz yoksa, JOSM uyumlu .osm dosyasına kaydedebilir daha sonra Vespucci ile veya JOSM ile sunucuya yükleyebilirsiniz. 

#### Yüklemelerdeki çatışmaları çözme

Vespucci has a simple conflict resolver. However if you suspect that there are major issues with your edits, export your changes to a .osc file ("Export" menu item in the "Transfer" menu) and fix and upload them with JOSM. See the detailed help on [conflict resolution](Conflict%20resolution.md).  

## GPS kullanma

Bir GPX izi oluşturmak ve cihazınızda görüntülemek için Vespucci'yi kullanabilirsiniz. Dahası GPS konumunuzu görüntüleyin (menüden "Konumu göster" seçin) ve/veya ekran merkezinin etrafını görüntüleyin ya da (GPS menüsünden "GPS Konumu İzle" seçip) konumu takip edin. 

If you have the latter set, moving the screen manually or editing will cause the "follow GPS" mode to be disabled and the blue GPS arrow will change from an outline to a filled arrow. To quickly return to the "follow" mode, simply touch GPS button or re-check the menu option.

## Notlar ve Hatalar

Vespucci supports downloading, commenting and closing of OSM Notes (formerly OSM Bugs) and the equivalent functionality for "Bugs" produced by the [OSMOSE quality assurance tool](http://osmose.openstreetmap.fr/en/map/). Both have to either be down loaded explicitly or you can use the auto download facility to access the items in your immediate area. Once edited or closed, you can either upload the bug or Note immediately or upload all at once.

On the map the Notes and bugs are represented by a small bug icon ![Bug](../images/bug_open.png), green ones are closed/resolved, blue ones have been created or edited by you, and yellow indicates that it is still active and hasn't been changed. 

The OSMOSE bug display will provide a link to the affected object in blue, touching the link will select the object, center the screen on it and down load the area beforehand if necessary. 

### Süzme

Besides globally enabling the notes and bugs display you can set a coarse grain display filter to reduce clutter. In the [Advanced preferences](Advanced%20preferences.md) you can individually select:

* Notes
* Osmose error
* Osmose warning
* Osmose minor issue
* Custom

<a id="indoor"></a>

## Kapalı mekan kipi

Mapping indoors is challenging due to the high number of objects that very often will overlay each other. Vespucci has a dedicated indoor mode that allows you to filter out all objects that are not on the same level and which will automatically add the current level to new objects created there.

Bu mod, kilit öğesinin üzerine uzun basarak ve ilgili menü girdisini seçerek aktifleştirilebilir. Şuraya bakın [Kilitle, kilidi aç, mod değiştir](#lock).

<a id="c-mode"></a>

## C-Mode

In C-Mode only objects are displayed that have a warning flag set, this makes it easy to spot objects that have specific problems or match configurable checks. If an object is selected and the Property Editor started in C-Mode the best matching preset will automatically be applied.

Bu mod, kilit öğesinin üzerine uzun basarak ve ilgili menü girdisini seçerek aktifleştirilebilir. Şuraya bakın [Kilitle, kilidi aç, mod değiştir](#lock).

### Denetimleri yapılandırma

Currently there are two configurable checks (there is a check for FIXME tags and a test for missing type tags on relations that are currently not configurable) both can be configured by selecting "Validator settings" in the "Preferences". 

The list of entries is split in to two, the top half lists "re-survey" entries, the bottom half "check entries". Entries can be edited by clicking them, the green menu button allows adding of entries.

#### Re-survey entries

Re-survey entries have the following properties:

* **Key** - Key of the tag of interest.
* **Value** - Value the tag of interest should have, if empty the tag value will be ignored.
* **Age** - how many days after the element was last changed the element should be re-surveyed, if a _check_date_ tag is present that will be the used, otherwise the date the current version was create. Setting the value to zero will lead to the check simply matching against key and value.
* **Regular expression** - if checked **Value** is assumed to be a JAVA regular expression.

**Key** and **Value** are checked against the _existing_ tags of the object in question.

The _Annotations_ group in the standard presets contain an item that will automatically add a _check_date_ tag with the current date.

#### Girdileri denetle

Girdilerin aşağıdaki iki özelliğe sahip olduğunu denetleyin:

* **Key** - Key that should be present on the object according to the matching preset.
* **Require optional** - Require the key even if the key is in the optional tags of the matching preset .

This check works by first determining the matching preset and then checking if **Key** is a "recommended" key for this object according to the preset, **Require optional** will expand the check to tags that are "optional* on the object. Note: currently linked presets are not checked.

## Süzgeçler

### Etiket temelli süzme

Bu filtre ana menüden devreye sokulabilir, filtre simgesine dokunulduğunda değişir. Daha fazla doküman burada bulunabilir  [Tag filter](Tag%20filter.md).

### Önayar temelli süzme

An alternative to the above, objects are filtered either on individual presets or on preset groups. Tapping on the filter icon will display a preset selection dialog similar to that used elsewhere in Vespucci. Individual presets can be selected by a normal click, preset groups by a long click (normal click enters the group). More documentation can be found here [Preset filter](Preset%20filter.md).

## Vespucci'yi Özelleştirme

Bu uygulama bir çok yönden kişiselleştirilebilir.Belirli bir şey arıyorsanız ve bulamadıysanız, [Vespucci'nin sitesi](https://vespucci.io/) aramaya uygundur ve cihaz için nelerin mümkün olabileceği hakkında ek bilgiyi içerir.

### Katman ayarları

Layer settings can be changed via the layer control ("hamburger" menu in the upper right corner), all other setting are reachable via the main menu preferences button. Layers can be enabled, disabled and temporarily hidden.

Uygun katman stilleri:

* Data layer - this is the layer OpenStreetMap data is loaded in to. In normal use you do not need to change anything here. Default: on.
* Background layer - there is a wide range of aerial and satellite background imagery available. The default value for this is the "standard style" map from openstreetmap.org.
* Overlay layer - these are semi-transparent layers with additional information, for example GPX tracks. Adding an overlay may cause issues with older devices and such with limited memory. Default: none.
* Notes/Bugs display - Open Notes and bugs will be displayed as a yellow bug icon, closed ones the same in green. Default: on.
* Photo layer - Displays geo-referenced photographs as red camera icons, if direction information is available the icon will be rotated. Default: off.
* Mapillary layer - Displays Mapillary segments with markers where images exist, clicking on a marker will display the image. Default: off.
* GeoJSON layer - Displays the contents of a GeoJSON file. Default: off.
* Grid - Displays a scale along the sides of the map or a grid. Default: on. 

#### Tercihler

* Keep screen on. Default: off.
* Large node drag area. Moving nodes on a device with touch input is problematic since your fingers will obscure the current position on the display. Turning this on will provide a large area which can be used for off-center dragging (selection and other operations still use the normal touch tolerance area). Default: off.

Tam kapsamlı açıklama burada bulunabilir: [Tercihler](Preferences.md)

#### Gelişmiş tercihler

* Düğüm simgeleri. Varsayılan: açık.
* Her zaman içerik menüsünü göster. Etkinleştirildiğinde, her seçim işlemi bir içerik menüsü gösterecektir ve devre dışı bırakılmış menü yalnızca kesin bir seçim belirlenemediğinde görüntülenir. Varsayılan: kapalı (açık olarak kullanılır).
* Işık temasını etkinleştirin. Modern cihazlarda bu varsayılan olarak etkindir. Android'in eski sürümleri için etkinleştirebilirken, stilin tutarsız olması muhtemeldir. 

Tam kapsamlı açıklama burada bulunabilir [Gelişmiş özellikler](Advanced%20preferences.md)

## Sorunları Bildirme

Vespucci çöker ya da tutarsız bir durum tespit ederse, kilitlenme dökümünü göndermeniz istenecektir. Bu olursa lütfen, sadece -belirli bir durum için- bir kez bunu yapın. Eğer daha fazla veri ya da benzer bir sorun açmak istiyorsanız, buraya gidiniz: [Vespucci issue tracker] (https://github.com/MarcusWolschon/osmeditor4android/issues). Ana menüden "Geri Bildirim Sağla" özelliği yeni bir sürüm açacak ve ek yazım olmadan ilgili uygulama ve cihaz bilgilerini içerecektir.

Vespucci ile alakalı bir şey tartışmak istiyorsanız, şu iki linkte tartışma başlatabilirsiniz. [Vespucci Google grubu](https://groups.google.com/forum/#!forum/osmeditor4android) ya da [OpenStreetMap Android forumu](http://forum.openstreetmap.org/viewforum.php?id=56)


