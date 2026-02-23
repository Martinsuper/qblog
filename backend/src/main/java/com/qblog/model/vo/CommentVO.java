package com.qblog.model.vo;

import lombok.Data;

import java.util.List;

/**
 * 评论 VO
 */
@Data
public class CommentVO {

    private Long id;

    private UserVO user;

    private String content;

    private Integer likeCount;

    private Boolean isLiked;

    private String createTime;

    private List<CommentVO> replies;
}
