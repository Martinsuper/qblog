package com.qblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qblog.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper 接口
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 按显式主键插入（备份恢复专用），保留原 id 供 author_id 等引用
     */
    @Insert("INSERT INTO `user` (id, username, password, nickname, avatar, email, role, status, " +
            "password_version, create_time, update_time) VALUES " +
            "(#{id}, #{username}, #{password}, #{nickname}, #{avatar}, #{email}, " +
            "COALESCE(#{role}, 0), COALESCE(#{status}, 1), COALESCE(#{passwordVersion}, 0), " +
            "#{createTime}, #{updateTime})")
    int insertWithId(User user);
}
