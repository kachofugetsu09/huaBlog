    import request from "../utils/request";

    export function getChatSessions(currentUserId) {
      return request({
        url: `/chat/sessions/${currentUserId}`,
        method: 'get'
      }).then(response => {
        console.log('API层收到的原始响应:', response);
        return response;
      });
    }
    // 获取聊天记录
    export function getChatMessages(sessionId) {
      return request({
        url: '/chat/messages',
        method: 'get',
        params: { sessionId }
      });
    }
    // 发送消息
    export function sendMessage(data) {
      return request({
        url: '/chat/send',
        method: 'post',
        data,
        timeout:10000
      })}

    export function createSession(userId, targetId) {
      return request({
        url: '/chat/session/create',
        method: 'post',
        data: {
          userId: userId,
          targetId: targetId
        }
      })
    }
