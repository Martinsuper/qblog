package com.qblog.model.vo;

import lombok.Data;

/**
 * 标签 VO
 */
@Data
public class TagVO {

    private Long id;

    private String name;

    private Integer articleCount;
}
