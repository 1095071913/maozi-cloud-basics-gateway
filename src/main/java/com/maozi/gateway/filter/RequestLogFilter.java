/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package com.maozi.gateway.filter;

import com.maozi.common.BaseCommon;
import com.maozi.gateway.config.ServerHttpResponseAgent;
import com.maozi.gateway.utils.RequestUtils;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class RequestLogFilter extends BaseCommon implements GlobalFilter, Ordered {
	
	
	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) { 
		
		Long requestTime = System.currentTimeMillis();
		
		Map<String,String> logs = new LinkedHashMap<String, String>();
		
		logs.put("IP", RequestUtils.getIpAddress(exchange.getRequest()));
		logs.put("Type", "Gateway");
		logs.put("URI", exchange.getRequest().getURI().toString());
		logs.put("Method", exchange.getRequest().getMethod().toString());
		
		return chain.filter(exchange.mutate().response(new ServerHttpResponseAgent(requestTime, logs,exchange.getResponse(),exchange.getAttributes())).build());
		
	}
	
	
	@Override
	public int getOrder() {return Ordered.HIGHEST_PRECEDENCE+1;}
	
	

}