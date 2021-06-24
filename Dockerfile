FROM openjdk:14

ARG version 
ARG app

ADD build/AppServerAgent-4.5.1.23676.zip /tmp/
RUN mkdir -p /opt/appdynamics/appagent && \
    cd /opt/appdynamics/appagent && \
    jar -xvf /tmp/AppServerAgent-4.5.1.23676.zip && \
    rm /tmp/AppServerAgent-4.5.1.23676.zip

COPY build/custom-interceptors.xml /opt/appdynamics/appagent/ver4.5.1.23676/conf

COPY /build/libs/${app}-${version}.jar /opt/${app}/service.jar

WORKDIR /opt/${app}

CMD ["/bin/sh", "-c", "java $JAVA_OPTIONS -Djava.util.logging.config.file=none -Djdk.module.illegalAccess=warn -Dappdynamics.agent.uniqueHostId=$(cat /proc/sys/kernel/random/uuid) $APPD_NODE_ARGS $APPD_ARGS -jar service.jar"]
