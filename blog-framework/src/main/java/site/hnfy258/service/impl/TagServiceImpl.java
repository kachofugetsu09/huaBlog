package site.hnfy258.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import site.hnfy258.DTO.TagListDTO;
import site.hnfy258.VO.PageVo;
import site.hnfy258.VO.TagVo;
import site.hnfy258.entity.Tag;
import site.hnfy258.mapper.TagMapper;
import org.springframework.stereotype.Service;
import site.hnfy258.service.TagService;
import site.hnfy258.utils.BeanCopyUtils;

import java.util.Collections;
import java.util.List;

/**
 * 标签(Tag)表服务实现类
 *
 * @author makejava
 * @since 2025-02-10 16:14:02
 */
@Service("tagService")
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements TagService {

    /**
     * @param pageNum
     * @param pageSize
     * @param tagListDTO
     * @return
     */
        @Autowired
        private TagMapper tagMapper;
        @Override
        public PageVo pageTagList(Integer pageNum, Integer pageSize, TagListDTO tagListDTO) {
            PageHelper.startPage(pageNum,pageSize);
            String name = tagListDTO.getName();
            String remark = tagListDTO.getRemark();
            List<Tag> list =tagMapper.selectTagList(name,remark);
            PageInfo<Tag> page =new PageInfo<>(list);
            PageVo pageVO = new PageVo();
            pageVO.setRows(page.getList());
            pageVO.setTotal(page.getTotal());
            return pageVO;
        }

    /**
     * @return
     */
    @Override
    public List<TagVo> listAllTag() {
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(Tag::getId,Tag::getName);
        List<Tag> list = list(wrapper);
        List<TagVo> tagVos = BeanCopyUtils.copyBeanList(list, TagVo.class);
        return tagVos;
    }
}


