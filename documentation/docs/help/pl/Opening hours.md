# Edytor godzin otwarcia OpenStreetMap

Specyfikacja godzin otwarcia w OpenStreetMap jest dość skomplikowana i nie ma prostego i intuicyjnego interfejsu użytkownika.

Jednak przez większość czasu używasz tylko niewielkiej części definicji. Edytor bierze to pod uwagę próbując ukryć bardziej niejasne funkcje w menu i redukując większość czasu pracy do małych poprawek wstępnie zdefiniowanych szablonów.

_Ta dokumentacja jest w trakcie opracowywania_

## Używanie edytora godzin otwarcia

Zazwyczaj podczas pracy obiekty, które edytujesz już mają tagi godzin otwarcia (opening_hours, service_times i collection_times) lub możesz ponownie zastosować wstępny zestaw dla obiektu, aby uzyskać puste pole godzin otwarcia. Jeżeli potrzebujesz dodać pole ręcznie i używasz Vespucci możesz wpisać klucz na stronie szczegółów i przełączyć się z powrotem na zakładkę z formularzem edycji. Jeżeli uważasz, że tag godzin otwarcia powinien być częścią wstępnych ustawień, zgłoś to twórcom swojego edytora.

Jeśli masz zdefiniowany domyślny szablon (zrób to prze "Zarządzanie szablonami"  w menu), to zostanie on automatycznie załadowany, jeśli edytor włączy się z pustą wartością. Funkcja "Wczytaj szablon" pozwala wczytać dowolny zapisany szablon, a menu "Zapisz szablon" umożliwia zapisanie aktualnych wartości jako szablon. Możesz określić osobne szablony i domyślne wartości dla tagów "opening_hours", "collection_times" i "service_times".

Oczywiście możesz zbudować wartość godzin otwarcia do podstaw, ale zalecamy na początek użycie jednego z istniejących szablonów.

