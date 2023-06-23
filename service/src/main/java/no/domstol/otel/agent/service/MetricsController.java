package no.domstol.otel.agent.service;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.prometheus.PrometheusMeterRegistry;

@RestController
@RequestMapping("/metrics")
public class MetricsController {

    private final PrometheusMeterRegistry registry;
    private boolean registered;

    public MetricsController(PrometheusMeterRegistry prometheusMeterRegistry) {
        this.registry = prometheusMeterRegistry;
    }

    private void registerCounters(String agentName) {
        Counter.builder("otel_agents_recorded_samples").tag("agent", agentName)
                .description("the number of samples recorded by the underlying sampler").baseUnit("samples")
                .register(registry);
        Counter.builder("otel_agents_dropped_samples").tag("agent", agentName)
                .description("the number of samples dropped by the underlying sampler").baseUnit("samples")
                .register(registry);
        Counter.builder("otel_agents_excluded_samples").tag("agent", agentName)
                .description("the number of samples recorded due to filtering rules ").baseUnit("samples")
                .register(registry);
        Counter.builder("otel_agents_included_samples").tag("agent", agentName)
                .description("the number of samples dropped due to filtering rules").baseUnit("samples")
                .register(registry);
        Counter.builder("otel_agents_total_samples").tag("agent", agentName)
                .description("the total number of samples processed")
                .baseUnit("samples").register(registry);
    }

    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    public String getMetrics() {
        return registry.scrape();
    }

    @PostMapping("{agentName}")
    public ResponseEntity<String> handleRequest(@PathVariable String agentName, @RequestBody SamplerMetrics metrics) {
        if (registered == false) {
            registerCounters(agentName);
            registered = true;
        }
        registry.counter("otel_agents_recorded_samples", "agent", agentName)
                .increment(metrics.recorded_samples.doubleValue());
        registry.counter("otel_agents_dropped_samples", "agent", agentName)
                .increment(metrics.dropped_samples.doubleValue());
        registry.counter("otel_agents_excluded_samples", "agent", agentName)
                .increment(metrics.excluded_samples.doubleValue());
        registry.counter("otel_agents_included_samples", "agent", agentName)
                .increment(metrics.included_samples.doubleValue());
        registry.counter("otel_agents_total_samples", "agent", agentName)
                .increment(metrics.total_samples.doubleValue());
        return ResponseEntity.ok("Success");
    }

    public boolean findCounterByTag(String counterName, String tagName, String tagValue) {
        Counter counter = registry.find(counterName).counter();
        List<Tag> tags = counter.getId().getTags();
        for (Tag tag : tags) {
            if (tag.getKey().equals(tagName) && tag.getValue().equals(tagValue)) {
                return true;
            }
        }
        return false;
    }
}
