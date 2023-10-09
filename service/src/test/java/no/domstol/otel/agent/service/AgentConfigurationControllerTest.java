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

    private static final String USER_AGENT_HEADER = "AgentConfigurationServiceClient/1.3";

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

        // First pass should return 201 CREATED
        mockMvc.perform(post("/agent-configuration")
                .contentType(MediaType.APPLICATION_JSON)
                .header("User-Agent", "MyUserAgent")
                .content(asJsonString(agentConfiguration)))
                .andExpect(status().isCreated());

        // Verify that the configuration has been added
        assert (configurations.containsKey("testAgent"));

        // Second pass should return 409 CONFLICT since the resource is already
        // present. Use PUT to update existing configurations
        mockMvc.perform(post("/agent-configuration")
                .contentType(MediaType.APPLICATION_JSON)
                .header("User-Agent", "MyUserAgent")
                .content(asJsonString(agentConfiguration)))
                .andExpect(status().is(409));
    }

    @Test
    public void testGetAgentConfiguration() throws Exception {
        AgentConfiguration agentConfiguration = new AgentConfiguration();
        agentConfiguration.setServiceName("testAgent");
        configurations.put(agentConfiguration.getServiceName(), agentConfiguration);

        mockMvc.perform(get("/agent-configuration/testAgent")
                .contentType(MediaType.APPLICATION_JSON)
                .header("User-Agent", "MyUserAgent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceName", is("testAgent")));
    }

    @Test
    public void testModifyReadOnlyAgentConfiguration() throws Exception {
        AgentConfiguration agentConfiguration = new AgentConfiguration();
        agentConfiguration.setServiceName("testAgent");
        agentConfiguration.setReadOnly(true);

        // First post should be OK – it is for creating the config
        mockMvc.perform(post("/agent-configuration")
                .contentType(MediaType.APPLICATION_JSON)
                .header("User-Agent", USER_AGENT_HEADER)
                .content(asJsonString(agentConfiguration)))
                .andExpect(status().isCreated());

        // This should fail because we use a unrecognized client
        mockMvc.perform(put("/agent-configuration/testAgent")
                .contentType(MediaType.APPLICATION_JSON)
                .header("User-Agent", "MyUserAgent")
                .content(asJsonString(agentConfiguration)))
                .andExpect(status().isForbidden());

        // This should pass because we use a recognized client which is allowed
        // to override the read only flag.
        mockMvc.perform(put("/agent-configuration/testAgent")
                .contentType(MediaType.APPLICATION_JSON)
                .header("User-Agent", USER_AGENT_HEADER)
                .content(asJsonString(agentConfiguration)))
                .andExpect(status().isOk());
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
                .header("User-Agent", "MyUserAgent")
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
                .contentType(MediaType.APPLICATION_JSON)
                .header("User-Agent", "MyUserAgent"))
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
