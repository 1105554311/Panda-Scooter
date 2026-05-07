package com.panda.mapper;

import com.panda.dto.PricingRuleEditDTO;
import com.panda.entity.PricingRule;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface PricingRuleMapper {

    /**
     * 获取所有定价规则
     */
    @Select("SELECT * FROM bike_system.pricing_rule ORDER BY id")
    List<PricingRule> getAllRules();

    /**
     * 更新定价规则（更新 id=1 的记录）
     */
    @Update("UPDATE pricing_rule SET " +
            "price_per_min = #{pricePerMin}, " +
            "base_price = #{basePrice}, " +
            "billing_interval = #{billingInterval} " +
            "WHERE id = 1")
    int updateRule(PricingRuleEditDTO editDTO);

    /**
     * 插入定价规则
     */
    @Insert("INSERT INTO pricing_rule (price_per_min, base_price, billing_interval, create_time) " +
            "VALUES (#{pricePerMin}, #{basePrice}, #{billingInterval}, NOW())")
    int insertRule(PricingRuleEditDTO editDTO);
}