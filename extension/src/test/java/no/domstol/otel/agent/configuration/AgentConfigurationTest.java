/*
 * Copyright Domstoladministrasjonen, Norway
 * SPDX-License-Identifier: Apache-2.0
 */
package no.domstol.otel.agent.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.opentelemetry.api.common.AttributeKey;

public class AgentConfigurationTest {

    private String json =
            "{\n" +
            "  \"serviceName\" : \"da-otel-agent-service\",\n" +
            "  \"sampler\" : \"parentbased_always_on\",\n" +
            "  \"sampleRatio\" : 0.2,\n" +
            "  \"readOnly\" : false,\n" +
            "  \"timestamp\" : 0,\n" +
            "  \"rules\" : [ {\n" +
            "    \"exclude\" : [ {\n" +
            "      \"http.target\" : \"/agent-configuration/.+\",\n" +
            "      \"http.method\" : \"GET\"\n" +
            "    } ]\n" +
            "  }, {\n" +
            "    \"include\" : [ {\n" +
            "      \"http.method\" : \"POST\"\n" +
            "    } ]\n" +
            "  } ]\n" +
            "}";

    @Test
    public void testSetAndGetServiceName() {
        AgentConfiguration config = new AgentConfiguration();
        config.setServiceName("TestService");
        assertEquals("TestService", config.getServiceName());
    }

    @Test
    public void testSetAndGetSampler() {
        AgentConfiguration config = new AgentConfiguration();
        config.setSampler(AgentConfiguration.SamplerType.always_on);
        assertEquals(AgentConfiguration.SamplerType.always_on, config.getSampler());
    }

    @Test
    public void testSetAndGetSampleRatio() {
        AgentConfiguration config = new AgentConfiguration();
        config.setSampleRatio(0.5);
        assertEquals(0.5, config.getSampleRatio());
    }

    @Test
    public void testLoadConfigurationFile() throws StreamReadException, DatabindException, IOException {
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        Path file = Paths.get("src", "test", "resources", "traces-configuration.yaml");
        AgentConfiguration configuration = yamlMapper.readValue(file.toFile(), AgentConfiguration.class);

        Map<String, List<Map<AttributeKey<String>, Pattern>>> rules = configuration.getRules();

        assertFalse(rules.get("include").isEmpty());
        assertFalse(rules.get("exclude").isEmpty());
    }

    @Test
    public void testSerializeToJSON() throws StreamReadException, DatabindException, IOException {
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        Path file = Paths.get("src", "test", "resources", "traces-configuration.yaml");
        AgentConfiguration configuration = yamlMapper.readValue(file.toFile(), AgentConfiguration.class);
        assertEquals(json, configuration.toString());
    }

    @Test
    public void testIsReadOnly() {
        AgentConfiguration config = new AgentConfiguration();
        config.setReadOnly(true);
        assertTrue(config.isReadOnly());
    }

    @Test
    public void testToString() {
        AgentConfiguration config = new AgentConfiguration("TestService");
        assertNotNull(config.toString());
    }

    @Test
    public void testEquals() {
        AgentConfiguration config1 = new AgentConfiguration("TestService");
        AgentConfiguration config2 = new AgentConfiguration("TestService");
        assertEquals(config1, config2);
    }
}
