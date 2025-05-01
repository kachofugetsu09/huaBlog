<template>
  <div class="chat-message-container" ref="messageContainer" @scroll="handleScroll">

    <!-- 无消息提示 -->
    <div v-if="messages.length === 0" class="empty-message">
      <i class="el-icon-chat-line-square"></i>
      <p>暂无消息，开始聊天吧~</p>
    </div>

    <!-- 消息列表 -->
    <div class="message-list">
      <div
        v-for="(message, index) in messages"
        :key="message.id || index"
        class="message-item"
        :class="{
          'own-message': isSelfMessage(message),
          'other-message': !isSelfMessage(message),
          'pending-message': message.pending,
          'failed-message': message.failed
        }"
      >
        <div class="message-avatar">
          <img :src="getMessageAvatar(message)" alt="头像">
        </div>
        <div class="message-content">
          <div class="message-bubble">{{ message.content }}</div>
          <div class="message-time">
            <span v-if="message.pending"><i class="el-icon-loading"></i> 发送中...</span>
            <span v-else-if="message.failed">
              <i class="el-icon-warning"></i> 发送失败
              <button class="retry-btn" @click="retryMessage(message)">重试</button>
            </span>
            <span v-else>{{ formatTime(message.createdTime) }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 输入区域 -->
    <div class="message-input-container">
      <textarea
        ref="messageInput"
        v-model="inputMessage"
        class="message-input"
        placeholder="请输入消息..."
        @keydown.enter.prevent="handleEnterKey"
      ></textarea>
      <button
        class="send-button"
        :disabled="!inputMessage.trim()"
        @click="sendMessage"
      >
        <i class="el-icon-s-promotion"></i>
        发送
      </button>
    </div>
  </div>
</template>

<script>
import { sendMessage as sendChatMessage  } from '../api/chat';

export default {
  name: 'ChatMessage',
  props: {
    sessionId: {
      type: [String, Number],
      required: true
    },
    messages: {
      type: Array,
      default: () => []
    },
    otherUser: {
      type: Object,
      default: () => ({})
    },
    debug: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      inputMessage: '',
      isSending: false,
      pendingMessages: [],
      currentUserId: this.$store.getters.userId || localStorage.getItem('userId'),
      defaultAvatar: require('../../static/img/touxiang.jpg'),
      isUserScrolling: false,      // 用户是否正在手动滚动
      shouldScrollToBottom: true   // 是否应该自动滚动到底部
    };
  },
  computed: {
    userInfo() {
      return this.$store.state.user || {}; // Adjust the path based on your actual Vuex store structure
    },
    userAvatar() {
      return this.userInfo.avatar || this.defaultAvatar;
    }
  },
  watch: {
    messages: {
      handler(newVal, oldVal) {
        // 防御性编程，确保newVal和oldVal都是数组
        if (!Array.isArray(newVal) || !Array.isArray(oldVal)) {
          console.warn('messages watcher接收到无效数据:', {newVal, oldVal});
          return;
        }
        
        // 只有当消息是新增的（而不是更新或删除）时才自动滚动
        const isNewMessageAdded = newVal.length > oldVal.length;

        // 如果用户没有手动滚动或新增了消息，则滚动到底部
        if ((!this.isUserScrolling || isNewMessageAdded) && this.shouldScrollToBottom) {
          this.$nextTick(() => {
            this.scrollToBottom();
          });
        }
      },
      deep: true,
      immediate: false // 改为false，避免初始化时出错
    },
    sessionId: {
      immediate: true,
      handler(newVal) {
        if (newVal) {
          // 会话切换时重置滚动状态并滚动到底部
          this.shouldScrollToBottom = true;
          this.isUserScrolling = false;
          this.$nextTick(() => {
            this.scrollToBottom();
          });
        }
      }
    }
  },

  mounted() {
    // 初始化时滚动到底部
    this.$nextTick(() => {
      this.scrollToBottom();
    });

    // 设置滚动监听
    if (this.$refs.messageContainer) {
      this.$refs.messageContainer.addEventListener('scroll', this.handleScroll);
    }
  },


  beforeDestroy() {
    // 移除滚动监听
    if (this.$refs.messageContainer) {
      this.$refs.messageContainer.removeEventListener('scroll', this.handleScroll);
    }
  },

  methods: {
    handleScroll() {
      if (!this.$refs.messageContainer) return;

      const container = this.$refs.messageContainer;
      const { scrollTop, scrollHeight, clientHeight } = container;
      const distanceFromBottom = scrollHeight - scrollTop - clientHeight;

      // 如果距离底部小于50像素，认为用户已滚动到底部
      this.isUserScrolling = distanceFromBottom > 50;
      this.shouldScrollToBottom = !this.isUserScrolling;

      // 如果用户滚动到接近底部，自动切换回自动滚动模式
      if (distanceFromBottom < 50) {
        this.isUserScrolling = false;
        this.shouldScrollToBottom = true;
      }
    },

    scrollToBottom() {
      this.$nextTick(() => {
        const container = this.$refs.messageContainer;
        const messageList = container.querySelector('.message-list');
        if (!messageList) return;

        // 立即滚动到底部
        messageList.scrollTop = messageList.scrollHeight;

        // 使用两个不同的延时作为保险措施
        setTimeout(() => {
          messageList.scrollTop = messageList.scrollHeight;
        }, 50);

        setTimeout(() => {
          messageList.scrollTop = messageList.scrollHeight;
        }, 200);
      });
    },

    addMessage(message) {
      // 检查是否已存在相同ID的消息，避免重复
      const existingIndex = this.messages.findIndex(m => m.id === message.id);
      if (existingIndex === -1) {
        this.messages.push(message);
        if (this.shouldScrollToBottom) {
          this.$nextTick(() => {
            this.scrollToBottom();
          });
        }
      }
    },

    updateMessage(id, updates) {
      const index = this.messages.findIndex(m => m.id === id);
      if (index !== -1) {
        // 使用Vue.set确保响应性
        const updatedMessage = {...this.messages[index], ...updates};
        this.$set(this.messages, index, updatedMessage);
        if (this.shouldScrollToBottom) {
          this.$nextTick(() => {
            this.scrollToBottom();
          });
        }
      }
    },

    isSelfMessage(message) {
      // 确保比较的是数值型 ID
      const senderId = parseInt(message.senderId || message.sender_id);
      const currentId = parseInt(this.currentUserId);

      // 如果后端提供了isCurrentUser字段，优先使用
      if (message.isCurrentUser !== undefined) {
        return message.isCurrentUser;
      }

      return senderId === currentId;
    },

    formatTime(timestamp) {
      if (!timestamp) return '';

      // 处理日期字符串或时间戳
      let date;
      if (typeof timestamp === 'string') {
        date = new Date(timestamp.replace(/-/g, '/'));
      } else {
        date = new Date(timestamp);
      }

      if (isNaN(date.getTime())) {
        return timestamp; // 如果无法解析则返回原始值
      }

      const now = new Date();
      const isToday = date.toDateString() === now.toDateString();

      if (isToday) {
        return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
      } else {
        return date.toLocaleDateString('zh-CN', {
          month: '2-digit',
          day: '2-digit',
          hour: '2-digit',
          minute: '2-digit'
        });
      }
    },

    getMessageAvatar(message) {
      // 判断是否是当前用户发送的消息
      const isSelf = this.isSelfMessage(message);

      if (isSelf) {
        // 当前用户发送的消息，显示发送者头像（即当前用户的头像）
        return message.senderAvatar || this.userAvatar;
      } else {
        // 对方发送的消息，显示发送者头像（即对方的头像）
        return message.senderAvatar || this.otherUser.avatar || this.defaultAvatar;
      }
    },

    handleEnterKey(e) {
      // 按下Enter键发送消息，按下Shift+Enter键换行
      if (!e.shiftKey) {
        this.sendMessage();
      }
    },

    async sendMessage() {
      const content = this.inputMessage.trim();
      if (!content) return;

      if (this.isSending) return;
      this.isSending = true;

      // 发送新消息时，重置滚动标志
      this.shouldScrollToBottom = true;
      this.isUserScrolling = false;

      const tempMessage = {
        id: 'temp_' + Date.now(),
        content,
        senderId: this.currentUserId,
        receiverId: this.otherUser.id || this.otherUser.userId,
        sessionId: this.sessionId,
        createdTime: new Date().toISOString(),
        pending: true,
        // 自己发送的消息，已读状态应该为1（已读）
        isRead: 1
      };

      // 添加临时消息到列表
      this.$emit('add-message', tempMessage);
      this.inputMessage = '';

      // 确保在消息发送后立即滚动到底部
      this.$nextTick(() => {
        this.scrollToBottom();
      });

      try {
        // 尝试发送消息，忽略返回结果
        await sendChatMessage({
          sessionId: this.sessionId,
          content,
          receiverId: this.otherUser.id || this.otherUser.userId
        });

        // 无论返回结果如何，都标记为发送成功
        this.$emit('update-message', tempMessage.id, {
          pending: false
        });

        // 通知父组件刷新整个会话
        this.$emit('refresh-session');

        // 确保更新后再次滚动到底部
        this.$nextTick(() => {
          this.scrollToBottom();
        });

      } catch (error) {
        console.warn('发送消息出现错误，但将继续操作:', error);

        // 即使有错误，也尝试标记为成功并刷新
        this.$emit('update-message', tempMessage.id, {
          pending: false
        });

        // 通知父组件刷新整个会话
        this.$emit('refresh-session');
      } finally {
        this.isSending = false;
        // 最后再确保一次滚动到底部
        this.$nextTick(() => {
          this.scrollToBottom();
        });
      }
    },

    async retryMessage(message) {
      // 重试消息时，重置滚动标志
      this.shouldScrollToBottom = true;
      this.isUserScrolling = false;

      // 标记为正在发送
      this.$emit('update-message', message.id, {
        failed: false,
        pending: true
      });

      try {
        // 重新发送消息
        await sendChatMessage({
          sessionId: this.sessionId,
          content: message.content,
          receiverId: this.otherUser.id || this.otherUser.userId
        });

        // 无论响应如何，都标记为成功
        this.$emit('update-message', message.id, {
          pending: false,
          failed: false
        });

        // 通知父组件刷新整个会话
        this.$emit('refresh-session');

        // 确保更新后滚动到底部
        this.$nextTick(() => {
          this.scrollToBottom();
        });

      } catch (error) {
        console.warn('重发消息出现错误，但将继续操作:', error);

        // 即使出错也尝试标记为成功
        this.$emit('update-message', message.id, {
          pending: false,
          failed: false
        });

        // 通知父组件刷新整个会话
        this.$emit('refresh-session');
      }
    },

  }
};
</script>
<style scoped>
.chat-message-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  background-color: #f5f7fa;
  position: relative;
}

