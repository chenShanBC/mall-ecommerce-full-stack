package com.mallfei.order.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mallfei.order.infrastructure.persistence.dataobject.OrderRefundDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderRefundMapper extends BaseMapper<OrderRefundDO> {
}
