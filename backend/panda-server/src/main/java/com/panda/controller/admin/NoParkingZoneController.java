package com.panda.controller.admin;

import com.panda.dto.AddNoParkingZoneDTO;
import com.panda.dto.DeleteNoParkingZoneDTO;
import com.panda.dto.EditNoParkingZoneDTO;
import com.panda.dto.NoParkingZoneListDTO;
import com.panda.result.Result;
import com.panda.service.AdminService;
import com.panda.vo.NoParkingZoneListVO;
import com.panda.vo.NoParkingZoneVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/noParkingZones")
@Slf4j
@RequiredArgsConstructor
public class NoParkingZoneController {

    private final AdminService adminService;

    @GetMapping("/getZoneList")
    public Result<NoParkingZoneListVO> getZoneList(NoParkingZoneListDTO listDTO) {
        return Result.success(adminService.getNoParkingZoneList(listDTO));
    }

    @PostMapping("/addZone")
    public Result<NoParkingZoneVO> addZone(@RequestBody AddNoParkingZoneDTO addDTO) {
        return Result.success(adminService.addNoParkingZone(addDTO));
    }

    @PostMapping("/editZone")
    public Result<NoParkingZoneVO> editZone(@RequestBody EditNoParkingZoneDTO editDTO) {
        return Result.success(adminService.editNoParkingZone(editDTO));
    }

    @DeleteMapping("/deleteZone")
    public Result<Void> deleteZone(@RequestParam("NoParkingAreaId") Long noParkingAreaId,
                                   @RequestParam(value = "name", required = false) String name) {
        DeleteNoParkingZoneDTO deleteDTO = new DeleteNoParkingZoneDTO();
        deleteDTO.setId(noParkingAreaId);
        deleteDTO.setName(name);
        adminService.deleteNoParkingZone(deleteDTO);
        return Result.success();
    }
}
