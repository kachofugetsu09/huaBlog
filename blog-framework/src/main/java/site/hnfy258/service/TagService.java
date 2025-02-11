package site.hnfy258.service;

import com.baomidou.mybatisplus.extension.service.IService;
import site.hnfy258.DTO.TagListDTO;
import site.hnfy258.VO.PageVo;
import site.hnfy258.VO.TagVo;
import site.hnfy258.entity.Tag;

import java.util.List;


/**
 * 标签(Tag)表服务接口
 *
 * @author makejava
 * @since 2025-02-10 16:14:02
 */
public interface TagService extends IService<Tag> {

    PageVo pageTagList(Integer pageNum, Integer pageSize, TagListDTO tagListDTO);

    List<TagVo> listAllTag();
}


