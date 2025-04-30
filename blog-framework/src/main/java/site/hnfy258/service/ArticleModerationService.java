package site.hnfy258.service;

import site.hnfy258.entity.Article;

public interface ArticleModerationService {
    
    /**
     * 检查文章内容是否包含敏感词
     * @param article 文章对象
     * @return 如果不包含敏感词，返回true；否则返回false
     */
    boolean checkArticleContent(Article article);
    
    /**
     * 审核通过后更新文章状态
     * @param articleId 文章ID
     */
    void approveArticle(Long articleId);
}
