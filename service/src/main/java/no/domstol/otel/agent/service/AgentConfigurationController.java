/*
 * Copyright Domstoladministrasjonen, Norway
 * SPDX-License-Identifier: Apache-2.0
 */
package no.domstol.otel.agent.service;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint for OTEL Agent configurations.
 *
 * @since 1.0
 */
@RestController
public class AgentConfigurationController {

    private ConcurrentMap<String, DynamicAgentConfiguration> configurations = new ConcurrentHashMap<>();

    @PostMapping("/agent-configuration")
    public void addAgentConfiguration(@RequestBody DynamicAgentConfiguration configuration) {
        getConfigurations().put(configuration.getServiceName(), configuration);
    }

    @GetMapping("/agent-configuration/{agentName}")
    public ResponseEntity<DynamicAgentConfiguration> getAgentConfiguration(@PathVariable String agentName) {
        if (!getConfigurations().containsKey(agentName)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(getConfigurations().get(agentName));
    }

    @PutMapping("/agent-configuration/{agentName}")
    public void editAgentConfiguration(@PathVariable String agentName, @RequestBody DynamicAgentConfiguration configuration) {
        getConfigurations().put(configuration.getServiceName(), configuration);
    }

    @DeleteMapping("/agent-configuration/{agentName}")
    public void deleteAgentConfiguration(@PathVariable String agentName) {
        getConfigurations().remove(agentName);
    }

    @GetMapping("/agent-configuration")
    public List<DynamicAgentConfiguration> getAgentConfiguration() {
        // Wrap in a tree map to get sorted keys
        return new ArrayList<>(new TreeMap<>(configurations).values());
    }

    public ConcurrentMap<String, DynamicAgentConfiguration> getConfigurations() {
        return configurations;
    }

    public void setConfigurations(ConcurrentMap<String, DynamicAgentConfiguration> configurations) {
        this.configurations = configurations;
    }


}
