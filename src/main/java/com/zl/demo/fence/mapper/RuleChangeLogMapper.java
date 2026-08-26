package com.zl.demo.fence.mapper;

import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.zl.demo.fence.entity.RuleChangeLog;

/**
 * 规则变更日志Mapper接口
 */
@Mapper
public interface RuleChangeLogMapper {

    /**
     * 根据ID查询变更记录
     *
     * @param changeId 变更记录ID
     * @return 变更记录对象
     */
    RuleChangeLog selectById(@Param("changeId") String changeId);

    /**
     * 根据规则ID查询变更记录
     *
     * @param ruleId 规则ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 变更记录列表
     */
    List<RuleChangeLog> selectByRuleId(@Param("ruleId") String ruleId,
                                       @Param("startTime") LocalDateTime startTime,
                                       @Param("endTime") LocalDateTime endTime);

    /**
     * 根据操作人查询变更记录
     *
     * @param operator 操作人
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 变更记录列表
     */
    List<RuleChangeLog> selectByOperator(@Param("operator") String operator,
                                         @Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime);

    /**
     * 根据操作类型查询变更记录
     *
     * @param action 操作类型
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 变更记录列表
     */
    List<RuleChangeLog> selectByAction(@Param("action") String action,
                                       @Param("startTime") LocalDateTime startTime,
                                       @Param("endTime") LocalDateTime endTime);

    /**
     * 查询最近的变更记录
     *
     * @param limit 限制数量
     * @return 变更记录列表
     */
    List<RuleChangeLog> selectRecent(@Param("limit") Integer limit);

    /**
     * 插入变更记录
     *
     * @param log 变更记录对象
     * @return 影响行数
     */
    int insert(RuleChangeLog log);

    /**
     * 删除指定时间之前的变更记录
     *
     * @param beforeTime 时间点
     * @return 影响行数
     */
    int deleteBeforeTime(@Param("beforeTime") LocalDateTime beforeTime);

    /**
     * 统计指定时间范围内的变更数量
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 变更数量
     */
    int countByTimeRange(@Param("startTime") LocalDateTime startTime,
                         @Param("endTime") LocalDateTime endTime);

    /**
     * 按操作类型统计
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计结果列表（action, count）
     */
    java.util.List<java.util.Map<String, Object>> countByAction(@Param("startTime") LocalDateTime startTime,
                                                                @Param("endTime") LocalDateTime endTime);

    /**
     * 按操作人统计
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计结果列表（operator, count）
     */
    java.util.List<java.util.Map<String, Object>> countByOperator(@Param("startTime") LocalDateTime startTime,
                                                                  @Param("endTime") LocalDateTime endTime);
}
