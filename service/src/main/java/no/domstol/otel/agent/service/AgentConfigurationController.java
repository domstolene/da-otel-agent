/*
 * Copyright Domstoladministrasjonen, Norway
 * SPDX-License-Identifier: Apache-2.0
 */
package no.domstol.otel.agent.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.http.HttpStatus;
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

    private ConcurrentMap<String, AgentConfiguration> configurations = new ConcurrentHashMap<>();

    @PostMapping("/agent-configuration")
    public ResponseEntity<String> addAgentConfiguration(@RequestBody AgentConfiguration configuration)
            throws URISyntaxException {
        if (getConfigurations().containsKey(configuration.getServiceName())
                && getConfigurations().get(configuration.getServiceName()).isReadOnly()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("This configuration is read-only");
        }
        getConfigurations().put(configuration.getServiceName(), configuration);
        return ResponseEntity.created(new URI("/agent-configuration/" + configuration.getServiceName())).build();
    }

    @GetMapping("/agent-configuration/{agentName}")
    public ResponseEntity<AgentConfiguration> getAgentConfiguration(@PathVariable String agentName) {
        if (!getConfigurations().containsKey(agentName)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(getConfigurations().get(agentName));
    }

    @PutMapping("/agent-configuration/{agentName}")
    public ResponseEntity<String> editAgentConfiguration(@PathVariable String agentName,
            @RequestBody AgentConfiguration configuration) {
        if (!getConfigurations().containsKey(agentName)) {
            return ResponseEntity.notFound().build();
        }
        if (getConfigurations().get(configuration.getServiceName()).isReadOnly()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("This configuration is read-only");
        }
        getConfigurations().put(configuration.getServiceName(), configuration);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/agent-configuration")
    public void editAgentConfiguration(@RequestBody AgentConfiguration configuration) {
        // TODO: Fix 404 or 403
        getConfigurations().put(configuration.getServiceName(), configuration);
    }

    @DeleteMapping("/agent-configuration/{agentName}")
    public void deleteAgentConfiguration(@PathVariable String agentName) {
        // TODO: Return 403 if readOnly and 404 if not found
        getConfigurations().remove(agentName);
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
