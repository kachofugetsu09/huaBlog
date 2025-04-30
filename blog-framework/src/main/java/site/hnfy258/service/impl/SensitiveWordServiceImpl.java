package site.hnfy258.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import site.hnfy258.service.SensitiveWordService;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 敏感词服务实现
 */
@Slf4j
@Service
public class SensitiveWordServiceImpl implements SensitiveWordService {

    @Value("${sensitive.words.file:sensitive_words.txt}")
    private String sensitiveWordsFile;


    private volatile TrieNode rootNode;
    

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    @PostConstruct
    public void init() {
        reloadSensitiveWords();
    }

    @Override
    public void reloadSensitiveWords() {
        try {

            Set<String> words = loadSensitiveWords();

            lock.writeLock().lock();
            try {
                initSensitiveWordTrie(words);
                log.info("成功加载{}个敏感词", words.size());
            } finally {
                lock.writeLock().unlock();
            }
        } catch (IOException e) {
            // 加载失败时记录日志
            log.error("加载敏感词库失败: {}", e.getMessage(), e);
        }
    }

    // 从文件加载敏感词
    private Set<String> loadSensitiveWords() throws IOException {
        Set<String> words = new HashSet<>();
        ClassPathResource resource = new ClassPathResource(sensitiveWordsFile);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#") || !StringUtils.hasText(line)) {
                    continue;
                }

                String[] lineWords = line.split("、");
                for (String word : lineWords) {
                    if (StringUtils.hasText(word)) {
                        words.add(word.trim());
                    }
                }
            }
        }

        return words;
    }

    private void initSensitiveWordTrie(Set<String> words) {
        rootNode = new TrieNode();

        // 构建前缀树
        for (String word : words) {
            if (StringUtils.hasText(word)) {
                addWordToTrie(word);
            }
        }
    }

    private void addWordToTrie(String word) {
        TrieNode currentNode = rootNode;

        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            TrieNode childNode = currentNode.getChildren().get(c);

            if (childNode == null) {
                childNode = new TrieNode();
                currentNode.getChildren().put(c, childNode);
            }

            currentNode = childNode;

            // 如果是单词的最后一个字符，标记为敏感词结尾
            if (i == word.length() - 1) {
                currentNode.setEndOfWord(true);
            }
        }
    }

    @Override
    public boolean containsAnySensitiveWords(String... texts) {
        if (texts == null || texts.length == 0) {
            return false;
        }
        
        for (String text : texts) {
            if (containsSensitiveWords(text)) {
                return true;
            }
        }
        
        return false;
    }

    @Override
    public boolean containsSensitiveWords(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }

        char[] chars = text.toCharArray();
        
        // 获取读锁
        lock.readLock().lock();
        try {
            // 从每个字符开始检查
            for (int i = 0; i < chars.length; i++) {
                if (checkSensitiveWord(chars, i)) {
                    return true;
                }
            }
        } finally {
            lock.readLock().unlock();
        }

        return false;
    }

    private boolean checkSensitiveWord(char[] chars, int beginIndex) {
        TrieNode currentNode = rootNode;

        for (int i = beginIndex; i < chars.length; i++) {
            char c = chars[i];

            TrieNode childNode = currentNode.getChildren().get(c);

            if (childNode == null) {
                break;
            }

            currentNode = childNode;

            if (currentNode.isEndOfWord()) {
                return true;
            }
        }

        return false;
    }

    // 查找所有敏感词
    @Override
    public List<String> findAllSensitiveWords(String text) {
        List<String> result = new ArrayList<>();

        if (!StringUtils.hasText(text)) {
            return result;
        }

        char[] chars = text.toCharArray();
        
        // 获取读锁
        lock.readLock().lock();
        try {
            for (int i = 0; i < chars.length; i++) {
                String word = checkAndGetSensitiveWord(chars, i);
                if (word != null && !result.contains(word)) {
                    result.add(word);
                }
            }
        } finally {
            lock.readLock().unlock();
        }

        return result;
    }

    private String checkAndGetSensitiveWord(char[] chars, int beginIndex) {
        StringBuilder word = new StringBuilder();
        TrieNode currentNode = rootNode;
        int resultLength = 0;

        for (int i = beginIndex; i < chars.length; i++) {
            char c = chars[i];
            word.append(c);

            TrieNode childNode = currentNode.getChildren().get(c);

            if (childNode == null) {
                break;
            }

            currentNode = childNode;

            if (currentNode.isEndOfWord()) {
                resultLength = i - beginIndex + 1;
            }
        }

        if (resultLength > 0) {
            return word.substring(0, resultLength);
        }

        return null;
    }

    @Override
    public String replaceSensitiveWords(String text, char replacement) {
        if (!StringUtils.hasText(text)) {
            return text;
        }

        char[] chars = text.toCharArray();
        StringBuilder result = new StringBuilder(text);
        
        // 获取读锁
        lock.readLock().lock();
        try {
            for (int i = 0; i < chars.length; i++) {
                int length = checkSensitiveWordLength(chars, i);

                if (length > 0) {
                    // 替换为指定字符
                    for (int j = 0; j < length; j++) {
                        result.setCharAt(i + j, replacement);
                    }
                    
                    // 跳过已替换的敏感词
                    i += length - 1;
                }
            }
        } finally {
            lock.readLock().unlock();
        }

        return result.toString();
    }

    private int checkSensitiveWordLength(char[] chars, int beginIndex) {
        TrieNode currentNode = rootNode;
        int resultLength = 0;

        for (int i = beginIndex; i < chars.length; i++) {
            char c = chars[i];

            TrieNode childNode = currentNode.getChildren().get(c);

            if (childNode == null) {
                break;
            }

            currentNode = childNode;

            if (currentNode.isEndOfWord()) {
                resultLength = i - beginIndex + 1;
            }
        }

        return resultLength;
    }

    // 前缀树节点类
    private static class TrieNode {
        private final Map<Character, TrieNode> children;
        private boolean endOfWord;

        public TrieNode() {
            children = new HashMap<>();
            endOfWord = false;
        }

        public Map<Character, TrieNode> getChildren() {
            return children;
        }

        public boolean isEndOfWord() {
            return endOfWord;
        }
        
        public void setEndOfWord(boolean endOfWord) {
            this.endOfWord = endOfWord;
        }
    }
}
