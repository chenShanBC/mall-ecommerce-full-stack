package com.mallfei.product.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mallfei.product.infrastructure.persistence.dataobject.ProductSpuDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductSpuMapper extends BaseMapper<ProductSpuDO> {
}
