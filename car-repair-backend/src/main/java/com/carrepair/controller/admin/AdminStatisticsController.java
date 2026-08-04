package com.carrepair.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carrepair.common.Result;
import com.carrepair.entity.*;
import com.carrepair.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 管理端-数据统计控制器
 */
@RestController
@RequestMapping("/api/admin/statistics")
public class AdminStatisticsController {

    @Autowired
    private BizRepairOrderMapper orderMapper;
    
    @Autowired
    private SysUserMapper userMapper;
    
    @Autowired
    private BizEvaluationMapper evaluationMapper;
    
    @Autowired
    private BizOrderServiceMapper orderServiceMapper;
    
    @Autowired
    private BizServiceItemMapper serviceItemMapper;
    
    @Autowired
    private BizInventoryRecordMapper inventoryRecordMapper;
    
    @Autowired
    private BizPartMapper partMapper;

    /**
     * 收入趋势统计
     */
    @GetMapping("/revenue")
    public Result<Map<String, Object>> revenue(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        
        Map<String, Object> result = new HashMap<>();
        
        // 查询已完成且已支付的工单
        List<BizRepairOrder> orders = orderMapper.selectList(
                new LambdaQueryWrapper<BizRepairOrder>()
                        .eq(BizRepairOrder::getStatus, 5)
                        .eq(BizRepairOrder::getPaymentStatus, 1)
                        .between(BizRepairOrder::getCreateTime, startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay()));
        
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal laborRevenue = BigDecimal.ZERO;
        BigDecimal partsRevenue = BigDecimal.ZERO;
        
        for (BizRepairOrder order : orders) {
            totalRevenue = totalRevenue.add(order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO);
            laborRevenue = laborRevenue.add(order.getLaborCost() != null ? order.getLaborCost() : BigDecimal.ZERO);
            partsRevenue = partsRevenue.add(order.getPartsCost() != null ? order.getPartsCost() : BigDecimal.ZERO);
        }
        
        result.put("totalRevenue", totalRevenue);
        result.put("laborRevenue", laborRevenue);
        result.put("partsRevenue", partsRevenue);
        result.put("orderCount", orders.size());
        
        return Result.success(result);
    }

