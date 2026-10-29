package com.qblog.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qblog.common.JwtUtil;
import com.qblog.common.SecurityUtils;
import com.qblog.common.exception.BusinessException;
import com.qblog.common.exception.ResourceNotFoundException;
import com.qblog.entity.User;
import com.qblog.mapper.UserMapper;
import com.qblog.model.dto.ChangePasswordDTO;
import com.qblog.model.dto.LoginDTO;
import com.qblog.model.dto.RegisterDTO;
import com.qblog.model.vo.UserVO;
import com.qblog.service.CacheService;
import com.qblog.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;



/**
 * 用户服务实现
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final CacheService cacheService;

    private static final String CACHE_USER_PWD_VERSION = "user:pwdver:";

    @Override
    public String login(LoginDTO loginDTO) {
        User user = getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, loginDTO.getUsername()));

        // 用户不存在与密码错误返回相同提示，避免账号枚举
        if (user == null || !passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }

        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(403, "账号已被禁用");
        }

        return jwtUtil.generateToken(user.getId(), user.getUsername(),
                user.getRole(), getPasswordVersionValue(user));
    }

    @Override
    public void register(RegisterDTO registerDTO) {
        Long count = count(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, registerDTO.getUsername()));
        if (count > 0) {
            throw new BusinessException(400, "用户名已存在");
        }

        User user = new User();
        BeanUtil.copyProperties(registerDTO, user);
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setRole(0);
        user.setStatus(1);
        user.setPasswordVersion(0);

        save(user);
    }

    @Override
    public UserVO getCurrentUser() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }
        return convertToVO(user);
    }

    @Override
    public UserVO getUserById(Long id) {
        User user = getById(id);
        if (user == null) {
            throw new ResourceNotFoundException("用户", id);
        }
        return convertToVO(user);
    }

    @Override
    public void changePassword(ChangePasswordDTO changePasswordDTO) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }

        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }

        if (!passwordEncoder.matches(changePasswordDTO.getOldPassword(), user.getPassword())) {
            throw new BusinessException(400, "旧密码错误");
        }

        if (changePasswordDTO.getOldPassword().equals(changePasswordDTO.getNewPassword())) {
            throw new BusinessException(400, "新密码不能与旧密码相同");
        }

        user.setPassword(passwordEncoder.encode(changePasswordDTO.getNewPassword()));
        // 密码版本 +1，使所有已签发的旧 Token 立即失效
        user.setPasswordVersion(getPasswordVersionValue(user) + 1);
        updateById(user);

        cacheService.delete(CACHE_USER_PWD_VERSION + userId);
    }

    @Override
    public Integer getPasswordVersion(Long userId) {
        if (userId == null) {
            return null;
        }
        // Revocation data must not be cached: an in-flight cache fill after a password
        // change could otherwise resurrect an old version for the cache TTL.
        User user = getById(userId);
        return user == null ? -1 : getPasswordVersionValue(user);
    }

    private Integer getPasswordVersionValue(User user) {
        return user.getPasswordVersion() == null ? 0 : user.getPasswordVersion();
    }

    private UserVO convertToVO(User user) {
        UserVO vo = new UserVO();
        BeanUtil.copyProperties(user, vo);
        return vo;
    }
}
