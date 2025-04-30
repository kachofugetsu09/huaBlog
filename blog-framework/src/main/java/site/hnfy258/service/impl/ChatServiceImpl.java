package site.hnfy258.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mysql.cj.protocol.Message;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.hnfy258.VO.MessageDetailVo;
import site.hnfy258.VO.MessagesVo;
import site.hnfy258.domain.ResponseResult;
import site.hnfy258.entity.LoginUser;
import site.hnfy258.entity.Messages;
import site.hnfy258.mapper.SessionMapper;
import site.hnfy258.mapper.UserMapper;
import site.hnfy258.service.ChatService;
import site.hnfy258.mapper.ChatMapper;
import site.hnfy258.entity.Sessions;
import site.hnfy258.service.UserService;
import site.hnfy258.utils.SecurityUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ChatServiceImpl extends ServiceImpl<ChatMapper, Messages> implements ChatService {
    /**
     * @param currentUserId
     * @return
     */
    @Autowired
    private ChatMapper chatMapper;
    @Autowired
    private SessionMapper sessionMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RedisMessageService redisMessageService;


    @Override
    public List<MessagesVo> getSessions(Long currentUserId) {
        String cacheKey = "chat:sessions:" + currentUserId;

        try {
            // 1. 尝试从缓存获取
            List<MessagesVo> cachedSessions = (List<MessagesVo>) redisTemplate.opsForValue().get(cacheKey);
            if (cachedSessions != null && !cachedSessions.isEmpty()) {
                return cachedSessions;
            }
        } catch (Exception e) {
            log.error("获取会话缓存失败", e);
        }

        // 2. 从数据库查询
        LambdaQueryWrapper<Sessions> sessionQuery = new LambdaQueryWrapper<>();
        sessionQuery.eq(Sessions::getUser1Id, currentUserId)
                .or(i -> i.eq(Sessions::getUser2Id, currentUserId));
        List<Sessions> sessions = sessionMapper.selectList(sessionQuery);

        // 3. 转换为VO并缓存
        List<MessagesVo> result = sessions.stream()
                .map(session -> convertToMessagesVo(session, currentUserId))
                .collect(Collectors.toList());

        try {
            redisTemplate.opsForValue().set(cacheKey, result, 10, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("缓存会话列表失败", e);
        }

        return result;
    }


    private MessagesVo convertToMessagesVo(Sessions session, Long currentUserId) {
        // 确定对方用户 ID
        Long otherUserId = session.getUser1Id().equals(currentUserId) ? session.getUser2Id() : session.getUser1Id();

        // 查询最后一条消息
        LambdaQueryWrapper<Messages> messageQuery = new LambdaQueryWrapper<>();
        messageQuery.eq(Messages::getSessionId, session.getId())
                .orderByDesc(Messages::getCreatedTime)
                .last("LIMIT 1");
        Messages lastMessage = chatMapper.selectOne(messageQuery);

        // 获取用户头像
        String avatar = userMapper.getAvatarById(otherUserId);
        String nickname = userMapper.getNicknameById(otherUserId);
        System.out.println("Avatar for user " + otherUserId + ": " + avatar);

        // 构造 VO 对象
        MessagesVo messagesVo = new MessagesVo()
                .setSessionId(session.getId())
                .setUserId(otherUserId)
                .setAvatar(avatar) // 确保头像被设置
                .setLastMessage(lastMessage != null ? lastMessage.getContent() : "No messages")
                .setUnreadCount(getUnreadCount(session.getId(), currentUserId))
                .setNickname(nickname);
        System.out.println("MessagesVo object: " + messagesVo);
        return messagesVo;
    }

    /**
     * @param sessionId
     * @return
     */
    @Override
    public List<MessageDetailVo> getMessages(Long sessionId) {
        Long currentUserId = SecurityUtils.getLoginUser().getUser().getId();
        String cacheKey = "chat:messages:session:" + sessionId;
        List<Messages> messages;

        try {
            // 1. 尝试从 Redis 获取缓存
            List<Messages> cachedMessages = (List<Messages>) redisTemplate.opsForValue().get(cacheKey);
            if (cachedMessages != null&&!cachedMessages.isEmpty()) {
                // 缓存命中：更新消息状态并直接返回
                return convertToMessageDetailVo(updateReadStatus(cachedMessages, sessionId), currentUserId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 2. 缓存未命中：从数据库加载数据
        LambdaQueryWrapper<Messages> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Messages::getSessionId, sessionId)
                .orderByAsc(Messages::getCreatedTime);
        messages = chatMapper.selectList(queryWrapper);

        // 3. 更新消息为已读状态
        messages = updateReadStatus(messages, sessionId);

        // 4. 存入 Redis 缓存（10秒过期）
        try {
            redisTemplate.opsForValue().set(cacheKey, messages, 10, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 5. 转换为前端需要的 VO 对象
        return convertToMessageDetailVo(messages, currentUserId);
    }
    private List<Messages> updateReadStatus(List<Messages> messages, Long sessionId) {
        // Get current user ID (implement this based on your security context)
        Long currentUserId = SecurityUtils.getLoginUser().getUser().getId();

        List<Messages> messagesToUpdate = new ArrayList<>();

        // Mark messages as read
        for (Messages msg : messages) {
            // Only mark as read if current user is the recipient and message is unread
            if (currentUserId.equals(msg.getReceiverId()) && msg.getIsRead() == 0) {
                msg.setIsRead(1);
                messagesToUpdate.add(msg);
            }
        }

        // Update in database if needed
        if (!messagesToUpdate.isEmpty()) {
            // Batch update to database
            for (Messages msg : messagesToUpdate) {
                chatMapper.updateById(msg);
            }

            // Update cache with new read status
            String cacheKey = "chat:messages:session:" + sessionId;
            try {
                redisTemplate.opsForValue().set(cacheKey, messages, 10, TimeUnit.MINUTES);

                // Clear unread count cache
                String unreadCountKey = "chat:unread:" + sessionId + ":" + currentUserId;
                redisTemplate.delete(unreadCountKey);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return messages;
    }
    private Integer getUnreadCount(Long sessionId, Long currentUserId) {
        String cacheKey = "chat:unread:" + sessionId + ":" + currentUserId;

        // Try to get count from cache
        try {
            Integer count = (Integer) redisTemplate.opsForValue().get(cacheKey);
            if (count != null) {
                return count;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // If not in cache, get from database
        LambdaQueryWrapper<Messages> unreadQuery = new LambdaQueryWrapper<>();
        unreadQuery.eq(Messages::getSessionId, sessionId)
                .eq(Messages::getReceiverId, currentUserId)
                .eq(Messages::getIsRead, 0);
        Integer count = chatMapper.selectCount(unreadQuery);

        // Cache the result
        try {
            redisTemplate.opsForValue().set(cacheKey, count, 10, TimeUnit.MINUTES);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return count;
    }

    /**
     * @param message
     */
    @Override
    public void sendMessage(Messages message) {
        // 验证消息长度
        final int MAX_MESSAGE_LENGTH = 500;
        if (message.getContent() == null || message.getContent().length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("消息内容无效");
        }

        // 设置发送者和接收者ID
        Long currentUserId = SecurityUtils.getLoginUser().getUser().getId();
        message.setSenderId(currentUserId);
        Sessions session = sessionMapper.selectById(message.getSessionId());
        message.setReceiverId(
                session.getUser1Id().equals(currentUserId)
                        ? session.getUser2Id()
                        : session.getUser1Id()
        );

        // 只发送到Redis Stream，不再直接操作数据库
        redisMessageService.sendMessage(message);
    }


    @Transactional
    public Long createSession(Long userId, Long targetId) {
        if (userId.equals(targetId)) {
            throw new IllegalArgumentException("Cannot create a chat session with yourself");
        }
        // Always put the smaller ID as user1Id for consistency
        Long smallerId = Math.min(userId, targetId);
        Long largerId = Math.max(userId, targetId);

        // Create a query with consistent ordering of user IDs
        LambdaQueryWrapper<Sessions> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Sessions::getUser1Id, smallerId)
                .eq(Sessions::getUser2Id, largerId);

        // Use selectList to avoid the TooManyResultsException
        List<Sessions> existingSessions = sessionMapper.selectList(queryWrapper);

        if (!existingSessions.isEmpty()) {
            // If there are somehow multiple sessions, return the first one
            // Later you might want to clean up duplicates in a maintenance task
            return existingSessions.get(0).getId();
        } else {
            // Create new session with consistent ordering
            Sessions newSession = new Sessions();
            newSession.setUser1Id(smallerId);
            newSession.setUser2Id(largerId);

            // Insert the new session
            sessionMapper.insert(newSession);

            // Return the new session id
            return newSession.getId();
        }
    }

    private List<MessageDetailVo> convertToMessageDetailVo(List<Messages> messages, Long currentUserId) {
        return messages.stream()
                .map(msg -> {
                    MessageDetailVo vo = new MessageDetailVo();
                    BeanUtils.copyProperties(msg, vo);
                    vo.setSenderAvatar(userMapper.getAvatarById(msg.getSenderId()));
                    vo.setReceiverAvatar(userMapper.getAvatarById(msg.getReceiverId()));
                    vo.setNickname(userMapper.getNicknameById(msg.getSenderId()));
                    vo.setIsCurrentUser(currentUserId.equals(msg.getSenderId()));
                    return vo;
                })
                .collect(Collectors.toList());
    }
}

