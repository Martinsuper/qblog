package com.qblog.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qblog.entity.Tag;
import com.qblog.mapper.TagMapper;
import com.qblog.service.TagService;
import org.springframework.stereotype.Service;

/**
 * 标签服务实现
 */
@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements TagService {
}
