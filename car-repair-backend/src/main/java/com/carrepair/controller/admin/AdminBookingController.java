package com.carrepair.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.carrepair.common.PageResult;
import com.carrepair.common.Result;
import com.carrepair.entity.BizBooking;
import com.carrepair.service.BizBookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 管理端-预约管理控制器
 */
@RestController
@RequestMapping("/api/admin/booking")
public class AdminBookingController {

    @Autowired
    private BizBookingService bookingService;

    /**
     * 获取预约列表
     */
    @GetMapping("/list")
    public Result<PageResult<BizBooking>> list(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        IPage<BizBooking> pageResult = bookingService.pageList(status, date, page, size);
        return Result.success(PageResult.of(pageResult));
    }

    /**
     * 确认预约
     */
    @PutMapping("/confirm/{id}")
    public Result<Void> confirm(@PathVariable Long id) {
        if (bookingService.confirm(id)) {
            return Result.success("预约已确认");
        }
        return Result.error("确认失败，预约状态不正确");
    }

    /**
     * 拒绝预约
     */
    @PutMapping("/reject/{id}")
    public Result<Void> reject(@PathVariable Long id) {
        if (bookingService.cancel(id)) {
            return Result.success("预约已拒绝");
        }
        return Result.error("拒绝失败");
    }
}
