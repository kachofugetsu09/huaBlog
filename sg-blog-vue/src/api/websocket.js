let socket = null;
let isConnected = false;
let reconnectAttempts = 0;
let heartbeatInterval;
let reconnectTimeout;
const MAX_RECONNECT_ATTEMPTS = 5;
const RECONNECT_DELAY = 3000;
const HEARTBEAT_INTERVAL = 15000; // 心跳间隔15秒

// 创建一个自定义事件以在接收到消息时分发
const createMessageEvent = (data) => {
  try {
    // 尝试解析JSON数据
    const jsonData = JSON.parse(data);
    return new CustomEvent('websocket-message', {
      detail: jsonData
    });
  } catch (e) {
    console.error('解析WebSocket消息失败:', e);
    // 如果解析失败，仍然分发原始数据
    return new CustomEvent('websocket-message', {
      detail: data
    });
  }
};

// 启动心跳
function startHeartbeat() {
  stopHeartbeat(); // 确保清除之前的心跳
  heartbeatInterval = setInterval(() => {
    if (socket && socket.readyState === WebSocket.OPEN) {
      socket.send('ping');
      console.log('发送心跳消息');
    }
  }, HEARTBEAT_INTERVAL);
}

// 停止心跳
function stopHeartbeat() {
  if (heartbeatInterval) {
    clearInterval(heartbeatInterval);
    heartbeatInterval = null;
  }
}

// 重连逻辑
function reconnect(userId) {
  if (reconnectTimeout) {
    clearTimeout(reconnectTimeout);
  }

  if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
    const delay = RECONNECT_DELAY * Math.pow(1.5, reconnectAttempts);
    console.log(`尝试重新连接... 尝试次数: ${reconnectAttempts + 1}, 延迟: ${delay}ms`);

    reconnectTimeout = setTimeout(() => {
      if (!isConnected) {
        reconnectAttempts++;
        connectWebSocket(userId);
      }
    }, delay);
  } else {
    console.warn('已达到最大重连次数，WebSocket连接失败');
    window.dispatchEvent(new CustomEvent('websocket-reconnect-failed'));
  }
}

export function connectWebSocket(userId) {
  // 清除先前的重连尝试
  reconnectAttempts = 0;

  // 如果已经存在连接，先关闭
  if (socket) {
    socket.close();
  }

  // 使用正确的协议连接WebSocket
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  const wsUrl = `${protocol}//49.232.191.22:7777/websocket/${userId}`;

  try {
    socket = new WebSocket(wsUrl);

    // 连接建立时的处理
    socket.onopen = (event) => {
      console.log('WebSocket连接已建立', event);
      isConnected = true;
      reconnectAttempts = 0; // 成功连接后重置重连计数
      startHeartbeat(); // 启动心跳

      // 分发连接成功事件
      window.dispatchEvent(new CustomEvent('websocket-connected'));
    };

    // 接收消息的处理
    socket.onmessage = (event) => {
      console.log('收到WebSocket原始消息:', event.data);

      // 处理心跳消息
      if (event.data === 'ping') {
        socket.send('pong');
        console.log('收到心跳请求，已回复pong');
      } else if (event.data === 'pong') {
        console.log('收到心跳响应');
      } else {
        try {
          // 分发消息事件，让组件可以接收到
          window.dispatchEvent(createMessageEvent(event.data));
        } catch (e) {
          console.error('处理WebSocket消息时发生错误:', e);
        }
      }
    };

    // 连接关闭时的处理
    socket.onclose = (event) => {
      console.log('WebSocket连接已关闭', event);
      isConnected = false;
      stopHeartbeat(); // 停止心跳

      // 分发连接关闭事件
      window.dispatchEvent(new CustomEvent('websocket-disconnected'));

      // 尝试重连
      reconnect(userId);
    };

    // 发生错误时的处理
    socket.onerror = (error) => {
      console.error('WebSocket发生错误:', error);
      isConnected = false;

      // 分发错误事件
      window.dispatchEvent(new CustomEvent('websocket-error', { detail: error }));
    };

    return socket;
  } catch (error) {
    console.error('创建WebSocket连接时发生错误:', error);
    isConnected = false;
    return null;
  }
}

export function sendWebSocketMessage(message) {
  if (!socket || socket.readyState !== WebSocket.OPEN) {
    console.warn('WebSocket未连接，无法发送消息');
    return false;
  }

  try {
    // 确保message是字符串
    const messageStr = typeof message === 'string'
      ? message
      : JSON.stringify(message);

    socket.send(messageStr);
    return true;
  } catch (error) {
    console.error('发送WebSocket消息时出错:', error);
    return false;
  }
}

export function getWebSocketStatus() {
  if (!socket) {
    return { connected: false, state: 'CLOSED', stateCode: -1 };
  }

  const states = ['CONNECTING', 'OPEN', 'CLOSING', 'CLOSED'];
  return {
    connected: socket.readyState === WebSocket.OPEN,
    state: states[socket.readyState],
    stateCode: socket.readyState
  };
}

export function isWebSocketConnected() {
  return socket && socket.readyState === WebSocket.OPEN;
}

export function closeWebSocket() {
  stopHeartbeat(); // 停止心跳
  if (socket) {
    socket.close();
    socket = null;
    isConnected = false;
    console.log('WebSocket连接已手动关闭');
  }
}
