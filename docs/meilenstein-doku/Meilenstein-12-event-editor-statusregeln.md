# Meilenstein 12 – Overex: Event-Editor, Statusregeln und „Meine Events“

| Feld | Wert |
|---|---|
| Projekt | Overex |
| Zeitraum | 2026-11-07 – 2026-11-23 |
| Dokumentiert am | 2026-11-23 19:30 CET |
| Repository / Service | `overex`: Backend `apps/backend`, Web-Client `apps/web`, Bruno-Collection, Doku |
| Thema | Events erstellen, bearbeiten, löschen; Statusregeln; Optimistic Locking; „Meine Events“; Besitzerprüfung; einheitlicher Fehlerbody; Tests auf allen Ebenen |
| Commits | 29 und 1 PR-Merge (`80a84ff`..`b943b74`); Bereich `5ee44be..b943b74` |
| Zeitaufwand | nicht erfasst |
| Bezug | Projektarbeit (5. Semester); Abschluss von Phase 1b, Grundlage für die Aufteilung in Services |

## Ziel
Ein Admin soll Events im Web-Client erstellen, bearbeiten, veröffentlichen, absagen und löschen können. Welche Statuswechsel erlaubt sind, soll nur das Backend entscheiden; das Frontend zeigt diese Entscheidung an. Gleichzeitige Änderungen dürfen sich nicht gegenseitig überschreiben, und fremde Events müssen geschützt sein. Am Ende soll Phase 1b getestet in `main` liegen, bevor das Backend in Services geteilt wird.

## Ausgangslage
- Im Web-Client konnte man Events nur ansehen, nicht erstellen oder bearbeiten.
- Ein neues Event war immer `DRAFT`; der Status ließ sich nach dem Anlegen nicht ändern.
- Die öffentliche Liste lieferte alle Events, auch Entwürfe. Jeder Admin konnte jedes Event ändern und löschen.
- Die Event-Endpoints gaben die Entity zurück. Es gab keinen Schutz vor gleichzeitigen Änderungen.

## Umsetzung (chronologisch)
1. **2026-11-07** – Doku M07 bis M11 und Übersicht (`80a84ff`). Create-Event-Seite mit react-hook-form und zod, Live-Vorschau, Kartenansicht auf dem Handy, größere UI-Maße (`b565602`).
2. **2026-11-08** – Formular sendet ab, als Entwurf oder veröffentlicht; Feldfehler des Backends stehen unter dem Feld (`37fcb0e`). Auth- und User-DTOs als Records (`72d6de7`), `UserController` mit `UserService` (`18f0335`). Gemeinsamer Hook und Filterleiste für beide Listen (`58280b5`). `/me` wird erst mit Access-Token geladen (`c621218`).
3. **2026-11-10** – Ein Fehlerbody `ApiError` für alle Fehler, Locking-Konflikte werden 409 (`39b1dd7`). Caching-Strategien notiert, die Hooks bleiben vorerst ungenutzt (`8f2d17a`). Persönliche Notizen zu den Statusregeln ignoriert (`68bb540`).
4. **2026-11-14** – `EventStatusRules`, `@Version`, Response-DTOs und `MyEventsController` (`1d72c94`). Seite „Meine Events“ mit Status-Tabs (`a03a3ff`). Ein Editor für Erstellen und Bearbeiten mit Status-Auswahl (`c8a0306`).
5. **2026-11-21** – Tab „Beendet“ und Zähler pro Tab mit `MyEventsFilter` und `MyEventCriteria` (`56abc8a`).
6. **2026-11-22** – Fehler behoben: PATCH gab die alte Version zurück (`ef72d99`). Statusregeln mit Regeltabelle dokumentiert (`45f5779`). Backend-Tests auf DTOs, Statusregeln und Fehlerbody umgestellt (`2c75897`), Slice- und Integrationstests für „Meine Events“ (`3debfed`), AAA-Kommentare (`159097d`).
7. **2026-11-23** – `EventStatusRulesTest` mit Grenzfällen (`6be0666`). Besitzerprüfung, verborgene Entwürfe, Status beim Erstellen (`3a5b2a7`). Datumsfunktionen in eigener Datei (`c438503`). Löschen und Seiten-Reset beim Tab-Wechsel (`75083ee`). Frontend-Unit-Tests (`c67fd38`), E2E-Tests (`b01dcd2`), Bruno neu geordnet, jede Route abgedeckt (`e78bbbf`).
8. **2026-11-23** – Rückfrage vor dem Löschen (`e4b4a18`), Tests für die Besitzerprüfung (`fdea445`), Healthcheck für die Test-Datenbank (`5515dee`). PR #29 `feature/event` → `main` gemergt (`b943b74`).

