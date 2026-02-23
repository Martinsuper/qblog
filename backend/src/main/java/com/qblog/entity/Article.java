package com.qblog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文章实体
 */
@Data
@TableName("article")
public class Article implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 文章 ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 标题
     */
    private String title;

    /**
     * 摘要
     */
    private String summary;

    /**
     * 文章内容 (Markdown/HTML)
     */
    private String content;

    /**
     * 封面图 URL
     */
    private String coverImage;

    /**
     * 作者 ID
     */
    private Long authorId;

    /**
     * 分类 ID
     */
    private Long categoryId;

    /**
     * 浏览量
     */
    private Integer viewCount;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 评论数
     */
    private Integer commentCount;

    /**
     * 状态：0-草稿 1-已发布 2-已删除
     */
    private Integer status;

    /**
     * 是否置顶：0-否 1-是
     */
    private Integer top;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 发布时间
     */
    private LocalDateTime publishTime;
}
