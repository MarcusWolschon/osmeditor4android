# Wprowadzenie do Vespucci

Vespucci is a full featured OpenStreetMap editor that supports most operations that desktop editors provide. It has been tested successfully on Google's Android 2.3 to 10.0 and various AOSP based variants. A word of caution: while mobile device capabilities have caught up with their desktop rivals, particularly older devices have very limited memory available and tend to be rather slow. You should take this in to account when using Vespucci and keep, for example, the areas you are editing to a reasonable size. 

## Pierwsze kroki

On startup Vespucci shows you the "Download other location"/"Load Area" dialog after asking for the required permissions and displaying a welcome message. If you have coordinates displayed and want to download immediately, you can select the appropriate option and set the radius around the location that you want to download. Do not select a large area on slow devices. 

Alternatywą dla powyższego jest wyłączenie panelu przez naciśnięcie "Pokaż mapę", a następnie przesunięcie i ustawienie przybliżenia do miejsca którego dane chcesz pbrać by je edytować. (zobacz: "Edycja z Vesspuci")

## Edycja z Vespucci

W zależności od wielkości ekranu oraz wieku twojego urządzenia opcje edycji mogą być dostępne bezpośrednio jako ikony na górnym pasku, przez rozwijalne menu po prawej stronie górnego paska, przez ikony dolnego paska (jeśli jest wyświetlany) lub przez klawisz menu.

<a id="download"></a>

### Pobieranie danych OSM

Kliknij albo na ikonę transferu ![Transfer](../images/menu_transfer.png) lub wybierz w menu "Transfer". Zostanie wyświetlone 7 opcji:

* **Download current view** - download the area visible on the screen and merge it with existing data *(requires network connectivity)*
* **Clear and download current view** - clear any data in memory and then download the area visible on the screen *(requires network connectivity)*
* **Upload data to OSM server** - upload edits to OpenStreetMap *(requires authentication)* *(requires network connectivity)*
* **Location based auto download** - download an area around the current geographic location automatically *(requires network connectivity or offline data)* *(requires GPS)*
* **Pan and zoom auto download** - download data for the currently displayed map area automatically *(requires network connectivity or offline data)* *(requires GPS)*
* **File...** - saving and loading OSM data to/from on device files.
* **Note/Bugs...** - download (automatically and manually) OSM Notes and "Bugs" from QA tools (currently OSMOSE) *(requires network connectivity)*

Najprostszym sposobem na pobranie danych na Twoje urządzenie jest przybliżenie i przesunięcie mapy do obszaru który chcesz edytować, a następnie wybranie opcji "Pobierz bieżący widok". Możesz przybliżać używając gestów albo poprzez naciśnięcie odpowiednich przycisków na mapie lub też przycisków kontroli głośności w urządzeniu. Vespucci powinien pobrać dane z z obszaru widocznego na ekranie. Nie jest do tego potrzebna autoryzacja ze strony serwera OSM.

### Edytowanie

<a id="lock"></a>

#### Zablokuj, odblokuj, przełączanie trybów

By uniknąć przypadkowych edycji Vespucci uruchamia się w trybie "zablokowanym" który umożliwia tylko wynieranie lokalizacji na mapie. Puknij w ikonę ![Locked](../images/locked.png) by odblokować ekran. 

Długie naciśnięcie na kłódkę pokaże menu które teraz zawiera 4 opcje:

