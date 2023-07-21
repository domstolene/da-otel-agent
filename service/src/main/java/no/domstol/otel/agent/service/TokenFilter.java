/*
 * Copyright Domstoladministrasjonen, Norway
 * SPDX-License-Identifier: Apache-2.0
 */
package no.domstol.otel.agent.service;
import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class TokenFilter implements Filter {

    private static final String API_KEY_HEADER = "X-API-KEY";

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String property = System.getProperty("otel.configuration.service.api.key");

        String token = request.getHeader(API_KEY_HEADER);

        if (property != null && (token == null || !validateToken(token))) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }

    private boolean validateToken(String token) {
        return token.equals(System.getProperty("otel.configuration.service.api.key"));
    }
}
