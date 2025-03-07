package site.hnfy258.runner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import site.hnfy258.entity.Article;
import site.hnfy258.mapper.ArticleMapper;
import site.hnfy258.utils.RedisCache;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ViewCountRunner implements CommandLineRunner {

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private RedisCache redisCache;

    @Override
    public void run(String... args) throws Exception {
        syncViewCountFromDBToRedis();
    }

    // You could also call this method periodically if needed
    public void syncViewCountFromDBToRedis() {
        //查询博客信息  id  viewCount
        List<Article> articles = articleMapper.selectList(null);
        Map<String, Integer> viewCountMap = articles.stream()
                .collect(Collectors.toMap(article -> article.getId().toString(), article -> {
                    return article.getViewCount().intValue();
                }));

        //获取当前Redis中的数据
        Map<String, Integer> currentViewCountMap = redisCache.getCacheMap("article:viewCount");

        //只更新Redis中不存在的文章
        for (Map.Entry<String, Integer> entry : viewCountMap.entrySet()) {
            if (!currentViewCountMap.containsKey(entry.getKey())) {
                currentViewCountMap.put(entry.getKey(), entry.getValue());
            }
        }

        //存储到redis中
        redisCache.setCacheMap("article:viewCount", currentViewCountMap);
    }
}