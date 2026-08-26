package com.zl.demo.fence.api;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zl.demo.config.DemoProperties;
import com.zl.demo.fence.degrade.CircuitBreaker;
import com.zl.demo.fence.monitor.MetricsCounter;

/**
 * 监控统计 API（P0-3，对应 3.3 统计查询接口）
 * <p>
 * 提供围栏运行指标查询、熔断器状态查询与应急重置能力。
 * 一期以本地计数实现，二期可平移到 Prometheus 指标暴露。
 * </p>
 */
@RestController
@RequestMapping("/api/v1")
public class MetricsController {

    private final MetricsCounter metricsCounter;
    private final CircuitBreaker circuitBreaker;
    private final DemoProperties props;

    public MetricsController(MetricsCounter metricsCounter, CircuitBreaker circuitBreaker,
                             DemoProperties props) {
        this.metricsCounter = metricsCounter;
        this.circuitBreaker = circuitBreaker;
        this.props = props;
    }

    /**
     * 查询围栏运行统计
     */
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("message", "success");
        result.put("data", metricsCounter.snapshot());
        return ResponseEntity.ok(result);
    }

    /**
     * 重置围栏运行统计（应急用）
     */
    @PostMapping("/metrics/reset")
    public ResponseEntity<Map<String, Object>> resetMetrics() {
        metricsCounter.reset();
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("message", "统计计数器已重置");
        return ResponseEntity.ok(result);
    }

    /**
     * 查询熔断器状态
     */
    @GetMapping("/circuit-breaker")
    public ResponseEntity<Map<String, Object>> getCircuitBreaker() {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("message", "success");
        result.put("data", circuitBreaker.snapshot());
        return ResponseEntity.ok(result);
    }

    /**
     * 重置熔断器（应急用）
     */
    @PostMapping("/circuit-breaker/reset")
    public ResponseEntity<Map<String, Object>> resetCircuitBreaker() {
        circuitBreaker.reset();
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("message", "熔断器已重置为 CLOSED 状态");
        return ResponseEntity.ok(result);
    }

    /**
     * 查询围栏配置状态
     */
    @GetMapping("/fence/status")
    public ResponseEntity<Map<String, Object>> getFenceStatus() {
        DemoProperties.Fence fence = props.getFence();
        Map<String, Object> data = new HashMap<>();
        data.put("enabled", fence.isEnabled());
        data.put("dry_run", fence.isDryRun());
        data.put("degrade_strategy", fence.getDegradeStrategy());
        data.put("circuit_breaker_threshold", fence.getCircuitBreakerThreshold());
        data.put("circuit_breaker_window_seconds", fence.getCircuitBreakerWindowSeconds());
        data.put("circuit_breaker_recovery_seconds", fence.getCircuitBreakerRecoverySeconds());

        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("message", "success");
        result.put("data", data);
        return ResponseEntity.ok(result);
    }
}
