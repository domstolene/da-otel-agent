
# DA OpenTelemetry Agent & Service

This project delivers an [OpenTelemetry Java Agent](https://opentelemetry.io/docs/instrumentation/java/automatic/) including a remotely configurable [sampler](https://opentelemetry.io/docs/concepts/sampling/), along with the accompanying REST service for configuring it. 

By utilizing a remotely controlled sampler, we can dynamically change its behavior based on the current needs. For example, we could adjust sampling rates or rules, all without having to redeploy or restart our applications – which would normally be the case. If remote control is not desirable, the agent configuration can be set to _read only_. This will still expose the configuration to the service along with any metrics collected. This allowing you to for example. get an idea of how many spans _would be_ sampled if the sampler was set to `always_on`.

The sampler is implemented as an [extension](https://opentelemetry.io/docs/instrumentation/java/automatic/extensions/) to the agent and made use of in a way that allows you to simply replace the existing `opentelemetry-javaagent.jar` with `da-opentelemetry-javaagent.jar`. Existing configurations can be used as is, and making use of the `dynamic` sampler is optional.

## The Java Agent Extension

The agent extension enhances the standard OTEL Java agent's capabilities by allowing it to switch between different [sampler implementations](https://github.com/open-telemetry/opentelemetry-java/blob/main/sdk-extensions/autoconfigure/README.md#sampler) and their configurations on the fly. This is enabled by making use of the `dynamic` sampler which delegates the sampling decisions to one of the available implementations. In addition the dynamic sampler includes a basic filtering mechanism that allows spans to be created or discarded based on a simple set of rules.

This means that you can adjust your sampling strategy during runtime without having to stop and restart your application. It adds a great deal of flexibility to your observability strategy and can help you adapt to changing diagnostic needs.

### Filtering

The filtering mechanism is applied *before* the underlying sampler recieves its data. It can be used to determine whether or not a span should be created. Note that inclusion takes precedence.

OTEL can automatically add certain pieces of metadata, or "tags", to each piece of data it collects. For HTTP requests, this might include things like the target URL of the request (http.target), or the HTTP method used (http.method). These tags are what is used for the filtering. For example:

```yaml
rules:
  - exclude:
    - http.target: "/health/.+"
      http.method: "GET"
    - http.target: "/metrics"
      http.method: "GET"
  - include:
    - http.method: "POST"
```

In this example all HTTP `GET` calls to the health endpoint are ignored, while all `POST` calls are sampled. If the tags does not match any of these configurations, it is up to the underlying sampler to determine whether or not the span should be created.

More than one tag can be specified in each rule, and all must match for the rule to trigger. Also notice that Java regular expressions can be used.

## The Configuration Service

The _OpenTelemetry Configuration Service_ is a component of this project that keeps track of different agent configurations. This service exposes a RESTful API that allows clients to interact with it. The API supports all the common REST verbs except `PATCH`. The endpoints are as follows:

* `POST /agent-configuration` – Posts a _new_ agent configuration. The configuration must be in the payload.
* `GET /agent-configuration/<id>` – Returns an agent configuration or 404 if not found.
* `PUT /agent-configuration/<id>` – Updates an existing configuration, returns 404 if not found, or 403 if it is set to be _read only_. The configuration must be in the payload.
* `PUT /agent-configuration` – Updates an existing configuration, returns 404 if not found, or 403 if it is set to be _read only_. The configuration must be in the payload.
* `DELETE /agent-configuration/<id>` – Deletes the agent configuration.
* `GET /agent-configuration` – Returns all agent configurations.

While the dynamic sampler is working, the following metrics are collected and exposed on the [Prometheus](https://prometheus.io) compatible endpoint `/metrics`. For each of the configured services, the following metrics are collected:

* the total number of samples processed
* the number of samples recorded by the underlying sampler
* the number of samples dropped due to filtering rules
* the number of samples dropped by the underlying sampler
* the number of samples recorded due to filtering rules

Note that the current implementation of the service does _not_ persist agent configurations or metrics. If the service is restarted everything will be lost, however data should be available again once the agents report their configurations and metrics. This may take up to 30 seconds.

### Configuration

The service is delivered as a Docker image, ready to be deployed. The service is itself configured to be instrumented with the agent. However no exporters are defined, so traces are not submitted to any collectors. The default values can be overridden by specifying these in the environment variable `$JAVA_OPTS`.

### Security

The configuration service can be secured using a API key. This is enabled by specifying `-Dotel.configuration.service.api.key="<key>"` as a JVM option when starting the service. The same property must be used when configuring the agent.

## Usage

There are basically three ways of configuring the agent. Either you use a file-based configuration, a service-based, or both. 

A typical use case would be to set up a file based configuration while pointing to the service. In this case the agent will load and use the configuration from the file. It will connect to the service, and if the the agent is not registered there, upload the current configuration. If the configuration is changed on the service, the agent will update and use this version, unless the `readOnly` flag is set to true. The configuration file will automatically be reloaded if changed.

### Example agent configuration

```shell
  -javaagent:da-opentelemetry-javaagent.jar \
  -Dotel.metrics.exporter=none \
  -Dotel.service.name=my-service \
  -Dotel.traces.sampler=dynamic \
  -Dotel.configuration.service.file=otel-configuration-file.yaml \
  -Dotel.configuration.service.url=http://otel-configuration-service.test \
  -Dotel.exporter.otlp.endpoint=http://otel-collector.test:4317 \
  -Dotel.traces.exporter=otlp
```

```yaml
serviceName: my-service
sampler: parentbased_traceidratio
sampleRatio: 0.1
readOnly: false
rules:
  - exclude:
    - http.target: "/agent-configuration/.+"
      http.method: "GET"
  - include:
    - http.method: "POST"
```

Notice that `otel.traces.sampler` must be set to `dynamic`, while the `sampler` entry in the configuration file points to the actual implementation. The configuration must explicitly set to `readOnly: false` in order for the service to change the configuration. The default value is `true`.

## Building and testing

In order to test this setup, first start Jaeger and Prometheus by calling `docker compose up` found in the root folder. This will start a new Jaeger instance in Docker and expose port 4317 for tracing and <a href="http://localhost:16686">http://localhost:16686</a> for the UI. The Prometheus UI will be available at <a href="http://localhost:9090">http://localhost:9090</a>

Now run `build-and-test.sh`. This will build the agent and the service, run the tests and start the service instrumented using the built agent. Note that the service has been configured to add a bit of security using an API key.

Since the configuration service is not started when it's being instrumented, obtaining the remote configuration will fail and defaults will be used. You will see something like this in the log:

```
opentelemetry-javaagent - version: 1.27.0-SNAPSHOT
Could not connect to OTEL Configuration Service at http://localhost:8080,
using sampler "parentbased_always_on".
```

If the OpenTelemetry Configuration Service is available, but does not contain a configuration for the agent, the agent will register itself. You can see this in the log as:

```
Self-registered as "da-otel-agent-service" at the OTEL Configuration Service
```

Now getting the available configurations from the service will a JSON array with all the agent configurations:

```
curl -s -H "X-API-KEY: 0DAE3387-4CDA-417D-B084-53BEC56B7B55"  -X GET http://localhost:8080/agent-configuration | jq
[
  {
    "serviceName": "da-otel-agent-service",
    "sampleRatio": 0.1,
    "sampler": "parentbased_always_on"
  }
]
```

In order to test the agent and the service you can execute the following to turn on sampling:

```bash
curl -H "X-API-KEY: 0DAE3387-4CDA-417D-B084-53BEC56B7B55" -X POST http://localhost:8080/agent-configuration \
  -H 'Content-Type: application/json' \
  -d '{"serviceName":"da-otel-agent-service", "sampler":"always_on"}'
```

The `DynamicSamplerProvider` in the _OpenTelemetry Configuration Service_ will now respond with the following log message:

```
Changing sampler to AlwaysOnSampler
```

The `DynamicSamplerProvider` will poll the service at regular intervals (currently each 30s) to check for updated configurations. You should be able to see this in the Jaeger UI.
