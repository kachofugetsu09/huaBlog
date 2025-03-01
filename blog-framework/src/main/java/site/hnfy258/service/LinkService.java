package site.hnfy258.service;

import com.baomidou.mybatisplus.extension.service.IService;
import site.hnfy258.VO.LinkVo;
import site.hnfy258.VO.PageVo;
import site.hnfy258.entity.Link;

import java.util.List;


/**
 * 友链(Link)表服务接口
 *
 * @author makejava
 * @since 2025-02-07 17:39:51
 */
public interface LinkService extends IService<Link> {


    List<LinkVo> getAllLink();

    PageVo pageLinkList(Link link, Integer pageNum, Integer pageSize);
}


