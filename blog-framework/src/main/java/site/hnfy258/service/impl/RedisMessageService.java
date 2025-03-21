package site.hnfy258.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import site.hnfy258.entity.Messages;
import site.hnfy258.websocket.WebSocketServer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class RedisMessageService {

    private static final String CHAT_STREAM = "chat-messages-stream";
    private static final String CONSUMER_GROUP = "chat-consumer-group";
    private static final String CONSUMER_NAME = "chat-consumer";

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 创建单线程执行器
    private static final ExecutorService MESSAGE_HANDLER_EXECUTOR =
            Executors.newSingleThreadExecutor();

    @PostConstruct
    public void init() {
        try {
            createStreamAndGroupIfNotExist();
// 启动消息处理线程
            MESSAGE_HANDLER_EXECUTOR.submit(new MessageHandler());
            log.info("Redis Streams消息监听器已初始化，监听流: {}", CHAT_STREAM);
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
     * 消息处理器线程
     */
    private class MessageHandler implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
// 从Stream中读取消息，阻塞等待2秒
                    List<MapRecord<String, Object, Object>> list = redisTemplate.opsForStream().read(
                            Consumer.from(CONSUMER_GROUP, CONSUMER_NAME),
                            StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                            StreamOffset.create(CHAT_STREAM, ReadOffset.lastConsumed())
                    );

// 如果没有消息，继续下一轮循环
                    if (list == null || list.isEmpty()) {
                        continue;
                    }

// 处理消息
                    MapRecord<String, Object, Object> record = list.get(0);
                    Map<Object, Object> value = record.getValue();

// 获取消息JSON
                    String messageJson = (String) value.get("message");
                    if (messageJson != null) {
// 解析消息对象
                        Messages chatMessage = objectMapper.readValue(messageJson, Messages.class);
                        log.info("接收到消息: {}", chatMessage);

// 通过WebSocket发送给接收者
                        WebSocketServer.sendMessage(
                                String.valueOf(chatMessage.getReceiverId()),
                                messageJson
                        );
                    }

// 确认消息已处理
                    redisTemplate.opsForStream().acknowledge(
                            CHAT_STREAM,
                            CONSUMER_GROUP,
                            record.getId()
                    );
                } catch (Exception e) {
                    log.error("处理Redis Stream消息失败", e);
// 处理失败时，尝试处理pending列表中的消息
                    handlePendingMessages();
                }
            }
        }
    }

    /**
     * 处理待处理的消息
     */
    private void handlePendingMessages() {
        try {
            while (true) {
// 从pending列表中读取消息
                List<MapRecord<String, Object, Object>> list = redisTemplate.opsForStream().read(
                        Consumer.from(CONSUMER_GROUP, CONSUMER_NAME),
                        StreamReadOptions.empty().count(1),
                        StreamOffset.create(CHAT_STREAM, ReadOffset.from("0"))
                );

// 如果没有pending消息，退出循环
                if (list == null || list.isEmpty()) {
                    break;
                }

// 处理pending消息
                MapRecord<String, Object, Object> record = list.get(0);
                Map<Object, Object> value = record.getValue();

// 获取消息JSON
                String messageJson = (String) value.get("message");
                if (messageJson != null) {
// 解析消息对象
                    Messages chatMessage = objectMapper.readValue(messageJson, Messages.class);
                    log.info("处理pending消息: {}", chatMessage);

// 通过WebSocket发送给接收者
                    WebSocketServer.sendMessage(
                            String.valueOf(chatMessage.getReceiverId()),
                            messageJson
                    );
                }

// 确认消息已处理
                redisTemplate.opsForStream().acknowledge(
                        CHAT_STREAM,
                        CONSUMER_GROUP,
                        record.getId()
                );
            }
        } catch (Exception e) {
            log.error("处理pending消息失败", e);
        }
    }

    /**
     * 定期检查并处理pending消息
     */
    @Scheduled(fixedRate = 60000) // 每分钟执行一次
    public void processPendingMessages() {
        try {
// 获取pending消息摘要
            PendingMessagesSummary summary = redisTemplate.opsForStream()
                    .pending(CHAT_STREAM, CONSUMER_GROUP);

            if (summary != null && summary.getTotalPendingMessages() > 0) {
                log.info("发现 {} 条待处理消息", summary.getTotalPendingMessages());
                handlePendingMessages();
            }
        } catch (Exception e) {
            log.error("处理pending消息摘要失败", e);
        }
    }

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

            log.info("消息已发送到Redis Stream: {}, recordId: {}", message, recordId);
        } catch (Exception e) {
            log.error("发送消息到Redis Stream失败", e);
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