# Meilenstein 10 – Overex: E2E-Tests für Events und ein gemeinsamer Workflow `web-checks`

| Feld | Wert |
|---|---|
| Projekt | Overex |
| Zeitraum | 2026-11-02 – 2026-11-02 |
| Dokumentiert am | 2026-11-03 19:13 CET (aus Git-Historie und der Übergabe-Notiz vom 02.11) |
| Repository / Service | `overex`: Web-Client `apps/web` (Playwright), Backend `apps/backend` (Seed), CI `.github/workflows`, Doku `docs/` |
| Thema | Playwright-Tests für Event-Liste, Details und Filter, als Gast und eingeloggt; Seed-Daten im Spring-Profil `e2e`; drei Frontend-Workflows zu `web-checks` zusammengeführt; Workflows lokal prüfen mit actionlint und act |
| Commits | 4 (`29d1717`..`65f6308`, Bereich `6a6ea24..65f6308`) |
| Zeitaufwand | ca. 4 h (laut User) |
| Bezug | Projektarbeit (5. Semester) |

## Ziel
Das Event-Feature sollte Ende zu Ende abgesichert werden: echter Browser, echtes Backend, echte Datenbank, und zwar für Gäste und für eingeloggte Nutzer. Gleichzeitig sollte die CI für den Web-Client übersichtlicher und schneller scheitern, und Workflow-Änderungen sollten sich lokal prüfen lassen, statt erst nach einem Push auf GitHub aufzufallen.

## Ausgangslage
- E2E-Tests gab es nur für Auth (`signed-in.spec.ts`, `signed-out.spec.ts`); `web-e2e-ci` war seit `54f61a9` rot.
- Der Helfer `expectEventFor(page, account)` ignorierte den Account und prüfte nur die URL.
- Die E2E-Datenbank war leer, Event-Tests hätten keine Daten gehabt.
- Drei getrennte Workflows `web-build-ci`, `web-unit-ci`, `web-e2e-ci`, die unabhängig voneinander liefen.
- Workflow-Fehler sah ich erst auf GitHub und musste sie mit einem neuen Commit beheben.

## Umsetzung (chronologisch)
1. **2026-11-02** – E2E-Testnamen nach dem Schema „<tut was> when <Bedingung>“ mit Arrange/Act/Assert. `expectEventFor` ersetzt durch `expectSignedIn(page)`: wartet zusätzlich auf den Sign-out-Button der Sidebar, der erst nach erfolgreichem `GET /me` erscheint. Weiterleitungstest pro Seite aufgeteilt; nach dem Sign-out wird geprüft, dass der Button verschwunden ist (Gastmodus) (`29d1717`).
2. **2026-11-02** – Seed `db/e2e-seed.sql` nur im Profil `e2e`: 12 Events (zwei Seiten), Kategorien Concert, Festival, Theater, Tennis (Theater und Tennis leer), Daten relativ zu `now()`, Featured Event = Event 07; gespiegelt in `__tests__/e2e/utils/seed.ts`. E2E-Tests für Liste, Details, Zurück-Navigation, Featured Event, Kategoriefilter, leere Kategorie, Sortierung, Paginierung und unbekannte Event-ID. Die Detailseite zeigt bei unbekannter ID „existiert nicht“ statt endlos zu laden. Specs nach `features/` sortiert, Test-IDs an den Event-Karten, Arrange-Helper `openEventDetails`. `run-e2e.sh` reicht Argumente an Playwright weiter (`npm run test:e2e -- -g …`), der HTML-Report öffnet sich nicht mehr von selbst (`4f43b25`).
3. **2026-11-02** – Liste und Details laufen in einer Schleife einmal als Gast und einmal eingeloggt. Die Gast-Variante beweist, dass der `AuthProvider` ohne Sitzung rendert und das Backend `GET /events` ohne Token liefert (`6a0f195`).
4. **2026-11-02** – `web-checks` ersetzt die drei Workflows: `build` (Lint, Typecheck, `next build`) und `unit` parallel, `e2e` erst wenn beide grün sind (`needs:`). Upload-Schritte unter act übersprungen (`!env.ACT`). `npm run lint:workflows` startet actionlint über das Docker-Image `rhysd/actionlint:1.7.12`; `.actrc` verknüpft `ubuntu-latest` mit `catthehacker/ubuntu:act-latest`; `docker-compose.test.yml` bekommt einen eigenen Projektnamen (`overex-test`); Doku in `docs/CI.md` (`65f6308`).

