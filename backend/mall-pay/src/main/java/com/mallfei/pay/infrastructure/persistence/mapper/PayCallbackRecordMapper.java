package com.mallfei.pay.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mallfei.pay.infrastructure.persistence.dataobject.PayCallbackRecordDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PayCallbackRecordMapper extends BaseMapper<PayCallbackRecordDO> {
}
