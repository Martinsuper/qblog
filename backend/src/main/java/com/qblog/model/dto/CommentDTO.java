package com.qblog.model.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 评论 DTO
 */
@Data
public class CommentDTO {

    @NotBlank(message = "评论内容不能为空")
    private String content;

    private Long parentId = 0L;
}
