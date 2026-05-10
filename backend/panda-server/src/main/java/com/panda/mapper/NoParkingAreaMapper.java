package com.panda.mapper;

import com.panda.entity.NoParkingArea;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NoParkingAreaMapper {

    List<NoParkingArea> listEnabled();

    List<NoParkingArea> listForAdmin(@Param("offset") Integer offset,
                                     @Param("pageSize") Integer pageSize,
                                     @Param("keyword") String keyword);

    Integer countForAdmin(@Param("keyword") String keyword);

    NoParkingArea getById(@Param("id") Long id);

    int insert(NoParkingArea noParkingArea);

    int update(NoParkingArea noParkingArea);

    int deleteById(@Param("id") Long id);
}
