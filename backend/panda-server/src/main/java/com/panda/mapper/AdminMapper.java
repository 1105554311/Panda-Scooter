package com.panda.mapper;

import com.panda.entity.Admin;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AdminMapper {
    @Select("SELECT * FROM admin WHERE email = #{email}")
    Admin getByEmail(String email);

    @Update("UPDATE admin SET password = #{password} WHERE id = #{id}")
    int updatePasswordById(@Param("id") Long id, @Param("password") String password);
}
