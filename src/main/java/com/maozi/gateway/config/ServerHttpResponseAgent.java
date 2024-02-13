package com.maozi.gateway.config;

import static com.maozi.common.BaseCommon.appendLog;
import static com.maozi.common.BaseCommon.log;

import com.maozi.common.BaseCommon;
import com.maozi.utils.MapperUtils;
import com.maozi.utils.context.ApplicationEnvironmentContext;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import org.jboss.logging.MDC;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class ServerHttpResponseAgent extends ServerHttpResponseDecorator {
	
	private Long requestTime;
	
	private Map<String,Object> attributes;
	
	private Map<String,String> logs;

	public ServerHttpResponseAgent(Long requestTime,Map<String,String> logs,ServerHttpResponse response,Map<String,Object> attributes) {
		
		super(response);
		
		this.requestTime=requestTime;
		
		this.logs=logs;
		
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
	            
	            
	            Map result = MapperUtils.jsonToPojo(new String(outputStream.toByteArray()), Map.class);
				
				Boolean customResultBoo=false;
				
				Object tid = attributes.get("TID");
				
				if(!BaseCommon.isNull(result) && result.containsKey("code") && result.containsKey("success")) {
					customResultBoo=true;
					result.put("id",tid);
				}
				
				byte[] resultByte = (customResultBoo ? MapperUtils.mapToJson(result).getBytes() : outputStream.toByteArray());

				MDC.put("TID",tid);
				MDC.put("serviceName", ApplicationEnvironmentContext.applicationName);
				
				logs.put("RT", System.currentTimeMillis() - requestTime+" ms");
				logs.put("Data", new String(resultByte));
				
				
				if(getDelegate().getRawStatusCode() != 200) {
					log.error(appendLog(logs).toString());
				}else if(!BaseCommon.isNull(result) && !BaseCommon.isNull(result.get("code")) && !"200".equals(result.get("code").toString())){
					log.error(appendLog(logs).toString());
				}else{
					log.info(appendLog(logs).toString());
				}

				getDelegate().getHeaders().setContentLength(resultByte.length);
				
				try {return bufferFactory.wrap(resultByte); } catch (Exception e) {return null;}finally {try {outputStream.close();} catch (IOException e) {}MDC.clear();}
				
	          }));

		}

		return super.writeWith(body);

	}

}