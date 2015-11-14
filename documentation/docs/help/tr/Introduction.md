# Vespucci Giriş

Vespucci tam donanımlı bir OpenStreetMap düzenleyicisidir masaüstünün sağladığı özelliklerin çoğunu sağlar. Anroid 2.3 den 5.1 e kadar başarıyla test edilmiştir. Bir uyarı: mobil cihazlar neredeyse masaüstü rakiplerini yakalamış durumda, özellikle eski cihazlarda çok az bellek vardır ve bu yüzden oldukça yavaş çalışma eğilimi gösterebilirler. Ör, makul düzeyde alan düzenlerken bile, Vespucci kullanırken bunu hesaba katmalısınız. 

## İlk kullanım

Başlangıçta Vespucci size "Başka konumu indir"/"Alanı Yükle" diyaloğu gösterir. Görütülediğiniz koordinatları hemen indirmek istiyorsanız, uygun seçeneği seçebilir indirmek istediğiniz alanın yarıçapını ayarlayabilirsiniz. Yavaş cihazlarda geniş alanlar seçmeyin. 

Alternatif olarak bu diyaloğu atlayıp "Haritaya git" düğmesine basın ve düzenlemek istediğiniz alanı yaklaştırıp verileri indirin. (aşağıya bkz: "Vespucci ile Düzenleme")

## Vespucci ile düzenleme

Cihazınızın yaşına ve ekran boyutuna bağlı olarak düzenleme menüsüne üst bardaki simgelerden, üst barın sağındaki açılır menüden, (eğer varsa) alttaki bardan veya menü tuşundan erişebilirsiniz.

### OSM Verisini indirme

Transfer simgesini ![](../images/menu_transfer.png) veya transfer menü ögesini seçin . Yedi seçenek sunacaktır:

* **Şimdiki görünümü indir** - ekranda görüntülenen alanı indir ve varsa mevcut veri ile değiştir  *(ağ bağlantısı gerekir)*
* **İndirmek için geçerli görünümü ekle** -ekranda görüntülenen alanı indir ve mevcut veri ile birleştir *(ağ bağlantısı gerekir)*
* **Başka konumu indir** - koordinat girmek için bir form gösterir, bir konumu arayabilir veya doğrudan konum girebilir, ve sonra o konumun alanını indirebilirsiniz *(ağ bağlantısı gerekir)*
* **Veriyi OSM sunucusuna yükle** - düzenlemeleri OpenStreetMap'e yükler *(doğrulama gerekir)* *(ağ bağlantısı gerekir)*
* **Değişiklikleri dışa aktar** - geçerli düzenlemeleri içeren bir ".osc" dosyası oluşturur, okunabilir örneğin JOSM okuyabilir
* **Dosyadan oku** - J(OSM) uyumlu XML biçimli dosyayı oku
* **Dosyaya kaydet** - JOSM uyumlu XML biçimli dosyaya kaydet

En kolay yol haritayı açın düzenlemek istediğiniz konuma yaklaştırın ve "Geçerli görünümü indir"i seçin. Jestleri kullanarak yaklaştırabilir siniz,  telefonun yaklaştır düğmesi veya ses seviye kontrol düğmelerini kullanabilirsiniz. Vespucci alanın verilerini indirebilir ve haritayı mevcut konumunuza ortalayabilir. Verileri indirmek için kimlik doğrulama gerekmez.

### Düzenleme

Yanlışlıkla düzenlemeleri önlemek için Vespucci "kilitli" modda başlar, bu mod sadece yaklaştırmaya ve haritayı hareket ettirmeye izin verir. Kilidi açmak için Ekrandaki [Kilitli] (../images/locked.png) simgesine dokunun.

Varsayılan olarak, seçilebilir düğümlerin ve yolların turuncu bir çevresi vardır. Bir nesneyi seçmeye çalıştığınızda Vespucci bu seçimin birden fazla nesnesi olduğunu saptarsa bir seçim menüsü sunacaktır. Seçilen nesne sarı renkle belirtilecektir.

Yüksek yoğunluklu bir yeri düzenlemek için yaklaştırmak iyi bir yoldur.

Vespucci'nin iyi bir geri al/yinele sistemi vardır cihazınızda bunu denemekten korkmayın, ancak lütfen deneme verilerini kaydedip sunucuya yüklemeyin.

#### Seçme / Bırakma

Vurgulamak için bir nesneyi seçin, ikinci dokunuş aynı nesne için etiket düzenleyicisini açacaktır. Ekranda boş bir yere dokunmak seçimi iptal eder. Bir nesneyi seçmişken başka bir nesneyi seçmek gerekirse seçimi iptal etmenize gerek yoktur, ilgili nesneye de dokunun.Nesneye çift dokunmak [Multiselect mode](Multiselect.md) başlatır

#### Yeni Düğüm/Nokta veya yol ekleme

Düğüm oluşturmak veya yol başlatmak için istediğiniz yere uzun dokunun. "Çapraz kıllar" sembolü göreceksiniz. Aynı yere tekrar dokunmak yeni düğüm oluşturur, dokunma toleransı dışında bir yere dokunmak orjinal konumdan geçerli konuma bir yol bölümü ekler. 

