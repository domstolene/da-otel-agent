package no.domstol.otel.agent.configuration;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class AgentConfigurationReaderTest {


    @Test
    public void readYamlFile() throws StreamReadException, DatabindException, IOException {
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        Path file = Paths.get("src", "test", "resources", "traces-configuration.yaml");
        AgentConfiguration configuration = yamlMapper.readValue(
                file.toFile(), AgentConfiguration.class);
        // for debugging
        ObjectMapper jsonMapper = new ObjectMapper();
        ObjectWriter writer = jsonMapper.writerWithDefaultPrettyPrinter();
        String pretty = writer.writeValueAsString(configuration);
        System.out.println(pretty);
    }
}
