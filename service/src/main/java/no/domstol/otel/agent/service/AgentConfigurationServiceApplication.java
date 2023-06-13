/*
 * Copyright Domstoladministrasjonen, Norway
 * SPDX-License-Identifier: Apache-2.0
 */
package no.domstol.otel.agent.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the OTEL Dynamic Agent Configuration Service.
 *
 * @since 1.0
 */
@SpringBootApplication
public class AgentConfigurationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AgentConfigurationServiceApplication.class, args);
	}

}
