package site.hnfy258.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import site.hnfy258.VO.NotificationVo;
import site.hnfy258.VO.PageVo;
import site.hnfy258.service.NotificationService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service("notificationService")
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    private static final String NOTIFICATION_KEY_PREFIX = "notification:user:";
    private static final String NOTIFICATION_DETAIL_PREFIX = "notification:detail:";
    private static final String UNREAD_COUNT_KEY_PREFIX = "notification:unread_count:";

    @Override
    public Long getUnreadCount(Long userId) {
        String key = UNREAD_COUNT_KEY_PREFIX + userId;
        String count = redisTemplate.opsForValue().get(key);
        return count != null ? Long.parseLong(count) : 0L;
    }

    @Override
    public PageVo getNotifications(Long userId, Integer pageNum, Integer pageSize) {
        String userNotificationKey = NOTIFICATION_KEY_PREFIX + userId;

        int start = (pageNum - 1) * pageSize;
        int end = start + pageSize - 1;

        // 按时间倒序获取通知ID
        Set<String> notificationIds = redisTemplate.opsForZSet().reverseRange(userNotificationKey, start, end);
        if (notificationIds == null || notificationIds.isEmpty()) {
            return new PageVo(new ArrayList<>(), 0L);
        }

        Long total = redisTemplate.opsForZSet().zCard(userNotificationKey);

        List<NotificationVo> notifications = new ArrayList<>();
        for (String id : notificationIds) {
            String detailKey = NOTIFICATION_DETAIL_PREFIX + id;
            String detail = redisTemplate.opsForValue().get(detailKey);

            if (detail != null) {
                try {
                    Map<String, Object> notificationMap = objectMapper.readValue(detail, Map.class);
                    NotificationVo notificationVo = new NotificationVo();

                    convertToVo(notificationVo, notificationMap);

                    notifications.add(notificationVo);
                } catch (Exception e) {
                    log.error("解析通知失败", e);
                }
            }
        }

        return new PageVo(notifications, total);
    }

    private static void convertToVo(NotificationVo notificationVo, Map<String, Object> notificationMap) {
        notificationVo.setNotificationId((String) notificationMap.get("notificationId"));
        notificationVo.setFromUserId(((Number) notificationMap.get("fromUserId")).longValue());
        notificationVo.setFromUserAvatar((String) notificationMap.get("fromUserAvatar"));
        notificationVo.setFromUserNickName((String) notificationMap.get("fromUserNickName"));
        notificationVo.setCommentId(((Number) notificationMap.get("commentId")).longValue());
        notificationVo.setCommentType((String) notificationMap.get("commentType"));
        notificationVo.setArticleId(((Number) notificationMap.get("articleId")).longValue());
        notificationVo.setCreateTime((Long) notificationMap.get("createTime"));
        notificationVo.setRead((Boolean) notificationMap.get("read"));
    }

    @Override
    public boolean markAsReadAndDelete(Long userId, Long notificationId) {
        try {
            // 1. 标记为已读
            String detailKey = NOTIFICATION_DETAIL_PREFIX + notificationId;
            String detail = redisTemplate.opsForValue().get(detailKey);

            if (detail != null) {
                Map<String, Object> notificationMap = objectMapper.readValue(detail, Map.class);
                notificationMap.put("read", true);
                redisTemplate.opsForValue().set(detailKey, objectMapper.writeValueAsString(notificationMap));

                // 2. 减少未读计数
                redisTemplate.opsForValue().decrement(UNREAD_COUNT_KEY_PREFIX + userId);

                // 3. 从用户通知列表中移除
                redisTemplate.opsForZSet().remove(NOTIFICATION_KEY_PREFIX + userId, notificationId.toString());

                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("标记通知已读失败", e);
            return false;
        }
    }

    @Override
    public void markAllAsReadAndDelete(Long userId) {
        try {
            // 1. 获取该用户所有通知ID
            String userNotificationKey = NOTIFICATION_KEY_PREFIX + userId;
            Set<String> notificationIds = redisTemplate.opsForZSet().range(userNotificationKey, 0, -1);

            if (notificationIds != null && !notificationIds.isEmpty()) {
                // 2. 将所有通知标记为已读
                for (String id : notificationIds) {
                    String detailKey = NOTIFICATION_DETAIL_PREFIX + id;
                    String detail = redisTemplate.opsForValue().get(detailKey);

                    if (detail != null) {
                        Map<String, Object> notificationMap = objectMapper.readValue(detail, Map.class);
                        notificationMap.put("read", true);
                        redisTemplate.opsForValue().set(detailKey, objectMapper.writeValueAsString(notificationMap));
                    }
                }

                // 3. 清空未读计数
                redisTemplate.delete(UNREAD_COUNT_KEY_PREFIX + userId);

                // 4. 清空通知列表
                redisTemplate.delete(userNotificationKey);
            }
        } catch (Exception e) {
            log.error("标记所有通知已读失败", e);
        }
    }
}

