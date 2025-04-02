package no.domstol.otel.agent.frontend;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.client.RestTemplate;

import no.domstol.otel.agent.service.AgentConfiguration;

@Controller
public class AgentConfigurationController {

    @Autowired
    private RestTemplate restTemplate;

    @GetMapping("/")
    public String listConfigurations(Model model) {
        // default value is for local development
        String internalURL = System.getProperty("otel.configuration.service.url", "http://localhost:8080");
        String publicURL = System.getProperty("otel.configuration.public.url", "http://localhost:8080");
        String jaegerURL = System.getProperty("otel.configuration.jaeger.url", "http://localhost:16686");
        AgentConfiguration[] configs = restTemplate.getForObject(internalURL + "/agent-configuration",
                AgentConfiguration[].class);
        model.addAttribute("serviceInternalURL", internalURL + "/agent-configuration");
        model.addAttribute("servicePublicURL", publicURL + "/agent-configuration");
        model.addAttribute("jaegerURL",jaegerURL);
        model.addAttribute("configs", Arrays.asList(configs));
        return "listConfigurations";
    }

    @GetMapping("/{configName}")
    public String configurationDetails(@PathVariable("configName") String configName, Model model) {
        // default value is for local development
        String internalURL = System.getProperty("otel.configuration.service.url", "http://localhost:8080");
        String publicURL = System.getProperty("otel.configuration.public.url", "http://localhost:8080");
        AgentConfiguration config = restTemplate.getForObject(internalURL + "/agent-configuration/" + configName,
                AgentConfiguration.class);
        model.addAttribute("name", configName);
        model.addAttribute("servicePublicURL", publicURL + "/agent-configuration");
        model.addAttribute("config", config);
        return "configurationDetails";
    }

}