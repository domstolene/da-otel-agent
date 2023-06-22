/*
 * Copyright Domstoladministrasjonen, Norway
 * SPDX-License-Identifier: Apache-2.0
 */
package no.domstol.otel.agent.configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import io.opentelemetry.api.common.AttributeKey;

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
    private SamplerType sampler = SamplerType.parentbased_always_off;

    @JsonProperty("sampleRatio")
    private Double sampleRatio = 0.0;

    @JsonProperty("readOnly")
    private boolean readOnly = true;

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

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Rule other = (Rule) obj;
            return Objects.equals(exclude, other.exclude) && Objects.equals(include, other.include);
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

    /**
     * Returns a compiled set of rules. This representation is already converted
     * into {@link AttributeKey} and {@link Pattern}, removing the need for doing
     * this conversion during filtering.
     *
     * @return a compiled set of filtering rules
     */
    @JsonIgnore
    public Map<String, List<Map<AttributeKey<String>, Pattern>>> getRules() {
        Map<String, List<Map<AttributeKey<String>, Pattern>>> ruleSets = new HashMap<>();
        if (rules == null) {
            return ruleSets;
        }

        Rule rule = rules.get(0);
        // deal with the excludes
        List<Map<AttributeKey<String>, Pattern>> excludeSet = new ArrayList<>();
        List<Map<String, String>> exclude = rule.getExclude();
        if (exclude != null) {
            for (Map<String, String> map : exclude) {
                Map<AttributeKey<String>, Pattern> ruleGroup = new HashMap<AttributeKey<String>, Pattern>();
                for (String string : map.keySet()) {
                    if (map.get(string) instanceof String) {
                        ruleGroup.put(AttributeKey.stringKey(string), Pattern.compile(map.get(string)));
                    }
                }
                excludeSet.add(ruleGroup);
            }
        }
        ruleSets.put("exclude", excludeSet);

        // deal with the includes
        List<Map<AttributeKey<String>, Pattern>> includeSet = new ArrayList<>();
        List<Map<String, String>> include = rule.getInclude();
        if (include != null) {
            for (Map<String, String> map : include) {
                Map<AttributeKey<String>, Pattern> ruleGroup = new HashMap<AttributeKey<String>, Pattern>();
                for (String string : map.keySet()) {
                    if (map.get(string) instanceof String) {
                        ruleGroup.put(AttributeKey.stringKey(string), Pattern.compile(map.get(string)));
                    }
                }
                includeSet.add(ruleGroup);
            }
        }
        ruleSets.put("include", includeSet);

        return ruleSets;
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

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        AgentConfiguration other = (AgentConfiguration) obj;
        return Objects.equals(rules, other.rules) && Objects.equals(sampleRatio, other.sampleRatio)
                && sampler == other.sampler && Objects.equals(serviceName, other.serviceName);
    }

}
