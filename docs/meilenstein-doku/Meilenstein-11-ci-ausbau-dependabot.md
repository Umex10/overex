# Meilenstein 11 – Overex: CI-Ausbau, Branch-Schutz und Dependabot

| Feld | Wert |
|---|---|
| Projekt | Overex |
| Zeitraum | 2026-11-03 – 2026-11-03 |
| Dokumentiert am | 2026-11-03 19:13 CET |
| Repository / Service | `overex`: CI `.github/`, Backend `apps/backend`, Web-Client `apps/web`, Skripte `scripts/`, Doku `docs/`; GitHub-Einstellungen des Repos |
| Thema | `workflow-lint` (actionlint auf GitHub) als Pflicht-Check; Ruleset für `main`; `docker-publish` erst nach grünen Backend-Tests (Reusable Workflow); `act-all.sh`; Dependabot für npm und Maven; erste Dependabot-Updates gemergt; `@types/node` 24; Node 24 lokal |
| Commits | 6 eigene (`68ca3b3`..`cc851be`), 4 von Dependabot, 7 PR-Merges; Bereich `65f6308..5ee44be` |
| Zeitaufwand | ca. 7 h (geschätzt: Lern- und Arbeitssession am 03.11, nicht notiert) |
| Bezug | Projektarbeit (5. Semester); CI/CD-Teil, Grundlage für die Release- und Deployment-Kapitel |

## Ziel
Ich wollte alle Workflows verstehen, nicht nur benutzen: was `github.ref`, `cancelled()`, Caching, Buildx und die Metadata-Action tun. Danach sollte die CI drei Lücken schließen: kaputte Workflow-Dateien dürfen nicht nach `main`, ein Image darf nur nach grünen Tests veröffentlicht werden, und Abhängigkeiten sollen automatisch aktuell bleiben. Zum Schluss sollte `feature/event` per Pull Request nach `main`.

## Ausgangslage
- actionlint lief nur lokal (`npm run lint:workflows`); vergaß ich es, fiel ein Fehler erst auf GitHub auf.
- `main` hatte keinen Schutz; das Repo war privat, und für private Repos im Free-Plan setzt GitHub Rulesets nicht durch (API: „Upgrade to GitHub Pro or make this repository public“).
- `docker-publish` lief parallel zu `backend-verify-ci`; ein Image konnte auch bei roten Tests auf ghcr.io landen.
- Keine automatischen Updates für Abhängigkeiten; Dependabot-Alerts und Security Updates waren aus.
- `@types/node` stand auf 20, die CI nutzt Node 24, lokal lief Node 22.

