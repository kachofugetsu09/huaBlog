<template>
  <div class="chat-page">
    <!-- 顶部导航栏 -->
    <div class="chat-navigation">
      <div class="back-button" @click="goBack">
        <i class="el-icon-arrow-left"></i>
        <span>返回</span>
      </div>
      <div class="page-title">
        <i class="el-icon-chat-dot-round"></i>
        <span>私信中心</span>
      </div>
      <div class="nav-actions">
        <el-dropdown trigger="click" @command="handleNavCommand">
          <span class="el-dropdown-link">
            <i class="el-icon-menu"></i>
          </span>
          <el-dropdown-menu slot="dropdown">
            <el-dropdown-item command="home">首页</el-dropdown-item>
            <el-dropdown-item command="share">分类</el-dropdown-item>
            <el-dropdown-item command="friends">友链</el-dropdown-item>
            <el-dropdown-item command="userinfo">个人中心</el-dropdown-item>
          </el-dropdown-menu>
        </el-dropdown>
      </div>
    </div>

    <div class="chat-container">
      <!-- 左侧会话列表 -->
      <ChatSessionList
        :currentUserId="currentUserId"
        :selectedId="selectedSessionId"
        @select-session="handleSelectSession"
        @mark-as-read="markSessionAsReadLocally"
        ref="chatSessionListRef"
      />
      <!-- 聊天区域 -->
      <div class="chat-content">
        <div v-if="!selectedSessionId" class="no-session-selected">
          <div class="empty-state">
            <i class="el-icon-chat-line-round"></i>
            <p>请选择一个会话或创建新会话</p>
          </div>
        </div>
        <template v-else>
          <div class="chat-header">
            <div class="user-info">
              <img :src="resolvedOtherUserInfo.avatar || require('../../static/img/touxiang.jpg')" alt="头像" class="user-avatar">
              <h3>{{ currentSessionName }}</h3>
            </div>
            <div class="connection-status" :class="{ 'connected': wsStatus.connected }">
              <i :class="wsStatus.connected ? 'el-icon-success' : 'el-icon-error'"></i>
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
  </div>
</template>

