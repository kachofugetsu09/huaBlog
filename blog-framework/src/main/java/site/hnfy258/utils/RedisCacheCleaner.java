package site.hnfy258.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis缓存清理工具
 * 用于解决序列化/反序列化冲突问题
 */
@Slf4j
@Component
@Order(1) // 确保在应用启动早期运行
public class RedisCacheCleaner implements CommandLineRunner {

    @Autowired
    @Qualifier("compatibilityRedisTemplate")
    private RedisTemplate<String, Object> redisTemplate;

    private final Set<String> prefixesToClean = new HashSet<>(Arrays.asList(
            "login:",     // 用户登录相关
            "bloglogin:", // 博客登录相关
            "user:"       // 用户信息相关
    ));

    @Override
    public void run(String... args) {
        log.info("启动时清理Redis缓存...");
        cleanRedisCache();
    }

    /**
     * 使用SCAN命令清理Redis缓存，避免使用KEYS阻塞Redis
     */
    public void cleanRedisCache() {
        try {
            RedisConnection connection = redisTemplate.getConnectionFactory().getConnection();
            long startTime = System.currentTimeMillis();
            int cleanedCount = 0;
            
            // 最多尝试3次清理
            for (int attempt = 1; attempt <= 3; attempt++) {
                try {
                    log.info("开始第{}次尝试清理Redis缓存", attempt);
                    
                    cleanedCount = scanAndCleanKeys(connection);
                    
                    if (cleanedCount > 0) {
                        log.info("Redis缓存清理完成，共清理{}个键，耗时{}ms", 
                                cleanedCount, System.currentTimeMillis() - startTime);
                        break;
                    }
                    
                    // 如果清理失败但未抛出异常，等待1秒后重试
                    if (attempt < 3) {
                        TimeUnit.SECONDS.sleep(1);
                    }
                } catch (Exception e) {
                    log.warn("第{}次清理Redis缓存异常: {}", attempt, e.getMessage());
                    if (attempt < 3) {
                        TimeUnit.SECONDS.sleep(1);
                    } else {
                        throw e;
                    }
                }
            }
            
            // 如果没有清理任何键，可能是连接问题或没有需要清理的键
            if (cleanedCount == 0) {
                log.info("未清理任何Redis键，可能是没有匹配的键或连接问题");
            }
        } catch (Exception e) {
            log.error("Redis缓存清理失败", e);
        }
    }

    private int scanAndCleanKeys(RedisConnection connection) throws IOException {
        int cleanedCount = 0;
        
        // 对每个前缀进行SCAN操作
        for (String prefix : prefixesToClean) {
            log.info("开始扫描并清理前缀为'{}'的键", prefix);
            
            ScanOptions options = ScanOptions.scanOptions()
                    .match(prefix + "*")
                    .count(100)
                    .build();
            
            Cursor<byte[]> cursor = connection.scan(options);
            while (cursor.hasNext()) {
                byte[] key = cursor.next();
                String keyStr = new String(key);
                connection.del(key);
                cleanedCount++;
                
                // 每清理10个键记录一次日志，避免日志过多
                if (cleanedCount % 10 == 0) {
                    log.info("已清理{}个键，最近一个: {}", cleanedCount, keyStr);
                }
            }
            
            cursor.close();
        }
        
        return cleanedCount;
    }
} 