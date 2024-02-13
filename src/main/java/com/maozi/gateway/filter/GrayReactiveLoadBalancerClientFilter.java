package com.maozi.gateway.filter;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_SCHEME_PREFIX_ATTR;

import com.maozi.common.BaseCommon;
import com.maozi.discovery.balancer.GrayRoundRobinLoadBalancer;
import java.net.URI;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultRequest;
import org.springframework.cloud.client.loadbalancer.LoadBalancerUriTools;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.gateway.config.GatewayLoadBalancerProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.ReactiveLoadBalancerClientFilter;
import org.springframework.cloud.gateway.support.DelegatingServiceInstance;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;


@Component
public class GrayReactiveLoadBalancerClientFilter extends ReactiveLoadBalancerClientFilter {

    private final LoadBalancerClientFactory grayClientFactory;

    public GrayReactiveLoadBalancerClientFilter(LoadBalancerClientFactory clientFactory, GatewayLoadBalancerProperties properties) {
        super(clientFactory, properties);
        this.grayClientFactory = clientFactory;
    }


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        URI url = (URI) exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);

        String schemePrefix = exchange.getAttribute(GATEWAY_SCHEME_PREFIX_ATTR);

        if (BaseCommon.isNull(url) || (!"lb".equals(url.getScheme()) && !"lb".equals(schemePrefix))) {
            return chain.filter(exchange);
        }

        ServerWebExchangeUtils.addOriginalRequestUrl(exchange, url);

        return this.choose(exchange).doOnNext((response) -> {

            if (!response.hasServer()) {
                throw NotFoundException.create(true, "服务不存在");
            }

            URI uri = exchange.getRequest().getURI();

            String overrideScheme = null;

            if (schemePrefix != null) {
                overrideScheme = url.getScheme();
            }

            DelegatingServiceInstance serviceInstance = new DelegatingServiceInstance((ServiceInstance) response.getServer(), overrideScheme);

            URI requestUrl = this.reconstructURI(serviceInstance, uri);

            exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR, requestUrl);

        }).then(chain.filter(exchange));

    }

    private Mono<Response<ServiceInstance>> choose(ServerWebExchange exchange) {
        URI uri = (URI) exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
        GrayRoundRobinLoadBalancer loadBalancer = new GrayRoundRobinLoadBalancer(grayClientFactory.getLazyProvider(uri.getHost(), ServiceInstanceListSupplier.class));
        return loadBalancer.choose(this.createRequest(exchange));
    }

    private Request createRequest(ServerWebExchange exchange) {
        HttpHeaders headers = exchange.getRequest().getHeaders();
        return new DefaultRequest<>(headers);
    }

    protected URI reconstructURI(ServiceInstance serviceInstance, URI original) {
        return LoadBalancerUriTools.reconstructURI(serviceInstance, original);
    }

}