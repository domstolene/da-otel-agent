FROM ibm-semeru-runtimes:open-17-jre-jammy

RUN mkdir -p /app

# Copy OpenTelemetry agent
COPY da-opentelemetry-javaagent.jar /app/da-opentelemetry-javaagent.jar
RUN chmod 555 /app/da-opentelemetry-javaagent.jar

# Copy OpenTelemetry sampler configuration
COPY service/src/main/resources/sampler-configuration.yaml /app/sampler-configuration.yaml
RUN chmod 555 /app/da-opentelemetry-javaagent.jar

# Copy application
COPY service.jar /app/service.jar
RUN chmod +x /app/service.jar
WORKDIR /app/

# Using Shell-form for normal shell processing
ENTRYPOINT java -jar $JAVA_OPTS \
  -javaagent:/app/da-opentelemetry-javaagent.jar \
  -Dotel.metrics.exporter="none" \
  -Dotel.traces.sampler="dynamic" \
  -Dotel.service.name="da-otel-agent-service" \
  -Dotel.configuration.service.url="http://localhost:8080" \
  -Dotel.configuration.service.fil=/app/sampler-configuration.yaml \
  -Dotel.javaagent.logging=application \
  /app/service.jar
