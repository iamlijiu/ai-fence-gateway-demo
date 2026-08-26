package com.zl.demo.fence.mapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.zl.demo.fence.entity.ViolationLog;

/**
 * 违规记录Mapper接口
 */
@Mapper
public interface ViolationLogMapper {

    /**
     * 根据ID查询违规记录
     *
     * @param violationId 违规记录ID
     * @return 违规记录对象
     */
    ViolationLog selectById(@Param("violationId") String violationId);

    /**
     * 根据请求ID查询违规记录
     *
     * @param requestId 请求ID
     * @return 违规记录列表
     */
    List<ViolationLog> selectByRequestId(@Param("requestId") String requestId);

    /**
     * 根据违规类型查询
     *
     * @param violationType 违规类型
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 违规记录列表
     */
    List<ViolationLog> selectByType(@Param("violationType") String violationType,
                                    @Param("startTime") LocalDateTime startTime,
                                    @Param("endTime") LocalDateTime endTime);

    /**
     * 根据业务方查询违规记录
     *
     * @param consumer 业务方标识
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 违规记录列表
     */
    List<ViolationLog> selectByConsumer(@Param("consumer") String consumer,
                                        @Param("startTime") LocalDateTime startTime,
                                        @Param("endTime") LocalDateTime endTime);

    /**
     * 查询待反馈的违规记录
     *
     * @param limit 限制数量
     * @return 违规记录列表
     */
    List<ViolationLog> selectPendingFeedback(@Param("limit") Integer limit);

    /**
     * 插入违规记录
     *
     * @param log 违规记录对象
     * @return 影响行数
     */
    int insert(ViolationLog log);

    /**
     * 更新反馈状态
     *
     * @param violationId 违规记录ID
     * @param feedbackStatus 反馈状态
     * @return 影响行数
     */
    int updateFeedbackStatus(@Param("violationId") String violationId,
                             @Param("feedbackStatus") String feedbackStatus);

    /**
     * 删除指定时间之前的违规记录
     *
     * @param beforeTime 时间点
     * @return 影响行数
     */
    int deleteBeforeTime(@Param("beforeTime") LocalDateTime beforeTime);

    /**
     * 统计指定时间范围内的违规数量
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 违规数量
     */
    int countByTimeRange(@Param("startTime") LocalDateTime startTime,
                         @Param("endTime") LocalDateTime endTime);

    /**
     * 按违规类型统计
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计结果列表（violation_type, count）
     */
    List<Map<String, Object>> countByType(@Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);

    /**
     * 按风险等级统计
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计结果列表（risk_level, count）
     */
    List<Map<String, Object>> countByRiskLevel(@Param("startTime") LocalDateTime startTime,
                                               @Param("endTime") LocalDateTime endTime);
}
