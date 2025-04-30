package site.hnfy258.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import site.hnfy258.entity.Article;
import site.hnfy258.mapper.ArticleMapper;
import site.hnfy258.service.ArticleModerationService;
import site.hnfy258.service.SensitiveWordService;


@Service
public class ArticleModerationServiceImpl implements ArticleModerationService {

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private SensitiveWordService sensitiveWordService;

    @Override
    public boolean checkArticleContent(Article article) {
        // 检查文章内容、标题和摘要是否包含敏感词
        return !sensitiveWordService.containsAnySensitiveWords(
                article.getContent(),
                article.getTitle(),
                article.getSummary()
        );
    }

    @Override
    public void approveArticle(Long articleId) {
        Article article = new Article();
        article.setId(articleId);
        // 将状态设置为已发布(0)
        article.setStatus("0");
        articleMapper.updateById(article);
    }

    /**
     * @param articleId
     */
    @Override
    public void rejectArticle(Long articleId) {
        Article article = new Article();
        article.setId(articleId);
        article.setStatus("2");
        articleMapper.updateById(article);
    }
}