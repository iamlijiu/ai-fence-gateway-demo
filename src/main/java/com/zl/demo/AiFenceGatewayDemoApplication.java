package com.zl.demo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.zl.demo.config.DemoProperties;

/**
 * AI 安全围栏网关 Demo
 * <p>技术栈与现有网关对齐：Spring Boot 2.0.8 + WebFlux 5.0.5 + Tomcat。
 * 提供 OpenAI 兼容的 POST /v1/chat/completions 单接口，按请求体 stream 标志分流：
 * stream=true  → WebClient 转发 + SSE 流式返回；
 * stream=false → RestTemplate 转发 + 一次性 JSON 返回。</p>
 */
@SpringBootApplication
@EnableConfigurationProperties(DemoProperties.class)
@MapperScan("com.zl.demo.fence.mapper")
public class AiFenceGatewayDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiFenceGatewayDemoApplication.class, args);
    }
}
