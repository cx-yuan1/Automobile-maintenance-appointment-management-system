package com.carrepair.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.carrepair.common.PageResult;
import com.carrepair.common.Result;
import com.carrepair.entity.BizOrderPart;
import com.carrepair.entity.BizPart;
import com.carrepair.mapper.BizOrderPartMapper;
import com.carrepair.security.LoginUser;
import com.carrepair.service.BizPartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理端-配件库存管理控制器
 */
@RestController
@RequestMapping("/api/admin/part")
public class AdminPartController {

    @Autowired
    private BizPartService partService;
    
    @Autowired
    private BizOrderPartMapper orderPartMapper;

    /**
     * 获取配件列表
     */
    @GetMapping("/list")
    public Result<PageResult<BizPart>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String stockStatus,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        IPage<BizPart> pageResult = partService.pageList(page, size, category, keyword, stockStatus);
        return Result.success(PageResult.of(pageResult));
    }

    /**
     * 添加配件
     */
    @PostMapping("/add")
    public Result<Void> add(@RequestBody BizPart part) {
        part.setStatus(1);
        part.setStockQuantity(0);
        part.setReservedQuantity(0);
        if (partService.save(part)) {
            return Result.success("添加成功");
        }
        return Result.error("添加失败");
    }

    /**
     * 编辑配件
     */
    @PutMapping("/update")
    public Result<Void> update(@RequestBody BizPart part) {
        if (partService.updateById(part)) {
            return Result.success("更新成功");
        }
        return Result.error("更新失败");
    }

    /**
     * 删除配件
     */
    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        // 检查是否被工单引用
        long count = orderPartMapper.selectCount(
                new LambdaQueryWrapper<BizOrderPart>().eq(BizOrderPart::getPartId, id));
        if (count > 0) {
            return Result.error("该配件已被工单使用，无法删除。如需停用请修改状态。");
        }
        
        if (partService.removeById(id)) {
            return Result.success("删除成功");
        }
        return Result.error("删除失败");
    }

    /**
     * 获取库存预警列表
     */
    @GetMapping("/warning")
    public Result<List<BizPart>> warning() {
        return Result.success(partService.listWarning());
    }
}
