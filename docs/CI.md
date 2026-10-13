# CI, Docker image and releases

Everything runs on GitHub Actions; the workflows live in `.github/workflows/`.
Each one only starts when files it depends on change, and a newer push to the
same branch cancels the older run.

---

## The workflows

| Workflow | Runs on | Triggered by changes in | What it does | Artifact |
|----------|---------|-------------------------|--------------|----------|
| `backend-verify-ci` | every branch | `apps/backend/**` | `./mvnw verify`: unit, slice and integration tests, JaCoCo report, jar | `backend-reports` (Surefire + JaCoCo HTML) |
| `docker-ci` | every branch except `main` | `apps/backend/**` | Builds the backend image (not pushed) to catch Dockerfile errors before a merge | – |
| `docker-publish` | `main` and version tags | `apps/backend/**` (tags always) | Builds the backend image and pushes it to ghcr.io | – |
| `web-build-ci` | every branch | `apps/web/**` | `npm ci`, ESLint, `tsc --noEmit`, `next build` | – |
| `web-unit-ci` | every branch | `apps/web/**` | Vitest unit tests | `unit-reports` (JUnit XML) |
| `web-e2e-ci` | every branch | `apps/web/**`, `apps/backend/**`, `docker-compose.test.yml` | Playwright against test DB + backend + frontend build | `e2e-reports` (Playwright HTML report, traces) |

All of them can also be started by hand with **Run workflow** in the Actions tab.

Artifacts are at the bottom of a run's page (Actions → workflow → run) and are
kept for 14 days. Open `index.html` inside the zip for the HTML reports.

### Why e2e also runs on backend changes

The e2e suite drives the real backend. A backend change can break the user
flow without touching a single frontend file, so `apps/backend/**` triggers it
as well.

### Caching

`setup-java` caches `~/.m2` (keyed by `pom.xml`), `setup-node` caches `~/.npm`
(keyed by `package-lock.json`), and the Docker builds cache their layers in the
GitHub Actions cache. A run with unchanged dependencies downloads nothing.

---

## The Docker image

The backend image is published to the GitHub Container Registry:

```
ghcr.io/umex10/overex-backend
```

| Tag | Points to |
|-----|-----------|
| `latest` | the newest image (last push to `main` or newest release) |
| `1.0.0` | exactly release v1.0.0, never changes |
| `1.0`, `1` | the newest release of that line |

The image contains no keys and never creates any. Mount a key pair and point
`JWT_PRIVATE_KEY_LOCATION` / `JWT_PUBLIC_KEY_LOCATION` at it (see
[RUNNING.md](RUNNING.md#jwt-keys)). On GitHub the image is listed under
**Packages** on the repository page.

---

## Making a release

1. Merge everything into `main`; all workflows green.
2. Tag the commit with a version and push the tag:
   `git tag -a v1.1.0 -m "…"` and `git push origin v1.1.0`.
3. `docker-publish` runs for the tag and pushes `1.1.0`, `1.1`, `1` and
   `latest`.
4. Create the GitHub release for the tag (Releases → Draft a new release, or
   `gh release create v1.1.0`) with the notes and the `docker pull` line.

Versions follow [Semantic Versioning](https://semver.org): MAJOR for breaking
API changes (e.g. a changed token format), MINOR for new features, PATCH for
fixes.
