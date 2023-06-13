/*
 * Copyright Domstoladministrasjonen, Norway
 * SPDX-License-Identifier: Apache-2.0
 */
package no.domstol.otel.trace.samplers;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.google.auto.service.AutoService;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSamplerProvider;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import no.domstol.otel.agent.configuration.DynamicAgentConfiguration;
import no.domstol.otel.agent.configuration.DynamicAgentConfigurationProvider;

/**
 * This type's responsibility is to create an initial sampler when first called,
 * then periodically call the sampler configuration service and update settings
 * as required. This may include changing the actual sampler, along with it's
 * configuration. In order to make use of this sampler provider, the agent must
 * be configured with
 *
 * <pre>
 * -Dotel.traces.sampler="dynamic"
 * </pre>
 *
 * @since 1.0
 */
@AutoService(ConfigurableSamplerProvider.class)
public class DynamicSamplerProvider implements ConfigurableSamplerProvider {

    private static final Logger logger = Logger.getLogger(DynamicSamplerProvider.class.getName());
    private static DynamicAgentConfigurationProvider provider = new DynamicAgentConfigurationProvider();
    private static DynamicSamplerWrapper wrapper;
    private static ConfigProperties initialConfig;

    public DynamicSamplerProvider() {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(DynamicSamplerProvider::updateSampler, 5, 5, TimeUnit.SECONDS);
    }

    @Override
    public Sampler createSampler(ConfigProperties config) {
        initialConfig = config;
        Sampler initialSampler = getConfiguredSampler(config);
        wrapper = new DynamicSamplerWrapper(initialSampler);
        return wrapper;
    }

    private static void updateSampler() {
        Sampler configuredSampler = getConfiguredSampler(initialConfig);
        if (!configuredSampler.equals(wrapper.getCurrentSampler())) {
            logger.info("Changing sampler to " + configuredSampler);
            wrapper.setCurrentSampler(configuredSampler);
        }
    }

    private static Sampler getConfiguredSampler(ConfigProperties config) {
        // use the configuration service to obtain a sampler for this application
        DynamicAgentConfiguration configuration = provider.getDynamicConfiguration(config);
        Sampler configuredSampler = Sampler.alwaysOn();
        switch (configuration.getSampler()) {
        case always_off:
            configuredSampler = Sampler.alwaysOff();
            break;
        case always_on:
            configuredSampler = Sampler.alwaysOn();
            break;
        case traceidratio:
            configuredSampler = Sampler.traceIdRatioBased(configuration.getSampleRatio());
            break;
        // TODO: Handle parentbased_always_on,parentbased_always_off,
        // parentbased_traceidratio
        default:
            break;
        }
        return configuredSampler;
    }

    @Override
    public String getName() {
        return "dynamic";
    }

}
