package com.panda.controller.admin;

import com.panda.dto.*;
import com.panda.result.Result;
import com.panda.service.AdminService;
import com.panda.vo.AddZoneVO;
import com.panda.vo.EditZoneVO;
import com.panda.vo.ZoneDetailVO;
import com.panda.vo.ZoneListVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/zones")
@Slf4j
@RequiredArgsConstructor
public class ZoneController {

    @Autowired
    private AdminService adminService;

    /**
     * 获取片区列表
     */
    @GetMapping("/getZoneList")
    public Result<ZoneListVO> getZoneList(ZoneListDTO zoneListDTO) {
        log.info("获取片区列表请求，参数：{}", zoneListDTO);
        ZoneListVO zoneListVO = adminService.getZoneList(zoneListDTO);
        return Result.success(zoneListVO);
    }

    /**
     * 新增片区
     */
    @PostMapping("/addZone")
    public Result<AddZoneVO> addZone(@RequestBody AddZoneDTO addZoneDTO) {
        log.info("新增片区请求，参数：{}", addZoneDTO);
        AddZoneVO addZoneVO = adminService.addZone(addZoneDTO);
        return Result.success(addZoneVO);
    }

    /**
     * 获取片区详情
     */
    @GetMapping("/getZoneDetail")
    public Result<ZoneDetailVO> getZoneDetail(ZoneDetailDTO zoneDetailDTO) {
        log.info("获取片区详情请求，参数：{}", zoneDetailDTO);
        ZoneDetailVO zoneDetailVO = adminService.getZoneDetail(zoneDetailDTO);
        return Result.success(zoneDetailVO);
    }

    /**
     * 编辑片区
     */
    @PostMapping("/editZone")
    public Result<EditZoneVO> editZone(@RequestBody EditZoneDTO editZoneDTO) {
        log.info("编辑片区请求，参数：{}", editZoneDTO);
        EditZoneVO editZoneVO = adminService.editZone(editZoneDTO);
        return Result.success(editZoneVO);
    }

    /**
     * 删除片区
     */
    @DeleteMapping("/deleteZone")
    public Result<Void> deleteZone(@RequestBody DeleteZoneDTO deleteZoneDTO) {
        log.info("删除片区请求，参数：{}", deleteZoneDTO);
        adminService.deleteZone(deleteZoneDTO);
        return Result.success();
    }
}