package com.carrepair.controller.admin;

import com.carrepair.common.Result;
import com.carrepair.entity.BizPart;
import com.carrepair.security.LoginUser;
import com.carrepair.service.BizPartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理端-库存管理控制器
 */
@RestController
@RequestMapping("/api/admin/inventory")
public class AdminInventoryController {

    @Autowired
    private BizPartService partService;

    /**
     * 配件入库
     */
    @PostMapping("/in")
    public Result<Void> stockIn(@RequestBody Map<String, Object> params, 
                                @AuthenticationPrincipal LoginUser loginUser) {
        Long partId = Long.valueOf(params.get("partId").toString());
        Integer quantity = Integer.valueOf(params.get("quantity").toString());
        String remark = params.get("remark") != null ? params.get("remark").toString() : "采购入库";
        
        if (partService.stockIn(partId, quantity, loginUser.getUser().getId(), remark)) {
            BizPart part = partService.getById(partId);
            return Result.success("入库成功，当前库存：" + part.getStockQuantity());
        }
        return Result.error("入库失败");
    }

    /**
     * 配件出库
     */
    @PostMapping("/out")
    public Result<Void> stockOut(@RequestBody Map<String, Object> params, 
                                 @AuthenticationPrincipal LoginUser loginUser) {
        Long partId = Long.valueOf(params.get("partId").toString());
        Integer quantity = Integer.valueOf(params.get("quantity").toString());
        String remark = params.get("remark") != null ? params.get("remark").toString() : "手动出库";
        
        if (partService.stockOut(partId, quantity, loginUser.getUser().getId(), remark)) {
            BizPart part = partService.getById(partId);
            return Result.success("出库成功，当前库存：" + part.getStockQuantity());
        }
        return Result.error("出库失败，库存不足");
    }
}
