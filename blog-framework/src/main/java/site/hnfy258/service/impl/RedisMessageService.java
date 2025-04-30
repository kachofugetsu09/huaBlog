package site.hnfy258.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.omg.SendingContext.RunTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import site.hnfy258.entity.Messages;
import site.hnfy258.mapper.ChatMapper;
import site.hnfy258.websocket.WebSocketServer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class RedisMessageService {

    private static final String CHAT_STREAM = "chat-messages-stream";
    private static final String CONSUMER_GROUP = "chat-consumer-group";
    private static final String CONSUMER_NAME = "chat-consumer-" + UUID.randomUUID().toString();

    private static final int CONSUMER_THREADS = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private ChatMapper chatMapper;

    @Autowired
    WebSocketServer webSocketServer;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 使用有界线程池避免资源耗尽
    private static final ExecutorService MESSAGE_HANDLER_EXECUTOR =
            Executors.newFixedThreadPool(CONSUMER_THREADS);

    @PostConstruct
    public void init() {
        try {
            createStreamAndGroupIfNotExist();
            // 启动适量消息处理线程
            for (int i = 0; i < CONSUMER_THREADS; i++) {
                MESSAGE_HANDLER_EXECUTOR.submit(new MessageHandler());
            }
            log.info("Redis Streams消息监听器已初始化，启动{}个处理线程，监听流: {}",
                    CONSUMER_THREADS, CHAT_STREAM);
        } catch (Exception e) {
            log.error("初始化Redis Streams失败", e);
        }
    }

    private void createStreamAndGroupIfNotExist() {
        try {
            redisTemplate.opsForStream().info(CHAT_STREAM);
        } catch (Exception e) {
// 流不存在，创建一个初始消息
            Map<String, String> dummyMessage = new HashMap<>();
            dummyMessage.put("init", "true");
            redisTemplate.opsForStream().add(CHAT_STREAM, dummyMessage);
            log.info("创建流 {}", CHAT_STREAM);
        }

        try {
            redisTemplate.opsForStream().createGroup(CHAT_STREAM, CONSUMER_GROUP);
            log.info("创建消费组 {}", CONSUMER_GROUP);
        } catch (Exception e) {
            log.info("消费组 {} 已存在", CONSUMER_GROUP);
        }
    }

    /**
     * 消息发送 - 仅负责投递消息到Stream
     */
    public void sendMessage(Messages message) {
        try {
            // 将Messages对象转换为JSON字符串
            String messageJson = objectMapper.writeValueAsString(message);

            // 创建消息记录
            Map<String, String> messageMap = new HashMap<>();
            messageMap.put("message", messageJson);
            messageMap.put("senderId", String.valueOf(message.getSenderId()));
            messageMap.put("receiverId", String.valueOf(message.getReceiverId()));
            messageMap.put("sessionId", String.valueOf(message.getSessionId()));
            messageMap.put("timestamp", String.valueOf(System.currentTimeMillis()));

            // 添加消息到Redis Stream
            RecordId recordId = redisTemplate.opsForStream().add(CHAT_STREAM, messageMap);
            log.info("消息已发送到Redis Stream: {}, recordId: {}", message.getId(), recordId);

        } catch (Exception e) {
            log.error("发送消息到Redis Stream失败", e);
            throw new RuntimeException("发送消息失败", e);
        }
    }

    /**
     * 消息处理器线程 - 单一职责：从Stream读取并处理消息
     */
    private class MessageHandler implements Runnable {
        @Override
        public void run() {
            String threadName = Thread.currentThread().getName();
            log.info("消息处理线程{}已启动", threadName);

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // 从Stream中读取消息，阻塞等待2秒
                    List<MapRecord<String, Object, Object>> list = redisTemplate.opsForStream().read(
                            Consumer.from(CONSUMER_GROUP, CONSUMER_NAME),
                            StreamReadOptions.empty().count(5).block(Duration.ofSeconds(2)),
                            StreamOffset.create(CHAT_STREAM, ReadOffset.lastConsumed())
                    );

                    // 如果没有消息，继续下一轮循环
                    if (list == null || list.isEmpty()) {
                        continue;
                    }

                    // 批量处理消息
                    for (MapRecord<String, Object, Object> record : list) {
                        try {
                            processMessage(record);
                            // 确认消息已处理
                            redisTemplate.opsForStream().acknowledge(CHAT_STREAM, CONSUMER_GROUP, record.getId());
                        } catch (Exception e) {
                            log.error("处理消息失败: {}", e.getMessage());
                            // 单条消息处理失败不影响其他消息
                        }
                    }
                } catch (Exception e) {
                    log.error("读取Redis Stream消息失败: {}", e.getMessage());
                    // 短暂暂停，避免在异常情况下CPU占用过高
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }

                    // 处理pending消息
                    handlePendingMessages();
                }
            }
            log.info("消息处理线程{}已停止", threadName);
        }
    }

    /**
     * 处理单条消息 - 包含WebSocket推送和数据库持久化
     */
    private void processMessage(MapRecord<String, Object, Object> record) throws Exception {
        Map<Object, Object> value = record.getValue();
        String messageJson = (String) value.get("message");

        if (messageJson == null) {
            log.warn("收到无效消息，缺少message字段");
            return;
        }

        Messages message = objectMapper.readValue(messageJson, Messages.class);
        Long receiverId = message.getReceiverId();
        log.debug("开始处理消息: {}", message.getId());

        // 1. 先执行数据库持久化
        CompletableFuture<Boolean> dbTask = CompletableFuture.supplyAsync(() -> {
            try {
                if (message.getId() == null) {
                    chatMapper.insert(message);
                    log.info("消息已保存到数据库: {}", message.getId());
                }
                return true;
            } catch (Exception e) {
                log.error("保存消息到数据库失败: {}", e.getMessage());
                return false;
            }
        });

        // 2. 等待数据库操作完成后，再执行WebSocket推送
        dbTask.thenCompose(dbSuccess -> {
            if (!dbSuccess) {
                return CompletableFuture.completedFuture(false);
            }

            return CompletableFuture.supplyAsync(() -> {
                try {
                    if (webSocketServer.isUserOnline(String.valueOf(receiverId))) {
                        WebSocketServer.sendMessage(
                                String.valueOf(message.getReceiverId()),
                                messageJson
                        );
                        log.info("消息已推送给用户: {}", receiverId);
                        return true;
                    } else {
                        log.info("用户{}当前不在线，消息将保存等待其上线时推送", receiverId);
                        return false;
                    }
                } catch (Exception e) {
                    log.error("WebSocket推送失败: {}", e.getMessage());
                    return false;
                }
            });
        }).thenCompose(wsSuccess -> {
            // 3. 最后清理缓存
            return CompletableFuture.supplyAsync(() -> {
                try {
                    String messagesCacheKey = "chat:messages:session:" + message.getSessionId();
                    String unreadCountKey = "chat:unread:" + message.getSessionId() + ":" + message.getReceiverId();

                    // 记录要清理的缓存键
                    log.info("准备清理缓存: {} 和 {}", messagesCacheKey, unreadCountKey);

                    redisTemplate.delete(messagesCacheKey);
                    redisTemplate.delete(unreadCountKey);

                    log.info("缓存清理完成: {}", messagesCacheKey);
                    return true;
                } catch (Exception e) {
                    log.error("清理缓存失败: {}", e.getMessage());
                    return false;
                }
            });
        }).exceptionally(throwable -> {
            log.error("消息处理过程中发生错误: {}", throwable.getMessage());
            return false;
        }).join(); // 等待所有操作完成
    }


    @Scheduled(fixedRate = 30000) // 每30秒检查一次
    public void scheduledPendingMessagesCheck() {
        handlePendingMessages();
    }


    private void handlePendingMessages() {
        try {
            // 获取pending消息摘要
            PendingMessagesSummary summary = redisTemplate.opsForStream()
                    .pending(CHAT_STREAM, CONSUMER_GROUP);

            if (summary == null || summary.getTotalPendingMessages() == 0) {
                return;
            }

            log.info("发现 {} 条待处理消息", summary.getTotalPendingMessages());

            // 一次处理最多30条
            int batchSize = 30;
            PendingMessages pendingMessages = redisTemplate.opsForStream()
                    .pending(CHAT_STREAM, CONSUMER_GROUP, Range.unbounded(), batchSize);

            if (pendingMessages == null || pendingMessages.isEmpty()) {
                return;
            }

            for (PendingMessage pendingMessage : pendingMessages) {
                RecordId messageId = pendingMessage.getId();
                Duration idleTime = pendingMessage.getElapsedTimeSinceLastDelivery();

                // 只处理闲置超过5秒的消息，避免抢占刚分配的消息
                if (idleTime.compareTo(Duration.ofSeconds(5)) <= 0) {
                    continue;
                }

                try {
                    // 直接读取消息内容
                    List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream()
                            .range(CHAT_STREAM, Range.closed(messageId.getValue(), messageId.getValue()));

                    if (records != null && !records.isEmpty()) {
                        // 处理消息
                        processMessage(records.get(0));

                        // 确认消息已处理
                        redisTemplate.opsForStream().acknowledge(
                                CHAT_STREAM, CONSUMER_GROUP, messageId);
                    }
                } catch (Exception e) {
                    log.error("处理pending消息{}失败: {}", messageId, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("处理pending消息失败: {}", e.getMessage());
        }
    }

    /**
     * 清理资源
     */
    @PreDestroy
    public void cleanup() {
        try {
// 停止消息处理线程池
            if (MESSAGE_HANDLER_EXECUTOR != null && !MESSAGE_HANDLER_EXECUTOR.isShutdown()) {
                MESSAGE_HANDLER_EXECUTOR.shutdown();
                log.info("消息处理线程池已关闭");
            }
        } catch (Exception e) {
            log.error("关闭消息处理线程池失败", e);
        }
    }


}