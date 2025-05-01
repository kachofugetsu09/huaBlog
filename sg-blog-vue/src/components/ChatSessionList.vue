<template>
  <div class="chat-session-list">
    <div class="session-header">
      <h3>会话列表</h3>
    </div>
    <div class="session-search">
      <input
        type="text"
        placeholder="搜索会话..."
        v-model="searchQuery"
      />
    </div>
    <div class="session-body">
      <div v-if="loading" class="loading-sessions">
        <p>加载中...</p>
      </div>
      <div v-else-if="error" class="error-message">
        <p>{{ error }}</p>
      </div>
      <div v-else-if="filteredSessions.length === 0" class="no-sessions">
        <i class="icon-message"></i>
        <p>暂无会话</p>
      </div>
      <div v-else>
        <div
          v-for="session in filteredSessions"
          :key="session.sessionId"
          class="session-item"
          :class="{ active: session.sessionId === selectedSessionId }"
          @click="selectSession(session.sessionId)"
        >
          <div class="avatar-container">
            <img :src="getAvatarUrl(session)" alt="头像" class="avatar" />
            <span
              v-if="showUnreadBadge(session)"
              class="unread-badge"
            >
              {{ session.unreadCount > 99 ? '99+' : session.unreadCount }}
            </span>
          </div>
          <div class="session-content">
            <div class="session-name">{{ session.nickname || '用户 ' + session.userId }}</div>
            <div class="session-preview">
              {{ getLastMessagePreview(session) }}
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
<script>
export default {
  name: 'ChatSessionList',
  props: {
    currentUserId: {
      type: [Number, String],
      default: ''
    },
    selectedId: {
      type: String,
      default: null
    },
    loading: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      selectedSessionId: this.selectedId,
      searchQuery: '',
      defaultAvatar: '../static/img/touxiang.jpg',
      error: '',
      sessionResponse: { code: 0, data: [], msg: '' },
      localUnreadCounts: {} // 新增：本地存储的未读计数
    };
  },
  computed: {
    sessions() {
      console.log('解析会话响应:', this.sessionResponse);
      if (!this.sessionResponse) {
        this.error = this.sessionResponse
          ? this.sessionResponse.msg
          : '获取会话列表失败';
        return [];
      }
      if (!Array.isArray(this.sessionResponse.data)) {
        console.error('会话数据不是数组:', this.sessionResponse.data);
        this.error = '会话数据格式不正确';
        return [];
      }
      this.error = '';
      
      // 确保每个会话有未读计数字段，并使用本地存储的计数
      const sessionsWithUnreadCount = this.sessionResponse.data.map(session => {
        const sessionId = session.sessionId;
        // 如果本地有存储这个会话的未读数，优先使用本地的
        if (sessionId && this.localUnreadCounts[sessionId] !== undefined) {
          session.unreadCount = this.localUnreadCounts[sessionId];
        } else if (session.unreadCount === undefined) {
          session.unreadCount = 0;
        }
        return session;
      });
      
      // 按最后消息时间排序，最新的在前
      return [...sessionsWithUnreadCount].sort((a, b) => {
        return new Date(b.lastMessageTime || 0) - new Date(a.lastMessageTime || 0);
      });
    },
    filteredSessions() {
      console.log('过滤会话，当前会话列表:', this.sessions);
      if (!this.searchQuery) {
        return this.sessions;
      }
      return this.sessions.filter(session => {
        return (session.lastMessage || '').toLowerCase().includes(this.searchQuery.toLowerCase()) ||
               (session.nickname || '').toLowerCase().includes(this.searchQuery.toLowerCase());
      });
    }
  },
  watch: {
    selectedId(newVal) {
      if (newVal) {
        this.selectedSessionId = newVal;
        // 选中会话时，将其未读计数清零
        this.clearUnreadCount(newVal);
      }
    },
    sessionResponse: {
      handler(newVal) {
        console.log('会话响应已更新:', newVal);
        if (newVal && (newVal.code !== 200 && newVal.code !== 0)) {
          this.error = newVal.msg || '获取会话列表失败';
        } else {
          this.error = '';
        }
      },
      immediate: false,
      deep: true
    }
  },
  mounted() {
    console.log('ChatSessionList mounted, sessions:', this.sessions);
    // 从localStorage加载本地存储的未读计数
    this.loadLocalUnreadCounts();
  },
  methods: {
    // 新方法：判断是否显示未读徽章
    showUnreadBadge(session) {
      const unreadCount = session.unreadCount || 0;
      // 只有数量大于0且不是当前选中的会话才显示
      return unreadCount > 0 && session.sessionId !== this.selectedSessionId;
    },
    
    // 新方法：加载本地存储的未读计数
    loadLocalUnreadCounts() {
      try {
        const savedCounts = localStorage.getItem('chat_unread_counts');
        if (savedCounts) {
          this.localUnreadCounts = JSON.parse(savedCounts);
        }
      } catch (e) {
        console.error('加载本地未读计数失败:', e);
        this.localUnreadCounts = {};
      }
    },
    
    // 新方法：保存本地未读计数到localStorage
    saveLocalUnreadCounts() {
      try {
        localStorage.setItem('chat_unread_counts', JSON.stringify(this.localUnreadCounts));
      } catch (e) {
        console.error('保存本地未读计数失败:', e);
      }
    },
    
    // 新方法：增加会话的未读计数
    increaseUnreadCount(sessionId) {
      if (!sessionId) return;
      
      // 如果不是当前选中的会话，增加未读计数
      if (sessionId !== this.selectedSessionId) {
        const currentCount = this.localUnreadCounts[sessionId] || 0;
        this.localUnreadCounts[sessionId] = currentCount + 1;
        this.saveLocalUnreadCounts();
        
        // 更新会话列表中的未读计数
        this.updateSessionUnreadCount(sessionId, currentCount + 1);
      }
    },
    
    // 新方法：清除会话的未读计数
    clearUnreadCount(sessionId) {
      if (!sessionId) return;
      
      this.localUnreadCounts[sessionId] = 0;
      this.saveLocalUnreadCounts();
      
      // 更新会话列表中的未读计数
      this.updateSessionUnreadCount(sessionId, 0);
    },
    
    // 新方法：更新会话列表中的未读计数
    updateSessionUnreadCount(sessionId, count) {
      if (!this.sessionResponse || !this.sessionResponse.data) return;
      
      const sessionIndex = this.sessionResponse.data.findIndex(s => s.sessionId === sessionId);
      if (sessionIndex !== -1) {
        this.$set(this.sessionResponse.data[sessionIndex], 'unreadCount', count);
        this.$forceUpdate();
      }
    },
    
    selectSession(sessionId) {
      console.log('选择会话:', sessionId);
      this.selectedSessionId = sessionId;
      this.$emit('select-session', sessionId);
      // 确保触发标记已读事件
      this.$emit('mark-as-read', sessionId);
      // 清除未读计数
      this.clearUnreadCount(sessionId);
    },
    getAvatarUrl(session) {
      return session.avatar || this.defaultAvatar;
    },
    updateSessions(data) {
      console.log('更新会话列表', data);
      if (!Array.isArray(data)) {
        console.error('updateSessions接收到无效数据:', data);
        return;
      }

      // 确保所有会话都有必要的字段，并应用本地存储的未读计数
      const normalizedData = data.map(session => {
        if (!session.sessionId) {
          console.warn('会话缺少sessionId:', session);
        }
        
        // 使用本地存储的未读计数，除非是当前选中的会话
        let unreadCount = session.unreadCount || 0;
        if (session.sessionId && this.localUnreadCounts[session.sessionId] !== undefined &&
            session.sessionId !== this.selectedSessionId) {
          unreadCount = this.localUnreadCounts[session.sessionId];
        } else if (session.sessionId === this.selectedSessionId) {
          // 如果是当前选中的会话，未读计数应该为0
          unreadCount = 0;
          this.localUnreadCounts[session.sessionId] = 0;
        }
        
        return {
          ...session,
          unreadCount: unreadCount,
          lastMessage: session.lastMessage || '暂无消息',
          lastMessageTime: session.lastMessageTime || new Date().toISOString()
        };
      });
      
      // 按最新消息时间排序
      const sortedData = [...normalizedData].sort((a, b) => {
        return new Date(b.lastMessageTime || 0) - new Date(a.lastMessageTime || 0);
      });
      
      // 对比每个会话的信息是否有更新
      const currentSessions = this.sessionResponse ? this.sessionResponse.data || [] : [];
      let hasChanges = false;
      
      if (currentSessions.length !== sortedData.length) {
        hasChanges = true;
      } else {
        // 检查每个会话的最后消息是否有变化
        for (let i = 0; i < sortedData.length; i++) {
          const newSession = sortedData[i];
          const oldSession = currentSessions.find(s => s.sessionId === newSession.sessionId);
          
          if (!oldSession || 
              oldSession.lastMessage !== newSession.lastMessage ||
              oldSession.lastMessageTime !== newSession.lastMessageTime ||
              oldSession.unreadCount !== newSession.unreadCount) {
            hasChanges = true;
            break;
          }
        }
      }
      
      // 只有在有变化时才更新数据，减少不必要的渲染
      if (hasChanges) {
        console.log('会话列表有更新，重新渲染');
        this.sessionResponse.data = sortedData;
        // 保存未读计数
        this.saveLocalUnreadCounts();
        // 强制刷新计算属性
        this.$forceUpdate();
      }
    },
    getLastMessagePreview(session) {
      if (!session.lastMessage) return '暂无消息';
      
      // 总是显示最新消息，无论是否是自己发送的
      const isSelf = session.lastMessageSenderId === parseInt(this.currentUserId);
      return (isSelf ? '我: ' : '') + session.lastMessage;
    }
  }
};
</script>
<style scoped>
.chat-session-list {
  width: 280px;
  height: 100%;
  display: flex;
  flex-direction: column;
  border-right: 1px solid #ebeef5;
  background-color: #ffffff;
  box-shadow: 2px 0 8px rgba(0,0,0,0.03);
}

