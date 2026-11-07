# Meilenstein 07 – Overex: Java 25, App-Shell und paginierte Event-Liste mit Kategorien

| Feld | Wert |
|---|---|
| Projekt | Overex |
| Zeitraum | 2026-10-17 – 2026-10-18 |
| Dokumentiert am | 2026-11-03 19:13 CET (nachträglich aus der Git-Historie) |
| Repository / Service | `overex` (github.com/Umex10/overex): Backend `apps/backend`, Web-Client `apps/web`, CI `.github/workflows` |
| Thema | Umstieg auf Java 25 (LTS); App-Shell mit Sidebar und Suche; Event-Liste aus der API mit Featured Event und Countdown; Kategorien und Paginierung im Backend; Frontend nach Feature-Ordnern |
| Commits | 6 (`f882b3f`..`7fcda8e`, Bereich `3d3b09a..7fcda8e`), davon ein Merge (`fb85f5d`, Branch `chore/jdk-25`) |
| Zeitaufwand | ca. 10 h: 17.10 ca. 4 h (geschätzt aus den Commits, nicht notiert), 18.10 ca. 6 h (laut User) |
| Bezug | Projektarbeit (5. Semester), erstes Frontend für das Event-Feature aus Meilenstein 06 |

## Ziel
Das Backend aus Meilenstein 06 konnte Events verwalten, im Web-Client gab es aber nur Login, Registrierung und ein leeres Dashboard. Ziel war eine erste echte Oberfläche: eine App-Shell (Sidebar, Kopfzeile, Suche) und eine Event-Seite, die die Events aus der API zeigt. Dafür brauchte das Backend Kategorien und eine paginierte Liste. Vorab sollte das Projekt auf Java 25 (aktuelle LTS) umziehen, ein offener Punkt aus Meilenstein 06.

## Ausgangslage
- Backend auf Java 21; VS Code meldete bei Lombok-Konstruktoren fälschlich „Flexible Constructor Bodies … source level 25“ (offener Punkt aus Meilenstein 06).
- `GET /event` lieferte alle Events ohne Paginierung, es gab keine Kategorien.
- Im Web-Client führte der Login auf `/dashboard`; Code war nach Art sortiert (`components/`, `lib/`, `redux/api/apis/`, `types/`), Dateinamen gemischt in PascalCase und kebab-case.

## Umsetzung (chronologisch)
1. **2026-10-17** – Java 25: `java.version` 25 in der `pom.xml`, Dockerfile auf `eclipse-temurin:25-jdk`/`25-jre`, Temurin 25 in `backend-verify-ci` und `web-e2e-ci`, README und `RUNNING.md` angepasst. Laut Commit-Nachricht unterstützen Lombok 1.18.44, MapStruct 1.6.3 und JaCoCo 0.8.15 Java 25 bereits; `./mvnw clean verify` lief mit allen 103 Tests (`f882b3f`, Merge `fb85f5d`).
2. **2026-10-17** – App-Shell: Route `/dashboard` → `/event` (Sign-in, Sign-up und `proxy.ts` leiten dorthin, Unit- und E2E-Tests angepasst); shadcn-Sidebar, die zu einer Icon-Leiste mit Tooltips einklappt; Overex-Logo (`SidebarLogo`); Kopfzeile mit Sidebar-Trigger und wiederverwendbarer `SearchBar`; Seite `/my-tickets` als Platzhalter. Im Datenmodell kamen `event_categories` und `events.category_id` dazu (`f7afcca`).
3. **2026-10-17** – Event-Liste: `getEvents` in der RTK-Query-API, `EventCard` als wiederverwendbarer Teaser, `FeaturedEventCard` für das Event mit dem nächsten Verkaufsstart, mit Live-Countdown (`react-countdown` 2.3.6) bis zum Verkaufsstart bzw. bis zum Eventbeginn; Bilder von `images.unsplash.com` in `next.config` erlaubt (`08ff7b1`).
4. **2026-10-18** – Kategorien und Paginierung: Backend mit `Category`-Entity, Repository, Service und `GET /category`; jedes Event gehört zu einer Kategorie (`categoryId` in Create- und Update-DTO, im Service geprüft). `GET /event` paginiert (10 pro Seite, sortiert nach `startsAt`) mit optionalem Filter `categoryId`, Antwort als DTO `{ content, page }`. Im Frontend shadcn-Pagination mit Ellipse. Laut Commit kompilierten die Backend-Tests danach nicht mehr, weil sie noch die alte `getEvents`-Signatur nutzten (`a259222`).
5. **2026-10-18** – Frontend-Umbau: Feature-Code nach `features/auth`, `features/user`, `features/event` wie die Backend-Pakete; `components/`, `hooks/`, `lib/`, `redux/`, `types/` nur noch für Gemeinsames; durchgehend kebab-case. Zustand und Queries der Event-Seite im Hook `useEventList` (neue Kategorie oder Sortierung setzt die Seite zurück); `EventPagination` und `CategoryFilter` als eigene Komponenten; Sortierung als ein Icon-Button; Tab-Titel „Events · Overex“ (`7fcda8e`).

## Geänderte Dateien
93 Dateien, +2918/−307 Zeilen (ohne Meilenstein-Doku).

