package com.zl.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 基础设施配置（MVC 版）
 * <p>
 * 1) RestTemplate —— 非流式转发（阻塞式，与现有网关一致）
 * 2) WebClient —— 流式转发（Reactor Netty 连接器，仅客户端用，不作为服务器）
 * </p>
 * 注意：不再需要 TomcatReactiveWebServerFactory！
 * MVC 模式下 Tomcat 自动作为 HTTP 服务器（spring-boot-starter-web 默认行为）。
 */
@Configuration
public class HttpClientConfig {

    /** 非流式转发客户端（阻塞式） */
    @Bean
    public RestTemplate restTemplate(DemoProperties props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.getConnectTimeoutMs());
        factory.setReadTimeout(props.getReadTimeoutMs());
        return new RestTemplate(factory);
    }

    /** 流式转发客户端（响应式，Reactor Netty 连接器自动引入） */
    @Bean
    public WebClient webClient(DemoProperties props) {
        return WebClient.builder()
                .baseUrl(props.getUpstreamBaseUrl())
                .build();
    }
}
