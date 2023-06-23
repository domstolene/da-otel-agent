package no.domstol.otel.trace.samplers;

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
    AtomicLong total_samples = new AtomicLong();

    /** The number of samples dropped due to filtering rules */
    @JsonProperty("excluded_samples")
    AtomicLong excluded_samples = new AtomicLong();

    /** The number of samples recorded due to filtering rules */
    @JsonProperty("included_samples")
    AtomicLong included_samples = new AtomicLong();

    /** The number of samples dropped by the underlying sampler */
    @JsonProperty("dropped_samples")
    AtomicLong dropped_samples = new AtomicLong();

    /** The number of samples recorded by the underlying sampler */
    @JsonProperty("recorded_samples")
    AtomicLong recorded_samples = new AtomicLong();

    public synchronized SamplerMetrics copyAndClear() {
        SamplerMetrics copy = new SamplerMetrics();
        copy.total_samples.set(total_samples.get());
        copy.excluded_samples.set(excluded_samples.get());
        copy.included_samples.set(included_samples.get());
        copy.dropped_samples.set(dropped_samples.get());
        copy.recorded_samples.set(recorded_samples.get());
        total_samples.set(0);
        excluded_samples.set(0);
        included_samples.set(0);
        dropped_samples.set(0);
        recorded_samples.set(0);
        return copy;
    }

}
