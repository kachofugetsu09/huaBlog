<template>
  <div class="chat-page">
    <!-- 左侧会话列表 -->
    <ChatSessionList
      :currentUserId="currentUserId"
      :selectedId="selectedSessionId"
      @select-session="handleSelectSession"
      @mark-as-read="markSessionAsRead"
      ref="chatSessionListRef"
    />
    <!-- 聊天区域 -->
    <div class="chat-content">
      <div v-if="!selectedSessionId" class="no-session-selected">
        <p>请选择一个会话或创建新会话</p>
      </div>
      <template v-else>
        <div class="chat-header">
          <h3>{{ currentSessionName }}</h3>
          <div class="connection-status" :class="{ 'connected': wsStatus.connected }">
            {{ wsStatus.connected ? '已连接' : '未连接' }}
          </div>
        </div>

        <ChatMessage
          :sessionId="selectedSessionId"
          :messages="messages"
          :otherUser="resolvedOtherUserInfo"
          @add-message="addMessage"
          @update-message="updateMessage"
          @refresh-session="refreshCurrentSession"
          ref="chatMessageRef"
        />
      </template>
    </div>
  </div>
</template>

<script>
import { getChatSessions, getChatMessages, sendMessage } from '../api/chat';
import ChatSessionList from '../components/ChatSessionList.vue';
import ChatMessage from '../components/ChatMessage.vue';
import { connectWebSocket, sendWebSocketMessage, getWebSocketStatus, closeWebSocket } from '../api/websocket';

