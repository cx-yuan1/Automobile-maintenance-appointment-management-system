package com.carrepair.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 车辆实体类
 */
@Data
@TableName("biz_vehicle")
public class BizVehicle {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户ID */
    private Long userId;

    /** 车牌号 */
    private String plateNumber;

    /** 品牌 */
    private String brand;

    /** 车型 */
    private String model;

    /** 车辆类型:1普通车 2豪华车 3新能源 */
    private Integer vehicleType;

    /** VIN码 */
    private String vin;

    /** 颜色 */
    private String color;

    /** 当前里程(km) */
    private Integer mileage;

    /** 车辆图片URL */
    private String imageUrl;

    /** 状态:0删除 1正常 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
