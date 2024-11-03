_Before we start: most screens have links in the menu to the on-device help system giving you direct access to information relevant for the current context, you can easily navigate back to this text too. If you have a larger device, for example a tablet, you can open the help system in a separate split window.  All the help texts and more (FAQs, tutorials) can be found on the [Vespucci documentation site](https://vespucci.io/) too. You can further start the help viewer directly on devices that support short cuts with a long press on the app icon and selecting "Help"_

# مقدمة فسبوتشي

Vespucci is a full featured OpenStreetMap editor that supports most operations that desktop editors provide. It has been tested successfully on Google's Android 2.3 to 14.0 (versions prior to 4.1 are no longer supported) and various AOSP based variants. A word of caution: while mobile device capabilities have caught up with their desktop rivals, particularly older devices have very limited memory available and tend to be rather slow. You should take this in to account when using Vespucci and keep, for example, the areas you are editing to a reasonable size.

## التحرير باستخدام فسبوتشي Vespucci

اعتمادًا على حجم الشاشة وعمر إجراءات التحرير الخاصة بجهازك ، يمكن الوصول إليها إما مباشرة عبر الرموز الموجودة في الشريط العلوي ، أو عبر قائمة منسدلة على يمين الشريط العلوي ، أو من الشريط السفلي (إن وجد) أو عبر زر القائمة.

<a id="download"></a>

### تنزيل بيانات خريطة الشارع المفتوحة

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

أسهل طريقة لتنزيل البيانات على الجهاز هي التكبير والتحريك إلى الموقع الذي تريد تعديله ثم تحديد "تنزيل العرض الحالي". يمكنك التكبير باستخدام الإيماءات أو أزرار التكبير أو أزرار التحكم في مستوى الصوت على جهازك. سيقوم برنامج فسبوتشي بعد ذلك بتنزيل البيانات للعرض الحالي. لا يلزم المصادقة وتسجيل الدخول بحساب لتنزيل البيانات على جهازك.

In unlocked state any non-downloaded areas will be dimmed relative to the downloaded ones if you are zoomed in far enough to enable editing. This is to avoid inadvertently adding duplicate objects in areas that are not being displayed. In the locked state dimming is disabled, this behaviour can be changed in the [Advanced preferences](Advanced%20preferences.md) so that dimming is always active.

If you need to use a non-standard OSM API entry, or use [offline data](https://vespucci.io/tutorials/offline/) in _MapSplit_ format you can add or change entries via the _Configure..._ entry for the data layer in the layer control.

### التحرير

<a id="lock"></a>

#### وضع القفل ، الفتح ، تبديل الوضع

لتجنب التعديلات غير المقصودة ، يبدأ فسبوتشي في وضع "القفل" ، وهو وضع يسمح فقط بتكبير الخريطة وتحريكها. اضغط على أيقونة! [مغلق] (../images/locked.png) لفتح الشاشة. 

A long press on the lock icon or the _Modes_ menu in the map display overflow menu will display a menu offering 4 options:

* **Normal** - the default editing mode, new objects can be added, existing ones edited, moved and removed. Simple white lock icon displayed.
* **Tag only** - selecting an existing object will start the Property Editor, new objects can be added via the green "+" button, or long press, but no other geometry operations are enabled. White lock icon with a "T" is displayed.
* **Address** - enables Address mode, a slightly simplified mode with specific actions available from the [Simple mode](../en/Simple%20actions.md) "+" button. White lock icon with an "A" is displayed.
* **Indoor** - enables Indoor mode, see [Indoor mode](#indoor). White lock icon with an "I" is displayed.
* **C-Mode** - enables C-Mode, only objects that have a warning flag set will be displayed, see [C-Mode](#c-mode). White lock icon with a "C" is displayed.

If you are using Vespucci on an Android device that supports short cuts (long press on the app icon) you can start directly to _Address_ and _Indoor_ mode.

#### نقرة واحدة أو نقرتين مزدوجة أو ضغطة طويلة

بشكل افتراضي ، تحتوي العقد/النقاط والطرق/الخطوط القابلة للتحديد على منطقة برتقالية حولها تشير تقريبًا إلى المكان الذي يجب أن تلمسه لتحديدها. ولديك ثلاثة خيارات:

* نقرة واحدة: لتحديد العنصر.
 * يتم تمييز العقدة / الطريق بشكل واضح على الفور.
 * ومع ذلك ، إذا حاولت تحديد عنصر وكان في مكان التحديد عناصر متعددة ، فسيقدم لك البرنامج قائمة اختيار ، مما يتيح لك اختيار العنصر الذي ترغب في تحديده بالضبط.
 * يتم تمييز العناصر المحددة باللون الأصفر.
 * لمزيد من المعلومات ، راجع[Node selected](Node%20selected.md), [Way selected](Way%20selected.md) و [Relation selected](Relation%20selected.md)
* انقر نقرًا مزدوجًا: لبدء وضع التحديد المتعدد [Multiselect mode](Multiselect.md)
* الضغط لفترة طويلة وبشكل مطول: لإنشاء "علامة متقاطعة" ، مما يتيح لك إضافة العقد ، انظر أدناه و [Creating new objects](Creating%20new%20objects.md). يتم تمكين هذا فقط إذا تم إلغاء تنشيط "الوضع البسيط".

إذا حاولت تحرير منطقة كثافة عالية من البيانات فمن المفضل أن تقوم بالتكبير قدر الأمكان لضمان عدم نفاذ ذاكرة الجهاز.

يحتوي تطبيق فسبوتشي على نظام "تراجع / إعادة الفعل" بشكل جيد ، لذا لا تخف من إجراء التجارب على جهازك ، ولكن يُرجى عدم رفع وحفظ بيانات الاختبار والتجارب.

#### التحديد / إلغاء التحديد (نقرة واحدة و "قائمة الاختيار")

المس عنصراً لتحديده وتمييزه. سيؤدي لمس الشاشة في منطقة فارغة إلى إلغاء التحديد. إذا قمت بتحديد عنصر وتحتاج إلى تحديد شيء آخر ، فما عليك سوى لمس العنصر المعني ، فلا داعي لإلغاء التحديد أولاً. سيبدأ النقر المزدوج العنصر في [Multiselect mode](Multiselect.md).

لاحظ أنه إذا حاولت تحديد عنصر ولاحظ تطبيق فسبوتشي أن التحديد يمكن أن يعني عناصر متعددة (مثل عقدة على طريقة أو عناصر أخرى متداخلة) فسيقدم قائمة لتختار منها: انقر فوق العنصر الذي ترغب في تحديده وسيكون هو العنصر المحدد. 

يشار إلى العناصر المحددة من خلال حد أصفر رفيع. قد يكون من الصعب تحديد الحد الأصفر ، اعتمادًا على خلفية الخريطة وعامل التكبير / التصغير. بمجرد إجراء التحديد ، سترى تنبيها يؤكد الاختيار.

بمجرد اكتمال التحديد ، سترى (إما كأزرار أو كعناصر قائمة) قائمة بالعمليات المدعومة للعنصر المحدد: لمزيد من المعلومات ، راجع [Node selected](Node%20selected.md), [Way selected](Way%20selected.md) and [Relation selected](Relation%20selected.md).

#### العناصر المحددة: تحرير الوسوم

لمسة ثانية على العنصر المحدد ستفتح لك محرر الوسوم ويمكنك تحرير الوسوم المرتبطة بالعنصر.

لاحظ أنه بالنسبة للعناصر المتداخلة (مثل عقدة ما في طريق ما) ، فإن قائمة التحديد ستظهر لك مرة أخرى عند تحديد نفس العنصر وسيظهر محرر الوسوم؛ وتحديد عنصر آخر سيؤدي لاختيار العنصر الآخر بكل بساطة.

#### العناصر المحددة: تحريك عقدة أو طريق

بمجرد تحديد عنصر، يمكن نقله. لاحظ أنه يمكن سحب/نقل العناصر فقط عند تحديدها، ما عليك سوى السحب بالقرب من (أي مكان من داخل منطقة التسامح) للعنصر المحدد لتحريكه. إذا قمت بتحديد مساحة السحب الكبيرة في [preferences](Preferences.md), فستحصل على مساحة كبيرة حول العقدة المحددة مما يسهل عليك إعادة تموضع اي عنصر. 

#### إضافة عقدة/ نقطة جديدة أو طريق جديد 

عند بدء تشغيل التطبيق لأول مرة في "الوضع البسيط" ، يمكن تغيير ذلك في القائمة الرئيسية عن طريق إلغاء تحديد مربع الاختيار المقابل.

##### الوضع البسيط

سيؤدي النقر فوق الزر العائم الأخضر الكبير على شاشة الخريطة إلى إظهار قائمة. بعد تحديد أحد العناصر، سيُطلب منك النقر على الشاشة في الموقع الذي تريد إنشاء العنصر فيه ، ويستمر التحريك والتكبير / التصغير في العمل إذا كنت بحاجة إلى ضبط عرض الخريطة. 

See [Creating new objects in simple actions mode](Simple%20actions.md) for more information. Simple mode os the default for new installs.

##### الوضع المتقدم (الضغط لفترة طويلة)

اضغط لفترة طويلة حيث تريد أن تكون العقدة أو بداية الطريق. سترى رمز "علامة التقاطع" سوداء.
* إذا كنت تريد إنشاء عقدة جديدة (غير متصلة بعنصر) ، فالمس بعيدًا عن العناصر الموجودة.
* إذا كنت تريد تمديد طريق ، فالمس داخل "منطقة التغيير" الخاصة بالطريق (أو على عقدة في الطريق). يشار إلى منطقة التغيير بالمناطق المحيطة بالعقدة أو الطريق.

بمجرد أن ترى رمز التقاطع ، لديك هذه الخيارات:

* _الضغط العادي في نفس المكان ._
* إذا لم يكن التقاطع بالقرب من عقدة ، فإن لمس نفس الموقع مرة أخرى يؤدي إلى إنشاء عقدة جديدة. إذا كنت بالقرب من طريق (ولكن ليس بالقرب من عقدة) ، فستكون العقدة الجديدة في الطريق (ومتصلة بالطريق).
* إذا كان التقاطع بالقرب من عقدة (أي داخل منطقة التغيير الخاصة بالعقدة) ، فإن لمس نفس الموقع يؤدي فقط إلى تحديد العقدة (ويفتح محرر الوسوم. ولن يتم إنشاء عقدة جديدة. والإجراء سيكون على نفس التحديد أعلاه.
* _ اللمس العادي في مكان آخر. _ لمس موقع آخر (خارج منطقة تغيير الخطوط المتقاطعة) يضيف مقطع طريق من الموضع الأصلي إلى الموضع الحالي. إذا كان التقاطع بالقرب من طريق أو عقدة ، فسيتم توصيل المقطع الجديد بتلك العقدة أو الطريق.

ما عليك سوى لمس الشاشة حيث تريد إضافة المزيد من العقد على الطريق. للإنهاء ، المس العقدة الأخيرة مرتين. إذا كانت العقدة النهائية موجودة في طريق أو عقدة ، فسيتم توصيل المقطع بالطريق أو العقدة تلقائيًا. 

يمكنك أيضًا استخدام عنصر قائمة: انظر إنشاء عناصر جديدة [Creating new objects](Creating%20new%20objects.md)  للمزيد من المعلومات.

#### إضافة مساحة شيء ما

لا تحتوي خرائط الشارع المفتوحة حاليًا على نوع عنصر"مساحة" بخلاف أنظمة البيانات الجغرافية الأخرى. يحاول محرر "iD" الذي يعمل على الإنترنت "iD" إنشاء تجريد مساحة من عناصر OSM الأساسية وهذا التجريد يعمل بشكل جيد في بعض الظروف، وفي حالات أخرى لا تكون كذلك. لا يحاول محرر فسبوتشي حاليًا القيام بأي شيء مماثل، لذلك عليك أن تعرف القليل عن الطريقة التي يتم بها تمثيل المساحات في هذا البرنامج:

* _ الطرق المغلقة (* المضلعات ") _: أبسط وأشهر الأشكال التي تمثل مساحة شيء ما، والطرق المغلقة هلي التي تكون العقدة الأولى والأخيرة مشتركة مشكلة"حلقة"مغلقة حيث يلتقي أول الخط بآخره (على سبيل المثال ، معظم المباني في الخريطة من هذا النوع). من السهل جدًا القيام بذلك الإنشاء على تطبيق فسبوتشي، ما عليك سوى توصيل العقدة الأخيرة بأول عقدة في الخط عند الانتهاء من رسم المساحة. ملاحظة: يعتمد تفسير ماهية الطريق المغلق على الوسوم التي عليه: على سبيل المثال ، إذا تم وضع وسم على الطريق المغلق كمبنى ، فسيتم اعتباره مساحة لمنطقة معينة، و لكن إذا تم تمييزه على أنه تقاطع دائري فلن يعتبر مساحة لمنطقة معينة، كما أنه في بعض الحالات التي قد يكون فيها كلا التفسيرين صحيحة عند ذلك يمكن أن يوضح الوسم "area" الاستخدام المقصود.
* _المضلعات المتداخلة_: تحتوي بعض المساحات على أجزاء متعددة وثقوب وحلقات لا يمكن تمثيلها بطريقة واحدة فقط. لذا ففي خرائط الشارع المفتوحة يتم استخدام نوعًا محددًا من العناصر يدعى بالعلاقة (وهي عنصر الغرض العام منه هو تمكين نمذجة العلاقات بين العناصر المختلفة وربطها مع بعضها) للالتفاف حول هذا ، فإن المضلعات المتعددة يمكن أن يحتوي المضلع المتعدد على أطراف "خارجية" متعددة وأطراف "داخلية" متعددة. يمكن أن تكون كل حلقة إما طريقاً مغلقاً كما هو موضح أعلاه ، أو طرق فردية متعددة لها عقد نهاية مشتركة. بينما يصعب التعامل مع المضلعات الكبيرة المتعددة باستخدام أي أداة ، إلا أنه ليس من الصعب إنشاء المضلعات الصغيرة في فسبوتشي.
* _ الخطوط الساحلية_: بالنسبة للأجسام الكبيرة جدًا والقارات والجزر ، حتى نموذج المضلعات المتعددة لا يعمل بطريقة مرضية. بالنسبة للسواحل natural=coastline، نفترض دلالات تعتمد على الاتجاه: الأرض على الجانب الأيسر من الطريق ، والمياه على الجانب الأيمن. أحد الآثار الجانبية لذلك ، بشكل عام ، أنه لا يجب عليك عكس اتجاه طريق ما باستخدام وسوم الخط الساحلي. يمكن العثور على مزيد من المعلومات على [OSM wiki](http://wiki.openstreetmap.org/wiki/Tag:natural%3Dcoastline).

#### تحسين هندسة وشكل الطرق

إذا قمت بالتكبير بشكل كافٍ للطريق المحدد ، فسترى "x" صغيرًا في منتصف الطريق وفي أجزائه الطويلة بما يكفي. سيؤدي سحب "x" إلى إنشاء عقدة في الطريق في ذلك الموقع. ملاحظة: لتجنب إنشاء العقد عن طريق الخطأ ، تكون منطقة التغيير باللمس لهذه العملية صغيرة إلى حد ما.

#### قص ونسخ ولصق

You can copy selected nodes and ways, and then paste once or multiple times to a new location. Cutting will retain the osm id and version, thus can only be pasted once. To paste long press the location you want to paste to (you will see a cross hair marking the location). Then select "Paste" from the menu.

#### إضافة العناوين بكفاءة

يدعم فسبوتشي الوظائف التي تجعل عناوين المسح أكثر كفاءة من خلال التنبؤ بأرقام المنازل (الجانبين الأيسر والأيمن للشوارع بشكل منفصل) وإضافة وسوم _addr:street_ أو _addr:place_ بناءً على آخر قيمة مستخدمة والقرب في أفضل الأحوال، وهذا يسمح بإضافة العنوان بدون الحاجة لكتابة أي شيء على الإطلاق.   

يمكن بدء إضافة الوسوم بالضغط على ![Address](../images/address.png): 

* بعد ضغطة طويلة (وضع غير بسيط فقط): سيضيف فسبوتشي عقدة في الموقع ويقدم أفضل تخمين لرقم المنزل ويضيف وسوم العنوان التي كنت تستخدمها مؤخرًا. إذا كانت العقدة موجودة في مخطط المبنى ، فسيضاف تلقائيًا وسم "entrance=yes" إلى العقدة. سيفتح محرر الوسوم للعنصر المعني ويسمح لك بإجراء أي تغييرات إضافية ضرورية.
* في أوضاع العقدة / الطريق المحددة: سيضيف فسبوتشي وسوم العنوان على النحو الوارد أعلاه ويبدأ محرر الوسوم.
* في محرر الخصائص.

لإضافة عقد عنوان فردية مباشرةً أثناء التواجد في الوضع الافتراضي "الوضع البسيط" ، قم بالتبديل إلى وضع تحرير "العنوان" (عبر الضغط لفترة طويلة على زر القفل) ، ستضيف "إضافة عقدة العنوان" بعد ذلك عقدة عنوان في الموقع وإذا كانت في وضع مخطط المبنى أضف وسم دخول إليه كما هو موضح أعلاه.

يتطلب التنبؤ برقم المنزل عادةً إدخال رقمين للمنزل على الأقل على كل جانب من الطريق للعمل ، وكلما زاد عدد الأرقام الموجودة في البيانات كان ذلك أفضل.

ضع في اعتبارك استخدام هذا مع أحد أوضاع التنزيل التلقائي [Auto-download](#download).  

#### إضافة قيود الدوران والالتفاف المروري

لدى فسبوتشي طريقة سريعة لإضافة قيود الانعطاف المرورية. إذا لزم الأمر ، فسيقوم بتقسيم الطرق تلقائيًا ويطلب منك إعادة تحديد العناصر. 

* حدد الطريق الموسوم بوسم الطريق السريع highway (لا يمكن إضافة قيود الانعطاف المروري إلا على الطرق السريعة)، وإذا كنت تريد القيام بذلك على الطرق الأخرى، فيرجى استخدام الوضع العام لـ "إنشاء علاقة" 
* حدد "إضافة قيود" من القائمة
* حدد "عبر" عقدة أو طريق (فقط العناصر الممكنة "عبر" ستظهر في منطقة اللمس)
* حدد "إلى" الطريق (من الممكن التكرار مرة أخرى وتعيين عنصر "إلى" على العنصر "من" ، وسيفترض تطبيق فسبوتشي أنك تضيف قيد عدم دوران no_u_turn)
* اضبط نوع القيد

### فسبوتشي في وضع "مغلق"

عند يكون القفل أعلى الشاشة باللون الأحمر، فلن تتوفر الإجراءات المتعلقة بالتحرير،كما أن الضغط لفترة طويلة على العنصر أو بالقرب منه يؤدي إلى عرض شاشة معلومات تفاصيل العنصر إذا كان عنصراً من عناصر خريطة الشارع المفتوحة.

### حفظ التغييرات الخاصة بك

*(لابد من وجود اتصال بالشبكة)*

اضغط نفس الزر أو على عنصر القائمة الذي قمت به للتنزيل وحدد الآن "رفع البيانات إلى خادم خريطة الشارع المفتوحة".

Vespucci supports OAuth 2, OAuth 1.0a authorization and the classical username and password method. Since July 1st 2024 the standard OpenStreetMap API only supports OAuth 2 and other methods are only available on private installations of the API or other projects that have repurposed OSM software.  

Authorizing Vespucci to access your account on your behalf requires you to one time login with your display name and password. If your Vespucci install isn't authorized when you attempt to upload modified data you will be asked to login to the OSM website (over an encrypted connection). After you have logged on you will be asked to authorize Vespucci to edit using your account. If you want to or need to authorize the OAuth access to your account before editing there is a corresponding item in the "Tools" menu.

إذا كنت تريد حفظ عملك وليس لديك اتصال بالإنترنت، فيمكنك حفظ تعديلاتك على ملف بصيغة .osm وهو متوافق مع محرر JOSM ومن ثم يمكنك رفعه لاحقًا باستخدام تطبيق فسبوتشي أو باستخدام JOSM. 

#### حل التعارضات عند قيامك بالرفع

لدى تطبيق فسبوتشي أداة حل تعارضات بسيطة، ومع ذلك ،إذا كنت تشك في وجود مشكلات كبيرة في تعديلاتك، فقم بتصدير التغييرات إلى ملف بصيغة .osc (اختر من القائمة "تصدير" في قائمة "نقل") وقم بإصلاحها ورفعها باستخدام JOSM. راجع المساعدة التفصيلية حول حل التعارض [conflict resolution](Conflict%20resolution.md).  

### Nearby point-of-interest display

A nearby point-of-interest display can be shown by pulling the handle in the middle and top of the bottom menu bar up. 

More information on this and other available functionality on the main display can be found here [Main map display](Main%20map%display.md).

## استخدام مسارات GPS و GPX

من خلال الإعدادات القياسية، سيحاول فسبوتشي تمكين خدمة GPS (وأنظمة الملاحة الأخرى المعتمدة على الأقمار الصناعية) وسيعود إلى تحديد الموقع عبر ما يسمى "موقع الشبكة" إذا لم يكن ذلك ممكنًا. يفترض هذا السلوك أنه في الاستخدام العادي سيكون جهاز أندرويد الخاص بك قد تم تكوينه لاستخدام المواقع التي تم إنشاؤها بواسطة GPX فقط (لتجنب التعقب)، وهذا يعني أنه تم إيقاف تشغيل خيار "تحسين دقة الموقع" المسمى بشكل ملطف، فإذا كنت تريد تمكين الخيار ولكنك تريد تجنب عودة فسبوتشي إلى "موقع الشبكة" ، فيجب عليك إيقاف تشغيل الخيار المقابل في الإعدادات المتقدمة [Advanced preferences](Advanced%20preferences.md). 

Touching the ![GPS](../images/menu_gps.png) button (normally on the left hand side of the map display) will center the screen on the current position and as you move the map display will be panned to maintain this.  Moving the screen manually or editing will cause the "follow GPS" mode to be disabled and the blue GPS arrow will change from an outline to a filled arrow. To quickly return to the "follow" mode, simply touch GPS button or re-check the equivalent menu option. If the device doesn't have a current location the location marker/arrow will be displayed in black, if a current location is available the marker will be blue.

لتسجيل مسار GPX وعرضه على جهازك ، حدد عنصر "بدء مسار GPX" في قائمة ![GPS](../images/menu_gps.png)، كما أن ذلك سيؤدي إلى إضافة طبقة إلى شاشة العرض تتضمن المسار الحالي المسجل، كما يمكنك تحميل وتصدير المسار من الإدخال عبر التحكم في الطبقة [layer control](Main%20map%20display.md)، كما يمكن إضافة طبقات أخرى من ملفات GPX المحلية والمسارات التي تم تنزيلها من الواجهة البرمجية لـOSM.

ملاحظة: افتراضيًا ، لن يقوم محرر فسبوتشي بتسجيل بيانات الارتفاع باستخدام مسار GPX الخاص بك ، ويرجع ذلك إلى بعض المشكلات الخاصة بنظام أندرويد، ولتمكين تسجيل الارتفاع قم بتثبيت نموذج جاذبية أو بشكل أبسط انتقل إلى الإعدادات المتقدمة [Advanced preferences](Advanced%20preferences.md) ثم قم بتكوين إعدادات NMEA.

### How to export a GPX track?

Open the layer menu, then click the 3-dots menu next to "GPX recording", then select **Export GPX track...**. Choose in which folder to export the track, then give it a name suffixed with `.gpx` (example: MyTrack.gpx).

## Notes, Bugs and Todos

يدعم تطبيق فسبوتشي تنزيل ملاحظات خريطة الشارع المفتوحة OSM Notes والتعليق عليها وإغلاقها (المعروفة سابقًا باسم OSM Bugs) والوظائف المكافئة لـ "Bugs" التي تنتجها [أداة ضمان جودة OSMOSE](http://osmose.openstreetmap.fr/en/map/). كلاهما يجب أن يتم تنزيلهما بشكل صريح أو يمكنك استخدام مرفق التنزيل التلقائي للوصول إلى العناصر الموجودة في منطقتك الحالية. بمجرد التحرير أو الإغلاق ، يمكنك إما رفع تعديلات الخطأ أو الملاحظة على الفور أو رفع الكل مرة واحدة. 

Further we support "Todos" that can either be created from OSM elements, from a GeoJSON layer, or externally to Vespucci. These provide a convenient way to keep track of work that you want to complete. 

On the map the Notes and bugs are represented by a small bug icon ![Bug](../images/bug_open.png), green ones are closed/resolved, blue ones have been created or edited by you, and yellow indicates that it is still active and hasn't been changed. Todos use a yellow checkbox icon.

The OSMOSE bug and Todos display will provide a link to the affected element in blue (in the case of Todos only if an OSM element is associated with it), touching the link will select the object, center the screen on it and down load the area beforehand if necessary. 

### الفلتره والتصفية

إلى جانب التمكين العام لعرض الملاحظات والمشاكل، يمكنك تحديد مصفي لعرض الحبوب الخشنة لتقليل الفوضى. يمكن الوصول إلى تكوين المصفي من إدخال طبقة المهمة في [التحكم في الطبقة](#layers):

* Notes
* Osmose error
* Osmose warning
* Osmose minor issue
* Maproulette
* Todo

<a id="indoor"></a>

## الوضع الداخلي

يمثل رسم الخرائط للعناصر التي داخل المباني تحديًا نظرًا للعدد الكبير من العناصر التي غالبًا ما تتراكب مع بعضها البعض، لذا فإن تطبيق فسبوتشي يحتوي على وضع داخلي مخصص يسمح لك بتصفية جميع الكائنات التي ليست على نفس المستوى والتي ستضيف وبشكل تلقائي المستوى الحالي إلى العناصر الجديدة التي تم إنشاؤها.

يمكن تمكين الوضع من خلال الضغط لفترة طويلة على رمز القفل ، راجع [وضع القفل ، الفتح ، تبديل الوضع](#lock) واختيار إدخال القائمة المقابل.

<a id="c-mode"></a>

## وضع الاكمال

في وضع الاكمال يتم عرض العناصر التي يوجد عليها علامة تحذير، وهذا يسهل عليك تحديد العناصر التي بها مشاكل محددة أو تطابق أشكال المشاكل المعينة التي قمت بإعدادها لتظهر لك. إذا تم تحديد عنصر وبدأ محرر الخصائص في وضع الاكمال، فسيتم تطبيق أفضل قالب مطابق بشكل تلقائي.

يمكن تمكين الوضع من خلال الضغط لفترة طويلة على رمز القفل ، راجع [وضع القفل ، الفتح ، تبديل الوضع](#lock) واختيار إدخال القائمة المقابل.

### إعداد الفحوصات

All validations can be disabled/enabled in the "Validator settings/Enabled validations" in the [preferences](Preferences.md). 

The configuration for "Re-survey" entries allows you to set a time after which a tag combination should be re-surveyed. "Check" entries are tags that should be present on objects as determined by matching presets. Entries can be edited by clicking them, the green menu button allows adding of entries.

#### مدخلات إعادة الدراسة والاستطلاع

تحتوي مدخلات إعادة الدراسة والاستطلاع على الخصائص التالية:

* **المفتاح** - مفتاح الوسوم التي تهتم لها.
* **القيمة** - القيمة التي يجب أن يحتوي عليها الوسوم التي تهتم لها، إذا كانت فارغة ، فسيتم تجاهل قيمة الوسم.
* **العمر** - كم عدد الأيام بعد آخر تغيير للعنصر ، يجب إعادة مسح العنصر ، إذا كانت علامة _check_date_ موجودة والتي ستكون هي المستخدمة ، وإلا فسيتم إنشاء الإصدار الحالي. سيؤدي تعيين القيمة إلى الصفر إلى مطابقة الاختيار ببساطة مع المفتاح والقيمة فقط.
* **التعبير العادي Regular expression** - إذا تم تحديده ، فمن المفترض أن تكون ** القيمة ** تعبيرًا عاديًا لـ JAVA.

يتم فحص **المفتاح** و **القيمة** مقابل الوسوم _الموجودة_ للعنصر المعني.

تحتوي مجموعة _Annotations_ في القوالب القياسية على عنصر سيضيف تلقائيًا وسم _check_date_ بالتاريخ الحالي.

#### تدقيق وفحص الإدخالات

ميزة تدقيق الإدخالات لها الخاصيتان التاليتان:

* **مفتاح** - المفتاح الذي يجب أن يكون موجودًا على العنصر وفقًا للقالب المطابق.
* **متطلب اختياري** - اطلب المفتاح حتى إذا كان المفتاح موجودًا في الوسوم الاختيارية للقالب المطابق.

يعمل هذا الفحص عن طريق تحديد القالب المطابق أولاً ثم التحقق مما إذا كان **المفتاح** هو مفتاح "موصى به" لهذا العنصر وفقًا للقالب، أما ما هو **مطلوب اختياريًا** فإن إدخاله سيؤدي إلى توسيع نطاق التحقق ليشمل الوسوم "الاختيارية* في العنصر. ملاحظة: لم يتم فحص القوالب المرتبطة حاليًا..

## الفلاتر والمصفيات

### التصفية بناءاً على الوسم

يمكن تمكين فلتر الوسوم من القائمة الرئيسية، ويمكن بعد ذلك تغييره من خلال النقر على أيقونة الفلتر في الشاشة الرئيسية. يمكن العثور على المزيد من الوثائق هنا فلتر الوسوم [Tag filter](Tag%20filter.md).

### الفلترة بناءاً على القالب

كبديل لما ورد أعلاه، يتم تصفية العناصر إما حسب القوالب الفردية أو على مجموعات قوالب. سيؤدي النقر فوق أيقونة المصفي/الفلتر إلى عرض نافذة للاختيار بين العناصر المتعارف عليها في أماكن أخرى في تطبيق فسبوتشي. كما يمكن تحديد القوالب بشكل فردي بنقرة عادية، ولتحديد مجموعات منها اضغط ضغطة مطولة عليها (النقر العادي يدخل المجموعة). يمكن العثور على المزيد من الوثائق هنا فلتر القوالب [Preset filter](Preset%20filter.md).

## تخصيص فسبوتشي

يمكن تخصيص العديد من جوانب التطبيق ، إذا كنت تبحث عن شيء محدد ولا يمكنك العثور عليه ، فإنه يمكنك البحث في [موقع Vespucci](https://vespucci.io/) كما أنه يحتوي على معلومات إضافية حول ما هو متاح على الجهاز.

<a id="layers"></a>

### إعدادات الطبقات

يمكن تغيير إعدادات الطبقات عن طريق التحكم في الطبقة (قائمة "hamburger" في الزاوية العليا على اليمين) ، ويمكن الوصول إلى جميع الإعدادات الأخرى عبر زر تفضيلات القائمة الرئيسية. كما يمكن تمكين الطبقات وتعطيلها وإخفائها مؤقتًا.

أنواع الطبقات المتاحة:

* Data layer - this is the layer OpenStreetMap data is loaded in to. In normal use you do not need to change anything here. Default: on.
* Background layer - there is a wide range of aerial and satellite background imagery available. The default value for this is the "standard style" map from openstreetmap.org.
* Overlay layer - these are semi-transparent layers with additional information, for example quality assurance information. Adding an overlay may cause issues with older devices and such with limited memory. Default: none.
* Notes/Bugs display - Open Notes and bugs will be displayed as a yellow bug icon, closed ones the same in green. Default: on.
* Photo layer - Displays geo-referenced photographs as red camera icons, if direction information is available the icon will be rotated. Default: off.
* Mapillary layer - Displays Mapillary segments with markers where images exist, clicking on a marker will display the image. Default: off.
* GeoJSON layer - Displays the contents of a GeoJSON file, multiple layers can be added from files. Default: none.
* GPX layer - Displays GPX tracks and way points, multiple layers can be added from files, during recording the generate GPX track is displayed in its own one . Default: none.
* Grid - Displays a scale along the sides of the map or a grid. Default: on. 

يمكن العثور على مزيد من المعلومات في القسم الموجود على عرض الخريطة [map display](Main%20map%20display.md).

#### التفضيلات

* حافظ على الشاشة قيد التشغيل. وهو خيار معطل بشكل افتراضي.
* منطقة سحب عقدة كبيرة. يعد تحريك العقد على جهاز يتم التحكم فيه عن طريق اللمس مشكلة نظرًا لأن أصابعك ستحجب الموضع الحالي على الشاشة. سيؤدي تشغيل هذا إلى توفير مساحة كبيرة يمكن استخدامها للسحب خارج مركز الشاشة (لا يزال التحديد والعمليات الأخرى تستخدم منطقة تحمل اللمس العادية). وهو خيار معطل بشكل افتراضي.

يمكن العثور على الشرح الكامل هنا في التفضيلات [Preferences](Preferences.md)

#### التفضيلات المتقدمة

* Full screen mode. On devices without hardware buttons Vespucci can run in full screen mode, that means that "virtual" navigation buttons will be automatically hidden while the map is displayed, providing more space on the screen for the map. Depending on your device this may work well or not,  In _Auto_ mode we try to determine automatically if using full screen mode is sensible or not, setting it to _Force_ or _Never_ skips the automatic check and full screen mode will always be used or always not be used respectively. On devices running Android 11 or higher the _Auto_ mode will never turn full screen mode on as Androids gesture navigation provides a viable alternative to it. Default: _Auto_.  
* Node icons. Default: _on_.
* Always show context menu. When turned on every selection process will show the context menu, turned off the menu is displayed only when no unambiguous selection can be determined. Default: off (used to be on).
* Enable light theme. On modern devices this is turned on by default. While you can enable it for older Android versions the style is likely to be inconsistent. 

يمكن العثور على الشرح الكامل هنا في شرح التفضيلات المتقدمة [Advanced preferences](Advanced%20preferences.md)

## Reporting and Resolving Issues

في حال تعطل تطبيق فسبوتشي Vespucci، أو اكتشاف حالة غير متناسقة ، سيُطلب منك إرسال ملف سجل الأعطال، يرجى القيام بذلك إذا حدث ذلك ، ولكن من فضلك قم بذلك مرة واحدة فقط لكل مشكلة محددة. إذا كنت ترغب في تقديم المزيد من المدخلات أو فتح مشكلة لطلب ميزة أو ما شابه ، فالرجاء القيام بذلك هنا: [Vespucci issue tracker](https://github.com/MarcusWolschon/osmeditor4android/issues). ستؤدي وظيفة "تقديم التعليقات" من القائمة الرئيسية إلى فتح مشكلة جديدة وتضمين المعلومات ذات الصلة ومعلومات الجهاز دون الحاجة إلى كتابة بيانات إضافية.

If you are experiencing difficulties starting the app after a crash, you can try to start it in _Safe_ mode on devices that support short cuts: long press on the app icon and then select _Safe_ from the menu. 

If you want to discuss something related to Vespucci, you can either start a discussion on the [OpenStreetMap forum](https://community.openstreetmap.org).


