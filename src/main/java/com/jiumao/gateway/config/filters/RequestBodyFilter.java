package com.jiumao.gateway.config.filters;
//package com.zhongshi.gateway.config.filters;
//
//import org.springframework.cloud.gateway.filter.GatewayFilterChain;
//import org.springframework.cloud.gateway.filter.GlobalFilter;
//import org.springframework.core.Ordered;
//import org.springframework.core.io.buffer.DataBuffer;
//import org.springframework.core.io.buffer.DataBufferUtils;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.server.reactive.ServerHttpRequest;
//import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
//import org.springframework.stereotype.Component;
//import org.springframework.web.server.ServerWebExchange;
//import com.zhongshi.factory.BaseResultFactory;
//import reactor.core.publisher.Flux;
//import reactor.core.publisher.Mono;
//
//
//@Component
//public class RequestBodyFilter implements GlobalFilter, Ordered {
//
//	@Override
//    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
//		
//        ServerHttpRequest request = exchange.getRequest();
//
//        String method = request.getMethodValue();
//        
//        HttpHeaders headers = request.getHeaders();
//        
//        if ("POST".equals(method)&&!BaseResultFactory.isNull(headers.getContentType().toString())&&"application/json".equals(headers.getContentType().toString())) {
//        	
//        	Flux<DataBuffer> body = exchange.getRequest().getBody();
//        	
//            return DataBufferUtils.join(body)
//            		
//                    .flatMap(dataBuffer -> {
//                    	
//                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
//                        
//                        dataBuffer.read(bytes);
//                        
//                        exchange.getAttributes().put("param", new String(bytes));
//                        
//                        DataBufferUtils.release(dataBuffer);
//                        
//                        Flux<DataBuffer> cachedFlux = Flux.defer(() -> Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
//
//                        ServerHttpRequest mutatedRequest = new ServerHttpRequestDecorator(exchange.getRequest()) {
//                        	
//                            @Override
//                            public Flux<DataBuffer> getBody() {
//                                return cachedFlux;
//                            }
//                            
//                            @Override
//        					public HttpHeaders getHeaders() {
//        						
//        						long contentLength = headers.getContentLength();
//        						
//        						HttpHeaders httpHeaders = new HttpHeaders();
//        						
//        						httpHeaders.putAll(super.getHeaders());
//        						
//        						if (contentLength > 0) {  
//        							httpHeaders.setContentLength(contentLength);
//        						} else {
//        							httpHeaders.set(HttpHeaders.TRANSFER_ENCODING, "chunked");
//        						}
//        						
//        						return httpHeaders;
//        					}
//                            
//                        };
//                        
//                        return chain.filter(exchange.mutate().request(mutatedRequest).build());
//                        
//                    }).switchIfEmpty(chain.filter(exchange));
//        }
//        
//        return chain.filter(exchange);
//    }
//
//	
//	@Override
//	public int getOrder() {return Ordered.HIGHEST_PRECEDENCE;}
//	
//	
//}