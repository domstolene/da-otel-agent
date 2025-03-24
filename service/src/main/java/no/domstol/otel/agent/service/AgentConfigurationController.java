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
package no.domstol.otel.agent.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * End point for OTEL Agent configurations.
 *
 * @since 1.0
 */
@RestController
@CrossOrigin(origins = "*")
public class AgentConfigurationController {

    private ConcurrentMap<String, AgentConfiguration> configurations = new ConcurrentHashMap<>();

    private static String CLIENT_ID = "AgentConfigurationServiceClient/1.3";
    
    @PostMapping("/agent-configuration")
    public ResponseEntity<?> addAgentConfigurations(@RequestHeader("User-Agent") String userAgent,
            @RequestBody List<AgentConfiguration> configurations) throws URISyntaxException {
        List<String> conflicts = new ArrayList<>();
        for (AgentConfiguration configuration : configurations) {
            if (getConfigurations().containsKey(configuration.getServiceName())) {
                conflicts.add(configuration.getServiceName());
            } else {
                // If timestamp is not provided, set it to current time
                if (configuration.getTimestamp() == 0) {
                    configuration.setTimestamp(Instant.now().toEpochMilli());
                }
                getConfigurations().put(configuration.getServiceName(), configuration);
            }
        }
        if (!conflicts.isEmpty()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("A configuration with the following name(s) already exist: " + conflicts
                            + ". Use PUT to modify them.");
        }
        // For simplicity, return an OK response indicating success.
        if (configurations.size()>1)
        	return ResponseEntity.created(new URI("/agent-configuration/")).build();
        else return ResponseEntity.created(new URI("/agent-configuration/" + configurations.get(0).getServiceName())).build();
    }
    
    @GetMapping("/agent-configuration/{agentName}")
    public ResponseEntity<AgentConfiguration> getAgentConfiguration(@RequestHeader("User-Agent") String userAgent,
            @PathVariable String agentName) {
        if (!getConfigurations().containsKey(agentName)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(getConfigurations().get(agentName));
    }

    @PutMapping("/agent-configuration/{agentName}")
    public ResponseEntity<String> editAgentConfiguration(@RequestHeader("User-Agent") String userAgent,
            @PathVariable String agentName,
            @RequestBody AgentConfiguration configuration) {
        Optional<AgentConfiguration> existingConfig = Optional
                .ofNullable(getConfigurations().get(agentName));
        if (existingConfig.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        // make sure we have a correct timestamp
        if (configuration.getTimestamp() == 0) {
            configuration.setTimestamp(Instant.now().toEpochMilli());
        }
        // only the agent is allowed to override a read-ony configuration
        if (!CLIENT_ID.equals(userAgent) && getConfigurations().get(agentName).isReadOnly()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("This configuration is read-only");
        }
        getConfigurations().put(agentName, configuration);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/agent-configuration/{agentName}")
    public ResponseEntity<String> deleteAgentConfiguration(@RequestHeader("User-Agent") String userAgent,
            @PathVariable String agentName) {
        Optional<AgentConfiguration> existingConfig = Optional
                .ofNullable(getConfigurations().get(agentName));
        if (existingConfig.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        // only the agent is allowed to override a read-ony configuration
        if (!CLIENT_ID.equals(userAgent) && getConfigurations().get(agentName).isReadOnly()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("This configuration is read-only");
        }
        getConfigurations().remove(agentName);
        return ResponseEntity.ok().build();
    }
    
    @PatchMapping("/agent-configuration/{agentName}")
    public ResponseEntity<?> patchAgentConfiguration(
            @RequestHeader("User-Agent") String userAgent,
            @PathVariable String agentName,
            @RequestBody Map<String, Object> updates) {
        // Retrieve the existing configuration.
        AgentConfiguration existingConfig = getConfigurations().get(agentName);
        if (existingConfig == null) {
        	return ResponseEntity.notFound().build();
        	}
        
        
        // only the agent owning the configuration is allowed to override a read-ony configuration
        if (!CLIENT_ID.equals(userAgent) && getConfigurations().get(agentName).isReadOnly()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("This configuration is read-only");
        }
        
        // Use Jackson to merge the changes. This only updates fields provided in the JSON.
        ObjectMapper mapper = new ObjectMapper();
        try {
            AgentConfiguration patchedConfig = mapper.updateValue(existingConfig, updates);
            // Update the timestamp for the configuration
            patchedConfig.setTimestamp(Instant.now().toEpochMilli());
            getConfigurations().put(agentName, patchedConfig);
            return ResponseEntity.ok(patchedConfig);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to update configuration: " + e.getMessage());
        }
    }
    
    @GetMapping("/agent-configuration")
    public List<AgentConfiguration> getAgentConfiguration() {
        // Wrap in a tree map to get sorted keys
        return new ArrayList<>(new TreeMap<>(configurations).values());
    }

    public ConcurrentMap<String, AgentConfiguration> getConfigurations() {
        return configurations;
    }

    public void setConfigurations(ConcurrentMap<String, AgentConfiguration> configurations) {
        this.configurations = configurations;
    }

}
