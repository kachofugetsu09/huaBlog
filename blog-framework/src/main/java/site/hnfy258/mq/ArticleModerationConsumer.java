package site.hnfy258.mq;

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import site.hnfy258.config.RabbitMQConfig;
import site.hnfy258.entity.Article;
import site.hnfy258.service.ArticleModerationService;

@Component
public class ArticleModerationConsumer {

    private static final Logger logger = LoggerFactory.getLogger(ArticleModerationConsumer.class);

    @Autowired
    private ArticleModerationService moderationService;

    @RabbitListener(queues = RabbitMQConfig.ARTICLE_MODERATION_QUEUE)
    public void onMessage(String message) {
        try {
            logger.info("收到文章审核消息: {}", message);
            Article article = JSON.parseObject(message, Article.class);
            
            // 检查文章内容
            boolean isContentSafe = moderationService.checkArticleContent(article);
            
            if (isContentSafe) {
                // 如果内容安全，则将文章状态更新为已发布
                logger.info("文章 {} 审核通过", article.getId());
                moderationService.approveArticle(article.getId());
            } else {
                logger.warn("文章 {} 包含敏感内容，审核不通过", article.getId());
                moderationService.rejectArticle(article.getId());
                logger.info("审核不通过，状态成功修改，文章ID: {}", article.getId());
            }
        } catch (Exception e) {
            logger.error("处理文章审核消息时发生错误", e);
        }
    }
}