<script>
import { getChatSessions, getChatMessages, sendMessage, markSessionAsRead } from '../api/chat';
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
      connectionCheckInterval: null,
      autoRefreshInterval: null
    };
  },
  created() {
    this.messages = [];
  },
  methods: {
    // 导航相关方法
    goBack() {
      this.$router.go(-1);
    },
    
    handleNavCommand(command) {
      switch(command) {
        case 'home':
          this.$router.push('/');
          break;
        case 'share':
          this.$router.push('/Share');
          break;
        case 'friends':
          this.$router.push('/FriendsLink');
          break;
        case 'userinfo':
          this.$router.push('/UserInfo');
          break;
      }
    },
    
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
          // 对每个会话更新最后一条消息信息
          const updatedSessions = await Promise.all(response.map(async (session) => {
            const sessionId = session.sessionId;
            
            try {
              // 获取每个会话的消息列表
              const messagesResponse = await getChatMessages(sessionId);
              let messages = [];
              
              if (Array.isArray(messagesResponse)) {
                messages = messagesResponse;
              } else if (messagesResponse && messagesResponse.data && Array.isArray(messagesResponse.data)) {
                messages = messagesResponse.data;
              }
              
              // 如果有消息，用最新的消息更新会话的lastMessage
              if (messages.length > 0) {
                // 按照创建时间排序
                messages.sort((a, b) => new Date(b.createdTime) - new Date(a.createdTime));
                
                // 获取最新的消息
                const latestMessage = messages[0];
                
                // 更新会话信息
                return {
                  ...session,
                  lastMessage: latestMessage.content || '暂无消息',
                  lastMessageTime: latestMessage.createdTime || new Date().toISOString(),
                  lastMessageSenderId: parseInt(latestMessage.senderId) || 0
                };
              }
            } catch (error) {
              console.warn(`获取会话 ${sessionId} 的消息失败:`, error);
            }
            
            // 如果获取消息失败或没有消息，返回原始会话
            return session;
          }));
          
          // 确保会话数据按照最新消息时间排序
          const sortedSessions = [...updatedSessions].sort((a, b) => {
            return new Date(b.lastMessageTime || 0) - new Date(a.lastMessageTime || 0);
          });
          
          this.sessions = sortedSessions;

          if (this.$refs.chatSessionListRef) {
            const formattedResponse = {
              code: 0,
              data: sortedSessions,
              msg: ''
            };
            this.$refs.chatSessionListRef.sessionResponse = formattedResponse;
            this.$refs.chatSessionListRef.updateSessions(sortedSessions);
            
            // 如果当前有选中的会话，则保持其未读状态为已读
            if (this.selectedSessionId) {
              this.markSessionAsReadLocally(this.selectedSessionId);
            }
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
      
      // 先清空消息列表，避免显示上一个会话的消息
      if (this.$refs.chatMessageRef) {
        this.$refs.chatMessageRef.messages = [];
      }
      this.messages = []; // 清空之前的聊天记录

      this.updateOtherUserInfo(sessionId);

      try {
        const response = await getChatMessages(sessionId);
        console.log('获取聊天记录响应:', response);

        let messageList = [];
        if (Array.isArray(response)) {
          messageList = response;
        } else if (response && response.data && response.data.code === 200 && Array.isArray(response.data.data)) {
          messageList = response.data.data;
        } else if (response && response.code === 200 && Array.isArray(response.data)) {
          messageList = response.data;
        } else {
          console.error('获取聊天记录响应格式不正确:', response);
          messageList = [];
        }
        
        this.messages = messageList;
        
        // 如果有消息，更新会话的最后一条消息
        if (messageList.length > 0 && this.$refs.chatSessionListRef) {
          // 按照创建时间排序
          const sortedMessages = [...messageList].sort((a, b) => 
            new Date(b.createdTime) - new Date(a.createdTime)
          );
          
          // 获取最新的消息
          const latestMessage = sortedMessages[0];
          
          // 更新会话列表中的最后一条消息
          const sessions = this.$refs.chatSessionListRef.sessionResponse.data;
          const sessionIndex = sessions.findIndex(s => s.sessionId === sessionId);
          
          if (sessionIndex !== -1) {
            // 更新最后一条消息信息
            this.$set(sessions[sessionIndex], 'lastMessage', latestMessage.content);
            this.$set(sessions[sessionIndex], 'lastMessageTime', latestMessage.createdTime);
            this.$set(sessions[sessionIndex], 'lastMessageSenderId', parseInt(latestMessage.senderId));
            
            // 重要：选择会话时，总是将其未读计数清零
            this.$set(sessions[sessionIndex], 'unreadCount', 0);
            
            // 更新会话列表
            this.$refs.chatSessionListRef.updateSessions(sessions);
          }
        }

        // 前端本地标记会话为已读
        this.markSessionAsReadLocally(sessionId);

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
      const tempMessage = {...newMessage, pending: true, id: `temp_${Date.now()}`};
      this.addMessage(tempMessage);
      console.log('添加临时消息后的消息列表:', this.messages);
      this.scrollToBottom();

      try {
        // 发送消息到服务器
        await sendMessage(newMessage);

        // 也通过WebSocket发送消息
        sendWebSocketMessage(JSON.stringify(newMessage));

        // 更新本地消息状态
        const tempIndex = this.messages.findIndex(m => m.id === tempMessage.id);
        if (tempIndex > -1) {
          this.$set(this.messages, tempIndex, {
            ...this.messages[tempIndex],
            pending: false
          });
        }

        // 立即更新会话列表
        this.updateSessionLastMessageAndRefresh(newMessage);
        
        // 等待一段时间后再次刷新所有数据
        setTimeout(async () => {
          await this.fetchChatSessions();
          this.refreshCurrentSessionMessages();
        }, 1000);
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
    
    // 立即更新会话列表中最后一条消息，不依赖后端响应
    updateSessionLastMessageAndRefresh(message) {
      if (!this.$refs.chatSessionListRef || 
          !this.$refs.chatSessionListRef.sessionResponse || 
          !this.$refs.chatSessionListRef.sessionResponse.data) {
        return;
      }

      const sessions = this.$refs.chatSessionListRef.sessionResponse.data;
      const sessionId = message.sessionId;
      const sessionIndex = sessions.findIndex(s => String(s.sessionId) === String(sessionId));
      
      if (sessionIndex !== -1) {
        // 更新最后一条消息信息 - 确保双方看到的消息内容一致
        this.$set(sessions[sessionIndex], 'lastMessage', message.content);
        this.$set(sessions[sessionIndex], 'lastMessageTime', message.createdTime || new Date().toISOString());
        this.$set(sessions[sessionIndex], 'lastMessageSenderId', parseInt(message.senderId));
        
        // 重新排序会话列表（最新消息在前面）
        const sortedSessions = [...sessions].sort((a, b) => {
          return new Date(b.lastMessageTime || 0) - new Date(a.lastMessageTime || 0);
        });
        
        // 更新会话列表
        this.$refs.chatSessionListRef.updateSessions(sortedSessions);
        
        // 刷新当前会话消息
        if (this.selectedSessionId === message.sessionId) {
          this.refreshCurrentSessionMessages();
        }
      }
    },

    handleWebSocketMessage(message) {
      console.log('ChatPage收到WebSocket消息:', message);

      // 更新特定元素而不是刷新整个页面
      this.refreshCurrentSession();

      // 立即获取最新会话列表，确保显示最新消息
      this.fetchChatSessions();
      
      // 如果消息是针对当前会话的，立即更新UI
      if (message.sessionId === this.selectedSessionId) {
        // 刷新当前会话消息
        this.refreshCurrentSessionMessages();
        
        // 处理消息并添加到消息列表
        const processedMessage = this.processReceivedMessage(message);
        this.addMessage(processedMessage);
        
        // 确保消息已读
        this.markSessionAsReadLocally(this.selectedSessionId);
        
        // 添加多重保险确保滚动到底部
        this.scrollToBottom();
        setTimeout(() => {
          this.scrollToBottom();
        }, 100);
      } else {
        // 如果不是当前会话的消息，确保未读计数显示
        this.updateUnreadCountForSession(message.sessionId);
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

        // 刷新消息时也标记为已读
        this.markSessionAsReadLocally(this.selectedSessionId);

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

      if (!message || !message.content || !message.sessionId) return;

      // 立即更新会话列表中的最后一条消息信息
      this.updateSessionLastMessageAndRefresh(message);

      // 确定消息是否是发给当前用户的
      const isMessageToCurrentUser = message.receiverId === this.currentUserId;

      // 如果消息是针对当前会话的，立即更新UI并标记为已读
      if (message.sessionId && String(message.sessionId) === String(this.selectedSessionId)) {
        this.refreshCurrentSessionMessages();
        // 前端本地标记当前会话内收到的新消息为已读
        this.markSessionAsReadLocally(this.selectedSessionId);
      } else if (message.content && message.sessionId && isMessageToCurrentUser) {
        // 只有当消息是发给当前用户的，才增加未读计数
        this.updateUnreadCountForSession(message.sessionId);
      }
      
      // 延迟刷新会话列表，确保获取最新数据
      setTimeout(() => {
        this.fetchChatSessions();
      }, 500);
    },

    // 更新特定会话的未读计数
    updateUnreadCountForSession(sessionId) {
      if (!sessionId || !this.$refs.chatSessionListRef || 
          !this.$refs.chatSessionListRef.sessionResponse || 
          !this.$refs.chatSessionListRef.sessionResponse.data) {
        return;
      }
      
      const sessions = this.$refs.chatSessionListRef.sessionResponse.data;
      const sessionIndex = sessions.findIndex(s => s.sessionId === sessionId);
      
      if (sessionIndex !== -1) {
        // 增加未读计数
        const currentCount = sessions[sessionIndex].unreadCount || 0;
        this.$set(sessions[sessionIndex], 'unreadCount', currentCount + 1);
        // 调用新增的方法，确保使用SessionList的未读消息管理功能
        if (this.$refs.chatSessionListRef.increaseUnreadCount) {
          this.$refs.chatSessionListRef.increaseUnreadCount(sessionId);
        }
        this.$refs.chatSessionListRef.updateSessions(sessions);
      }
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

    // 本地标记会话为已读（不调用后端API）
    markSessionAsReadLocally(sessionId) {
      if (!sessionId) return;
      
      console.log('前端本地标记会话为已读:', sessionId);
      
      // 更新本地会话列表中的未读计数
      if (this.$refs.chatSessionListRef && this.$refs.chatSessionListRef.sessionResponse && 
          this.$refs.chatSessionListRef.sessionResponse.data) {
        const sessions = this.$refs.chatSessionListRef.sessionResponse.data;
        const sessionIndex = sessions.findIndex(s => s.sessionId === sessionId);
        
        if (sessionIndex !== -1) {
          // 将当前会话的未读数设为0
          this.$set(sessions[sessionIndex], 'unreadCount', 0);
          // 使用SessionList的方法来清除未读计数
          if (this.$refs.chatSessionListRef.clearUnreadCount) {
            this.$refs.chatSessionListRef.clearUnreadCount(sessionId);
          }
          this.$refs.chatSessionListRef.updateSessions(sessions);
        }
      }
    },
  },
  mounted() {
    console.log('ChatPage组件已挂载');
    const userId = this.getUserId();
    console.log('当前用户ID:', userId);

    if (userId) {
      this.fetchChatSessions();
      this.initWebSocket();
      
      // 监听页面可见性变化，当用户返回页面时刷新数据
      document.addEventListener('visibilitychange', this.handleVisibilityChange);
      
      // 设置自动刷新定时器，每5秒刷新一次会话和消息
      this.autoRefreshInterval = setInterval(() => {
        this.fetchChatSessions();
        if (this.selectedSessionId) {
          this.refreshCurrentSessionMessages();
        }
      }, 5000);
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
    
    // 清除自动刷新定时器
    if (this.autoRefreshInterval) {
      clearInterval(this.autoRefreshInterval);
    }

    // 移除事件监听器
    window.removeEventListener('websocket-message', this.handleRawWebSocketMessage);
    document.removeEventListener('visibilitychange', this.handleVisibilityChange);

    // 关闭WebSocket连接
    closeWebSocket();
  },
  // 新增方法：处理页面可见性变化
  handleVisibilityChange() {
    if (document.visibilityState === 'visible') {
      // 页面变为可见时，立即刷新数据
      console.log('页面重新变为可见，刷新数据');
      this.fetchChatSessions();
      if (this.selectedSessionId) {
        this.refreshCurrentSessionMessages();
        // 确保当前选中的会话被标记为已读
        this.markSessionAsReadLocally(this.selectedSessionId);
      }
    }
  }
};
</script>

<style scoped>
.chat-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
  background-color: #f5f7fa;
}

.chat-container {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.chat-navigation {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  background-color: #ffffff;
  box-shadow: 0 1px 4px rgba(0,0,0,0.1);
  z-index: 10;
}

.back-button {
  display: flex;
  align-items: center;
  cursor: pointer;
  color: #409EFF;
  font-weight: 500;
  transition: all 0.3s;
}

.back-button:hover {
  color: #66b1ff;
}

.back-button i {
  margin-right: 5px;
  font-size: 16px;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  display: flex;
  align-items: center;
}

.page-title i {
  margin-right: 8px;
  color: #409EFF;
  font-size: 20px;
}

.nav-actions {
  display: flex;
  align-items: center;
}

.el-dropdown-link {
  cursor: pointer;
  color: #409EFF;
  font-size: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  transition: all 0.3s;
}

.el-dropdown-link:hover {
  background-color: #ecf5ff;
}

.chat-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  border-radius: 8px;
  background-color: #ffffff;
  margin: 0 15px 15px 0;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.chat-header {
  padding: 15px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #ebeef5;
  background-color: #fff;
}

.user-info {
  display: flex;
  align-items: center;
}

.user-info h3 {
  margin: 0;
  font-size: 16px;
  color: #303133;
  font-weight: 600;
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  margin-right: 12px;
  object-fit: cover;
  border: 2px solid #ebeef5;
}

.connection-status {
  display: flex;
  align-items: center;
  font-size: 13px;
  color: #f56c6c;
  padding: 4px 10px;
  border-radius: 12px;
  background-color: #fef0f0;
}

.connection-status.connected {
  color: #67c23a;
  background-color: #f0f9eb;
}

.connection-status i {
  margin-right: 5px;
  font-size: 14px;
}

.no-session-selected {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #fafafa;
  border-radius: 8px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  text-align: center;
}

.empty-state i {
  font-size: 64px;
  color: #dcdfe6;
  margin-bottom: 20px;
}

.empty-state p {
  font-size: 16px;
  color: #909399;
  margin: 0;
}

@media (max-width: 768px) {
  .chat-container {
    flex-direction: column;
  }
  
  .chat-content {
    margin: 0;
    border-radius: 0;
  }
  
  .chat-navigation {
    padding: 8px 15px;
  }
  
  .page-title {
    font-size: 16px;
  }
  
  .user-avatar {
    width: 32px;
    height: 32px;
  }
  
  .user-info h3 {
    font-size: 14px;
  }
  
  .connection-status {
    font-size: 12px;
    padding: 3px 8px;
  }
}
</style>
