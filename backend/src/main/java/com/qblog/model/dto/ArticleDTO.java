package com.qblog.model.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 文章创建/更新 DTO
 */
@Data
public class ArticleDTO {

    private Long id;

    private Long version;

    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题长度不能超过 200")
    private String title;

    @Size(max = 500, message = "摘要长度不能超过 500")
    private String summary;

    @NotBlank(message = "内容不能为空")
    private String content;

    private String coverImage;

    private Long categoryId;

    private java.util.List<Long> tagIds;

    /**
     * 状态：0-草稿 1-发布。可空：创建时缺省为草稿，更新时为 null 表示保持原状态，
     * 避免"PUT 不带 status 就意外发布"
     */
    private Integer status;
}
