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

package com.jiumao.gateway.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jboss.logging.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ServerWebExchange;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.jiumao.factory.BaseResultFactory;
import com.jiumao.factory.result.AbstractBaseResult;
import com.jiumao.gateway.config.tool.RequestTool;
import com.jiumao.tool.ApplicationEnvironmentConfig;

import reactor.core.publisher.Mono;

/**
 * 
 * 功能说明：网关错误映射器
 * 
 * 功能作者：彭晋龙 ( 联系方式QQ/微信：1095071913 )
 *
 * 创建日期：2019-10-31 ：20:16:00
 *
 * 版权归属：蓝河团队
 *
 * 协议说明：Apache2.0（ 文件顶端 ）
 *
 */

public class GatewayExceptionHandler extends BaseResultFactory implements ErrorWebExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GatewayExceptionHandler.class);

	private List<HttpMessageReader<?>> messageReaders = Collections.emptyList();

	private List<HttpMessageWriter<?>> messageWriters = Collections.emptyList();

	private List<ViewResolver> viewResolvers = Collections.emptyList();

	private ThreadLocal<AbstractBaseResult> exceptionHandlerResult = new ThreadLocal<>();

	public void setMessageReaders(List<HttpMessageReader<?>> messageReaders) {
		Assert.notNull(messageReaders, "'messageReaders' must not be null");
		this.messageReaders = messageReaders;
	}

	public void setViewResolvers(List<ViewResolver> viewResolvers) {
		this.viewResolvers = viewResolvers;
	}

	public void setMessageWriters(List<HttpMessageWriter<?>> messageWriters) {
		Assert.notNull(messageWriters, "'messageWriters' must not be null");
		this.messageWriters = messageWriters;
	}

	protected Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
		AbstractBaseResult result = exceptionHandlerResult.get();
		return ServerResponse.status(result.getHttpCode()).contentType(MediaType.APPLICATION_JSON_UTF8)
				.body(BodyInserters.fromObject(result));
	}

	private Mono<? extends Void> write(ServerWebExchange exchange, ServerResponse response) {
		exchange.getResponse().getHeaders().setContentType(response.headers().getContentType());
		return response.writeTo(exchange, new ResponseContext());
	}

	private class ResponseContext implements ServerResponse.Context {
		@Override
		public List<HttpMessageWriter<?>> messageWriters() {
			return GatewayExceptionHandler.this.messageWriters;
		}

		@Override
		public List<ViewResolver> viewResolvers() {
			return GatewayExceptionHandler.this.viewResolvers;
		}
	}

	@Override
	public Mono<Void> handle(ServerWebExchange exchange, Throwable e) {
		
		AbstractBaseResult result = null;

		ServerHttpRequest request = exchange.getRequest();
		
		MDC.put("tid",exchange.getAttributes().get("TID"));
		MDC.put("applicationName", ApplicationEnvironmentConfig.applicationName);
		
		Map<String,String> logs = new LinkedHashMap<String, String>();
		
		logs.put("reqType", "gateway");
		logs.put("reqIp", RequestTool.getIpAddress(request)+" net");
		logs.put("reqUrl", request.getURI().toString());
		logs.put("reqMethod", request.getMethod().toString());
        logs.put("errorDesc", e.getLocalizedMessage());
        
        
        log.error(getStackTrace(e));

		if (e instanceof NotFoundException) {
			result = error(code(404),404);
		}else if(BlockException.isBlockException(e)){
			result =error(code(601),500);
		}else {
			result = error(code(500),500);
		}
		
		logs.put("respData", result.toString());
		
		log.error(BaseResultFactory.appendLog(logs).toString());
		
		if (exchange.getResponse().isCommitted()) {
			return Mono.error(e);
		}
		
		MDC.clear();
		
		exceptionHandlerResult.set(result);
		ServerRequest newRequest = ServerRequest.create(exchange, this.messageReaders);
		return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse).route(newRequest)
				.switchIfEmpty(Mono.error(e)).flatMap((handler) -> handler.handle(newRequest))
				.flatMap((response) -> write(exchange, response));
	}

}