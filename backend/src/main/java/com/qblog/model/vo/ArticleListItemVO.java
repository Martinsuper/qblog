package com.qblog.model.vo;

import lombok.Data;

/**
 * 文章列表项 VO
 */
@Data
public class ArticleListItemVO {

    private Long id;

    private String title;

    private String summary;

    private String coverImage;

    private UserVO author;

    private CategoryVO category;

    private java.util.List<TagVO> tags;

    private Integer viewCount;

    private Integer likeCount;

    private Integer commentCount;

    private Integer status;

    private Integer top;

    private String createTime;

    private String publishTime;
}
