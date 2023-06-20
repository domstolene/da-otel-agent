/*
 * Copyright Domstoladministrasjonen, Norway
 * SPDX-License-Identifier: Apache-2.0
 */
package no.domstol.otel.trace.samplers;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;

/**
 * This type serves as a basic wrapper for the actual {@link Sampler}
 * implementation which can be dynamically replaced. In addition it provides
 * means of filtering head-based samples.
 *
 * @since 1.0
 */
public class DynamicSamplerWrapper implements Sampler {

    private static final Logger logger = Logger.getLogger(DynamicSamplerWrapper.class.getName());
    private Sampler currentSampler;
    private Map<String, List<Map<AttributeKey<String>, Pattern>>> rules;

    public DynamicSamplerWrapper(Sampler initialSampler, Map<String, List<Map<AttributeKey<String>, Pattern>>> rules) {
        this.setCurrentSampler(initialSampler);
        this.setRules(rules);
    }

    @Override
    public SamplingResult shouldSample(Context parentContext, String traceId, String name, SpanKind spanKind,
            Attributes attributes, List<LinkData> parentLinks) {
        try {

            // Include samples based on the rules provided
            List<Map<AttributeKey<String>, Pattern>> includes = rules.get("include");
            if (includes != null) {
                for (Map<AttributeKey<String>, Pattern> group : includes) {
                    boolean include = false;
                    for (AttributeKey<String> key : group.keySet()) {
                        String string = attributes.get(key);
                        if (string != null && group.get(key).matcher(string).find()) {
                                include = true;
                            } else {
                                include = false;
                                break;
                        }
                    }
                    if (include) {
                        logger.info("including sample because " + group);
                        return SamplingResult.create(SamplingDecision.RECORD_AND_SAMPLE);
                    }
                }
            }
            // Exclude samples based on the rules provided
            List<Map<AttributeKey<String>, Pattern>> excludes = rules.get("exclude");
            if (excludes != null) {
                for (Map<AttributeKey<String>, Pattern> group : excludes) {
                    boolean exclude = false;
                    for (AttributeKey<String> key : group.keySet()) {
                        String string = attributes.get(key);
                        if (string != null && group.get(key).matcher(string).find()) {
                                exclude = true;
                            } else {
                                exclude = false;
                                break;
                            }
                    }
                    if (exclude) {
                        logger.info("Dropping sample because " + group);
                        return SamplingResult.create(SamplingDecision.DROP);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.severe(e.getMessage());
        }
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

    public void setRules(Map<String, List<Map<AttributeKey<String>, Pattern>>> rules) {
        this.rules = rules;
    }

}
