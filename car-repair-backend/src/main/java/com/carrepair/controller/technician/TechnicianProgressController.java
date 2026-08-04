package com.carrepair.controller.technician;

import com.carrepair.common.Result;
import com.carrepair.entity.BizRepairProgress;
import com.carrepair.security.LoginUser;
import com.carrepair.service.BizRepairProgressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 维修端-进度更新控制器
 */
@RestController
@RequestMapping("/api/technician/progress")
public class TechnicianProgressController {

    @Autowired
    private BizRepairProgressService progressService;

    /**
     * 更新维修进度
     */
    @PostMapping("/update")
    public Result<Void> update(@RequestBody BizRepairProgress progress, 
                               @AuthenticationPrincipal LoginUser loginUser) {
        progress.setOperatorId(loginUser.getUser().getId());
        progress.setOperatorName(loginUser.getUser().getRealName());
        
        if (progressService.addProgress(progress)) {
            return Result.success("进度更新成功");
        }
        return Result.error("进度更新失败");
    }
}
