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

package com.maozi.gateway.config;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.maozi.common.BaseCommon;
import com.maozi.common.result.code.CodeAttribute;
import com.maozi.common.result.error.ErrorResult;
import com.maozi.gateway.utils.RequestUtils;
import com.maozi.utils.context.ApplicationEnvironmentContext;
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
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class GatewayExceptionHandler extends BaseCommon implements ErrorWebExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GatewayExceptionHandler.class);

	private List<HttpMessageReader<?>> messageReaders = Collections.emptyList();

	private List<HttpMessageWriter<?>> messageWriters = Collections.emptyList();

	private List<ViewResolver> viewResolvers = Collections.emptyList();

	private ThreadLocal<ErrorResult> exceptionHandlerResult = new ThreadLocal<>();

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
		ErrorResult result = exceptionHandlerResult.get();
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

		ServerHttpRequest request = exchange.getRequest();

		MDC.put("tid",exchange.getAttributes().get("TID"));
		MDC.put("applicationName", ApplicationEnvironmentContext.applicationName);
		
		Map<String,String> logs = new LinkedHashMap<String, String>();
		
		logs.put("Type", "Gateway");
		logs.put("IP", RequestUtils.getIpAddress(request));
		logs.put("URI", request.getURI().toString());
		logs.put("Method", request.getMethod().toString());
        logs.put("ErrorDesc", e.getLocalizedMessage());
        
        
        log.error(getStackTrace(e));

		ErrorResult result = null;

		if (e instanceof NotFoundException) {
			result = error(new CodeAttribute<>(404,"服务不存在"),404);
		}else if(BlockException.isBlockException(e)){
			result = error(new CodeAttribute<>(429,"限流中"),429);
		}else {
			result = error(new CodeAttribute<>(500,"内部服务错误"),500);
		}
		
		logs.put("Data", result.toString());
		
		log.error(BaseCommon.appendLog(logs).toString());
		
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