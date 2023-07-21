#!/bin/bash

current_dir=$(pwd)
./gradlew build

java \
  -Djava.security.egd=file:/dev/./urandom \
  -javaagent:$current_dir/extension/build/libs/da-opentelemetry-javaagent.jar \
  -Dotel.metrics.exporter="none" \
  -Dotel.traces.sampler="dynamic" \
  -Dotel.service.name="da-otel-agent-service" \
  -Dotel.configuration.service.url="http://localhost:8080" \
  -Dotel.configuration.service.file=$current_dir/extension/src/test/resources/traces-configuration.yaml \
  -Dotel.configuration.service.api.key="0DAE3387-4CDA-417D-B084-53BEC56B7B55" \
  -Dotel.javaagent.logging=application \
  -jar $current_dir/service/build/libs/service.jar
