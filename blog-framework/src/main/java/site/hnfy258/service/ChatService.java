package site.hnfy258.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.mysql.cj.protocol.Message;
import site.hnfy258.VO.MessageDetailVo;
import site.hnfy258.VO.MessagesVo;
import site.hnfy258.VO.PageVo;
import site.hnfy258.domain.ResponseResult;
import site.hnfy258.entity.Messages;

import java.util.List;

public interface ChatService extends IService<Messages> {
    List<MessagesVo> getSessions(Long currentUserId);

    List<MessageDetailVo>   getMessages(Long sessionId);

    void sendMessage(Messages message);

    Long createSession(Long userId, Long targetId);
}
