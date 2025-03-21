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
              v-if="session.unreadCount > 0"
              class="unread-badge"
            >
              {{ session.unreadCount > 99 ? '99+' : session.unreadCount }}
            </span>
          </div>
          <div class="session-content">
            <div class="session-name">{{ session.nickname || '用户 ' + session.userId }}</div>
            <div class="session-preview">
              {{ session.lastMessage || '暂无消息' }}
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
      sessionResponse: { code: 0, data: [], msg: '' }
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
      return this.sessionResponse.data;
    },
    filteredSessions() {
      console.log('过滤会话，当前会话列表:', this.sessions);
      if (!this.searchQuery) {
        return this.sessions;
      }
      return this.sessions.filter(session => {
        return (session.lastMessage || '').toLowerCase().includes(this.searchQuery.toLowerCase());
      });
    }
  },
  watch: {
    selectedId(newVal) {
      if (newVal) {
        this.selectedSessionId = newVal;
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
  },
  methods: {
    selectSession(sessionId) {
      this.selectedSessionId = sessionId;
      this.$emit('select-session', sessionId);
    },
    getAvatarUrl(session) {
      return session.avatar || this.defaultAvatar;
    },
    updateSessions(data) {
      console.log('更新会话列表', data);
      this.sessionResponse.data = data;
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
  border-right: 1px solid #e0e0e0;
  background-color: #f8f9fa;
}

.session-header {
  padding: 15px;
  border-bottom: 1px solid #e0e0e0;
  background-color: #ffffff;
}

.session-header h3 {
  margin: 0;
  font-size: 18px;
  color: #333;
}

.session-search {
  padding: 10px 15px;
  background-color: #ffffff;
  border-bottom: 1px solid #e0e0e0;
}

.session-search input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 20px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.3s;
}

.session-search input:focus {
  border-color: #1890ff;
}

.session-body {
  flex: 1;
  overflow-y: auto;
  padding: 0;
}

.no-sessions {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: #999;
}

.loading-sessions,
.error-message {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: #999;
}

.error-message {
  color: #f56c6c;
}

.icon-message {
  font-size: 40px;
  margin-bottom: 10px;
}

.session-item {
  display: flex;
  align-items: center;
  padding: 12px 15px;
  cursor: pointer;
  transition: background-color 0.2s;
  border-bottom: 1px solid #f0f0f0;
}

.session-item:hover {
  background-color: #f0f2f5;
}

.session-item.active {
  background-color: #e6f7ff;
  border-right: 3px solid #1890ff;
}

.avatar-container {
  position: relative;
  margin-right: 12px;
}

.avatar {
  width: 45px;
  height: 45px;
  border-radius: 50%;
  object-fit: cover;
  background-color: #eee;
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
}

.session-content {
  flex: 1;
  overflow: hidden;
}

.session-name {
  font-size: 15px;
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-preview {
  font-size: 13px;
  color: #999;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
