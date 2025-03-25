package no.domstol.otel.agent.frontend;

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

		StringBuilder sb = new StringBuilder();
		for (Rule rule : config.getRules()) {
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
