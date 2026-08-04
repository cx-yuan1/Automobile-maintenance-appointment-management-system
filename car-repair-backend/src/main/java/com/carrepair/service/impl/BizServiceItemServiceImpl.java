package com.carrepair.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.carrepair.entity.BizServiceItem;
import com.carrepair.mapper.BizServiceItemMapper;
import com.carrepair.service.BizServiceItemService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.util.List;

@Service
public class BizServiceItemServiceImpl extends ServiceImpl<BizServiceItemMapper, BizServiceItem> implements BizServiceItemService {

    @Override
    public List<BizServiceItem> listEnabled() {
        return list(new LambdaQueryWrapper<BizServiceItem>()
                .eq(BizServiceItem::getStatus, 1)
                .orderByAsc(BizServiceItem::getCategory));
    }

    @Override
    public IPage<BizServiceItem> pageList(Integer page, Integer size, String serviceName, String category, Integer status) {
        LambdaQueryWrapper<BizServiceItem> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(serviceName)) {
            wrapper.like(BizServiceItem::getServiceName, serviceName);
        }
        if (StringUtils.hasText(category)) {
            wrapper.eq(BizServiceItem::getCategory, category);
        }
        if (status != null) {
            wrapper.eq(BizServiceItem::getStatus, status);
        }
        wrapper.orderByDesc(BizServiceItem::getCreateTime);
        return page(new Page<>(page, size), wrapper);
    }
}
