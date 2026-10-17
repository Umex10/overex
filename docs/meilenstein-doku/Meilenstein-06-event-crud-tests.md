# Meilenstein 06 – Overex: Datenmodell und Event-Feature mit Tests und Bruno-Collection

| Feld | Wert |
|---|---|
| Projekt | Overex |
| Zeitraum | 2026-10-13 – 2026-10-17 |
| Dokumentiert am | 2026-10-17 15:36 CEST |
| Repository / Service | `overex` (github.com/Umex10/overex): Monolith-Backend `apps/backend`, Bruno-Collection `bruno/`, Doku `docs/` |
| Thema | ER-Datenmodell, `Event`-Entity mit CRUD-Endpoints (inkl. Liste), Unit-/Slice-/Integrationstests, Teststruktur nach Feature, Bruno-Collection mit automatisiertem Lauf, 400 statt 500 bei ungültiger ID |
| Commits | 11 (`7474533`..`a81e02d`, Bereich `949e762..a81e02d`), Branch `feature/event`, per `--no-ff` nach `main` gemergt (`7474b38`), danach ein Commit direkt auf `main` (`a81e02d`) |
| Zeitaufwand | nicht erfasst |
| Bezug | Projektarbeit (5. Semester) und Grundlage für die Bachelorarbeit (6. Semester); erstes fachliches Feature des Monolithen nach der Übernahme von AuthKit |

## Ziel
Nach dem Auth-Teil sollte das erste fachliche Feature von Overex entstehen: Events, für die später Ticketkategorien, Reservierungen und Bestellungen angelegt werden. Dafür wollte ich zuerst das Datenmodell festlegen, dann die `Event`-Entity mit allen CRUD-Endpoints bauen und das Ganze auf allen drei Testebenen (Unit, Slice, Integration) sowie mit einer Bruno-Collection gegen die laufende App absichern.

## Ausgangslage
- Das Repository war gerade von AuthKit auf Overex umbenannt (`747d5c0`, nicht Teil dieses Meilensteins). Im Backend gab es nur Auth (`/auth/*`, JWKS) und `/me`.
- Es gab kein Datenmodell für die fachlichen Tabellen.
- Die Tests waren nach Testart sortiert (`unit/`, `slice/`, `integration/`, `utils/`), alle Features gemischt; die Hilfsklassen hießen `TestDataFactory`, `TestKeys` und `TkFactory`.
- `BackendApplicationTests` enthielt die kompletten Auth-Integrationstests, nicht den Smoke-Test, den der Name verspricht.

