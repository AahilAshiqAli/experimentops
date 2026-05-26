FROM eclipse-temurin:21-jdk-alpine

RUN addgroup --system app && adduser -S -s /bin/false -G app app

WORKDIR /app

COPY target/platform-api*.jar /app/app.jar

RUN chown -R app:app /app

USER app

EXPOSE 8080

CMD ["java", "-jar", "/app/app.jar"]
