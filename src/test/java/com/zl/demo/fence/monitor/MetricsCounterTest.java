package com.zl.demo.fence.monitor;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

/**
 * MetricsCounter 单元测试
 */
public class MetricsCounterTest {

    private MetricsCounter metricsCounter;

    @Before
    public void setUp() {
        metricsCounter = new MetricsCounter();
    }

    @Test
    public void testInitialState() {
        Map<String, Object> snapshot = metricsCounter.snapshot();

        assertEquals(0L, snapshot.get("total_requests"));
        assertEquals(0L, snapshot.get("pass_count"));
        assertEquals(0L, snapshot.get("desensitized_count"));
        assertEquals(0L, snapshot.get("blocked_count"));
        assertEquals(0L, snapshot.get("degraded_count"));
        assertEquals(0L, snapshot.get("dry_run_count"));
        assertEquals(0L, snapshot.get("rule_hit_count"));
        assertEquals(0L, snapshot.get("avg_cost_ms"));
    }

    @Test
    public void testRecord_pass() {
        metricsCounter.record(false, false, false, 0, 10);

        Map<String, Object> snapshot = metricsCounter.snapshot();
        assertEquals(1L, snapshot.get("total_requests"));
        assertEquals(1L, snapshot.get("pass_count"));
        assertEquals(0L, snapshot.get("desensitized_count"));
    }

    @Test
    public void testRecord_desensitized() {
        metricsCounter.record(false, true, false, 2, 15);

        Map<String, Object> snapshot = metricsCounter.snapshot();
        assertEquals(1L, snapshot.get("total_requests"));
        assertEquals(0L, snapshot.get("pass_count"));
        assertEquals(1L, snapshot.get("desensitized_count"));
        assertEquals(2L, snapshot.get("rule_hit_count"));
    }

    @Test
    public void testRecord_blocked() {
        metricsCounter.record(true, false, false, 1, 5);

        Map<String, Object> snapshot = metricsCounter.snapshot();
        assertEquals(1L, snapshot.get("total_requests"));
        assertEquals(1L, snapshot.get("blocked_count"));
    }

    @Test
    public void testRecord_degraded() {
        metricsCounter.record(false, false, true, 0, 3);

        Map<String, Object> snapshot = metricsCounter.snapshot();
        assertEquals(1L, snapshot.get("total_requests"));
        assertEquals(1L, snapshot.get("degraded_count"));
    }

    @Test
    public void testRecord_dryRun() {
        metricsCounter.record(false, false, false, true, 2, 8);

        Map<String, Object> snapshot = metricsCounter.snapshot();
        assertEquals(1L, snapshot.get("total_requests"));
        assertEquals(1L, snapshot.get("dry_run_count"));
        assertEquals(2L, snapshot.get("rule_hit_count"));
    }

    @Test
    public void testRecord_multiple() {
        // 记录多个请求
        metricsCounter.record(false, false, false, 0, 10);  // pass
        metricsCounter.record(false, true, false, 1, 15);   // desensitized
        metricsCounter.record(true, false, false, 1, 5);    // blocked
        metricsCounter.record(false, false, true, 0, 3);    // degraded
        metricsCounter.record(false, false, false, 0, 8);   // pass

        Map<String, Object> snapshot = metricsCounter.snapshot();
        assertEquals(5L, snapshot.get("total_requests"));
        assertEquals(2L, snapshot.get("pass_count"));
        assertEquals(1L, snapshot.get("desensitized_count"));
        assertEquals(1L, snapshot.get("blocked_count"));
        assertEquals(1L, snapshot.get("degraded_count"));
        assertEquals(2L, snapshot.get("rule_hit_count"));
        // 平均耗时：(10+15+5+3+8)/5 = 8
        assertEquals(8L, snapshot.get("avg_cost_ms"));
    }

    @Test
    public void testReset() {
        // 记录一些数据
        metricsCounter.record(false, false, false, 0, 10);
        metricsCounter.record(true, false, false, 1, 5);

        // 重置
        metricsCounter.reset();

        Map<String, Object> snapshot = metricsCounter.snapshot();
        assertEquals(0L, snapshot.get("total_requests"));
        assertEquals(0L, snapshot.get("pass_count"));
        assertEquals(0L, snapshot.get("blocked_count"));
    }

    @Test
    public void testUptimeSeconds() throws InterruptedException {
        // 等待一小段时间
        Thread.sleep(100);

        Map<String, Object> snapshot = metricsCounter.snapshot();
        long uptime = (Long) snapshot.get("uptime_seconds");

        assertTrue("运行时间应大于 0", uptime >= 0);
    }
}
