package com.panda.mapper;

import com.panda.entity.Area;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AreaMapper {

    Area getById(@Param("id") Long id);

    List<Area> listAll();

    /**
     * 分页查询片区列表
     */
    List<Area> getZoneList(@Param("offset") Integer offset,
                           @Param("pageSize") Integer pageSize,
                           @Param("keyword") String keyword);

    /**
     * 统计片区总数
     */
    Integer countZoneList(@Param("keyword") String keyword);

    /**
     * 新增片区
     */
    int insertArea(Area area);

    /**
     * 根据ID获取片区详情
     */
    Area getZoneDetailById(@Param("id") Long id);

    /**
     * 更新片区
     */
    int updateArea(Area area);

    /**
     * 根据ID删除片区
     */
    int deleteById(@Param("id") Long id);
}
