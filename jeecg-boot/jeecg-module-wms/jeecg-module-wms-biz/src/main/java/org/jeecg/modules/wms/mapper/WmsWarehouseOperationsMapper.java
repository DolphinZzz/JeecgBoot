package org.jeecg.modules.wms.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.wms.entity.WmsWarehouseOperations;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * @Description: 库内作业单
 * @Author: jeecg-boot
 * @Date: 2026-10-29
 * @Version: V1.0
 */
public interface WmsWarehouseOperationsMapper extends BaseMapper<WmsWarehouseOperations> {

    /**
     * 查询作业单明细。
     */
    List<org.jeecg.modules.wms.entity.WmsWarehouseOperationsDetails> selectDetailsByOperationId(@Param("operationId") Long operationId);
}
