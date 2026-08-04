package com.carrepair.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.carrepair.entity.BizServiceItem;
import java.util.List;

public interface BizServiceItemService extends IService<BizServiceItem> {
    
    /** 获取启用的服务项目列表 */
    List<BizServiceItem> listEnabled();
    
    /** 分页查询 */
    IPage<BizServiceItem> pageList(Integer page, Integer size, String serviceName, String category, Integer status);
}
