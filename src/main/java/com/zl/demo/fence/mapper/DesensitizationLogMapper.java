package com.zl.demo.fence.mapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.zl.demo.fence.entity.DesensitizationLog;

/**
 * 脱敏操作日志Mapper接口
 */
@Mapper
public interface DesensitizationLogMapper {

    /**
     * 根据ID查询日志
     *
     * @param logId 日志ID
     * @return 日志对象
     */
    DesensitizationLog selectById(@Param("logId") String logId);

    /**
     * 根据请求ID查询日志
     *
     * @param requestId 请求ID
     * @return 日志列表
     */
    List<DesensitizationLog> selectByRequestId(@Param("requestId") String requestId);

    /**
     * 根据规则ID查询日志
     *
     * @param ruleId 规则ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 日志列表
     */
    List<DesensitizationLog> selectByRuleId(@Param("ruleId") String ruleId,
                                            @Param("startTime") LocalDateTime startTime,
                                            @Param("endTime") LocalDateTime endTime);

    /**
     * 根据路由查询日志
     *
     * @param route 路由
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 日志列表
     */
    List<DesensitizationLog> selectByRoute(@Param("route") String route,
                                           @Param("startTime") LocalDateTime startTime,
                                           @Param("endTime") LocalDateTime endTime);

    /**
     * 根据业务方查询日志
     *
     * @param consumer 业务方标识
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 日志列表
     */
    List<DesensitizationLog> selectByConsumer(@Param("consumer") String consumer,
                                              @Param("startTime") LocalDateTime startTime,
                                              @Param("endTime") LocalDateTime endTime);

    /**
     * 查询高风险日志
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 日志列表
     */
    List<DesensitizationLog> selectHighRisk(@Param("startTime") LocalDateTime startTime,
                                            @Param("endTime") LocalDateTime endTime);

    /**
     * 插入日志
     *
     * @param log 日志对象
     * @return 影响行数
     */
    int insert(DesensitizationLog log);

    /**
     * 批量插入日志
     *
     * @param logs 日志列表
     * @return 影响行数
     */
    int batchInsert(@Param("logs") List<DesensitizationLog> logs);

    /**
     * 删除指定时间之前的日志
     *
     * @param beforeTime 时间点
     * @return 影响行数
     */
    int deleteBeforeTime(@Param("beforeTime") LocalDateTime beforeTime);

    /**
     * 统计指定时间范围内的日志数量
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 日志数量
     */
    int countByTimeRange(@Param("startTime") LocalDateTime startTime,
                         @Param("endTime") LocalDateTime endTime);

    /**
     * 按路由统计日志数量
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计结果列表（route, count）
     */
    List<Map<String, Object>> countByRoute(@Param("startTime") LocalDateTime startTime,
                                           @Param("endTime") LocalDateTime endTime);

    /**
     * 按规则统计日志数量
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计结果列表（rule_id, rule_name, count）
     */
    List<Map<String, Object>> countByRule(@Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);

    /**
     * 按业务方统计拦截数量
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计结果列表（consumer, count）
     */
    List<Map<String, Object>> countBlockedByConsumer(@Param("startTime") LocalDateTime startTime,
                                                     @Param("endTime") LocalDateTime endTime);

    /**
     * 统计平均处理耗时（按路由）
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计结果列表（route, avg_cost_ms, max_cost_ms, count）
     */
    List<Map<String, Object>> avgCostByRoute(@Param("startTime") LocalDateTime startTime,
                                             @Param("endTime") LocalDateTime endTime);
}
