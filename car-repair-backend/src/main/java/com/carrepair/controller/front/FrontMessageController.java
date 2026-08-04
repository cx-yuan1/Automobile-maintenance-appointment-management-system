package com.carrepair.controller.front;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.carrepair.common.PageResult;
import com.carrepair.common.Result;
import com.carrepair.entity.SysMessage;
import com.carrepair.mapper.SysMessageMapper;
import com.carrepair.security.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 前台-消息中心控制器
 */
@RestController
@RequestMapping("/api/front/message")
public class FrontMessageController {

    @Autowired
    private SysMessageMapper messageMapper;

    /**
     * 获取消息列表
     */
    @GetMapping("/list")
    public Result<PageResult<SysMessage>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer isRead,
            @AuthenticationPrincipal LoginUser loginUser) {
        
        LambdaQueryWrapper<SysMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysMessage::getUserId, loginUser.getUser().getId());
        if (isRead != null) {
            wrapper.eq(SysMessage::getIsRead, isRead);
        }
        wrapper.orderByDesc(SysMessage::getCreateTime);
        
        IPage<SysMessage> pageResult = messageMapper.selectPage(new Page<>(page, size), wrapper);
        return Result.success(PageResult.of(pageResult));
    }

    /**
     * 获取未读消息数量
     */
    @GetMapping("/unreadCount")
    public Result<Long> unreadCount(@AuthenticationPrincipal LoginUser loginUser) {
        long count = messageMapper.selectCount(
                new LambdaQueryWrapper<SysMessage>()
                        .eq(SysMessage::getUserId, loginUser.getUser().getId())
                        .eq(SysMessage::getIsRead, 0));
        return Result.success(count);
    }

    /**
     * 标记消息为已读
     */
    @PutMapping("/read/{id}")
    public Result<Void> read(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        SysMessage message = messageMapper.selectById(id);
        if (message == null || !message.getUserId().equals(loginUser.getUser().getId())) {
            return Result.error("消息不存在");
        }
        message.setIsRead(1);
        messageMapper.updateById(message);
        return Result.success("标记成功");
    }

    /**
     * 标记所有消息为已读
     */
    @PutMapping("/readAll")
    public Result<Void> readAll(@AuthenticationPrincipal LoginUser loginUser) {
        SysMessage update = new SysMessage();
        update.setIsRead(1);
        messageMapper.update(update, 
                new LambdaQueryWrapper<SysMessage>()
                        .eq(SysMessage::getUserId, loginUser.getUser().getId())
                        .eq(SysMessage::getIsRead, 0));
        return Result.success("全部标记成功");
    }

    /**
     * 删除消息
     */
    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        SysMessage message = messageMapper.selectById(id);
        if (message == null || !message.getUserId().equals(loginUser.getUser().getId())) {
            return Result.error("消息不存在");
        }
        messageMapper.deleteById(id);
        return Result.success("删除成功");
    }
}
