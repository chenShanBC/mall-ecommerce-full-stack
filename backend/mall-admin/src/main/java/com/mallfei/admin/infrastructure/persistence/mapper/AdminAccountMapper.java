package com.mallfei.admin.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mallfei.admin.infrastructure.persistence.dataobject.AdminAccountDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AdminAccountMapper extends BaseMapper<AdminAccountDO> {
}
