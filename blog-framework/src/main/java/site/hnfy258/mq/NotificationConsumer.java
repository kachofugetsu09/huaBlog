package site.hnfy258.mq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import site.hnfy258.DTO.NotificationMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
@RocketMQMessageListener(topic = "comment-topic",consumerGroup = "comment-consumer-group")
public class NotificationConsumer implements RocketMQListener<NotificationMessage> {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    private static final String NOTIFICATION_KEY_PREFIX = "notification:user:";
    private static final String NOTIFICATION_UNREAD = "notification:unread_count:";
    private static final int DEFAULT_EXPIRES_DAYS = 7;
    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;


    /**
     * @param notificationMessage
     */
    @Override
    public void onMessage(NotificationMessage notificationMessage) {
        log.info("收到消息：{}", notificationMessage);
        try{
            Long userId = notificationMessage.getToUserId();
            if(userId == null){
                log.error("消息接收者ID为空");
                return;
            }

            String notificationId = UUID.randomUUID().toString();
            Map<String, Object> notificationMap = new HashMap<>();
            notificationMap.put("notificationId", notificationId);
            notificationMap.put("fromUserId", notificationMessage.getFromUserId());
            notificationMap.put("fromUserAvatar", notificationMessage.getFromUserAvatar());
            notificationMap.put("fromUserNickName", notificationMessage.getFromUserNickName());
            notificationMap.put("commentId", notificationMessage.getCommentId());
            notificationMap.put("commentType", notificationMessage.getCommentType());
            notificationMap.put("articleId", notificationMessage.getArticleId());
            notificationMap.put("createTime", notificationMessage.getCreateTime());
            notificationMap.put("read", false);

            String detailKey = "notification:detail:" + notificationId;
            String notificationJson = objectMapper.writeValueAsString(notificationMap);
            log.info("保存通知详情，key: {}, value: {}", detailKey, notificationJson);

            // 修复：直接存储JSON字符串，不要使用对象序列化
            stringRedisTemplate.opsForValue().set(detailKey, notificationJson, DEFAULT_EXPIRES_DAYS, java.util.concurrent.TimeUnit.DAYS);

            //添加到用户的通知列表
            String userNotificationKey = NOTIFICATION_KEY_PREFIX + userId;
            log.info("添加到用户通知列表，key: {}, id: {}", userNotificationKey, notificationId);
            stringRedisTemplate.opsForZSet().add(
                    userNotificationKey,
                    notificationId,
                    notificationMessage.getCreateTime().getTime()
            );

            log.info("增加用户未读通知计数，key: {}", NOTIFICATION_UNREAD + userId);
            stringRedisTemplate.opsForValue().increment(NOTIFICATION_UNREAD + userId, 1);

            log.info("发送评论通知成功,存入user:{}",  userId);
        } catch (Exception e) {
            log.error("发送评论通知失败",e);
        }
    }
}
