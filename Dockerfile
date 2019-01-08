FROM openjdk:8

ARG version 
ARG app

COPY /build/libs/${app}-${version}.jar /opt/${app}/service.jar
COPY /build/resources/main/stacks-configuration.yaml /opt/config/stacks-configuration.yaml

WORKDIR /opt/${app}

CMD ["/bin/sh", "-c", "java $JAVA_OPTIONS -Djava.util.logging.config.file=none -jar service.jar"]
