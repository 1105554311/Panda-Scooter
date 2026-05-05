package com.panda.controller.admin;

import com.panda.dto.AddDispatcherDTO;
import com.panda.dto.DispatcherListDTO;
import com.panda.dto.EditDispatcherDTO;
import com.panda.result.Result;
import com.panda.service.AdminService;
import com.panda.vo.AddDispatcherVO;
import com.panda.vo.DispatcherListVO;
import com.panda.vo.EditDispatcherVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/dispatchers")
@Slf4j
public class AdminDispatcherController {   // 改名

    @Autowired
    private AdminService adminService;

    @GetMapping("/getDispatcherList")
    public Result<DispatcherListVO> getDispatcherList(DispatcherListDTO dispatcherListDTO) {
        log.info("获取调度员列表请求，参数：{}", dispatcherListDTO);
        DispatcherListVO dispatcherListVO = adminService.getDispatcherList(dispatcherListDTO);
        return Result.success(dispatcherListVO);
    }

    /**
     * 新增调度员
     */
    @PostMapping("/addDispatcher")
    public Result<AddDispatcherVO> addDispatcher(@RequestBody AddDispatcherDTO addDispatcherDTO) {
        log.info("新增调度员请求，参数：{}", addDispatcherDTO);
        AddDispatcherVO addDispatcherVO = adminService.addDispatcher(addDispatcherDTO);
        return Result.success(addDispatcherVO);
    }

    /**
     * 编辑调度员
     */
    @PostMapping("/editDispatcher")
    public Result<EditDispatcherVO> editDispatcher(@RequestBody EditDispatcherDTO editDispatcherDTO) {
        log.info("编辑调度员请求，参数：{}", editDispatcherDTO);
        EditDispatcherVO editDispatcherVO = adminService.editDispatcher(editDispatcherDTO);
        return Result.success(editDispatcherVO);
    }
}