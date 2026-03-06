package com.qblog.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qblog.entity.Tag;
import com.qblog.mapper.TagMapper;
import com.qblog.model.vo.TagVO;
import com.qblog.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 标签服务实现
 */
@Service
@RequiredArgsConstructor
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements TagService {

    private final TagMapper tagMapper;

    @Override
    public List<TagVO> listWithArticleCount() {
        return tagMapper.selectTagListWithArticleCount();
    }
}
