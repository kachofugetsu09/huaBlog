package site.hnfy258.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import site.hnfy258.entity.Article;
import site.hnfy258.mapper.ArticleMapper;
import site.hnfy258.service.ArticleModerationService;

import java.util.Arrays;
import java.util.List;

@Service
public class ArticleModerationServiceImpl implements ArticleModerationService {

    // 示例敏感词列表，实际应用中可以从数据库或配置文件加载
    private final List<String> sensitiveWords = Arrays.asList(
            "暴力", "色情", "赌博", "毒品", "恐怖", "政治敏感", "违法"
    );
    
    @Autowired
    private ArticleMapper articleMapper;

    @Override
    public boolean checkArticleContent(Article article) {
        String content = article.getContent();
        String title = article.getTitle();
        String summary = article.getSummary();
        
        // 检查文章内容、标题和摘要是否包含敏感词
        for (String word : sensitiveWords) {
            if ((content != null && content.contains(word)) || 
                (title != null && title.contains(word)) || 
                (summary != null && summary.contains(word))) {
                return false;
            }
        }
        
        return true;
    }

    @Override
    public void approveArticle(Long articleId) {
        Article article = new Article();
        article.setId(articleId);
        // 将状态设置为已发布(0)
        article.setStatus("0");
        articleMapper.updateById(article);
    }
}
