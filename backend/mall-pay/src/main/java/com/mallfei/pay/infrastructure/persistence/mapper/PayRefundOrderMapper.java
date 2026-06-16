package com.mallfei.pay.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mallfei.pay.infrastructure.persistence.dataobject.PayRefundOrderDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PayRefundOrderMapper extends BaseMapper<PayRefundOrderDO> {
}
