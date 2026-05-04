package com.panda.controller.admin;

import com.panda.dto.AddPackageDTO;
import com.panda.dto.DeletePackageDTO;
import com.panda.dto.EditPackageDTO;
import com.panda.dto.PackageListDTO;
import com.panda.result.Result;
import com.panda.service.AdminService;
import com.panda.vo.PackageListVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/packages")
@Slf4j
@RequiredArgsConstructor
public class PackageController {

    @Autowired
    private AdminService adminService;

    /**
     * 获取套餐列表
     */
    @GetMapping("/getPackageList")
    public Result<PackageListVO> getPackageList(PackageListDTO packageListDTO) {
        log.info("获取套餐列表请求，参数：{}", packageListDTO);
        PackageListVO packageListVO = adminService.getPackageList(packageListDTO);
        return Result.success(packageListVO);
    }

    /**
     * 新增套餐
     */
    @PostMapping("/addPackage")
    public Result<Void> addPackage(@RequestBody AddPackageDTO addPackageDTO) {
        log.info("新增套餐请求，参数：{}", addPackageDTO);
        adminService.addPackage(addPackageDTO);
        return Result.success();
    }

    /**
     * 编辑套餐
     */
    @PostMapping("/editPackage")
    public Result<Void> editPackage(@RequestBody EditPackageDTO editPackageDTO) {
        log.info("编辑套餐请求，参数：{}", editPackageDTO);
        adminService.editPackage(editPackageDTO);
        return Result.success();
    }

    /**
     * 删除套餐
     */
    @DeleteMapping("/deletePackage")
    public Result<Void> deletePackage(@RequestBody DeletePackageDTO deletePackageDTO) {
        log.info("删除套餐请求，参数：{}", deletePackageDTO);
        adminService.deletePackage(deletePackageDTO);
        return Result.success();
    }
}