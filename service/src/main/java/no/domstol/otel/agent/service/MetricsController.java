package no.domstol.otel.agent.service;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.micrometer.core.instrument.Counter;
import io.micrometer.prometheus.PrometheusMeterRegistry;

@RestController
@RequestMapping("/metrics")
public class MetricsController {

    private static final String TAG_NAME = "otel.service.name";
    private final PrometheusMeterRegistry registry;
    private boolean registered;

    public MetricsController(PrometheusMeterRegistry prometheusMeterRegistry) {
        this.registry = prometheusMeterRegistry;
    }

    private void registerCounters(String serviceName) {
        Counter.builder("otel_agents_recorded_samples").tag(TAG_NAME, serviceName)
                .description("the number of samples recorded by the underlying sampler").baseUnit("samples")
                .register(registry);
        Counter.builder("otel_agents_dropped_samples").tag(TAG_NAME, serviceName)
                .description("the number of samples dropped by the underlying sampler").baseUnit("samples")
                .register(registry);
        Counter.builder("otel_agents_excluded_samples").tag(TAG_NAME, serviceName)
                .description("the number of samples recorded due to filtering rules ").baseUnit("samples")
                .register(registry);
        Counter.builder("otel_agents_included_samples").tag(TAG_NAME, serviceName)
                .description("the number of samples dropped due to filtering rules").baseUnit("samples")
                .register(registry);
        Counter.builder("otel_agents_total_samples").tag(TAG_NAME, serviceName)
                .description("the total number of samples processed")
                .baseUnit("samples").register(registry);
    }

    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    public String getMetrics() {
        return registry.scrape();
    }

    @PostMapping("{serviceName}")
    public ResponseEntity<String> handleRequest(@PathVariable String serviceName, @RequestBody SamplerMetrics metrics) {
        // must be done first or the registry will get confused and hide metrics.
        if (registered == false) {
            registerCounters(serviceName);
            registered = true;
        }
        registry.counter("otel_agents_recorded_samples", TAG_NAME, serviceName)
                .increment(metrics.recorded_samples.doubleValue());
        registry.counter("otel_agents_dropped_samples", TAG_NAME, serviceName)
                .increment(metrics.dropped_samples.doubleValue());
        registry.counter("otel_agents_excluded_samples", TAG_NAME, serviceName)
                .increment(metrics.excluded_samples.doubleValue());
        registry.counter("otel_agents_included_samples", TAG_NAME, serviceName)
                .increment(metrics.included_samples.doubleValue());
        registry.counter("otel_agents_total_samples", TAG_NAME, serviceName)
                .increment(metrics.total_samples.doubleValue());
        return ResponseEntity.ok("Success");
    }

}
