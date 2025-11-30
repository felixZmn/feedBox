FROM eclipse-temurin:25-alpine AS builder

RUN apk add --no-cache maven

WORKDIR /app

COPY ./src ./src
COPY pom.xml .
RUN mvn --no-transfer-progress clean install

FROM eclipse-temurin:25-alpine
LABEL org.opencontainers.image.source=https://github.com/felixZmn/feedBox

WORKDIR /app

COPY --from=builder /app/target/feedBox.jar ./feedBox.jar

EXPOSE 7000

ENTRYPOINT ["java", "-jar", "/app/feedBox.jar"]
