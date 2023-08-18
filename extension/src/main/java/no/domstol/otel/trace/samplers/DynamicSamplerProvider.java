/*
 * Copyright 2023 Domstoladministrasjonen, Norway
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package no.domstol.otel.trace.samplers;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
 * <li>Upload the current configuration if not found in the service</li>
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
    private static AgentConfigurationServiceClient remoteConfigReader = new AgentConfigurationServiceClient();
    private static ConfigurationFileReader localConfigReader;
    private static DynamicSamplerWrapper wrapper;
    private static ConfigProperties initialConfig;
    private static AgentConfiguration configuration;
    private static ScheduledExecutorService executor;

    private class ConfigurationFileReader extends Thread {
        private WatchService watchService;
        private Path path;

        public ConfigurationFileReader(String configurationServiceFile) {
            try {
                path = Paths.get(configurationServiceFile);
                watchService = FileSystems.getDefault().newWatchService();
                path.getParent().register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
                this.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            Boolean poll = true;
            while (poll) {
                try {
                    poll = pollEvents(watchService);
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        protected boolean pollEvents(WatchService watchService) throws InterruptedException {
            WatchKey key = watchService.take();
            for (WatchEvent<?> event : key.pollEvents()) {
                @SuppressWarnings("unchecked")
                WatchEvent<Path> ev = (WatchEvent<Path>) event;
                if (ev.context().equals(path.getFileName())) {
                    updateConfigurationFromFile();
                }
            }
            return key.reset();
        }

        private AgentConfiguration readConfigurationFile() {
            logger.info("Loading OTEL Agent configuration from " + path);
            try {
                ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
                AgentConfiguration configuration = mapper.readValue(path.toFile(), AgentConfiguration.class);
                BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
                Instant lastModifiedTime = attrs.lastModifiedTime().toInstant();
                ZonedDateTime utcTime = lastModifiedTime.atZone(ZoneId.of("UTC"));
                configuration.setTimestamp(utcTime.toInstant().toEpochMilli());
                return configuration;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

    }

    public DynamicSamplerProvider() {
    }

    @Override
    public Sampler createSampler(ConfigProperties config) {
        initialConfig = config;

        String configurationServiceFile = config.getString("otel.configuration.service.file");
        String configurationServiceUrl = config.getString("otel.configuration.service.url");
        String serviceName = initialConfig.getString("otel.service.name");

        // there is no reason to not specify a name for the service, unless one
        // is not sampling anything
        if (serviceName == null) {
            throw new IllegalArgumentException("The OTEL service name must be specified with 'otel.service.name'");
        }

        if (configurationServiceFile == null && configurationServiceUrl == null) {
            throw new IllegalArgumentException(
                    "At least one of 'otel.configuration.service.file' and 'otel.configuration.service.url' must be specified");
        }

        // create the initial configuration
        configuration = new AgentConfiguration();
        // set the service name, in case we don't have a configuration file and
        // the agent configuration is not found at the configuration service
        configuration.setServiceName(serviceName);

        // read the configuration from a file if specified
        if (configurationServiceFile != null) {
            File file = new File(configurationServiceFile);
            if (!file.exists()) {
                logger.severe("The specified configuration file '" + file + "' does not exist!");
            }
            localConfigReader = new ConfigurationFileReader(configurationServiceFile);
            configuration = localConfigReader.readConfigurationFile();
            wrapper = new DynamicSamplerWrapper(getConfiguredSampler(configuration), configuration.getRules());
        } else {
            logger.info("Sampler configuration file not specified, using defaults");
        }

        // read the configuration from the service if specified
        if (configurationServiceUrl != null) {
            configuration = remoteConfigReader.synchronize(configuration, config, null);
            wrapper = new DynamicSamplerWrapper(getConfiguredSampler(configuration), configuration.getRules());
            executor = Executors.newScheduledThreadPool(1);
            executor.scheduleWithFixedDelay(DynamicSamplerProvider::synchronizeWithConfigurationService, 5, 30,
                    TimeUnit.SECONDS);
        }
        return wrapper;
    }

    private static void synchronizeWithConfigurationService() {
        try {
            AgentConfiguration newConfiguration = remoteConfigReader.synchronize(configuration, initialConfig,
                    wrapper.getMetrics());
            if (!configuration.isReadOnly() && !newConfiguration.equals(configuration)) {
                logger.info("Updating sampler configuration from OTEL Configuration Service");
                wrapper.setCurrentSampler(getConfiguredSampler(newConfiguration));
                wrapper.setRules(newConfiguration.getRules());
                configuration = newConfiguration;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void updateConfigurationFromFile() {
        try {
            AgentConfiguration newConfiguration = localConfigReader.readConfigurationFile();
            ;
            if (!newConfiguration.equals(configuration)) {
                logger.info("Updating sampler configuration from file");
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

    @Override
    public String getName() {
        return "dynamic";
    }

}
