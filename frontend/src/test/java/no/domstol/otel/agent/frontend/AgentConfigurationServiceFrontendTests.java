package no.domstol.otel.agent.frontend;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import no.domstol.otel.agent.service.AgentConfiguration;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class AgentConfigurationServiceFrontendTests {
	@Autowired
	private AgentConfigurationController springController;

	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext context;

	@BeforeEach
	void setup() {
		mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
	}

	@Autowired
	private RestTemplate applicationRestTemplate;

	@Test
	void contextLoads() {
		assertThat(springController).isNotNull();
	}

	@Test
	void listConfigurationsAddsRunningVersionsToModel() {
		RestTemplate restTemplate = new RestTemplate();
		MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
		AgentConfigurationController controller = new AgentConfigurationController(restTemplate, "1.7.7-frontend");
		ExtendedModelMap model = new ExtendedModelMap();

		server.expect(requestTo("http://localhost:8080/agent-configuration"))
				.andRespond(withSuccess("""
						[
						  {
						    "serviceName": "test-service",
						    "sampler": "parentbased_always_off",
						    "sampleRatio": 0.0,
						    "timestamp": 1735689600000
						  }
						]
						""", APPLICATION_JSON));
		server.expect(requestTo("http://localhost:8080/actuator/info"))
				.andRespond(withSuccess("""
						{
						  "build": {
						    "version": "1.7.5"
						  }
						}
						""", APPLICATION_JSON));

		String view = controller.listConfigurations(model);
		List<?> configs = (List<?>) model.getAttribute("configs");

		assertThat(view).isEqualTo("listConfigurations");
		assertThat(model.getAttribute("frontendVersion")).isEqualTo("1.7.7-frontend");
		assertThat(model.getAttribute("serviceVersion")).isEqualTo("1.7.5");
		assertThat(configs).hasSize(1);
		assertThat(((AgentConfiguration) configs.get(0)).getServiceName()).isEqualTo("test-service");
		assertThat((AgentConfiguration.SamplerType[]) model.getAttribute("samplerTypes"))
				.containsExactly(AgentConfiguration.SamplerType.values());
		server.verify();
	}

	@Test
	void listConfigurationsFallsBackToUnknownServiceVersion() {
		RestTemplate restTemplate = new RestTemplate();
		MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
		AgentConfigurationController controller = new AgentConfigurationController(restTemplate, "1.7.7-frontend");
		ExtendedModelMap model = new ExtendedModelMap();

		server.expect(requestTo("http://localhost:8080/agent-configuration"))
				.andRespond(withSuccess("[]", APPLICATION_JSON));
		server.expect(requestTo("http://localhost:8080/actuator/info"))
				.andRespond(withSuccess("{}", APPLICATION_JSON));

		controller.listConfigurations(model);

		assertThat(model.getAttribute("frontendVersion")).isEqualTo("1.7.7-frontend");
		assertThat(model.getAttribute("serviceVersion")).isEqualTo("unknown");
		server.verify();
	}

	@Test
	void listConfigurationsRendersWithRulesWithoutRestrictedThymeleafExpressions() throws Exception {
		MockRestServiceServer server = MockRestServiceServer.createServer(applicationRestTemplate);

		server.expect(requestTo("http://localhost:8080/agent-configuration"))
				.andRespond(withSuccess("""
						[
						  {
						    "serviceName": "test-service",
						    "sampler": "traceidratio",
						    "sampleRatio": 0.5,
						    "timestamp": 1735689600000,
						    "rules": [
						      {
						        "exclude": [
						          {
						            "http.target": "/health/.+",
						            "http.method": "GET"
						          }
						        ]
						      }
						    ]
						  }
						]
						""", APPLICATION_JSON));
		server.expect(requestTo("http://localhost:8080/actuator/info"))
				.andRespond(withSuccess("""
						{
						  "build": {
						    "version": "1.7.5"
						  }
						}
						""", APPLICATION_JSON));

		mockMvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("test-service")))
				.andExpect(content().string(containsString("traceidratio")));
		server.verify();
	}

	@Test
	void configurationDetailsRendersWithoutRestrictedThymeleafExpressions() throws Exception {
		MockRestServiceServer server = MockRestServiceServer.createServer(applicationRestTemplate);

		server.expect(requestTo("http://localhost:8080/agent-configuration/test-service"))
				.andRespond(withSuccess("""
						  {
						    "serviceName": "test-service",
						    "sampler": "parentbased_traceidratio",
						    "sampleRatio": 0.25,
						    "timestamp": 1735689600000,
						    "rules": [
						      {
						        "include": [
						          {
						            "http.method": "POST"
						          }
						        ]
						      }
						    ]
						  }
						""", APPLICATION_JSON));
		server.expect(requestTo("http://localhost:8080/actuator/info"))
				.andRespond(withSuccess("""
						{
						  "build": {
						    "version": "1.7.5"
						  }
						}
						""", APPLICATION_JSON));

		mockMvc.perform(get("/test-service"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("parentbased_traceidratio")))
				.andExpect(content().string(containsString("http.method")));
		server.verify();
	}

}