Jeżeli są załadowane istniejące godziny otwarcia, zostaje podjęta próba automatycznej korekty, aby dostosować się do specyfikacji godzin otwarcia. Jeżeli to jest niemożliwe, to taka lokalizacja zostanie podświetlona w wyświetlaniu surowych danych OH i możesz spróbować poprawić to ręcznie. Z grubsza jedna czwarta wartości OH w bazie danych OpenStreetMap ma problemy, ale mniej niż 10% nie może być poprawionych. Zobacz [OpeningHoursParser](https://github.com/simonpoole/OpeningHoursParser), aby dowiedzieć się jakie odstępstwa od specyfikacji są tolerowane.

### Przycisk głównego menu

* __Dodaj regułę__: dodaje nową regułę.
* __Dodaj regułę dla dni świątecznych__: dodaje nową regułę dla dni świątecznych wraz ze zmianą stanu.
* __Dodaj regułę dla 24/7__: dodaje regułę dla obiektów, które są zawsze otwarte, specyfikacja godzin otwarcia nie wspiera żadnych innych wartości dla 24/7, ale można dodać selektory wyższego poziomu (na przykład zakresy lat).
* __Wczytaj szablon__: wczytuje istniejący szablon.
* __Zapisz do szablonu__: zapisuje aktualne wartości godzin otwarcia jako szablon do użycia w przyszłości.
* __Zarządzaj szablonami__: edytuje, na przykład zmienia nazwę i usuwa istniejące szablony.
* __Wczytaj ponownie__: analizuje ponownie wartość godzin otwarcia.
* __Usuń wszystkie__: usuwa wszystkie reguły.

### Reguły

Domyślne reguły są dodawane jako _normalne_, co skutkuje tym, że nadpisują one wartości poprzednich reguł dla tych samych dni. To może być uciążliwe, kiedy określasz rozszerzone okresy czasu, więc pewnie będziesz chciał wtedy przełączyć reguły za pomocą _Pokaż rodzaj reguły_ na _dodające_.

#### Menu reguły

* __Dodaj modyfkator/komentarz__: zmienia efekt działania tej reguły i dodaje opcjonalny komentarz.
* __Dodaj dni wolne__: dodaje selektor dla dni świątecznych lub wakacji.
* __Dodaj okres czasu...__
    * __Czas - czas__: czas początkowy do czasu końcowego w tym samym dniu.
    * __Czas - wydłużony czas__: czas początkowy do czasu końcowego w następnym dniu (przykładowo 26:00 daje 02:00 (w nocy) następnego dnia).
    * __Zmienny czas - czas__: od zmiennego czasu (świ, zmierzch, wschód i zachód słońca) do czasu końcowego w tym samym diu.
    * __Zmienny czas - wydłużony czas__: od zmiennego czasu do czasu końcowego w następnym dniu.
    * __Czas - zmienny czas__: czas początkowy do zmiennego czasu.
    * __Zmienny czas - zmienny czas__: zmienny czas do zmiennego czasu.
    * __Czas__: punkt w czasie.
    * __Czas - bez końca__: od punktu początkowego w czasie dalej.
    * __Zmienny czas__: o zmiennym czasie
    * __Zmienny czas - bez końca__: od zmiennego czasu dalej
* __Dodaj zakres dni tygodnia__: dodaj selektor na bazie dni tygodnia.
* __Dodaj zakres dat...__
    * __Data - data__: od daty początkowej (rok, miesiąc, dzień) do daty końcowej.
    * __Zmienna data - data__: od zmiennej daty początkowej (aktualnie specyfikacja definiuje tylko _Wielkanoc_) do daty końcowej.
    * __Data - zmienna data__: od daty początkowej do zmiennej daty.
    * __Zmienna data - zmienna data__: od zmiennej daty do zmiennej daty.
    * __Wystąpienie w miesiącu - wystąpienie w miesiącu__: od początkowego dnia wystąpienia w miesiącu do tego samego.
    * __Wystąpienie w miesiącu - data__: od początkowego dnia wystąpienia w miesiącu do daty końcowej.
    * __Data - wystąpienie w miesiącu__: od daty początkowej do końcowego dnia wystąpienia w miesiącu.
    * __Wystąpienie w miesiącu - zmienna data__: od początkowego dnia wystąpienia w miesiącu do końcowej zmiennej daty.
    * __Zmienna data - wystąpienie w miesiącu__: od początkowej zmiennej daty do końcowego dnia wystąpienia w miesiącu.
    * __Data - bez końca__: od daty początkowej dalej.
    * __Zmienna data - bez końca__: od początkowej zmiennej daty dalej.
    * __Wystąpienie w miesiącu - bez końca__: od początkowego dnia wystąpienia w miesiącu dalej.
    * __Z przesunięciami...__: te same wpisy, co powyżej, ale z określonymi przesunięciami (rzadko używane).
* __Dodaj zakres roczny__: dodaje selektor bazujący na roku.
* __Dodaj zakres tygodniowy__: dodaje selektor bazujący na numerze tygodnia.
* __Duplikuj__: tworzy kopię tej reguły i wstawia ją za aktualną pozycją.
* __Pokaż rodzaj reguły__: wyświetla i pozwala zmieniać rodzaj reguły pomiędzy _normalna_, _dodająca_ i _awaryjna_ (niedostępne dla pierwszej reguły).
* __Przenieś w górę__: przenosi tą regułę o jedną pozycję w górę (niedostępne dla pierwszej reguły).
* __Przenieś w dół__: przenosi tą regułę o jedną pozycję w dół.
* __Usuń__: usuwa tą regułę.

### Okresy czasu

Aby uczynić edytowanie okresów czasu tak łatwym, jak to tylko możliwe, próbujemy wybrać optymalny zakres czasu i skok pasków przy ładowaniu istniejących wartości. Dla nowych okresów czasu paski zaczynają się o 6:00 (rano) i mają 15-minutowy skok, co można zmienić w menu.

Clicking (not on the pins) the time bar will open the large time picker, when using the bars directly is too difficult. The time pickers extend in to the next day, so they are a simple way to extend a time range without having to delete and re-add the the range.

#### Menu okresów czasu

* __Display time picker__: show a large time picker for selecting start and end time, on very small displays this is the preferred way of changing times.
* __Przełącz na 15-minutowe skoki__: używa 15-minutowego skoku dla pasków zakresu.
* __Przełącz na 5-minutowe skoki__: używa 5-minutowego skoku dla pasków zakresu.
* __Przełącz na 1-minutowe skoki__: używa 1-minutowego skoku dla pasków zakresu, bardzo trudne w użyciu na telefonie.
* __Początek o północy__: ustawia początek paska zakresu o północy.
* __Pokaż interwał__: pokazuje pole interwału dla określenia go w minutach.
* __Usuń__: usuwa ten okres czasu.

