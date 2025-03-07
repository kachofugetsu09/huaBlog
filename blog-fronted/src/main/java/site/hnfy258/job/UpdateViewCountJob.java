package site.hnfy258.job;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import site.hnfy258.entity.Article;
import site.hnfy258.service.ArticleService;
import site.hnfy258.utils.RedisCache;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class UpdateViewCountJob {

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private ArticleService articleService;

    @Scheduled(cron = "0/5 * * * * ?")
    public void updateViewCount(){
        //获取redis中的浏览量
        Map<String, Integer> viewCountMap = redisCache.getCacheMap("article:viewCount");

        if (viewCountMap.isEmpty()) {
            return;  // Nothing to update
        }

        List<Article> articles = viewCountMap.entrySet()
                .stream()
                .map(entry -> new Article(Long.valueOf(entry.getKey()), entry.getValue().longValue()))
                .collect(Collectors.toList());

        //更新到数据库中
        articleService.updateBatchById(articles);

        // 不清空缓存，保留现有的计数
        // redisCache.deleteObject("article:viewCount");
    }
}