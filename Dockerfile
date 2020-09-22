FROM openjdk:8

ARG version 
ARG app

ADD build/AppServerAgent-4.5.1.23676.zip /tmp/
RUN mkdir -p /opt/appdynamics/appagent && \
    unzip -oq /tmp/AppServerAgent-4.5.1.23676.zip -d /opt/appdynamics/appagent && \
    rm /tmp/AppServerAgent-4.5.1.23676.zip

COPY build/custom-interceptors.xml /opt/appdynamics/appagent/ver4.5.1.23676/conf

COPY /build/libs/${app}-${version}.jar /opt/${app}/service.jar

WORKDIR /opt/${app}

CMD ["/bin/sh", "-c", "java $JAVA_OPTIONS -Djava.util.logging.config.file=none -Dappdynamics.agent.uniqueHostId=$(sed -e 's#.*/##' /proc/self/cgroup | grep -v '^$' | uniq | sed -rn -e 's/^(.{12}).*/\\1/p') $APPD_NODE_ARGS $APPD_ARGS -jar service.jar"]