export default {
  name: 'ChatPage',
  components: { ChatSessionList, ChatMessage },
  data() {
    return {
      sessions: [],
      selectedSessionId: null,
      messages: [],
      currentUserId: null,
      userAvatar: '',
      resolvedOtherUserInfo: {
        userId: '',
        avatar: '',
        userName: ''
      },
      wsStatus: { connected: false, state: 'CLOSED', stateCode: -1 },
      loading: false,
      currentSessionName: '',
      connectionCheckInterval: null
    };
  },
  created() {
    this.messages = [];
  },
  methods: {
    scrollToBottom() {
      this.$nextTick(() => {
        if (this.$refs.chatMessageRef && this.$refs.chatMessageRef.scrollToBottom) {
          this.$refs.chatMessageRef.scrollToBottom();
          
          // 添加延时滚动作为额外保险
          setTimeout(() => {
            this.$refs.chatMessageRef.scrollToBottom();
          }, 100);
          
          setTimeout(() => {
            this.$refs.chatMessageRef.scrollToBottom();
          }, 300);
        }
      });
    },

    // 标记会话为已读
    async markSessionAsRead(sessionId) {
      try {
        // 调用后端API将会话标记为已读
        const response = await this.markChatSessionAsRead(sessionId);
        console.log('标记会话已读结果:', response);
        
        // 无论API调用成功与否，更新本地会话列表状态
        this.updateSessionUnreadCount(sessionId, 0);
        
        // 刷新会话列表
        this.fetchChatSessions();
      } catch (error) {
        console.warn('标记会话已读失败:', error);
      }
    },
    
    // 调用后端API标记会话为已读
    markChatSessionAsRead(sessionId) {
      // 如果后端有标记已读的API，使用以下代码
      // return request({
      //   url: `/chat/sessions/${sessionId}/read`,
      //   method: 'put'
      // });
      
      // 如果没有API，创建一个模拟的Promise
      return new Promise(resolve => {
        console.log('模拟标记会话已读:', sessionId);
        setTimeout(() => resolve({ code: 200, msg: '操作成功' }), 100);
      });
    },
    
    // 更新本地会话的未读计数
    updateSessionUnreadCount(sessionId, count) {
      if (this.$refs.chatSessionListRef && this.$refs.chatSessionListRef.sessionResponse) {
        const sessions = this.$refs.chatSessionListRef.sessionResponse.data;
        const sessionIndex = sessions.findIndex(s => s.sessionId === sessionId);
        
        if (sessionIndex !== -1) {
          // 更新未读消息计数
          this.$set(sessions[sessionIndex], 'unreadCount', count);
        }
      }
    },

    getUserId() {
      const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
      this.currentUserId = userInfo.userId || userInfo.id;
      this.userAvatar = userInfo.avatar || '';
      return this.currentUserId;
    },

    async fetchChatHistory() {
      try {
        const response = await getChatHistory(this.sessionId);
        if (response && response.code === 200 && Array.isArray(response.data)) {
          this.messages = [...response.data];
          console.log('设置消息列表:', this.messages);
          this.scrollToBottom();
        }
      } catch (error) {
        console.error('获取聊天记录失败:', error);
        this.$message.error('获取聊天记录失败');
      }
    },

    async fetchChatSessions() {
      this.loading = true;
      try {
        const response = await getChatSessions(this.currentUserId);
        console.log('获取会话列表响应:', response);

        if (Array.isArray(response)) {
          // 确保每个会话数据包含必要的字段
          const processedSessions = response.map(session => {
            return {
              ...session,
              // 确保有未读计数字段
              unreadCount: session.unreadCount || 0,
              // 确保有发送者ID信息
              lastMessageSenderId: session.lastMessageSenderId || null
            };
          });
          
          this.sessions = processedSessions;

          if (this.$refs.chatSessionListRef) {
            const formattedResponse = {
              code: 0,
              data: processedSessions,
              msg: ''
            };
            this.$refs.chatSessionListRef.sessionResponse = formattedResponse;
            this.$refs.chatSessionListRef.updateSessions(processedSessions);
          }
        } else {
          console.error('会话列表响应不是数组:', response);
        }
      } catch (error) {
        console.error('获取会话列表失败:', error);
      } finally {
        this.loading = false;
      }
    },

    updateMessage(id, updates) {
      console.log('更新消息:', id, updates);
      const index = this.messages.findIndex(m => m.id === id);
      if (index !== -1) {
        const updatedMessage = {...this.messages[index], ...updates};
        console.log('更新前:', this.messages[index]);
        console.log('更新后:', updatedMessage);
        this.$set(this.messages, index, updatedMessage);
      } else {
        console.warn('未找到要更新的消息:', id);
      }
    },

    async handleSelectSession(sessionId) {
      console.log('切换到会话:', sessionId);
      this.selectedSessionId = sessionId;
      this.messages = []; // 清空之前的聊天记录

      this.updateOtherUserInfo(sessionId);

      try {
        const response = await getChatMessages(sessionId);
        console.log('获取聊天记录响应:', response);

        if (Array.isArray(response)) {
          this.messages = response;
        } else if (response && response.data && response.data.code === 200 && Array.isArray(response.data.data)) {
          this.messages = response.data.data;
        } else if (response && response.code === 200 && Array.isArray(response.data)) {
          this.messages = response.data;
        } else {
          console.error('获取聊天记录响应格式不正确:', response);
          this.messages = [];
        }

        // 多重保险确保在会话切换后滚动到底部
        this.$nextTick(() => {
          this.scrollToBottom();
          
          // 延迟100ms再次滚动（防止DOM更新延迟）
          setTimeout(() => {
            this.scrollToBottom();
          }, 100);
          
          // 延迟300ms再次滚动（防止图片加载等情况）
          setTimeout(() => {
            this.scrollToBottom();
          }, 300);
        });
      } catch (error) {
        console.error('获取聊天记录失败:', error);
      }
    },

    updateOtherUserInfo(sessionId) {
      const currentSession = this.sessions.find(s => s.sessionId === sessionId);
      console.log('当前选中的会话:', currentSession);

      if (currentSession) {
        this.resolvedOtherUserInfo = {
          userId: currentSession.userId || '',
          avatar: currentSession.avatar || '',
          userName: currentSession.userName || ''
        };
        this.currentSessionName = currentSession.nickname || currentSession.userName || `用户 ${currentSession.userId}`;
        console.log('已更新对话用户信息:', this.resolvedOtherUserInfo);
      } else {
        this.resolvedOtherUserInfo = {
          userId: '',
          avatar: '',
          userName: ''
        };
        this.currentSessionName = '';
      }
    },

    async handleSendMessage(messageContent) {
      if (!messageContent.trim()) return;
      console.log('发送消息:', messageContent);

      const newMessage = {
        sessionId: this.selectedSessionId,
        senderId: this.currentUserId,
        receiverId: this.resolvedOtherUserInfo.userId,
        content: messageContent,
        createdTime: new Date().toISOString(),
      };

      // 先本地添加消息（优化体验）
      const tempMessage = {...newMessage, pending: true, id: Date.now()};
      this.addMessage(tempMessage);
      console.log('添加临时消息后的消息列表:', this.messages);
      this.scrollToBottom();

      try {
        // 发送消息到服务器
        await sendMessage(newMessage);

        // 也通过WebSocket发送消息
        sendWebSocketMessage(JSON.stringify(newMessage));
        this.fetchChatSessions();

        // 更新本地消息状态
        const tempIndex = this.messages.findIndex(m => m.id === tempMessage.id);
        if (tempIndex > -1) {
          this.$set(this.messages, tempIndex, {
            ...this.messages[tempIndex],
            pending: false
          });
        }

        // 刷新会话列表
        this.fetchChatSessions();
        this.refreshCurrentSessionMessages();
      } catch (error) {
        console.warn('发送消息可能失败:', error);

        // 即使发送失败也刷新会话列表
        this.fetchChatSessions();

        // 标记消息发送失败
        const tempIndex = this.messages.findIndex(m => m.id === tempMessage.id);
        if (tempIndex > -1) {
          this.$set(this.messages, tempIndex, {
            ...this.messages[tempIndex],
            failed: true,
            pending: false
          });
        }
        this.refreshCurrentSessionMessages();
      }
    },

    handleWebSocketMessage(message) {
      console.log('ChatPage收到WebSocket消息:', message);

      // 更新特定元素而不是刷新整个页面
      this.refreshCurrentSession();

      // 如果消息是针对当前会话的，立即更新UI
      if (message.sessionId === this.selectedSessionId) {
        const processedMessage = this.processReceivedMessage(message);
        this.addMessage(processedMessage);
        // 添加多重保险确保滚动到底部
        this.scrollToBottom();
        setTimeout(() => {
          this.scrollToBottom();
        }, 100);
      }
    },

    async refreshCurrentSession() {
      await this.fetchChatSessions();
      if (this.selectedSessionId) {
        this.refreshCurrentSessionMessages();
      }
    },

    async refreshCurrentSessionMessages() {
      try {
        const response = await getChatMessages(this.selectedSessionId);

        if (Array.isArray(response)) {
          this.messages = response;
        } else if (response && response.data && Array.isArray(response.data)) {
          this.messages = response.data;
        }

        // 确保在消息更新后滚动到底部，使用多个nextTick和setTimeout
        this.$nextTick(() => {
          this.scrollToBottom();
          // 再添加一个延时滚动（双保险）
          setTimeout(() => {
            this.scrollToBottom();
          }, 100);
        });
      } catch (error) {
        console.warn('刷新消息失败，但不影响操作:', error);
      }
    },

    processReceivedMessage(message) {
      // 确保senderId和currentUserId都是字符串或数字类型进行比较
      const isSelf = String(message.senderId) === String(this.currentUserId);

      return {
        ...message,
        isCurrentUser: isSelf,
        senderAvatar: isSelf ? this.userAvatar : this.resolvedOtherUserInfo.avatar,
        // 确保有创建时间
        createdTime: message.createdTime || new Date().toISOString()
      };
    },

    initWebSocket() {
      try {
        // 使用导入的connectWebSocket函数
        connectWebSocket(this.currentUserId);

        // 设置WebSocket消息处理器
        window.addEventListener('websocket-message', this.handleRawWebSocketMessage);

        // 设置状态更新检查器
        this.connectionCheckInterval = setInterval(() => {
          this.wsStatus = getWebSocketStatus();
        }, 2000);
      } catch (error) {
        console.error('初始化WebSocket失败:', error);
      }
    },

    handleRawWebSocketMessage(event) {
      console.log("接收到WebSocket消息事件:", event.detail);

      // 因为我们在websocket.js中已经解析了JSON，所以event.detail应该已经是对象
      const message = event.detail;

      if (!message) return;

      // 收到消息后，更新会话未读状态
      if (message.sessionId && message.senderId && 
          String(message.senderId) !== String(this.currentUserId)) {
        // 如果不是当前选中的会话，增加未读计数
        if (String(message.sessionId) !== String(this.selectedSessionId)) {
          this.incrementSessionUnreadCount(message.sessionId);
        }
      }

      this.fetchChatSessions();

      // 如果消息是针对当前会话的，立即更新UI
      if (message.sessionId && String(message.sessionId) === String(this.selectedSessionId)) {
        this.refreshCurrentSessionMessages();
      }

      // 刷新会话列表以显示最新状态
      this.fetchChatSessions();
    },

    addMessage(message) {
      // 防止重复添加相同ID的消息
      if (!this.messages.some(m => m.id === message.id)) {
        this.messages.push(message);
        this.$nextTick(() => {
          this.scrollToBottom();
          // 延迟滚动到底部（双保险）
          setTimeout(() => {
            this.scrollToBottom();
          }, 100);
        });
      }
    },

    // 增加会话的未读计数
    incrementSessionUnreadCount(sessionId) {
      if (this.$refs.chatSessionListRef && this.$refs.chatSessionListRef.sessionResponse) {
        const sessions = this.$refs.chatSessionListRef.sessionResponse.data;
        const sessionIndex = sessions.findIndex(s => s.sessionId === sessionId);
        
        if (sessionIndex !== -1) {
          // 增加未读消息计数
          const currentCount = sessions[sessionIndex].unreadCount || 0;
          this.$set(sessions[sessionIndex], 'unreadCount', currentCount + 1);
        }
      }
    }
  },
  mounted() {
    console.log('ChatPage组件已挂载');
    const userId = this.getUserId();
    console.log('当前用户ID:', userId);

    if (userId) {
      this.fetchChatSessions();
      this.initWebSocket();
    } else {
      // 如果没有用户ID，重定向到登录页
      const message = '请先登录';
      if (window.$message) {
        window.$message.error(message);
      } else {
        alert(message);
      }
      this.$router.push('/Login');
    }
  },
  beforeDestroy() {
    // 组件销毁时清除定时器
    if (this.connectionCheckInterval) {
      clearInterval(this.connectionCheckInterval);
    }

    // 移除事件监听器
    window.removeEventListener('websocket-message', this.handleRawWebSocketMessage);

    // 关闭WebSocket连接
    closeWebSocket();
  }
};
</script>

<style scoped>
.chat-page {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.chat-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.chat-header {
  padding: 15px;
  border-bottom: 1px solid #e0e0e0;
  background-color: #fff;
}

.chat-header h3 {
  margin: 0;
  font-size: 18px;
  color: #333;
}

.no-session-selected {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 18px;
  color: #999;
}
</style>
