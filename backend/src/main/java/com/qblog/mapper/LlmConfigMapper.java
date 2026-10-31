package com.qblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qblog.entity.LlmConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 大模型配置 Mapper
 */
@Mapper
public interface LlmConfigMapper extends BaseMapper<LlmConfig> {
}
