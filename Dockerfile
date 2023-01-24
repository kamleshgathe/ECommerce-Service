FROM openjdk:17

ARG version 
ARG app

ADD build/AppServerAgent-1.8-21.11.2.33305.zip /tmp/
RUN mkdir -p /opt/appdynamics/appagent && \
    cd /opt/appdynamics/appagent && \
    jar -xvf /tmp/AppServerAgent-1.8-21.11.2.33305.zip && \
    rm /tmp/AppServerAgent-1.8-21.11.2.33305.zip

RUN microdnf install findutils

COPY build/custom-interceptors.xml /opt/appdynamics/appagent/ver21.11.2.33305/conf

COPY /build/libs/${app}-${version}.jar /opt/${app}/service.jar

WORKDIR /opt/${app}

CMD ["/bin/sh", "-c", "java $JAVA_OPTIONS -Djava.util.logging.config.file=none -Djdk.module.illegalAccess=warn -Djdk.tls.client.protocols=TLSv1.2 -Dappdynamics.agent.uniqueHostId=$(cat /proc/sys/kernel/random/uuid) $APPD_NODE_ARGS $APPD_ARGS -jar service.jar"]
