/*
 * Copyright Domstoladministrasjonen, Norway
 * SPDX-License-Identifier: Apache-2.0
 */
package no.domstol.otel.agent.configuration;

import java.io.IOException;
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
import io.opentelemetry.sdk.trace.samplers.Sampler;

/**
 * This type will connect to the OpenTelemetry Configuration Service and if
 * possible return an {@link AgentConfiguration} for use in the {@link Sampler}.
 *
 * @since 1.0
 */
public class AgentConfigurationServiceClient {

    private static final Logger logger = Logger.getLogger(AgentConfigurationServiceClient.class.getName());
    private static final CloseableHttpClient httpClient = HttpClients.createDefault();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Calls the configuration service to obtain a sampler configuration for this
     * agent. If the agent is not registered or obtaining a configuration fails, the
     * initial configuration will be returned.
     *
     * @param initialConfig Initial agent configuration
     * @param otelConfig    OpenTelemetry configuration
     *
     * @return the agent configuration
     */
    public AgentConfiguration getDynamicConfiguration(AgentConfiguration initialConfig, ConfigProperties otelConfig) {
        String configurationServiceUrl = otelConfig.getString("otel.configuration.service.url");
        try {
            HttpGet request = new HttpGet(
                    configurationServiceUrl + "/agent-configuration/" + initialConfig.getServiceName());
            request.addHeader("Accept", "application/json");
            HttpResponse response = httpClient.execute(request);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    String result = EntityUtils.toString(entity);
                    AgentConfiguration agentConfiguration = objectMapper.readValue(result, AgentConfiguration.class);
                    return agentConfiguration;
                }
            } else if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                try {
                    HttpPost postRequest = new HttpPost(configurationServiceUrl + "/agent-configuration");
                    postRequest.setEntity(new StringEntity(objectMapper.writeValueAsString(initialConfig),
                            ContentType.APPLICATION_JSON.withCharset("UTF-8")));
                    httpClient.execute(postRequest);
                    logger.info("Self-registered as \"" + initialConfig.getServiceName()
                            + "\" at the OTEL Configuration Service");
                } catch (IOException e) {
                    logger.severe("Failed to self-register at the OTEL Configuration Service");
                }
            } else {
                logger.severe("Error: " + response.getStatusLine().getStatusCode());
            }
        } catch (IOException e) {
            logger.severe("Could not connect to OTEL Configuration Service at " + configurationServiceUrl
                    + ", using sampler \"" + initialConfig.getSampler() + "\".");
        }
        return initialConfig;
    }

}
