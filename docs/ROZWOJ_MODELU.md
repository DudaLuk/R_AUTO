# Następny etap: wiarygodna ocena wymowy offline

Nie dołączono atrap modelu ani progów udających ocenę „r”. Obecny SoundGate jest
wyłącznie narzędziem sprawdzenia przepływu mikrofon → zdarzenie → samochód.

Do wdrożenia automatycznej oceny potrzebne są:

1. Zakres ćwiczeń i definicja poprawności ustalone z logopedą. Inne zadania to
   izolowana głoska, sylaba i słowo. Nie należy zakładać wspólnego progu.
2. Osobny, świadomie uruchamiany proces zebrania reprezentatywnych nagrań
   z odpowiednimi zgodami. Obecna aplikacja ich nie zbiera.
3. Etykiety ekspertów: poprawnie, niepoprawnie, niepewne / nieocenialne.
   Uwzględnić różne realizacje błędne, hałas, ciszę, oddechy i inne głoski.
4. Rozdzielenie dzieci między zbiory uczenia, walidacji i testu, aby model nie
   otrzymywał na teście nagrań dzieci znanych z uczenia.
5. Ocena fałszywego nagradzania błędnej wymowy, odrzucania poprawnej oraz udziału
   niepewnych wyników; wyniki także dla poszczególnych ćwiczeń i urządzeń.
6. Dobór modelu i konwersja do runtime mobilnego. Pomiar opóźnienia i zużycia energii
   na docelowym telefonie. Model i wszystkie zasoby muszą znajdować się w APK.
7. Integracja osobnego interfejsu oceny zwracającego poprawne / niepoprawne /
   niepewne. Wynik niepewny nie powinien powodować kary. Pauza na oddech to nie błąd.
8. Dopiero po walidacji przez logopedę włączenie nagród na podstawie klasyfikatora.

Nie wystarczy wymienić próg głośności na próg „wibracji”. Nie wystarczy też
zamienić nagrania na tekst i sprawdzić obecność litery r.
