/*
 * Copyright Domstoladministrasjonen, Norway
 * SPDX-License-Identifier: Apache-2.0
 */
package no.domstol.otel.agent.configuration;

/**
 * Represents a sampler configuration as serviced by the Agebt Configuration
 * Service.
 *
 * @see https://github.com/open-telemetry/opentelemetry-java/blob/main/sdk-extensions/autoconfigure/README.md
 * @since 1.0
 */
public class DynamicAgentConfiguration {

    public enum SamplerType {
        always_on,
        always_off,
        traceidratio,
        parentbased_always_on,
        parentbased_always_off,
        parentbased_traceidratio
    }

    private String serviceName = "<unspecified>";
    private double sampleRatio = 0.0;
    private SamplerType sampler = SamplerType.always_off;

    public DynamicAgentConfiguration() {
    }

    public DynamicAgentConfiguration(String serviceName) {
        this.serviceName = serviceName;
    }

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

    public SamplerType getSampler() {
        return sampler;
    }

    public void setSampler(SamplerType sampler) {
        this.sampler = sampler;
    }

    @Override
    public String toString() {
        return "DynamicAgentConfiguration [serviceName=" + serviceName + ", sampleRatio=" + sampleRatio + ", sampler="
                + sampler
                + "]";
    }
}
