package com.maozi.gateway.config.tool;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Flux;

public class RequestTool {
	
	
	public static String getIpAddress(ServerHttpRequest request) {
	    HttpHeaders headers = request.getHeaders();
	    String ip = headers.getFirst("x-forwarded-for");
	    if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
	        if (ip.indexOf(",") != -1) {
	            ip = ip.split(",")[0];
	        }
	    }
	    if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
	        ip = headers.getFirst("Proxy-Client-IP");
	    }
	    if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
	        ip = headers.getFirst("WL-Proxy-Client-IP");
	    }
	    if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
	        ip = headers.getFirst("HTTP_CLIENT_IP");
	    }
	    if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
	        ip = headers.getFirst("HTTP_X_FORWARDED_FOR");
	    }
	    if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
	        ip = headers.getFirst("X-Real-IP");
	    }
	    if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) { 
	        ip = request.getRemoteAddress().getAddress().getHostAddress();
	    }
	    return ip.equals("0:0:0:0:0:0:0:1")?"127.0.0.1":ip; 
	}

	public static String getBody(Flux<DataBuffer> body) {
        StringBuilder paramData=new StringBuilder();
        body.subscribe(buffer -> {
            byte[] bytes = new byte[buffer.readableByteCount()];
            buffer.read(bytes);
            DataBufferUtils.release(buffer);
            paramData.append(new String(bytes));
        });
        return paramData.toString();
    }
	
}
