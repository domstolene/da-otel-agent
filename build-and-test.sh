#!/bin/bash

set -euo pipefail

current_dir=$(pwd)
agent_path="$current_dir/extension/build/libs/da-opentelemetry-javaagent.jar"
service_pid=""
frontend_pid=""

terminate_process() {
  local pid="$1"

  if [[ -z "$pid" ]] || ! kill -0 "$pid" 2>/dev/null; then
    return 0
  fi

  kill "$pid" 2>/dev/null || true

  for _ in 1 2 3 4 5 6 7 8 9 10; do
    if ! kill -0 "$pid" 2>/dev/null; then
      wait "$pid" 2>/dev/null || true
      return 0
    fi
    sleep 0.5
  done

  kill -KILL "$pid" 2>/dev/null || true
  wait "$pid" 2>/dev/null || true
}

cleanup() {
  terminate_process "$frontend_pid"
  terminate_process "$service_pid"
}

trap cleanup EXIT
trap 'exit 129' HUP
trap 'exit 130' INT
trap 'exit 143' TERM

common_java_args=(
  -Djava.security.egd=file:/dev/./urandom
  "-javaagent:$agent_path"
  -Dotel.metrics.exporter=none
  -Dotel.traces.sampler=dynamic
  -Dotel.logs.exporter=none
)

# Update settings and subproject lock files
./gradlew --write-locks --refresh-dependencies \
  dependencies \
  :extension:dependencies \
  :service:dependencies \
  :frontend:dependencies
# Do the build
./gradlew build

java \
  "${common_java_args[@]}" \
  -Dotel.service.name=da-otel-agent-service \
  -Dotel.configuration.service.url=http://localhost:8080 \
  "-Dotel.configuration.service.file=$current_dir/extension/src/test/resources/traces-configuration.yaml" \
  -Dotel.javaagent.logging=application \
  -jar "$current_dir/service/build/libs/service.jar" &
service_pid=$!

java \
  "${common_java_args[@]}" \
  -Dotel.service.name=da-otel-agent-frontend \
  -Dotel.configuration.service.url=http://localhost:8080 \
  -jar "$current_dir/frontend/build/libs/frontend.jar" &
frontend_pid=$!

echo "Service available at http://localhost:8080"
echo "Frontend available at http://localhost:8081"

while true; do
  if ! kill -0 "$service_pid" 2>/dev/null; then
    wait "$service_pid"
    exit $?
  fi
  if ! kill -0 "$frontend_pid" 2>/dev/null; then
    wait "$frontend_pid"
    exit $?
  fi
  sleep 1
done
