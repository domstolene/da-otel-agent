#!/bin/bash

set -euo pipefail

current_dir=$(pwd)
# Update settings and subproject lock files
./gradlew --write-locks --refresh-dependencies \
  dependencies \
  :extension:dependencies \
  :service:dependencies \
  :frontend:dependencies
# Do the build
./gradlew build

java \
  -Djava.security.egd=file:/dev/./urandom \
  -javaagent:$current_dir/extension/build/libs/da-opentelemetry-javaagent.jar \
  -Dotel.metrics.exporter="none" \
  -Dotel.traces.sampler="dynamic" \
  -Dotel.logs.exporter=none \
  -Dlogging.structured.format.console=ecs \
  -Dotel.service.name="da-otel-agent-service" \
  -Dotel.configuration.service.url="http://localhost:8080" \
  -Dotel.configuration.service.file=$current_dir/extension/src/test/resources/traces-configuration.yaml \
  -Dotel.javaagent.logging=application \
  -jar $current_dir/service/build/libs/service.jar
