# RDetector — notatki rozwojowe

## Cel

Dać dziecku natychmiastową, łagodną informację zwrotną w grze bez płatnych usług
chmurowych. Detektor ma preferować motywację i responsywność, a nie pełną diagnozę
artykulacji.

## Aktualny algorytm

- PCM mono 16 kHz, 16-bit.
- Ramka wejściowa: około 50 ms.
- Okno analizy: do 600 ms.
- Autokorelacja w zakresie około 80–400 Hz -> orientacyjna dźwięczność.
- Obwiednia RMS w blokach 10 ms -> wyszukiwanie krótkich lokalnych spadków i odbić.
- Zero crossing rate -> odrzucanie części szumu/syczenia/kliknięć.
- Łączny R-score: 0–100.
- Startowy próg zaliczenia: 67/100 przez co najmniej około 120 ms.
- Kolejna detekcja wymaga około 280 ms ciszy.

## Ograniczenia

Algorytm jest heurystyczny. Może pomylić inne dźwięczne, szybko modulowane dźwięki
z „r” albo nie zaliczyć słabego, ale poprawnego „r”. Parametry wymagają kalibracji
na rzeczywistych nagraniach w różnych telefonach i warunkach akustycznych.

## Zalecany kolejny krok

Dodać opcjonalny tryb zbierania oznaczonych próbek przez dorosłego/logopedę:
`poprawne R`, `próba R`, `inne`. Nagrania powinny domyślnie pozostawać lokalnie.
Na takim zbiorze można wyznaczyć lepsze progi albo wytrenować mały klasyfikator
TFLite, pozostawiając interfejs `RDetector` bez zmian.
