package site.hnfy258.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import site.hnfy258.entity.Tag;
import site.hnfy258.mapper.TagMapper;
import org.springframework.stereotype.Service;
import site.hnfy258.service.TagService;

/**
 * 标签(Tag)表服务实现类
 *
 * @author makejava
 * @since 2025-02-10 16:14:02
 */
@Service("tagService")
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements TagService {

}


