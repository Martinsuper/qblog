package com.qblog.model.vo;

import lombok.Data;

/**
 * 分类 VO
 */
@Data
public class CategoryVO {

    private Long id;

    private String name;

    private String description;

    private Integer sort;

    private Integer articleCount;
}
