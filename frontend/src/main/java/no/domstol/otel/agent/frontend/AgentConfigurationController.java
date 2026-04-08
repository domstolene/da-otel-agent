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

import java.util.Arrays;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.boot.info.BuildProperties;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import no.domstol.otel.agent.service.AgentConfiguration;

@Controller
public class AgentConfigurationController {
    private static final String UNKNOWN_VERSION = "unknown";

    private final RestTemplate restTemplate;
    private final String frontendVersion;

    @Autowired
    public AgentConfigurationController(RestTemplate restTemplate, ObjectProvider<BuildProperties> buildPropertiesProvider) {
        this(restTemplate, buildPropertiesProvider.getIfAvailable());
    }

    AgentConfigurationController(RestTemplate restTemplate, BuildProperties buildProperties) {
        this.restTemplate = restTemplate;
        this.frontendVersion = resolveFrontendVersion(buildProperties);
    }

    AgentConfigurationController(RestTemplate restTemplate, String frontendVersion) {
        this.restTemplate = restTemplate;
        this.frontendVersion = frontendVersion;
    }

    @GetMapping("/")
    public String listConfigurations(Model model) {
        String internalURL = System.getProperty("otel.configuration.service.url", "http://localhost:8080");
        AgentConfiguration[] configs = restTemplate.getForObject(internalURL + "/agent-configuration",
                AgentConfiguration[].class);
        addCommonModelAttributes(model, internalURL);
        model.addAttribute("configs", Arrays.asList(configs));
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
    }

    private String resolveFrontendVersion(BuildProperties buildProperties) {
        if (buildProperties == null || buildProperties.getVersion() == null || buildProperties.getVersion().isBlank()) {
            return UNKNOWN_VERSION;
        }
        return buildProperties.getVersion();
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
