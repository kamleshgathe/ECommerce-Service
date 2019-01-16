FROM openjdk:8

ARG version 
ARG app

COPY /build/libs/${app}-${version}.jar /opt/${app}/service.jar

WORKDIR /opt/${app}

CMD ["/bin/sh", "-c", "java $JAVA_OPTIONS -Djava.util.logging.config.file=none -jar service.jar"]
