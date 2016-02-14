# Wprowadzenie do Vespucci

Vespucci jest wszechstronnym edytorem OpenStreetMap, który pozwala na wykonanie większości działań możliwych do wykonania w edytorach na komputerach stacjonarnych. Pozytywnie przeszedł testy na platformie Android firmy Google - wersje od 2.3 do 6.0 - oraz wielu wariantach systemów AOSP. Uwaga dla użytkowników: o ile współczesne urządzenia mobilne dorównały możliwościami komputerom stacjonarnym, to szczególnie starsze urządzenia posiadające ograniczoną ilość pamięci na ogól są wolniejsze. Należy brać to pod uwagę podczas korzystania z Vespucci i, dla przykładu, utrzymywać wielkości obszarów edytowanych w rozsądnych ramach. 

## Pierwsze kroki

 Po włączeniu Vespucci pokazuje panel "Pobierz inny obszar"/"Wczytaj obszar". Jeśli widzisz współrzędne na ekranie i chcesz pobrać dane od razu, możesz wybrać odpowiednią opcję i ustawić promień wokół miejsca z którego chcesz pobierać dane. Nie wybieraj zbyt dużych obszarów używając słabszych urządzeń. 

Alternatywą dla powyższego jest wyłączenie panelu przez naciśnięcie "Pokaż mapę", a następnie przesunięcie i ustawienie przybliżenia do miejsca którego dane chcesz pbrać by je edytować. (zobacz: "Edycja z Vesspuci")

## Edycja z Vespucci

W zależności od wielkości ekranu oraz wieku twojego urządzenia opcje edycji mogą być dostępne bezpośrednio jako ikony na górnym pasku, przez rozwijalne menu po prawej stronie górnego paska, przez ikony dolnego paska (jeśli jest wyświetlany) lub przez klawisz menu.

### Pobieranie danych OSM

Kliknij albo na ikonę transferu ![](../images/menu_transfer.png) lub wybierz w menu "Transfer". Zostanie wyświetlone 7 opcji:

* **Pobierz bieżący widok** - pobiera dane obszaru widocznego na ekranie zastępując wcześniej pobrane dane *(wymagane połączenie z internetem)*
* **Dodaj bieżący widok do pobrania** - pobiera dane obszaru widocznego na ekranie i łączy go z wcześniej pobranymi danymi *(wymagane połączenie z internetem)*
* **Pobierz inny obszar** - pokazuje panel który pozwala na wprowadzenie współrzędnych, wyszukiwanie miejsc lub użycie bieżących współrzędnych; by pobrać dane ze wskazanej okolicy *(wymagane połączenie z internetem)*
* **Wyślij dane na serwer OSM** - wysyła i zapisuje zmienione przez Ciebie dane na OpenStreetMap *(wymagane logowanie)* *(wymagane połączenie z internetem)*
* **Auto-pobieranie** - pobiera dane wokół aktualnej lokalizacji automatycznie *(wymagane połączenie z internetem)* *(wymagany sygnał GPS)*
* **Plik...** - zapisywanie i wczytywanie danych OSM z/do pliku na urządzeniu
* **Notatki/Błędy...** - pobieranie (automatyczne lub manualne) notatek z OSM lub "Błędów" z narzędzi weryfikacji jakości danych (aktualnie OSMOSE) *(wymagane połączenie z internetem)*

Najprostszym sposobem na pobranie danych na Twoje urządzenie jest przybliżenie i przesunięcie mapy do obszaru który chcesz edytować, a następnie wybranie opcji "Pobierz bieżący widok". Możesz przybliżać używając gestów albo poprzez naciśnięcie odpowiednich przycisków na mapie lub też przycisków kontroli głośności na telefonie. Vespucci powinien pobrać dane z z obszaru widocznego na ekranie. Nie jest do tego potrzebna autoryzacja ze strony serwera OSM.

### Edytowanie

Aby uniknąć przypadkowych edycji Vespucci uruchamia się w trybie "zablokowanym", który pozwala tylko na przesuwanie mapy. Naciśnij ikonę ![Locked](../images/locked.png) aby odblokować. Długie naciśnięcie na ikonę kłódki włączy tryb "edytowania tagów" w którym nie będzie możliwe tworzenie nowych obiektów ani zmienianie geometrii obiektów już istniejących, ten tryb jest oznaczony ikoną otwartej białej kłódki z literą T.

