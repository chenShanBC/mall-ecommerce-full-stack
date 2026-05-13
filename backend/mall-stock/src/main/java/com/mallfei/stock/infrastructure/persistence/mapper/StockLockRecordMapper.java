package com.mallfei.stock.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mallfei.stock.infrastructure.persistence.dataobject.StockLockRecordDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StockLockRecordMapper extends BaseMapper<StockLockRecordDO> {
}
