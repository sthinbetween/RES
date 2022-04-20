FROM amazoncorretto:17-alpine-jdk
VOLUME /tmp
ARG JAR_FILE
COPY /target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT exec java $JAVA_OPTS -jar /app.jar $ARGS
