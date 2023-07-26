/*
 * Copyright Domstoladministrasjonen, Norway
 * SPDX-License-Identifier: Apache-2.0
 */
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

}