## Umsetzung (chronologisch)
1. **2026-10-13** – ER-Datenmodell in `docs/DATA_MODEL.md` mit Mermaid-Quelle und PNG/SVG: Monolith (Schema `core`: `users`, `events`, `ticket_categories`, `orders`, `tickets`) und Reservierungs-Service (Schema `reservation`), Verweise über die Service-Grenze per gRPC statt Fremdschlüssel (`7474533`, Merge `5e28f17`).
2. **2026-10-13** – Event-CRUD: `Event`-Entity mit `Status` (`DRAFT`, `PUBLISHED`, `CANCELED`), Records `CreateEventDto`/`UpdateEventDto` mit Bean-Validation, `EventMapper` (MapStruct), `EventService`, `EventController`. Beim Update drei Geschäftsregeln als 400 mit Feldfehler (`OwnException`): Event hat schon begonnen, neue Startzeit in der Vergangenheit, Verkaufsstart nicht vor Eventbeginn. Erste Bruno-Collection für Auth und Event; den `slug` habe ich aus dem Datenmodell gestrichen (`ee31f25`).
3. **2026-10-13** – `main` in `feature/event` gemergt (`4122bc2`); README ins Englische übersetzt (`a3ee76d`).
4. **2026-10-14** – Teststruktur nach Feature umgebaut (`auth/`, `event/`, `user/`, jeweils `unit/`, `slice/`, `integration/`, `utils/`); `TestDataFactory` → `AuthTestDataFactory`; neu `EventTestDataFactory`, `EventControllerTest` und `EventServiceTest`, der jede Regel in `updateEvent` mit einem eigenen Payload auslöst (`0467fbd`).
5. **2026-10-15** – Endpoint `GET /event` (Liste); `EventIntegrationTest` mit echtem Sign-up und Token; Auth-Flow-Tests in `AuthIntegrationTest` umbenannt, `BackendApplicationTests` ist nur noch `contextLoads()` im Root-Paket; `TkFactory` kann sich registrieren und den Access-Token zurückgeben (`bf6b3ee`).
6. **2026-10-15** – CI-Fehler behoben: `EventIntegrationTest` schreibt wie `AuthIntegrationTest` eigene RSA-Keys in einen temporären Ordner (`f8792bf`).
7. **2026-10-17** – Hilfsklassen getrennt: `AuthTestClient` (Requests: Sign-up, Sign-in, Token auslesen, `authenticatedAs`) und `AuthTestDataFactory` (nur Daten); `TestKeys` → `TestRsaKeys`; `AuthIntegrationTest` nutzt `AuthTestClient` statt eigener Hilfsmethoden. Event-Tests vereinheitlicht (ein gemeinsames Fixture pro Klasse, IDs nur aus dem Fixture, URI-Templates, keine ungenutzten Imports, Javadoc). Neue Tests: Liste (Unit und Slice), ungültiger Create-Payload, ungültiger Access-Token, Delete prüft danach 404, `null`-E-Mail in `normalizeEmail` (`53b08c5`).
8. **2026-10-17** – Merge `feature/event` → `main` (`7474b38`).
9. **2026-10-17** – Bruno-Lauf fand einen Fehler: `GET /event/abc` lieferte 500. `GlobalExceptionHandler` behandelt jetzt `MethodArgumentTypeMismatchException` als 400 mit Feldfehler, abgesichert durch einen Slice-Test. Bruno-Collection ergänzt (401 ohne bzw. mit ungültigem Token, genauere Assertions) und mit `bruno/package.json` per `npm test` automatisiert (`a81e02d`).

## Geänderte Dateien
| Datei | Art | Zweck |
|---|---|---|
| `docs/DATA_MODEL.md`, `docs/diagrams/datenmodell.{mmd,png,svg}` | neu | ER-Modell von Monolith und Reservierungs-Service |
| `apps/backend/pom.xml` | geändert | MapStruct 1.6.3 mit `mapstruct-processor` und `lombok-mapstruct-binding` |
| `apps/backend/src/main/java/com/overex/backend/event/Event.java`, `Status.java` | neu | Entity `events` mit Status-Enum |
| `.../event/dto/requests/CreateEventDto.java`, `UpdateEventDto.java` | neu | Request-Records mit Bean-Validation (`@NotBlank`, `@Size`, `@URL`, `@Future` nur beim Create) |
| `.../event/EventMapper.java` | neu | MapStruct: DTO → Entity, Update auf bestehende Entity |
| `.../event/EventRepository.java`, `EventService.java`, `EventController.java` | neu | `GET /event`, `GET/PATCH/DELETE /event/{eventId}`, `POST /event`; Geschäftsregeln beim Update |
| `.../common/exception/GlobalExceptionHandler.java` | geändert | 400 statt 500 bei falschem Parametertyp (z. B. keine UUID) |
| `.../auth/config/RsaKeyFiles.java` | geändert | Javadoc-Verweis auf `TestRsaKeys` |
| `apps/backend/src/test/java/com/overex/backend/{auth,user}/**` | verschoben | Tests nach Feature und Testart (`git mv`, Historie erhalten) |
| `.../auth/integration/AuthIntegrationTest.java` | umbenannt | vorher `BackendApplicationTests`, nutzt `AuthTestClient` |
| `.../BackendApplicationTests.java` | neu | Smoke-Test `contextLoads()` |
| `.../auth/utils/AuthTestClient.java` | neu (aus `TkFactory`) | Auth-Requests für MockMvc-Tests |
| `.../auth/utils/AuthTestDataFactory.java`, `TestRsaKeys.java` | umbenannt | vorher `TestDataFactory`, `TestKeys` |
| `.../event/unit/EventServiceTest.java` | neu | 13 Tests, jede Regel in `updateEvent` einzeln |
| `.../event/slice/EventControllerTest.java` | neu | 12 Tests: alle Endpoints, Validierung, 404/400-Mapping |
| `.../event/integration/EventIntegrationTest.java` | neu | 9 Tests gegen die ganze App mit H2 und echtem Token |
| `.../event/utils/EventTestDataFactory.java` | neu | Fixtures, jeder Wert genau einmal als Konstante |
| `bruno/**` | neu | 22 Requests (Auth und Event, ok- und fail-Fälle) mit Assertions; `package.json` pinnt die Bruno-CLI |
| `docs/TESTING.md`, `docs/ARCHITECTURE.md` | geändert | Teststruktur nach Feature, Abschnitt zu Bruno, Verweis auf `AuthIntegrationTest` |
| `README.md` | geändert | Übersetzung ins Englische, Link zum Datenmodell |

