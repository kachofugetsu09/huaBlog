package site.hnfy258.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import site.hnfy258.VO.LinkVo;
import site.hnfy258.VO.PageVo;
import site.hnfy258.entity.Link;
import site.hnfy258.mapper.LinkMapper;
import site.hnfy258.service.LinkService;
import site.hnfy258.utils.BeanCopyUtils;

import java.util.Collections;
import java.util.List;

/**
 * 友链(Link)表服务实现类
 *
 * @author makejava
 * @since 2025-02-07 17:39:51
 */
@Service
public class LinkServiceImpl extends ServiceImpl<LinkMapper, Link> implements LinkService {
    @Autowired
    private LinkMapper linkMapper;
    /**
     * @return
     */
    @Override
    public List<LinkVo> getAllLink() {
        List<Link> linkList = linkMapper.getAllLink();
        List<LinkVo> linkVos = BeanCopyUtils.copyBeanList(linkList, LinkVo.class);
        return linkVos;
    }

    /**
     * @param link
     * @param pageNum
     * @param pageSize
     * @return
     */
    @Override
    public PageVo pageLinkList(Link link, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<Link> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(link.getName()!=null,Link::getName,link.getName());
        queryWrapper.eq(link.getStatus()!=null,Link::getStatus,link.getStatus());

        Page<Link> page = new Page<>();
        page.setCurrent(pageNum);
        page.setSize(pageSize);
        page(page,queryWrapper);
        long total = page.getTotal();
        List<Link> records = page.getRecords();

        PageVo pageVo = new PageVo();
        pageVo.setRows(records);
        pageVo.setTotal(total);
        return pageVo;
    }
}