.session-header {
  padding: 16px;
  border-bottom: 1px solid #ebeef5;
  background-color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.session-header h3 {
  margin: 0;
  font-size: 16px;
  color: #303133;
  font-weight: 600;
}

.session-search {
  padding: 12px 16px;
  background-color: #ffffff;
  border-bottom: 1px solid #f0f2f5;
}

.session-search input {
  width: 100%;
  padding: 8px 16px;
  border: 1px solid #e4e7ed;
  border-radius: 20px;
  font-size: 14px;
  outline: none;
  transition: all 0.3s;
  background-color: #f5f7fa;
}

.session-search input:focus {
  border-color: #409EFF;
  background-color: #ffffff;
  box-shadow: 0 0 0 2px rgba(64,158,255,.2);
}

.session-body {
  flex: 1;
  overflow-y: auto;
  padding: 0;
  scrollbar-width: thin;
  scrollbar-color: #c0c4cc #f5f7fa;
}

.session-body::-webkit-scrollbar {
  width: 6px;
}

.session-body::-webkit-scrollbar-thumb {
  background-color: #c0c4cc;
  border-radius: 3px;
}

.session-body::-webkit-scrollbar-track {
  background-color: #f5f7fa;
}

.no-sessions {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: #909399;
  text-align: center;
  padding: 20px;
}

.loading-sessions,
.error-message {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: #909399;
  text-align: center;
}

.error-message {
  color: #f56c6c;
}

.icon-message {
  font-size: 40px;
  margin-bottom: 15px;
  color: #dcdfe6;
}

.session-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
  border-bottom: 1px solid #f5f7fa;
}

