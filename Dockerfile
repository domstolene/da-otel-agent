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

# Using Shell-form for normal shell processing. Any settings in JAVA_OPTS should
# override the default values specified here.
ENTRYPOINT java -jar \
  -javaagent:/app/da-opentelemetry-javaagent.jar \
  -Dotel.traces.exporter="none" \
  -Dotel.metrics.exporter="none" \
  -Dotel.logs.exporter="none" \
  -Dotel.traces.sampler="dynamic" \
  -Dotel.service.name="da-otel-agent-service" \
  -Dotel.configuration.service.url="http://localhost:8080" \
  -Dotel.configuration.service.fil=/app/sampler-configuration.yaml \
  -Dotel.exporter.otlp.endpoint="http://otel-collector.test:4318" \
  -Dotel.javaagent.logging=application \
  $JAVA_OPTS \
  /app/service.jar
