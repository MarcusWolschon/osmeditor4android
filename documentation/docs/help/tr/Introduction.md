_Before we start: most screens have links in the menu to the on-device help system giving you direct access to information relevant for the current context, you can easily navigate back to this text too. If you have a larger device, for example a tablet, you can open the help system in a separate split window.  All the help texts and more (FAQs, tutorials) can be found on the [Vespucci documentation site](https://vespucci.io/) too. You can further start the help viewer directly on devices that support short cuts with a long press on the app icon and selecting "Help"_

# Vespucci Giriş

Vespucci is a full featured OpenStreetMap editor that supports most operations that desktop editors provide. It has been tested successfully on Google's Android 2.3 to 14.0 (versions prior to 4.1 are no longer supported) and various AOSP based variants. A word of caution: while mobile device capabilities have caught up with their desktop rivals, particularly older devices have very limited memory available and tend to be rather slow. You should take this in to account when using Vespucci and keep, for example, the areas you are editing to a reasonable size.

## Vespucci ile düzenleme

Cihazınızın yaşına ve ekran boyutuna bağlı olarak düzenleme menüsüne üst bardaki simgelerden, üst barın sağındaki açılır menüden, (eğer varsa) alttaki bardan veya menü tuşundan erişebilirsiniz.

<a id="download"></a>

### OSM Verisini indirme

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

Cihazınıza verileri indirmenin en kolay yolu, düzenlemek istediğiniz konumu yakınlaştırıp "Geçerli görüntüyü indir" i seçmektir. Jestleri, cihazınızdaki yakınlaştırma düğmelerini veya ses kontrol düğmelerini kullanarak yakınlaştırma yapabilirsiniz. Vespucci artık geçerli görüntü için veriyi indirebilir. Verileri cihazınıza indirmek için kimlik doğrulama gerekmemektedir.

In unlocked state any non-downloaded areas will be dimmed relative to the downloaded ones if you are zoomed in far enough to enable editing. This is to avoid inadvertently adding duplicate objects in areas that are not being displayed. In the locked state dimming is disabled, this behaviour can be changed in the [Advanced preferences](Advanced%20preferences.md) so that dimming is always active.

If you need to use a non-standard OSM API entry, or use [offline data](https://vespucci.io/tutorials/offline/) in _MapSplit_ format you can add or change entries via the _Configure..._ entry for the data layer in the layer control.

### Düzenleme

<a id="lock"></a>

#### Kilitli, kilitsiz, kip anahtarlama

Kazara yapılan düzenlemeleri önlemek için Vespucci "kilitli" modda başlatılır, bu mod sadece yakınlaştırmaya ve haritayı hareket ettirmeye izin verir. Ekran kilidini açmak için  ![Locked](../images/locked.png) simgesine tıklatın. 

A long press on the lock icon or the _Modes_ menu in the map display overflow menu will display a menu offering 4 options:

* **Normal** - the default editing mode, new objects can be added, existing ones edited, moved and removed. Simple white lock icon displayed.
* **Tag only** - selecting an existing object will start the Property Editor, new objects can be added via the green "+" button, or long press, but no other geometry operations are enabled. White lock icon with a "T" is displayed.
* **Address** - enables Address mode, a slightly simplified mode with specific actions available from the [Simple mode](../en/Simple%20actions.md) "+" button. White lock icon with an "A" is displayed.
* **Indoor** - enables Indoor mode, see [Indoor mode](#indoor). White lock icon with an "I" is displayed.
* **C-Mode** - enables C-Mode, only objects that have a warning flag set will be displayed, see [C-Mode](#c-mode). White lock icon with a "C" is displayed.

If you are using Vespucci on an Android device that supports short cuts (long press on the app icon) you can start directly to _Address_ and _Indoor_ mode.

#### Tek dokunuş, çift dokunuş, ve uzun basış

Varsayılan olarak, seçilebilir düğümler ve yolların etrafında, bir nesneyi seçmek için kabaca nereye dokunmanız gerektiğini gösteren turuncu bir alan vardır. Üç seçeneğiniz var:

* Tek dokunuş: Nesneyi seçer.
* İzole edilmiş bir düğüm / yol hemen vurgulanır.
* Ancak, bir nesneyi seçmeye çalışırsanız ve Vespucci seçimin birden fazla nesne anlamına gelebileceğini belirlerse, bir seçim menüsü sunarak, seçmek istediğiniz nesneyi seçmenize olanak tanır.
* Seçilen nesneler sarı ile vurgulanır.
* Daha fazla bilgi için bkz. [Node selected] (Node% 20selected.md), [Way selected] (Way% 20selected.md) ve [Relation selected] (Relation% 20selected.md).
* Çift dokunma: Başlat [Çoklu seçim modu] (Multiselect.md)
* Uzun basış: Düğüm eklemenizi sağlayan bir "artı işareti" oluşturur, aşağıya bakın ve [Yeni nesneler oluşturma] (Oluşturma% 20new% 20objects.md). Bu, yalnızca "Basit mod" devre dışı bırakıldığında etkinleştirilir.

Yüksek yoğunluklu bir yeri düzenlemek için yaklaştırmak iyi bir yoldur.

Vespucci'nin iyi bir geri al/yinele sistemi vardır cihazınızda bunu denemekten korkmayın, ancak lütfen deneme verilerini kaydedip sunucuya yüklemeyin.

#### Seçme / Seçimi kaldırma (tek dokunuş ve "seçim menüsü")

Seçmek ve vurgulamak için bir nesneye dokunun. Boş bir bölgedeki ekrana dokunduğunuzda seçim kaldırılacaktır. Bir nesne seçtiyseniz ve başka bir şey seçmeniz gerekiyorsa, söz konusu nesneye dokunmanız yeterlidir, önce seçimi kaldırmaya gerek yoktur. Bir nesneye çift dokunulduğunda [Çoklu seçim modu] (Multiselect.md) başlar.

Eğer bir nesneyi seçmeye çalışıyorsanız ve Vespucci bu seçimin birden fazla nesne (yoldaki bir düğüm veya diğer üst üste gelen nesneler gibi) içerdiğini belirlerse size bir seçenekler menüsü gösterecektir: Seçmek istediğiniz nesneye tıklayın ve işte oldu. 

Seçilen nesneler ince sarı bir kenarlıkla belirtilir. Harita arka planına ve yakınlaştırma faktörüne bağlı olarak sarı sınırın fark edilmesi zor olabilir. Seçim yapıldıktan sonra, seçimi onaylayan bir bildirim göreceksiniz.

Seçim tamamlandığında, seçilmiş öğeler için desteklenen işlemlerin listesini (düğme ya da menü öğesi olarak) göreceksiniz: Daha fazla bilgi için [Düğüm seçimi] (Node%20selected.md), [Yol seçimi] (Way%20selected.md) ve [İlişki seçimi] (Relation%20selected.md).

#### Seçilen nesneler: Yaftaları düzenleme

Seçilen nesneye ikinci kez dokunulduğunda etiket düzenleyici açılır ve nesne ile ilişkili etiketleri düzenleyebilirsiniz.

Çakışan nesneler için (bir yol üzerindeki düğüm gibi) seçim menüsünün ikinci kez geri geldiğini unutmayın. Aynı nesnenin seçilmesi etiket düzenleyiciyi getirir; başka bir nesnenin seçilmesi sadece diğer nesneyi seçer.

#### Seçilen nesneler: bir Düğümü veya Yolu Taşıma

Once you have selected an object, it can be moved. Note that objects can be dragged/moved only when they are selected. Simply drag near (i.e. within the tolerance zone of) the selected object to move it. If you select the large drag area in the [preferences](Preferences.md), you get a large area around the selected node that makes it easier to position the object. 

#### Yeni bir Düğüm/Nokta veya Yol ekleme 

Uygulama ilk çalıştırılmasında "Basit mod" da başlatılır, bu ana menüdeki ilgili onay kutusunun işaretini kaldırarak değiştirilebilir.

##### Basit kip

Harita ekranındaki büyük yeşil yüzen düğmeye dokunduğunuzda bir menü görüntülenir. Öğelerden birini seçtikten sonra, nesneyi oluşturmak istediğiniz konumda ekrana dokunmanız istenir, harita görünümünü ayarlamanız gerekirse kaydırma ve yakınlaştırma çalışmaya devam eder. 

See [Creating new objects in simple actions mode](Simple%20actions.md) for more information. Simple mode os the default for new installs.

##### Gelişmiş (uzun basma) modu

Düğümün olmasını istediğiniz yere veya başlama yoluna uzun basın. Siyah bir "artı" sembolü göreceksiniz.
* Yeni bir düğüm oluşturmak istiyorsanız (bir nesneye bağlı olmayan), mevcut nesnelerden uzağa dokunun.
* Bir yolu uzatmak istiyorsanız, yolun "tolerans bölgesi" içerisine (veya yoldaki bir düğüme) dokunun. Tolerans bölgesi, bir düğümün veya yolun etrafındaki alanlarla gösterilir.

İnceartı gösterge simgesini gördüğünüzde, şu seçeneklere sahipsiniz:

* _Aynı yerde normal basış._
* Artı işareti bir düğümün yakınında değilse, aynı konuma tekrar dokunmak yeni bir düğüm oluşturur. Bir yola yakınsanız (ancak bir düğümün yakınında değilseniz), yeni düğüm yolda olacak (ve yola bağlanacaktır).
Artı işareti bir düğümün yakınındaysa (yani düğümün tolerans bölgesi içindeyse), aynı konuma dokunmak yalnızca düğümü seçer (ve etiket düzenleyici açılır. Yeni düğüm oluşturulmaz. Eylem, yukarıdaki seçimle aynıdır.
* _ Başka bir yerde normal dokunuş._ Başka bir konuma (artı işaretinin tolerans bölgesinin dışında) dokunmak, orijinal konumdan geçerli konuma bir yol parçası ekler. Artı işareti bir yola veya düğüme yakınsa, yeni bölüm bu düğüme veya yola bağlanacaktır.

Yol üzerinde başka nereye düğüm eklemek istiyorsanız sadece ekrana dokunmanız yeterli. Bitirmek için son düğüme iki kez dokunun. Eğer son düğüm bir yol veya başka bir düğüm üzerine denk geliyorsa, bu bölüm o yola veya düğüme otomatik olarak bağlanacaktır. 

Bir menü öğesi de kullanabilirsiniz: Daha fazla bilgi için şunu seçin [Yeni nesne oluşturma](Creating%20new%20objects.md) 

#### Alan Ekleme

OpenStreetMap şu anda diğer coğrafi veri sistemlerinden farklı olarak "alan" nesne türüne sahip değildir. Çevrimiçi düzenleyici "iD", temeldeki OSM öğelerinden bazı durumlarda iyi çalışan, bazılarında ise pek işe yaramayan bir alan soyutlaması oluşturmaya çalışır. Vespucci şu anda benzer bir şey yapmaya çalışmıyor, bu yüzden alanların temsil edilme şekli hakkında biraz bilgi sahibi olmanız gerekiyor:

* _closed ways (*polygons")_: the simplest and most common area variant, are ways that have a shared first and last node forming a closed "ring" (for example most buildings are of this type). These are very easy to create in Vespucci, simply connect back to the first node when you are finished with drawing the area. Note: the interpretation of the closed way depends on its tagging: for example if a closed way is tagged as a building it will be considered an area, if it is tagged as a roundabout it wont. In some situations in which both interpretations may be valid, an "area" tag can clarify the intended use.
* _multi-polygons_: some areas have multiple parts, holes and rings that can't be represented with just one way. OSM uses a specific type of relation (our general purpose object that can model relations between elements) to get around this, a multi-polygon. A multi-polygon can have multiple "outer" rings, and multiple "inner" rings. Each ring can either be a closed way as described above, or multiple individual ways that have common end nodes. While large multi-polygons are difficult to handle with any tool, small ones are not difficult to create in Vespucci. 
* _coastlines_: for very large objects, continents and islands, even the multi-polygon model doesn't work in a satisfactory way. For natural=coastline ways we assume direction dependent semantics: the land is on the left side of the way, the water on the right side. A side effect of this is that, in general, you shouldn't reverse the direction of a way with coastline tagging. More information can be found on the [OSM wiki](http://wiki.openstreetmap.org/wiki/Tag:natural%3Dcoastline).

#### Yolun Geometrisini Geliştirme

Seçilen bir yoldan yeterince yakınlaştırırsanız, yeterince uzun olan bölümlerin ortasında küçük bir "x" görürsünüz. "X" i sürüklemek, bu konumda yol üzerinde bir düğüm oluşturacaktır. Not: Yanlışlıkla düğüm oluşturmayı önlemek için, bu işlem için dokunma toleransı alanı oldukça küçüktür.

#### Kes, Kopyala & Yapıştır

You can copy selected nodes and ways, and then paste once or multiple times to a new location. Cutting will retain the osm id and version, thus can only be pasted once. To paste long press the location you want to paste to (you will see a cross hair marking the location). Then select "Paste" from the menu.

#### Etkin Adresler Ekleme

Vespucci supports functionality that makes surveying addresses more efficient by predicting house numbers (left and right sides of streets separately) and automatically adding _addr:street_ or _addr:place_ tags based on the last used value and proximity. In the best case this allows adding an address without any typing at all.   

Adding the tags can be triggered by pressing ![Address](../images/address.png): 

* after a long press (in non-simple mode only): Vespucci will add a node at the location and make a best guess at the house number and add address tags that you have been lately been using. If the node is on a building outline it will automatically add an "entrance=yes" tag to the node. The tag editor will open for the object in question and let you make any necessary further changes.
* in the node/way selected modes: Vespucci will add address tags as above and start the tag editor.
* in the property editor.

To add individual address nodes directly while in the default "Simple mode" switch to "Address" editing mode (long press on the lock button), "Add address node" will then add an address node at the location and if it is on a building outline add a entrance tag to it as described above.

Ev numarası tahmini genelde en az 2 numara gerektirir çalışmaya yolun her iki tarafıda girilebilir, verilerde ne kadar çok numara olursa o kadar iyidir.

Consider using this with one of the [Auto-download](#download) modes.  

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

Vespucci supports OAuth 2, OAuth 1.0a authorization and the classical username and password method. Since July 1st 2024 the standard OpenStreetMap API only supports OAuth 2 and other methods are only available on private installations of the API or other projects that have repurposed OSM software.  

Authorizing Vespucci to access your account on your behalf requires you to one time login with your display name and password. If your Vespucci install isn't authorized when you attempt to upload modified data you will be asked to login to the OSM website (over an encrypted connection). After you have logged on you will be asked to authorize Vespucci to edit using your account. If you want to or need to authorize the OAuth access to your account before editing there is a corresponding item in the "Tools" menu.

Eğer çalışmanızı kaydetmek istiyorsanız fakat internet erişiminiz yoksa, JOSM uyumlu .osm dosyasına kaydedebilir daha sonra Vespucci ile veya JOSM ile sunucuya yükleyebilirsiniz. 

#### Yüklemelerdeki çatışmaları çözme

Vespucci'nin basit bir sorun çözücüsü var. Ancak, düzenlemelerinizle ilgili büyük sorunlar olduğundan şüpheleniyorsanız, değişikliklerinizi bir .osc dosyasına aktarın ("Aktar" menüsünde "Dışa Aktar" menü öğesi) ve düzeltip JOSM ile yükleyin. [Conflict% 20resolution.md) ile ilgili ayrıntılı yardıma bakın.  

### Nearby point-of-interest display

A nearby point-of-interest display can be shown by pulling the handle in the middle and top of the bottom menu bar up. 

More information on this and other available functionality on the main display can be found here [Main map display](Main%20map%display.md).

## Using GPS and GPX tracks

With standard settings Vespucci will try to enable GPS (and other satellite based navigation systems) and will fallback to determining the position via so called "network location" if this is not possible. This behaviour assumes that you in normal use have your Android device itself configured to only use GPX generated locations (to avoid tracking), that is you have the euphemistically named "Improve Location Accuracy" option turned off. If you want to enable the option but want to avoid Vespucci falling back to "network location", you should turn the corresponding option in the [Advanced preferences](Advanced%20preferences.md) off. 

Touching the ![GPS](../images/menu_gps.png) button (normally on the left hand side of the map display) will center the screen on the current position and as you move the map display will be panned to maintain this.  Moving the screen manually or editing will cause the "follow GPS" mode to be disabled and the blue GPS arrow will change from an outline to a filled arrow. To quickly return to the "follow" mode, simply touch GPS button or re-check the equivalent menu option. If the device doesn't have a current location the location marker/arrow will be displayed in black, if a current location is available the marker will be blue.

To record a GPX track and display it on your device select "Start GPX track" item in the ![GPS](../images/menu_gps.png) menu. This will add layer to the display with the current recorded track, you can upload and export the track from the entry in the [layer control](Main%20map%20display.md). Further layers can be added from local GPX files and tracks downloaded from the OSM API.

Note: by default Vespucci will not record elevation data with your GPX track, this is due to some Android specific issues. To enable elevation recording, either install a gravitational model, or, simpler, go to the [Advanced preferences](Advanced%20preferences.md) and configure NMEA input.

### How to export a GPX track?

Open the layer menu, then click the 3-dots menu next to "GPX recording", then select **Export GPX track...**. Choose in which folder to export the track, then give it a name suffixed with `.gpx` (example: MyTrack.gpx).

## Notes, Bugs and Todos

Vespucci, OSM notlarının (eski adıyla OSM Hataları) indirilmesini, yorumlanmasını ve kapatılmasını ve [OSMOSE kalite güvence aracı] (http://osmose.openstreetmap.fr/en/map/) tarafından üretilen "Hatalar" için eşdeğer işlevselliği destekler. Her ikisinin de ya açıkça indirilmesi gerekir ya da yakın çevrenizdeki öğelere erişmek için otomatik indirme özelliğini kullanabilirsiniz. Düzenlendikten veya kapatıldıktan sonra, hatayı veya notu hemen yükleyebilir veya hepsini bir kerede yükleyebilirsiniz. 

Further we support "Todos" that can either be created from OSM elements, from a GeoJSON layer, or externally to Vespucci. These provide a convenient way to keep track of work that you want to complete. 

On the map the Notes and bugs are represented by a small bug icon ![Bug](../images/bug_open.png), green ones are closed/resolved, blue ones have been created or edited by you, and yellow indicates that it is still active and hasn't been changed. Todos use a yellow checkbox icon.

The OSMOSE bug and Todos display will provide a link to the affected element in blue (in the case of Todos only if an OSM element is associated with it), touching the link will select the object, center the screen on it and down load the area beforehand if necessary. 

### Süzme

Besides globally enabling the notes and bugs display you can set a coarse grain display filter to reduce clutter. The filter configuration can be accessed from the task layer entry in the [layer control](#layers):

* Notes
* Osmose error
* Osmose warning
* Osmose minor issue
* Maproulette
* Todo

<a id="indoor"></a>

## Kapalı mekan kipi

İç mekanlarda haritalama, sıklıkla birbirini kaplayan çok sayıda nesne nedeniyle zordur. Vespucci, aynı seviyede olmayan tüm nesneleri filtrelemenize izin veren ve mevcut seviyeyi orada oluşturulan yeni nesnelere otomatik olarak ekleyen özel bir iç mekan moduna sahiptir.

Bu mod, kilit öğesinin üzerine uzun basarak ve ilgili menü girdisini seçerek aktifleştirilebilir. Şuraya bakın [Kilitle, kilidi aç, mod değiştir](#lock).

<a id="c-mode"></a>

## C-Modu

C-Modunda yalnızca bir uyarı bayrağı ayarlanmış nesneler görüntülenir, bu, belirli sorunları olan nesneleri bulmayı veya yapılandırılabilir kontrolleri eşleştirmeyi kolaylaştırır. Bir nesne seçilirse ve Özellik Düzenleyicisi C-Modunda başlatılırsa, en iyi eşleşen ön ayar otomatik olarak uygulanacaktır.

Bu mod, kilit öğesinin üzerine uzun basarak ve ilgili menü girdisini seçerek aktifleştirilebilir. Şuraya bakın [Kilitle, kilidi aç, mod değiştir](#lock).

### Denetimleri yapılandırma

All validations can be disabled/enabled in the "Validator settings/Enabled validations" in the [preferences](Preferences.md). 

The configuration for "Re-survey" entries allows you to set a time after which a tag combination should be re-surveyed. "Check" entries are tags that should be present on objects as determined by matching presets. Entries can be edited by clicking them, the green menu button allows adding of entries.

#### Re-survey entries

Re-survey entries have the following properties:

* **Key** - Key of the tag of interest.
* **Value** - Value the tag of interest should have, if empty the tag value will be ignored.
* **Age** - how many days after the element was last changed the element should be re-surveyed, if a _check_date_ tag is present that will be the used, otherwise the date the current version was create. Setting the value to zero will lead to the check simply matching against key and value.
* **Regular expression** - if checked **Value** is assumed to be a JAVA regular expression.

** Anahtar ** ve ** Değer **, söz konusu nesnenin _ mevcut _ etiketleriyle karşılaştırılır.

Standart ön ayarlardaki _Annotations_ grubu, geçerli tarihle otomatik olarak bir _check_date_ etiketi ekleyen bir öğe içerir.

#### Girdileri denetle

Girdilerin aşağıdaki iki özelliğe sahip olduğunu denetleyin:

* **Key** - Key that should be present on the object according to the matching preset.
* **Require optional** - Require the key even if the key is in the optional tags of the matching preset.

Bu kontrol, önce eşleşen ön ayarı belirleyerek ve ardından ** Anahtar ** ın ön ayara göre bu nesne için "önerilen" anahtar olup olmadığını kontrol ederek çalışır, ** İsteğe bağlı gerektirir **, denetimi nesne üzerinde "isteğe bağlı * olan etiketlere genişletir. Not: Şu anda bağlantılı ön ayarlar kontrol edilmemiştir.

## Süzgeçler

### Etiket temelli süzme

Bu filtre ana menüden devreye sokulabilir, filtre simgesine dokunulduğunda değişir. Daha fazla doküman burada bulunabilir  [Tag filter](Tag%20filter.md).

### Önayar temelli süzme

Yukarıdakilere bir alternatif olarak, nesneler ayrı ön ayarlara veya ön ayar gruplarına göre filtrelenir. Filtre simgesine dokunduğunuzda, Vespucci'de başka yerlerde kullanılana benzer bir önceden ayarlanmış seçim diyaloğu görüntülenir. Ayrı ön ayarlar normal bir tıklama ile, önceden ayarlanmış gruplar uzun bir tıklama ile seçilebilir (normal tıklama gruba girer). Daha fazla dokümantasyonu burada [Preset filter] (Preset% 20filter.md) bulabilirsiniz.

## Vespucci'yi Özelleştirme

Bu uygulama bir çok yönden kişiselleştirilebilir.Belirli bir şey arıyorsanız ve bulamadıysanız, [Vespucci'nin sitesi](https://vespucci.io/) aramaya uygundur ve cihaz için nelerin mümkün olabileceği hakkında ek bilgiyi içerir.

<a id="layers"></a>

### Katman ayarları

Katman ayarları katman kontrolünden (sağ üst köşedeki "hamburger" menüsü) değiştirilebilir, diğer tüm ayarlara ana menü tercihleri butonu ile ulaşılabilir. Katmanlar etkinleştirilebilir, devre dışı bırakılabilir ve geçici olarak gizlenebilir.

Uygun katman stilleri:

* Data layer - this is the layer OpenStreetMap data is loaded in to. In normal use you do not need to change anything here. Default: on.
* Background layer - there is a wide range of aerial and satellite background imagery available. The default value for this is the "standard style" map from openstreetmap.org.
* Overlay layer - these are semi-transparent layers with additional information, for example quality assurance information. Adding an overlay may cause issues with older devices and such with limited memory. Default: none.
* Notes/Bugs display - Open Notes and bugs will be displayed as a yellow bug icon, closed ones the same in green. Default: on.
* Photo layer - Displays geo-referenced photographs as red camera icons, if direction information is available the icon will be rotated. Default: off.
* Mapillary layer - Displays Mapillary segments with markers where images exist, clicking on a marker will display the image. Default: off.
* GeoJSON layer - Displays the contents of a GeoJSON file, multiple layers can be added from files. Default: none.
* GPX layer - Displays GPX tracks and way points, multiple layers can be added from files, during recording the generate GPX track is displayed in its own one . Default: none.
* Grid - Displays a scale along the sides of the map or a grid. Default: on. 

More information can be found in the section on the [map display](Main%20map%20display.md).

#### Tercihler

* Ekranını açık tut. Varsayılan: kapalı.
* Büyük düğüm sürükleme alanı. Dokunmatik girişi olan bir cihazda düğümlerin taşınması sorunludur çünkü parmaklarınız ekrandaki mevcut konumu gizleyecektir. Bunu açmak, merkez dışı sürükleme için kullanılabilecek geniş bir alan sağlayacaktır (seçim ve diğer işlemler hala normal dokunma toleransı alanını kullanır). Varsayılan: kapalı.

Tam kapsamlı açıklama burada bulunabilir: [Tercihler](Preferences.md)

#### Gelişmiş tercihler

* Full screen mode. On devices without hardware buttons Vespucci can run in full screen mode, that means that "virtual" navigation buttons will be automatically hidden while the map is displayed, providing more space on the screen for the map. Depending on your device this may work well or not,  In _Auto_ mode we try to determine automatically if using full screen mode is sensible or not, setting it to _Force_ or _Never_ skips the automatic check and full screen mode will always be used or always not be used respectively. On devices running Android 11 or higher the _Auto_ mode will never turn full screen mode on as Androids gesture navigation provides a viable alternative to it. Default: _Auto_.  
* Node icons. Default: _on_.
* Always show context menu. When turned on every selection process will show the context menu, turned off the menu is displayed only when no unambiguous selection can be determined. Default: off (used to be on).
* Enable light theme. On modern devices this is turned on by default. While you can enable it for older Android versions the style is likely to be inconsistent. 

Tam kapsamlı açıklama burada bulunabilir [Gelişmiş özellikler](Advanced%20preferences.md)

## Reporting and Resolving Issues

Vespucci çöker ya da tutarsız bir durum tespit ederse, kilitlenme dökümünü göndermeniz istenecektir. Bu olursa lütfen, sadece -belirli bir durum için- bir kez bunu yapın. Eğer daha fazla veri ya da benzer bir sorun açmak istiyorsanız, buraya gidiniz: [Vespucci issue tracker] (https://github.com/MarcusWolschon/osmeditor4android/issues). Ana menüden "Geri Bildirim Sağla" özelliği yeni bir sürüm açacak ve ek yazım olmadan ilgili uygulama ve cihaz bilgilerini içerecektir.

If you are experiencing difficulties starting the app after a crash, you can try to start it in _Safe_ mode on devices that support short cuts: long press on the app icon and then select _Safe_ from the menu. 

If you want to discuss something related to Vespucci, you can either start a discussion on the [OpenStreetMap forum](https://community.openstreetmap.org).


