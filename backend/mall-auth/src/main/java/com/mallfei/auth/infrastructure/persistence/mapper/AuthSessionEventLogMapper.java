package com.mallfei.auth.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mallfei.auth.infrastructure.persistence.dataobject.AuthSessionEventLogDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuthSessionEventLogMapper extends BaseMapper<AuthSessionEventLogDO> {
}
