package com.qblog.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

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

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
