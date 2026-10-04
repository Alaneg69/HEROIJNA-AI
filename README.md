# Herobrine AI — Fabric 26.2

Pierwsza grywalna wersja moda o Herobrine, którego zachowanie jest sterowane przez system planowania i pamięć gracza.

## Co działa

- Herobrine jako własna encja z prostym pathfindingiem.
- Adaptacyjna pamięć gracza: unikanie, reakcje, liczba spotkań, rozmowy i ostatnia taktyka.
- Eventy: pojawianie się, szept/wiadomość, tabliczki, małe budowle, opętanie zwierzęcia, koszmar.
- Rozmowa przez zwykły czat: gdy napiszesz `Herobrine` / `herobrine`, system może odpowiedzieć.
- `/herobrine summon`, `/herobrine awaken`, `/herobrine nightmare`, `/herobrine map`, `/herobrine status`, `/herobrine reset`.
- Bez klucza API mod działa w trybie heurystycznym.
- Z kluczem API może używać zewnętrznego modelu do planowania następnej taktyki i generowania dialogu.

## AI / prywatność

Prawdziwy model AI wymaga `HEROBRINE_OPENAI_API_KEY` w środowisku procesu Minecrafta.
Mod wysyła do API tylko kontekst potrzebny do decyzji: pseudonim gracza, ogólne zachowanie, statystyki reakcji oraz ostatnią wiadomość na czacie. Nie wkładaj klucza API do plików moda.

Domyślny model to `gpt-5` i endpoint Responses API. Możesz zmienić go przez zmienne `HEROBRINE_OPENAI_MODEL` i `HEROBRINE_OPENAI_URL`.

## Uruchomienie development

Aktualny Fabric 26.2 wymaga JDK 25. Otwórz folder jako projekt Gradle w IntelliJ IDEA. Przykładowy projekt Fabric 26.2 używa Loom 1.18-SNAPSHOT, Loader 0.19.5, Fabric API 0.161.0+26.2 oraz Gradle 9.7.1.

Po skonfigurowaniu środowiska:

```text
./gradlew runClient
./gradlew build
```

Gotowy plik moda pojawi się w `build/libs/`.

## Start w grze

```text
/herobrine awaken
```

Automatyczne eventy są aktywne. Żeby wymusić scenę testową:

```text
/herobrine summon
/herobrine map
/herobrine nightmare
```

## Ważne

To jest pierwsza wersja architektury, a nie finalny „AAA horror”. AI ma ograniczony zestaw narzędzi i samo nie może wykonywać arbitralnych komend Minecrafta. To celowe: model może planować, ale wykonanie przechodzi przez walidowane akcje moda.