Standardowo, możliwe do zaznaczenia węzły oraz linie mają pomarańczową obwódkę wokół nich pokazującą gdzie - mniej więcej - należny nacisnąć by wybrać ten obiekt. Jeśli próbujesz wybrać obiekt, a Vespucci wykryje że miejsce które zostało naciśnięte może odnosić się do wielu obiektów, zostanie pokazana lista z której można będzie wybrać interesujący obiekt. Zaznaczone obiekty wyświetlane są w kolorze żółtym na mapie.

Dobrą praktyką jest przybliżanie widoku gdy edytujesz obszar o dużej ilości elementów.

System cofania i ponawiania zmian w Vespucci jest dobrze dopracowany, więc nie bój się eksperymentować, jednakże nie wysyłaj testowych danych na serwer.

#### Zaznaczanie/ Odznaczanie

Dotknij obiektu by zaznaczyć i podświetlić go, kolejne dotknięcie otworzy edytor tagów tego elementu. Dotknięcie ekranu w miejscu w którym nie znajduje się żaden obiekt spowoduje odznaczenie obiektów. Jeśli zaznaczyłeś już obiekt i potrzebujesz zaznaczyć inny, wystarczy że dotkniesz ten następny, nie jest potrzebne wcześniejsze odznaczanie. Szybkie podwójne dotknięcie na obiekt rozpocznie  [Multiselect mode](../en/Multiselect.md).

#### Dodawanie nowych Węzłów/Punktów lub Linii

Długo naciśnij miejsce w którym chcesz by pojawił się węzeł lub początek linii. Zobaczysz czarny "celownik". Dotknięcie tego samego miejsca stworzy węzeł, zaś dotknięcie miejsca poza obszarem tworzącym węzeł - istnieje pewien stopień tolerancji -stworzy odcinek stanowiący część linii z początkowym miejscem kliknięcia jako pierwszym węzłem tej linii. 

Dotykaj kolejne miejsca na ekranie by dodać dalsze węzły tworzące linie. Aby zakończyć kliknij ostatni węzeł dwa razy. Jeśli początkowy i końcowy węzeł znajdują się na linii zostaną włączone w nią automatycznie.

#### Przenoszenie Węzłów lub Linii

Obiekty mogą być przenoszone tylko gdy zostały uprzednio zaznaczone. Jeśli opcja "Duży obszar przeciągania węzłów" jest włączona, wyświetlany jest duży obszar wokół zaznaczonego węzła pozwalając na bardziej precyzyjne przesuwanie. 

#### Ulepszanie Geometrii Linii

Jeśli odpowiednio oddalisz mapę, zauważysz mały "x" na środku odcinków linii które są odpowiednio długie. Przeciągnięcie "x" utworzy nowy węzeł linii w tym miejscu. Uwaga: aby uniknąć przypadkowego dodawania węzłów, tolerancja nacisku dla tej czynności jest dość mała.

#### Wytnij, Kopiuj & Wklej

Możesz skopiować lub wyciąć zaznaczone węzły i linie, by później wkleić je raz lub wiele razy do nowych lokalizacji. Wycinanie zachowuje osm id oraz wersję obiektu. By wkleić długo naciśnij docelowe miejsce (zobaczysz celownik wskazujący dokładnie gdzie obiekt się pojawi), a następnie wybierz "Wklej" z menu.

#### Efektywne Dodawanie Adresów

Vespucci posiada funkcję "Dodaj tagi adresowe" która ma na celu ułatwienie kartowania adresów. Może zostać wybrana 

* po długim nacisku: Vespucci doda węzeł tym miejscu i postara się zgadnąć który jest to numer domu i  doda odpowiednie tagi które ostatnio używano. Jeśli węzeł jest na brzegu budynku dodatakże tag "entrance=yes" w tym węźle. Edytor tagów otworzy się dla tego obiektu aby pozowlić Ci wprowadzić dalsze zmiany.
* w czasie gdy zaznaczone są węzły/linie: Vespucci doda tagi tak jak w powyższym przypadku i włączy edytor tagów.
* w edytorze tagów.