## Geänderte Dateien
25 Dateien, +1026/−387 Zeilen.

| Datei | Art | Zweck |
|---|---|---|
| `apps/backend/src/main/resources/db/e2e-seed.sql`, `application-e2e.properties` | neu / geändert | Testdaten nur im Profil `e2e` |
| `apps/web/__tests__/e2e/features/events/events.spec.ts` | neu | Event-Tests, Schleife Gast/eingeloggt |
| `apps/web/__tests__/e2e/features/auth/{signed-in,signed-out}.spec.ts` | neu / verschoben | Auth-Tests in `features/` |
| `apps/web/__tests__/e2e/utils/{flows,seed}.ts`, `setup/auth.setup.ts` | geändert / neu | `expectSignedIn`, `openEventDetails`, Spiegel des Seeds |
| `apps/web/app/(shell)/events/[id]/page.tsx` | geändert | „existiert nicht“ bei unbekannter ID |
| `apps/web/features/event/components/*.tsx` | geändert | Test-IDs |
| `apps/web/scripts/run-e2e.sh`, `playwright.config.ts`, `package.json` | neu / geändert | DB hoch, Playwright, Port 8081 freigeben, DB runter, Exit-Code durchreichen; `lint:workflows` |
| `.github/workflows/web-checks.yml` | neu | ein Workflow, drei Jobs |
| `.github/workflows/web-{build,unit,e2e}-ci.yml` | gelöscht | ersetzt durch `web-checks` |
| `.actrc`, `docker-compose.test.yml` | neu / geändert | act-Runner-Image, eigener Compose-Projektname |
| `docs/CI.md`, `docs/TESTING.md`, `README.md` | geändert | Workflow, actionlint, act, ein Badge |

## Entscheidungen
| Entscheidung | Verworfene Alternativen | Begründung |
|---|---|---|
| Ein Workflow mit drei Jobs, `e2e` per `needs:` hinter `build` und `unit` | drei unabhängige Workflows (alter Stand) | Fail-fast: der teure E2E-Job läuft nur, wenn die billigen Checks grün sind (laut Übergabe, angelehnt an Cal.com) |
| `build` und `unit` laufen auch bei Backend-Änderungen mit | Pfad-Filter pro Job | übersprungene Jobs würden `e2e` über `needs` mit überspringen; der Umweg kostet mehr Komplexität als die eine Minute |
| Seed per SQL im Profil `e2e`, Daten relativ zu `now()` | Testdaten per API im Test anlegen; feste Datumswerte | stabile Daten für alle Tests, veralten nicht |
| Liste und Details in einer Schleife als Gast und eingeloggt | nur eingeloggt testen | der Gastmodus aus M08 wird mitgeprüft |
| actionlint über ein gepinntes Docker-Image (1.7.12) | lokale Installation, `latest` | jeder Rechner prüft gleich, nur Docker nötig |
| In der CI ein Playwright-Worker, lokal halbe CPU-Kerne | gleiche Einstellung überall | Stabilität auf der kleinen CI-Maschine mit geteiltem Backend (laut Übergabe) |

