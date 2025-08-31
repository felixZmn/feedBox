FROM eclipse-temurin:21-alpine
COPY target/feedBox.jar /feedBox.jar
EXPOSE 7000 
ENTRYPOINT ["java", "-jar", "/feedBox.jar"]
