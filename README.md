# Nacos配置

<br/>

**Nacos地址默认为localhost:8081，若不是则添加环境变量NACOS_CONFIG_SERVER**

或找到 src/main/resources/bootstrap.properties 添加

```
spring.cloud.nacos.config.server-addr=localhost:8081
```

<br/>

## 配置说明

<br/>

Nacos搜索配置：maozi-cloud-gateway.yml

```yaml
application-port: 10000

application-project-name: gateway

spring: 
  cloud:
    sentinel:
      filter:
        enabled: false
      datasource:
        gw-flow:
          nacos:
            server-addr: ${spring.cloud.nacos.config.server-addr}
            dataId: ${spring.application.name}-gw-flow
            groupId: SENTINEL_GROUP
            rule-type: gw-flow
        gw-api-group:
          nacos:
            server-addr: ${spring.cloud.nacos.config.server-addr}
            dataId: ${spring.application.name}-gw-api-group
            groupId: SENTINEL_GROUP
            rule-type: gw-api-group
    gateway:
      discovery:
        locator:
          enabled: true
      default-filters:
      - StripPrefix=1
      - name: Retry
        args:
          retries: 1
          methods: GET,POST
          statuses: BAD_GATEWAY,GATEWAY_TIMEOUT,SERVICE_UNAVAILABLE
          series: INFORMATIONAL
          exceptions: java.net.ConnectException,org.springframework.cloud.gateway.support.NotFoundException


      routes: 
        - id: MAOZI-CLOUD-USER
          uri: lb://maozi-cloud-user
          predicates:
          - Path=/user/**

        - id: MAOZI-CLOUD-OAUTH
          uri: lb://maozi-cloud-oauth
          predicates:
          - Path=/oauth/**
          
```

**有新应用添加需在routes里添加即可**

<br/>

# 即可启动

<br/>

请求 localhost:10000/doc.html 进入接口文档

<br/>

**前提要开启：**

https://gitee.com/xmaozi/maozi-cloud-user  或

https://gitee.com/xmaozi/maozi-cloud-oauth

不然会报错找不到，看到user模块的文档即可成功