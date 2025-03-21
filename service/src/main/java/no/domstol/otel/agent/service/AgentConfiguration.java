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

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

/**
 * Represents a sampler configuration as serviced by the Agent Configuration
 * Service or read from a YAML-file.
 *
 * @since 1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgentConfiguration {

    public enum SamplerType {
        always_on, always_off, traceidratio, parentbased_always_on, parentbased_always_off, parentbased_traceidratio,
    }

    @JsonProperty("serviceName")
    private String serviceName;

    @JsonProperty("sampler")
    private SamplerType sampler;

    @JsonProperty("sampleRatio")
    private Double sampleRatio = 0.0;

    @JsonProperty("readOnly")
    private boolean readOnly = false;

    @JsonProperty("timestamp")
    private long timestamp = 0;

    @JsonProperty("rules")
    private List<Rule> rules;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Rule {
        @JsonProperty("exclude")
        private List<Map<String, String>> exclude;

        @JsonProperty("include")
        private List<Map<String, String>> include;

        public List<Map<String, String>> getExclude() {
            return exclude;
        }

        public void setExclude(List<Map<String, String>> exclude) {
            this.exclude = exclude;
        }

        public List<Map<String, String>> getInclude() {
            return include;
        }

        public void setInclude(List<Map<String, String>> include) {
            this.include = include;
        }

    }

    public AgentConfiguration() {
    }

    public AgentConfiguration(String serviceName) {
        this();
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public double getSampleRatio() {
        return sampleRatio;
    }

    public void setSampleRatio(double sampleRatio) {
        this.sampleRatio = sampleRatio;
    }

    public SamplerType getSampler() {
        return sampler;
    }

    public void setSampler(SamplerType sampler) {
        this.sampler = sampler;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public String toString() {
        ObjectMapper jsonMapper = new ObjectMapper();
        ObjectWriter writer = jsonMapper.writerWithDefaultPrettyPrinter();
        try {
            return writer.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

	public List<Rule> getRules() {
		return rules;
	}

	public void setRules(List<Rule> rules) {
		this.rules = rules;
	}

}
