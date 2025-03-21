package site.hnfy258.websocket;

import org.springframework.stereotype.Component;
import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/websocket/{userId}")
@Component
public class WebSocketServer {

    // Store all socket connections
    private static final ConcurrentHashMap<String, Session> SESSIONS = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("userId") String userId) {
        SESSIONS.put(userId, session);
        System.out.println("New connection: " + userId);
    }

    @OnClose
    public void onClose(@PathParam("userId") String userId) {
        SESSIONS.remove(userId);
        System.out.println("Connection closed: " + userId);
    }

    @OnMessage
    public void onMessage(String message, @PathParam("userId") String userId) {
        System.out.println("Received message from " + userId + ": " + message);
        // Process message and potentially send to other users
    }

    @OnError
    public void onError(Session session, Throwable error) {
        error.printStackTrace();
    }

    // Send message to specific user
    public static void sendMessage(String userId, String message) {
        try {
            Session session = SESSIONS.get(userId);
            if (session != null && session.isOpen()) {
                session.getBasicRemote().sendText(message);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}