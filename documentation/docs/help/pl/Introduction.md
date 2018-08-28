# Wprowadzenie do Vespucci

Vespucci jest wszechstronnym edytorem OpenStreetMap, który pozwala na wykonanie większości działań możliwych do wykonania w edytorach na komputerach stacjonarnych. Pozytywnie przeszedł testy na platformie Android firmy Google - wersje od 2.3 do 7.0 - oraz wielu wariantach systemów AOSP. Uwaga dla użytkowników: o ile współczesne urządzenia mobilne dorównały możliwościami komputerom stacjonarnym, to szczególnie starsze urządzenia posiadające ograniczoną ilość pamięci na ogól są wolniejsze. Należy brać to pod uwagę podczas korzystania z Vespucci i, dla przykładu, utrzymywać wielkości obszarów edytowanych w rozsądnych ramach. 

## Pierwsze kroki

 Po włączeniu Vespucci pokazuje panel "Pobierz inny obszar"/"Wczytaj obszar". Jeśli widzisz współrzędne na ekranie i chcesz pobrać dane od razu, możesz wybrać odpowiednią opcję i ustawić promień wokół miejsca z którego chcesz pobierać dane. Nie wybieraj zbyt dużych obszarów używając słabszych urządzeń. 

Alternatywą dla powyższego jest wyłączenie panelu przez naciśnięcie "Pokaż mapę", a następnie przesunięcie i ustawienie przybliżenia do miejsca którego dane chcesz pbrać by je edytować. (zobacz: "Edycja z Vesspuci")

## Edycja z Vespucci

W zależności od wielkości ekranu oraz wieku twojego urządzenia opcje edycji mogą być dostępne bezpośrednio jako ikony na górnym pasku, przez rozwijalne menu po prawej stronie górnego paska, przez ikony dolnego paska (jeśli jest wyświetlany) lub przez klawisz menu.

<a id="download"></a>

### Pobieranie danych OSM

Kliknij albo na ikonę transferu ![Transfer](../images/menu_transfer.png) lub wybierz w menu "Transfer". Zostanie wyświetlone 7 opcji:

* **Pobierz bieżący widok** - pobiera dane obszaru widocznego na ekranie zastępując wcześniej pobrane dane *(wymagane połączenie z internetem)*
* **Dodaj bieżący widok do pobrania** - pobiera dane obszaru widocznego na ekranie i łączy go z wcześniej pobranymi danymi *(wymagane połączenie z internetem)*
* **Pobierz inny obszar** - pokazuje panel który pozwala na wprowadzenie współrzędnych, wyszukiwanie miejsc lub użycie bieżących współrzędnych; by pobrać dane ze wskazanej okolicy *(wymagane połączenie z internetem)*
* **Wyślij dane na serwer OSM** - wysyła i zapisuje zmienione przez Ciebie dane na OpenStreetMap *(wymagane logowanie)* *(wymagane połączenie z internetem)*
* **Auto-pobieranie** - pobiera dane wokół aktualnej lokalizacji geograficznej automatycznie *(wymagane połączenie z internetem)* *(wymagany sygnał GPS)*
* **Plik...** - zapisywanie i wczytywanie danych OSM z/do pliku na urządzeniu
* **Notatki/Błędy...** - pobieranie (automatyczne lub manualne) notatek z OSM lub "Błędów" z narzędzi weryfikacji jakości danych (aktualnie OSMOSE) *(wymagane połączenie z internetem)*

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

* Pojedyncze dotknięcie: Wybierz obiekt. 
    * Pojedynczy węzeł/linia zostaje od razu podświetlony. 
    * Jeżeli jednak spróbujesz wybrać obiekt, a Vespucci stwierdzi, że wybór może dotyczyć wielu obiektów, pokaże się menu wyboru, pozwalając sprecyzować, o który obiekt Ci chodzi. 
    * Wybrane obiekty są podświetlone na żółto. 
    * Aby dowiedzieć się więcej zobacz [Node selected](Node%20selected.md), [Way selected](Way%20selected.md) and [Relation selected](Relation%20selected.md).
* Podwójne dotknięcie: Rozpocznij [Multiselect mode](Multiselect.md)
* Długie dotknięcie: Pojawia się "celownik", pozwalający na dodawanie węzłów, spójrz niżej oraz [Creating new objects](Creating%20new%20objects.md)

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

