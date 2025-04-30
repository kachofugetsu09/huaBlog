package site.hnfy258.filter;

import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ContentFilter {

    private final Map<String, Integer> contentHashCache = new ConcurrentHashMap<>();
    
    /**
     * 检查内容是否为重复攻击
     * @param content 内容
     * @param userId 用户ID
     * @return true表示是重复攻击
     */
    public boolean isRepeatedAttack(String content, String userId) {
        if (content == null || content.length() < 10) {
            return false;
        }
        
        // 计算内容哈希
        String contentHash = DigestUtils.md5DigestAsHex(content.getBytes());
        String key = userId + ":" + contentHash;
        
        // 检查是否在短时间内重复提交相同内容
        Integer count = contentHashCache.get(key);
        if (count == null) {
            contentHashCache.put(key, 1);
            return false;
        } else {
            contentHashCache.put(key, count + 1);
            return count >= 3;
        }
    }
}
