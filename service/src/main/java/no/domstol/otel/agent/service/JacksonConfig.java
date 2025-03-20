package no.domstol.otel.agent.service;
import com.fasterxml.jackson.databind.DeserializationFeature;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {
	
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> builder.featuresToEnable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
    }
}