Przewidywanie numerów adresowych zazwyczaj wymaga przynajmniej dwóch numerów po obu stronach drogi by zostać skutecznie użyta, im więcej numerów już zmapowanych tym lepsza dokładność.

Zastanów się nad użyciem trybu "Auto-pobierania" podczas użytkowania tej funkcji.  

#### Dodawanie ograniczeń skrętu

W Vespucci jest szybki sposób na dodanie ograniczeń skrętu. Uwaga: musisz wcześniej podzielić linię by umożliwić poprawne dodanie ograniczenia

* zaznacz linię z tagiem highway (droga), będzie to element "from" - ograniczenia skrętu moga być dodane tylko do dróg, jeżeli potrzebujesz je zasosowac do innych linii, mozesz użyć bardziej ogólnej funkcji "Utwórz relację", dodatkowo jeżeli nie ma dostępnych prawidłowych elementów "via" pozycja nie wyświetli się 
* wybierz "Dodaj ograniczenie" z menu
* zaznacz węzeł lub linię "via" (wszystkie elementy możliwe do wykorzystania jako "via" zostaną podświetlone)
* zaznacz linię "to" (możliwe jest wybranie tego samego elementu "to" jak i "from", Vespucci uzna że chodzi o zakaz zawracania "no_u_turn")
* ustaw typ ograniczenia w menu tagów

### Vespucci w trybie "zablokowanym"

Gdy czerwona kłódka jest widoczna wszystkie nie-edytujące funkcje są dostępne. Dodatkowo długie naciśnięcie na lub obok obiektu pokaże dokładne informacje o nim, o ile jest to obiekt z OSM.

### Zapisywanie Zmian

*(wymagane jest połączenie z Internetem)*

Kliknij ten sam przycisk lub pozycję w menu, który wybrałeś by pobrać dane i wybierz "Wyślij dane na serwer OSM"

Vespucci obsługuje autoryzację OAuth oraz klasyczną metodę podawania loginu i hasła. OAuth jest preferowane gdyż unika wysyłanie niezaszyfrowanego hasła.

Niedawno zainstalowane wersje Vespucci mają domyślanie włączona autoryzację OAuth. Przy pierwszej próbie wysłania zmodyfikowanych danych, ukaże się strona internetowa OSM. Po zalogowaniu (poprzez szyfrowane połączenie) zostaniesz zapytany/zapytana o autoryzację dla Vespucci by móc za jego pomocą edytować dane. Jeśli chcesz lub musisz uwierzytelnić OAuth przed edycją istnieje taka opcja w menu "Narzędzia".

Jeśli chcesz zapisać swoją pracą, ale nie masz połączenia z internetem możesz zapisać ją do pliku .osm kompatybilnego z JOSM. Następnie po uzyskaniu połączenia możesz wysłać dane za pomocą Vespucci lub JOSM. 

#### Rozwiązywanie konfliktujących zmian

Vespucci posiada prostą funkcję rozwiązywania konfliktów edycji. Jednakże jeśli podejrzewasz że istnieją poważne problemy z twoim zestawem zmian, wyeksportuj go do pliku .osc ("Eksport" w menu "Transfer") i spróbuj naprawić je w JOSM. Zobacz dalsze wskazania na [conflict resolution](../en/Conflict resolution.md).  

# Użycie GPS

Możesz użyć Vespucci by utworzyć ślad GPX i odczytać go na swoim urządzeniu. Co więcej możesz wyświetlić swoją aktualną pozycję GPS (włączając opcję "Pokaż lokalizację" w menu GPS) i/lub włączyć centrowanie na niej oraz podążanie za pozycją GPS (włączając opcję "Podążaj za pozycją GPS" w menu GPS).  

Jeśli wybrałeś drugą opcję, przesunięcie ekranu ręcznie lub edycja sprawi że opcja "podążaj za GPS" wyłączy się i charakter niebieskiej strzałki GPS zmieni się z obrysu na wypełniony kształt. By szybko wrócić do trybu "podążania", wystarczy że dotkniesz ponownie strzałkę lub ponownie włączysz ta opcję w menu.

## Notatki i Błędy

