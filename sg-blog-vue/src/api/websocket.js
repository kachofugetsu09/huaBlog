// websocket.js - Improved implementation
let socket = null;
let isConnected = false;
let reconnectAttempts = 0;
const MAX_RECONNECT_ATTEMPTS = 5;
const RECONNECT_DELAY = 3000;

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

export function connectWebSocket(userId) {
  // 清除先前的重连尝试
  reconnectAttempts = 0;

  // 如果已经存在连接，先关闭
  if (socket) {
    socket.close();
  }

  // 使用正确的协议连接WebSocket
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  const wsUrl = `${protocol}//localhost:7777/websocket/${userId}`;

  try {
    socket = new WebSocket(wsUrl);

    // 连接建立时的处理
    socket.onopen = (event) => {
      console.log('WebSocket连接已建立', event);
      isConnected = true;
      reconnectAttempts = 0; // 成功连接后重置重连计数

      // 分发连接成功事件
      window.dispatchEvent(new CustomEvent('websocket-connected'));
    };

    // 接收消息的处理
    socket.onmessage = (event) => {
      console.log('收到WebSocket原始消息:', event.data);

      try {
        // 分发消息事件，让组件可以接收到
        window.dispatchEvent(createMessageEvent(event.data));
      } catch (e) {
        console.error('处理WebSocket消息时发生错误:', e);
      }
    };

    // 连接关闭时的处理
    socket.onclose = (event) => {
      console.log('WebSocket连接已关闭', event);
      isConnected = false;

      // 分发连接关闭事件
      window.dispatchEvent(new CustomEvent('websocket-disconnected'));

      // 实现带退避策略的重连逻辑
      if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
        const delay = RECONNECT_DELAY * Math.pow(1.5, reconnectAttempts);
        console.log(`尝试重新连接... 尝试次数: ${reconnectAttempts + 1}, 延迟: ${delay}ms`);

        setTimeout(() => {
          if (!isConnected) {
            reconnectAttempts++;
            connectWebSocket(userId);
          }
        }, delay);
      } else {
        console.warn('已达到最大重连次数，WebSocket连接失败');
        // 分发重连失败事件
        window.dispatchEvent(new CustomEvent('websocket-reconnect-failed'));
      }
    };

    // 发生错误时的处理
    socket.onerror = (error) => {
      console.error('WebSocket发生错误:', error);
      isConnected = false;

      // 分发错误事件
      window.dispatchEvent(new CustomEvent('websocket-error', { detail: error }));
      // 不需要在这里重连，因为onclose会在错误后被触发
    };

    return socket;
  } catch (error) {
    console.error('创建WebSocket连接时发生错误:', error);
    isConnected = false;
    return null;
  }
}

// 通过WebSocket发送消息，带有重试逻辑
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

// 检查WebSocket连接状态，提供更详细的状态信息
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

// 检查WebSocket是否已连接
export function isWebSocketConnected() {
  return socket && socket.readyState === WebSocket.OPEN;
}

// 显式关闭连接
export function closeWebSocket() {
  if (socket) {
    socket.close();
    socket = null;
    isConnected = false;
    console.log('WebSocket连接已手动关闭');
  }
}
