# Ocena głoski „r” online

Wersja 0.2 dodaje tryb **Ocena wymowy online — Azure Speech**. Aplikacja używa
Azure AI Speech Pronunciation Assessment z językiem `pl-PL`, tekstem referencyjnym
wpisanym w polu ćwiczenia i oceną fonemową. Wynik 0–100 steruje nagrodą:

- wynik co najmniej 70 — próba zaliczona, dopalacz i punkty;
- wynik poniżej 70 — łagodna zachęta do powtórzenia, bez kary dla punktów;
- błąd sieci, brak rozpoznania lub wynik niepewny — brak nagrody i możliwość próby ponownie.

## Konfiguracja Azure

1. W portalu Azure utwórz zasób **Azure AI Speech**.
2. Wybierz region zasobu i skopiuj jeden z kluczy.
3. W aplikacji wybierz tryb online i przy pierwszej próbie wpisz klucz oraz region,
   np. `westeurope`.
4. Klucz jest przechowywany lokalnie w preferencjach Androida. W wersji produkcyjnej
   zalecane jest zastąpienie bezpośredniego klucza własnym serwerem pośredniczącym,
   bo klucz zapisany w APK lub na urządzeniu może zostać odczytany.

Aplikacja nie wysyła dźwięku w tle. Nagranie jest przekazywane do usługi tylko po
naciśnięciu **Udana próba** w trybie online. Do poprawnej walidacji nadal potrzebna
jest ocena logopedy: próg 70 jest wartością startową, nie diagnozą kliniczną.

Tekst referencyjny powinien odpowiadać zadaniu logopedycznemu, np. `ra`, `ryba`,
`rower`. Sama obecność litery „r” w rozpoznanym tekście nie jest kryterium — używany
jest wynik Pronunciation Assessment. Wypowiedź dziecka może być oceniona gorzej przez
hałas, odległość od telefonu, wiek dziecka lub nietypową realizację głoski.
