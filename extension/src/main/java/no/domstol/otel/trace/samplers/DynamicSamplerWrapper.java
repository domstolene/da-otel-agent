/*
 * Copyright Domstoladministrasjonen, Norway
 * SPDX-License-Identifier: Apache-2.0
 */
package no.domstol.otel.trace.samplers;

import java.util.List;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;

/**
 * This type serves as a basic wrapper for the actual {@link Sampler}
 * implementation which can be dynamically replaced.
 *
 * @since 1.0
 */
public class DynamicSamplerWrapper implements Sampler {

    private Sampler currentSampler;

    public DynamicSamplerWrapper(Sampler initialSampler) {
        this.setCurrentSampler(initialSampler);
    }

    @Override
    public SamplingResult shouldSample(Context parentContext, String traceId, String name, SpanKind spanKind,
            Attributes attributes, List<LinkData> parentLinks) {
        return getCurrentSampler().shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);
    }

    @Override
    public String getDescription() {
        return "DA Dynamic Sampler";
    }

    public Sampler getCurrentSampler() {
        return currentSampler;
    }

    public void setCurrentSampler(Sampler currentSampler) {
        this.currentSampler = currentSampler;
    }

}