* **Tryb zwykły** - domyślny tryb edycji, można dodawać nowe obiekty, edytować, przesuwać i usuwać istniejące. Wyświetla się prosta biała ikona kłódki.
* **Tryb tagów** - wybranie istniejącego obiektu uruchomi Edytor właściwości, długie naciśnięcie na ekranie głównym dodaje obiekty, ale nie działają żadne inne operacje na geometrii. Wyświetla się biała ikona kłódki z literą "T".
* **Tryb wnętrz** - aktywuje tryb wnętrz, zobacz [Indoor mode](#indoor). Wyświetla się biała ikona kłódki z literą "I".
* **Tryb błędów** - aktywuje tryb błędów, tylko obiekty z ustawioną flagą ostrzeżenia będą wyświetlane, zobacz [C-Mode](#c-mode). Wyświetla się biała ikona kłódki z literą "C".

#### Pojedyncze dotknięcie, podwójne dotknięcie i długie dotknięcie

Standardowo, możliwe do zaznaczenia węzły oraz linie mają pomarańczową obwódkę wokół nich pokazującą gdzie - mniej więcej - należny nacisnąć by wybrać ten obiekt. Masz trzy opcje:

* Single tap: Selects object. 
    * An isolated node/way is highlighted immediately. 
    * However, if you try to select an object and Vespucci determines that the selection could mean multiple objects it will present a selection menu, enabling you to choose the object you wish to select. 
    * Selected objects are highlighted in yellow. 
    * For further information see [Node selected](Node%20selected.md), [Way selected](Way%20selected.md) and [Relation selected](Relation%20selected.md).
* Double tap: Start [Multiselect mode](Multiselect.md)
* Long press: Creates a "crosshair", enabling you to add nodes, see below and [Creating new objects](Creating%20new%20objects.md). This is only enabled if "Simple mode" is deactivated.

Dobrą praktyką jest przybliżanie widoku gdy edytujesz obszar o dużej ilości elementów.

System cofania i ponawiania zmian w Vespucci jest dobrze dopracowany, więc nie bój się eksperymentować, jednakże nie wysyłaj testowych danych na serwer.

#### Zaznaczanie/ Odznaczanie (pojedyncze dotknięcie i "menu zaznaczenia")

Dotknij obiektu by zaznaczyć i podświetlić go. Dotknięcie ekranu w miejscu w którym nie znajduje się żaden obiekt spowoduje odznaczenie obiektów. Jeśli zaznaczyłeś już obiekt i potrzebujesz zaznaczyć inny, wystarczy że dotkniesz ten następny, nie jest potrzebne wcześniejsze odznaczanie. Szybkie podwójne dotknięcie na obiekt rozpocznie  [Multiselect mode](Multiselect.md).

Zauważ, że jeżeli spróbujesz wybrać obiekt, a Vespucci stwierdzi, że wybór może dotyczyć wielu obiektów (takie jak węzeł na linii lub inne nakładające się obiekty), pokaże się menu wyboru: możesz wtedy dotknąć odpowiedniego obiektu, który zostanie zaznaczony. 

Wybrane obiekty są oznaczane przez cienką, żółtą obwódkę. Ta ramka może być trudna do zauważenia przy niektórych tłach mapy i poziomach przybliżenia. Kiedy dokonasz zaznaczenia, zobaczysz powiadomienia potwierdzające to działanie.

Kiedy skończysz zaznaczanie zobaczysz (jako menu lub przyciski) listę dostępnych operacji dla wybranych obiektów: aby dowiedzieć się więcej zobacz [Node selected](Node%20selected.md), [Way selected](Way%20selected.md) i [Relation selected](Relation%20selected.md).

#### Zaznaczone obiekty: Edytowanie tagów

Drugie dotknięcie zaznaczonego obiektu otwiera edytor tagów, aby móc edytować te powiązane z danym obiektem.

Zauważ, że dla nakładających się obiektów (takich, jak węzeł na linii) menu wyboru pojawia się ponownie. Wybranie tego samego obiektu pokazuje edytor tagów; wybranie innego po prostu zaznacza go.

#### Zaznaczone obiekty: Przenoszenie Węzłów lub Linii

Kiedy zaznaczysz obiekt, może on zostać przesunięty. Zauważ, że obiekty mogą być przenoszone tylko gdy zostały uprzednio zaznaczone. Zwyczajnie przeciągnij obok (np. w obszarze tolerancji) zaznaczonego obiektu, aby go przesunąć. Jeśli opcja "Duży obszar przeciągania węzłów" jest włączona, wyświetlany jest duży obszar wokół zaznaczonego węzła pozwalając na bardziej precyzyjne przesuwanie. 

#### Dodawanie nowego węzła/punktu lub drogi 

"Tryb uproszczony" jest domyślny. Można to zmienić w menu głównym klikając odpowiednią opcję.

##### Tryb prosty

Tapping the large green floating button on the map screen will show a menu. After you've selected one of the items, you will be asked to tap the screen at the location where you want to create the object, pan and zoom continues to work if you need to adjust the map view. 

See [Creating new objects in simple actions mode](Creating%20new%20objects%20in%20simple%20actions%20mode.md) for more information.

##### Advanced (long press) mode
 
Long press where you want the node to be or the way to start. You will see a black "crosshair" symbol. 
* If you want to create a new node (not connected to an object), click away from existing objects.
* If you want to extend a way, click within the "tolerance zone" of the way (or a node on the way). The tolerance zone is indicated by the areas around a node or way.

Kiedy zobaczysz symbol celownika, masz następujące opcje:

* Dotknij w tym samym miejscu.
    * Jeżeli celownik nie jest w pobliżu węzła, ponowne dotknięcie tego samego miejsca utworzy węzeł. Jeśli jesteś w pobliżu linii (ale nie węzła), nowy węzeł będzie na linii (i będzie połączony z nią).
    * Jeżeli celownik jest w pobliżu węzła (np. w jego granicy tolerancji), dotknięcie tego samego miejsca zaznaczy węzeł (i otworzy edytor tagów). Nie powstanie żaden nowy węzeł. Działanie jest takie samo, jak dla opisanego wyżej zaznaczania.
* Dotknij w innym miejscu. Dotknięcie innego miejsca (poza granicą tolerancji celownika) dodaje segment linii pomiędzy miejscami pierwszego i drugiego dotknięcia. Jeżeli celownik jest w pobliżu linii lub węzła, to nowy segment będzie do nich dołączony.

Dotykaj kolejne miejsca na ekranie by dodać dalsze węzły tworzące linie. Aby zakończyć kliknij ostatni węzeł dwa razy. Jeśli końcowy węzeł znajduje się na linii lub węźle, segment zostanie połączony z nimi automatycznie. 

You can also use a menu item: See [Creating new objects](Creating%20new%20objects.md) for more information.

#### Dodawanie obszaru

W przeciwieństwie do innych systemów danych geograficznych, OpenStreetMap aktualnie nie posiada obiektu typu "obszar". Edytor "iD" próbuje tworzyć obszary z podstawowych elementów OSM, co czasami działa lepiej, czasami gorzej. Aktualnie Vespucci nie próbuje robić nic podobnego, więc musisz wiedzieć trochę więcej o tym, jak prezentowane są obszary:

* _zamknięte linie_: najprostszy i najpopularniejszy wariant obszaru, to linie, które współdzielą pierwszy i ostatni węzeł tworząc zamknięty "pierścień" (na przykład większość budynków jest tego typu). Bardzo łatwo go utworzyć w Vespucci, po prostu dołącz ostatnią linię do pierwszego węzła. Uwaga: interpretacja zamkniętej linii zależy od jej tagów: na przykład, jeśli zamknięta linia jest otagowana jako budynek, to będzie traktowana jako obszar, a jeżeli jako rondo, to nadal będzie tylko linia. W niektórych przypadkach, kiedy obydwie możliwości mogą być poprawne tag "area" wyjaśnia zamierzone użycie.
* _wielokąty złożone ("multi-polygons")_: niektóre obszary mają wiele części, dziur i pierścieni, których nie da się odwzorować jedną linią. OSM używa specjalnego typu relacji (nasz podstawowy obiekt pozwalający określać relacje pomiędzy elementami), aby to obejść, wielokąta. Wielokąt może mieć wiele "zewnętrznych" pierścieni i wiele "wewnętrznych" pierścieni. Każdy pierścień może składać się z zamkniętej linii, jaką opisano powyżej, lub wielu pojedynczych linii o wspólnych węzłach. Podczas kiedy praca z dużymi wielokątami jest problematyczna w każdym edytorze, te mniejsze stosunkowo łatwo utworzyć w Vespucci. 
* _linie brzegowe_: dla bardzo dużych obiektów, takich jak kontynenty i wyspy, nawet wielokąty nie są odpowiednie. Dla linii natural=coastline zakładamy nazewnictwo zależne od kierunku: ląd jest po lewej stronie linii, a woda po prawej. Efektem ubocznym tego jest, że nie powinno się odwracać kierunku linii z otagowanej jako linia brzegowa. Więcej informacji można znaleźć w [OSM wiki](http://wiki.openstreetmap.org/wiki/Tag:natural%3Dcoastline).

#### Ulepszanie Geometrii Linii

Jeśli odpowiednio oddalisz mapę, na zaznaczonej drodze zauważysz mały "x" na środku odcinków linii które są odpowiednio długie. Przeciągnięcie "x" utworzy nowy węzeł linii w tym miejscu. Uwaga: aby uniknąć przypadkowego dodawania węzłów, tolerancja nacisku dla tej czynności jest dość mała.

#### Wytnij, Kopiuj & Wklej

Możesz skopiować lub wyciąć zaznaczone węzły i linie, by później wkleić je raz lub wiele razy do nowych lokalizacji. Wycinanie zachowuje osm id oraz wersję obiektu. By wkleić długo naciśnij docelowe miejsce (zobaczysz celownik wskazujący dokładnie gdzie obiekt się pojawi), a następnie wybierz "Wklej" z menu.

#### Efektywne Dodawanie Adresów

Vespucci has an ![Address](../images/address.png) "add address tags" function that tries to make surveying addresses more efficient by predicting the current house number. It can be selected:

* after a long press (_non-simple mode only:): Vespucci will add a node at the location and make a best guess at the house number and add address tags that you have been lately been using. If the node is on a building outline it will automatically add a "entrance=yes" tag to the node. The tag editor will open for the object in question and let you make any necessary further changes.
* in the node/way selected modes: Vespucci will add address tags as above and start the tag editor.
* in the property editor.

Przewidywanie numerów adresowych zazwyczaj wymaga przynajmniej dwóch numerów po obu stronach drogi by zostać skutecznie użyta, im więcej numerów już zmapowanych tym lepsza dokładność.

Zastanów się nad użyciem trybu [Auto-pobierania](#download) podczas użytkowania tej funkcji.  

#### Dodawanie ograniczeń skrętu

Vespucci ma  możliwość szybiego dodawania zakazów skrętu. Jeśli będzie to potrzebne to drogi zostaną automatycznie podzielone na fragmenty. W takim przypadku konieczne jest ponowne wybranie odpowiednich elementów. 

* zaznacz linię z tagiem highway (droga), będzie to element "from" - ograniczenia skrętu moga być dodane tylko do dróg, jeżeli potrzebujesz je zasosowac do innych linii, mozesz użyć bardziej ogólnej funkcji "Utwórz relację", dodatkowo jeżeli nie ma dostępnych prawidłowych elementów "via" pozycja nie wyświetli się 
* wybierz "Dodaj ograniczenie" z menu
* zaznacz węzeł lub linię "via" (tylko elementy możliwe do wykorzystania jako "via" będą miały pokazane obszary dotyku)
* zaznacz linię "to" (możliwe jest wybranie tego samego elementu "to" jak i "from", Vespucci uzna że chodzi o zakaz zawracania "no_u_turn")
* ustaw typ ograniczenia

### Vespucci w trybie "zablokowanym"

Gdy czerwona kłódka jest widoczna wszystkie nie-edytujące funkcje są dostępne. Dodatkowo długie naciśnięcie na lub obok obiektu pokaże dokładne informacje o nim, o ile jest to obiekt z OSM.

### Zapisywanie Zmian

*(wymagane jest połączenie z Internetem)*

Kliknij ten sam przycisk lub pozycję w menu, który wybrałeś by pobrać dane i wybierz "Wyślij dane na serwer OSM"

Vespucci obsługuje autoryzację OAuth oraz klasyczną metodę podawania loginu i hasła. OAuth jest preferowane gdyż unika wysyłanie niezaszyfrowanego hasła.

Niedawno zainstalowane wersje Vespucci mają domyślanie włączona autoryzację OAuth. Przy pierwszej próbie wysłania zmodyfikowanych danych, ukaże się strona internetowa OSM. Po zalogowaniu (poprzez szyfrowane połączenie) zostaniesz zapytany/zapytana o autoryzację dla Vespucci by móc za jego pomocą edytować dane. Jeśli chcesz lub musisz uwierzytelnić OAuth przed edycją istnieje taka opcja w menu "Narzędzia".

Jeśli chcesz zapisać swoją pracą, ale nie masz połączenia z internetem możesz zapisać ją do pliku .osm kompatybilnego z JOSM. Następnie po uzyskaniu połączenia możesz wysłać dane za pomocą Vespucci lub JOSM. 

#### Rozwiązywanie konfliktujących zmian

Vespucci posiada prostą funkcję rozwiązywania konfliktów edycji. Jednakże jeśli podejrzewasz że istnieją poważne problemy z twoim zestawem zmian, wyeksportuj go do pliku .osc ("Eksport" w menu "Transfer") i spróbuj naprawić je w JOSM. Zobacz dalsze wskazania na [conflict resolution](Conflict%20resolution.md).  

# Użycie GPS

Możesz użyć Vespucci by utworzyć ślad GPX i odczytać go na swoim urządzeniu. Co więcej możesz wyświetlić swoją aktualną pozycję GPS (włączając opcję "Pokaż lokalizację" w menu GPS) i/lub włączyć centrowanie na niej oraz podążanie za pozycją GPS (włączając opcję "Podążaj za pozycją GPS" w menu GPS).  

Jeśli wybrałeś drugą opcję, przesunięcie ekranu ręcznie lub edycja sprawi że opcja "podążaj za GPS" wyłączy się i charakter niebieskiej strzałki GPS zmieni się z obrysu na wypełniony kształt. By szybko wrócić do trybu "podążania", wystarczy że dotkniesz przycisku GPS lub ponownie włączysz ta opcję w menu.

## Notatki i Błędy

Vespucci umożliwia pobieranie, komentowanie i zamykanie Notatek OSM (poprzednio Błędów OSM) oraz "Błędów" wykrywanych przez [OSMOSE quality assurance tool](http://osmose.openstreetmap.fr/en/map/). Obydwa mogą zostać pobrane manualnie lub przez funkcję auto-pobierania. Zmienione i zamknięte wpisy można wysyłać pojedynczo od razu lub wszystkie naraz po pewnym czasie.

Na mapie Notatki i Błędu wyświetlają się jako mała ikonka robaka ![Bug](../images/bug_open.png), zielone oznaczają zamknięte/rozwiązane błędy, niebieskie zostały stworzone lub zmienione przez Ciebie, a żółte oznaczają Notatki/Błędy dalej aktywne nie zmienione przez nikogo. 

Błędy OSMOSE po zaznaczeniu dają możliwość wybrania adresu do obiektu, dotknięcie adresu wybierze obiekt, wyśrodkuje ekran na nim i pobierze obszar potrzebny do jego edycji jeśli zachodzi taka potrzeba. 

Filtrowanie

Poza globalnym aktywowaniem wyświetlania notatek i błędów, możesz ustawić filtr, aby ograniczyć bałagan. W [Advanced preferences](Advanced%20preferences.md) możesz wybierać niezależnie:

* Notatka
* Osmose błąd
* Osmose ostrzeżenie
* Osmose pomniejszy błąd
* Dostosowane

<a id="indoor"></a>

## Tryb wnętrz

Mapowanie wnętrz jest wyzwaniem ze względu na dużą ilość obiektów, które bardzo często się nakładają na siebie. Vespucci ma dedykowany tryb wnętrz, który pozwala na odfiltrowanie wszystkich obiektów, które nie są na tym samym poziomie oraz automatyczne dodawanie aktualnego poziomu do nowo tworzonych obiektów.

Ten tryb może być włączony przez długie naciśnięcie na ikonie kłódki, zobacz [Lock, unlock, mode switching](#lock) i wybranie odpowiedniego wpisu z menu.

1

## Tryb błędów

W trybie błędów wyświetlane są tylko obiekty, które mają ustawioną flagę ostrzeżenia, co ułatwia znalezienie obiektów z konkretnymi problemami lub pasują do sprawdzeń. Jeżeli jest wybrany obiekt i uruchomiony Edytor właściwości w trybie błędów, najlepiej pasujący szablon zostanie automatycznie zastosowany.

Ten tryb może być włączony przez długie naciśnięcie na ikonie kłódki, zobacz [Lock, unlock, mode switching](#lock) i wybranie odpowiedniego wpisu z menu.

### Konfigurowanie sprawdzeń

Currently there are two configurable checks (there is a check for FIXME tags and a test for missing type tags on relations that are currently not configurable) both can be configured by selecting "Validator settings" in the "Preferences". 

Lista wpisów jest podzielona na dwie części, górna pokazuje wpisy do "ponownego przeglądu", dolna "sprawdź wpisy". Wpisy mogą być edytowane przez kliknięcie na nich, zielony przycisk menu pozwala dodawać wpisy.

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
* **Require optional** - Require the key even if the key is in the optional tags of the matching preset .

This check works by first determining the matching preset and then checking if **Key** is a "recommended" key for this object according to the preset, **Require optional** will expand the check to tags that are "optional* on the object. Note: currently linked presets are not checked.

## Filtry

### Filtr bazujący na tagach

Filtr może być aktywowany w głównym menu, a następnie zmieniany przez dotknięcie ikony filtra. Cała dokumentacja jest dostępna tutaj [Tag filter](Tag%20filter.md).

### Filtr bazujący na szablonach

Alternatywnie do powyższego, obiekty są filtrowane na bazie indywidualnych szablonów lub ich grup. Dotknięcie ikony filtra wyświetli okno wyboru szablonu podobne do innych używanych w Vespucci. Indywidualne szablony można wybrać zwykłym kliknięciem, natomiast grupy przez długie dotknięcie (normalne kliknięcie otwiera grupę). Cała dokumentacja dostępna jest tutaj [Preset filter](Preset%20filter.md).

## Dostosowywanie Vespucci

Many aspects of the app can be customized, if you are looking for something specific and can't find it, [the Vespucci website](https://vespucci.io/) is searchable and contains additional information over what is available on device.

### Layer settings

Layer settings can be changed via the layer control (upper right corner), all other setting are reachable via the main menu preferences button.

* Background layer - there is a wide range of aerial and satellite background imagery available, , the default value for this is the "standard style" map from openstreetmap.org.
* Overlay layer - these are semi-transparent layers with additional information, for example GPX tracks. Adding an overlay may cause issues with older devices and such with limited memory. Default: none.
* Notes/Bugs display. Open Notes and bugs will be displayed as a yellow bug icon, closed ones the same in green. Default: on.
* Photo layer. Displays geo-referenced photographs as red camera icons, if direction information is available the icon will be rotated. Default: off.

#### Preferences

* Keep screen on. Default: off.
* Large node drag area. Moving nodes on a device with touch input is problematic since your fingers will obscure the current position on the display. Turning this on will provide a large area which can be used for off-center dragging (selection and other operations still use the normal touch tolerance area). Default: off.

The full description can be found here [Preferences](Preferences.md)

#### Ustawienia zaawansowane

* Node icons. Default: on.
* Always show context menu. When turned on every selection process will show the context menu, turned off the menu is displayed only when no unambiguous selection can be determined. Default: off (used to be on).
* Enable light theme. On modern devices this is turned on by default. While you can enable it for older Android versions the style is likely to be inconsistent. 

The full description can be found here [Advanced preferences](Advanced%20preferences.md)

## Zgłaszanie Problemów

If Vespucci crashes, or it detects an inconsistent state, you will be asked to send in the crash dump. Please do so if that happens, but please only once per specific situation. If you want to give further input or open an issue for a feature request or similar, please do so here: [Vespucci issue tracker](https://github.com/MarcusWolschon/osmeditor4android/issues). The "Provide feedback" function from the main menu will open a new issue and include the relevant app and device information without extra typing.

Jeśli chcesz przedyskutować coś związanego z Vespucci, możesz zrobić to na [Vespucci Google group](https://groups.google.com/forum/#!forum/osmeditor4android) lub na [OpenStreetMap Android forum](http://forum.openstreetmap.org/viewforum.php?id=56)