## Umsetzung (chronologisch)
1. **2026-11-03** – Neuer Workflow `workflow-lint`: dasselbe actionlint-Image (1.7.12) wie lokal, zunächst nur bei Änderungen unter `.github/workflows/**`. Lokal mit act geprüft, auch mit einer absichtlich kaputten Datei als Gegenprobe (`68ca3b3`).
2. **2026-11-03** – Repo auf öffentlich gestellt (durch mich), Ruleset für `main` angelegt (durch mich, über die Oberfläche): Pull Request Pflicht mit 0 Approvals, `actionlint` als Pflicht-Check, Löschen und Force-Push blockiert, leere Bypass-Liste. `workflow-lint` läuft seitdem ohne `paths`-Filter bei jedem Push, weil ein übersprungener Pflicht-Check nie ein Ergebnis meldet und den Merge für immer blockieren würde (`dc9fc00`).
3. **2026-11-03** – `backend-verify-ci` bekommt den Auslöser `workflow_call` und läuft als erster Job `verify` in `docker-publish`; `publish-image` hat `needs: verify`. Die `concurrency`-Gruppe nutzt `github.workflow`, damit sich der eigenständige und der aufgerufene Lauf auf `main` nicht gegenseitig abbrechen. Upload-Schritt unter act übersprungen. `scripts/act-all.sh` (`npm run act:all`) startet alle Workflows außer `docker-publish` lokal mit act, parallel, mit je einer Log-Datei (`bb8ec20`).
4. **2026-11-03** – `.github/dependabot.yml`: wöchentlich für `apps/web` (npm) und `apps/backend` (Maven), 7 Tage Cooldown, Minor- und Patch-Updates je Ökosystem gruppiert, eine Gruppe für die drei jjwt-Artefakte; Doku in `docs/CI.md` (`17c63fa`).
5. **2026-11-03** – PR #1 `feature/event` → `main` gemergt (`12b197a`, 31 Commits). Danach lief `docker-publish` erstmals mit `verify` → `publish-image`.
6. **2026-11-03** – Der erste Dependabot-Lauf zeigte drei Fehler in der Konfiguration. Korrektur per PR #8: `backend-minor-and-patch` schließt `io.jsonwebtoken:*` aus; neue Gruppe `vitest` für `vitest` und `@vitest/*`; Major-Updates von `@types/node` werden ignoriert (`700c534`, Merge `512ea65`).
7. **2026-11-03** – Dependabot-PRs gemergt: Backend-Gruppe mit 4 Updates (u. a. Spring Boot 4.1.2, Maven-Wrapper 3.10.0) (#10), Frontend-Gruppe mit 20 Updates (#3), vitest-Gruppe (#9), jsdom 30 (#5). #9 und #5 hatten Konflikte in `package-lock.json` und wurden per `@dependabot rebase` neu aufgebaut.
8. **2026-11-03** – `@types/node` auf 24 gehoben, nur die Einträge `@types/node` und `undici-types` in der Lock-Datei geändert (PR #14, `cc851be`). Lokal Node 24.22.2 per nvm installiert und als Standard gesetzt (keine Repo-Änderung).
9. **2026-11-03** – In den GitHub-Einstellungen Dependabot-Alerts und Security Updates eingeschaltet (durch mich).

## Geänderte Dateien
10 Dateien (ohne Meilenstein-Doku); die großen Zeilenzahlen kommen aus `package-lock.json`.

| Datei | Art | Zweck |
|---|---|---|
| `.github/workflows/workflow-lint.yml` | neu | actionlint bei jedem Push, Pflicht-Check für `main` |
| `.github/workflows/backend-verify-ci.yml` | geändert | `workflow_call`, `concurrency` mit `github.workflow`, Upload unter act aus |
| `.github/workflows/docker-publish.yml` | geändert | Job `verify` (Reusable Workflow), `publish-image` mit `needs: verify` |
| `.github/dependabot.yml` | neu | Version Updates für npm und Maven, Gruppen, Cooldown, `ignore` |
| `scripts/act-all.sh`, `apps/web/package.json` (`act:all`) | neu / geändert | alle Workflows außer `docker-publish` lokal |
| `apps/backend/pom.xml`, `.mvn/wrapper/maven-wrapper.properties` | geändert (Dependabot) | Spring Boot 4.1.2, springdoc, JaCoCo, Maven 3.10.0 |
| `apps/web/package.json`, `package-lock.json` | geändert (Dependabot, eigener Commit) | 20 Minor/Patch-Updates, vitest 5, jsdom 30, `@types/node` 24 |
| `docs/CI.md` | geändert | `workflow-lint`, Pflicht-Checks, Reusable Workflow, act-all, Abschnitt Dependabot |

## Entscheidungen
| Entscheidung | Verworfene Alternativen | Begründung |
|---|---|---|
| `workflow-lint` als eigener Workflow, parallel | vor allen Workflows laufen lassen | `needs` gibt es nur innerhalb eines Workflows; `workflow_run` würde jeden Lauf verzögern |
| `workflow-lint` ohne `paths`-Filter | Filter auf `.github/workflows/**` | ein übersprungener Pflicht-Check meldet nichts, GitHub wartet ewig; der Lauf dauert ca. 10 s |
| Nur `actionlint` als Pflicht-Check, vorerst nicht `backend-verify-ci`/`web-checks` | alle Workflows Pflicht | beide haben `paths`-Filter; als Pflicht würden PRs ohne passende Änderung blockiert. Lösung (Filter entfernen oder auf Job-Ebene filtern) als nächster Schritt geplant |
| Repo öffentlich | GitHub Pro (Student Developer Pack) | Rulesets wirken sofort; Actions-Minuten sind für öffentliche Repos kostenlos |
| Reusable Workflow im selben Repo | Steps kopieren; `workflow_run`; eigenes Repo „workflows“ | Tests nur an einer Stelle; `workflow_run` hätte `github.ref` immer auf `main` gesetzt und die Semver-Tags zerstört; ein eigenes Repo lohnt sich erst bei vielen Projekten |
| Nur `docker-publish` wartet auf Tests, `docker-ci` bleibt parallel | auch `docker-ci` warten lassen | warten lohnt sich nur vor einem Schritt mit Wirkung nach außen; parallel kommen beide Rückmeldungen gleichzeitig |
| `docker-publish` nie lokal mit act | komplett lokal testen | act ignoriert den `branches`-Filter und würde mit gültigem Token wirklich pushen; `docker-ci` baut dasselbe Dockerfile ohne Push |
| Dependabot mit Gruppen und `exclude-patterns` | Reihenfolge der Gruppen allein | die Reihenfolge hat jjwt nicht aus der großen Gruppe gehalten |
| `@types/node` Majors ignorieren, von Hand mit Node heben | alle Updates annehmen | Typen müssen zur Laufzeit-Version passen |
| Lock-Datei gezielt ändern | einfaches `npm install` | npm 11.5.2 entfernte dabei 17 `@rolldown/binding-*`-Einträge, die vitest 5 unter Linux braucht |

## Neu gelernt
- **`github.ref`**: vollständiger Name der Referenz, auf der ein Lauf läuft (`refs/heads/…`, `refs/tags/…`, `refs/pull/N/merge`). Beim Dispatch ist es der gewählte Branch oder Tag, nicht leer; leer ist `github.head_ref` außerhalb von Pull Requests.
- **Status-Funktionen im `if`**: ohne Angabe gilt `success()`; `!cancelled()` läuft nach Erfolg und Fehler, aber nicht nach Abbruch; ohne Status-Funktion setzt GitHub `success() &&` davor. `cancelled()` heißt: dieser Lauf wurde abgebrochen.
- **Cache von `setup-java`**: Schlüssel = OS, Architektur, `maven` und der SHA-256 der `pom.xml`; Inhalt = `~/.m2/repository` (plus Maven-Wrapper). Der nächste Lauf berechnet den Schlüssel neu und findet so den Eintrag.
- **Buildx und BuildKit**: BuildKit führt das Dockerfile aus. Das in den Daemon eingebaute BuildKit kann im klassischen Setup nicht nach `type=gha` exportieren; `setup-buildx-action` startet ein eigenständiges BuildKit in einem Container. `build-push-action` baut selbst nichts, sondern ruft `docker buildx build` auf.
- **Docker-Schichten**: Der Cache-Schlüssel (vorheriger Schlüssel + Anweisung + bei `COPY` Hash der Quelldateien) wird vor dem Ausführen berechnet und zeigt auf eine Schicht; die Schicht heißt nach dem Hash ihres Inhalts. Beim Pull gibt es keinen Cache-Schlüssel, Docker sucht direkt nach den Schicht-Hashes aus dem Manifest. Das Manifest listet die Schichten von unten nach oben, die obere gewinnt.
- **Metadata-Action und Semver-Tags**: aus `refs/tags/v1.2.3` werden `1.2.3`, `1.2`, `1` und `latest`; `latest` ist ein normales, verschiebbares Tag für den zuletzt gebauten Stand, nicht automatisch das letzte Release.
- **`GITHUB_TOKEN`**: wird pro Job automatisch erzeugt, gilt nur für dieses Repo bis zum Jobende; was es darf, steht im `permissions:`-Block.
- **CI/CD**: CI = oft und klein integrieren, jeder Stand wird automatisch geprüft, auch nach dem Merge auf `main`. Continuous Delivery = jeder Stand ergibt automatisch ein auslieferbares Paket (bei Overex das Image); Continuous Deployment rollt es zusätzlich automatisch aus.
- **Rulesets und Pflicht-Checks**: „Require a pull request“ und „Require status checks“ sind getrennte Regeln. Ein Pflicht-Check muss auf dem letzten Commit eines PRs ein Ergebnis haben.
- **Dependabot**: Alerts, Security Updates und Version Updates sind drei Funktionen; nur die letzte braucht `dependabot.yml`, die nur von `main` gelesen wird. Bei Maven gibt es keine Lock-Datei; Versionen ohne `<version>` kommen aus dem `dependencyManagement` des Spring-Boot-Parents.

## Tests & Verifikation
| Test | Ergebnis |
|---|---|
| `npm run lint:workflows` nach jeder Workflow-Änderung | keine Befunde |
| `gh act push -j actionlint` mit absichtlich kaputter Datei (`needs: biuld`, `github.reff`) | beide Fehler gefunden, Job rot (Gegenprobe) |
| `gh act workflow_dispatch -W docker-publish.yml -j verify` | 111 von 111 Backend-Tests, Job grün |
| `scripts/act-all.sh` (durch mich ausgeführt) | `backend-verify-ci`, `docker-ci`, `web-checks`, `workflow-lint` grün |
| PR #1 auf GitHub | 5 Checks grün, `actionlint` als „Required“ angezeigt |
| `docker-publish` nach dem Merge von #10 | `verify / verify` 17:06–17:07, danach `publish-image` 17:07–17:09, beide grün |
| `web-checks` auf `main` nach den Dependabot-Merges | grün (build, unit, e2e) |
| PR #14 (`@types/node` 24) | `npm ci` und `tsc` lokal fehlerfrei; auf GitHub build, unit, e2e und actionlint grün |

## Probleme & Lösungen
- **Problem:** Branch-Schutz ließ sich nicht durchsetzen. → **Lösung:** Repo öffentlich gemacht; private Repos brauchen dafür GitHub Pro.
- **Problem:** Ein Pflicht-Check mit `paths`-Filter würde PRs blockieren, die keine passende Datei ändern. → **Lösung:** für `actionlint` den Filter entfernt; für die anderen Workflows offen.
- **Problem:** Gleiche `concurrency`-Gruppe für den eigenständigen und den aufgerufenen Lauf von `backend-verify-ci`. → **Lösung:** Gruppenname mit `github.workflow`.
- **Problem:** act ignoriert den `branches`-Filter; ein einfaches `gh act push` hätte auch `docker-publish` gestartet. → **Lösung:** `act-all.sh` startet die Workflows einzeln ohne `docker-publish`.
- **Problem:** Dependabot packte jjwt trotz eigener Gruppe in die große Backend-Gruppe; das Update auf 0.13 entfernte `Jwts.parserBuilder()` und brach den Build (`JwtService.java:153`), was alle sieben Updates blockierte. → **Lösung:** `exclude-patterns`. Ein eigener jjwt-PR kam danach nicht; der Grund ist offen.
- **Problem:** vitest und `@vitest/coverage-v8` kamen als getrennte PRs mit gemischten Versionen, beide rot. → **Lösung:** Gruppe `vitest`.
- **Problem:** Beim Merge von #5 war nur `actionlint` gemeldet, `web-checks` hatte für den neuen Commit noch nicht begonnen; GitHub erlaubte den Merge. → **Lösung:** auf `main` lief `web-checks` danach grün; bei PR #14 auf alle Checks gewartet. Grundsätzlich lösen das weitere Pflicht-Checks.
- **Problem:** `npm install` mit npm 11.5.2 entfernte plattformspezifische Einträge aus der Lock-Datei. → **Lösung:** nur die Einträge von `@types/node` und `undici-types` übernommen, mit `npm ci` geprüft.

## Eingesetzte Hilfsmittel
Claude (Claude Code) hat in dieser Session die meisten Erklärungen geliefert und die Dateien `workflow-lint.yml`, die Änderungen an `backend-verify-ci.yml` und `docker-publish.yml`, `act-all.sh`, `dependabot.yml`, die Doku in `docs/CI.md`, die Commits, die PRs (#8, #14) und die Merges per `gh` erstellt. Ich habe die Fragen gestellt, die Entscheidungen getroffen, das Repo öffentlich gemacht, das Ruleset und die Sicherheitseinstellungen angelegt, PR #1 erstellt und gemergt und `act-all.sh` ausgeführt. Alle eigenen Commits tragen Claude als Co-Autor. (Hinweis: FH-Richtlinien zur Kennzeichnung beachten.)

## Offene Punkte / Nächste Schritte
- `backend-verify-ci` und `web-checks` zu Pflicht-Checks machen: zuerst Workflows anpassen (Filter entfernen oder auf Job-Ebene filtern), dann im Ruleset eintragen.
- jjwt auf 0.12+ heben und `JwtService` umstellen (`Jwts.parser()`, `verifyWith`, `parseSignedClaims`).
- Drei Bruno-Security-PRs (#11–#13); „Grouped security updates“ oder `/bruno` in `dependabot.yml`.
- Malware-Alerts einschalten; optional automatische Dependency Submission für Maven.
- Kommentar in `docker-publish.yml` („every merge … its own image“) korrigieren.

## Commit-Verlauf
| Datum | Hash | Nachricht |
|---|---|---|
| 2026-11-03 13:52 | `68ca3b3` | ci: lint the workflow files with actionlint on GitHub |
| 2026-11-03 14:04 | `dc9fc00` | ci: run workflow-lint on every push, as a required check for main |
| 2026-11-03 16:13 | `bb8ec20` | ci: publish the image only after green backend tests; act-all script |
| 2026-11-03 17:20 | `17c63fa` | ci: Dependabot version updates for npm and Maven |
| 2026-11-03 17:32 | `12b197a` | Merge pull request #1 from Umex10/feature/event |
| 2026-11-03 17:51 | `700c534` | ci: keep jjwt and vitest out of the broad Dependabot groups |
| 2026-11-03 17:52 | `512ea65` | Merge pull request #8 from Umex10/chore/dependabot-groups |
| 2026-11-03 16:53 | `b77d40e` | chore(backend): bump the backend-minor-and-patch group across 1 directory with 4 updates (Dependabot) |
| 2026-11-03 18:06 | `8ad0ff0` | Merge pull request #10 (Dependabot, Backend) |
| 2026-11-03 16:56 | `6d0ca85` | chore(web): bump the web-minor-and-patch group across 1 directory with 20 updates (Dependabot) |
| 2026-11-03 18:06 | `b915769` | Merge pull request #3 (Dependabot, Frontend) |
| 2026-11-03 17:08 | `f97497a` | chore(web): bump the vitest group across 1 directory with 2 updates (Dependabot) |
| 2026-11-03 18:12 | `880b944` | Merge pull request #9 (Dependabot, vitest) |
| 2026-11-03 17:14 | `e930a0d` | chore(web): bump jsdom from 29.1.1 to 30.1.3 in /apps/web (Dependabot) |
| 2026-11-03 18:14 | `5489bee` | Merge pull request #5 (Dependabot, jsdom) |
| 2026-11-03 18:25 | `cc851be` | chore(web): bump @types/node to 24, matching the Node version in CI |
| 2026-11-03 18:29 | `5ee44be` | Merge pull request #14 from Umex10/chore/types-node-24 |
