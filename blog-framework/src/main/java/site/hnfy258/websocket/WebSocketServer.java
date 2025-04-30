package site.hnfy258.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static ch.qos.logback.core.db.DBHelper.closeConnection;

@ServerEndpoint("/websocket/{userId}")
@Component
public class WebSocketServer {
    private static final ConcurrentHashMap<String, Session> SESSIONS = new ConcurrentHashMap<>();
    private static final Logger log = LoggerFactory.getLogger(WebSocketServer.class);

    private static final long HEARTBEAT_TIMEOUT = 30000;
    private static final long HEARTBEAT_INTERVAL = 15000;

    private static final String PING_MESSAGE = "ping";
    private static final String PONG_MESSAGE= "pong";

    // 用于存储每个会话的最后活动时间
    private static final ConcurrentHashMap<String, Long> LAST_ACTIVITY_TIME = new ConcurrentHashMap<>();

    // 用于存储每个会话的未响应心跳次数
    private static final ConcurrentHashMap<String, AtomicInteger> UNRESPONDED_PINGS = new ConcurrentHashMap<>();

    // 心跳检测线程池
    private static final ScheduledExecutorService HEARTBEAT_SCHEDULER = Executors.newSingleThreadScheduledExecutor();

    static {
        HEARTBEAT_SCHEDULER.scheduleAtFixedRate(
                WebSocketServer::checkHeartbeats,
                HEARTBEAT_INTERVAL,
                HEARTBEAT_INTERVAL,
                TimeUnit.MILLISECONDS
        );

    }


    @OnOpen
    public void onOpen(Session session, @PathParam("userId") String userId) {
        SESSIONS.put(userId, session);
        LAST_ACTIVITY_TIME.put(userId, System.currentTimeMillis());
        UNRESPONDED_PINGS.put(userId, new AtomicInteger(0));
        log.info("WebSocket连接已建立 - 用户ID: {}", userId);
    }

    @OnClose
    public void onClose(@PathParam("userId") String userId) {
        SESSIONS.remove(userId);
        LAST_ACTIVITY_TIME.remove(userId);
        UNRESPONDED_PINGS.remove(userId);
        log.info("WebSocket连接已关闭 - 用户ID: {}", userId);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("WebSocket错误", error);
    }

    @OnMessage
    public void onMessage(String message, Session session, @PathParam("userId") String userId) {
        // 更新最后活动时间
        LAST_ACTIVITY_TIME.put(userId, System.currentTimeMillis());

        // 处理心跳消息
        if (PING_MESSAGE.equals(message)) {
            try {
                session.getBasicRemote().sendText(PONG_MESSAGE);
                log.debug("收到心跳请求并回复 - 用户ID: {}", userId);
            } catch (IOException e) {
                log.error("回复心跳消息失败 - 用户ID: {}", userId, e);
            }
        } else if (PONG_MESSAGE.equals(message)) {
            // 收到pong消息，重置未响应计数
            UNRESPONDED_PINGS.get(userId).set(0);
            log.debug("收到心跳响应 - 用户ID: {}", userId);
        } else {
            // 处理其他业务消息
            log.debug("收到业务消息 - 用户ID: {}, 内容: {}", userId, message);
        }
    }

    public static void sendMessage(String userId, String message) {
        Session session = SESSIONS.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(message);
                LAST_ACTIVITY_TIME.put(userId, System.currentTimeMillis());
                log.debug("消息已发送到用户: {}", userId);
            } catch (IOException e) {
                log.error("发送WebSocket消息失败 - 用户ID: {}", userId, e);
            }
        } else {
            log.warn("用户未连接或连接已关闭 - 用户ID: {}", userId);
        }
    }

    public boolean isUserOnline(String userId) {
        return SESSIONS.containsKey(userId);
    }

    public static Set<String> getOnlineUsers(){
        return SESSIONS.keySet();
    }

    // 发送心跳消息
    private static void sendPing(String userId) {
        Session session = SESSIONS.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(PING_MESSAGE);
                // 增加未响应计数
                UNRESPONDED_PINGS.get(userId).incrementAndGet();
                log.debug("发送心跳消息 - 用户ID: {}", userId);
            } catch (IOException e) {
                log.error("发送心跳消息失败 - 用户ID: {}", userId, e);
            }
        }
    }

    private static void checkHeartbeats() {
        long currentTime = System.currentTimeMillis();

        for (String userId : SESSIONS.keySet()) {
            Long lastActivity = LAST_ACTIVITY_TIME.get(userId);
            if (lastActivity == null) continue;

            // 检查是否超时
            if (currentTime - lastActivity > HEARTBEAT_TIMEOUT) {
                // 检查未响应心跳次数
                AtomicInteger unrespondedPings = UNRESPONDED_PINGS.get(userId);
                if (unrespondedPings != null && unrespondedPings.get() >= 2) {
                    // 连续两次未响应，关闭连接
                    closeConnection(userId);
                    log.warn("心跳超时，关闭连接 - 用户ID: {}", userId);
                } else {
                    // 发送新的心跳
                    sendPing(userId);
                }
            }
        }
    }

    // 关闭连接
    private static void closeConnection(String userId) {
        Session session = SESSIONS.get(userId);
        if (session != null) {
            try {
                session.close();
            } catch (IOException e) {
                log.error("关闭WebSocket连接失败 - 用户ID: {}", userId, e);
            }
        }
    }

    // 清理资源

    public static void shutdown() {
        HEARTBEAT_SCHEDULER.shutdown();
        try {
            if (!HEARTBEAT_SCHEDULER.awaitTermination(5, TimeUnit.SECONDS)) {
                HEARTBEAT_SCHEDULER.shutdownNow();
            }
        } catch (InterruptedException e) {
            HEARTBEAT_SCHEDULER.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @PreDestroy
    public void stop(){
        shutdown();
    }
}