## Geänderte Dateien
251 Dateien (ohne Meilenstein-Doku), 9900 Zeilen neu, 1924 gelöscht. Auswahl der wichtigsten:

| Datei | Art | Zweck |
|---|---|---|
| `event/EventStatusRules.java` | neu | entscheidet jeden Statuswechsel und liefert den Grund |
| `event/EventService.java` | geändert | Besitzerprüfung, Versionsvergleich, Regeln beim Erstellen und Ändern, öffentliche Liste |
| `event/EventRepository.java` | geändert | Queries für die öffentliche Liste und `findMyEvents` |
| `event/MyEventsController.java`, `MyEventsFilter.java`, `MyEventCriteria.java` | neu | `GET /me/events` und `/me/events/counts` |
| `event/Event.java` | geändert | `@Version` |
| `event/dto/responses/*` | neu | Response-DTOs statt Entity |
| `common/exception/ApiError.java`, `GlobalExceptionHandler.java` | geändert | ein Fehlerbody; `ErrorResponse` und `ErrorResponseFactory` gelöscht |
| `user/UserController.java`, `UserService.java` | neu | ersetzen `MeController` |
| `components/EventEditor.tsx`, `hooks/use-event-editor.ts` | neu | Editor für Erstellen und Bearbeiten |
| `app/(shell)/my-events/**`, `hooks/use-my-event-list.ts` | neu | Seite „Meine Events“ |
| `components/delete-event-dialog.tsx` | neu | Rückfrage vor dem Löschen |
| `features/event/date-time.ts`, `event-validation.ts` | neu | Datum und Uhrzeit umwandeln, zod-Schema |
| `EventStatusRulesTest`, `MyEventsControllerTest`, `MyEventsIntegrationTest` | neu | Backend-Tests |
| `use-event-editor.test.ts`, `use-my-event-list.test.ts`, `date-time.test.ts`, `my-events.spec.ts` | neu | Frontend-Unit- und E2E-Tests |
| `bruno/` | geändert | ein Ordner pro Route |
| `docker-compose.test.yml`, `apps/web/package.json` | geändert | Healthcheck und `up --wait`; Frontend-Pakete |

Die Backend-Pfade liegen unter `apps/backend/src/main/java/com/overex/backend/`, die Frontend-Pfade unter `apps/web/` (`hooks/` und `delete-event-dialog.tsx` unter `features/event/`).

## Entscheidungen
| Entscheidung | Verworfene Alternativen | Begründung |
|---|---|---|
| Das Backend liefert `statusOptions` samt Grund | Regeln im Frontend nachbauen | Anzeige und Prüfung nutzen dieselbe Methode und können nicht auseinanderlaufen |
| `switch` ohne `default` | `default`-Zweig | ein neuer Status kompiliert erst, wenn er Regeln hat |
| „Beendet“ wird berechnet (`PUBLISHED` und Start vorbei) | eigener gespeicherter Status | ergibt sich aus der Zeit |
| Version von Hand mit dem DTO vergleichen | nur `@Version` | `findById` lädt die aktuelle Version, Hibernates eigene Prüfung würde eine veraltete Client-Version nicht bemerken |
| Zähler mit derselben Query wie die Liste | eigene Count-Query | Tab und Zähler können sich nicht widersprechen |
| Fremder Entwurf: 404 | 403 | 403 würde verraten, dass es ihn gibt |
| Besitzerprüfung vor allen anderen Prüfungen | nach der Versionsprüfung | Fremde erfahren nichts über Version oder Status |
| `@Query` für „Meine Events“ | eine Methode je Filterkombination | optionale Filter gehen mit Methodennamen nicht |
| Response-DTOs | Entity zurückgeben | `createdBy` und damit der User verlässt das Backend nicht |
| Caching-Hooks vorerst nicht nutzen | sofort einbauen | die Bearbeiten-Seite braucht frische Daten mit `statusOptions` |