Vespucci umożliwia pobieranie, komentowanie i zamykanie Notatek OSM (poprzednio Błędów OSM) oraz "Błędów" wykrywanych przez [OSMOSE quality assurance tool](http://osmose.openstreetmap.fr/en/map/). Obydwa mogą zostać pobrane manualnie lub przez funkcję auto-pobierania. Zmienione i zamknięte wpisy można wysyłać pojedynczo od razu lub wszystkie naraz po pewnym czasie.

Na mapie Notatki i Błędu wyświetlają się jako mała ikonka robaka ![](../images/bug_open.png), zielone oznaczają zamknięte/rozwiązane błędy, niebieskie zostały stworzone lub zmienione przez Ciebie, a żółte oznaczają Notatki/Błędy dalej aktywne nie zmienione przez nikogo. 

Błędy OSMOSE po zaznaczeniu dają możliwość wybrania adresu do obiektu, dotknięcie adresu wybierze obiekt, wyśrodkuje ekran na nim i pobierze obszar potrzebny do jego edycji jeśli zachodzi taka potrzeba. 

## Dostosowywanie Vespucci

### Opcje które mógłbyś/mogłabyś chcieć zmienić

* Mapa w tle
* Nakładka mapy. Dodanie nakładki może powodować problemy na starszych urządzeniach i tych z ograniczoną pamięcią. Domyślnie: brak.
* Wyświetlanie Notatek/Błędów. Aktywne Notatki i Błędy będą wyświetlane jako żółta ikona robaczka, rozwiązane jako zielone. Domyślnie: włączone.
* Warstwa foto. Wyświetla zgeoreferencjonowane (skalibrowane) fotografie jako czerwone ikony aparatu, jeśli informacja o kierunku jest dostępna ikona zostanie obrócona zgodnie z nią. Domyślnie: wyłączone.
* Ikony węzłów. Domyślnie: włączone.
* Pozostaw ekran włączony. Domyślnie: wyłączone.
* Duży obszar przeciągania węzłów. Przesuwanie węzłów na urządzeniach dotykowych może być problematyczne gdyż twoje palce mogą przysłaniać aktualną pozycję węzła. Włączenie tej opcji zwiększa obszar który może być użyty do przesuwania węzłów przez co można przesuwać węzły nie koniecznie dotykając dokładnie tam gdzie się znajdują (zaznaczanie i inne operacje dalej zachowują normalny obszar responsywności). Domyślnie: wyłączone.

#### Ustawienia zaawansowane

* Włącz rozdzielony pasek nawigacyjny. Na nowych urządzeniach pasek nawigacyjny zostanie rozdzielony na dwie części górną oraz dolną, dolna zawierająca przyciski. To zazwyczaj pozwala na pokazanie większej ilości przycisków, ale zajmuje więcej miejsca na ekranie. Wyłączenie tej opcji przeniesie przyciski do górnego paska. Uwaga: należy ponownie uruchomić Vespucci by nastąpiła zmiana.
* Zawsze pokazuj menu kontekstowe. Włączone zawsze pokazuje menu kontekstowe gdy następuje wybór węzła/linii, wyłączone pokazuje menu kontekstowe tylko gdy zaznaczenie jest niejednoznaczne. Domyślnie: wyłączone (dawniej włączone).
* Włącz jasny styl. Na nowych urządzeniach domyślnie włączone. Na starszych urządzeniach może być wadliwe.
* Pokaż statystyki. Pokazuje statystyki służące do odnajdywania błędów oprogramowania, nie są zbyt przydatne. Domyślnie: wyłączone (dawniej włączone).  

## Zgłaszanie Problemów

Jeśli nastąpi awaria Vespucci, lub plik stanu będzie wadliwy, zostaniesz zapytany czy chcesz wysłać raport o błędach. Apelujemy byś to zrobił/zrobiła, ale tylko raz na ten sam rodzaj błędu. Jeśli chcesz dodać więcej informacji lub złożyć prośbę o nową funkcjonalność lub w podobnej sprawie, zrób to tutaj: [Vespucci issue tracker](https://github.com/MarcusWolschon/osmeditor4android/issues). Jeżeli chcesz przedyskutować sprawę związaną z Vespucci, możesz to zrobić albo na [Vespucci google group](https://groups.google.com/forum/#!forum/osmeditor4android) albo na [OpenStreetMap Android forum](http://forum.openstreetmap.org/viewforum.php?id=56)


