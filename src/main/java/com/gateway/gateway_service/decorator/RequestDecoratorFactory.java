package com.gateway.gateway_service.decorator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateway.gateway_service.model.GatewayRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class RequestDecoratorFactory {

    private final ObjectMapper objectMapper;

    public ServerHttpRequestDecorator getDecorator(GatewayRequest request) {
        return switch (request.getTargetMethod().name().toUpperCase()) {
            case "GET" -> new GetRequestDecorator(request);
            case "PUT" -> new PutRequestDecorator(request, objectMapper);
            default -> throw new IllegalArgumentException("Invalid http method");
        };
    }
}