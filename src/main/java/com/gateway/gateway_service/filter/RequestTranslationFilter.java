package com.gateway.gateway_service.filter;

import com.gateway.gateway_service.decorator.RequestDecoratorFactory;
import com.gateway.gateway_service.model.GatewayRequest;
import com.gateway.gateway_service.utils.RequestBodyExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;


@Component
@RequiredArgsConstructor
@Slf4j
public class RequestTranslationFilter implements GlobalFilter {

    private final RequestBodyExtractor requestBodyExtractor;
    private final RequestDecoratorFactory requestDecoratorFactory;


    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain) {

        // By default, set the response status to 400. This will be overridden if the request is valid.
        exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);

        // Simple check to see if the request has a content type and is a POST request
        if (exchange.getRequest().getHeaders().getContentType() == null || !exchange.getRequest().getMethod().equals(HttpMethod.POST)) {
            log.info("Request does not have a content type or is not a POST request");
            return exchange.getResponse().setComplete();
        } else {
            return DataBufferUtils.join(exchange.getRequest().getBody())
                    .flatMap(dataBuffer -> {
                        GatewayRequest request = requestBodyExtractor.getRequest(exchange, dataBuffer);
                        ServerHttpRequest mutatedRequest = requestDecoratorFactory.getDecorator(request);
                        //RouteToRequestUrlFilter writes the URI to the exchange attributes *before* any global filters run.
                        exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR, mutatedRequest.getURI());
                        if(request.getQueryParams() != null) {
                            request.getQueryParams().clear();
                        }
                        log.info("Proxying request: {} {}", mutatedRequest.getMethod(), mutatedRequest.getURI());
                        return chain.filter(exchange.mutate().request(mutatedRequest).build());
                    });
        }
    }
}