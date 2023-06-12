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

package com.maozi.gateway.config.filters;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.maozi.common.BaseCommon;
import com.maozi.gateway.config.ServerHttpResponseAgent;
import com.maozi.gateway.config.utils.RequestTool;

import reactor.core.publisher.Mono;

/**
 * 
 * 功能说明：网关日志收集
 * 
 * 功能作者：彭晋龙 ( 联系方式QQ/微信：1095071913 )
 *
 * 创建日期：2019-08-05 ：15:56:00
 *
 * 版权归属：蓝河团队
 *
 * 协议说明：Apache2.0（ 文件顶端 ）
 *
 */

@Component
public class RequestLogFilter extends BaseCommon implements GlobalFilter, Ordered {
	
	
	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) { 
		
		Long requestTime = System.currentTimeMillis();
		
		Map<String,String> logs = new LinkedHashMap<String, String>();
		
		logs.put("reqIp", RequestTool.getIpAddress(exchange.getRequest())+" net");
		logs.put("reqType", "gateway");
		logs.put("reqUrl", exchange.getRequest().getURI().toString());
		logs.put("reqMethod", exchange.getRequest().getMethod().toString());
		
		return chain.filter(exchange.mutate().response(new ServerHttpResponseAgent(requestTime, logs, exchange.getRequest(), exchange.getResponse(),exchange.getAttributes())).build());
		
	}
	
	
	@Override
	public int getOrder() {return Ordered.HIGHEST_PRECEDENCE+1;}
	
	

}