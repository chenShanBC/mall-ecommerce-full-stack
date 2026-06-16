package com.mallfei.aftersale.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mallfei.aftersale.infrastructure.persistence.dataobject.AftersaleOrderDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AftersaleOrderMapper extends BaseMapper<AftersaleOrderDO> {
}