| Datei | Art | Zweck |
|---|---|---|
| `apps/backend/pom.xml`, `apps/backend/Dockerfile` | geändert | Java 25, Temurin-25-Images |
| `.github/workflows/backend-verify-ci.yml`, `web-e2e-ci.yml`, `docker-publish.yml` | geändert | JDK 25 in der CI |
| `.../event/Category.java`, `CategoryController.java`, `CategoryRepository.java`, `CategoryService.java` | neu | Kategorien, `GET /category` |
| `.../event/Event.java`, `EventController.java`, `EventService.java`, `EventRepository.java`, `EventMapper.java`, DTOs | geändert | Kategorie am Event, paginierte Liste mit Filter |
| `.../auth/JwtService.java` | geändert | Token-Laufzeiten als statische Konstanten |
| `apps/backend/src/test/.../event/**` | geändert | an Kategorie angepasst (Paginierung erst in M08 vollständig getestet) |
| `apps/web/app/(shell)/event/{layout,page}.tsx`, `my-tickets/page.tsx` | neu | Event-Seite, Platzhalter „Meine Tickets“ |
| `apps/web/app/(shell)/dashboard/page.tsx` | gelöscht | ersetzt durch `/event` |
| `apps/web/components/app-sidebar.tsx`, `search-bar.tsx`, `sidebar-logo.tsx` | neu | App-Shell |
| `apps/web/components/ui/{sidebar,sheet,skeleton,card,badge,pagination,toggle,toggle-group}.tsx` | neu | shadcn-Bausteine |
| `apps/web/features/event/**` | neu | `event-api.ts`, `category-api.ts`, `event-types.ts`, `use-event-list.ts`, `event-card.tsx`, `featured-event-card.tsx`, `countdown-cards.tsx`, `category-filter.tsx`, `event-pagination.tsx` |
| `apps/web/features/auth/**`, `features/user/**` | verschoben/umbenannt | vorher `actions/`, `components/`, `lib/`, `redux/api/apis/`, `types/` |
| `apps/web/__tests__/**` | geändert/umbenannt | neue Route, kebab-case |
| `docs/diagrams/datenmodell.{mmd,svg}`, `docs/RUNNING.md`, `README.md` | geändert | Kategorien im Datenmodell, Java 25 |

## Entscheidungen
| Entscheidung | Verworfene Alternativen | Begründung |
|---|---|---|
| Java 25 in einem eigenen Branch (`chore/jdk-25`) | Upgrade nebenbei in `feature/event` | laut offenem Punkt aus M06: Lombok, JaCoCo, CI und Dockerfile hängen daran, eigener Merge hält es nachvollziehbar |
| Frontend nach Feature-Ordnern wie die Backend-Pakete | Sortierung nach Art (alter Stand) | laut Commit `7fcda8e`: ein Feature an einem Ort, gleiche Struktur wie im Backend |
| Paginierung im Backend (10 pro Seite, sortiert nach `startsAt`) | ganze Liste ans Frontend | aus dem Commit belegt; Begründung nicht festgehalten |
| Antwort als eigenes DTO `{ content, page }` | Spring-`Page` direkt serialisieren | aus dem Commit belegt; Begründung nicht festgehalten |
| Countdown mit `react-countdown` (gepinnt 2.3.6) | eigener Timer | aus dem Commit belegt; Begründung nicht festgehalten |

## Neu gelernt
– (für diesen Zeitraum nicht festgehalten; der Meilenstein ist nachträglich aus der Git-Historie entstanden)

## Tests & Verifikation
| Test | Ergebnis |
|---|---|
| `./mvnw clean verify` auf Java 25 (laut Commit `f882b3f`) | 103 Tests bestanden, Docker-Image baut und läuft auf `25-jre` |
| CI auf `f882b3f` / Merge `fb85f5d` (GitHub Actions) | `backend-verify-ci`, `docker-ci`, `docker-publish`, `web-e2e-ci` grün |
| CI auf `7fcda8e` (Ende des Meilensteins) | `backend-verify-ci`, `docker-ci`, `web-build-ci`, `web-e2e-ci` rot, `web-unit-ci` grün: die Backend-Tests waren nach der Paginierung noch nicht angepasst (laut Commit `a259222`), behoben in Meilenstein 08 |

## Probleme & Lösungen
- **Problem:** Nach der Paginierung kompilierten die Backend-Tests nicht mehr (alte `getEvents`-Signatur). → **Lösung:** bewusst als „work in progress“ committet; Tests in Meilenstein 08 nachgezogen (`2ff5d22`).
- Weitere Probleme sind für diesen Zeitraum nicht festgehalten.

## Eingesetzte Hilfsmittel
Alle fünf eigenen Commits tragen Claude (Claude Code) als Co-Autor; wie groß der Anteil je Commit war, ist nicht erfasst. Laut User habe ich KI nur teilweise eingesetzt. (Hinweis: FH-Richtlinien zur Kennzeichnung beachten.)

## Offene Punkte / Nächste Schritte
- Backend-Tests für die paginierte Liste und den Kategorie-Filter (erledigt in M08).
- E2E-Tests auf die neue Route umstellen (erledigt in M08).
- Event-Detailseite.

## Commit-Verlauf
| Datum | Hash | Nachricht |
|---|---|---|
| 2026-10-17 15:48 | `f882b3f` | chore: upgrade to Java 25 (LTS) |
| 2026-10-17 15:48 | `fb85f5d` | Merge branch 'chore/jdk-25' |
| 2026-10-17 20:02 | `f7afcca` | feat(web): app shell with sidebar, header search and /event route |
| 2026-10-17 22:51 | `08ff7b1` | feat(web): event list from the API with featured event and sale countdown |
| 2026-10-18 12:31 | `a259222` | feat: event categories and paginated event list |
| 2026-10-18 20:55 | `7fcda8e` | refactor(web): feature folders, event list hook and category filter |