## Neu gelernt
- **Optimistic Locking**: Jede Änderung erhöht die Version. Der Client schickt die geladene Version mit; passt sie nicht mehr, antwortet das Backend mit 409.
- **`save` und `saveAndFlush`**: `save` schreibt bei einer geladenen Entity nicht sofort. Erst beim Flush erhöht Hibernate die Version.
- **Methodenname oder `@Query`**: Feste, einfache Bedingungen gehen über den Methodennamen. Für optionale Filter, Klammern und Berechnungen nehme ich `@Query`. `findByStatus(null)` sucht `IS NULL` und lässt den Filter nicht weg.
- **`Page` und `Pageable`**: gehören zu Spring Data, nicht zu JPA. Spring Data fragt die Seite und bei Bedarf die Gesamtzahl ab.
- **Docker-Healthcheck**: „gestartet“ heißt nicht „bereit“. `up --wait` wartet, bis der Healthcheck erfolgreich ist.

## Tests & Verifikation
| Test | Ergebnis |
|---|---|
| `backend-verify-ci` auf `5515dee` | 135 Tests, 0 Fehler |
| `web-checks` auf `5515dee`: Unit | 127 Tests in 13 Dateien grün |
| `web-checks` auf `5515dee`: E2E | 47 Tests grün |
| `workflow-lint`, `docker-ci` auf `5515dee` | grün |
| Besitzerprüfung: `ensureOwner`-Aufrufe testweise entfernt | genau die zwei neuen Tests rot (Gegenprobe) |
| Healthcheck: 3× `npm run e2e:db-up`, danach sofort `psql` | 3 von 3 erfolgreich |
| Lösch-Dialog | nur mit einem temporären Komponententest geprüft, nicht im Browser |

## Probleme & Lösungen
- **Problem:** Nach einem PATCH bekam der Client die alte Version, das nächste Speichern gab fälschlich 409. → **Lösung:** `saveAndFlush` vor dem Mapping.
- **Problem:** Die Shell rief `/me` vor dem Token-Refresh auf und bekam 401. → **Lösung:** Query wartet auf das Token.
- **Problem:** Ein Klick im Lösch-Dialog hätte den Link der Zeile ausgelöst, weil React-Events durch Portale aufsteigen. → **Lösung:** `preventDefault` auf einem umschließenden Element.
- **Problem:** Die E2E-Seed-Daten hatten keinen Besitzer (`created_by` ist Pflicht). → **Lösung:** Seed ergänzt.
- **Problem:** Lokale E2E-Läufe brachen teils beim Start ab. Zuerst vermutet: Datenbank nicht bereit. → **Ursache laut Log:** `next build` mit Next 16.4.3 bricht mit `SecurityFatalError` ab, der nächste Versuch läuft durch. **Offen**; der Healthcheck behebt das nicht.

## Eingesetzte Hilfsmittel
Claude (Claude Code) hat viele Konzepte erklärt. Am 23.11. hat Claude die Besitzerprüfung, verborgene Entwürfe, den Status beim Erstellen, das Löschen mit Dialog, den Healthcheck, die Tests, die Bruno-Collection, Commits und PR #29 samt Merge umgesetzt sowie diese Doku entworfen. Ich habe Anforderungen und Entscheidungen vorgegeben. Der Anteil an den früheren Commits ist in dieser Session nicht erfasst. (Hinweis: FH-Richtlinien zur Kennzeichnung beachten.)

