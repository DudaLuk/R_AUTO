# R Auto 0.3.0 — lista zmian

- usunięto Microsoft Azure Speech SDK,
- usunięto konfigurację Azure i klucze z UI/preferences,
- usunięto uprawnienie INTERNET,
- dodano tryb `Automatyczne R — BETA (offline)`,
- dodano `RDetector.kt` analizujący lokalnie PCM 16 kHz,
- detektor wykorzystuje dźwięczność/autokorelację, obwiednię RMS i ZCR,
- dodano wynik `R-score` 0–100 i automatyczne nagradzanie po utrzymaniu progu,
- dodano blokadę wielokrotnej nagrody za jedną próbę i reset po ciszy,
- zachowano ręczny tryb rodzica oraz Laboratorium głośności,
- `AudioMonitor` przekazuje teraz także surowe ramki PCM,
- nagrania nadal nie są zapisywane ani wysyłane,
- zaktualizowano README i dokumentację `docs/R_DETECTOR.md`,
- dodano `RDetectorTest.kt`,
- poprawiono `.gitignore` dla Android Studio / Gradle,
- wersja aplikacji: 0.3.0 / versionCode 3.

## Proponowany commit

`feat(audio): replace Azure with offline experimental R detector`
