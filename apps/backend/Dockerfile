# =============================================================================
# Overex backend — multi-stage image
#
# Stage 1 builds the fat jar with the Maven wrapper (no local Maven needed).
# Stage 2 ships only a JRE + the jar, keeping the runtime image small.
# Build context is apps/backend (see docker-compose.yml).
#
# The Dockerfile is self-contained: building the image needs only Docker and
# the sources, no Java or Maven on the host, because the jar is built inside
# a container from the JDK image (a container only sees the files of its
# image, never the Java installed on the host).
#
#   JDK = JRE + compiler (javac) and other developer tools  -> needed to build
#   JRE = JVM + standard library + the "java" command        -> needed to run
#   JVM = the program that executes the bytecode (.class files)
# =============================================================================

# ---- Build stage ------------------------------------------------------------
# Base image with a JDK 25 (Eclipse Temurin). "AS build" names the stage so the
# runtime stage can copy from it. Only the files of this image are visible here.
FROM eclipse-temurin:25-jdk AS build
# Working directory for all following instructions (created if missing).
WORKDIR /app

# Copy only what is needed to resolve dependencies first, so this layer is
# cached and re-downloaded only when pom.xml changes.
# .mvn/ holds the wrapper config (pinned Maven version); mvnw is the wrapper.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
# chmod +x: make sure mvnw is executable even if the bit got lost (e.g. after
# a checkout on Windows). ./mvnw: run the file from this folder (not via PATH).
# -B: batch mode (no prompts, no progress bars). -q: quiet (errors only).
# dependency:go-offline only downloads all dependencies and plugins into
# ~/.m2 of this layer, without building. Because this layer depends only on
# pom.xml, it stays cached when just the code changes, so the download is
# skipped on later builds.
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

# Now copy the sources and build the fat jar into target/. The tests are
# compiled but not run (-DskipTests): they run in the CI, and the image build
# must not depend on a test environment.
COPY src ./src
RUN ./mvnw -B -q clean package -DskipTests

# ---- Runtime stage ----------------------------------------------------------
# A fresh, smaller base image with only a JRE: running the jar needs no
# compiler. The JDK, Maven and ~/.m2 of the build stage are not part of it.
FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app

# Run as a non-root user: create the system account "overex" with the fixed
# uid 1001 (predictable for file permissions of mounted files such as the JWT
# keys, and for Kubernetes runAsUser), then switch to it.
RUN useradd --system --uid 1001 overex
USER overex

# Take only the fat jar from the build stage (it already contains all
# dependencies). *.jar does not match the thin "*.jar.original".
COPY --from=build /app/target/*.jar app.jar

# Never generate JWT keys inside the container: they must be mounted (see
# docker-compose.yml). Missing keys then stop the service with a clear error
# instead of signing with a throwaway key that changes on every restart.
ENV JWT_GENERATE_MISSING_KEYS=false

# Documentation only: the app listens on 8080. Publishing the port is done when
# the container starts (e.g. "ports:" in docker-compose.yml).
EXPOSE 8080
# Runs when the container starts. Exec form (JSON array): java runs directly as
# the main process, receives stop signals and can shut down cleanly. Arguments
# given at start are appended, e.g. "--server.port=9090".
ENTRYPOINT ["java", "-jar", "app.jar"]
