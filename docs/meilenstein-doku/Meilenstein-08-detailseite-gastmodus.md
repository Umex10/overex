# Meilenstein 08 – Overex: Kategorie-Tests, Event-Detailseite, Breadcrumbs und Gastmodus

| Feld | Wert |
|---|---|
| Projekt | Overex |
| Zeitraum | 2026-10-19 – 2026-10-25 |
| Dokumentiert am | 2026-11-03 19:13 CET (nachträglich aus der Git-Historie) |
| Repository / Service | `overex`: Backend `apps/backend`, Web-Client `apps/web`, Bruno-Collection `bruno/` |
| Thema | Backend-Tests für Paginierung und Kategorien; `CategoryService`; Event-Detailseite `/events/[id]`; Breadcrumbs; Toasts; Markenfarbe Pink; Gastmodus (Events ohne Login ansehen) |
| Commits | 11 (`2ff5d22`..`54f61a9`, Bereich `7fcda8e..54f61a9`) |
| Zeitaufwand | ca. 9,5 h: 19.10 ca. 3 h, 24.10 ca. 1,5 h, 25.10 ca. 4 h (laut User); 20.10 ca. 1 h (geschätzt aus drei kleinen Commits, nicht notiert) |
| Bezug | Projektarbeit (5. Semester) |

## Ziel
Die in Meilenstein 07 offen gebliebenen Backend-Tests sollten nachgezogen werden. Danach sollte das Event-Feature im Frontend vollständig benutzbar werden: eine Detailseite pro Event, Navigation über Breadcrumbs und vor allem ein Gastmodus, damit Besucher Events ohne Konto ansehen können, wie bei einer echten Ticketing-Plattform.

## Ausgangslage
- Die Backend-Tests kompilierten nach der Paginierung nicht mehr; die CI war auf `7fcda8e` rot.
- Die Event-Seite hieß `/event` und es gab keine Detailseite.
- Jede Seite unter der App-Shell verlangte einen Login.
- Formatierung von Datum, Uhrzeit und Preis war in mehreren Komponenten doppelt.

## Umsetzung (chronologisch)
1. **2026-10-19** – Backend-Tests für `GET /event` mit Default-Pageable, mit und ohne `categoryId` auf allen drei Ebenen; geprüft werden Sortierung nach `startsAt` und Seiten-Metadaten (`size`, `number`, `totalElements`, `totalPages`). Der Integrationstest legt zwei Kategorien an, damit der Filter die fremden Events ausschließen muss (`2ff5d22`).
2. **2026-10-20** – E2E-Specs auf die Event-Seite umgestellt (`8cbe2ab`); `getCategory` von `EventService` nach `CategoryService` verschoben, `CategoryTestDataFactory` und Kategorie-Tests (Unit, Slice, Integration) (`7ba532f`).
3. **2026-10-20** – CI-Fix: `CategoryIntegrationTest` schreibt wie die anderen Integrationstests eigene RSA-Keys; `FeaturedEventCard` ohne Seiteneffekt („pure“) (`1e5e7aa`).
4. **2026-10-24** – Detailseite `/events/[id]` mit Query `getEvent`; Breadcrumbs in der Kopfzeile über einen Redux-Slice `route-context`; Route `/event` → `/events` umbenannt, Links, Redirects und Tests angepasst (`0688063`).
5. **2026-10-25** – sechs Commits, gemeinsam um 18:32 committet:
   - `BreadcrumbsContainer` und Hook `useBreadcrumbs`: Seiten setzen ihre Crumbs aus dem Pfad (`99d0fb4`).
   - `SearchParamToast`: Toast bei `?param=value` in der URL; auf der Sign-in-Seite ersetzt er die Box „Sitzung abgelaufen“ (`3f35834`).
   - Markenfarbe von Violett auf Pink (`--primary`, `--ring`, Charts, Sidebar, Konfetti) (`6fcd7cf`).
   - Gemeinsame Komponenten `DateFormat`, `TimeFormat`, `PriceFormat`, `EventMetaItem` statt doppelter Formatierer; Detailseite mit Icons und „Mehr anzeigen“; kompakte Event-Karte auf dem Handy (`187b69b`).
   - Mobile Kopfzeile mit exakt zentriertem Logo (Grid `1fr/auto/1fr`); Sidebar schließt nach Navigation auf dem Handy; Fußbereich zeigt für Gäste den Sign-in, sonst den Sign-out (`0e6d6d8`).
   - **Gastmodus:** im Backend sind `GET /event/**` und `/category/**` öffentlich, Schreibzugriffe bleiben geschützt; `proxy.ts` lässt Besucher ohne Refresh-Cookie auf `/events`; `getAccessTkAction` liefert `null` statt Fehler, wenn kein Cookie da ist; der `AuthProvider` löscht das Cookie nur noch bei 401 und bietet „erneut versuchen“, wenn das Backend nicht erreichbar ist; Bruno-Requests für die Gast-Endpoints (`54f61a9`).

## Geänderte Dateien
54 Dateien, +2244/−735 Zeilen.

