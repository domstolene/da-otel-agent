/*
 * Copyright Domstoladministrasjonen, Norway
 * SPDX-License-Identifier: Apache-2.0
 */
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

    /** The number of samples dropped due to filtering rules */
    @JsonProperty("filter_excluded_samples")
    AtomicLong filter_excluded_samples = new AtomicLong();

    /** The number of samples recorded due to filtering rules */
    @JsonProperty("filter_included_samples")
    AtomicLong filter_included_samples = new AtomicLong();

    /** The number of samples dropped due to sampler rules */
    @JsonProperty("sampler_excluded_samples")
    AtomicLong sampler_excluded_samples = new AtomicLong();

    /** The number of samples recorded due to sampler rules */
    @JsonProperty("sampler_included_samples")
    AtomicLong sampler_included_samples = new AtomicLong();

    /** The number of samples processed */
    @JsonProperty("processed_samples")
    AtomicLong processed_samples = new AtomicLong();

    /** The number of samples dropped */
    @JsonProperty("dropped_samples")
    AtomicLong dropped_samples = new AtomicLong();

    /** The number of samples recorded */
    @JsonProperty("recorded_samples")
    AtomicLong recorded_samples = new AtomicLong();

    public synchronized SamplerMetrics copyAndClear() {
        SamplerMetrics copy = new SamplerMetrics();
        copy.filter_excluded_samples.set(filter_excluded_samples.get());
        copy.filter_included_samples.set(filter_included_samples.get());
        copy.sampler_excluded_samples.set(sampler_excluded_samples.get());
        copy.sampler_included_samples.set(sampler_included_samples.get());
        copy.processed_samples.set(processed_samples.get());
        copy.dropped_samples.set(dropped_samples.get());
        copy.recorded_samples.set(recorded_samples.get());
        filter_excluded_samples.set(0);
        filter_included_samples.set(0);
        sampler_excluded_samples.set(0);
        sampler_included_samples.set(0);
        processed_samples.set(0);
        dropped_samples.set(0);
        recorded_samples.set(0);
        return copy;
    }

}
