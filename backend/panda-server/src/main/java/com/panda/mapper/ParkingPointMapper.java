package com.panda.mapper;

import com.panda.entity.ParkingPoint;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ParkingPointMapper {

    List<ParkingPoint> listEnabled();

    List<ParkingPoint> listForAdmin(@Param("offset") Integer offset,
                                    @Param("pageSize") Integer pageSize,
                                    @Param("keyword") String keyword);

    Integer countForAdmin(@Param("keyword") String keyword);

    ParkingPoint getById(@Param("id") Long id);

    int insert(ParkingPoint parkingPoint);

    int update(ParkingPoint parkingPoint);

    int deleteById(@Param("id") Long id);
}
