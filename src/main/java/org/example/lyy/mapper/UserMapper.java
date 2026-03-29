package org.example.lyy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.lyy.entity.User;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}