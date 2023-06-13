/*
 * Copyright Domstoladministrasjonen, Norway
 * SPDX-License-Identifier: Apache-2.0
 */
package no.domstol.otel.agent.configuration;

import java.io.IOException;
import java.util.UUID;
import java.util.logging.Logger;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;

/**
 * This type dynamically provides configurations for the OTEL agent. The
 *
 * @since 1.0
 */
public class DynamicAgentConfigurationProvider {

    private static final Logger logger = Logger.getLogger(DynamicAgentConfigurationProvider.class.getName());
    private static final CloseableHttpClient httpClient = HttpClients.createDefault();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String fallbackServicename = UUID.randomUUID().toString();

    /**
     * Calls the configuration service to obtain a sampler configuration for this
     * agent. If the agent is not registered or obtaining a configuration fails, the
     * default configuration will be returned.
     *
     * @param config
     *
     * @return the agent configuration
     * @see DynamicAgentConfiguration
     */
    public DynamicAgentConfiguration getDynamicConfiguration(ConfigProperties config) {
        // Use the fallback service name if none have been specified
        String serviceName = config.getString("otel.service.name", fallbackServicename);
        String configurationServiceUrl = config.getString("otel.configuration.service.url", "http://localhost:8080");
        DynamicAgentConfiguration defaultAgentConfiguration = new DynamicAgentConfiguration(serviceName);
        try {
            HttpGet request = new HttpGet(configurationServiceUrl + "/agent-configuration/" + serviceName);
            request.addHeader("Accept", "application/json");
            HttpResponse response = httpClient.execute(request);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    String result = EntityUtils.toString(entity);
                    DynamicAgentConfiguration agentConfiguration = objectMapper.readValue(result, DynamicAgentConfiguration.class);
                    return agentConfiguration;
                }
            } else if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                try {
                    HttpPost postRequest = new HttpPost(configurationServiceUrl + "/agent-configuration");
                    postRequest.setEntity(new StringEntity(objectMapper.writeValueAsString(defaultAgentConfiguration),
                            ContentType.APPLICATION_JSON.withCharset("UTF-8")));
                    httpClient.execute(postRequest);
                    logger.severe("Self-registered as \"" + serviceName + "\" at the OTEL Configuration Service");
                } catch (IOException e) {
                    logger.severe("Failed to self-register at the OTEL Configuration Service");
                }
            } else {
                logger.severe("Error: " + response.getStatusLine().getStatusCode());
            }
        } catch (IOException e) {
            logger.severe("Could not connect to OTEL Configuration Service at " + configurationServiceUrl
                    + ", using default sampler \"" + defaultAgentConfiguration.getSampler() + "\".");
        }
        return defaultAgentConfiguration;
    }

}
