package com.panda.mapper;

import com.panda.entity.SubscriptionPackage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SubscriptionPackageMapper {

    List<SubscriptionPackage> listAll();

    SubscriptionPackage getById(Long id);

    /**
     * 分页查询套餐列表
     */
    List<SubscriptionPackage> getPackageList(@Param("offset") Integer offset,
                                             @Param("pageSize") Integer pageSize,
                                             @Param("keyword") String keyword);

    /**
     * 统计套餐总数
     */
    Integer countPackageList(@Param("keyword") String keyword);

    /**
     * 新增套餐
     */
    int insertPackage(SubscriptionPackage subscriptionPackage);

    /**
     * 更新套餐
     */
    int updatePackage(SubscriptionPackage subscriptionPackage);

    /**
     * 根据ID删除套餐
     */
    int deleteById(@Param("id") Long id);
}
