import request from '@/utils/request'

// 获取未读通知数量
export function getUnreadNotificationCount(userId) {
  if (!userId) {
    console.error('userId 参数无效:', userId);
    return Promise.reject(new Error('userId 参数不能为空'));
  }
  return request({
    url: `/notifications/unread-count`,
    method: 'get',
    params: { userId }
  });
}


// 获取通知列表
export function getNotifications(userId, pageNum = 1, pageSize = 10) {
  if (!userId) {
    console.error('userId 参数无效:', userId);
    return Promise.reject(new Error('userId 参数不能为空'));
  }
  console.log('获取通知列表请求参数:', { userId, pageNum, pageSize });
  return request({
    url: '/notifications',
    method: 'get',
    params: {
      userId,
      pageNum,
      pageSize
    }
  }).then(response => {
    console.log('通知列表响应数据:', response);
    return response;
  }).catch(error => {
    console.error('获取通知列表失败:', error);
    throw error;
  });
}

// 标记单条通知为已读
export function markNotificationAsRead(userId, notificationId) {
  if (!userId || !notificationId) {
    return Promise.reject(new Error('参数不能为空'));
  }
  console.log('标记通知已读请求参数:', { userId, notificationId });
  return request({
    url: `/notifications/${notificationId}/read`,
    method: 'post',
    params: { userId }
  }).then(response => {
    console.log('标记通知已读响应:', response);
    // 即使响应为undefined，也视为成功
    return response || { code: 200, msg: "操作成功" };
  }).catch(error => {
    console.error('标记通知已读请求失败:', error);
    throw error;
  });
}

// 标记全部通知为已读
export function markAllNotificationsAsRead(userId) {
  if (!userId) {
    return Promise.reject(new Error('userId 参数不能为空'));
  }
  console.log('标记全部通知已读请求参数:', { userId });
  return request({
    url: '/notifications/read-all',
    method: 'post',
    params: { userId }
  }).then(response => {
    console.log('标记全部通知已读响应:', response);
    // 即使响应为undefined，也视为成功
    return response || { code: 200, msg: "操作成功" };
  }).catch(error => {
    console.error('标记全部通知已读请求失败:', error);
    throw error;
  });
}
