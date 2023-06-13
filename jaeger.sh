#!/bin/bash

docker run -d --name jaeger \
  -e COLLECTOR_ZIPKIN_HOST_PORT=:9411 \
  -p 4317:4317 \
  -p 16686:16686 \
  jaegertracing/all-in-one:latest

echo "Jaeger UI is at http://localhost:16686/search"
