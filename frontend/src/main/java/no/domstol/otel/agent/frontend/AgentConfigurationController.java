package no.domstol.otel.agent.frontend;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestTemplate;

import no.domstol.otel.agent.service.AgentConfiguration;

@Controller
public class AgentConfigurationController {

    @Autowired
    private RestTemplate restTemplate;

    @GetMapping("/")
    public String listConfigurations(Model model) {
        String url = "http://localhost:8080/agent-configuration";
        AgentConfiguration[] configs = restTemplate.getForObject(url, AgentConfiguration[].class);
        model.addAttribute("configs", Arrays.asList(configs));
        return "listConfigurations";
    }
}