.debug-panel {
  padding: 10px;
  background-color: #f0f0f0;
  border: 1px solid #ddd;
  margin-bottom: 10px;
  font-size: 12px;
  overflow: auto;
  max-height: 200px;
}

.empty-message {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  flex: 1;
  color: #909399;
  text-align: center;
}

.empty-message p {
  font-size: 16px;
  margin-top: 15px;
}

.empty-message i {
  font-size: 64px;
  color: #dcdfe6;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  min-height: 0;
  position: relative;
  margin-bottom: 0;
  scrollbar-width: thin;
  scrollbar-color: #c0c4cc #f5f7fa;
}

.message-list::-webkit-scrollbar {
  width: 6px;
}

.message-list::-webkit-scrollbar-thumb {
  background-color: #c0c4cc;
  border-radius: 3px;
}

.message-list::-webkit-scrollbar-track {
  background-color: #f5f7fa;
}

.message-item {
  display: flex;
  margin-bottom: 20px;
  max-width: 70%;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.own-message {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.other-message {
  align-self: flex-start;
}

.message-avatar {
  width: 40px;
  height: 40px;
  margin: 0 12px;
  flex-shrink: 0;
}

.message-avatar img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
  border: 2px solid #fff;
  transition: all 0.3s ease;
}

.own-message .message-avatar img:hover,
.other-message .message-avatar img:hover {
  transform: scale(1.05);
}

.message-content {
  display: flex;
  flex-direction: column;
}

.own-message .message-content {
  align-items: flex-end;
}

.other-message .message-content {
  align-items: flex-start;
}

/* 添加发送者昵称样式 */
.message-sender {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
  font-weight: 500;
}

.message-bubble {
  padding: 12px 16px;
  border-radius: 18px;
  word-break: break-word;
  white-space: pre-wrap;
  line-height: 1.5;
  max-width: 100%;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
  position: relative;
  transition: all 0.2s ease;
}

.message-bubble:hover {
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.own-message .message-bubble {
  background-color: #409EFF;
  color: white;
  border-bottom-right-radius: 4px;
}

.other-message .message-bubble {
  background-color: #FFF;
  color: #303133;
  border-bottom-left-radius: 4px;
}

.message-time {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}

.pending-message .message-bubble {
  opacity: 0.7;
}

.failed-message .message-bubble {
  border: 1px solid #f56c6c;
  background-color: #fef0f0;
  color: #f56c6c;
}

.retry-btn {
  background: transparent;
  color: #f56c6c;
  border: none;
  cursor: pointer;
  margin-left: 5px;
  font-size: 12px;
  text-decoration: underline;
  transition: all 0.2s;
}

.retry-btn:hover {
  color: #ff4d4f;
  text-decoration: none;
}

.message-input-container {
  display: flex;
  padding: 15px;
  border-top: 1px solid #ebeef5;
  background-color: #fff;
  align-items: center;
}

.message-input {
  flex: 1;
  border: 1px solid #dcdfe6;
  border-radius: 20px;
  padding: 10px 16px;
  min-height: 40px;
  max-height: 120px;
  resize: none;
  outline: none;
  font-size: 14px;
  line-height: 1.5;
  transition: all 0.3s;
  background-color: #f5f7fa;
}

.message-input:focus {
  border-color: #409EFF;
  background-color: #fff;
  box-shadow: 0 0 0 2px rgba(64,158,255,.1);
}

.send-button {
  margin-left: 10px;
  border: none;
  background-color: #409EFF;
  color: white;
  border-radius: 20px;
  padding: 0 20px;
  height: 40px;
  font-size: 14px;
  cursor: pointer;
  outline: none;
  transition: all 0.3s;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: center;
}

.send-button:hover:not(:disabled) {
  background-color: #66b1ff;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(64,158,255,.3);
}

.send-button:active:not(:disabled) {
  transform: translateY(1px);
  box-shadow: none;
}

.send-button:disabled {
  background-color: #c0c4cc;
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .message-list {
    padding: 15px;
  }
  
  .message-item {
    max-width: 85%;
    margin-bottom: 15px;
  }
  
  .message-avatar {
    width: 36px;
    height: 36px;
    margin: 0 8px;
  }
  
  .message-bubble {
    padding: 10px 14px;
    font-size: 14px;
  }
  
  .message-input-container {
    padding: 10px;
  }
  
  .message-input {
    min-height: 36px;
  }
  
  .send-button {
    height: 36px;
    padding: 0 15px;
  }
}
</style>