Düğümleri eklemek istediğiniz yere basitçe dokunun. Bitirmek için, son düğüme iki kez dokunun. Eğer ilk düğüm bir yol üzerindeyse, düğüm yola otomatik olarak bağlanacaktır.

#### Yolu veya Düğümü taşıma

Nesneler sadece seçildiklerinde taşınabilir/sürüklenebilir. Eğer seçeneklerden geniş sürükleme alanını seçerseniz, sürüklemek için daha geniş bir alana sahip olursunuz ve bu işlemi daha kolay gerçekleştirirsiniz. 

#### Yolun Geometrisini Geliştirme

Eğer yeterince yaklaştırırsanız yeterince uzun yolların ortasında ufak bir "x" simgesi göreceksiniz. "x" simgesini sürüklemek o konumda bir
düğüm oluşturacaktır. Not: yanlışlıkla düğüm oluşturmayı önlemek için dokunma toleransı bu işlem için oldukça düşüktür.

#### Kes, Kopyala & Yapıştır

Seçilen düğümleri ve yolları kopyalayabilir veya kesebilir, bir veya daha fazla kez yeni konumlara yapıştırabilirsiniz. Kesme işlemi osm id ve sürümünü koruyacaktır. Yapıştırmak istediğiniz konuma uzun dokunun (konumda çapraz kıl işareti göreceksiniz). Ardından menüden "Yapıştır"'ı seçin.

#### Verimli Adres Ekleme

Vespucci'nin keşfedilen alanları daha verimli eklemek için çalışan bir "adres etiketi ekleme" özelliği vardır. Buradan seçebilirsiniz 

* uzun bastıktan sonra: Vespucci o konumda bir düğüm ekleyebilir ve ev numarası için iyi bir tahminde bulunabilir, son zamanlarda kullandığınız etiketide ekleyebilir siniz. Eğer düğüm bir bina anahattı üzerindeyse **entrance=yes"" etiketi düğüme otomatik olarak eklenir. Etiket editörü söz konusu nesnede değişiklik yapmaya devam etmeniz içinde açılacaktır.
* Düğüm/yol seçim modunda: Vecpucci yukarıdaki gibi adres etiketlerini ekler ve  etiket editörünü başlatır.
* Etiket editöründe.

Ev numarası tahmini genelde en az 2 numara gerektirir çalışmaya yolun her iki tarafıda girilebilir, verilerde ne kadar çok numara olursa o kadar iyidir.

Bunu "Oto-indir" modunda kullanmayı düşünün.  

#### Dönüş Kısıtlamaları Ekleme

Vespucci'nin dönüş kısıtlaması eklemek için hızlı bir yöntemi vardır. Not: Eğer kısıtlama için yolu bölmek gerekiyorsa lütfen bunu başlamadan yapın.

* Otoyol etiketli bir yolu seçmek (dönüş kısıtlaması sadece otoyol için eklenebilir, bunu diğer yollara da yapmak gerekiyorsa, lütfen genel "ilişki oluşturma" modunu kullanın, eğer nesne mümkün değilse içerik menüsünde gösterilmez)
* Menüden "Kısıtlama Ekle" yi seçin
* "Üzerinden" düğüm'ü veya yol'u seçin (tüm "üzerinden" nesneleri nesne vurgulayarak seçilebilir)
* "için" yol'u seçin (eğer çift yönlüyse "için" nesnesini "itibaren" nesnesine ayarlayın, Vespucci no_u_turn kısıtlaması ekleyecektir)
*Kısıtlama tipini etiket menüsünden belirleyin

### "Kilitli" modda Vespucci

Kırmızı kilit simgesi göründüğünde aşağıdaki düzenleme dışındaki eylemler mevcuttur. Eğer bir OSM nesnesiyse ayrıca nesneye veya yakınına uzun dokunabilir ve daha detaylı bilgi alabilirsiniz.

### Değişenleri Kaydetmek

*(ağ bağlantısı gerekir)*

İndirmek için aynı butonu veya menüyü seçin ve ardından "Verileri OSM sunucusuna yükle"'yi seçin

Vecpucci klasik kullanıcı adı ve parolanın yanında OAuth doğrulamasını da destekler. OAuth daha iyidir, özellikle mobil uygulamalarda parolayı açık şekilde göndermeyi önler.

Yeni Vespucci OAuth etkin olarak yüklenir. Değiştirilen verileri OSM sunucusuna ilk yükleme girişiminde OSM web sayfası açılır. Giriş yaptıktan sonra (şifreli bağlantı üzerinden) Vespucci için düzenleme yetkisi istenecektir. İşlem tamamladıktan sonra Vespucci'ye geri dönecektir, tekrar yüklemeyi deneyebilirsiniz, şimdi başarılı olması gerekir.

Eğer çalışmanızı kaydetmek istiyorsanız fakat internet erişiminiz yoksa, JOSM uyumlu .osm dosyasına kaydedebilir daha sonra Vespucci ile veya JOSM ile sunucuya yükleyebilirsiniz. 

