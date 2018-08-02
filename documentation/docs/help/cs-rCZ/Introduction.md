# Úvod do Vespucci

Vespucci je plně vybavený editor OpenStreetMap, podporující většinu operací, které poskytují desktopové editory. Aplikace byla otestována pro Android 2.3 až 7.0 a také pro různé varianty založené na AOSP. Jedno upozornění: zatímco schopnosti mobilních zařízení dostihly desktopové stroje, zejména starší zařízení mají velmi omezenou dostupnou paměť a bývají spíše pomalejší. Měli byste to při používání Vespucci vzít v potaz a pamatovat, že například oblasti, které upravujete, by měli být rozumného rozsahu. 

## První použití

Po spuštění Vespucci se zobrazí dialog "Stáhnout jinou oblast"/"Načíst oblast". Pokud se vám zobrazují souřadnice a chcete zahájit stahování okamžitě, můžete zvolit odpovídající možnost a vybrat poloměr oblasti, kterou chcete stáhnout. Na pomalých zařízeních nevolte rozsáhlé oblasti. 

Alternativně lze dialog zrušit stiskem tlačítka "Zobrazit mapu", požadovanou oblast najít na mapě a poté data stáhnout (viz níže "Editování s Vespucci").

## Editování s Vespucci

V závislosti na velikosti obrazovky a stáří vašeho zařízení mohou být možnosti editace dostupné buď přímo skrze ikony na horním panelu, skrze rozbalovací nabídku vpravo na horním panelu, nebo skrze dolní panel (pokud tu je), a nebo skrze klávesu nabídky.

<a id="download"></a>

###Stahování OSM dat

Zvolte buď ikonu ![Přenos](../images/menu_transfer.png) nebo položku "Přenos" z menu. Zobrazí se sedm možností:

* **Stáhnout aktuální pohled** - stáhne oblast viditelnou na obrazovce a nahradí jakákoliv existující data *(vyžaduje připojení k síti)*
* **Přidat aktuální pohled ke stažení** - stáhne oblast viditelnou na obrazovce a sloučí ji s existujícími daty *(vyžaduje připojení k síti)*
* **Stáhnout jinou oblast** - zobrazí formulář, který umožňuje zadat souřadnice, vyhledat místo nebo použít aktuální pozici a poté stáhnout oblast kolem daného místa *(vyžaduje připojení k síti)*
* **Nahrát data na OSM server** - nahraje úpravy na OpenStreetMap *(vyžaduje ověření)* *(vyžaduje připojení k síti)*
* **Stahovat automaticky** - automaticky stahuje oblast okolo aktuální pozice *(vyžaduje připojení k síti)* *(vyžaduje GPS)*
* **Soubor...** - ukládání a načítání OSM dat do/ze souborů v zařízení
* **Poznámky/Chyby...** - stahování (automatické i manuální) OSM Poznámek a "Chyb" z QA nástrojů (aktuálně z OSMOSE) *(vyžaduje připojení k síti)*

Nejjednodušším způsobem stahování dat je najít požadovanou oblast, kterou chcete editovat, na mapě a poté zvolit "Stáhnout aktuální pohled". Přibližovat můžete pomocí gest, tlačítek přiblížení nebo pomocí tlačítek pro ovládání hlasitosti vašeho zařízení. Vespucci by poté měl stáhnout data pro aktuální pohled. Pro stahování dat do vašeho zařízení není zapotřebí žádného ověření.

### Editování

<a id="lock"></a>

#### Uzamčení, odemčení, přepínání režimů

K zabránění nechtěných úprav se Vespucci spustí v "uzamčeném" režimu, který dovoluje pouze přibližování a posouvání mapy. Pro odemčení obrazovky klepněte na ikonu ![zámku](../images/locked.png). 

Dlouhým stisknutím ikony zámku se zobrazí nabídka se 4 možnostmi:

* **Normální** - výchozí režim úprav, lze vytvářet nové objekty, existující upravovat, přesouvat a odstraňovat. Symbolizován jednoduchou bílou ikonou zámku.
* **Editování tagů** - výběr existujících objektů vyvolá Editor vlastností, dlouhý stisk na hlavní obrazovce vytvoří objekt, nicméně měnit pozice objektů není možné. Symbolizován bílou ikonou zámku s "T".
* **Vnitřní prostory** - aktivuje režim pro Editaci vnitřních prostor, viz [režim Vnitřní prostory](#indoor). Symbolizován bílou ikonou zámku s "I".
* **Režim kontroly** - aktivuje Režim kontroly, zobrazeny jsou pouze objekty splňující nastavená kritéria, viz [Režim kontroly](#c-mode). Symbolizován bílou ikonou zámku s "C".

#### Jedno klepnutí, dvě klepnutí a dlouhé stisknutí

Ve výchozím nastavení mají uzly a cesty, které lze zvolit, oranžově vyznačenou oblast do které musíte přibližně klepnout pro zvolení objektu. Existují tři možnosti:

* Single tap: Selects object. 
    * An isolated node/way is highlighted immediately. 
    * However, if you try to select an object and Vespucci determines that the selection could mean multiple objects it will present a selection menu, enabling you to choose the object you wish to select. 
    * Selected objects are highlighted in yellow. 
    * For further information see [Node selected](Node%20selected.md), [Way selected](Way%20selected.md) and [Relation selected](Relation%20selected.md).
* Double tap: Start [Multiselect mode](Multiselect.md)
* Long press: Creates a "crosshair", enabling you to add nodes, see below and [Creating new objects](Creating%20new%20objects.md)

Vyplatí se přiblížit mapu, pokud plánujete upravovat v oblasti s vysokou hustotou.

Vespucci má kvalitní systém "zpět/vpřed", takže se nemusíte bát experimentovat s vaším zařízením, nicméně prosíme nenahrávejte a neukládejte čistě testovací data.

#### Výběr / Zrušení výběru (jedno klepnutí a "nabídka výběru")

Touch an object to select and highlight it. Touching the screen in an empty region will de-select. If you have selected an object and you need to select something else, simply touch the object in question, there is no need to de-select first. A double tap on an object will start [Multiselect mode](Multiselect.md).

Všimněte si, že pokud se pokusíte vybrat objekt a Vespucci nazná, že je výběr nejednoznačný (např. uzel ležící na cestě nebo jiné překrývající se objekty), zobrazí se nabídka výběru - klepněte na objekt, který chcete vybrat. 

Vybrané objekty jsou odlišeny tenkým žlutým okrajem. Žlutý okraj může být obtížné zpozorovat v závislosti na mapovém podkladu a přiblížení. Po provedení výběru se objeví oznámení potvrzující výběr.

Once the selection has completed you will see (either as buttons or as menu items) a list of supported operations for the selected object: For further information see [Node selected](Node%20selected.md), [Way selected](Way%20selected.md) and [Relation selected](Relation%20selected.md).

#### Zvolené objekty: Úprava tagů

Druhé klepnutí na zvolený objekt otevře editor tagů, ve kterém můžete upravovat tagy přiřazené k objektu.

Všimněte si, že pro překrývající se objekty (např. uzel na cestě) se podruhé zobrazí menu s výběrem. Zvolení stejného objektu zobrazí editor tagů; zvolení jiného objektu jednoduše zvolí daný objekt.

#### Zvolené objekty: Posun Uzlu nebo Cesty

Zvolené objekty můžete posouvat. Všimněte si, že objekty lze přetahovat/posouvat pouze pokud jsou zvolené. Jednoduše uchopte vybraný objekt (v oblasti tolerance) a posuňte jej. V nastavení můžete zvolit Větší oblast pro výběr uzlu, díky které je snazší objekt uchopit a umístit. 

#### Přidání nového Uzlu/Bodu nebo Cesty (dlouhé stisknutí)

Dlouze stiskněte na místo, kde chcete vytvořit uzel nebo počátek cesty. Zobrazí se černý symbol "zaměřovače". 
* Pokud chcete vytvořit nový uzel (nenapojený na objekt), klikněte mimo existující objekty.
* Pokud chcete rozšířit cestu, klikněte do oblasti tolerance výběru cesty (nebo uzlu cesty). Tolerance výběru je vyznačena oranžovou oblastí okolo uzlu nebo cesty.

Jakmile se zobrazí symbol zaměřovače, máte tři možnosti:

* Touch in the same place.
    * If the crosshair is not near a node, touching the same location again creates a new node. If you are near a way (but not near a node), the new node will be on the way (and connected to the way).
    * If the crosshair is near a node (i.e. within the tolerance zone of the node), touching the same location just selects the node (and the tag editor opens. No new node is created. The action is the same as the selection above.
* Touch another place. Touching another location (outside of the tolerance zone of the crosshair) adds a way segment from the original position to the current position. If the crosshair was near a way or node, the new segment will be connected to that node or way.

Jednoduše klepněte na místo, kam chcete přidat další uzel cesty. Pro dokončení cesty klepněte na koncový uzel podruhé. Pokud se koncový uzel nachází na cestě nebo existujícím uzlu, tak se na ni vytvářený segment automaticky napojí. 

You can also use a menu item: See [Creating new objects](/Creating%20new%20objects.md) for more information.

#### Adding an Area

OpenStreetMap currently doesn't have an "area" object type unlike other geo-data systems. The online editor "iD" tries to create an area abstraction from the underlying OSM elements which works well in some circumstances, in others not so. Vespucci currently doesn't try to do anything similar, so you need to know a bit about the way areas are represented:

* _closed ways (*polygons")_: the simplest and most common area variant, are ways that have a shared first and last node forming a closed "ring" (for example most buildings are of this type). These are very easy to create in Vespucci, simply connect back to the first node when you are finished with drawing the area. Note: the interpretation of the closed way depends on its tagging: for example if a closed way is tagged as a building it will be considered an area, if it is tagged as a roundabout it wont. In some situations in which both interpretations may be valid, an "area" tag can clarify the intended use.
* _multi-ploygons_: some areas have multiple parts, holes and rings that can't be represented with just one way. OSM uses a specific type of relation (our general purpose object that can model relations between elements) to get around this, a multi-polygon. A multi-polygon can have multiple "outer" rings, and multiple "inner" rings. Each ring can either be a closed way as described above, or multiple individual ways that have common end nodes. While large multi-polygons are difficult to handle with any tool, small ones are not difficult to create in Vespucci. 
* _coastlines_: for very large objects, continents and islands, even the multi-polygon model doesn't work in a satisfactory way. For natural=coastline ways we assume direction dependent semantics: the land is on the left side of the way, the water on the right side. A side effect of this is that, in general, you shouldn't reverse the direction of a way with coastline tagging. More information can be found on the [OSM wiki](http://wiki.openstreetmap.org/wiki/Tag:natural%3Dcoastline).

#### Improving Way Geometry

If you zoom in far enough on a selected way you will see a small "x" in the middle of the way segments that are long enough. Dragging the "x" will create a node in the way at that location. Note: to avoid accidentally creating nodes, the touch tolerance area for this operation is fairly small.

#### Cut, Copy & Paste

You can copy or cut selected nodes and ways, and then paste once or multiple times to a new location. Cutting will retain the osm id and version. To paste long press the location you want to paste to (you will see a cross hair marking the location). Then select "Paste" from the menu.

#### Efficiently Adding Addresses

Vespucci has an "add address tags" function that tries to make surveying addresses more efficient. It can be selected:

* after a long press: Vespucci will add a node at the location and make a best guess at the house number and add address tags that you have been lately been using. If the node is on a building outline it will automatically add a "entrance=yes" tag to the node. The tag editor will open for the object in question and let you make any necessary further changes.
* in the node/way selected modes: Vespucci will add address tags as above and start the tag editor.
* in the tag editor.

House number prediction typically requires at least two house numbers on each side of the road to be entered to work, the more numbers present in the data the better.

Consider using this with the [Auto-download](#download) mode.  

#### Adding Turn Restrictions

Vespucci has a fast way to add turn restrictions. if necessary it will split ways automatically and ask you to re-select elements. 

* select a way with a highway tag (turn restrictions can only be added to highways, if you need to do this for other ways, please use the generic "create relation" mode)
* select "Add restriction" from the menu
* select the "via" node or way (only possible "via" elements will have the touch area shown)
* select the "to" way (it is possible to double back and set the "to" element to the "from" element, Vespucci will assume that you are adding an no_u_turn restriction)
* set the restriction type

### Vespucci in "locked" mode

When the red lock is displayed all non-editing actions are available. Additionally a long press on or near to an object will display the detail information screen if it is an OSM object.

### Saving Your Changes

*(requires network connectivity)*

Select the same button or menu item you did for the download and now select "Upload data to OSM server".

Vespucci supports OAuth authorization and the classical username and password method. OAuth is preferable since it avoids sending passwords in the clear.

New Vespucci installs will have OAuth enabled by default. On your first attempt to upload modified data, a page from the OSM website loads. After you have logged on (over an encrypted connection) you will be asked to authorize Vespucci to edit using your account. If you want to or need to authorize the OAuth access to your account before editing there is a corresponding item in the "Tools" menu.

If you want to save your work and do not have Internet access, you can save to a JOSM compatible .osm file and either upload later with Vespucci or with JOSM. 

#### Resolving conflicts on uploads

Vespucci has a simple conflict resolver. However if you suspect that there are major issues with your edits, export your changes to a .osc file ("Export" menu item in the "Transfer" menu) and fix and upload them with JOSM. See the detailed help on [conflict resolution](Conflict%20resolution.md).  

## Using GPS

You can use Vespucci to create a GPX track and display it on your device. Further you can display the current GPS position (set "Show location" in the GPS menu) and/or have the screen center around and follow the position (set "Follow GPS Position" in the GPS menu). 

If you have the latter set, moving the screen manually or editing will cause the "follow GPS" mode to be disabled and the blue GPS arrow will change from an outline to a filled arrow. To quickly return to the "follow" mode, simply touch GPS button or re-check the menu option.

## Notes and Bugs

Vespucci supports downloading, commenting and closing of OSM Notes (formerly OSM Bugs) and the equivalent functionality for "Bugs" produced by the [OSMOSE quality assurance tool](http://osmose.openstreetmap.fr/en/map/). Both have to either be down loaded explicitly or you can use the auto download facility to access the items in your immediate area. Once edited or closed, you can either upload the bug or Note immediately or upload all at once.

On the map the Notes and bugs are represented by a small bug icon ![Bug](../images/bug_open.png), green ones are closed/resolved, blue ones have been created or edited by you, and yellow indicates that it is still active and hasn't been changed. 

The OSMOSE bug display will provide a link to the affected object in blue, touching the link will select the object, center the screen on it and down load the area beforehand if necessary. 

### Filtering

Besides globally enabling the notes and bugs display you can set a coarse grain display filter to reduce clutter. In the [Advanced preferences](Advanced%20preferences.md) you can individually select:

* Notes
* Osmose error
* Osmose warning
* Osmose minor issue
* Custom

<a id="indoor"></a>

## Indoor mode

Mapping indoors is challenging due to the high number of objects that very often will overlay each other. Vespucci has a dedicated indoor mode that allows you to filter out all objects that are not on the same level and which will automatically add the current level to new objects created there.

The mode can be enabled by long pressing on the lock item, see [Lock, unlock, mode switching](#lock) and selecting the corresponding menu entry.

<a id="c-mode"></a>

## C-Mode

In C-Mode only objects are displayed that have a warning flag set, this makes it easy to spot objects that have specific problems or match configurable checks. If an object is selected and the Property Editor started in C-Mode the best matching preset will automatically be applied.

The mode can be enabled by long pressing on the lock item, see [Lock, unlock, mode switching](#lock) and selecting the corresponding menu entry.

### Configuring checks

Currently there are two configurable checks (there is a check for FIXME tags and a test for missing type tags on relations that are currently not configurable) both can be configured by selecting "Validator preferences" in the "Preferences". 

The list of entries is split in to two, the top half lists "re-survey" entries, the bottom half "check entries". Entries can be edited by clicking them, the green menu button allows adding of entries.

#### Re-survey entries

Re-survey entries have the following properties:

* **Key** - Key of the tag of interest.
* **Value** - Value the tag of interest should have, if empty the tag value will be ignored.
* **Age** - how many days after the element was last changed the element should be re-surveyed, if a check_date field is present that will be the used, otherwise the date the current version was create. Setting the value to zero will lead to the check simply matching against key and value.
* **Regular expression** - if checked **Value** is assumed to be a JAVA regular expression.

**Key** and **Value** are checked against the _existing_ tags of the object in question.

#### Check entries

Check entries have the following two properties:

* **Key** - Key that should be present on the object according to the matching preset.
* **Check optional** - Check the optional tags of the matching preset.

This check works be first determining the matching preset and then checking if **Key** is a "recommended" key for this object according to the preset, **Check optional** will expand the check to tags that are "optional* on the object. Note: currently linked presets are not checked.

## Filters

### Tag based filter

The filter can be enabled from the main menu, it can then be changed by tapping the filter icon. More documentation can be found here [Tag filter](Tag%20filter.md).

### Preset based filter

An alternative to the above, objects are filtered either on individual presets or on preset groups. Tapping on the filter icon will display a preset selection dialog similar to that used elsewhere in Vespucci. Individual presets can be selected by a normal click, preset groups by a long click (normal click enters the group). More documentation can be found here [Preset filter](Preset%20filter.md).

## Customizing Vespucci

### Settings that you might want to change

* Background layer
* Overlay layer. Adding an overlay may cause issues with older devices and such with limited memory. Default: none.
* Notes/Bugs display. Open Notes and bugs will be displayed as a yellow bug icon, closed ones the same in green. Default: on.
* Photo layer. Displays geo-referenced photographs as red camera icons, if direction information is available the icon will be rotated. Default: off.
* Keep screen on. Default: off.
* Large node drag area. Moving nodes on a device with touch input is problematic since your fingers will obscure the current position on the display. Turning this on will provide a large area which can be used for off-center dragging (selection and other operations still use the normal touch tolerance area). Default: off.

#### Advanced preferences

* Node icons. Default: on.
* Always show context menu. When turned on every selection process will show the context menu, turned off the menu is displayed only when no unambiguous selection can be determined. Default: off (used to be on).
* Enable light theme. On modern devices this is turned on by default. While you can enable it for older Android versions the style is likely to be inconsistent.
* Show statistics. Will show some statistics for debugging, not really useful. Default: off (used to be on).  

## Reporting Problems

If Vespucci crashes, or it detects an inconsistent state, you will be asked to send in the crash dump. Please do so if that happens, but please only once per specific situation. If you want to give further input or open an issue for a feature request or similar, please do so here: [Vespucci issue tracker](https://github.com/MarcusWolschon/osmeditor4android/issues). If you want to discuss something related to Vespucci, you can either start a discussion on the [Vespucci Google group](https://groups.google.com/forum/#!forum/osmeditor4android) or on the [OpenStreetMap Android forum](http://forum.openstreetmap.org/viewforum.php?id=56)