## Neu gelernt
Laut Übergabe-Notiz vom 02.11 (Abschnitt „Gelerntes“):
- **Playwright-Locators**: Beschreibungen, die bei jeder Benutzung neu auflösen; Web-first-Assertions warten automatisch. Reihenfolge `getByRole` > `getByLabel` > `getByText` > `getByTestId`; `getByRole` nutzt den Accessibility-Tree.
- **Workers und Kontexte**: `workers` = parallele Node-Prozesse mit eigenem Browser; jeder Test bekommt einen neuen Browser-Kontext (wie ein Inkognito-Fenster), `storageState` befüllt ihn vor.
- **Docker-Grundlagen**: Image vs. Container, Schichten, Build-Cache, Digest vs. Tag, Compose-Netzwerke und Dienstnamen als Hostnamen.
- **GitHub Actions**: `concurrency.group` als Warteschlange, Actions-Cache mit Schlüssel aus dem Lockfile-Hash.
- **actionlint und act**: actionlint liest Workflow-Dateien und findet Fehler in Syntax, Ausdrücken und `run:`-Skripten; act führt Workflows lokal in Docker-Containern aus.

## Tests & Verifikation
| Test | Ergebnis |
|---|---|
| CI auf `29d1717` | `web-build-ci`, `web-unit-ci`, `web-e2e-ci` grün (E2E erstmals seit `54f61a9` wieder grün) |
| CI auf `4f43b25` | alle fünf Workflows grün |
| `gh act push -j e2e` lokal (laut Übergabe) | 28 von 28 E2E-Tests bestanden |
| CI auf `65f6308` (erster Lauf von `web-checks`, geprüft am 03.11) | grün, 5 min 23 s |
| `npm run test:unit` / Playwright (laut Übergabe) | 76 Unit-Tests, 28 E2E-Tests |

## Probleme & Lösungen
- **Problem:** Die Detailseite lud bei einer unbekannten Event-ID endlos. → **Lösung:** eigene Meldung „existiert nicht“, abgesichert durch einen E2E-Test (`4f43b25`).
- **Problem:** `expectEventFor` prüfte nur die URL, ein fehlgeschlagener Login wäre unbemerkt geblieben. → **Lösung:** `expectSignedIn` wartet auf ein Element, das erst nach erfolgreichem `GET /me` erscheint (`29d1717`).
- **Problem:** `upload-artifact@v7` funktioniert mit dem Artifact-Server von act nicht. → **Lösung:** Upload-Schritte mit `if: ${{ !cancelled() && !env.ACT }}` (`65f6308`).
- **Problem:** Test- und Dev-Compose teilten sich ein Netzwerk. → **Lösung:** eigener Projektname `overex-test` (`65f6308`).
- **Problem:** Der Spring-Prozess überlebte Playwright und blockierte Port 8081 für den nächsten Lauf. → **Lösung:** `run-e2e.sh` beendet den Prozess mit `fuser -k 8081/tcp`.
- Laut Übergabe wurde `apps/web` kurz in `apps/frontend` umbenannt und wieder zurück, weil ältere Commit-Nachrichten den Scope `web` nutzen.

## Eingesetzte Hilfsmittel
Alle vier Commits tragen Claude (Claude Code) als Co-Autor. Laut Übergabe galt „ich schreibe, Claude reviewt“ und „Fragen erklären statt Code schreiben“; laut User habe ich KI nur teilweise eingesetzt. Der Anteil je Commit ist nicht erfasst. (Hinweis: FH-Richtlinien zur Kennzeichnung beachten.)

## Offene Punkte / Nächste Schritte
- Ersten `web-checks`-Lauf auf GitHub prüfen (am 03.11 erledigt: grün).
- `npm audit` in `apps/web` (46 Lücken laut Übergabe).
- Breadcrumb der Detailseite zeigt die Event-ID statt des Titels.
- Seed: Tennis fehlt in der Übersicht oben in `e2e-seed.sql`.

## Commit-Verlauf
| Datum | Hash | Nachricht |
|---|---|---|
| 2026-11-02 10:34 | `29d1717` | test(web): clearer e2e test names and a real signed-in check |
| 2026-11-02 15:18 | `4f43b25` | test(web): e2e tests for the event list, details and filters |
| 2026-11-02 17:57 | `6a0f195` | test(web): run the event list e2e tests as guest and as signed-in user |
| 2026-11-02 23:54 | `65f6308` | ci(web): one web-checks workflow with build, unit and e2e jobs |
