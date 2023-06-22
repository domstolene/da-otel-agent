
# DA OpenTelemetry Agent and Configuration Service

This project includes a web service with a Representational State Transfer (REST) Application Programming Interface (API). The web service is designed to manage OpenTelemetry Sampler Configurations.

## Motivation

By creating a remotely controlled agent, we can dynamically change its behavior based on the current needs. For example, we could adjust sampling rates, enable or disable certain types of telemetry, or update configuration settings, all without having to redeploy or restart our applications.

## Components

### OpenTelemetry Configuration Service

The _OpenTelemetry Configuration Service_ is a component of this project that keeps track of different sampler configurations. The sampler is a part of the OpenTelemetry SDK that determines whether a given span should be sampled and processed further. The configurations define the conditions under which a span is considered for sampling.

This service exposes a RESTful API that allows clients to interact with it. The API provides a programmatic interface to the service, allowing software clients to interact with the service.

You can use HTTP methods like `GET`, `POST`, `PUT`, `DELETE`, etc., to interact with the service via the REST API. For instance, you can retrieve current configurations, create new configurations, update existing ones, or delete configurations.

### OpenTelemetry Java Agent Extension

The project also includes an extension to the standard OpenTelemetry Java agent. This extension enables the Java agent to dynamically change the sampler implementation and its configuration.

The OpenTelemetry Java agent is a tool that automatically instruments your Java application to track transactions and report metrics. Our extension enhances this agent's capabilities by allowing it to switch between different sampler implementations and configurations on the fly. It also includes a basic filtering mechanism that allows samples to be ignored or accepted based on a simple set of rules.

This means that you can adjust your sampling strategy during runtime without having to stop and restart your application. It adds a great deal of flexibility to your observability strategy and can help you adapt to changing system dynamics or diagnostic needs.

## Usage

There are basically three ways of configuring the agent. Either you use a file based configuration, a service based or both. 

A typical use case would be to set up a file based configuration while pointing to the service. In this case the agent will load and use the configuration from the file. It will then connect to the service, and if the the agent is not registered there, upload the current configuration. If the configuration is changed on the service, the agent will update and use this.

### Example configuration

```shell
  -javaagent:da-opentelemetry-javaagent.jar \
  -Dotel.traces.sampler="dynamic" \
  -Dotel.configuration.service.file="otel-configuration-file.yaml" \
  -Dotel.configuration.service.url="http://otel-configuration-service" \
```

```yaml
serviceName: my-service-name
sampler: parentbased_always_off
sampleRatio: 0.1
readOnly: false
rules:
  - exclude:
    - http.target: "/agent-configuration/.+"
      http.method: "GET"
  - include:
    - http.method: "POST"
```


Notice that `otel.traces.sampler` must be set to `dynamic` in order for this sampler to be used. The configuration must explicitly set to `readOnly: false` in order to use the REST API to change the configuration. The default value is `true`.

## Local testing

In order to test this setup, first start a Jaeger instance by calling `jaeger.sh` found in the root folder. This will start a new instance in Docker and expose ports 4317 and 16686. Having another instance at `localhost` with the same ports exposed is also fine.

Now run `build-and-test.sh`. This will build the agent and the service, run the tests and start the service using the built agent.

Since the configuration service is not started when it's being instrumented, obtaining the remote configuration will fail and defaults will be used. You will see something like this in the log:

```
opentelemetry-javaagent - version: 1.27.0-SNAPSHOT
Could not connect to OTEL Configuration Service at http://localhost:8080, using default sampler "always_off".
```

If the OpenTelemetry Configuration Service is available, but does not contain a configuration for the agent, the agent will register itself. You can see this in the log as:

```
Self-registered as "da-otel-agent-service" at the OTEL Configuration Service
```

Now getting the available configurations from the service will yield:

```
curl -s -X GET http://localhost:8080/agent-configuration | jq
[
  {
    "serviceName": "da-otel-agent-service",
    "sampleRatio": 0,
    "sampler": "always_off"
  }
]
```

In order to test the agent and the service you can execute the following to turn on sampling:

```bash
curl -X POST http://localhost:8080/agent-configuration \
  -H 'Content-Type: application/json' \
  -d '{"serviceName":"da-otel-agent-service", "sampler":"always_on"}'
```

The `DynamicSamplerProvicder` in the _OpenTelemetry Configuration Service_ will now respond with the following log message:

```
Changing sampler to AlwaysOnSampler
```

The `DynamicSamplerProvider` will poll the service at regular intervals (currently each 5s) to check for updated configurations. You should be able to see this in the Jaeger UI.
