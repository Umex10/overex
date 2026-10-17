# Projektdoku – Zeitleiste

| Nr | Meilenstein | Zeitraum | Service | Zeitaufwand | End-Commit | Dokument |
|---|---|---|---|---|---|---|
| M01 | AuthKit: Umstellung von HS256 auf RS256 mit JWKS-Endpoint | 2026-09-29 – 2026-10-01 | authkit (Backend, künftiger auth-service) | ca. 4 h | `23bd50b` | [Meilenstein 01](Meilenstein-01-authkit-rs256-jwks.md) |
| M02 | AuthKit: Tests, Coverage und AuthenticationManager-Fix | 2026-10-05 – 2026-10-07 | authkit (Backend, künftiger auth-service) | nicht erfasst | `119a0e9` | [Meilenstein 02](Meilenstein-02-authkit-tests-coverage.md) |
| M03 | AuthKit: Docker, CI/CD mit GitHub Actions und Veröffentlichung auf ghcr.io | 2026-10-09 – 2026-10-10 | authkit (Backend, CI, Image `ghcr.io/umex10/authkit-backend`) | nicht erfasst | `edd0e90` | [Meilenstein 03](Meilenstein-03-authkit-docker-ci-ghcr.md) |
| M04 | AuthKit: Integrationstest, Key-Handling und 401 bei ungültigem Refresh-Cookie | 2026-10-11 – 2026-10-11 | authkit (Backend, künftiger auth-service) | nicht erfasst | `c54c3f6` | [Meilenstein 04](Meilenstein-04-authkit-integrationstest-keys.md) |
| M05 | AuthKit: Sicherheits- und Web-Härtung, Frontend-CI und Release v1.0.0 | 2026-10-11 – 2026-10-12 | authkit (Backend, Web-Client, CI, Image `ghcr.io/umex10/authkit-backend:1.0.0`) | nicht erfasst | `7fb315a` | [Meilenstein 05](Meilenstein-05-authkit-web-haertung-ci-release.md) |
| M06 | Overex: Datenmodell und Event-Feature mit Tests und Bruno-Collection | 2026-10-13 – 2026-10-17 | overex (Monolith-Backend `apps/backend`, Bruno-Collection) | nicht erfasst | `a81e02d` | [Meilenstein 06](Meilenstein-06-event-crud-tests.md) |

**Gesamtaufwand bisher:** ca. 4 h (nur erfasste Stunden; M02 bis M06 nicht erfasst)
