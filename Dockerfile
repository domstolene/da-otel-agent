FROM ibm-semeru-runtimes:open-17-jre-jammy

RUN mkdir -p /app

# Copy OpenTelemetry agent
COPY da-opentelemetry-javaagent.jar /app/da-opentelemetry-javaagent.jar
RUN chmod 555 /javaagent/da-opentelemetry-javaagent.jar

# Copy application
COPY service.jar /app/service.jar
RUN chmod +x /app/service.jar
WORKDIR /app/

# Bruker Shell-form her for å få normal shell prosessering
ENTRYPOINT java -jar $JAVA_OPTS \
  --add-opens java.base/jdk.internal.misc=ALL-UNNAMED \
  --add-opens java.base/java.nio=ALL-UNNAMED \
  service.jar
