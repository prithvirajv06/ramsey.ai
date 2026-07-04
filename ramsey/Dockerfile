# ---- Build stage ----
FROM gradle:9-jdk25 AS build
WORKDIR /build

# Copy build files first to leverage Docker layer caching for dependencies
COPY build.gradle settings.gradle ./
COPY src ./src

RUN gradle clean bootJar --no-daemon -x test

# ---- Runtime stage ----
FROM eclipse-temurin:25-jre-jammy

# Install native Tesseract engine + English language data.
# Add more language packs as needed, e.g. tesseract-ocr-fra, tesseract-ocr-deu.
RUN apt-get update && apt-get install -y --no-install-recommends \
        tesseract-ocr \
        tesseract-ocr-eng \
        libtesseract-dev \
        libleptonica-dev \
    && rm -rf /var/lib/apt/lists/*

# Confirm/locate tessdata (varies slightly by distro/version)
ENV TESSDATA_PREFIX=/usr/share/tesseract-ocr/4.00/tessdata

WORKDIR /app
COPY --from=build /build/build/libs/ramsey.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]