## Entscheidungen
| Entscheidung | Verworfene Alternativen | Begründung |
|---|---|---|
| Tests nach Feature, darin nach Testart (`event/unit`, `event/slice`, …) | Tests nach Testart mit gemischten Features (alter Stand) | Ein Feature hat alles an einem Ort, gleiche Struktur wie in `main` |
| Testdaten (`AuthTestDataFactory`) und Requests (`AuthTestClient`) getrennt | Beides in einer Klasse (kurz umgesetzt, dann zurückgebaut) | Single Responsibility: Daten haben keine Abhängigkeiten, Requests brauchen MockMvc; der Klassenname soll sagen, was drin ist |
| Geschäftsregeln im `EventServiceTest` prüfen, im Controller-Test nur das Mapping auf 400 | Jede Regel im Controller-Test | Im Slice-Test ist der Service ein Mock, die `if`s laufen dort nie |
| Im Integrationstest das Ergebnis prüfen (Response, Datenbank) | `verify(eventMapper)` im Integrationstest | Dort ist nichts gemockt; prüft außerdem das echte Mapping |
| Ein Test für ungültigen Token, nicht pro Route | Jede Route mit ungültigem Token | Die Token-Prüfung läuft zentral in der Security-Filterkette; getestet wird jede Regel einmal |
| Gemeinsames `event`-Fixture als Feld in Unit- und Slice-Tests, lokal im Integrationstest | Mal Feld, mal lokal | Einheitlich; im Integrationstest erzeugt die Datenbank ID und Zeitstempel |
| `BackendApplicationTests` als reiner Smoke-Test, Auth-Flows in `AuthIntegrationTest` | Event-Integrationstests in `BackendApplicationTests` | Name passt zum Inhalt, Features bleiben getrennt |
| `@Import(JacksonConfig.class)` im Slice-Test | `@Import(ObjectMapper.class)` | Ein importierter `ObjectMapper` ist „nackt“ ohne `JavaTimeModule` |
| Kein `status`-Feld im `UpdateEventDto` | Status über das Update ändern (kurz begonnen, auf Wunsch zurückgesetzt) | Status-Änderungen sind noch nicht geplant |
| Bruno-CLI in `bruno/package.json` gepinnt | `npx` ohne Version, globale Installation | Gleiche Version lokal und später in der CI |

