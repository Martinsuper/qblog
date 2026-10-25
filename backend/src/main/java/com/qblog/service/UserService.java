package com.qblog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qblog.entity.User;
import com.qblog.model.dto.ChangePasswordDTO;
import com.qblog.model.dto.LoginDTO;
import com.qblog.model.dto.RegisterDTO;
import com.qblog.model.vo.UserVO;

/**
 * 用户服务接口
 */
public interface UserService extends IService<User> {

    /**
     * 用户登录
     */
    String login(LoginDTO loginDTO);

    /**
     * 用户注册
     */
    void register(RegisterDTO registerDTO);

    /**
     * 获取当前用户信息
     */
    UserVO getCurrentUser();

    /**
     * 根据 ID 获取用户信息
     */
    UserVO getUserById(Long id);

    /**
     * 修改密码
     */
    void changePassword(ChangePasswordDTO changePasswordDTO);

    /**
     * 获取用户当前密码版本（带缓存），用于校验 Token 是否已因改密而失效
     */
    Integer getPasswordVersion(Long userId);
}
