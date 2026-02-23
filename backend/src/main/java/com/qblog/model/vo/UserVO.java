package com.qblog.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息 VO
 */
@Data
public class UserVO {

    private Long id;

    private String username;

    private String nickname;

    private String avatar;

    private String email;

    private Integer role;

    private Integer articleCount;

    private Integer likeCount;

    private LocalDateTime createTime;
}
