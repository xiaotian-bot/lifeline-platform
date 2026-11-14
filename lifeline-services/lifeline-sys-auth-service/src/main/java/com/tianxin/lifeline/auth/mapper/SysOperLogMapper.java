package com.tianxin.lifeline.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianxin.lifeline.auth.entity.SysOperLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作审计日志 Mapper。
 */
@Mapper
public interface SysOperLogMapper extends BaseMapper<SysOperLog> {
}
