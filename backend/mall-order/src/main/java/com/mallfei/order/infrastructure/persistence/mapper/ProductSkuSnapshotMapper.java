package com.mallfei.order.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mallfei.order.infrastructure.persistence.dataobject.ProductSkuSnapshotDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductSkuSnapshotMapper extends BaseMapper<ProductSkuSnapshotDO> {
}
