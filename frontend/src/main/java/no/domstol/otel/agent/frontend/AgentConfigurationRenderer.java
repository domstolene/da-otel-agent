/*
 * Copyright 2025 Domstoladministrasjonen, Norway
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
package no.domstol.otel.agent.frontend;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import no.domstol.otel.agent.service.AgentConfiguration;
import no.domstol.otel.agent.service.AgentConfiguration.Rule;

@Component("configurationRenderer")
public class AgentConfigurationRenderer {

	/**
	 * Pretty prints the rules as YAML.
	 * 
	 * @return
	 */
	public String getFormattedRules(AgentConfiguration config) {

		List<Rule> rules = config.getRules();

		if (rules == null || rules.isEmpty()) {
			return "";
		}

		StringBuilder sb = new StringBuilder();
		for (Rule rule : rules) {

			if (rule.getExclude() != null && !rule.getExclude().isEmpty()) {
				sb.append("exclude:\n");
				for (Map<String, String> set : rule.getExclude()) {
					sb.append("- ");
					boolean first = true;
					for (Map.Entry<String, String> entry : set.entrySet()) {
						String key = entry.getKey();
						String val = entry.getValue();
						if (!first)
							sb.append("  ");
						sb.append(key);
						sb.append(": ");
						sb.append(val);
						sb.append("\n");
						first = false;
					}
				}
			}
			if (rule.getInclude() != null && !rule.getInclude().isEmpty()) {
				sb.append("include:\n");
				for (Map<String, String> set : rule.getInclude()) {
					sb.append("  -");
					boolean first = true;
					for (Map.Entry<String, String> entry : set.entrySet()) {
						String key = entry.getKey();
						String val = entry.getValue();
						if (!first)
							sb.append("  ");
						sb.append(key);
						sb.append(": ");
						sb.append(val);
						sb.append("\n");
						first = false;
					}
				}
			}
		}
		return sb.toString();
	}

}
