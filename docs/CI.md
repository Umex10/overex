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
| `web-checks` | every branch | `apps/web/**`, `apps/backend/**`, `docker-compose.test.yml` | Three jobs: `build` (`npm ci`, ESLint, `tsc --noEmit`, `next build`) and `unit` (Vitest) in parallel, then `e2e` (Playwright against test DB + backend + frontend build) once both are green | `unit-reports` (JUnit XML), `e2e-reports` (Playwright HTML report, traces) |
| `workflow-lint` | every branch | `.github/workflows/**` | Runs actionlint over all workflow files, the same check as `npm run lint:workflows`; a safety net when it was forgotten locally | – |

All of them can also be started by hand with **Run workflow** in the Actions tab.

Artifacts are at the bottom of a run's page (Actions → workflow → run) and are
kept for 14 days. Open `index.html` inside the zip for the HTML reports.

### Why one workflow with three jobs

The e2e job is by far the slowest (database, Spring, a second production build,
a browser). It only starts once `build` and `unit` are green (`needs:`), so a
failing lint or unit test stops the run early instead of waiting for e2e. The
jobs still show up as separate checks with their own logs, and a flaky e2e job
can be re-run on its own.

### Why it also runs on backend changes

The e2e suite drives the real backend. A backend change can break the user
flow without touching a single frontend file, so `apps/backend/**` triggers the
workflow as well. `build` and `unit` then run too: they take about a minute,
while skipping them per job (a path filter plus `if:` on every job) would make
`e2e` skip as well, because a job is skipped when one it `needs` was skipped.
Working around that costs more complexity than the minute it saves.

### Checking workflows locally

Two tools, two questions. Usual order: write the workflow, run actionlint, if
needed run it with act, then push to a feature branch. GitHub stays the source
of truth; both tools only catch problems earlier.

| Tool | Question | Runs the workflow? | Time |
|------|----------|--------------------|------|
| actionlint | Is the workflow file written correctly? | no, only reads it | ~1 s |
| act | Does the workflow actually pass? | yes, in Docker containers | minutes |

#### actionlint: is the file correct?

[actionlint](https://github.com/rhysd/actionlint) (by rhysd, not by GitHub, but
the common linter for GitHub Actions) reads every file in `.github/workflows/`
and reports, with file, line and rule name:

- schema errors (unknown keys, `need:` instead of `needs:`),
- `needs:` pointing to a job that does not exist,
- expressions in `${{ … }}`: unknown functions (`cancled()`), contexts, outputs,
- inputs of known actions (`node-versio` instead of `node-version`),
- shell mistakes in `run:` blocks (via shellcheck), e.g. unquoted variables.

It cannot know every field of `github.event`; the real run still matters.

Run it from `apps/web`:

```sh
npm run lint:workflows             # no output and exit code 0 = no findings
npm run lint:workflows -- -verbose # also lists every file it checked
```

The script needs nothing installed but Docker. It starts the official image
[`rhysd/actionlint`](https://hub.docker.com/r/rhysd/actionlint) on Docker Hub,
pinned to a version (`1.7.12`) so every machine checks the same way. The image
also contains shellcheck and pyflakes, which actionlint uses for `run:` blocks;
a bare actionlint binary would skip those checks.

```sh
docker run --rm -v "$(git rev-parse --show-toplevel):/repo" -w /repo rhysd/actionlint:1.7.12 -color
#          │    │                                          │        └ image          └ argument for actionlint
#          │    │                                          └ start in /repo (actionlint looks for .github/workflows there)
#          │    └ mount the repository root into the container as /repo
#          └ delete the container afterwards (the image stays)
```

#### act: does the workflow pass?

[act](https://github.com/nektos/act) (by nektos, not by GitHub) runs workflows
on your machine. It starts one Docker container per job, from an image that
imitates GitHub's `ubuntu-latest` runner, and runs the steps in it with
`docker exec`, actions (`setup-node`, …) included. It needs Docker running.

Install it once, as an extension of the GitHub CLI `gh`
([nektos/gh-act](https://github.com/nektos/gh-act)); `gh` picks the right build
for the system, so nothing has to go into `PATH` by hand:

```sh
gh extension install nektos/gh-act
gh act --version
```

Then, **from the repository root**:

```sh
gh act -l                 # list the jobs and their order (stage = needs:)
gh act push -j unit       # run one job as if a push had happened
gh act push -j e2e        # runs build and unit first (needs:), then e2e
```

**`.actrc`** in the repository root is act's configuration file (an "rc" file
like `.npmrc` or `.zshrc`). act reads it on every start; each line counts as a
command-line option. Ours maps the runner label to the image:

```
-P ubuntu-latest=catthehacker/ubuntu:act-latest
```

So `gh act push -j e2e` runs as if `-P ubuntu-latest=…` had been typed. The
image [`catthehacker/ubuntu:act-latest`](https://hub.docker.com/r/catthehacker/ubuntu)
(source: [catthehacker/docker_images](https://github.com/catthehacker/docker_images))
is Ubuntu plus the most important runner tools, about 1-2 GB on the first run.
The plain `ubuntu` image would lack most of them; GitHub's own `ubuntu-latest`
is a full virtual machine that act cannot use.

act also reads a personal `~/.actrc`. Order: `~/.actrc`, then `./.actrc`, then
the command line; the later one wins. Personal settings (e.g. `--pull=false` to
skip the image check on every run) belong in `~/.actrc`, never secrets in the
repository's `.actrc`.

The e2e job works without extra setup: act runs the job container in the host
network and passes the host's Docker socket into it, so `docker compose` starts
the test database on the host's Docker and `localhost:5433` reaches it.

Limits to keep in mind:

- **Your working tree, not the commit.** act copies the current folder,
  uncommitted changes included. GitHub checks out the pushed commit. A green
  local run proves your local state, not the commit.
- **No artifact upload.** act's artifact server does not support
  `upload-artifact@v7` yet, so both upload steps are skipped under act
  (`!env.ACT`; act sets `ACT=true`, GitHub does not).
- **An imitation.** The image has fewer tools than GitHub's runner; the
  workflows install what they need (`setup-java`, `playwright install
  --with-deps`), which makes the first local run slow. Later runs reuse act's
  local cache (npm, Maven, JDK).

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
