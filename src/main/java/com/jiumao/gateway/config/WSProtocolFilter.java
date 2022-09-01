//package com.jiumao.gateway.config;
///*
// * Copyright 2012-2018 the original author or authors.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *      https://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// * 
// */
//
//
//
//import java.util.List;
//
//import org.springframework.cloud.gateway.filter.GatewayFilter;
//import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
//import org.springframework.stereotype.Component;
//
//import com.jiumao.factory.BaseResultFactory;
//
///**
// * 
// * 功能说明：网关路由局部拦截
// * 
// * 功能作者：彭晋龙 ( 联系方式QQ/微信：1095071913 )
// *
// * 创建日期：2019-08-14 ：20:09:00
// *
// * 版权归属：蓝河团队
// *
// * 协议说明：Apache2.0（ 文件顶端 ）
// *
// */
//
//@Component
//public class WSProtocolFilter extends AbstractGatewayFilterFactory {
//
//	@Override
//	public GatewayFilter apply(Object config) {
//		return (exchange, chain) -> {
//			
//			List<String> list = exchange.getRequest().getHeaders().get("sec-websocket-protocol");
//			
//			if(BaseResultFactory.isNotNull(list)) {
////				exchange.getResponse().getHeaders().set("sec-websocket-protocol", list.get(0));
//			}
//			
//			return chain.filter(exchange);
//		};
//	}
//}