## Offene Punkte / Nächste Schritte
- Fehlende Tests: fremder Entwurf (404), `CANCELED` und falscher Verkaufsstart beim Erstellen, Lösch-Dialog.
- Next.js aktualisieren (Dependabot-PR #25) und prüfen, ob der Build-Abbruch verschwindet.
- GitHub meldet 53 bekannte Schwachstellen auf `main` (Stand 23.11., vor dem Merge).
- Push nach GitLab nachholen (Server antwortet mit 503).
- Docker Mastery ab Abschnitt 4, danach Aufteilung in Auth- und Event-Service.

## Commit-Verlauf
| Datum | Hash | Nachricht |
|---|---|---|
| 2026-11-07 15:13 | `80a84ff` | docs: add milestone documentation 07-11 and overview |
| 2026-11-07 22:28 | `b565602` | feat(web): create-event form, card view toggle and modern UI sizes |
| 2026-11-08 00:22 | `37fcb0e` | feat(web): submit create-event form as draft or published |
| 2026-11-08 20:40 | `72d6de7` | refactor(backend): auth and user DTOs as records |
| 2026-11-08 21:05 | `18f0335` | refactor(backend): UserController with UserService, drop /admin/ping |
| 2026-11-08 21:40 | `58280b5` | refactor(web): shared list params hook and filter bar for event lists |
| 2026-11-08 22:20 | `c621218` | fix(web): load /me only once an access token exists |
| 2026-11-10 20:45 | `39b1dd7` | refactor: one error body ApiError for every error response |
| 2026-11-10 21:30 | `8f2d17a` | docs: caching strategies; seed and prefetch hooks kept unused |
| 2026-11-10 22:05 | `68bb540` | chore: ignore personal status-rule notes |
| 2026-11-14 22:30 | `1d72c94` | feat(backend): event status rules, optimistic locking and response DTOs |
| 2026-11-14 22:50 | `a03a3ff` | feat(web): "Meine Events" with status tabs and counts |
| 2026-11-14 23:20 | `c8a0306` | feat(web): one event editor for create and edit with status select |
| 2026-11-21 20:30 | `56abc8a` | feat: "Beendet" tab and per-tab counts for "Meine Events" |
| 2026-11-22 03:16 | `ef72d99` | fix(backend): return the new version after an event update |
| 2026-11-22 03:16 | `45f5779` | docs(backend): explain the status rules with a rule table and examples |
| 2026-11-22 03:16 | `2c75897` | test(backend): event tests on response DTOs, status rules and error body |
| 2026-11-22 03:16 | `3debfed` | test(backend): slice and integration tests for "Meine Events" |
| 2026-11-22 03:16 | `159097d` | test(backend): Arrange / Act / Assert comments in every test |
| 2026-11-23 15:54 | `6be0666` | test(backend): EventStatusRulesTest with boundary cases, unused code removed |
| 2026-11-23 15:54 | `3a5b2a7` | feat(backend): owner check, hidden drafts and status on create |
| 2026-11-23 15:54 | `c438503` | refactor(web): date helpers in their own file, unused code removed |
| 2026-11-23 15:54 | `75083ee` | feat(web): delete events and reset the page on tab change in "Meine Events" |
| 2026-11-23 15:54 | `c67fd38` | test(web): unit tests for the event editor, my events and date helpers |
| 2026-11-23 15:54 | `b01dcd2` | test(web): e2e tests for "Meine Events" and the event editor |
| 2026-11-23 15:54 | `e78bbbf` | test(bruno): one folder per route, every route covered |
| 2026-11-23 18:37 | `e4b4a18` | feat(web): ask before an event is deleted |
| 2026-11-23 18:37 | `fdea445` | test(backend): only the owner may update or delete an event |
| 2026-11-23 18:37 | `5515dee` | test(e2e): wait until the test database is healthy |
| 2026-11-23 19:29 | `b943b74` | Merge pull request #29 from Umex10/feature/event |
