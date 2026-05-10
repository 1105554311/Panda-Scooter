package com.panda.controller.admin;

import com.panda.dto.AddParkingPointDTO;
import com.panda.dto.DeleteParkingPointDTO;
import com.panda.dto.EditParkingPointDTO;
import com.panda.dto.ParkingPointListDTO;
import com.panda.result.Result;
import com.panda.service.AdminService;
import com.panda.vo.ParkingPointListVO;
import com.panda.vo.ParkingPointVO;
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
@RequestMapping("/admin/ParkingPoint")
@Slf4j
@RequiredArgsConstructor
public class ParkingPointAdminController {

    private final AdminService adminService;

    @GetMapping("/getPointList")
    public Result<ParkingPointListVO> getPointList(ParkingPointListDTO listDTO) {
        return Result.success(adminService.getParkingPointList(listDTO));
    }

    @PostMapping("/addPoint")
    public Result<ParkingPointVO> addPoint(@RequestBody AddParkingPointDTO addDTO) {
        return Result.success(adminService.addParkingPoint(addDTO));
    }

    @PostMapping("/editPoint")
    public Result<ParkingPointVO> editPoint(@RequestBody EditParkingPointDTO editDTO) {
        return Result.success(adminService.editParkingPoint(editDTO));
    }

    @DeleteMapping("/deletePoint")
    public Result<Void> deletePoint(@RequestParam("ParkingPointId") Long parkingPointId,
                                    @RequestParam(value = "name", required = false) String name) {
        DeleteParkingPointDTO deleteDTO = new DeleteParkingPointDTO();
        deleteDTO.setId(parkingPointId);
        deleteDTO.setName(name);
        adminService.deleteParkingPoint(deleteDTO);
        return Result.success();
    }
}
