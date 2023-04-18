FROM eclipse-temurin:17-alpine
COPY news-1.0-SNAPSHOT-all.jar /app.jar
COPY seeds.json .
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar", "--roles=webserver", "--roles=orchestrator", "--distributed", \
    "--port=8080", "--elastic-host=http://elasticsearch:9200", "--zookeeper-host=zookeeper:2181", \
    "--kafka-hosts=kafka:29092"]

# Could use multi stage dockerfile to build the jar (huge size due to Playwright)
# For Playwright security considerations in Docker see https://playwright.dev/docs/docker#usage
# FROM mcr.microsoft.com/playwright/java:v1.29.0-focal

# TERM=cygwin ./gradlew run --args="--roles=orchestrator --roles=webserver --lucene-dir=volumes/lucene-index"
# ./gradlew shadowJar
# java -jar build/libs/news-1.0-SNAPSHOT-all.jar --roles=orchestrator --roles=webserver --lucene-dir=volumes/lucene-index

# docker build -f Dockerfile -t news-app build/libs
# docker run --rm --name news-app --network news_default -p 8080:8080 news-app
# docker kill --signal=SIGTERM news-app
