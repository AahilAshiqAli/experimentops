FROM eclipse-temurin:21-jdk-alpine

# Creates a linux group named app with a system user named app
RUN addgroup --system app && adduser -S -s /bin/false -G app app

# docker commands will run inside this folder in container
WORKDIR /app

# copies the built platform-api jar from target/platform-api*.jar into /app/app.jar. So, make sure that jar file represents current code
COPY target/platform-api*.jar /app/app.jar

# change ownership of folder app to provide full access to app user and app group
RUN chown -R app:app /app

# change user from root to app
USER app

# listens on this port
EXPOSE 8080

# command runs the spring boot jar
CMD ["java", "-jar", "/app/app.jar"]
