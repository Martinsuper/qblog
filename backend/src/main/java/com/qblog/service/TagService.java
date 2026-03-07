package com.qblog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qblog.entity.Tag;
import com.qblog.model.vo.TagVO;

import java.util.List;

/**
 * 标签服务接口
 */
public interface TagService extends IService<Tag> {

    /**
     * 获取标签列表及其文章数量
     */
    List<TagVO> listWithArticleCount();

    /**
     * 删除标签（级联删除文章标签关联）
     * @param id 标签 ID
     * @return 是否删除成功
     */
    boolean deleteTagWithRelations(Long id);
}
