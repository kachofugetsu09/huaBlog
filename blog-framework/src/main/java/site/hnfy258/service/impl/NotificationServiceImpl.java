package site.hnfy258.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
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
        try {
            if (userId == null) {
                log.error("获取未读通知数量失败：用户ID为空");
                return 0L;
            }
            String key = UNREAD_COUNT_KEY_PREFIX + userId;
            String count = redisTemplate.opsForValue().get(key);
            if (count == null) {
                log.info("用户 {} 没有未读通知", userId);
                return 0L;
            }
            try {
                return Long.parseLong(count);
            } catch (NumberFormatException e) {
                log.error("解析未读通知数量失败，key: {}, value: {}", key, count, e);
                return 0L;
            }
        } catch (Exception e) {
            log.error("获取未读通知数量失败", e);
            return 0L;
        }
    }

    @Override
    public PageVo getNotifications(Long userId, Integer pageNum, Integer pageSize) {
        String userNotificationKey = NOTIFICATION_KEY_PREFIX + userId;

        log.info("查询用户通知，userId: {}, key: {}", userId, userNotificationKey);

        int start = (pageNum - 1) * pageSize;
        int end = start + pageSize - 1;

        // 按时间倒序获取通知ID
        Set<String> notificationIds = redisTemplate.opsForZSet().reverseRange(userNotificationKey, start, end);
        log.info("获取到的通知ID: {}", notificationIds);

        if (notificationIds == null || notificationIds.isEmpty()) {
            log.warn("未找到通知数据");
            return new PageVo(new ArrayList<>(), 0L);
        }

        Long total = redisTemplate.opsForZSet().zCard(userNotificationKey);
        log.info("总通知数: {}", total);

        List<NotificationVo> notifications = new ArrayList<>();
        for (String id : notificationIds) {
            // 移除通知ID中可能存在的引号
            String cleanId = id.replace("\"", "");
            String detailKey = NOTIFICATION_DETAIL_PREFIX + cleanId;
            String detail = redisTemplate.opsForValue().get(detailKey);

            log.info("通知详情, key: {}, detail: {}", detailKey, detail);

            if (detail != null) {
                try {
                    // 修复这里：处理JSON字符串被额外包裹在引号中的情况
                    if (detail.startsWith("\"") && detail.endsWith("\"")) {
                        // 移除外层引号并解析转义字符
                        detail = objectMapper.readValue(detail, String.class);
                    }

                    Map<String, Object> notificationMap = objectMapper.readValue(detail, Map.class);
                    NotificationVo notificationVo = new NotificationVo();
                    convertToVo(notificationVo, notificationMap);
                    notifications.add(notificationVo);
                } catch (Exception e) {
                    log.error("解析通知失败", e);
                }
            } else {
                log.warn("通知详情不存在: {}", cleanId);
            }
        }

        return new PageVo(notifications, total);
    }

    @Override
    public boolean markAsReadAndDelete(Long userId, String notificationId) {
        try {
            // 1. 标记为已读
            String detailKey = NOTIFICATION_DETAIL_PREFIX + notificationId;
            String detail = redisTemplate.opsForValue().get(detailKey);

            if (detail != null) {
                // 修复这里：处理JSON字符串被额外包裹在引号中的情况
                if (detail.startsWith("\"") && detail.endsWith("\"")) {
                    detail = objectMapper.readValue(detail, String.class);
                }

                Map<String, Object> notificationMap = objectMapper.readValue(detail, Map.class);
                notificationMap.put("read", true);
                redisTemplate.opsForValue().set(detailKey, objectMapper.writeValueAsString(notificationMap));

                // 2. 减少未读计数
                redisTemplate.opsForValue().decrement(UNREAD_COUNT_KEY_PREFIX + userId);

                // 3. 从用户通知列表中移除
                redisTemplate.opsForZSet().remove(NOTIFICATION_KEY_PREFIX + userId, notificationId);

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
                    String cleanId = id.replace("\"", "");
                    String detailKey = NOTIFICATION_DETAIL_PREFIX + cleanId;
                    String detail = redisTemplate.opsForValue().get(detailKey);

                    if (detail != null) {
                        // 修复这里：处理JSON字符串被额外包裹在引号中的情况
                        if (detail.startsWith("\"") && detail.endsWith("\"")) {
                            detail = objectMapper.readValue(detail, String.class);
                        }

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


    private static void convertToVo(NotificationVo notificationVo, Map<String, Object> notificationMap) {
        notificationVo.setNotificationId((String) notificationMap.get("notificationId"));

        // 处理fromUserId
        Object fromUserId = notificationMap.get("fromUserId");
        if (fromUserId instanceof Number) {
            notificationVo.setFromUserId(((Number) fromUserId).longValue());
        } else if (fromUserId instanceof String) {
            notificationVo.setFromUserId(Long.parseLong((String) fromUserId));
        }

        notificationVo.setFromUserAvatar((String) notificationMap.get("fromUserAvatar"));
        notificationVo.setFromUserNickName((String) notificationMap.get("fromUserNickName"));

        // 处理commentId
        Object commentId = notificationMap.get("commentId");
        if (commentId instanceof Number) {
            notificationVo.setCommentId(((Number) commentId).longValue());
        } else if (commentId instanceof String) {
            notificationVo.setCommentId(Long.parseLong((String) commentId));
        }

        notificationVo.setCommentType((String) notificationMap.get("commentType"));

        // 处理articleId
        Object articleId = notificationMap.get("articleId");
        if (articleId instanceof Number) {
            notificationVo.setArticleId(((Number) articleId).longValue());
        } else if (articleId instanceof String) {
            notificationVo.setArticleId(Long.parseLong((String) articleId));
        }

        // 处理createTime
        Object createTime = notificationMap.get("createTime");
        if (createTime instanceof Long) {
            notificationVo.setCreateTime((Long) createTime);
        } else if (createTime instanceof Number) {
            notificationVo.setCreateTime(((Number) createTime).longValue());
        } else if (createTime instanceof String) {
            try {
                // 尝试解析ISO格式的日期字符串
                if (((String) createTime).contains("T")) {
                    // 使用DateTimeFormatter而不是直接使用Instant.parse
                    java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ISO_DATE_TIME;
                    java.time.OffsetDateTime offsetDateTime = java.time.OffsetDateTime.parse((String) createTime, formatter);
                    java.time.Instant instant = offsetDateTime.toInstant();
                    notificationVo.setCreateTime(instant.toEpochMilli());
                } else {
                    // 尝试直接解析为Long
                    notificationVo.setCreateTime(Long.parseLong((String) createTime));
                }
            } catch (Exception e) {
                log.error("解析createTime失败: {}", createTime, e);
                // 默认设置为当前时间
                notificationVo.setCreateTime(System.currentTimeMillis());
            }
        } else {
            // 默认设置为当前时间
            notificationVo.setCreateTime(System.currentTimeMillis());
        }


        // 处理read标志
        Object read = notificationMap.get("read");
        if (read instanceof Boolean) {
            notificationVo.setRead((Boolean) read);
        } else if (read instanceof String) {
            notificationVo.setRead(Boolean.parseBoolean((String) read));
        } else {
            notificationVo.setRead(false);
        }
    }

}

