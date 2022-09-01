package com.jiumao.gateway.config;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import org.jboss.logging.MDC;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;

import com.jiumao.factory.BaseResultFactory;
import com.jiumao.tool.ApplicationEnvironmentConfig;
import com.jiumao.tool.MapperUtils;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
public class ServerHttpResponseAgent extends ServerHttpResponseDecorator{
	
	private Long requestTime;
	
	private ServerHttpRequest request;
	
	private Map<String,Object> attributes;
	
	private Map<String,String> logs;

	public ServerHttpResponseAgent(Long requestTime,Map<String,String> logs,ServerHttpRequest request,ServerHttpResponse response,Map<String,Object> attributes) {
		
		super(response);
		
		this.requestTime=requestTime;
		
		this.logs=logs;
		
		this.request=request;
		
		this.attributes=attributes;
		
	}
	
	
	
	@Override
	public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
		
		if (body instanceof Flux) {
			
			DataBufferFactory bufferFactory = getDelegate().bufferFactory();
			
			Flux<? extends DataBuffer> fluxBody = (Flux<? extends DataBuffer>) body;
			
			return super.writeWith(fluxBody.buffer().map(dataBuffers -> {
				
	            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
	            
	            dataBuffers.forEach(i -> {
	            	
	              byte[] array = new byte[i.readableByteCount()];
	              
	              i.read(array);
	              
	              try {outputStream.write(array);} catch (IOException e) {}
	              
	              DataBufferUtils.release(i); 
	              
	            });
	            
	            
	            Map result = null;  
	            
				try {result = MapperUtils.json2pojo(new String(outputStream.toByteArray()), Map.class);} catch (Exception e) {}
				
				Long returnTime=System.currentTimeMillis()-requestTime; 

				
				
				Boolean customResultBoo=false;
				
				Object tid = attributes.get("TID");
				
				if(!BaseResultFactory.isNull(result) && result.containsKey("code") && result.containsKey("success")) {	
					customResultBoo=true;
					result.put("id",tid);
				}
				
				
				
				byte[] resultByte=(customResultBoo?MapperUtils.mapToJson(result).getBytes():outputStream.toByteArray());
				
				
				MDC.put("tid",tid);
				MDC.put("applicationName", ApplicationEnvironmentConfig.applicationName);
				
				logs.put("respTime", returnTime.toString()+" ms");
				logs.put("respData", new String(resultByte));
				
				
				if(getDelegate().getRawStatusCode()!=200) {
					log.error(BaseResultFactory.appendLog(logs).toString()); 
				}else {  
					if(!BaseResultFactory.isNull(result) && !BaseResultFactory.isNull(result.get("code")) && !"200".equals(result.get("code").toString())) {
						log.error(BaseResultFactory.appendLog(logs).toString());
					}else {
						log.info(BaseResultFactory.appendLog(logs).toString());
					}
				}
				getDelegate().getHeaders().setContentLength(resultByte.length);
				
				try {return bufferFactory.wrap(resultByte); } catch (Exception e) {return null;}finally {try {outputStream.close();} catch (IOException e) {}MDC.clear();}
				
	          })); 
		}
		return super.writeWith(body);
	}
	
	
	

}
