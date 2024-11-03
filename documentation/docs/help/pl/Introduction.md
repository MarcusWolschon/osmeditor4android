_Before we start: most screens have links in the menu to the on-device help system giving you direct access to information relevant for the current context, you can easily navigate back to this text too. If you have a larger device, for example a tablet, you can open the help system in a separate split window.  All the help texts and more (FAQs, tutorials) can be found on the [Vespucci documentation site](https://vespucci.io/) too. You can further start the help viewer directly on devices that support short cuts with a long press on the app icon and selecting "Help"_

# Wprowadzenie do Vespucci

Vespucci is a full featured OpenStreetMap editor that supports most operations that desktop editors provide. It has been tested successfully on Google's Android 2.3 to 14.0 (versions prior to 4.1 are no longer supported) and various AOSP based variants. A word of caution: while mobile device capabilities have caught up with their desktop rivals, particularly older devices have very limited memory available and tend to be rather slow. You should take this in to account when using Vespucci and keep, for example, the areas you are editing to a reasonable size.

## Edycja z Vespucci

W zależności od wielkości ekranu oraz wieku twojego urządzenia opcje edycji mogą być dostępne bezpośrednio jako ikony na górnym pasku, przez rozwijalne menu po prawej stronie górnego paska, przez ikony dolnego paska (jeśli jest wyświetlany) lub przez klawisz menu.

<a id="download"></a>

### Pobieranie danych OSM

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

Najprostszym sposobem na pobranie danych na twoje urządzenie jest przybliżenie i przesunięcie mapy do obszaru który chcesz edytować, a następnie wybranie opcji "Pobierz bieżący widok". Możesz przybliżać używając gestów albo poprzez naciśnięcie odpowiednich przycisków na mapie lub też przycisków kontroli głośności w urządzeniu. Vespucci powinien pobrać dane z obszaru widocznego na ekranie. Nie jest do tego potrzebna autoryzacja ze strony serwera OSM.

In unlocked state any non-downloaded areas will be dimmed relative to the downloaded ones if you are zoomed in far enough to enable editing. This is to avoid inadvertently adding duplicate objects in areas that are not being displayed. In the locked state dimming is disabled, this behaviour can be changed in the [Advanced preferences](Advanced%20preferences.md) so that dimming is always active.

If you need to use a non-standard OSM API entry, or use [offline data](https://vespucci.io/tutorials/offline/) in _MapSplit_ format you can add or change entries via the _Configure..._ entry for the data layer in the layer control.

### Edytowanie

<a id="lock"></a>

#### Zablokuj, odblokuj, przełączanie trybów

By uniknąć przypadkowych edycji, Vespucci uruchamia się w trybie "zablokowanym", który umożliwia tylko wybieranie lokalizacji na mapie. Puknij w ikonę ![Locked](../images/locked.png), by odblokować ekran. 

A long press on the lock icon or the _Modes_ menu in the map display overflow menu will display a menu offering 4 options:

* **Normal** - the default editing mode, new objects can be added, existing ones edited, moved and removed. Simple white lock icon displayed.
* **Tag only** - selecting an existing object will start the Property Editor, new objects can be added via the green "+" button, or long press, but no other geometry operations are enabled. White lock icon with a "T" is displayed.
* **Address** - enables Address mode, a slightly simplified mode with specific actions available from the [Simple mode](../en/Simple%20actions.md) "+" button. White lock icon with an "A" is displayed.
* **Indoor** - enables Indoor mode, see [Indoor mode](#indoor). White lock icon with an "I" is displayed.
* **C-Mode** - enables C-Mode, only objects that have a warning flag set will be displayed, see [C-Mode](#c-mode). White lock icon with a "C" is displayed.

If you are using Vespucci on an Android device that supports short cuts (long press on the app icon) you can start directly to _Address_ and _Indoor_ mode.

#### Pojedyncze dotknięcie, podwójne dotknięcie i długie dotknięcie

Standardowo, możliwe do zaznaczenia węzły oraz linie mają pomarańczową obwódkę wokół nich pokazującą, gdzie mniej więcej należy nacisnąć, by wybrać ten obiekt. Są opcje:

* Single tap: Selects object. 
    * An isolated node/way is highlighted immediately. 
    * However, if you try to select an object and Vespucci determines that the selection could mean multiple objects it will present a selection menu, enabling you to choose the object you wish to select. 
    * Selected objects are highlighted in yellow. 
    * For further information see [Node selected](Node%20selected.md), [Way selected](Way%20selected.md) and [Relation selected](Relation%20selected.md).
* Double tap: Start [Multiselect mode](Multiselect.md)
* Long press: Creates a "crosshair", enabling you to add nodes, see below and [Creating new objects](Creating%20new%20objects.md). This is only enabled if "Simple mode" is deactivated.

Dobrą praktyką jest przybliżanie widoku, gdy edytujesz obszar o dużej ilości elementów.

System cofania i ponawiania zmian w Vespucci jest dobrze dopracowany, więc nie bój się eksperymentować, jednakże nie wysyłaj testowych danych na serwer.

#### Zaznaczanie/ Odznaczanie (pojedyncze dotknięcie i "menu zaznaczenia")

Dotknij obiektu, by zaznaczyć i podświetlić go. Dotknięcie ekranu w miejscu, w którym nie znajduje się żaden obiekt, spowoduje odznaczenie obiektów. Jeśli zaznaczyłeś już obiekt i potrzebujesz zaznaczyć inny, wystarczy, że dotkniesz ten następny. Nie jest potrzebne wcześniejsze odznaczanie. Szybkie podwójne dotknięcie na obiekt rozpocznie  [Multiselect mode](Multiselect.md).

Zauważ, że jeżeli spróbujesz wybrać obiekt, a Vespucci stwierdzi, że wybór może dotyczyć wielu obiektów (takie jak węzeł na linii lub inne nakładające się obiekty), pokaże się menu wyboru: możesz wtedy dotknąć odpowiedniego obiektu, który zostanie zaznaczony. 

Wybrane obiekty są oznaczane przez cienką, żółtą obwódkę. Ta ramka może być trudna do zauważenia przy niektórych tłach mapy i poziomach przybliżenia. Kiedy dokonasz zaznaczenia, zobaczysz powiadomienia potwierdzające to działanie.

Kiedy skończysz zaznaczanie, zobaczysz (jako menu lub przyciski) listę dostępnych operacji na wybranych obiektach: aby dowiedzieć się więcej, zobacz [Node selected](Node%20selected.md), [Way selected](Way%20selected.md) i [Relation selected](Relation%20selected.md).

#### Zaznaczone obiekty: Edytowanie tagów

Drugie dotknięcie zaznaczonego obiektu otwiera edytor tagów, aby móc edytować te powiązane z danym obiektem.

Zauważ, że w przypadku nakładających się obiektów (takich jak węzeł na linii) menu wyboru pojawia się ponownie. Wybranie tego samego obiektu pokazuje edytor tagów; wybranie innego po prostu zaznacza go.

#### Zaznaczone obiekty: Przenoszenie Węzłów lub Linii

Once you have selected an object, it can be moved. Note that objects can be dragged/moved only when they are selected. Simply drag near (i.e. within the tolerance zone of) the selected object to move it. If you select the large drag area in the [preferences](Preferences.md), you get a large area around the selected node that makes it easier to position the object. 

#### Dodawanie nowego węzła/punktu lub drogi 

"Tryb uproszczony" jest domyślny. Można to zmienić w menu głównym klikając odpowiednią opcję.

##### Tryb prosty

Tapping the large green floating button on the map screen will show a menu. After you've selected one of the items, you will be asked to tap the screen at the location where you want to create the object, pan and zoom continues to work if you need to adjust the map view. 

See [Creating new objects in simple actions mode](Simple%20actions.md) for more information. Simple mode os the default for new installs.

##### Advanced (long press) mode
 
Long press where you want the node to be or the way to start. You will see a black "crosshair" symbol. 
* If you want to create a new node (not connected to an object), touch away from existing objects.
* If you want to extend a way, touch within the "tolerance zone" of the way (or a node on the way). The tolerance zone is indicated by the areas around a node or way.

Kiedy zobaczysz symbol celownika, masz następujące opcje:

* _Normal press in the same place._
    * If the crosshair is not near a node, touching the same location again creates a new node. If you are near a way (but not near a node), the new node will be on the way (and connected to the way).
    * If the crosshair is near a node (i.e. within the tolerance zone of the node), touching the same location just selects the node (and the tag editor opens. No new node is created. The action is the same as the selection above.
* _Normal touch in another place._ Touching another location (outside of the tolerance zone of the crosshair) adds a way segment from the original position to the current position. If the crosshair was near a way or node, the new segment will be connected to that node or way.

Simply touch the screen where you want to add further nodes of the way. To finish, touch the final node twice. If the final node is located on a way or node, the segment will be connected to the way or node automatically. 

You can also use a menu item: See [Creating new objects](Creating%20new%20objects.md) for more information.

#### Dodawanie obszaru

W przeciwieństwie do innych systemów danych geograficznych, OpenStreetMap aktualnie nie posiada obiektu typu "obszar". Edytor iD próbuje tworzyć obszary z podstawowych elementów OSM, co czasami działa lepiej, czasami gorzej. Aktualnie Vespucci nie próbuje robić nic podobnego, więc musisz wiedzieć trochę więcej o tym, jak prezentowane są obszary:

* _closed ways (*polygons")_: the simplest and most common area variant, are ways that have a shared first and last node forming a closed "ring" (for example most buildings are of this type). These are very easy to create in Vespucci, simply connect back to the first node when you are finished with drawing the area. Note: the interpretation of the closed way depends on its tagging: for example if a closed way is tagged as a building it will be considered an area, if it is tagged as a roundabout it wont. In some situations in which both interpretations may be valid, an "area" tag can clarify the intended use.
* _multi-polygons_: some areas have multiple parts, holes and rings that can't be represented with just one way. OSM uses a specific type of relation (our general purpose object that can model relations between elements) to get around this, a multi-polygon. A multi-polygon can have multiple "outer" rings, and multiple "inner" rings. Each ring can either be a closed way as described above, or multiple individual ways that have common end nodes. While large multi-polygons are difficult to handle with any tool, small ones are not difficult to create in Vespucci. 
* _coastlines_: for very large objects, continents and islands, even the multi-polygon model doesn't work in a satisfactory way. For natural=coastline ways we assume direction dependent semantics: the land is on the left side of the way, the water on the right side. A side effect of this is that, in general, you shouldn't reverse the direction of a way with coastline tagging. More information can be found on the [OSM wiki](http://wiki.openstreetmap.org/wiki/Tag:natural%3Dcoastline).

#### Ulepszanie Geometrii Linii

Jeśli odpowiednio oddalisz mapę, na zaznaczonej drodze zauważysz mały "x" na środku odcinków linii, które są odpowiednio długie. Przeciągnięcie "x" utworzy nowy węzeł linii w tym miejscu. Uwaga: aby uniknąć przypadkowego dodawania węzłów, tolerancja nacisku w przypadku tej czynności jest dość mała.

#### Wytnij, Kopiuj & Wklej

You can copy selected nodes and ways, and then paste once or multiple times to a new location. Cutting will retain the osm id and version, thus can only be pasted once. To paste long press the location you want to paste to (you will see a cross hair marking the location). Then select "Paste" from the menu.

#### Efektywne Dodawanie Adresów

Vespucci supports functionality that makes surveying addresses more efficient by predicting house numbers (left and right sides of streets separately) and automatically adding _addr:street_ or _addr:place_ tags based on the last used value and proximity. In the best case this allows adding an address without any typing at all.   

Adding the tags can be triggered by pressing ![Address](../images/address.png): 

* after a long press (in non-simple mode only): Vespucci will add a node at the location and make a best guess at the house number and add address tags that you have been lately been using. If the node is on a building outline it will automatically add an "entrance=yes" tag to the node. The tag editor will open for the object in question and let you make any necessary further changes.
* in the node/way selected modes: Vespucci will add address tags as above and start the tag editor.
* in the property editor.

To add individual address nodes directly while in the default "Simple mode" switch to "Address" editing mode (long press on the lock button), "Add address node" will then add an address node at the location and if it is on a building outline add a entrance tag to it as described above.

Przewidywanie numerów adresowych zazwyczaj wymaga przynajmniej dwóch numerów po obu stronach drogi, żeby skutecznie działać. Im więcej numerów już zmapowanych, tym lepsza dokładność.

Consider using this with one of the [Auto-download](#download) modes.  

#### Dodawanie ograniczeń skrętu

Vespucci ma możliwość szybiego dodawania zakazów skrętu. Jeśli będzie to potrzebne, to drogi zostaną automatycznie podzielone na fragmenty. W takim przypadku konieczne jest ponowne wybranie odpowiednich elementów. 

* zaznacz linię z tagiem highway (droga), będzie to element "from" – ograniczenia skrętu moga być dodane tylko do dróg, jeżeli potrzebujesz je zastosować do innych linii, możesz użyć bardziej ogólnej funkcji "Utwórz relację", dodatkowo jeżeli nie ma dostępnych prawidłowych elementów "via", pozycja nie wyświetli się 
* wybierz "Dodaj ograniczenie" z menu
* zaznacz węzeł lub linię "via" (tylko elementy możliwe do wykorzystania jako "via" będą miały pokazane obszary dotyku)
* zaznacz linię "to" (możliwe jest wybranie tego samego elementu "to" jak i "from", Vespucci uzna, że chodzi o zakaz zawracania "no_u_turn")
* ustaw typ ograniczenia

### Vespucci w trybie "zablokowanym"

Gdy czerwona kłódka jest widoczna wszystkie nieedytujące funkcje są dostępne. Dodatkowo, długie naciśnięcie na obiekt lub obok niego pokaże dokładne informacje o nim, o ile jest to obiekt z OSM.

### Zapisywanie Zmian

*(wymagane jest połączenie z Internetem)*

Wybierz ten sam przycisk lub pozycję w menu, który wybrałeś, by pobrać dane i wybierz "Wyślij dane na serwer OSM"

Vespucci supports OAuth 2, OAuth 1.0a authorization and the classical username and password method. Since July 1st 2024 the standard OpenStreetMap API only supports OAuth 2 and other methods are only available on private installations of the API or other projects that have repurposed OSM software.  

Authorizing Vespucci to access your account on your behalf requires you to one time login with your display name and password. If your Vespucci install isn't authorized when you attempt to upload modified data you will be asked to login to the OSM website (over an encrypted connection). After you have logged on you will be asked to authorize Vespucci to edit using your account. If you want to or need to authorize the OAuth access to your account before editing there is a corresponding item in the "Tools" menu.

Jeśli chcesz zapisać swoją pracą, ale nie masz połączenia z Internetem, możesz zapisać ją do pliku .osm kompatybilnego z JOSM. Następnie po uzyskaniu połączenia możesz wysłać dane za pomocą Vespucci lub JOSM. 

#### Rozwiązywanie konfliktów przy wysyłaniu

Vespucci posiada prostą funkcję rozwiązywania konfliktów edycji. Jednakże jeśli podejrzewasz, że istnieją poważne problemy z twoim zestawem zmian, wyeksportuj go do pliku .osc ("Eksport" w menu "Transfer") i spróbuj naprawić je w JOSM. Zobacz dalsze wskazania na [conflict resolution](Conflict%20resolution.md).  

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

Vespucci umożliwia pobieranie, komentowanie i zamykanie Uwag OSM (poprzednio Błędów OSM) oraz "Błędów" wykrywanych przez [OSMOSE quality assurance tool](http://osmose.openstreetmap.fr/en/map/). Obydwa mogą zostać pobrane manualnie lub przez funkcję auto-pobierania. Zmienione i zamknięte wpisy można wysyłać pojedynczo od razu lub wszystkie naraz po pewnym czasie. 

Further we support "Todos" that can either be created from OSM elements, from a GeoJSON layer, or externally to Vespucci. These provide a convenient way to keep track of work that you want to complete. 

On the map the Notes and bugs are represented by a small bug icon ![Bug](../images/bug_open.png), green ones are closed/resolved, blue ones have been created or edited by you, and yellow indicates that it is still active and hasn't been changed. Todos use a yellow checkbox icon.

The OSMOSE bug and Todos display will provide a link to the affected element in blue (in the case of Todos only if an OSM element is associated with it), touching the link will select the object, center the screen on it and down load the area beforehand if necessary. 

Filtrowanie

Besides globally enabling the notes and bugs display you can set a coarse grain display filter to reduce clutter. The filter configuration can be accessed from the task layer entry in the [layer control](#layers):

* Notes
* Osmose error
* Osmose warning
* Osmose minor issue
* Maproulette
* Todo

<a id="indoor"></a>

## Tryb wnętrz

Mapowanie wnętrz jest wyzwaniem ze względu na dużą liczbę obiektów, które bardzo często się nakładają na siebie. Vespucci ma dedykowany tryb wnętrz, który pozwala na odfiltrowanie wszystkich obiektów, które nie są na tym samym poziomie oraz automatyczne dodawanie aktualnego poziomu do nowo tworzonych obiektów.

Ten tryb może być włączony przez długie naciśnięcie na ikonie kłódki, zobacz [Lock, unlock, mode switching](#lock) i wybranie odpowiedniego wpisu z menu.

<a id="c-mode"></a>

## Tryb błędów

W trybie błędów wyświetlane są tylko obiekty, które mają ustawioną flagę ostrzeżenia, co ułatwia znalezienie obiektów z konkretnymi problemami lub pasującymi do sprawdzeń. Jeżeli jest wybrany obiekt i uruchomiony Edytor właściwości w trybie błędów, najlepiej pasujący szablon zostanie automatycznie zastosowany.

Ten tryb może być włączony przez długie naciśnięcie na ikonie kłódki, zobacz [Lock, unlock, mode switching](#lock) i wybranie odpowiedniego wpisu z menu.

### Konfigurowanie sprawdzeń

All validations can be disabled/enabled in the "Validator settings/Enabled validations" in the [preferences](Preferences.md). 

The configuration for "Re-survey" entries allows you to set a time after which a tag combination should be re-surveyed. "Check" entries are tags that should be present on objects as determined by matching presets. Entries can be edited by clicking them, the green menu button allows adding of entries.

#### Wpisy do ponownego przeglądu

Wpisy do ponownego przeglądu mają następujące właściwości:

* **Key** - Key of the tag of interest.
* **Value** - Value the tag of interest should have, if empty the tag value will be ignored.
* **Age** - how many days after the element was last changed the element should be re-surveyed, if a _check_date_ tag is present that will be the used, otherwise the date the current version was create. Setting the value to zero will lead to the check simply matching against key and value.
* **Regular expression** - if checked **Value** is assumed to be a JAVA regular expression.

**Klucz** i **Wartość** są sprawdzane pod kątem _istniejących_ tagów obiektów w zapytaniu.

The _Annotations_ group in the standard presets contain an item that will automatically add a _check_date_ tag with the current date.

#### Sprawdź wpisy

Sprawdzenie wpisów ma następujące dwie właściwości:

* **Key** - Key that should be present on the object according to the matching preset.
* **Require optional** - Require the key even if the key is in the optional tags of the matching preset.

This check works by first determining the matching preset and then checking if **Key** is a "recommended" key for this object according to the preset, **Require optional** will expand the check to tags that are "optional* on the object. Note: currently linked presets are not checked.

## Filtry

### Filtr bazujący na tagach

Filtr może być aktywowany w głównym menu, a następnie zmieniany przez dotknięcie ikony filtra. Cała dokumentacja jest dostępna tutaj [Tag filter](Tag%20filter.md).

### Filtr bazujący na szablonach

Alternatywnie do powyższego, obiekty są filtrowane na bazie indywidualnych szablonów lub ich grup. Dotknięcie ikony filtra wyświetli okno wyboru szablonu podobne do innych używanych w Vespucci. Indywidualne szablony można wybrać zwykłym kliknięciem, natomiast grupy przez długie dotknięcie (normalne kliknięcie otwiera grupę). Cała dokumentacja dostępna jest tutaj [Preset filter](Preset%20filter.md).

## Dostosowywanie Vespucci

Many aspects of the app can be customized, if you are looking for something specific and can't find it, [the Vespucci website](https://vespucci.io/) is searchable and contains additional information over what is available on device.

<a id="layers"></a>

### Ustawienia warstw

Layer settings can be changed via the layer control ("hamburger" menu in the upper right corner), all other setting are reachable via the main menu preferences button. Layers can be enabled, disabled and temporarily hidden.

Dostępne typy warstw:

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

#### Ustawienia

* Keep screen on. Default: off.
* Large node drag area. Moving nodes on a device with touch input is problematic since your fingers will obscure the current position on the display. Turning this on will provide a large area which can be used for off-center dragging (selection and other operations still use the normal touch tolerance area). Default: off.

The full description can be found here [Preferences](Preferences.md)

#### Ustawienia zaawansowane

* Full screen mode. On devices without hardware buttons Vespucci can run in full screen mode, that means that "virtual" navigation buttons will be automatically hidden while the map is displayed, providing more space on the screen for the map. Depending on your device this may work well or not,  In _Auto_ mode we try to determine automatically if using full screen mode is sensible or not, setting it to _Force_ or _Never_ skips the automatic check and full screen mode will always be used or always not be used respectively. On devices running Android 11 or higher the _Auto_ mode will never turn full screen mode on as Androids gesture navigation provides a viable alternative to it. Default: _Auto_.  
* Node icons. Default: _on_.
* Always show context menu. When turned on every selection process will show the context menu, turned off the menu is displayed only when no unambiguous selection can be determined. Default: off (used to be on).
* Enable light theme. On modern devices this is turned on by default. While you can enable it for older Android versions the style is likely to be inconsistent. 

The full description can be found here [Advanced preferences](Advanced%20preferences.md)

## Reporting and Resolving Issues

If Vespucci crashes, or it detects an inconsistent state, you will be asked to send in the crash dump. Please do so if that happens, but please only once per specific situation. If you want to give further input or open an issue for a feature request or similar, please do so here: [Vespucci issue tracker](https://github.com/MarcusWolschon/osmeditor4android/issues). The "Provide feedback" function from the main menu will open a new issue and include the relevant app and device information without extra typing.

If you are experiencing difficulties starting the app after a crash, you can try to start it in _Safe_ mode on devices that support short cuts: long press on the app icon and then select _Safe_ from the menu. 

If you want to discuss something related to Vespucci, you can either start a discussion on the [OpenStreetMap forum](https://community.openstreetmap.org).


