_Before we start: most screens have links in the menu to the on-device help system giving you direct access to information relevant for the current context, you can easily navigate back to this text too. If you have a larger device, for example a tablet, you can open the help system in a separate split window.  All the help texts and more (FAQs, tutorials) can be found on the [Vespucci documentation site](https://vespucci.io/) too. You can further start the help viewer directly on devices that support short cuts with a long press on the app icon and selecting "Help"_

# معرفی وسپوچی

Vespucci is a full featured OpenStreetMap editor that supports most operations that desktop editors provide. It has been tested successfully on Google's Android 2.3 to 14.0 (versions prior to 4.1 are no longer supported) and various AOSP based variants. A word of caution: while mobile device capabilities have caught up with their desktop rivals, particularly older devices have very limited memory available and tend to be rather slow. You should take this in to account when using Vespucci and keep, for example, the areas you are editing to a reasonable size.

## ویرایش با وسپوچی

بسته به اندازهٔ صفحه‌نمایش و عمر دستگاه، کنش‌های ویرایش مستقیماً از طریق نمادهای موجود در نوار بالا، از طریق منوی کشویی در سمت راست نوار بالا، از نوار پایین (در صورت وجود) یا از طریق کلید منو قابل دسترسی است.

<a id="download"></a>

### بارگیری دادهٔ OSM

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

ساده‌ترین روش بارگیری داده در دستگاه این است که با حرکت و زوم به مکانی که می‌خواهید ویرایش کنید بروید و «بارگیری نمای کنونی» را انتخاب نمایید. می‌توانید با ژست‌های حرکتی، دکمه‌های زوم یا دکمه‌های کنترل صدای دستگاه، زوم کنید. سپس وسپوچی دادهٔ نمای کنونی را دریافت می‌کند. برای بارگیری داده در دستگاهتان به احراز هویت نیازی نیست.

In unlocked state any non-downloaded areas will be dimmed relative to the downloaded ones if you are zoomed in far enough to enable editing. This is to avoid inadvertently adding duplicate objects in areas that are not being displayed. In the locked state dimming is disabled, this behaviour can be changed in the [Advanced preferences](Advanced%20preferences.md) so that dimming is always active.

If you need to use a non-standard OSM API entry, or use [offline data](https://vespucci.io/tutorials/offline/) in _MapSplit_ format you can add or change entries via the _Configure..._ entry for the data layer in the layer control.

### ویرایش

<a id="lock"></a>

#### حالت قفل، حالت باز، تعویض حالت

برای پیشگیری از ویرایش‌های ناخواسته، وسپوچی در حالت «قفل» آغاز می‌شود؛ حالتی که در آن فقط می‌توانید زوم کنید و نقشه را جابه‌جا نمایید. روی نماد ![قفل](../images/locked.png) بزنید تا قفل باز شود. 

A long press on the lock icon or the _Modes_ menu in the map display overflow menu will display a menu offering 4 options:

* **Normal** - the default editing mode, new objects can be added, existing ones edited, moved and removed. Simple white lock icon displayed.
* **Tag only** - selecting an existing object will start the Property Editor, new objects can be added via the green "+" button, or long press, but no other geometry operations are enabled. White lock icon with a "T" is displayed.
* **Address** - enables Address mode, a slightly simplified mode with specific actions available from the [Simple mode](../en/Simple%20actions.md) "+" button. White lock icon with an "A" is displayed.
* **Indoor** - enables Indoor mode, see [Indoor mode](#indoor). White lock icon with an "I" is displayed.
* **C-Mode** - enables C-Mode, only objects that have a warning flag set will be displayed, see [C-Mode](#c-mode). White lock icon with a "C" is displayed.

If you are using Vespucci on an Android device that supports short cuts (long press on the app icon) you can start directly to _Address_ and _Indoor_ mode.

#### تک‌ضربه، دوضربه و لمس طولانی

به‌طور پیشفرض، دور گره‌ها و راه‌های قابل‌انتخاب محدودهٔ نارنجی‌رنگی مشاهده می‌کنید. با لمس این محدوده می‌توانید چیزها را انتخاب کنید. سه گزینه پیش روی شماست:

* تک‌ضربه: چیز را انتخاب می‌کند. 
    * بلافاصله گره/راه تکی انتخاب می‌شود. 
    * البته، اگر هنگام انتخاب یک چیز، وسپوچی ببیند چند چیز در معرض انتخاب هستند، منوی انتخاب را نشانتان می‌دهد و می‌توانید دقیقاً شیء موردنظر خود را انتخاب نمایید. 
    * دور چیزهای انتخاب‌شده هالهٔ زردرنگ نمایش می‌یابد. 
    * برای اطلاعات بیشتر [انتخاب گره](Node%20selected.md)، [انتخاب راه](Way%20selected.md) و [انتخاب رابطه](Relation%20selected.md) را ببینید.
* دو ضربه: [حالت انتخاب چندتایی](Multiselect.md) را فعال می‌کند.
* لمس طولانی: علامت + ایجاد می‌کند و می‌توانید گره اضافه کنید. بخش زیر و [ایجاد اشیای جدید](Creating%20new%20objects.md) را ببینید. این قابلیت فقط اگر «حالت ساده» غیرفعال باشد کار می‌کند.

اگر سعی می‌کنید ناحیه ای با تراکم بالا را ویرایش کنید، بهتر است بیشتر زوم کنید.

وسپوچی عملکرد «واگرد/ازنو»ی خوبی دارد، بنابراین با خیال راحت روی دستگاه خود آزمایش کنید. اما لطفاً دادهٔ آزمایشی را بارگذاری و ذخیره ننمایید.

#### انتخاب کردن/نکردن (تک ضربه و "منوی انتخاب")

شیئی را لمس کنید تا انتخاب شود. اگر فضای خالی از صفحه را لمس کنید، از انتخاب در می‌آید. اگر چیزی را انتخاب کرده‌اید و می‌خواهید چیز دیگری را انتخاب کنید، خیلی ساده است، آن چیز جدید را لمس کنید. لازم نیست ابتدا شیء قبلی را از انتخاب در آورید. اگر روی شیئی دوضربه بزنید، [حالت انتخاب چندتایی](Multiselect.md) فعال می‌شود.

توجه کنید که اگر تلاش کنید شیئی را انتخاب نمایید و وسپوچی ببیند چیزهای دیگری هم در محل لمس قرار دارند (مانند گره روی راه یا چیزهای دیگری که همپوشانی دارند) در این صورت منوی انتخاب را نشانتان می‌دهد: حال روی شیء موردنظرتان بزنید تا انتخاب شود. 

اشیای انتخاب‌شده با خط زردرنگی که دورشان ظاهر می‌شود برجسته می‌شوند. بسته به پس‌زمینهٔ نقشه و میزان زوم، شاید این حاشیهٔ زردرنگ را به‌وضوح نبینید. پس از انتخاب هر چیز، پیامی ظاهر می‌شود و انتخاب را تأیید می‌کند.

پس از انتخاب، لیستی از کنش‌های پشتیبانی‌شده برای شیء انتخابی نشان داده می‌شود (در قالب دکمه یا گزینه‌های منو): برای اطلاعات بیشتر [انتخاب گره](Node%20selected.md)، [انتخاب راه](Way%20selected.md) و [انتخاب رابطه](Relation%20selected.md) را ببینید.

#### اشیای انتخاب‌شده: ویرایش تگ‌ها

با لمس دوبارهٔ شیء انتخاب‌شده، ویرایشگر تگ باز می‌شود و می‌توانید تگ‌های آن را ویرایش کنید.

توجه کنید که برای اشیای هم‌پوشان (مانند گره روی راه) منوی انتخاب دوباره ظاهر می‌شود. اگر دوباره شیء انتخاب‌شده را برگزینید ویرایشگر تگ باز می‌شود. اما اگر شیء دیگری را برگزینید، آن شیء در حالت انتخاب قرار می‌گیرد.

#### اشیای انتخاب‌شده: جابه‌جایی گره یا راه

هنگامی که یک شی را انتخاب کردید ، می توانید آن را جابجا کنید. توجه داشته باشید که اشیاء تنها در صورت انتخاب می توانند کشیده شوند. به سادگی شیء انتخاب شده را نزدیک (یعنی در محدوده تحمل) بکشید تا آن را منتقل کنید. اگر ناحیه کشیدن بزرگ را در [preferences] (Preferences.md) انتخاب کنید ، یک ناحیه بزرگ در اطراف گره انتخاب شده بدست می آورید که موقعیت یابی شی را آسان تر می کند. 

#### افزودن گره/نقطه یا راه جدید 

در اولین اجرا، برنامه در «حالت ساده» بالا می‌آید. این حالت را می‌توانید از منوی اصلی با برداشتن تیک مربوطه غیرفعال کنید. 

##### حالت ساده

با زدن روی دکمهٔ شناور سبزرنگ بزرگی که روی صفحه است منویی نمایان می‌شود. پس از انتخاب یکی از موارد، از شما خواسته می‌شود مکان موردنظرتان را برای رسم شیء لمس کنید. اگر نیاز دارید نمای نقشه را تنظیم کنید، از کشیدن و زوم‌کردن بهره ببرید. 

See [Creating new objects in simple actions mode](Simple%20actions.md) for more information. Simple mode os the default for new installs.

##### حالت پیشرفته (فشار طولانی)

فشار طولانی را در جایی که می خواهید گره باشد یا راه شروع را فشار دهید. نماد سیاه "crosshair" را مشاهده خواهید کرد.
* اگر می خواهید یک گره جدید ایجاد کنید (به یک شی متصل نیست) ، اجسام موجود را دور بزنید.
* اگر می خواهید راهی را گسترش دهید ، "منطقه تحمل" راه (یا یک گره در راه) را لمس کنید. منطقه تحمل توسط مناطق اطراف یک گره یا راه نشان داده می شود.

هنگامی که علامت + نمایان شد، این گزینه‌ها پیش روی شماست:

* _ فشار معمولی در همان مکان ._
* اگر موهای زائد نزدیک یک گره نباشند، لمس مجدد همان محل دوباره یک گره جدید ایجاد می کند. اگر نزدیک راهی هستید (اما نزدیک یک گره نیستید)، گره جدید در راه است (و به راه متصل است).
* اگر قسمت متقاطع نزدیک یک گره (یعنی در محدوده تحمل گره) است، با لمس یک مکان فقط گره را انتخاب می‌کنید (و ویرایشگر برچسب باز می‌شود. هیچ گره جدیدی ایجاد نمی‌شود. عمل همان انتخاب بالا است.
* _ فشار طبیعی در مکان دیگری. اگر موهای زائد نزدیک یک راه یا گره بود، بخش جدید به آن گره یا راه متصل می‌شود.

به سادگی صفحه ای را که می خواهید گره های بیشتری را به آن اضافه کنید لمس کنید. برای اتمام، گره نهایی را دوبار لمس کنید. اگر گره نهایی در یک راه یا گره قرار داشته باشد، قطعه به طور خودکار به راه یا گره متصل می شود. 

همچنین می‌توانید از گزینه‌ای در منو استفاده کنید: برای اطلاعات بیشتر [ایجاد اشیای جدید](Creating%20new%20objects.md) را ببینید.

#### افزودن محدوده

برخلاف سایر سامانه‌های دادهٔ مکانی، اوپن‌استریت‌مپ هم‌اکنون نوع دادهٔ «محدوده» ندارد. ویرایشگر آنلاین iD تلاش می‌کند که مفهوم محدوده را بر اساس عناصر پایه‌ای OSM ایجاد کند که در برخی وضعیت‌ها نیز به‌خوبی کار می‌کند اما نه همیشه. در وسپوچی فعلاً قرار نیست چنین کار مشابهی انجام شود، بنابراین لازم است کمی دربارهٔ چگونگی ارائهٔ محدوده‌ها بدانید:

* _راههای بسته (*چندضلعی‌ها")_: ساده ترین و رایج ترین گونهٔ محدوده‌ راههایی هستند که اولین و آخرین گره آن‌ها یکی است و "حلقه"بسته‌ای شکل می‌دهند (برای مثال اکثر ساختمان ها از این نوع هستند). ساخت این‌ها در وسپوچی بسیار آسان است. کافی است آخرین گره را به اولین گره متصل کنید تا کار را تمام کنید و محدوده رسم شود. توجه: تفسیر راهِ بسته، به تگ‌گذاری آن بستگی دارد: مثلاً اگر راه بسته‌ای به‌عنوان ساختمان تگ‌دهی شود، محدوده به حساب می‌آید اما اگر به‌عنوان فلکه تگ‌گذاری شود محدوده نیست و فقط حلقه است. در برخی وضعیت‌ها هر دو تفسیر ممکن است درست باشد. یک تگ محدوده‌ساز می‌تواند کاربرد نهایی را مشخص کند.
* _چند-چندضلعی‌ها_ یا _multi-ploygon_: برخی محدوده‌ها چند بخش، حفره و حلقه دارند که تنها با یک راه درست نمی‌شود. برای این موضوع OSM نوع خاصی از رابطه (عنصر همه‌کاره‌ای که با آن ارتباط بین عنصرها را مدل‌سازی می‌کنیم) را به کار می‌برد؛ چند-چندضلعی یا multi-polygon. هر multi-polygon می‌تواند چندین حلقهٔ بیرونی یا outer و چندین حلقهٔ درونی یا inner  داشته باشد. هر حلقه می‌تواند راهی بسته باشد (که توضیح دادیم) یا از چندین راه منفرد که گره‌های پایانی مشترک دارند تشکیل شود. اگرچه کارکردن با چند-چندضلعی‌های بزرگ با استفاده از هر ابزاری دشوار است اما ساخت موارد کوچک‌تر در وسپوچی سخت نیست.
* _خط‌های ساحلی_: برای چیزهای خیلی بزرگ، مانند قاره‌ها و جزیره‌ها، حتی مدل‌سازی با multi-polygon نیز چندان رضایت‌بخش نیست. برای راه‌هایی که تگ natural=coastline می‌گیرند مفاهیمی وابسته به جهت را به کار می‌بریم: خشکی سمت چپ راه قرار می‌گیرد و آب سمت راست آن. یکی از آثار جانبی این کار این است که اگر راهی به‌عنوان coastline تگ‌گذاری شده، نباید جهت آن را عوض کنید. اطلاعات بیشتر را در [ویکی OSM](http://wiki.openstreetmap.org/wiki/Tag:natural%3Dcoastline) خواهید یافت.

#### بهبود هندسهٔ راه

اگر به اندازهٔ کافی روی راهی زوم کنید، وسط پاره‌هایی از راه که به قدر کافی بلند باشند، "×" کوچکی می‌بینید. با کشیدن ضربدر، در آن مکان گرهی روی راه ایجاد می‌شود. نکته: برای پیشگیری از ایجاد گره‌های تصادفی، فضای انتخاب پیرامون این ضربدر نسبتاً کوچک در نظر گرفته شده است.

#### برش، کپی و درج

You can copy selected nodes and ways, and then paste once or multiple times to a new location. Cutting will retain the osm id and version, thus can only be pasted once. To paste long press the location you want to paste to (you will see a cross hair marking the location). Then select "Paste" from the menu.

#### نشانی‌زنی به‌طور کارآمد

وسپوچی از عملکردی پشتیبانی می‌کند که با پیش‌بینی شماره خانه (سمت چپ و راست خیابان‌ها به طور جداگانه) و افزودن خودکار برچسب‌های _addr:street_ یا _addr:place_ بر اساس آخرین مقدار استفاده شده و مجاورت، آدرس‌های نقشه‌برداری را کارآمدتر می‌کند. در بهترین حالت، این ویژگی امکان اضافه کردن یک آدرس را بدون هیچ گونه تایپی فراهم می‌کند.   

امکان افزودن برچسب‌ها با فشار دادن ![Address](../images/address.png): 

* با لمس طولانی (_فقط حالت غیرساده:): وسپوچی در مکان انتخاب‌شده گرهی اضافه می‌کند و بهترین حدسش را برای پلاک خانه می‌زند و تگ‌های نشانی‌ای که اخیراً استفاده می‌کردید را اضافه می‌کند. اگر گره روی خط دور ساختمان قرار گرفته باشد، به‌طور خودکار تگ entrance=yes را به گره می‌دهد. ویرایشگر تگ برای شیء موردبحث باز می‌شود و می‌توانید سایر تغییرات لازم را انجام بدهید..
* در حالت‌های انتخاب گره/راه: وسپوچی همانند بالا تگ‌های نشانی را اضافه می‌کند و ویرایشگر تگ را باز می‌کند.
* در ویرایشگر خصوصیت.

برای افزودن مستقیم گره‌های آدرس جداگانه در حالی که در حالت پیش‌فرض «حالت ساده» به حالت ویرایش «آدرس» تغییر می‌کند (دکمه قفل را فشار دهید)، «افزودن گره آدرس» سپس یک گره آدرس را در محل اضافه می‌کند و اگر روی یک طرح کلی ساختمان قرار دارید همانطور که در بالا توضیح داده شد یک برچسب ورودی به آن اضافه کنید.

برای اینکه پیشبینی پلاک خانه کار کند، دست‌کم لازم است دو شمارهٔ خانه در هر کنارهٔ جاده وجود داشته باشد. هر چه تعداد شماره‌ها بیشتر باشد، بهتر است.

استفاده از این را با یکی از حالت‌های [Auto-Download] (#download) در نظر بگیرید.  

#### افزودن محدودیت‌های گردش

وسپوچی برای افزودن محدودیت‌های گردش روش سریعی دارد. اگر لازم باشد خودش راه‌ها را دو نیم می‌کند و دوباره از شما می‌خواهد که عنصرها را انتخاب کنید. 

* راهی با برچسب highway انتخاب کنید (محدودیت‌های گردش فقط به معابر اضافه می‌شود، اگر می‌خواهید این کار را برای راه‌های دیگر انجام دهید لطفاً از حالت عمومی «ساخت رابطه» استفاده نمایید)
* «افزودن محدودیت» را از منو انتخاب کنید
* گره یا راه via را انتخاب کنید (فقط مواردی که می‌توانند نقش via داشته باشند قابل‌انتخاب هستند)
* راه to را انتخاب کنید (می‌توانید برگردید و عنصر from را مجدداً به‌عنوان عنصر to انتخاب کنید. وسپوچی فرض می‌کند که محدودیت «دورزدن ممنوع» no_u_turn ایجاد می‌کنید)
* نوع محدودیت را تنظیم کنید

### وسپوچی در حالت «قفل»

هنگامی که قفل قرمز نشان داده می‌شود همهٔ کنش‌های غیرویرایشی فعال هستند. همچنین با لمس طولانی روی یک عنصر OSMای، کادری حاوی اطلاعات جزئی از آن عنصر نمایش داده می‌شود.

### ذخیره‌سازی تغییراتتان

*(به اتصال شبکه نیاز دارد)*

روی همان دکمه یا گزینهٔ منو که برای بارگیری زدید، بزنید و اکنون "بارگذاری داده ها در سرور OSM" را انتخاب کنید.

Vespucci supports OAuth 2, OAuth 1.0a authorization and the classical username and password method. Since July 1st 2024 the standard OpenStreetMap API only supports OAuth 2 and other methods are only available on private installations of the API or other projects that have repurposed OSM software.  

Authorizing Vespucci to access your account on your behalf requires you to one time login with your display name and password. If your Vespucci install isn't authorized when you attempt to upload modified data you will be asked to login to the OSM website (over an encrypted connection). After you have logged on you will be asked to authorize Vespucci to edit using your account. If you want to or need to authorize the OAuth access to your account before editing there is a corresponding item in the "Tools" menu.

اگر می‌خواهید کار خود را ذخیره کنید اما به اینترنت دسترسی ندارید، می‌توانید تغییرات را در قالب پروندهٔ ‎.osm سازگار با JOSM ذخیره کنید و آن را در آینده با استفاده از وسپوچی یا JOSM بارگذاری نمایید. 

#### حل تداخل‌ها هنگام بارگذاری

وسپوچی حل‌کنندهٔ تداخل ساده‌ای دارد. البته اگر فکر می‌کنید مسئله‌ای جدّی در ویرایش‌هایتان وجود دارد، تغییرات خود را در قالب پروندهٔ ‎.osc برون‌برد کنید (گزینهٔ «انتقال» در منوی «داده‌رسانی») و کار اصلاح و بارگذاری را با JOSM انجام دهید. راهنمای مفصل‌تر را در [حل تداخل](Conflict%20resolution.md) ببینید.  

### Nearby point-of-interest display

A nearby point-of-interest display can be shown by pulling the handle in the middle and top of the bottom menu bar up. 

More information on this and other available functionality on the main display can be found here [Main map display](Main%20map%display.md).

## استفاده از GPS و مسیرهای GPX

وسپوچی با تنظیمات استاندارد سعی می‌کند GPS (و سایر سیستم‌های ناوبری مبتنی بر ماهواره) را فعال کند و در صورت عدم امکان، موقعیت را از طریق به اصطلاح "موقعیت شبکه" تعیین می‌کند. این رفتار فرض می‌کند که شما در استفاده معمولی، خود دستگاه اندروید خود را طوری پیکربندی کرده‌اید که فقط از مکان‌های تولید شده GPX استفاده کند (برای جلوگیری از ردیابی)، یعنی گزینه «بهبود دقت موقعیت مکانی» را خاموش کرده‌اید. اگر می‌خواهید این گزینه را فعال کنید اما می‌خواهید از بازگشت وسپوچی به "موقعیت شبکه" جلوگیری کنید، باید گزینه مربوطه را در [ترجیحات پیشرفته] (Advanced%20preferences.md) خاموش کنید. 

Touching the ![GPS](../images/menu_gps.png) button (normally on the left hand side of the map display) will center the screen on the current position and as you move the map display will be panned to maintain this.  Moving the screen manually or editing will cause the "follow GPS" mode to be disabled and the blue GPS arrow will change from an outline to a filled arrow. To quickly return to the "follow" mode, simply touch GPS button or re-check the equivalent menu option. If the device doesn't have a current location the location marker/arrow will be displayed in black, if a current location is available the marker will be blue.

برای ضبط یک رد GPX و نمایش آن در دستگاه خود، مورد "Start GPX track" را در منوی ![GPS](../images/menu_gps.png) انتخاب کنید. این یک لایه به صفحه نمایش با رد ضبط شده فعلی اضافه می‌کند، می توانید رد را از ورودی در [کنترل لایه] (Main%20map%20display.md) آپلود و صادر کنید. لایه‌های بیشتری را می‌توان از فایل‌های GPX محلی و ردهای دانلود شده از OSM API اضافه کرد.

توجه: به‌طور پیش‌فرض وسپوچی داده‌های ارتفاع را با مسیر GPX شما ثبت نمی‌کند، این به دلیل برخی مشکلات خاص اندروید است. برای فعال کردن ضبط ارتفاع، یا یک مدل گرانشی نصب کنید، یا برای حالت ساده تر، به [ترجیحات پیشرفته] (Advanced%20preferences.md) بروید و ورودی NMEA را پیکربندی کنید.

### How to export a GPX track?

Open the layer menu, then click the 3-dots menu next to "GPX recording", then select **Export GPX track...**. Choose in which folder to export the track, then give it a name suffixed with `.gpx` (example: MyTrack.gpx).

## Notes, Bugs and Todos

وسپوچی از بارگیری، نظردهی و بستن یادداشت‌های OSM (همان باگ‌های OSM در گذشته) و قابلیت متناظر با «باگ» که [ابزار تضمین کیفیت OSMOSE](http://osmose.openstreetmap.fr/en/map/) آن را تولید می‌کند، پشتیبانی می‌کند. هر کدام از این دو مورد را می‌توانید به‌طور دستی بارگیری کنید یا از قابلیت بارگیری خودکار بهره ببرید و آن‌ها را در محدوده‌ای که هر لحظه کار می‌کنید مشاهده نمایید. پس از ویرایش یا بستن آن‌ها، می‌توانید بلافاصله یادداشت یا باگ را بارگذاری نمایید یا همگی را با هم بارگذاری کنید. 

Further we support "Todos" that can either be created from OSM elements, from a GeoJSON layer, or externally to Vespucci. These provide a convenient way to keep track of work that you want to complete. 

On the map the Notes and bugs are represented by a small bug icon ![Bug](../images/bug_open.png), green ones are closed/resolved, blue ones have been created or edited by you, and yellow indicates that it is still active and hasn't been changed. Todos use a yellow checkbox icon.

The OSMOSE bug and Todos display will provide a link to the affected element in blue (in the case of Todos only if an OSM element is associated with it), touching the link will select the object, center the screen on it and down load the area beforehand if necessary. 

### پالایش

علاوه بر فعال سازی جهانی نمایش یادداشت ها و اشکالات ، می توانید یک فیلتر نمایش دانه درشت را برای کاهش درهم ریختگی تنظیم کنید. پیکربندی فیلتر را می توان از ورودی لایه وظیفه در [کنترل لایه] (#لایه) دریافت کرد:

* Notes
* Osmose error
* Osmose warning
* Osmose minor issue
* Maproulette
* Todo

<a id="indoor"></a>

## حالت داخلی

نقشه‌کشی داخلی کار چالش‌برانگیزی است. زیرا چیزهای زیادی هست که معمولاً روی هم می‌افتند. وسپوچی حالت ویژه‌ای برای این منظور دارد که به‌وسیلهٔ آن می‌توانید چیزهایی را که در یک طبقه نیستند فیلتر کنید و همچنین به‌طور خودکار شمارهٔ طبقهٔ کنونی را به همهٔ اشیای نوساخته در آن طبقه می‌افزاید.

این حالت با لمس طولانی روی نماد قفل و انتخاب گزینهٔ مربوطه از منو قابل‌فعال‌سازی است. [حالت قفل، حالت باز، تعویض حالت](#قفل) را ببینید.

<a id="c-mode"></a>

## حالت C

در حالت C فقط چیزهایی نشان داده می‌شود که پرچم هشدار داشته باشند که درنتیجه کمک می‌کند تا چیزهای مشکل‌دار یا چیزهایی را که بر اساس بررسی‌های قابل‌پیکربندی به دست می‌آیند، آسان‌تر بیابیم. در حالت C اگر عنصری انتخاب و ویرایشگر خصوصیت باز شود، بهترین تنظیم از پیش تنظیم شده به طور خودکار اعمال می شود.

این حالت با لمس طولانی روی نماد قفل و انتخاب گزینهٔ مربوطه از منو قابل‌فعال‌سازی است. [حالت قفل، حالت باز، تعویض حالت](#قفل) را ببینید.

### پیکربندی بررسی‌ها

All validations can be disabled/enabled in the "Validator settings/Enabled validations" in the [preferences](Preferences.md). 

The configuration for "Re-survey" entries allows you to set a time after which a tag combination should be re-surveyed. "Check" entries are tags that should be present on objects as determined by matching presets. Entries can be edited by clicking them, the green menu button allows adding of entries.

#### مدخل‌های بازنقشه‌برداری

مدخل‌های نقشه‌برداری مجدد این خصوصیت‌ها را دارند:

*** کلید ** - کلید برچسب مورد علاقه.
*** ارزش ** - مقداری که برچسب مورد علاقه باید داشته باشد ، در صورت خالی ، مقدار برچسب نادیده گرفته می شود.
*** عمر** - چند روز پس از آخرین تغییر عنصر ، عنصر باید مجدداً بررسی شود ، در صورت وجود تگ _check_date_ که مورد استفاده قرار می گیرد ، در غیر این صورت تاریخی که نسخه فعلی ایجاد شد. تنظیم مقدار بر روی صفر منجر به تطبیق چک به سادگی با کلید و مقدار می شود.
*** عبارت منظم ** - اگر علامت زده شود ** مقدار ** یک عبارت معمولی JAVA در نظر گرفته می‌شود.

**کلید** و **مقدار** با تگ‌های _موجود_ در عنصر موردبحث مطابقت داده می‌شود.

گروه _Annotations_ در پیش‌تنظیم‌های استاندارد گزینه‌ای دارد که به‌طور خودکار تگ _check_date_ را همراه با تاریخ جاری اضافه می‌کند.

#### مدخل‌های بررسی

مدخل‌های بررسی این دو خصوصیت را دارند:

* **کلید** - کلیدی که مطابق با پیش‌تنظیم متناظر، باید روی شیء وجود داشته باشد.
* **اختیاریِ لازم** - کلید ضروری است هرچند در پیش‌تنظیم متناظر، جزو کلیدهای اختیاری تعریف شده باشد.

این بررسی، ابتدا پیش‌تنظیم جور را تشخیص می‌دهد و سپس بررسی می‌کند که طبق پیش‌تنظیم، آیا **کلید** برای این عنصر «توصیه‌شده» است. گزینهٔ **اختیاریِ لازم** بررسی را به تگ‌های «اختیاری» عارضه گسترش می‌دهد. توجه: فعلاً پیش‌تنظیم‌های پیوندشده بررسی نمی‌شوند.

## فیلترها

### فیلتر مبتنی بر تگ

فیلتر را می‌توانید از منوی اصلی فعال کنید. سپس با زدن روی نماد فیلتر، آن را تغییر دهید. مستندات بیشتر در این باره در [فیلتر با تگ](Tag%20filter.md) آمده است.

### فیلتر مبتنی بر پیش‌تنظیم

راه دیگری برای کار بالا، فیلتر عارضه‌ها بر اساس پیش‌تنظیم یا گروهی از پیش‌تنظیم‌ها است. با زدن روی نماد پالایه، مشابه با جاهای دیگر وسپوچی، کادری برای انتخاب پیش‌تنظیم باز می‌شود. برای انتخاب پیش‌تنظیم، یک بار لمس کافی است. برای انتخاب گروه پیش‌تنظیم‌ها لمس طولانی بکنید (لمس کوتاه گروه را باز می‌کند). مستندات بیشتر در [فیلتر با پیش‌تنظیم](Preset%20filter.md) آمده است.

## سفارشی‌سازی وسپوچی

برنامه را از جنبه‌های مختلفی می‌توانید سفارشی کنید. اگر به دنبال چیز خاصی هستید و پیدایش نمی‌کنید، [وبسایت وسپوچی](https://vespucci.io/) قابل‌جستجو است و اطلاعاتی علاوه بر اطلاعات موجود روی دستگاه دارد.

<a id="layers"></a>

### تنظیمات لایه

تنظیمات لایه را می توان از طریق کنترل لایه (منوی "همبرگر" در گوشه بالا سمت راست) تغییر داد ، همه تنظیمات دیگر از طریق دکمه تنظیمات منوی اصلی قابل دسترسی است. لایه ها را می توان فعال ، غیرفعال و به طور موقت پنهان کرد.

انواع لایه های موجود:

* Data layer - this is the layer OpenStreetMap data is loaded in to. In normal use you do not need to change anything here. Default: on.
* Background layer - there is a wide range of aerial and satellite background imagery available. The default value for this is the "standard style" map from openstreetmap.org.
* Overlay layer - these are semi-transparent layers with additional information, for example quality assurance information. Adding an overlay may cause issues with older devices and such with limited memory. Default: none.
* Notes/Bugs display - Open Notes and bugs will be displayed as a yellow bug icon, closed ones the same in green. Default: on.
* Photo layer - Displays geo-referenced photographs as red camera icons, if direction information is available the icon will be rotated. Default: off.
* Mapillary layer - Displays Mapillary segments with markers where images exist, clicking on a marker will display the image. Default: off.
* GeoJSON layer - Displays the contents of a GeoJSON file, multiple layers can be added from files. Default: none.
* GPX layer - Displays GPX tracks and way points, multiple layers can be added from files, during recording the generate GPX track is displayed in its own one . Default: none.
* Grid - Displays a scale along the sides of the map or a grid. Default: on. 

اطلاعات بیشتر را می توانید در بخش [نمایش نقشه] (Main%20map%20display.md) مشاهده کنید.

#### ترجیحات

* روشن‌ماندن صفحه. پیشفرض: خاموش.
* فضای بزرگ برای کشیدن گره. جابه‌جاکردن گره‌ها در دستگاهی با ورودی لمسی مشکل است، زیرا انگشت‌هایتان موقعیت جاری روی صفحه را مبهم می‌کند. با روشن‌کردن این گزینه، فضای بزرگی پیرامون عنصر ایجاد می‌شود که با استفاده از آن می‌توانید عنصر را جابه‌جا کنید (انتخاب و سایر کنش‌ها هنوز در «فضای انتخاب» عنصر کار می‌کنند). پیشفرض: خاموش.

توضیحات کامل را اینجا ببینید [ترجیحات](Preferences.md)

#### ترجیحات پیشرفته

* Full screen mode. On devices without hardware buttons Vespucci can run in full screen mode, that means that "virtual" navigation buttons will be automatically hidden while the map is displayed, providing more space on the screen for the map. Depending on your device this may work well or not,  In _Auto_ mode we try to determine automatically if using full screen mode is sensible or not, setting it to _Force_ or _Never_ skips the automatic check and full screen mode will always be used or always not be used respectively. On devices running Android 11 or higher the _Auto_ mode will never turn full screen mode on as Androids gesture navigation provides a viable alternative to it. Default: _Auto_.  
* Node icons. Default: _on_.
* Always show context menu. When turned on every selection process will show the context menu, turned off the menu is displayed only when no unambiguous selection can be determined. Default: off (used to be on).
* Enable light theme. On modern devices this is turned on by default. While you can enable it for older Android versions the style is likely to be inconsistent. 

توضیحات کامل را اینجا ببینید [ترجیحات پیشرفته](Advanced%20preferences.md)

## Reporting and Resolving Issues

اگر وسپوچی خراب شود یا حالتی ناسازگار را تشخیص دهد، از شما خواسته می شود که برگرفتِ خرابی را ارسال کنید. لطفاً اگر چنین شد، این کار را انجام دهید، اما فقط یک بار در هر موقعیت خاص. اگر می‌خواهید اطلاعات بیشتری بدهید یا برای درخواست ویژگی یا موارد مشابه مسئله‌ای باز کنید، لطفاً این کار را در اینجا انجام دهید: [پیگیر مسائل وسپوچی] (https://github.com/MarcusWolschon/osmeditor4android/issues). عملکرد «ارائه بازخورد» از منوی اصلی مسئلهٔ جدیدی باز می‌کند و اطلاعات مربوط به برنامه و دستگاه را بدون تایپ اضافی شامل می‌شود.

If you are experiencing difficulties starting the app after a crash, you can try to start it in _Safe_ mode on devices that support short cuts: long press on the app icon and then select _Safe_ from the menu. 

If you want to discuss something related to Vespucci, you can either start a discussion on the [OpenStreetMap forum](https://community.openstreetmap.org).


