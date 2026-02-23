package com.qblog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文章标签关联实体
 */
@Data
@TableName("article_tag")
public class ArticleTag implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 文章 ID
     */
    private Long articleId;

    /**
     * 标签 ID
     */
    private Long tagId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
