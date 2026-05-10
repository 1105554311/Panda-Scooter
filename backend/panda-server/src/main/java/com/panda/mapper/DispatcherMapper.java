package com.panda.mapper;

import com.panda.entity.Dispatcher;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DispatcherMapper {

    int insert(Dispatcher dispatcher);

    Dispatcher getByEmail(@Param("email") String email);

    Dispatcher getById(@Param("id") Long id);

    int updatePasswordById(@Param("id") Long id, @Param("password") String password);

    int deleteById(@Param("id") Long id);

    /**
     * 根据片区ID获取第一个调度员
     */
    @Select("SELECT id, name, email, area_id FROM dispatcher WHERE area_id = #{areaId} LIMIT 1")
    Dispatcher getByAreaId(@Param("areaId") Long areaId);

    @Select("SELECT id, name, email, area_id FROM dispatcher WHERE area_id = #{areaId} ORDER BY id DESC")
    List<Dispatcher> listByAreaId(@Param("areaId") Long areaId);

    /**
     * 分页查询调度员列表
     */
    List<Dispatcher> getDispatcherList(@Param("offset") Integer offset,
                                       @Param("pageSize") Integer pageSize,
                                       @Param("keyword") String keyword,
                                       @Param("areaId") Long areaId);
    /**
     * 统计调度员总数
     */
    Integer countDispatcherList(@Param("keyword") String keyword,
                                @Param("areaId") Long areaId);

    /**
     * 新增调度员
     */
    int insertDispatcher(Dispatcher dispatcher);

    /**
     * 更新调度员
     */
    int updateDispatcher(Dispatcher dispatcher);
}