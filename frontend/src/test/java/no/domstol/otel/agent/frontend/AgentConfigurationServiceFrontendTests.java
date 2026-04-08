package no.domstol.otel.agent.frontend;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import no.domstol.otel.agent.service.AgentConfiguration;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@SpringBootTest
class AgentConfigurationServiceFrontendTests {
	@Autowired
	private AgentConfigurationController springController;

	@Test
	void contextLoads() {
		assertThat(springController).isNotNull();
	}

	@Test
	void listConfigurationsAddsRunningVersionsToModel() {
		RestTemplate restTemplate = new RestTemplate();
		MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
		AgentConfigurationController controller = new AgentConfigurationController(restTemplate, "1.7.6-frontend");
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
		assertThat(model.getAttribute("frontendVersion")).isEqualTo("1.7.6-frontend");
		assertThat(model.getAttribute("serviceVersion")).isEqualTo("1.7.5");
		assertThat(configs).hasSize(1);
		assertThat(((AgentConfiguration) configs.get(0)).getServiceName()).isEqualTo("test-service");
		server.verify();
	}

	@Test
	void listConfigurationsFallsBackToUnknownServiceVersion() {
		RestTemplate restTemplate = new RestTemplate();
		MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
		AgentConfigurationController controller = new AgentConfigurationController(restTemplate, "1.7.6-frontend");
		ExtendedModelMap model = new ExtendedModelMap();

		server.expect(requestTo("http://localhost:8080/agent-configuration"))
				.andRespond(withSuccess("[]", APPLICATION_JSON));
		server.expect(requestTo("http://localhost:8080/actuator/info"))
				.andRespond(withSuccess("{}", APPLICATION_JSON));

		controller.listConfigurations(model);

		assertThat(model.getAttribute("frontendVersion")).isEqualTo("1.7.6-frontend");
		assertThat(model.getAttribute("serviceVersion")).isEqualTo("unknown");
		server.verify();
	}

}