#### Dodawanie nowych Węzłów/Punktów lub Linii (długie przyciśnięcie)

Przyciśnij długo w miejscu, gdzie chcesz dodać węzeł lub zacząć linię. Zobaczysz symbol czarnego "celownika". 
* Jeśli chcesz utworzyć węzeł (bez połączenia z obiektem), kliknij z dala od istniejących obiektów.
* Jeżeli chcesz przedłużyć linię, kliknij w "granicach tolerancji" linii (lub jej węzła). Granica tolerancji jest wskazywana przez obszar wokół węzła lub linii.

Kiedy zobaczysz symbol celownika, masz następujące opcje:

* Dotknij w tym samym miejscu.
    * Jeżeli celownik nie jest w pobliżu węzła, ponowne dotknięcie tego samego miejsca utworzy węzeł. Jeśli jesteś w pobliżu linii (ale nie węzła), nowy węzeł będzie na linii (i będzie połączony z nią).
    * Jeżeli celownik jest w pobliżu węzła (np. w jego granicy tolerancji), dotknięcie tego samego miejsca zaznaczy węzeł (i otworzy edytor tagów). Nie powstanie żaden nowy węzeł. Działanie jest takie samo, jak dla opisanego wyżej zaznaczania.
* Dotknij w innym miejscu. Dotknięcie innego miejsca (poza granicą tolerancji celownika) dodaje segment linii pomiędzy miejscami pierwszego i drugiego dotknięcia. Jeżeli celownik jest w pobliżu linii lub węzła, to nowy segment będzie do nich dołączony.

Dotykaj kolejne miejsca na ekranie by dodać dalsze węzły tworzące linie. Aby zakończyć kliknij ostatni węzeł dwa razy. Jeśli końcowy węzeł znajduje się na linii lub węźle, segment zostanie połączony z nimi automatycznie. 

Możesz też użyć menu: Zobacz [Creating new objects](/Creating%20new%20objects.md), aby uzyskać więcej informacji.

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

Vespucci posiada funkcję "Dodaj tagi adresowe" która ma na celu ułatwienie kartowania adresów. Może zostać wybrana:

* po długim nacisku: Vespucci doda węzeł tym miejscu i postara się zgadnąć który jest to numer domu i  doda odpowiednie tagi które ostatnio używano. Jeśli węzeł jest na brzegu budynku doda także tag "entrance=yes" w tym węźle. Edytor tagów otworzy się dla tego obiektu aby pozwolić Ci wprowadzić konieczne dalsze zmiany.
* w czasie gdy zaznaczone są węzły/linie: Vespucci doda tagi tak jak w powyższym przypadku i włączy edytor tagów.
* w edytorze tagów.


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

Aktualnie są dwa konfigurowalne sprawdzenia (sprawdzenie dla tagów FIXME i test na brakujące tagi typów w relacjach, które nie są aktualnie konfigurowalne), które mogą być ustawione poprzez wybranie "Ustawień wykrywania błędów" w "Ustawieniach". 

Lista wpisów jest podzielona na dwie części, górna pokazuje wpisy do "ponownego przeglądu", dolna "sprawdź wpisy". Wpisy mogą być edytowane przez kliknięcie na nich, zielony przycisk menu pozwala dodawać wpisy.

#### Wpisy do ponownego przeglądu

Wpisy do ponownego przeglądu mają następujące właściwości:

* **Klucz** - Klucz tagu.
* **Wartość** - Wartość, którą powinien mieć tag, jeżeli pozostanie pusta, to będzie ignorowana.
* **Wiek** - ile dni po ostatnim sprawdzeniu elementu powinien on być ponownie przejrzany, jeżeli jest obecne pole check_date, to zostanie ono użyte, w przeciwnym wypadku będzie to data utworzenia bieżącej wersji. Ustawienie wartości na zero spowoduje, że sprawdzenie po prostu dopasuje klucz i wartość.
* **Wyrażenie regularne** - jeżeli jest zaznaczone, to przyjmuje się, że **Wartość** jest wyrażeniem regularnym JAVA.

**Klucz** i **Wartość** są sprawdzane pod kątem _istniejących_ tagów obiektów w zapytaniu.

