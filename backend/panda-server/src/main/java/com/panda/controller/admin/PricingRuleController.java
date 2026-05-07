package com.panda.controller.admin;

import com.panda.dto.PricingRuleEditDTO;
import com.panda.result.Result;
import com.panda.service.AdminService;
import com.panda.vo.PricingRuleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/princingRule")
@Slf4j
@RequiredArgsConstructor
public class PricingRuleController {

    @Autowired
    private AdminService adminService;

    /**
     * 获取定价策略
     */
    @GetMapping("/getRules")
    public Result<PricingRuleVO> getRules() {  // 改为 PricingRuleVO，不是 List
        log.info("获取定价策略请求");
        PricingRuleVO rule = adminService.getPricingRules();
        return Result.success(rule);
    }

    /**
     * 修改定价策略
     */
    @PostMapping("/editRules")
    public Result<Void> editRules(@RequestBody PricingRuleEditDTO editDTO) {
        log.info("修改定价策略请求，参数：{}", editDTO);
        adminService.editRules(editDTO);
        return Result.success();
    }
}