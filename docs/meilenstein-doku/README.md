# Projektdoku – Zeitleiste

| Nr | Meilenstein | Zeitraum | Service | Zeitaufwand | End-Commit | Dokument |
|---|---|---|---|---|---|---|
| M01 | AuthKit: Umstellung von HS256 auf RS256 mit JWKS-Endpoint | 2026-09-29 – 2026-10-01 | authkit (Backend, künftiger auth-service) | ca. 4 h | `23bd50b` | [Meilenstein 01](Meilenstein-01-authkit-rs256-jwks.md) |
| M02 | AuthKit: Tests, Coverage und AuthenticationManager-Fix | 2026-10-05 – 2026-10-07 | authkit (Backend, künftiger auth-service) | nicht erfasst | `119a0e9` | [Meilenstein 02](Meilenstein-02-authkit-tests-coverage.md) |
| M03 | AuthKit: Docker, CI/CD mit GitHub Actions und Veröffentlichung auf ghcr.io | 2026-10-09 – 2026-10-10 | authkit (Backend, CI, Image `ghcr.io/umex10/authkit-backend`) | nicht erfasst | `edd0e90` | [Meilenstein 03](Meilenstein-03-authkit-docker-ci-ghcr.md) |
| M04 | AuthKit: Integrationstest, Key-Handling und 401 bei ungültigem Refresh-Cookie | 2026-10-11 – 2026-10-11 | authkit (Backend, künftiger auth-service) | nicht erfasst | `c54c3f6` | [Meilenstein 04](Meilenstein-04-authkit-integrationstest-keys.md) |
| M05 | AuthKit: Sicherheits- und Web-Härtung, Frontend-CI und Release v1.0.0 | 2026-10-11 – 2026-10-12 | authkit (Backend, Web-Client, CI, Image `ghcr.io/umex10/authkit-backend:1.0.0`) | nicht erfasst | `7fb315a` | [Meilenstein 05](Meilenstein-05-authkit-web-haertung-ci-release.md) |
| M06 | Overex: Datenmodell und Event-Feature mit Tests und Bruno-Collection | 2026-10-13 – 2026-10-17 | overex (Monolith-Backend `apps/backend`, Bruno-Collection) | nicht erfasst | `a81e02d` | [Meilenstein 06](Meilenstein-06-event-crud-tests.md) |
| M07 | Overex: Java 25, App-Shell und paginierte Event-Liste mit Kategorien | 2026-10-17 – 2026-10-18 | overex (Backend, Web-Client, CI) | ca. 10 h (davon 4 h geschätzt) | `7fcda8e` | [Meilenstein 07](Meilenstein-07-java25-frontend-eventliste.md) |
| M08 | Overex: Kategorie-Tests, Event-Detailseite, Breadcrumbs und Gastmodus | 2026-10-19 – 2026-10-25 | overex (Backend, Web-Client, Bruno) | ca. 9,5 h (davon 1 h geschätzt) | `54f61a9` | [Meilenstein 08](Meilenstein-08-detailseite-gastmodus.md) |
| M09 | Overex: Umbau nach Feature und Frontend-Unit-Tests | 2026-10-26 – 2026-11-02 | overex (Backend, Web-Client, Bruno, Doku) | ca. 7,5 h | `6a6ea24` | [Meilenstein 09](Meilenstein-09-umbau-frontend-unit-tests.md) |
| M10 | Overex: E2E-Tests für Events und ein gemeinsamer Workflow `web-checks` | 2026-11-02 – 2026-11-02 | overex (Web-Client, Seed, CI) | ca. 4 h | `65f6308` | [Meilenstein 10](Meilenstein-10-e2e-tests-web-checks.md) |
| M11 | Overex: CI-Ausbau, Branch-Schutz und Dependabot | 2026-11-03 – 2026-11-03 | overex (CI, GitHub-Einstellungen, Abhängigkeiten) | ca. 7 h (geschätzt) | `5ee44be` | [Meilenstein 11](Meilenstein-11-ci-ausbau-dependabot.md) |

**Gesamtaufwand bisher:** ca. 42 h (M01 und M07–M11; davon ca. 12 h geschätzt: 17.10, 20.10, 03.11; Zeiten erst ab 18.10 notiert; M02 bis M06 nicht erfasst)

**Übersicht auf einen Blick:** [Uebersicht.md](Uebersicht.md) (als PDF: `pdf/Overex-Meilensteine-Uebersicht.pdf`)