#### Yüklemelerdeki çatışmaları çözme

Vespucci basit bir çatışma çözümleyicisine sahiptir. Fakat düzenlemenizle ilgili büyük sorunlar olmasından şüpheniz varsa, değişiklilerinizi  ("Dışa aktar" menüsünden "Aktarım" seçin) .osc dosyasına aktarın, düzeltip JOSM ile yükleyebilirsiniz. Daha detaylı yardım için [conflict resolution](Conflict resolution.md) bakınız.  

## GPS kullanma

Bir GPX izi oluşturmak ve cihazınızda görüntülemek için Vespucci'yi kullanabilirsiniz. Dahası GPS konumunuzu görüntüleyin (menüden "Konumu göster" seçin) ve/veya ekran merkezinin etrafını görüntüleyin ya da (GPS menüsünden "GPS Konumu İzle" seçip) konumu takip edin. 

Eğer daha sonra ayarlarsanız, ekranı elle hareket ettirmek ve düzenleme "GPS takip et" modunu iptal eder, GPS ok'u dolu bir ok'a dönüşür. Hızlıca "takip" moduna dönmek için, ok'a tekrar dokunun ve menüden seçeneği tekrar seçin.

### Oto-İndir

*(ağ bağlantısı gerekir)*

Eğer "Konumu göster" ve "GPS konumunu Takip Et" etkinse, Vespucci mevcut konumunuzun etrafındaki ufak bir alanı otomatik indirmenize izin verir (varsayılan 50m yarıçapında). Eğer ekranı elle hareket ettirirseniz veya bir nesnenin geometrisini değiştirmek isterseniz takibe devam etmek için "GPS Konumunu Takip Et" tekrar seçmeniz gerekir. 

Notlar:

* ilk olarak başlangıç alanını elle indirmeniz gerekir
* bu fonksiyon OpenStreetMap API ile sorunlara sebep olmamak için 6km/s (tempolu yürüme) hızının altında çalışır.

## Vespucci'yi Özelleştirme

### Değiştirmek isteyebileceğiniz ayarlar

* Arkaplan katmanı
* Kaplama katmanı. Bir kaplama katmanı eklemek sınırlı hafızası olan eski cihazlarda sorun çıkarabilir. Varsayılan: Hiçbiri.
* Notları göster. Açık Notlar kırmızı dolgulu daire şeklinde gösterilir, kapalı Notlar aynısının mavisi olarak gösterilir. Varsayılan: kapalı
* Fotoğraf Katmanı. Coğrafi referanslı fotoğraflar kırmızı kamera simgesiyle gösterilir, yön bilgisi varsa ikon döndürülür. Varsayılan: kapalı
*Düğüm simgesi. Varsayılan: kapalı
* Geniş düğüm sürükleme alanı. Dokunmatik ekranlarda düğümleri taşımak sorunlu oluyor, parmaklar geçerli konumda istenmeyen yerlere dokunabiliyor. Bunu açmak merkez-dışı sürüklemek için geniş bir alan sağlar (seçimler ve diğer işlemler yine normal dokunma toleransını kullanır). Varsayılan: kapalı.

#### Gelişmiş tercihler

* Bölme eylem çubuğunu etkinleştir. Son telefonlarda eylem çubuğu alt ve üst çubuk olarak ikiye ayrılır, alt çubuk'un tuşları vardır. Bu genelde daha fazla düğme görüntülenmesini sağlar. Bunu kapatmak tuşları üst çubuğa taşır. Not: Değişikliğin geçerli olması için Vespucci'yi yeniden başlatmanız gerekir.
* İçerik Menüsünü her zaman göster. Bunu açarsanız ger seçimden sonra bir içerik menüsü gösterir, istenmeyen seçimlerden kaçınmak için bunu kapatın. Varsayılan: kapalı (kullanmak için açılabilir)
* Hafif temayı etkinleştir. Modern cihazlarda bu varsayılan olarak etkindir. Eski Android cihazlarda tutarsızlık yaşıyorsanız etkinleştirebilir siniz
* İstatiksel verileri göster. Hata ayıklama için bazı istatistiksel verileri gösterir, aslında pek kullanışlı değildir. Varsayılan: kapalı (kullanmak için açılabilir).  

## Sorunları Bildirme

Vespucci çökerse, tutarsız bir durum algılanırsa, kilitlenme bilgi dökümü göndermek için size soracaktır. Böyle bir şey olursa lütfen bildirin, ve lütfen tek seferde tek sorun bildirin. Eğer daha fazla bilgi vermek, ya da bir özellik isteğinde bulunmak istiyorsanız bkz: [Vespucci issue tracker](https://github.com/MarcusWolschon/osmeditor4android/issues). Bir tartışma başlatmak ve Vespucci ile ilgili tartışmak istiyorsanız  [Vespucci google group](https://groups.google.com/forum/#!forum/osmeditor4android) veya [OpenStreetMap Android forum](http://forum.openstreetmap.org/viewforum.php?id=56)


