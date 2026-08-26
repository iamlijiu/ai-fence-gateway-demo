package com.zl.demo.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Demo 配置：上游地址、超时、ZL-OPENID 白名单、围栏开关、降级策略等。
 */
@ConfigurationProperties(prefix = "demo")
public class DemoProperties {

    /** 服务端口 */
    private int serverPort = 8080;

    /** 上游 AI 接口基础地址 */
    private String upstreamBaseUrl = "http://localhost:8080/mock/v1";

    private int connectTimeoutMs = 5000;

    private int readTimeoutMs = 60000;

    private final Openid openid = new Openid();

    private final Fence fence = new Fence();

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public String getUpstreamBaseUrl() {
        return upstreamBaseUrl;
    }

    public void setUpstreamBaseUrl(String upstreamBaseUrl) {
        this.upstreamBaseUrl = upstreamBaseUrl;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public Openid getOpenid() {
        return openid;
    }

    public Fence getFence() {
        return fence;
    }

    /** ZL-OPENID 简单认证配置 */
    public static class Openid {
        private boolean enabled = true;
        private List<String> whitelist = new ArrayList<>();
        private List<String> bypassPaths = new ArrayList<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public List<String> getWhitelist() {
            return whitelist;
        }

        public void setWhitelist(List<String> whitelist) {
            this.whitelist = whitelist;
        }

        public List<String> getBypassPaths() {
            return bypassPaths;
        }

        public void setBypassPaths(List<String> bypassPaths) {
            this.bypassPaths = bypassPaths;
        }
    }

    /** 安全围栏配置 */
    public static class Fence {
        /** 围栏总开关（一键启停） */
        private boolean enabled = true;

        /** 旁路观察模式：true=只检测不执行处置，记录"如执行会命中什么"日志 */
        private boolean dryRun = false;

        /** 降级策略：reject=拒绝请求 / passthrough=透传放行（记录原因） */
        private String degradeStrategy = "passthrough";

        /** 熔断阈值：处理异常率超过此值自动降级（百分比，如 5 表示 5%） */
        private double circuitBreakerThreshold = 5.0;

        /** 熔断统计窗口（秒） */
        private int circuitBreakerWindowSeconds = 60;

        /** 熔断恢复时间（秒） */
        private int circuitBreakerRecoverySeconds = 300;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isDryRun() {
            return dryRun;
        }

        public void setDryRun(boolean dryRun) {
            this.dryRun = dryRun;
        }

        public String getDegradeStrategy() {
            return degradeStrategy;
        }

        public void setDegradeStrategy(String degradeStrategy) {
            this.degradeStrategy = degradeStrategy;
        }

        public double getCircuitBreakerThreshold() {
            return circuitBreakerThreshold;
        }

        public void setCircuitBreakerThreshold(double circuitBreakerThreshold) {
            this.circuitBreakerThreshold = circuitBreakerThreshold;
        }

        public int getCircuitBreakerWindowSeconds() {
            return circuitBreakerWindowSeconds;
        }

        public void setCircuitBreakerWindowSeconds(int circuitBreakerWindowSeconds) {
            this.circuitBreakerWindowSeconds = circuitBreakerWindowSeconds;
        }

        public int getCircuitBreakerRecoverySeconds() {
            return circuitBreakerRecoverySeconds;
        }

        public void setCircuitBreakerRecoverySeconds(int circuitBreakerRecoverySeconds) {
            this.circuitBreakerRecoverySeconds = circuitBreakerRecoverySeconds;
        }
    }
}
