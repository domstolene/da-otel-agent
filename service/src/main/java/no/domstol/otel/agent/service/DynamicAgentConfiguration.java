/*
 * Copyright Domstoladministrasjonen, Norway
 * SPDX-License-Identifier: Apache-2.0
 */
package no.domstol.otel.agent.service;

/**
 * Basic agent configuration. This roughly corresponds to the OTEL
 * TracerProviderConfiguration type.
 *
 * @see https://github.com/open-telemetry/opentelemetry-java/blob/main/sdk-extensions/autoconfigure/README.md
 * @since 1.0
 */
public class DynamicAgentConfiguration {

    public enum Sampler {
        always_on,
        always_off,
        traceidratio,
        parentbased_always_on,
        parentbased_always_off,
        parentbased_traceidratio
    }

    private String serviceName;
    private double sampleRatio;
    private Sampler sampler;

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public double getSampleRatio() {
        return sampleRatio;
    }

    public void setSampleRatio(double sampleRatio) {
        this.sampleRatio = sampleRatio;
    }

    public Sampler getSampler() {
        return sampler;
    }

    public void setSampler(Sampler sampler) {
        this.sampler = sampler;
    }

    @Override
    public String toString() {
        return "AgentConfiguration [serviceName=" + serviceName + ", sampleRatio=" + sampleRatio + ", sampler="
                + sampler
                + "]";
    }
}
