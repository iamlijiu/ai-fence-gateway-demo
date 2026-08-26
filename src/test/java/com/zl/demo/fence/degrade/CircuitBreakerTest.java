package com.zl.demo.fence.degrade;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.zl.demo.config.DemoProperties;

/**
 * CircuitBreaker 单元测试
 * <p>
 * 验证熔断器状态机：CLOSED → OPEN → HALF_OPEN → CLOSED
 * </p>
 */
public class CircuitBreakerTest {

    private CircuitBreaker circuitBreaker;
    private DemoProperties props;

    @Before
    public void setUp() {
        props = new DemoProperties();
        // 设置较低的阈值便于测试
        props.getFence().setCircuitBreakerThreshold(50.0); // 50% 异常率触发熔断
        props.getFence().setCircuitBreakerWindowSeconds(60);
        props.getFence().setCircuitBreakerRecoverySeconds(1); // 1秒恢复（测试用）
        circuitBreaker = new CircuitBreaker(props);
    }

    @Test
    public void testInitialState_closed() {
        // 初始状态应为 CLOSED
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
        assertTrue("初始状态应允许请求", circuitBreaker.allowRequest());
    }

    @Test
    public void testNormalTraffic_stayClosed() {
        // 正常流量（全部成功）应保持 CLOSED
        for (int i = 0; i < 20; i++) {
            circuitBreaker.record(true);
        }

        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
        assertTrue("正常流量应允许请求", circuitBreaker.allowRequest());
    }

    @Test
    public void testHighErrorRate_triggerOpen() {
        // 高异常率应触发 OPEN
        // 先发送 10 个请求（最小样本）
        for (int i = 0; i < 5; i++) {
            circuitBreaker.record(true); // 5 个成功
        }
        for (int i = 0; i < 5; i++) {
            circuitBreaker.record(false); // 5 个失败
        }

        // 50% 异常率，应触发熔断
        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());
        assertFalse("熔断状态不应允许请求", circuitBreaker.allowRequest());
    }

    @Test
    public void testOpenState_blockRequests() {
        // 触发熔断
        triggerOpen();

        // 熔断状态下应拒绝请求
        assertFalse("OPEN 状态应拒绝请求", circuitBreaker.allowRequest());
        assertFalse("OPEN 状态应拒绝请求", circuitBreaker.allowRequest());
    }

    @Test
    public void testRecovery_halfOpen() throws InterruptedException {
        // 触发熔断
        triggerOpen();

        // 等待恢复时间
        Thread.sleep(1500); // 等待 1.5 秒（恢复时间设置为 1 秒）

        // 应进入 HALF_OPEN 状态
        assertTrue("恢复后应允许请求", circuitBreaker.allowRequest());
        assertEquals(CircuitBreaker.State.HALF_OPEN, circuitBreaker.getState());
    }

    @Test
    public void testHalfOpen_success_backToClosed() throws InterruptedException {
        // 触发熔断
        triggerOpen();

        // 等待恢复
        Thread.sleep(1500);
        circuitBreaker.allowRequest(); // 触发 HALF_OPEN

        // 探测成功
        circuitBreaker.record(true);
        circuitBreaker.record(true);
        circuitBreaker.record(true);

        // 应恢复为 CLOSED
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
        assertTrue("恢复后应允许请求", circuitBreaker.allowRequest());
    }

    @Test
    public void testHalfOpen_failure_backToOpen() throws InterruptedException {
        // 触发熔断
        triggerOpen();

        // 等待恢复
        Thread.sleep(1500);
        circuitBreaker.allowRequest(); // 触发 HALF_OPEN

        // 探测失败
        circuitBreaker.record(false);

        // 应回到 OPEN
        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());
    }

    @Test
    public void testManualReset() {
        // 触发熔断
        triggerOpen();
        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());

        // 手动重置
        circuitBreaker.reset();

        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
        assertTrue("重置后应允许请求", circuitBreaker.allowRequest());
    }

    @Test
    public void testSnapshot() {
        // 记录一些数据
        circuitBreaker.record(true);
        circuitBreaker.record(true);
        circuitBreaker.record(false);

        java.util.Map<String, Object> snapshot = circuitBreaker.snapshot();

        assertEquals("CLOSED", snapshot.get("state"));
        assertEquals(3L, snapshot.get("window_total"));
        assertEquals(1L, snapshot.get("window_errors"));
        assertEquals(50.0, (Double) snapshot.get("threshold_percent"), 0.01);
    }

    @Test
    public void testSmallSample_noTrip() {
        // 小样本不应触发熔断（至少需要 10 个请求）
        for (int i = 0; i < 5; i++) {
            circuitBreaker.record(false);
        }

        // 只有 5 个请求，不应触发熔断
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
    }

    /**
     * 触发熔断状态
     */
    private void triggerOpen() {
        // 发送足够的请求并产生高异常率
        for (int i = 0; i < 10; i++) {
            circuitBreaker.record(false);
        }
        // 再发一个触发检查
        circuitBreaker.record(false);
    }
}
