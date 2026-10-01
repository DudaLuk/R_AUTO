# R Auto 0.3.0 — Android / Kotlin

Gra wspierająca ćwiczenia głoski „r”. Wersja 0.3 dodaje eksperymentalny,
całkowicie lokalny detektor cech polskiego drżącego „r”. Nie wymaga Azure,
Amazon, konta, klucza API ani połączenia z internetem.

> Automatyczna ocena jest funkcją BETA i elementem gry. Nie jest narzędziem
> diagnostycznym i nie zastępuje oceny logopedy.

## Tryby

1. **Z rodzicem** — dorosły przyznaje gwiazdkę po udanej próbie.
2. **Automatyczne R — BETA** — mikrofon analizowany jest lokalnie; wysoki R-score
   automatycznie daje dopalacz i gwiazdkę.
3. **Laboratorium** — prosty test mikrofonu reagujący wyłącznie na głośność.

## Jak działa Automatyczne R

`AudioRecord` pobiera mono PCM 16 kHz w blokach po 50 ms. `RDetector` utrzymuje
krótkie okno około 600 ms i wylicza wynik 0–100 na podstawie kilku cech:

- dźwięczności / okresowości sygnału (autokorelacja),
- szybkich spadków i odbić obwiedni amplitudy, które mogą odpowiadać kolejnym
  kontaktom języka przy drżącym „r”,
- energii sygnału,
- kary za sygnały o wysokiej liczbie przejść przez zero, typowe dla szumu,
  syczenia i części krótkich zakłóceń.

Nagroda pojawia się dopiero po utrzymaniu wysokiego wyniku przez kilka kolejnych
ramek. Po zaliczeniu detektor czeka na krótką ciszę przed kolejną nagrodą.
Ma to ograniczyć wielokrotne naliczanie punktów za jedną próbę.

Próg i wagi są startowe. Najważniejszy kolejny etap to zebranie prawdziwych próbek
„r” / „nie-r” i dostrojenie detektora, a później opcjonalnie zastąpienie heurystyki
małym lokalnym modelem TFLite.

## Prywatność

- brak uprawnienia `INTERNET`,
- brak SDK Azure/Amazon,
- brak wysyłania audio,
- brak zapisywania nagrań,
- PCM istnieje wyłącznie w pamięci podczas aktywnej sesji,
- mikrofon zatrzymuje się po pauzie lub przejściu aplikacji w tło.

## Kompilacja

Wymagania: Android Studio, JDK 17, Android SDK 35.

Windows:

```bat
gradlew.bat assembleDebug testDebugUnitTest
```

Linux/macOS:

```sh
chmod +x gradlew
./gradlew assembleDebug testDebugUnitTest
```

APK po kompilacji:

`app/build/outputs/apk/debug/app-debug.apk`

## Najważniejsze pliki

- `MainActivity.kt` — UI i obsługa trybów gry,
- `AudioMonitor.kt` — lokalny odczyt PCM 16 kHz,
- `RDetector.kt` — eksperymentalny lokalny detektor „r”,
- `SoundGate.kt` — detektor głośności używany tylko w Laboratorium,
- `RaceEngine.kt` — mechanika samochodu,
- `RewardSystem.kt` — punkty, serie i bonusy.

## Wersja 0.3.0

- usunięto Azure Speech SDK i konfigurację klucza/regionu,
- usunięto uprawnienie `INTERNET`,
- dodano tryb **Automatyczne R — BETA**, działający offline,
- `AudioMonitor` przekazuje surowe ramki PCM do lokalnego analizatora,
- dodano `RDetector` i R-score 0–100,
- automatyczna nagroda wymaga utrzymania wysokiego wyniku,
- zachowano tryb rodzica i Laboratorium mikrofonu.
