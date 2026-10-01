# R Auto 0.1.0 — projekt Android / Kotlin

Gra offline do wspólnego ćwiczenia z dzieckiem. Dorosły wpisuje ćwiczenie ustalone
z logopedą i ocenia próbę, a samochód przyspiesza po naciśnięciu przycisku.

**To działający prototyp gry, nie automatyczny logopeda. W tej wersji nie ma
modelu rozpoznającego poprawność polskiej głoski „r”.** Laboratorium reaguje
wyłącznie na głośność dźwięku — również klaskanie, telewizor i inne głoski.
Nie należy traktować jego reakcji jako potwierdzenia poprawnej wymowy.

## Kompilacja w Android Studio

1. Rozpakuj ZIP. Otwórz folder `RAuto` (z plikiem `settings.gradle.kts`) przez **Open**.
2. Wybierz JDK 17 dla Gradle: Settings → Build, Execution, Deployment → Build Tools → Gradle.
3. W SDK Manager zainstaluj Android SDK Platform 35 i Build Tools 35.0.0.
4. Poczekaj na synchronizację Gradle. Pierwsza kompilacja wymaga internetu do
   pobrania zależności; aplikacja po instalacji działa całkowicie offline.
5. Podłącz telefon z Androidem 8.0 lub nowszym, włącz debugowanie USB i naciśnij **Run**.
6. Aby zbudować plik APK, uruchom z terminala w folderze `RAuto`:

Windows:

```bat
gradlew.bat assembleDebug testDebugUnitTest
```

Linux / macOS:

```sh
chmod +x gradlew
./gradlew assembleDebug testDebugUnitTest
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.
Jeśli SDK nie zostanie znalezione, otwórz projekt w Android Studio albo dodaj
lokalny `local.properties`, np. `sdk.dir=C:/Users/Jan/AppData/Local/Android/Sdk`.
Nie dodawaj tego pliku do repozytorium. Projekt zawiera standardowy Gradle Wrapper.

Konfiguracja: AGP 8.7.3, Gradle 8.9, Kotlin 2.0.21, JDK 17, compile/target SDK 35,
minimum SDK 26. Nie jest wymagany NDK, konto chmurowe ani klucz API.
To konfiguracja do budowania prototypu, nie deklaracja zgodności z przyszłymi
wymaganiami publikacji Google Play.

## Pierwsza sesja

1. Zostaw tryb **Z rodzicem**. Wpisz ćwiczenie zalecone przez logopedę.
2. Wybierz 60 lub 120 sekund i naciśnij **Start podróży**.
3. Po udanej próbie naciśnij **Udana próba** — 3,5 sekundy dopalacza i jedna gwiazdka.
4. **Jeszcze próbujemy** wygasza dopalacz. Samochód łagodnie zwalnia, nie traci gwiazdek.
5. Przerwa na oddech nie wymaga ciągłego wydawania dźwięku.
6. W garażu dostępne są trzy kolory auta: żółty od początku, koralowy od 10 gwiazdek,
   niebieski od 30. Gwiazdki są nagrodą w grze, nie miarą postępów logopedycznych.

## Laboratorium mikrofonu

Wybierz tryb Laboratorium przed startem i przyznaj dostęp do mikrofonu.
Po przyznaniu zgody naciśnij Start ponownie. Próg można dostosować suwakiem:
przesunięcie w prawo wymaga głośniejszego sygnału. To względny poziom dBFS,
nie pomiar natężenia dźwięku w otoczeniu. Nie zachęcaj do krzyku.

Detektor wymaga około 200 ms sygnału powyżej progu i około 400 ms ciszy przed
kolejnym wyzwoleniem. Nie klasyfikuje mowy. Nie daje gwiazdek ani oceny
„poprawnie/niepoprawnie”. Przy braku zgody na mikrofon tryb rodzica nadal działa.

## Dane i cykl życia

- Brak uprawnienia INTERNET, reklam, analityki, usług sieciowych i zewnętrznych assetów.
- Bufor PCM przetwarzany jest tylko w pamięci; nagrania nie są zapisywane.
- Mikrofon działa wyłącznie podczas aktywnej sesji testowej. Pauza, zablokowanie
  ekranu i przejście w tło kończą nasłuch. Wznowienie wymaga przycisku.
- Lokalnie zapisywane są gwiazdki, kolor auta, treść ćwiczenia i próg detektora.
- Obrót ekranu / odtworzenie Activity resetuje bieżącą trasę. Gwiazdki pozostają.
- Wyczyszczenie danych aplikacji w ustawieniach Androida usuwa zapisany postęp.
- Kopia zapasowa Androida jest wyłączona w manifeście.
- Gra celowo nie odtwarza dźwięku silnika, żeby nie pobudzać własnego detektora.

## Struktura

- `MainActivity.kt`: interfejs, zgody, sterowanie sesją, lokalny postęp.
- `RoadView.kt`: grafika Canvas; cała scena rysowana programowo.
- `RaceEngine.kt`: czas sesji, prędkość i nagrody, bez zależności od Androida.
- `AudioMonitor.kt`: AudioRecord, mono PCM 16 kHz, obsługa błędów i zatrzymywania.
- `SoundGate.kt`: detektor energii, bez analizy fonetycznej.
- `EngineTest.kt`: testy mechaniki i bramki dźwięku.
- `docs/ROZWOJ_MODELU.md`: zakres kolejnego etapu automatycznej oceny.
- `docs/TESTY_MANUALNE.md`: scenariusze do sprawdzenia na telefonie.

## Opis commita

`feat(android): add offline R Auto parent-guided racing prototype and microphone lab`

## Zmiany 0.1.0

- Gra 2D z przyspieszaniem i łagodnym zwalnianiem.
- Dwa tryby: ocena rodzica oraz jawnie oznaczony test głośności.
- Sesje 60/120 sekund, pauza, gwiazdki, garaż i lokalne ustawienia.
- Nasłuch bez zapisu i bez internetu, kontrola zgód i cyklu życia.
- Gradle Wrapper, instrukcja kompilacji i testy jednostkowe.
- Azure Speech Pronunciation Assessment w trybie `pl-PL` oraz konfiguracja klucza/regionu.
- Punkty, serie, poziomy i bonusowe dopalacze co piątą poprawną próbę.

## Wersja 0.2 — internetowa ocena wymowy i bonusy

Dodano tryb Azure Speech z oceną fonemową dla `pl-PL` oraz lokalny system progresji:
serie poprawnych prób, punkty, poziomy i dopalacze. Szczegóły konfiguracji połączenia
znajdują się w `docs/ONLINE_PRONUNCIATION.md`. Tryb rodzica i laboratorium offline
nadal pozostają dostępne.

Ważne: bez konfiguracji Azure tryb online nie działa. Próg 70/100 jest parametrem
startowym do testów i musi zostać zweryfikowany z logopedą.

## Gotowy APK

W folderze `apk` znajduje się `RAuto-0.1.0-debug.apk`, zbudowany z dołączonego kodu.
Możesz skopiować go na telefon i otworzyć, dopuszczając instalację z wybranego
menedżera plików. To wersja debug do testów. Zainstalowanie własnej kompilacji
podpisanej innym kluczem może wymagać odinstalowania tej wersji (utrata postępu).
Wyniki sprawdzenia są w `docs/WERYFIKACJA.md`.
