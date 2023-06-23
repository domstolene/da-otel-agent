/*
 * Copyright Domstoladministrasjonen, Norway
 * SPDX-License-Identifier: Apache-2.0
 */
package no.domstol.otel.trace.samplers;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.auto.service.AutoService;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSamplerProvider;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import no.domstol.otel.agent.configuration.AgentConfiguration;
import no.domstol.otel.agent.configuration.AgentConfigurationServiceClient;

/**
 * This type dynamically provides configurations for the OTEL agent. In order to
 * do so, it will connect to the OTEL Agent Configuration Service or read the
 * configuration from a file, or both. The order is as follows:
 * <ol>
 * <li>Read the configuration from a file if specified in
 * <code>otel.configuration.service.file</code></li>
 * <li>Read the configuration from a service if specified in
 * <code>otel.configuration.service.url</code></li>
 * <li>Periodically poll the service for an updated configuration</li>
 * </ol>
 *
 * In order to make use of this sampler provider, the agent must be configured
 * with
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
    private static AgentConfigurationServiceClient client = new AgentConfigurationServiceClient();
    private static DynamicSamplerWrapper wrapper;
    private static ConfigProperties initialConfig;
    private static AgentConfiguration configuration = new AgentConfiguration();
    private static ScheduledExecutorService executor;

    public DynamicSamplerProvider() {
    }

    @Override
    public Sampler createSampler(ConfigProperties config) {
        initialConfig = config;

        String configurationServiceFile = config.getString("otel.configuration.service.file");
        String configurationServiceUrl = config.getString("otel.configuration.service.url");

        if (configurationServiceFile == null && configurationServiceUrl == null) {
            throw new IllegalArgumentException(
                    "One of 'otel.configuration.service.file' and 'otel.configuration.service.url' or both must be specified");
        }

        // read the configuration from a file if specified
        if (configurationServiceFile != null) {
            File file = new File(configurationServiceFile);
            if (!file.exists()) {
                logger.severe("The specified configuration file '" + file + "' does not exist!");
            }
            logger.info("Loading OTEL Agent configuration from " + file);
            configuration = readConfigurationFile(file);
            wrapper = new DynamicSamplerWrapper(getConfiguredSampler(configuration), configuration.getRules());
        }

        // read the configuration from the service
        if (configurationServiceUrl != null) {
            configuration = client.synchronize(configuration, config, null);
            wrapper = new DynamicSamplerWrapper(getConfiguredSampler(configuration), configuration.getRules());
            executor = Executors.newScheduledThreadPool(1);
            executor.scheduleWithFixedDelay(DynamicSamplerProvider::updateConfigurationFromService, 5, 5,
                    TimeUnit.SECONDS);
        }
        return wrapper;
    }

    private static void updateConfigurationFromService() {
        try {
        AgentConfiguration newConfiguration = client.synchronize(configuration, initialConfig, wrapper.getMetrics());
        if (!newConfiguration.equals(configuration)) {
            logger.info("Changing configuration and sampler to " + newConfiguration.getSampler());
            wrapper.setCurrentSampler(getConfiguredSampler(newConfiguration));
            wrapper.setRules(newConfiguration.getRules());
            configuration = newConfiguration;
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
    }

    private static Sampler getConfiguredSampler(AgentConfiguration configuration) {
        Sampler configuredSampler = Sampler.alwaysOff();
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
        case parentbased_always_off:
            configuredSampler = Sampler.parentBased(Sampler.alwaysOff());
            break;
        case parentbased_always_on:
            configuredSampler = Sampler.parentBased(Sampler.alwaysOn());
            break;
        case parentbased_traceidratio:
            configuredSampler = Sampler
                .parentBasedBuilder(Sampler.traceIdRatioBased(configuration.getSampleRatio()))
                    .build();
            break;
        default:
            break;
        }
        return configuredSampler;
    }

    private AgentConfiguration readConfigurationFile(File file) {
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            AgentConfiguration configuration = mapper.readValue(file, AgentConfiguration.class);
            return configuration;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String getName() {
        return "dynamic";
    }

}
