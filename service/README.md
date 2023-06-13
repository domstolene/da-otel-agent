# OpenTelemetry Agent Configuration Service

This folder holds a small Spring Boot based application that keeps track of OpenTelemetry Agent configurations.


In order to create a new configuration:

```shell
curl -X POST http://localhost:8080/agent-configuration -H 'Content-Type: application/json' -d '{"agentName":"tester","sampleRatio":"0.5", "sampler":"traceidration"}'
```