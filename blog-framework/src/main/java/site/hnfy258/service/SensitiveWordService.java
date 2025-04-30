package site.hnfy258.service;

import java.util.List;
import java.util.Set;

/**
 * 敏感词服务接口
 */
public interface SensitiveWordService {
    
    /**
     * 检查文本是否包含敏感词
     * @param text 待检查文本
     * @return 包含敏感词返回true，否则返回false
     */
    boolean containsSensitiveWords(String text);
    
    /**
     * 检查多个文本是否包含敏感词
     * @param texts 待检查的多个文本
     * @return 任一文本包含敏感词返回true，否则返回false
     */
    boolean containsAnySensitiveWords(String... texts);
    
    /**
     * 查找文本中的所有敏感词
     * @param text 待检查文本
     * @return 发现的敏感词列表
     */
    List<String> findAllSensitiveWords(String text);
    
    /**
     * 替换文本中的敏感词为指定字符
     * @param text 原始文本
     * @param replacement 替换字符
     * @return 替换后的文本
     */
    String replaceSensitiveWords(String text, char replacement);
    
    /**
     * 重新加载敏感词库
     */
    void reloadSensitiveWords();
}
