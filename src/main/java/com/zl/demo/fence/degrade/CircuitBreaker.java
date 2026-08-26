package com.zl.demo.fence.degrade;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.zl.demo.config.DemoProperties;

/**
 * 熔断器（P0-2 异常降级模块）
 * <p>
 * 基于滑动窗口统计异常率，超过阈值自动触发降级。
 * 状态机：CLOSED（正常）→ OPEN（熔断）→ HALF_OPEN（探测恢复）
 * </p>
 */
@Component
public class CircuitBreaker {

    private static final Logger log = LoggerFactory.getLogger(CircuitBreaker.class);

    public enum State {
        /** 正常状态，请求正常通过 */
        CLOSED,
        /** 熔断状态，请求走降级策略 */
        OPEN,
        /** 半开状态，允许少量请求探测是否恢复 */
        HALF_OPEN
    }

    private final DemoProperties.Fence fenceConfig;

    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);

    /** 窗口内总请求数 */
    private final AtomicLong windowTotal = new AtomicLong(0);

    /** 窗口内异常数 */
    private final AtomicLong windowErrors = new AtomicLong(0);

    /** 窗口开始时间 */
    private volatile long windowStartMs = System.currentTimeMillis();

    /** 熔断触发时间 */
    private volatile long openTimestampMs = 0;

    /** HALF_OPEN 状态下已放行的探测请求数 */
    private final AtomicLong probeCount = new AtomicLong(0);

    public CircuitBreaker(DemoProperties props) {
        this.fenceConfig = props.getFence();
    }

    /**
     * 判断是否允许请求通过
     *
     * @return true=允许通过，false=熔断中，应走降级
     */
    public boolean allowRequest() {
        State current = state.get();

        if (current == State.CLOSED) {
            return true;
        }

        if (current == State.OPEN) {
            // 检查是否到达恢复时间
            long recoveryMs = fenceConfig.getCircuitBreakerRecoverySeconds() * 1000L;
            if (System.currentTimeMillis() - openTimestampMs >= recoveryMs) {
                // 转为 HALF_OPEN，允许探测
                if (state.compareAndSet(State.OPEN, State.HALF_OPEN)) {
                    probeCount.set(0);
                    log.info("[熔断器] 状态变更 OPEN → HALF_OPEN，开始探测恢复");
                }
                return true;
            }
            return false;
        }

        // HALF_OPEN 状态：允许少量探测请求
        long count = probeCount.incrementAndGet();
        if (count <= 3) {
            return true;
        }
        return false;
    }

    /**
     * 记录请求结果
     *
     * @param success true=处理成功，false=处理异常
     */
    public void record(boolean success) {
        State current = state.get();

        // 熔断状态下不统计
        if (current == State.OPEN) {
            return;
        }

        // 检查窗口是否过期，重置
        checkAndResetWindow();

        windowTotal.incrementAndGet();
        if (!success) {
            windowErrors.incrementAndGet();
        }

        // 计算异常率
        long total = windowTotal.get();
        long errors = windowErrors.get();

        // 至少 10 个请求才计算异常率（避免小样本误触发）
        if (total < 10) {
            return;
        }

        double errorRate = (double) errors / total * 100;
        double threshold = fenceConfig.getCircuitBreakerThreshold();

        if (current == State.CLOSED && errorRate >= threshold) {
            // 触发熔断
            if (state.compareAndSet(State.CLOSED, State.OPEN)) {
                openTimestampMs = System.currentTimeMillis();
                log.warn("[熔断器] 异常率 {:.2f}% 超过阈值 {}%，状态变更 CLOSED → OPEN", errorRate, threshold);
            }
        } else if (current == State.HALF_OPEN) {
            if (success) {
                // 探测成功，恢复 CLOSED
                if (state.compareAndSet(State.HALF_OPEN, State.CLOSED)) {
                    windowTotal.set(0);
                    windowErrors.set(0);
                    log.info("[熔断器] 探测成功，状态变更 HALF_OPEN → CLOSED，恢复正常");
                }
            } else {
                // 探测失败，重回 OPEN
                if (state.compareAndSet(State.HALF_OPEN, State.OPEN)) {
                    openTimestampMs = System.currentTimeMillis();
                    log.warn("[熔断器] 探测失败，状态变更 HALF_OPEN → OPEN");
                }
            }
        }
    }

    /**
     * 获取当前熔断器状态
     */
    public State getState() {
        return state.get();
    }

    /**
     * 手动重置熔断器（应急用）
     */
    public void reset() {
        state.set(State.CLOSED);
        windowTotal.set(0);
        windowErrors.set(0);
        windowStartMs = System.currentTimeMillis();
        log.info("[熔断器] 已手动重置为 CLOSED 状态");
    }

    /**
     * 获取当前统计快照
     */
    public java.util.Map<String, Object> snapshot() {
        java.util.Map<String, Object> stats = new java.util.LinkedHashMap<>();
        stats.put("state", state.get().name());
        stats.put("window_total", windowTotal.get());
        stats.put("window_errors", windowErrors.get());
        long total = windowTotal.get();
        stats.put("error_rate_percent", total == 0 ? 0.0 : (double) windowErrors.get() / total * 100);
        stats.put("threshold_percent", fenceConfig.getCircuitBreakerThreshold());
        return stats;
    }

    private void checkAndResetWindow() {
        long windowMs = fenceConfig.getCircuitBreakerWindowSeconds() * 1000L;
        long now = System.currentTimeMillis();
        if (now - windowStartMs >= windowMs) {
            synchronized (this) {
                if (now - windowStartMs >= windowMs) {
                    windowTotal.set(0);
                    windowErrors.set(0);
                    windowStartMs = now;
                }
            }
        }
    }
}
