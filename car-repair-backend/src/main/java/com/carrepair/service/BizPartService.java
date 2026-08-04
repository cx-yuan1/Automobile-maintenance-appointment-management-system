package com.carrepair.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.carrepair.entity.BizPart;
import java.util.List;

public interface BizPartService extends IService<BizPart> {
    
    /** 分页查询 */
    IPage<BizPart> pageList(Integer page, Integer size, String category, String keyword, String stockStatus);
    
    /** 获取库存预警列表 */
    List<BizPart> listWarning();
    
    /** 入库 */
    boolean stockIn(Long partId, Integer quantity, Long operatorId, String remark);
    
    /** 出库 */
    boolean stockOut(Long partId, Integer quantity, Long operatorId, String remark);
    
    /** 预留配件 */
    boolean reservePart(Long partId, Integer quantity, Long orderId, Long operatorId);
    
    /** 取消预留 */
    boolean cancelReserve(Long partId, Integer quantity, Long orderId, Long operatorId);
}
