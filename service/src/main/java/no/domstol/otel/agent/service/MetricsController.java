/*
 * Copyright Domstoladministrasjonen, Norway
 * SPDX-License-Identifier: Apache-2.0
 */
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

    private static final String OTEL_AGENTS_PROCESSED_SAMPLES = "otel_agents_processed_samples";
    private static final String OTEL_AGENTS_SAMPLER_INCLUDED_SAMPLES = "otel_agents_sampler_included_samples";
    private static final String OTEL_AGENTS_SAMPLER_EXCLUDED_SAMPLES = "otel_agents_sampler_excluded_samples";
    private static final String OTEL_AGENTS_FILTER_INCLUDED_SAMPLES = "otel_agents_filter_included_samples";
    private static final String OTEL_AGENTS_FILTER_EXCLUDED_SAMPLES = "otel_agents_filter_excluded_samples";
    private static final String OTEL_AGENTS_DROPPED_SAMPLES = "otel_agents_dropped_samples";
    private static final String OTEL_AGENTS_RECORDED_SAMPLES = "otel_agents_recorded_samples";
    private static final String TAG_NAME = "otel.service.name";
    private final PrometheusMeterRegistry registry;
    private boolean registered;

    public MetricsController(PrometheusMeterRegistry prometheusMeterRegistry) {
        this.registry = prometheusMeterRegistry;
    }

    private void registerCounters(String serviceName) {
        Counter.builder(OTEL_AGENTS_RECORDED_SAMPLES).tag(TAG_NAME, serviceName)
                .description("the number of samples recorded").baseUnit("samples")
                .register(registry);
        Counter.builder(OTEL_AGENTS_DROPPED_SAMPLES).tag(TAG_NAME, serviceName)
                .description("the number of samples dropped").baseUnit("samples")
                .register(registry);
        Counter.builder(OTEL_AGENTS_FILTER_EXCLUDED_SAMPLES).tag(TAG_NAME, serviceName)
                .description("the number of samples excluded due to filtering rules").baseUnit("samples")
                .register(registry);
        Counter.builder(OTEL_AGENTS_FILTER_INCLUDED_SAMPLES).tag(TAG_NAME, serviceName)
                .description("the number of samples included due to filtering rules").baseUnit("samples")
                .register(registry);
        Counter.builder(OTEL_AGENTS_SAMPLER_EXCLUDED_SAMPLES).tag(TAG_NAME, serviceName)
                .description("the number of samples excluded due to sampling rules").baseUnit("samples")
                .register(registry);
        Counter.builder(OTEL_AGENTS_SAMPLER_INCLUDED_SAMPLES).tag(TAG_NAME, serviceName)
                .description("the number of samples included due to sampling rules").baseUnit("samples")
                .register(registry);
        Counter.builder(OTEL_AGENTS_PROCESSED_SAMPLES).tag(TAG_NAME, serviceName)
                .description("the number of samples processed")
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
        registry.counter(OTEL_AGENTS_RECORDED_SAMPLES, TAG_NAME, serviceName)
                .increment(metrics.recorded_samples.doubleValue());
        registry.counter(OTEL_AGENTS_DROPPED_SAMPLES, TAG_NAME, serviceName)
                .increment(metrics.dropped_samples.doubleValue());
        registry.counter(OTEL_AGENTS_FILTER_EXCLUDED_SAMPLES, TAG_NAME, serviceName)
                .increment(metrics.filter_excluded_samples.doubleValue());
        registry.counter(OTEL_AGENTS_FILTER_INCLUDED_SAMPLES, TAG_NAME, serviceName)
                .increment(metrics.filter_included_samples.doubleValue());
        registry.counter(OTEL_AGENTS_SAMPLER_EXCLUDED_SAMPLES, TAG_NAME, serviceName)
                .increment(metrics.sampler_excluded_samples.doubleValue());
        registry.counter(OTEL_AGENTS_SAMPLER_INCLUDED_SAMPLES, TAG_NAME, serviceName)
                .increment(metrics.sampler_included_samples.doubleValue());
        registry.counter(OTEL_AGENTS_PROCESSED_SAMPLES, TAG_NAME, serviceName)
                .increment(metrics.processed_samples.doubleValue());
        return ResponseEntity.ok("Success");
    }

}
