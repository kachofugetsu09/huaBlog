package site.hnfy258.service;

import site.hnfy258.VO.PageVo;

public interface NotificationService {
    public Long getUnreadCount(Long userId);
    public PageVo getNotifications(Long userId, Integer pageNum, Integer pageSize);
    public boolean markAsReadAndDelete(Long userId,Long notificationId);
    public void markAllAsReadAndDelete(Long userId);
}
