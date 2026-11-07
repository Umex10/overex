# Overex – Meilensteine auf einen Blick

Stand: 2026-11-03 · Projektarbeit (5. Semester, FH JOANNEUM, MSD24) und Grundlage der Bachelorarbeit · Repository `github.com/Umex10/overex`

Overex ist eine Ticketing-Plattform für Flash-Sales (Spring Boot, Next.js, später gRPC und Kubernetes). Der Auth-Teil ist aus meinem Projekt AuthKit entstanden. Diese Seite fasst alle Meilensteine kurz zusammen; die Details stehen in den verlinkten Dokumenten.

## Inhaltsverzeichnis

**Phase 1 – AuthKit: sichere Authentifizierung als Fundament**
1. [M01 – Umstellung von HS256 auf RS256 mit JWKS-Endpoint](#m01)
2. [M02 – Tests, Coverage und AuthenticationManager-Fix](#m02)
3. [M03 – Docker, CI/CD mit GitHub Actions und ghcr.io](#m03)
4. [M04 – Integrationstest, Key-Handling und 401 beim Refresh](#m04)
5. [M05 – Web-Härtung, Frontend-CI und Release v1.0.0](#m05)

**Phase 2 – Overex: das Event-Feature**

6. [M06 – Datenmodell und Event-Feature mit Tests und Bruno-Collection](#m06)
7. [M07 – Java 25, App-Shell und paginierte Event-Liste mit Kategorien](#m07)
8. [M08 – Kategorie-Tests, Event-Detailseite, Breadcrumbs und Gastmodus](#m08)
9. [M09 – Umbau nach Feature und Frontend-Unit-Tests](#m09)

**Phase 3 – Qualitätssicherung und CI/CD**

10. [M10 – E2E-Tests für Events und gemeinsamer Workflow `web-checks`](#m10)
11. [M11 – CI-Ausbau, Branch-Schutz und Dependabot](#m11)

[Zeitaufwand](#zeitaufwand) · [Aktueller Stand und nächste Schritte](#stand)

## Überblick

| Nr | Meilenstein | Zeitraum | Aufwand |
|---|---|---|---|
| M01 | HS256 → RS256 mit JWKS | 29.09. – 01.10. | ca. 4 h |
| M02 | Tests, Coverage, AuthenticationManager-Fix | 05.10. – 07.10. | nicht erfasst |
| M03 | Docker, CI/CD, ghcr.io | 09.10. – 10.10. | nicht erfasst |
| M04 | Integrationstest, Key-Handling | 11.10. | nicht erfasst |
| M05 | Web-Härtung, Frontend-CI, Release v1.0.0 | 11.10. – 12.10. | nicht erfasst |
| M06 | Datenmodell, Event-CRUD, Bruno | 13.10. – 17.10. | nicht erfasst |
| M07 | Java 25, App-Shell, Event-Liste | 17.10. – 18.10. | ca. 10 h |
| M08 | Detailseite, Breadcrumbs, Gastmodus | 19.10. – 25.10. | ca. 9,5 h |
| M09 | Umbau nach Feature, Frontend-Unit-Tests | 26.10. – 02.11. | ca. 7,5 h |
| M10 | E2E-Tests, `web-checks` | 02.11. | ca. 4 h |
| M11 | CI-Ausbau, Branch-Schutz, Dependabot | 03.11. | ca. 7 h |

---

## Phase 1 – AuthKit

### <a id="m01"></a>M01 – Umstellung von HS256 auf RS256 mit JWKS-Endpoint
*29.09. – 01.10.2026 · ca. 4 h · [Dokument](Meilenstein-01-authkit-rs256-jwks.md)*

Die Tokens werden mit einem privaten RSA-Schlüssel (RS256) signiert statt mit einem geteilten Geheimnis (HS256). Geprüft werden sie über den OAuth2 Resource Server von Spring, der öffentliche Schlüssel ist als JWKS-Endpoint abrufbar. Damit können später andere Services Tokens prüfen, ohne das Geheimnis zu kennen.

### <a id="m02"></a>M02 – Tests, Coverage und AuthenticationManager-Fix
*05.10. – 07.10.2026 · nicht erfasst · [Dokument](Meilenstein-02-authkit-tests-coverage.md)*

Unit-, Slice- und Integrationstests für das RS256-Backend, Coverage mit JaCoCo. Dabei habe ich eine Endlosrekursion im `AuthenticationManager` gefunden und behoben.

### <a id="m03"></a>M03 – Docker, CI/CD mit GitHub Actions und Veröffentlichung auf ghcr.io
*09.10. – 10.10.2026 · nicht erfasst · [Dokument](Meilenstein-03-authkit-docker-ci-ghcr.md)*

Das Backend läuft als Docker-Image und lässt sich lokal mit Schlüsseln starten. Drei GitHub-Actions-Workflows testen, bauen das Image und veröffentlichen es auf `ghcr.io`.

### <a id="m04"></a>M04 – Integrationstest, Key-Handling und 401 beim Refresh
*11.10.2026 · nicht erfasst · [Dokument](Meilenstein-04-authkit-integrationstest-keys.md)*

Ein vollständiger `@SpringBootTest`, der ohne lokale Schlüsseldateien auskommt; RSA-Schlüssel werden zentral gelesen und geschrieben, beim ersten Start entstehen automatisch Dev-Schlüssel. Ein ungültiges Refresh-Cookie liefert jetzt 401 statt 500.

### <a id="m05"></a>M05 – Sicherheits- und Web-Härtung, Frontend-CI und Release v1.0.0
*11.10. – 12.10.2026 · nicht erfasst · [Dokument](Meilenstein-05-authkit-web-haertung-ci-release.md)*

Keine selbst gewählte Admin-Rolle mehr bei der Registrierung, Sitzungs- und Token-Handling im Frontend repariert, stabile Frontend-Tests mit eigener CI. Versionierte Docker-Images und das erste Release `v1.0.0`.

## Phase 2 – Overex: das Event-Feature

### <a id="m06"></a>M06 – Datenmodell und Event-Feature mit Tests und Bruno-Collection
*13.10. – 17.10.2026 · nicht erfasst · [Dokument](Meilenstein-06-event-crud-tests.md)*

Das ER-Datenmodell für den Monolithen und den künftigen Reservierungs-Service. Die `Event`-Entity mit allen CRUD-Endpoints und Geschäftsregeln, getestet auf drei Ebenen; Tests nach Feature sortiert. Eine Bruno-Collection prüft die laufende App und fand einen 500er, der jetzt 400 liefert.

### <a id="m07"></a>M07 – Java 25, App-Shell und paginierte Event-Liste mit Kategorien
*17.10. – 18.10.2026 · ca. 10 h (4 h geschätzt) · [Dokument](Meilenstein-07-java25-frontend-eventliste.md)*

Umstieg auf Java 25 (LTS) in Code, Docker und CI. Im Web-Client entstehen die App-Shell (Sidebar, Suche) und die Event-Seite mit Karten, Featured Event und Countdown bis zum Verkaufsstart. Das Backend bekommt Kategorien und eine paginierte, filterbare Event-Liste; das Frontend wird nach Features geordnet.

### <a id="m08"></a>M08 – Kategorie-Tests, Event-Detailseite, Breadcrumbs und Gastmodus
*19.10. – 25.10.2026 · ca. 9,5 h (1 h geschätzt) · [Dokument](Meilenstein-08-detailseite-gastmodus.md)*

Die Backend-Tests für Paginierung und Kategorien werden nachgezogen. Neu sind die Detailseite `/events/[id]`, Breadcrumbs, Toasts und Pink als Markenfarbe. Mit dem Gastmodus können Besucher Events ohne Konto ansehen; Schreibzugriffe bleiben geschützt.

### <a id="m09"></a>M09 – Umbau nach Feature und Frontend-Unit-Tests
*26.10. – 02.11.2026 · ca. 7,5 h · [Dokument](Meilenstein-09-umbau-frontend-unit-tests.md)*

REST-Routen im Plural, Backend-Pakete und App-Shell nach Feature sortiert. Danach Unit-Tests für Hooks, Server Actions, den `AuthProvider`, Paginierung und Featured Event, mit einheitlichen Konventionen (AAA, Namensschema, DAMP). Ein Test deckte einen Fehler im Breadcrumb-Hook auf.

## Phase 3 – Qualitätssicherung und CI/CD

### <a id="m10"></a>M10 – E2E-Tests für Events und gemeinsamer Workflow `web-checks`
*02.11.2026 · ca. 4 h · [Dokument](Meilenstein-10-e2e-tests-web-checks.md)*

Playwright-Tests für Liste, Details, Filter, Sortierung und Paginierung, jeweils als Gast und eingeloggt, auf Seed-Daten im Spring-Profil `e2e`. Drei Frontend-Workflows werden zu `web-checks` (build und unit parallel, dann e2e). Workflows lassen sich lokal mit actionlint und act prüfen.

### <a id="m11"></a>M11 – CI-Ausbau, Branch-Schutz und Dependabot
*03.11.2026 · ca. 7 h (geschätzt) · [Dokument](Meilenstein-11-ci-ausbau-dependabot.md)*

actionlint läuft auf GitHub als Pflicht-Check; ein Ruleset schützt `main` (nur per Pull Request). `docker-publish` veröffentlicht das Image erst nach grünen Backend-Tests (Reusable Workflow). Dependabot hält npm- und Maven-Abhängigkeiten aktuell; die ersten Updates (u. a. Spring Boot 4.1.2, vitest 5) sind gemergt. Dazu die Grundlagen von GitHub Actions, Docker-Schichten und CI/CD.

---

## <a id="zeitaufwand"></a>Zeitaufwand

| | Stunden |
|---|---|
| Laut eigener Notiz (M01; M07–M10 ab 18.10.) | ca. 30 h |
| Geschätzt aus den Commits (17.10., 20.10., 03.11.) | ca. 12 h |
| **Summe erfasst und geschätzt** | **ca. 42 h** |
| Nicht erfasst | M02 bis M06 |

Zeiten notiere ich erst seit dem 18.10.2026. Geschätzte Werte sind in den Meilensteinen als solche gekennzeichnet.

## <a id="stand"></a>Aktueller Stand und nächste Schritte

- **Stand:** Auth (RS256, JWKS), Event-Feature mit Kategorien, Detailseite und Gastmodus; Tests auf allen Ebenen (Backend 111 Tests, Frontend 76 Unit- und 28 E2E-Tests); CI mit Pflicht-Check, geschütztem `main`, Image-Veröffentlichung nach grünen Tests und Dependabot.
- **Nächste Schritte:**
  - `backend-verify-ci` und `web-checks` zu Pflicht-Checks machen
  - jjwt auf 0.12+ heben und `JwtService` anpassen
  - offene Dependabot-Security-PRs für `bruno/`
  - fachlich weiter laut Datenmodell: Ticketkategorien, Reservierungs-Service
