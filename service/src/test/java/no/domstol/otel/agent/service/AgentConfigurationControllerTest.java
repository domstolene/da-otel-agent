/*
 * Copyright Domstoladministrasjonen, Norway
 * SPDX-License-Identifier: Apache-2.0
 */
package no.domstol.otel.agent.service;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
public class AgentConfigurationControllerTest {

    private MockMvc mockMvc;

    @InjectMocks
    private AgentConfigurationController agentConfigurationController;

    private static ConcurrentMap<String, AgentConfiguration> configurations;

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(agentConfigurationController).build();
        configurations = new ConcurrentHashMap<>();
        agentConfigurationController.setConfigurations(configurations);
    }

    @Test
    public void testAddAgentConfiguration() throws Exception {
        AgentConfiguration agentConfiguration = new AgentConfiguration();
        agentConfiguration.setServiceName("testAgent");
        agentConfiguration.setReadOnly(false);
        configurations.put(agentConfiguration.getServiceName(), agentConfiguration);

        mockMvc.perform(post("/agent-configuration")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(agentConfiguration)))
                .andExpect(status().isCreated());

        // Verify that the configuration has been added
        assert(configurations.containsKey("testAgent"));
    }

    @Test
    public void testGetAgentConfiguration() throws Exception {
        AgentConfiguration agentConfiguration = new AgentConfiguration();
        agentConfiguration.setServiceName("testAgent");
        configurations.put(agentConfiguration.getServiceName(), agentConfiguration);

        mockMvc.perform(get("/agent-configuration/testAgent")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceName", is("testAgent")));
    }

    @Test
    public void testEditAgentConfiguration() throws Exception {
        AgentConfiguration agentConfiguration = new AgentConfiguration();
        agentConfiguration.setServiceName("testAgent");
        configurations.put(agentConfiguration.getServiceName(), agentConfiguration);

        AgentConfiguration newConfiguration = new AgentConfiguration();
        newConfiguration.setServiceName("testAgent");
        agentConfiguration.setReadOnly(false);
        newConfiguration.setSampleRatio(0.5);

        mockMvc.perform(put("/agent-configuration/testAgent")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(newConfiguration)))
                .andExpect(status().isOk());

        // Verify the configuration has been updated
        assert (configurations.get("testAgent").getSampleRatio() == 0.5);
    }

    @Test
    public void testGetAllAgentConfigurations() throws Exception {
        AgentConfiguration agentConfiguration1 = new AgentConfiguration();
        agentConfiguration1.setServiceName("testAgent1");
        configurations.put(agentConfiguration1.getServiceName(), agentConfiguration1);

        AgentConfiguration agentConfiguration2 = new AgentConfiguration();
        agentConfiguration2.setServiceName("testAgent2");
        configurations.put(agentConfiguration2.getServiceName(), agentConfiguration2);

        mockMvc.perform(get("/agent-configuration")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].serviceName", is("testAgent1")))
                .andExpect(jsonPath("$[1].serviceName", is("testAgent2")));
    }

    // A helper method to turn an object into a JSON string
    private static String asJsonString(final Object obj) {
        try {
            return new ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