#### Sprawdź wpisy

Sprawdzenie wpisów ma następujące dwie właściwości:

* **Klucz** - Klucz, który powinien być obecny dla obiektu zgodnie z szablonem dopasowania.
* **Sprawdź opcjonalne** - Sprawdź opcjonalne tagi szablonu dopasowania.

To sprawdzenie najpierw określa pasujący szablon a następnie sprawdza, czy **Klucz** jest "rekomendowanym" kluczem dla tego obiektu zgodnie z szablonem. **Sprawdź opcjonalne** rozwinie sprawdzenie dla tagów, które są "opcjonalne" dla obiektu. Uwaga: aktualnie połączone szablony nie są sprawdzane.

## Filtry

### Filtr bazujący na tagach

Filtr może być aktywowany w głównym menu, a następnie zmieniany przez dotknięcie ikony filtra. Cała dokumentacja jest dostępna tutaj [Tag filter](Tag%20filter.md).

### Filtr bazujący na szablonach

Alternatywnie do powyższego, obiekty są filtrowane na bazie indywidualnych szablonów lub ich grup. Dotknięcie ikony filtra wyświetli okno wyboru szablonu podobne do innych używanych w Vespucci. Indywidualne szablony można wybrać zwykłym kliknięciem, natomiast grupy przez długie dotknięcie (normalne kliknięcie otwiera grupę). Cała dokumentacja dostępna jest tutaj [Preset filter](Preset%20filter.md).

## Dostosowywanie Vespucci

### Opcje które mógłbyś/mogłabyś chcieć zmienić

* Mapa w tle
* Nakładka mapy. Dodanie nakładki może powodować problemy na starszych urządzeniach i tych z ograniczoną pamięcią. Domyślnie: brak.
* Wyświetlanie Notatek/Błędów. Aktywne Notatki i Błędy będą wyświetlane jako żółta ikona robaczka, rozwiązane jako zielone. Domyślnie: włączone.
* Warstwa foto. Wyświetla skalibrowane fotografie jako czerwone ikony aparatu, jeśli informacja o kierunku jest dostępna ikona zostanie obrócona zgodnie z nią. Domyślnie: wyłączone.
* Pozostaw ekran włączony. Domyślnie: wyłączone.
* Duży obszar przeciągania węzłów. Przesuwanie węzłów na urządzeniach dotykowych może być problematyczne gdyż twoje palce mogą przysłaniać aktualną pozycję węzła. Włączenie tej opcji zwiększa obszar który może być użyty do przesuwania węzłów przez co można przesuwać węzły nie koniecznie dotykając dokładnie tam gdzie się znajdują (zaznaczanie i inne operacje dalej zachowują normalny obszar responsywności). Domyślnie: wyłączone.

#### Ustawienia zaawansowane

* Ikony węzłów. Domyślnie: włączone.
* Zawsze pokazuj menu kontekstowe. Włączone zawsze pokazuje menu kontekstowe gdy następuje wybór węzła/linii, wyłączone pokazuje menu kontekstowe tylko gdy zaznaczenie jest niejednoznaczne. Domyślnie: wyłączone (dawniej włączone).
* Włącz jasny styl. Na nowych urządzeniach domyślnie włączone. Na starszych urządzeniach może być wadliwe.
* Pokaż statystyki. Pokazuje statystyki służące do odnajdywania błędów oprogramowania, nie są zbyt przydatne. Domyślnie: wyłączone (dawniej włączone).  

## Zgłaszanie Problemów

Jeśli nastąpi awaria Vespucci, lub plik stanu będzie wadliwy, zostaniesz zapytany czy chcesz wysłać raport o błędach. Apelujemy byś to zrobił/zrobiła, ale tylko raz na ten sam rodzaj błędu. Jeśli chcesz dodać więcej informacji lub złożyć prośbę o nową funkcjonalność lub w podobnej sprawie, zrób to tutaj: [Vespucci issue tracker](https://github.com/MarcusWolschon/osmeditor4android/issues). Jeżeli chcesz przedyskutować sprawę związaną z Vespucci, możesz to zrobić albo na [Vespucci google group](https://groups.google.com/forum/#!forum/osmeditor4android) albo na [OpenStreetMap Android forum](http://forum.openstreetmap.org/viewforum.php?id=56)


