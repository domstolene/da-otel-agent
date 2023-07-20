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

    private String json = """
            {
              "serviceName" : "da-otel-agent-service",
              "sampler" : "parentbased_always_on",
              "sampleRatio" : 0.1,
              "readOnly" : false,
              "rules" : [ {
                "exclude" : [ {
                  "http.target" : "/agent-configuration/.+",
                  "http.method" : "GET"
                } ]
              }, {
                "include" : [ {
                  "http.method" : "POST"
                } ]
              } ]
            }
            """.trim();

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
        assertTrue(json.equals(configuration.toString()));
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
