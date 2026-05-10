package com.panda.controller.admin;

import com.panda.result.Result;
import com.panda.service.AdminService;
import com.panda.vo.AdminScooterListVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/scooter")
@Slf4j
@RequiredArgsConstructor
public class AdminScooterController {

    private final AdminService adminService;

    @GetMapping("/getScooterList")
    public Result<AdminScooterListVO> getScooterList(@RequestParam(value = "areaId", required = false) Integer areaId) {
        return Result.success(adminService.getScooterList(areaId));
    }
}
