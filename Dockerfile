FROM eclipse-temurin:21-alpine
LABEL org.opencontainers.image.source=https://github.com/felixZmn/feedBox
COPY target/feedBox.jar /feedBox.jar
EXPOSE 7000 
ENTRYPOINT ["java", "-jar", "/feedBox.jar"]