## Neu gelernt
- **`@MockitoBean` vs. `@Mock` vs. `@Autowired`**: `@Autowired` holt eine echte Bean aus dem Spring-Context, `@MockitoBean` legt einen Mock als Bean in den Context (damit Spring ihn z. B. in den Controller injizieren kann), `@Mock` ist ein reiner Mockito-Mock ohne Spring.
- **`@Import` im Test**: Man kann Konfigurationsklassen, aber auch normale Klassen importieren; eine normale Klasse wird dann einfach mit ihrem Konstruktor erzeugt, also ohne meine Konfiguration.
- **`jwt()` aus `spring-security-test`**: Legt ein fertiges `Authentication`-Objekt in den SecurityContext. Spring Security prüft bei der Autorisierung nur diesen Context, nicht ob ein Token verifiziert wurde.
- **`@AutoConfigureMockMvc`**: Erzeugt bei `@SpringBootTest` die `MockMvc`-Bean, die ich dann mit `@Autowired` hole; `@WebMvcTest` bringt sie schon mit.
- **`@Valid` und Feldfehler**: Bei Verstößen ruft Spring die Controller-Methode gar nicht auf, sondern wirft `MethodArgumentNotValidException`; der `GlobalExceptionHandler` baut daraus die Feldfehler. Geschäftsregeln laufen über `OwnException`, beide liefern dasselbe JSON-Format.
- **`jsonPath`**: Parst den Body in Java-Objekte (`List`, `Map`, `String` …); `$[*].title` liefert die Liste aller Titel. Deshalb vergleiche ich `UUID` und `Instant` als String. `hasItems` erlaubt zusätzliche Elemente, `containsInAnyOrder` verlangt genau dieselben.
- **`when(...)` bei `void`-Methoden**: Geht nicht, weil `when` einen Rückgabewert braucht; stattdessen `doThrow(...).when(mock).methode()`.
- **Checked Exceptions in Lambdas**: Ein Lambda in `forEach` darf keine checked Exception werfen, das `throws` der Testmethode hilft dort nicht; eine normale `for`-Schleife löst das.
- **Context-Caching**: Spring verwendet den Context für mehrere Testklassen nur wieder, wenn die Konfiguration gleich ist; unterschiedliche Annotationen oder `@DynamicPropertySource` starten jeweils einen neuen Context.
- **Single Responsibility**: Pakete nach Feature, Klassen nach Aufgabe.

## Tests & Verifikation
| Test | Ergebnis |
|---|---|
| `./mvnw clean test` (alle Backend-Tests, 2026-10-17) | 103 Tests, 0 Fehler: Auth 60, Event 34 (Unit 13, Slice 12, Integration 9), User 8, Smoke 1 |
| Bruno-Collection gegen die Version mit 400-Fix (lokal auf Port 8081, 2026-10-17) | 22 von 22 Requests, 41 von 41 Assertions bestanden |
| Bruno-Collection gegen die laufende App ohne Fix (Port 8080) | 19 von 20 Requests: `GET /event/abc` lieferte 500 statt 400 → behoben in `a81e02d` |
| CI `backend-verify-ci` auf `feature/event` vor `f8792bf` | fehlgeschlagen (laut User): Spring-Context von `EventIntegrationTest` startete ohne Keys nicht |
| CI nach `f8792bf` bzw. auf `main` | nicht in dieser Session geprüft |

## Probleme & Lösungen
- **Problem:** `Instant` ließ sich im Slice-Test nicht serialisieren. → **Lösung:** `@Import(JacksonConfig.class)` statt `@Import(ObjectMapper.class)`; der importierte `ObjectMapper` hatte kein `JavaTimeModule`.
- **Problem:** Ein Not-Found-Test war grün, obwohl die Exception nie geworfen wurde (`doThrow(...).deleteEvent(null)` passte nicht zur echten UUID). → **Lösung:** `any()` als Matcher und 404 mit Fehler-JSON erwarten.
- **Problem:** Integrationstests riefen Endpoints mit der festen Fixture-ID auf, die Datenbank vergibt aber eigene IDs. → **Lösung:** `createEvent(...)` gibt das gespeicherte Event aus der Response zurück.
- **Problem:** CI brach ab (`ApplicationContext failure threshold exceeded`), lokal lief alles. → **Lösung:** Lokal lagen Keys im git-ignorierten `keys/`; der Test schreibt jetzt eigene Keys per `@TempDir` und `@DynamicPropertySource`.
- **Problem:** `GET /event/abc` lieferte 500. → **Lösung:** Handler für `MethodArgumentTypeMismatchException` → 400.
- **Problem:** VS Code markierte jede Lombok-Konstruktor-Annotation mit „Flexible Constructor Bodies … source level 25“ und schrieb beim Kompilieren kaputte Klassen nach `target/classes` (`ClassFormatError` bei `User`, `VerifyError` bei `ApiError`). → **Lösung (vorläufig):** `./mvnw clean …` baut alles mit `javac` neu; Maven und CI sind nicht betroffen. Dauerhafte Lösung offen (siehe unten).
- **Problem:** Ungespeicherte Editor-Puffer und Dateiverschiebungen während offener Tabs erzeugten doppelte Methoden bzw. eine doppelte Testdatei. → **Lösung:** Datei aus dem Editor neu laden („Revert File“), veraltete Kopie gelöscht.

