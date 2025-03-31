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
package no.domstol.otel.agent.configuration;

import java.io.IOException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
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
 * This type deals with the OpenTelemetry Configuration Service.
 *
 * @since 1.0
 */
public class AgentConfigurationServiceClient {

    private static final String API_KEY_HEADER = "X-API-KEY";
    private static final String USER_AGENT_HEADER = "AgentConfigurationServiceClient/1.2";
    private static final Logger logger = Logger.getLogger(AgentConfigurationServiceClient.class.getName());
    private static final CloseableHttpClient httpClient = HttpClients.createDefault();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Calls the configuration service to obtain a sampler configuration for this
     * agent. If the agent is not registered or obtaining a configuration fails, the
     * initial configuration will be returned.
     * <p>
     * If the configuration service contains the agent configuration, collected
     * {@link Sampler} metrics for this will be posted.
     * </p>
     *
     * @param localConfig Initial agent configuration
     * @param otelConfig    OpenTelemetry configuration
     * @param metrics       Sampler metrics
     *
     * @return the agent configuration
     */
    public AgentConfiguration synchronize(AgentConfiguration localConfig, ConfigProperties otelConfig,
            SamplerMetrics metrics) {
        String configurationServiceUrl = otelConfig.getString("otel.configuration.service.url");
        String apiKey = otelConfig.getString("otel.configuration.service.api.key");
        try {
            HttpGet request = new HttpGet(
                    configurationServiceUrl + "/agent-configuration/" + localConfig.getServiceName());
            request.addHeader("Accept", "application/json");
            request.setHeader("User-Agent", USER_AGENT_HEADER);
            if (apiKey != null)
                request.addHeader(API_KEY_HEADER, apiKey);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        String result = EntityUtils.toString(entity);
                        // read the remote agent configuration
                        AgentConfiguration remoteConfig = objectMapper.readValue(result,
                                AgentConfiguration.class);
                        // while we're at it, post the metrics
                        postMetrics(localConfig.getServiceName(), configurationServiceUrl, apiKey, metrics);
                        // if the local configuration does not have a timestamp,
                        // it is the default version, has not been read from a
                        // file and we should use the remote version
                        if (localConfig.getTimestamp() == 0) {
                            return remoteConfig;
                        }
                        // if the local configuration is newer, it has been read
                        // from a file that is more current, so we should update
                        // the remote configuration
                        if (localConfig.getTimestamp() > remoteConfig.getTimestamp()) {
                            updateRemoteConfiguration(localConfig, configurationServiceUrl, apiKey);
                            return localConfig;
                        }
                        return remoteConfig;
                    }
                } else if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                    selfRegister(localConfig, configurationServiceUrl, apiKey);
                } else {
                    logger.severe("Configuration service connection failed with status code: "
                            + response.getStatusLine().getStatusCode());
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Could not connect to OTEL Configuration Service at " + configurationServiceUrl
                    + ", using sampler \"" + localConfig.getSampler() + "\".", e);
        }
        return localConfig;
    }

    private void updateRemoteConfiguration(AgentConfiguration configuration, String configurationServiceUrl, String apiKey)
            throws UnsupportedCharsetException, JsonProcessingException {
        HttpPut request = new HttpPut(
                configurationServiceUrl + "/agent-configuration/" + configuration.getServiceName());
        if (apiKey != null)
            request.addHeader(API_KEY_HEADER, apiKey);
        request.setHeader("User-Agent", USER_AGENT_HEADER);
        request.setEntity(new StringEntity(objectMapper.writeValueAsString(configuration),
                ContentType.APPLICATION_JSON.withCharset("UTF-8")));
        try (CloseableHttpResponse execute = httpClient.execute(request)) {
            int statusCode = execute.getStatusLine().getStatusCode();
            if (statusCode >= 200 && statusCode < 300) {
                logger.info("Uploading current configuration to the OTEL Configuration Service");
            } else {
                logger.severe(
                        "Uploading current configuration to the OTEL Configuration Service failed with status code: "
                                + statusCode);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to upload current configuration to the OTEL Configuration Service", e);
        }
    }

    private void selfRegister(AgentConfiguration configuration, String configurationServiceUrl, String apiKey)
            throws UnsupportedCharsetException, JsonProcessingException {
            HttpPost postRequest = new HttpPost(configurationServiceUrl + "/agent-configuration");
            if (apiKey != null)
                postRequest.addHeader(API_KEY_HEADER, apiKey);
            postRequest.setHeader("User-Agent", USER_AGENT_HEADER);
            postRequest.setEntity(new StringEntity(objectMapper.writeValueAsString(configuration),
                    ContentType.APPLICATION_JSON.withCharset("UTF-8")));
            try (CloseableHttpResponse execute = httpClient.execute(postRequest)) {
                int statusCode = execute.getStatusLine().getStatusCode();
                if (statusCode >= 200 && statusCode < 300) {
                    logger.info("Self-registered as \"" + configuration.getServiceName()
                            + "\" at the OTEL Configuration Service");
                } else {
                    logger.severe("Self registering failed with status code: " + statusCode);
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to self-register at the OTEL Configuration Service", e);
            }
    }

    private void postMetrics(String serviceName, String configurationServiceUrl, String apiKey, SamplerMetrics metrics)
            throws UnsupportedCharsetException, ClientProtocolException, IOException {
    	if (metrics != null) {
	        HttpPost postRequest = new HttpPost(configurationServiceUrl + "/metrics/" + serviceName);
	        if (apiKey != null)
	            postRequest.addHeader(API_KEY_HEADER, apiKey);
	        postRequest.setHeader("User-Agent", USER_AGENT_HEADER);
	        // XXX: Cannot invoke "no.domstol.otel.trace.samplers.SamplerMetrics.copyAndClear()" because "metrics" is null
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

}
