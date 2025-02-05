package site.hnfy258.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import site.hnfy258.domain.VO.HotArticleVo;
import site.hnfy258.domain.entity.Article;
import site.hnfy258.mapper.ArticleMapper;
import site.hnfy258.service.ArticleService;

import java.util.ArrayList;
import java.util.List;

@Service

public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService {
    /**
     * @return
     */
    @Autowired
    private ArticleMapper articleMapper;
    @Override
    public List<HotArticleVo> hotArticleList() {
        List<HotArticleVo> hotArticleVoList = new ArrayList<>();
        List<Article> articleList = articleMapper.selectHotArticleList();
        for(Article a:articleList){
            HotArticleVo hotArticleVo = new HotArticleVo();
            BeanUtils.copyProperties(a,hotArticleVo);
            hotArticleVoList.add(hotArticleVo);
        }
        return hotArticleVoList;
    }
}
