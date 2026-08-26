package com.zl.demo.fence.monitor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

/**
 * 基础监控计数器（P0-7，本地轻量实现）
 * <p>
 * 一期不接 Prometheus（需技评），先以本地原子计数 + 统计接口方式提供可观测性，
 * 后续可平移到 Prometheus 指标暴露。
 * </p>
 */
@Component
public class MetricsCounter {

    private final AtomicLong totalRequests = new AtomicLong();
    private final AtomicLong passCount = new AtomicLong();          // 放行（无命中）
    private final AtomicLong desensitizedCount = new AtomicLong();  // 脱敏放行
    private final AtomicLong blockedCount = new AtomicLong();       // 拦截
    private final AtomicLong degradedCount = new AtomicLong();      // 降级
    private final AtomicLong dryRunCount = new AtomicLong();        // dry-run 观察模式
    private final AtomicLong ruleHitCount = new AtomicLong();       // 规则命中次数（含多条）
    private final AtomicLong totalCostMs = new AtomicLong();        // 围栏处理总耗时
    private final AtomicLong startedAt = new AtomicLong(System.currentTimeMillis());

    /** 记录一次请求的处理结果 */
    public void record(boolean blocked, boolean desensitized, boolean degraded,
                       int hitRules, long costMs) {
        record(blocked, desensitized, degraded, false, hitRules, costMs);
    }

    /** 记录一次请求的处理结果（含 dry-run） */
    public void record(boolean blocked, boolean desensitized, boolean degraded, boolean dryRun,
                       int hitRules, long costMs) {
        totalRequests.incrementAndGet();
        if (blocked) blockedCount.incrementAndGet();
        else if (degraded) degradedCount.incrementAndGet();
        else if (dryRun) dryRunCount.incrementAndGet();
        else if (desensitized) desensitizedCount.incrementAndGet();
        else passCount.incrementAndGet();

        if (hitRules > 0) ruleHitCount.addAndGet(hitRules);
        totalCostMs.addAndGet(costMs);
    }

    /** 统计快照（对应 3.3 统计查询接口） */
    public Map<String, Object> snapshot() {
        Map<String, Object> stats = new LinkedHashMap<>();
        long total = totalRequests.get();
        stats.put("total_requests", total);
        stats.put("pass_count", passCount.get());
        stats.put("desensitized_count", desensitizedCount.get());
        stats.put("blocked_count", blockedCount.get());
        stats.put("degraded_count", degradedCount.get());
        stats.put("dry_run_count", dryRunCount.get());
        stats.put("rule_hit_count", ruleHitCount.get());
        stats.put("avg_cost_ms", total == 0 ? 0 : totalCostMs.get() / total);
        stats.put("uptime_seconds", (System.currentTimeMillis() - startedAt.get()) / 1000);
        return stats;
    }

    /** 重置计数器（应急用） */
    public void reset() {
        totalRequests.set(0);
        passCount.set(0);
        desensitizedCount.set(0);
        blockedCount.set(0);
        degradedCount.set(0);
        dryRunCount.set(0);
        ruleHitCount.set(0);
        totalCostMs.set(0);
        startedAt.set(System.currentTimeMillis());
    }
}
