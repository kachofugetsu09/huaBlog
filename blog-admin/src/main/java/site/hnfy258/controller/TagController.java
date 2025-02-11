package site.hnfy258.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import site.hnfy258.DTO.AddTagDto;
import site.hnfy258.DTO.EditTagDto;
import site.hnfy258.DTO.TagListDTO;
import site.hnfy258.VO.PageVo;
import site.hnfy258.VO.TagVo;
import site.hnfy258.domain.ResponseResult;
import site.hnfy258.entity.Tag;
import site.hnfy258.service.TagService;
import site.hnfy258.utils.BeanCopyUtils;

import java.util.List;

@RestController
@RequestMapping("/content/tag")
public class TagController {
    @Autowired
    private TagService tagService;
    @GetMapping("/list")
    public ResponseResult<PageVo> list(Integer pageNum,Integer pageSize, TagListDTO tagListDTO){
        PageVo pageVo = tagService.pageTagList(pageNum,pageSize,tagListDTO);
        return ResponseResult.okResult(pageVo);
    }
    @PostMapping
    public ResponseResult add(@RequestBody AddTagDto tagDto){
        Tag tag = BeanCopyUtils.copyBean(tagDto, Tag.class);
        tagService.save(tag);
        return ResponseResult.okResult();
    }

    @DeleteMapping("/{id}")
    public ResponseResult delete(@PathVariable Long id){
        tagService.removeById(id);
        return ResponseResult.okResult();
    }

    @PutMapping
    public ResponseResult edit(@RequestBody EditTagDto tagDto){
        Tag tag = BeanCopyUtils.copyBean(tagDto,Tag.class);
        tagService.updateById(tag);
        return ResponseResult.okResult();
    }
    @GetMapping("/listAllTag")
    public ResponseResult listAllTag(){
        List<TagVo> list = tagService.listAllTag();
        return ResponseResult.okResult(list);
    }
}