## Eingesetzte Hilfsmittel
Claude (Claude Code) hat mich in diesem Meilenstein unterstützt: Erklärungen zu Spring-Test, Mockito, JsonPath und Lombok; Boilerplate in den Tests (z. B. `jsonPath`-Prüfungen, Fixtures in `EventTestDataFactory`, `EventServiceTest`, Update- und Not-Found-Tests); das Umbauen der Teststruktur und Umbenennen der Hilfsklassen; den 400-Handler; die Bruno-Ergänzungen und deren automatisierten Lauf; Javadoc, Commits und diese Doku. Den Großteil von `EventIntegrationTest` und mehrere Tests im Controller- und Service-Test habe ich in dieser Session selbst geschrieben; die Entscheidungen zur Struktur habe ich getroffen bzw. bestätigt. Der Commit `ee31f25` (Entity, Service, Controller, erste Bruno-Collection) trägt Claude als Co-Autor; wie groß dieser Anteil war, ist nicht erfasst. Das Datenmodell (`7474533`) ist ohne KI-Co-Autor committet. (Hinweis: FH-Richtlinien zur Kennzeichnung beachten.)

## Offene Punkte / Nächste Schritte
- VS-Code-Fehlalarm „Flexible Constructor Bodies“ dauerhaft lösen (Lombok-Version, Java-Extension oder Umstieg auf JDK 25).
- Umstieg auf JDK 25 (aktuelle LTS) in einem eigenen Branch: `pom.xml`, Lombok ≥ 1.18.40, JaCoCo, CI, Dockerfile.
- Beim Create fehlt die Regel „Verkaufsstart vor Eventbeginn“, die es beim Update gibt.
- `EventMapper.toCreateEventDto` existiert nur für den Integrationstest; besser in die `EventTestDataFactory` verschieben.
- Gemeinsame Basisklasse `AbstractIntegrationTest` (Key-Setup, gleiche Konfiguration, ein gemeinsamer Context).
- Bruno-Lauf in die CI aufnehmen; die festen Datumswerte (2027) in den Requests veralten irgendwann.
- Nächste Features laut Datenmodell: `ticket_categories`, Reservierungs-Service.

## Commit-Verlauf
| Datum | Hash | Nachricht |
|---|---|---|
| 2026-10-13 14:31 | `7474533` | docs: ER-Datenmodell mit Diagramm (PNG/SVG/Mermaid) |
| 2026-10-13 14:32 | `5e28f17` | Merge branch 'docs/datenmodell' |
| 2026-10-13 22:50 | `ee31f25` | feat(event): event CRUD with MapStruct mapping and Bruno collection |
| 2026-10-13 22:53 | `4122bc2` | Merge branch 'main' into feature/event |
| 2026-10-13 22:53 | `a3ee76d` | docs: translate README to English |
| 2026-10-14 21:47 | `0467fbd` | test(event): controller and service tests, tests grouped by feature |
| 2026-10-15 01:08 | `bf6b3ee` | feat(event): list endpoint and event integration tests |
| 2026-10-15 01:12 | `f8792bf` | fix(test): bring own JWT keys in EventIntegrationTest |
| 2026-10-17 15:09 | `53b08c5` | test: consistent event tests, AuthTestClient and TestRsaKeys |
| 2026-10-17 15:09 | `7474b38` | Merge branch 'feature/event' |
| 2026-10-17 15:36 | `a81e02d` | fix(api): 400 for a malformed id, complete Bruno event collection |
