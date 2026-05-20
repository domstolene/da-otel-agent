/*
 * Copyright 2025 Domstoladministrasjonen, Norway
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
package no.domstol.otel.agent.frontend;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.boot.info.BuildProperties;
import org.springframework.ui.Model;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import no.domstol.otel.agent.service.AgentConfiguration;

@Controller
public class AgentConfigurationController {
    private static final String UNKNOWN_VERSION = "unknown";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RestTemplate restTemplate;
    private final AgentConfigurationRenderer configurationRenderer;
    private final String frontendVersion;

    @Autowired
    public AgentConfigurationController(RestTemplate restTemplate, ObjectProvider<BuildProperties> buildPropertiesProvider,
            AgentConfigurationRenderer configurationRenderer) {
        this(restTemplate, buildPropertiesProvider.getIfAvailable(), configurationRenderer);
    }

    AgentConfigurationController(RestTemplate restTemplate, BuildProperties buildProperties) {
        this(restTemplate, buildProperties, new AgentConfigurationRenderer());
    }

    AgentConfigurationController(RestTemplate restTemplate, String frontendVersion) {
        this(restTemplate, frontendVersion, new AgentConfigurationRenderer());
    }

    AgentConfigurationController(RestTemplate restTemplate, BuildProperties buildProperties,
            AgentConfigurationRenderer configurationRenderer) {
        this(restTemplate, resolveFrontendVersion(buildProperties), configurationRenderer);
    }

    AgentConfigurationController(RestTemplate restTemplate, String frontendVersion,
            AgentConfigurationRenderer configurationRenderer) {
        this.restTemplate = restTemplate;
        this.frontendVersion = frontendVersion;
        this.configurationRenderer = configurationRenderer;
    }

    @GetMapping("/")
    public String listConfigurations(Model model) {
        String internalURL = System.getProperty("otel.configuration.service.url", "http://localhost:8080");
        AgentConfiguration[] configs = restTemplate.getForObject(internalURL + "/agent-configuration",
                AgentConfiguration[].class);
        List<AgentConfiguration> configurationList = configs == null ? List.of() : Arrays.asList(configs);
        addCommonModelAttributes(model, internalURL);
        addListModelAttributes(model, configurationList);
        model.addAttribute("configs", configurationList);
        return "listConfigurations";
    }

    @GetMapping("/{configName}")
    public String configurationDetails(@PathVariable("configName") String configName, Model model) {
        String internalURL = System.getProperty("otel.configuration.service.url", "http://localhost:8080");
        AgentConfiguration config = restTemplate.getForObject(internalURL + "/agent-configuration/" + configName,
                AgentConfiguration.class);
        model.addAttribute("name", configName);
        addCommonModelAttributes(model, internalURL);
        model.addAttribute("config", config);
        model.addAttribute("formattedRules", configurationRenderer.getFormattedRules(config));
        model.addAttribute("formattedTimestamp", formatTimestamp(config.getTimestamp()));
        return "configurationDetails";
    }

    private void addCommonModelAttributes(Model model, String internalURL) {
        String publicURL = System.getProperty("otel.configuration.public.url", "http://localhost:8080");
        String jaegerURL = System.getProperty("otel.configuration.jaeger.url", "http://localhost:16686");
        model.addAttribute("serviceInternalURL", internalURL + "/agent-configuration");
        model.addAttribute("servicePublicURL", publicURL + "/agent-configuration");
        model.addAttribute("jaegerURL", jaegerURL);
        model.addAttribute("frontendVersion", frontendVersion);
        model.addAttribute("serviceVersion", getServiceVersion(internalURL));
        model.addAttribute("samplerTypes", AgentConfiguration.SamplerType.values());
    }

    private void addListModelAttributes(Model model, List<AgentConfiguration> configs) {
        Map<String, String> formattedTimestamps = new LinkedHashMap<>();
        Map<String, String> rulesPopoverContent = new LinkedHashMap<>();

        for (AgentConfiguration config : configs) {
            String formattedRules = configurationRenderer.getFormattedRules(config);
            formattedTimestamps.put(config.getServiceName(), formatTimestamp(config.getTimestamp()));
            rulesPopoverContent.put(config.getServiceName(), "<pre>" + HtmlUtils.htmlEscape(formattedRules) + "</pre>");
        }

        model.addAttribute("formattedTimestamps", formattedTimestamps);
        model.addAttribute("rulesPopoverContent", rulesPopoverContent);
    }

    private static String resolveFrontendVersion(BuildProperties buildProperties) {
        if (buildProperties == null || buildProperties.getVersion() == null || buildProperties.getVersion().isBlank()) {
            return UNKNOWN_VERSION;
        }
        return buildProperties.getVersion();
    }

    private String formatTimestamp(long timestamp) {
        return Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault())
                .format(TIMESTAMP_FORMATTER);
    }

    private String getServiceVersion(String internalURL) {
        try {
            Map<?, ?> info = restTemplate.getForObject(internalURL + "/actuator/info", Map.class);
            if (info == null) {
                return UNKNOWN_VERSION;
            }

            Object build = info.get("build");
            if (build instanceof Map<?, ?> buildInfo) {
                Object version = buildInfo.get("version");
                if (version instanceof String serviceVersion && !serviceVersion.isBlank()) {
                    return serviceVersion;
                }
            }
        } catch (RestClientException e) {
            return UNKNOWN_VERSION;
        }

        return UNKNOWN_VERSION;
    }
}
