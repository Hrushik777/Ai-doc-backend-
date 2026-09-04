FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Dependencies resolve in their own layer, so a source-only change does not re-download the
# world on every deploy. Render's free plan allows 500 build minutes a month.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
# Tests are skipped here deliberately: the suite already runs locally, and repeating it on
# every image build spends build minutes without telling us anything new.
RUN mvn -B -DskipTests package


FROM eclipse-temurin:17-jre
WORKDIR /app

# PdfDocumentRenderer rasterizes pages through Java2D, which needs fontconfig and freetype
# present to draw glyphs. Without them some PDFs render blank or throw outright.
#
# This is also why the base image is not Alpine: musl routinely breaks AWT/ImageIO, and
# rendering is the first stage of the pipeline.
RUN apt-get update \
    && apt-get install -y --no-install-recommends fontconfig libfreetype6 \
    && rm -rf /var/lib/apt/lists/*

# Wildcard rather than the literal 0.0.1-SNAPSHOT, so a version bump does not break the build.
COPY --from=build /build/target/ai-doc-*.jar app.jar

# The JVM gives the heap 25% of container RAM by default. On a 512 MB instance that is ~128 MB,
# and a single 200-DPI A4 page is a ~15 MB bitmap before its PNG and base64 copies - it will run
# out. 75% gives ~384 MB. Override JAVA_OPTS from the host's dashboard to retune without a rebuild.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

# Documentation only. The platform routes to the port named by PORT, which
# application.properties binds through server.port=${PORT:8080}.
EXPOSE 8080

# exec, so the JVM replaces the shell and becomes PID 1 - otherwise it never receives the
# SIGTERM that lets it shut down gracefully. The shell form is what expands $JAVA_OPTS.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
