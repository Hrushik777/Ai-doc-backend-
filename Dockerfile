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

# The JVM gives the heap 25% of container RAM by default. On a 512 MB instance that is ~128 MB,
# and a single 200-DPI A4 page is a ~15 MB bitmap before its PNG and base64 copies - it will run
# out. 75% gives ~384 MB. Override JAVA_OPTS from the host's dashboard to retune without a rebuild.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

# Wildcard rather than the literal 0.0.1-SNAPSHOT, so a version bump does not break the build.
# Outside /app, because extracting refuses a destination that is not empty. Extraction names
# the thin jar after this one, which is what makes it app.jar.
COPY --from=build /build/target/ai-doc-*.jar /tmp/app.jar

# Cold starts are what a visitor waits through: the free instance sleeps when idle, and the
# first request pays for the JVM loading and verifying every Spring, Tomcat and POI class
# again. A class-data-sharing archive does that work once, here, and the JVM maps it in at
# start instead.
#
# CDS needs the unpacked layout (a thin jar beside lib/) - it cannot share classes loaded out
# of nested jars. The training run starts the context and exits as soon as it has refreshed;
# the credentials it is given exist only for this RUN, which cannot reach NVIDIA or Google and
# does not try to. If the archive is ever unusable - a different JVM, different GC - the JVM
# falls back to a normal start rather than failing.
RUN java -Djarmode=tools -jar /tmp/app.jar extract --destination . \
    && rm /tmp/app.jar \
    && GOOGLE_CLIENT_ID=cds-training.apps.googleusercontent.com \
       JWT_SECRET=cds-training-run-only-this-is-not-a-real-signing-key \
       NVIDIA_API_KEY=cds-training \
       java $JAVA_OPTS -XX:ArchiveClassesAtExit=app.jsa -Dspring.context.exit=onRefresh -jar app.jar

# Documentation only. The platform routes to the port named by PORT, which
# application.properties binds through server.port=${PORT:8080}.
EXPOSE 8080

# exec, so the JVM replaces the shell and becomes PID 1 - otherwise it never receives the
# SIGTERM that lets it shut down gracefully. The shell form is what expands $JAVA_OPTS.
ENTRYPOINT ["sh", "-c", "exec java -XX:SharedArchiveFile=app.jsa $JAVA_OPTS -jar app.jar"]
