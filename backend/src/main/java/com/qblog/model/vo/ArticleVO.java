package com.qblog.model.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章详情 VO
 */
@Data
public class ArticleVO {

    private Long id;

    private Long version;

    private String title;

    private String summary;

    private String content;

    private String coverImage;

    private UserVO author;

    private CategoryVO category;

    private List<TagVO> tags;

    private Integer viewCount;

    /**
     * 状态：0-草稿 1-已发布 2-已删除
     */
    private Integer status;

    private Integer top;

    private Boolean isLiked;

    private Boolean isFavorited;

    private LocalDateTime createTime;

    private LocalDateTime publishTime;
}
