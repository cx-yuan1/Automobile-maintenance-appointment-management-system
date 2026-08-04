package com.carrepair;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 汽车维修预约管理系统 - 启动类
 */
@SpringBootApplication
@MapperScan("com.carrepair.mapper")
@EnableScheduling
public class CarRepairApplication {

    public static void main(String[] args) {
        SpringApplication.run(CarRepairApplication.class, args);
        System.out.println("========================================");
        System.out.println("  汽车维修预约管理系统启动成功！");
        System.out.println("  后端地址: http://localhost:8080");
        System.out.println("========================================");
    }
}
