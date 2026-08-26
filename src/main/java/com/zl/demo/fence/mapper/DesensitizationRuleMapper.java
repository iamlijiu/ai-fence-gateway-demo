package com.zl.demo.fence.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.zl.demo.fence.entity.DesensitizationRule;

/**
 * 脱敏规则Mapper接口
 */
@Mapper
public interface DesensitizationRuleMapper {

    /**
     * 根据ID查询规则
     *
     * @param ruleId 规则ID
     * @return 规则对象
     */
    DesensitizationRule selectById(@Param("ruleId") String ruleId);

    /**
     * 查询所有启用的规则（按优先级排序）
     *
     * @return 规则列表
     */
    List<DesensitizationRule> selectAllEnabled();

    /**
     * 查询所有规则（包括禁用的）
     *
     * @return 规则列表
     */
    List<DesensitizationRule> selectAll();

    /**
     * 插入规则
     *
     * @param rule 规则对象
     * @return 影响行数
     */
    int insert(DesensitizationRule rule);

    /**
     * 更新规则
     *
     * @param rule 规则对象
     * @return 影响行数
     */
    int update(DesensitizationRule rule);

    /**
     * 逻辑删除规则
     *
     * @param ruleId 规则ID
     * @param updatedBy 更新人
     * @return 影响行数
     */
    int deleteById(@Param("ruleId") String ruleId, @Param("updatedBy") String updatedBy);

    /**
     * 启用/禁用规则
     *
     * @param ruleId 规则ID
     * @param enabled 是否启用
     * @param updatedBy 更新人
     * @return 影响行数
     */
    int updateEnabled(@Param("ruleId") String ruleId, @Param("enabled") Integer enabled,
                      @Param("updatedBy") String updatedBy);

    /**
     * 更新规则版本号
     *
     * @param ruleId 规则ID
     * @param ruleVersion 新版本号
     * @return 影响行数
     */
    int updateVersion(@Param("ruleId") String ruleId, @Param("ruleVersion") Integer ruleVersion);

    /**
     * 统计规则总数
     *
     * @return 规则数量
     */
    int countAll();

    /**
     * 统计启用规则数
     *
     * @return 启用规则数量
     */
    int countEnabled();
}
