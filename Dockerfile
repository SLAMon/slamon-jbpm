FROM jboss/jbpm-workbench:latest

USER root

RUN mkdir /workspace
WORKDIR /workspace
ADD settings.gradle ./settings.gradle
ADD gradle/ ./gradle/
ADD gradlew ./gradlew
ADD LICENSE ./LICENSE
ADD etc/ ./etc/
ADD src/ ./src/
ADD build.gradle ./build.gradle
RUN chown -R jboss:jboss /workspace

USER jboss
RUN ./gradlew uploadArchives
RUN cp -nav /workspace/build/libs/* $JBOSS_HOME/standalone/deployments/jbpm-console.war/WEB-INF/lib/

# jBPM Custom Configuration files
USER root
ADD etc/standalone-full-jbpm.xml $JBOSS_HOME/standalone/configuration/standalone-full-jbpm.xml
ADD etc/jbpm-users.properties $JBOSS_HOME/standalone/configuration/jbpm-users.properties
ADD etc/jbpm-roles.properties $JBOSS_HOME/standalone/configuration/jbpm-roles.properties
RUN chown jboss:jboss $JBOSS_HOME/standalone/configuration/*

# Run jBPM
USER jboss
WORKDIR $JBOSS_HOME/bin/
CMD ["./standalone.sh", "-b", "0.0.0.0", "--server-config=standalone-full-jbpm.xml"]
