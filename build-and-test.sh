#!/bin/bash

current_dir=$(pwd)
./gradlew build

java \
  -Djava.security.egd=file:/dev/./urandom \
  -javaagent:$current_dir/extension/build/libs/da-opentelemetry-javaagent.jar \
  -Dotel.service.name="da-otel-agent-service" \
  -Dotel.metrics.exporter="none" \
  -Dotel.traces.sampler="dynamic" \
  -Dotel.configuration.service.url="http://localhost:8080" \
  -Dotel.javaagent.logging=application \
  -jar $current_dir/service/build/libs/service-1.0.0-SNAPSHOT.jar
