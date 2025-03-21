package site.hnfy258.controller;

import com.mysql.cj.protocol.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import site.hnfy258.VO.MessagesVo;
import site.hnfy258.VO.PageVo;
import site.hnfy258.domain.ResponseResult;
import site.hnfy258.entity.Messages;
import site.hnfy258.service.ChatService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/chat")
public class ChatController {
    @Autowired
    private ChatService chatService;
    @GetMapping("/sessions/{currentUserId}")
    public ResponseResult getSessions(@PathVariable Long currentUserId) {
        return ResponseResult.okResult(chatService.getSessions(currentUserId));
    }
    @GetMapping("/messages")
    public ResponseResult getMessages(@RequestParam Long sessionId) {
        return ResponseResult.okResult(chatService.getMessages(sessionId));
    }

    @PostMapping("/send")
    public ResponseResult sendMessage(@RequestBody Messages message) {
        chatService.sendMessage(message);
        return ResponseResult.okResult();
    }
    @PostMapping("/session/create")
    public ResponseResult createSession(@RequestBody Map<String, Long> request) {
        return ResponseResult.okResult(chatService.createSession(request.get("userId"), request.get("targetId")));
    }



}