.session-item:hover {
  background-color: #f5f7fa;
}

.session-item.active {
  background-color: #ecf5ff;
  border-right: 3px solid #409EFF;
}

.avatar-container {
  position: relative;
  margin-right: 12px;
  flex-shrink: 0;
}

.avatar {
  width: 45px;
  height: 45px;
  border-radius: 50%;
  object-fit: cover;
  background-color: #eee;
  border: 2px solid #ebeef5;
  transition: all 0.3s;
}

.session-item:hover .avatar {
  border-color: #c6e2ff;
}

.session-item.active .avatar {
  border-color: #409EFF;
}

.unread-badge {
  position: absolute;
  top: -5px;
  right: -5px;
  min-width: 18px;
  height: 18px;
  line-height: 18px;
  text-align: center;
  background-color: #f56c6c;
  color: white;
  border-radius: 9px;
  font-size: 12px;
  padding: 0 5px;
  font-weight: bold;
  box-shadow: 0 3px 6px rgba(0,0,0,0.2);
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% {
    transform: scale(1);
    box-shadow: 0 3px 6px rgba(0,0,0,0.2);
  }
  50% {
    transform: scale(1.1);
    box-shadow: 0 4px 8px rgba(0,0,0,0.3);
  }
  100% {
    transform: scale(1);
    box-shadow: 0 3px 6px rgba(0,0,0,0.2);
  }
}

.session-content {
  flex: 1;
  overflow: hidden;
  min-width: 0;
}

.session-name {
  font-size: 15px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-item.active .session-name {
  color: #409EFF;
}

.session-preview {
  font-size: 13px;
  color: #909399;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.3;
}

@media (max-width: 768px) {
  .chat-session-list {
    width: 100%;
    height: auto;
    max-height: 35vh;
    border-right: none;
    border-bottom: 1px solid #ebeef5;
  }
  
  .session-header {
    padding: 12px;
  }
  
  .session-search {
    padding: 8px 12px;
  }
  
  .avatar {
    width: 40px;
    height: 40px;
  }
  
  .session-item {
    padding: 10px 12px;
  }
}
</style>