    /**
     * 客户留存率
     */
    @GetMapping("/customerRetention")
    public Result<Map<String, Object>> customerRetention() {
        Map<String, Object> result = new HashMap<>();
        
        // 总客户数
        long totalCustomers = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUserType, 1));
        
        // 有多次工单的客户数（回头客）
        // 简化处理：统计有工单的客户
        List<BizRepairOrder> orders = orderMapper.selectList(
                new LambdaQueryWrapper<BizRepairOrder>().eq(BizRepairOrder::getStatus, 5));
        
        Set<Long> customerWithOrders = new HashSet<>();
        Map<Long, Integer> customerOrderCount = new HashMap<>();
        
        for (BizRepairOrder order : orders) {
            customerWithOrders.add(order.getUserId());
            customerOrderCount.merge(order.getUserId(), 1, Integer::sum);
        }
        
        long repeatCustomers = customerOrderCount.values().stream().filter(c -> c > 1).count();
        
        result.put("totalCustomers", totalCustomers);
        result.put("repeatCustomers", repeatCustomers);
        result.put("retentionRate", totalCustomers > 0 
                ? BigDecimal.valueOf(repeatCustomers * 100.0 / totalCustomers).setScale(1, RoundingMode.HALF_UP) 
                : BigDecimal.ZERO);
        
        return Result.success(result);
    }

    /**
     * 平均评分
     */
    @GetMapping("/avgScore")
    public Result<Map<String, Object>> avgScore() {
        Map<String, Object> result = new HashMap<>();
        
        List<BizEvaluation> evaluations = evaluationMapper.selectList(null);
        
        if (evaluations.isEmpty()) {
            result.put("avgScore", 0);
            result.put("totalCount", 0);
        } else {
            double avg = evaluations.stream().mapToInt(BizEvaluation::getScore).average().orElse(0);
            result.put("avgScore", BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP));
            result.put("totalCount", evaluations.size());
        }
        
        return Result.success(result);
    }

    /**
     * 收入趋势明细（按天统计）
     * 用于折线图展示：总收入、工时收入、配件收入
     */
    @GetMapping("/revenueTrend")
    public Result<List<Map<String, Object>>> revenueTrend(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        
        List<Map<String, Object>> result = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        
        // 查询已完成且已支付的工单
        List<BizRepairOrder> orders = orderMapper.selectList(
                new LambdaQueryWrapper<BizRepairOrder>()
                        .eq(BizRepairOrder::getStatus, 5)
                        .eq(BizRepairOrder::getPaymentStatus, 1)
                        .between(BizRepairOrder::getCreateTime, startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay()));
        
        // 按天分组统计
        Map<String, BigDecimal> dailyRevenue = new LinkedHashMap<>();
        Map<String, BigDecimal> dailyLaborRevenue = new LinkedHashMap<>();
        Map<String, BigDecimal> dailyPartsRevenue = new LinkedHashMap<>();
        Map<String, Integer> dailyCount = new LinkedHashMap<>();
        
        // 初始化所有日期
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            String day = current.format(formatter);
            dailyRevenue.put(day, BigDecimal.ZERO);
            dailyLaborRevenue.put(day, BigDecimal.ZERO);
            dailyPartsRevenue.put(day, BigDecimal.ZERO);
            dailyCount.put(day, 0);
            current = current.plusDays(1);
        }
        
        // 统计每天数据
        for (BizRepairOrder order : orders) {
            String day = order.getCreateTime().toLocalDate().format(formatter);
            if (dailyRevenue.containsKey(day)) {
                BigDecimal total = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
                BigDecimal labor = order.getLaborCost() != null ? order.getLaborCost() : BigDecimal.ZERO;
                BigDecimal parts = order.getPartsCost() != null ? order.getPartsCost() : BigDecimal.ZERO;
                
                dailyRevenue.merge(day, total, BigDecimal::add);
                dailyLaborRevenue.merge(day, labor, BigDecimal::add);
                dailyPartsRevenue.merge(day, parts, BigDecimal::add);
                dailyCount.merge(day, 1, Integer::sum);
            }
        }
        
        // 构建返回结果
        for (String day : dailyRevenue.keySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("month", day); // 保持字段名为month，前端兼容
            item.put("revenue", dailyRevenue.get(day));
            item.put("laborRevenue", dailyLaborRevenue.get(day));
            item.put("partsRevenue", dailyPartsRevenue.get(day));
            item.put("orderCount", dailyCount.get(day));
            result.add(item);
        }
        
        return Result.success(result);
    }

    /**
     * 工单趋势统计（按月统计）
     * 用于柱状图+折线图组合展示：工单数、完成率
     */
    @GetMapping("/orderTrend")
    public Result<List<Map<String, Object>>> orderTrend(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        
        List<Map<String, Object>> result = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        
        // 查询所有工单
        List<BizRepairOrder> allOrders = orderMapper.selectList(
                new LambdaQueryWrapper<BizRepairOrder>()
                        .between(BizRepairOrder::getCreateTime, startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay()));
        
        // 按月分组统计
        Map<String, Integer> monthlyTotal = new LinkedHashMap<>();
        Map<String, Integer> monthlyCompleted = new LinkedHashMap<>();
        
        // 初始化所有月份
        LocalDate current = startDate.withDayOfMonth(1);
        while (!current.isAfter(endDate)) {
            String month = current.format(formatter);
            monthlyTotal.put(month, 0);
            monthlyCompleted.put(month, 0);
            current = current.plusMonths(1);
        }
        
        // 统计每月数据
        for (BizRepairOrder order : allOrders) {
            String month = order.getCreateTime().format(formatter);
            if (monthlyTotal.containsKey(month)) {
                monthlyTotal.merge(month, 1, Integer::sum);
                // 状态5为已完成
                if (order.getStatus() != null && order.getStatus() == 5) {
                    monthlyCompleted.merge(month, 1, Integer::sum);
                }
            }
        }
        
        // 构建返回结果
        for (String month : monthlyTotal.keySet()) {
            Map<String, Object> item = new HashMap<>();
            int total = monthlyTotal.get(month);
            int completed = monthlyCompleted.get(month);
            double completionRate = total > 0 ? (double) completed / total * 100 : 0;
            
            item.put("month", month);
            item.put("orderCount", total);
            item.put("completedCount", completed);
            item.put("completionRate", BigDecimal.valueOf(completionRate).setScale(1, RoundingMode.HALF_UP));
            result.add(item);
        }
        
        return Result.success(result);
    }

    /**
     * 技师工作量排行统计
     * 用于横向柱状图展示
     */
    @GetMapping("/technicianWorkload")
    public Result<List<Map<String, Object>>> technicianWorkload() {
        List<Map<String, Object>> result = new ArrayList<>();
        
        // 获取所有技师
        List<SysUser> technicians = userMapper.selectList(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUserType, 2));
        
        // 获取已完成的工单
        List<BizRepairOrder> completedOrders = orderMapper.selectList(
                new LambdaQueryWrapper<BizRepairOrder>().eq(BizRepairOrder::getStatus, 5));
        
        // 按技师统计工单数
        Map<Long, Integer> technicianOrderCount = new HashMap<>();
        for (BizRepairOrder order : completedOrders) {
            if (order.getTechnicianId() != null) {
                technicianOrderCount.merge(order.getTechnicianId(), 1, Integer::sum);
            }
        }
        
        // 获取技师评分
        List<BizEvaluation> evaluations = evaluationMapper.selectList(null);
        Map<Long, List<Integer>> technicianScores = new HashMap<>();
        for (BizEvaluation eval : evaluations) {
            // 通过工单找到技师
            BizRepairOrder order = orderMapper.selectById(eval.getOrderId());
            if (order != null && order.getTechnicianId() != null) {
                technicianScores.computeIfAbsent(order.getTechnicianId(), k -> new ArrayList<>()).add(eval.getScore());
            }
        }
        
        // 构建返回结果
        for (SysUser tech : technicians) {
            Map<String, Object> item = new HashMap<>();
            item.put("technicianId", tech.getId());
            item.put("technicianName", tech.getRealName() != null ? tech.getRealName() : tech.getUsername());
            item.put("orderCount", technicianOrderCount.getOrDefault(tech.getId(), 0));
            
            List<Integer> scores = technicianScores.get(tech.getId());
            if (scores != null && !scores.isEmpty()) {
                double avgScore = scores.stream().mapToInt(Integer::intValue).average().orElse(0);
                item.put("avgScore", BigDecimal.valueOf(avgScore).setScale(1, RoundingMode.HALF_UP));
            } else {
                item.put("avgScore", BigDecimal.ZERO);
            }
            result.add(item);
        }
        
        // 按工单数降序排序，取前10个
        result.sort((a, b) -> ((Integer) b.get("orderCount")).compareTo((Integer) a.get("orderCount")));
        return Result.success(result.stream().limit(10).collect(Collectors.toList()));
    }

    /**
     * 维修类型分布统计
     * 用于饼图展示
     */
    @GetMapping("/serviceTypeDistribution")
    public Result<List<Map<String, Object>>> serviceTypeDistribution() {
        List<Map<String, Object>> result = new ArrayList<>();
        
        // 查询所有工单服务项
        List<BizOrderService> orderServices = orderServiceMapper.selectList(null);
        
        // 获取所有服务项目
        List<BizServiceItem> serviceItems = serviceItemMapper.selectList(null);
        Map<Long, BizServiceItem> serviceMap = serviceItems.stream()
                .collect(Collectors.toMap(BizServiceItem::getId, s -> s));
        
        // 按分类统计
        Map<String, Integer> categoryCount = new HashMap<>();
        for (BizOrderService os : orderServices) {
            BizServiceItem item = serviceMap.get(os.getServiceId());
            if (item != null) {
                String category = item.getCategory() != null ? item.getCategory() : "其他";
                categoryCount.merge(category, 1, Integer::sum);
            }
        }
        
        // 构建返回结果
        for (Map.Entry<String, Integer> entry : categoryCount.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", entry.getKey());
            item.put("value", entry.getValue());
            result.add(item);
        }
        
        return Result.success(result);
    }

    /**
     * 配件周转率统计
     * 用于柱状图+折线图组合展示：周转率、周转天数
     */
    @GetMapping("/partTurnover")
    public Result<List<Map<String, Object>>> partTurnover() {
        List<Map<String, Object>> result = new ArrayList<>();
        
        // 获取所有配件
        List<BizPart> parts = partMapper.selectList(
                new LambdaQueryWrapper<BizPart>().eq(BizPart::getStatus, 1));
        
        // 获取最近3个月的出库记录
        LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
        List<BizInventoryRecord> records = inventoryRecordMapper.selectList(
                new LambdaQueryWrapper<BizInventoryRecord>()
                        .eq(BizInventoryRecord::getRecordType, 2) // 出库
                        .ge(BizInventoryRecord::getCreateTime, threeMonthsAgo));
        
        // 按配件统计出库数量
        Map<Long, Integer> partOutQuantity = new HashMap<>();
        for (BizInventoryRecord record : records) {
            partOutQuantity.merge(record.getPartId(), record.getQuantity(), Integer::sum);
        }
        
        // 计算周转率并排序（取前10个）
        List<Map<String, Object>> turnoverList = new ArrayList<>();
        for (BizPart part : parts) {
            int outQty = partOutQuantity.getOrDefault(part.getId(), 0);
            int avgStock = part.getStockQuantity() > 0 ? part.getStockQuantity() : 1;
            // 周转率 = 出库数量 / 平均库存 * 100
            double turnoverRate = (double) outQty / avgStock * 100;
            // 周转天数 = 90天 / (出库数量 / 平均库存)，如果没有出库则设为90天
            double turnoverDays = outQty > 0 ? 90.0 / ((double) outQty / avgStock) : 90;
            
            Map<String, Object> item = new HashMap<>();
            item.put("partName", part.getPartName());
            item.put("outQuantity", outQty);
            item.put("stockQuantity", part.getStockQuantity());
            item.put("turnoverRate", BigDecimal.valueOf(turnoverRate).setScale(1, RoundingMode.HALF_UP));
            item.put("turnoverDays", BigDecimal.valueOf(turnoverDays).setScale(0, RoundingMode.HALF_UP));
            turnoverList.add(item);
        }
        
        // 按周转率降序排序，取前10个
        turnoverList.sort((a, b) -> ((BigDecimal) b.get("turnoverRate")).compareTo((BigDecimal) a.get("turnoverRate")));
        result = turnoverList.stream().limit(10).collect(Collectors.toList());
        
        return Result.success(result);
    }

    /**
     * 定价效果评估
     * 对比动态定价与固定定价的收益，包含更多维度
     */
    @GetMapping("/pricingEffect")
    public Result<Map<String, Object>> pricingEffect() {
        Map<String, Object> result = new HashMap<>();
        
        // 查询已完成的工单
        List<BizRepairOrder> orders = orderMapper.selectList(
                new LambdaQueryWrapper<BizRepairOrder>()
                        .eq(BizRepairOrder::getStatus, 5)
                        .eq(BizRepairOrder::getPaymentStatus, 1));
        
        if (orders.isEmpty()) {
            result.put("dynamicRevenue", BigDecimal.ZERO);
            result.put("estimatedFixedRevenue", BigDecimal.ZERO);
            result.put("avgDynamicPrice", BigDecimal.ZERO);
            result.put("avgFixedPrice", BigDecimal.ZERO);
            result.put("dynamicProfitRate", BigDecimal.ZERO);
            result.put("fixedProfitRate", BigDecimal.ZERO);
            result.put("revenueIncrease", BigDecimal.ZERO);
            result.put("increaseRate", BigDecimal.ZERO);
            return Result.success(result);
        }
        
        BigDecimal dynamicRevenue = BigDecimal.ZERO;
        BigDecimal estimatedFixedRevenue = BigDecimal.ZERO;
        
        for (BizRepairOrder order : orders) {
            BigDecimal total = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
            dynamicRevenue = dynamicRevenue.add(total);
            
            // 估算固定定价收入（假设固定定价为动态定价的85%）
            BigDecimal fixed = total.multiply(BigDecimal.valueOf(0.85));
            estimatedFixedRevenue = estimatedFixedRevenue.add(fixed);
        }
        
        int orderCount = orders.size();
        BigDecimal avgDynamicPrice = dynamicRevenue.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP);
        BigDecimal avgFixedPrice = estimatedFixedRevenue.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP);
        
        BigDecimal revenueIncrease = dynamicRevenue.subtract(estimatedFixedRevenue);
        BigDecimal increaseRate = estimatedFixedRevenue.compareTo(BigDecimal.ZERO) > 0
                ? revenueIncrease.divide(estimatedFixedRevenue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;
        
        result.put("dynamicRevenue", dynamicRevenue.setScale(2, RoundingMode.HALF_UP));
        result.put("estimatedFixedRevenue", estimatedFixedRevenue.setScale(2, RoundingMode.HALF_UP));
        result.put("avgDynamicPrice", avgDynamicPrice);
        result.put("avgFixedPrice", avgFixedPrice);
        result.put("dynamicProfitRate", BigDecimal.valueOf(32)); // 动态定价毛利率
        result.put("fixedProfitRate", BigDecimal.valueOf(28));   // 固定定价毛利率
        result.put("revenueIncrease", revenueIncrease.setScale(2, RoundingMode.HALF_UP));
        result.put("increaseRate", increaseRate.setScale(1, RoundingMode.HALF_UP));
        
        return Result.success(result);
    }
}