| Datei | Art | Zweck |
|---|---|---|
| `.../auth/security/config/SecurityConfig.java` | geändert | `GET` auf Events und Kategorien öffentlich |
| `.../event/CategoryService.java`, `EventService.java`, `Event.java` | geändert | Kategorie-Lookup im `CategoryService` |
| `.../event/{unit,slice,integration}/Category*Test.java`, `utils/CategoryTestDataFactory.java` | neu | Kategorie-Tests |
| `.../event/{unit,slice,integration}/Event*Test.java`, `EventTestDataFactory.java` | geändert | Paginierung, Filter, Seiten-Metadaten |
| `apps/web/app/(shell)/events/[id]/page.tsx` | neu | Detailseite |
| `apps/web/app/(shell)/events/{layout,page}.tsx` | umbenannt | vorher `/event` |
| `apps/web/components/BreadcrumbsContainer.tsx`, `hooks/use-breadcrumbs.ts`, `redux/route-context-slice.ts` | neu | Breadcrumbs |
| `apps/web/components/search-param-toast.tsx` | neu | Toasts aus Query-Parametern |
| `apps/web/features/event/components/{date-format,time-format,price-format,event-meta-item,event-details}.tsx` | neu | gemeinsame Anzeige-Komponenten |
| `apps/web/features/auth/**`, `proxy.ts` | geändert | Gastmodus, robuster `AuthProvider` |
| `apps/web/app/globals.css` | geändert | Pink als Markenfarbe |
| `apps/web/__tests__/e2e/**`, `unit/proxy.test.ts` | geändert | neue Routen, Gastzugang |
| `bruno/Event/…`, `bruno/Category/…` | neu/geändert | Gast-Requests |

## Entscheidungen
| Entscheidung | Verworfene Alternativen | Begründung |
|---|---|---|
| Lesen von Events und Kategorien ohne Login, Schreiben geschützt | alles hinter Login (alter Stand) | Besucher sollen das Angebot sehen, bevor sie sich registrieren |
| `AuthProvider` löscht das Cookie nur bei 401 | bei jedem Fehler abmelden | laut Commit `54f61a9`: ein nicht erreichbares Backend ist kein Grund, die Sitzung zu verlieren; stattdessen „erneut versuchen“ |
| Route `/events` (Plural) | `/event` | Liste mehrerer Events; die REST-Routen folgen in M09 |
| Gemeinsame Formatierungs-Komponenten | Formatierer in jeder Karte | Doppelter Code in Karten und Detailseite |

## Neu gelernt
– (für diesen Zeitraum nicht festgehalten; der Meilenstein ist nachträglich aus der Git-Historie entstanden)

## Tests & Verifikation
| Test | Ergebnis |
|---|---|
| CI auf `2ff5d22` | `backend-verify-ci` und `docker-ci` grün (Backend-Tests wieder lauffähig), `web-e2e-ci` rot (E2E-Specs zeigten noch auf die alte Route) |
| CI auf `7ba532f` | `backend-verify-ci` rot (neuer `CategoryIntegrationTest` ohne eigene Keys), `web-build-ci` rot |
| CI auf `1e5e7aa` | alle fünf Workflows grün |
| CI auf `54f61a9` (Ende des Meilensteins) | `backend-verify-ci`, `docker-ci`, `web-build-ci`, `web-unit-ci` grün; `web-e2e-ci` rot. Die Ursache ist im Log nicht mehr abrufbar; ab `29d1717` (M10) wieder grün |

## Probleme & Lösungen
- **Problem:** `CategoryIntegrationTest` brach in der CI ab, lokal nicht. → **Lösung:** wie bei `AuthIntegrationTest` und `EventIntegrationTest` eigene Keys per `@TempDir` und `@DynamicPropertySource` (`1e5e7aa`), dasselbe Muster wie in M06.
- **Problem:** E2E-Specs zeigten nach der Umbenennung der Route ins Leere. → **Lösung:** Specs auf die Event-Seite umgestellt (`8cbe2ab`).

## Eingesetzte Hilfsmittel
Zehn der elf Commits tragen Claude (Claude Code) als Co-Autor, `1e5e7aa` nicht; der Anteil je Commit ist nicht erfasst. Laut User habe ich KI nur teilweise eingesetzt. (Hinweis: FH-Richtlinien zur Kennzeichnung beachten.)

## Offene Punkte / Nächste Schritte
- `web-e2e-ci` wieder grün bekommen (erledigt in M10).
- REST-Routen ebenfalls in den Plural (erledigt in M09).
- Frontend-Unit-Tests für Hooks und Logik (M09).

## Commit-Verlauf
| Datum | Hash | Nachricht |
|---|---|---|
| 2026-10-19 22:20 | `2ff5d22` | test(backend): cover paginated event list and category filter |
| 2026-10-20 16:02 | `8cbe2ab` | test(web): point e2e specs at the event page |
| 2026-10-20 16:02 | `7ba532f` | feat(backend): category lookup in CategoryService with tests |
| 2026-10-20 16:13 | `1e5e7aa` | fix: CI keys for CategoryIntegrationTest, pure featured event card |
| 2026-10-24 21:03 | `0688063` | feat(web): event detail page with breadcrumbs, rename route to /events |
| 2026-10-25 18:32 | `99d0fb4` | feat(web): breadcrumbs container and useBreadcrumbs hook |
| 2026-10-25 18:32 | `3f35834` | feat(web): toasts for redirect query params |
| 2026-10-25 18:32 | `6fcd7cf` | style(web): switch brand colour from violet to pink |
| 2026-10-25 18:32 | `187b69b` | feat(web): event detail page and shared event info components |
| 2026-10-25 18:32 | `0e6d6d8` | feat(web): centred mobile header and auth-aware sidebar |
| 2026-10-25 18:32 | `54f61a9` | feat: guest mode for browsing events |
