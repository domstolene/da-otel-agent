package no.domstol.otel.agent.service;

import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This type keeps track of samples processed in various ways since the last
 * time it was submitted to the Agent Configuration Service.
 *
 * @since 1.0
 */
public class SamplerMetrics {

    /** The total number of samples processed */
    @JsonProperty("total_samples")
    AtomicLong total_samples;

    /** The number of samples dropped due to filtering rules */
    @JsonProperty("excluded_samples")
    AtomicLong excluded_samples;

    /** The number of samples recorded due to filtering rules */
    @JsonProperty("included_samples")
    AtomicLong included_samples;

    /** The number of samples dropped by the underlying sampler */
    @JsonProperty("dropped_samples")
    AtomicLong dropped_samples;

    /** The number of samples recorded by the underlying sampler */
    @JsonProperty("recorded_samples")
    AtomicLong recorded_samples;

}
