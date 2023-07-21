/*
 * Copyright Domstoladministrasjonen, Norway
 * SPDX-License-Identifier: Apache-2.0
 */
package no.domstol.otel.agent.configuration;

import java.io.IOException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.logging.Logger;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import no.domstol.otel.trace.samplers.SamplerMetrics;

/**
 * This type will connect to the OpenTelemetry Configuration Service and if
 * possible return an {@link AgentConfiguration} for use in the {@link Sampler}.
 *
 * @since 1.0
 */
public class AgentConfigurationServiceClient {

    private static final String API_KEY_HEADER = "X-API-KEY";
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
     * @param metrics       Sampler metrics
     *
     * @return the agent configuration
     */
    public AgentConfiguration synchronize(AgentConfiguration initialConfig, ConfigProperties otelConfig,
            SamplerMetrics metrics) {
        String configurationServiceUrl = otelConfig.getString("otel.configuration.service.url");
        String apiKey = otelConfig.getString("otel.configuration.service.api.key");
        try {
            HttpGet request = new HttpGet(
                    configurationServiceUrl + "/agent-configuration/" + initialConfig.getServiceName());
            request.addHeader("Accept", "application/json");
            if (apiKey != null)
                request.addHeader(API_KEY_HEADER, apiKey);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        String result = EntityUtils.toString(entity);
                        AgentConfiguration agentConfiguration = objectMapper.readValue(result,
                                AgentConfiguration.class);
                        postMetrics(initialConfig.getServiceName(), configurationServiceUrl, apiKey, metrics);
                        return agentConfiguration;
                    }
                } else if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                    selfRegister(initialConfig, configurationServiceUrl, apiKey);
                } else {
                    logger.severe("Configuration service connection failed with status code: "
                            + response.getStatusLine().getStatusCode());
                }
            }
        } catch (Exception e) {
            logger.severe("Could not connect to OTEL Configuration Service at " + configurationServiceUrl
                    + ", using sampler \"" + initialConfig.getSampler() + "\".");
        }
        return initialConfig;
    }

    private void selfRegister(AgentConfiguration initialConfig, String configurationServiceUrl, String apiKey)
            throws UnsupportedCharsetException, JsonProcessingException {
            HttpPost postRequest = new HttpPost(configurationServiceUrl + "/agent-configuration");
            if (apiKey != null)
                postRequest.addHeader(API_KEY_HEADER, apiKey);
            postRequest.setEntity(new StringEntity(objectMapper.writeValueAsString(initialConfig),
                    ContentType.APPLICATION_JSON.withCharset("UTF-8")));
            try (CloseableHttpResponse execute = httpClient.execute(postRequest)) {
                int statusCode = execute.getStatusLine().getStatusCode();
                if (statusCode >= 200 && statusCode < 300) {
                    logger.info("Self-registered as \"" + initialConfig.getServiceName()
                            + "\" at the OTEL Configuration Service");
                } else {
                    logger.severe("Self registering failed with status code: " + statusCode);
                }
            } catch (Exception e) {
                logger.severe("Failed to self-register at the OTEL Configuration Service");
            }
    }

    public void postMetrics(String serviceName, String configurationServiceUrl, String apiKey, SamplerMetrics metrics)
            throws UnsupportedCharsetException, ClientProtocolException, IOException {
        HttpPost postRequest = new HttpPost(configurationServiceUrl + "/metrics/" + serviceName);
        if (apiKey != null)
            postRequest.addHeader(API_KEY_HEADER, apiKey);
        postRequest.setEntity(new StringEntity(objectMapper.writeValueAsString(metrics.copyAndClear()),
                ContentType.APPLICATION_JSON.withCharset("UTF-8")));
        try (CloseableHttpResponse execute = httpClient.execute(postRequest)) {
            int statusCode = execute.getStatusLine().getStatusCode();
            if (statusCode > 300) {
                logger.severe("Metrics post failed with status code: " + statusCode);
            }
        }
    }

}
