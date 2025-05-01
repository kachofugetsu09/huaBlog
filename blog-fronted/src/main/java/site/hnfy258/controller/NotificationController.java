package site.hnfy258.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import site.hnfy258.VO.PageVo;
import site.hnfy258.domain.ResponseResult;
import site.hnfy258.service.NotificationService;

@RestController
@RequestMapping("/notifications")
public class NotificationController {
    @Autowired
    private NotificationService notificationService;

    @GetMapping("/unread-count")
    public ResponseResult<Long> getUnreadCount(Long userId) {
        Long unreadCount = notificationService.getUnreadCount(userId);
        return ResponseResult.okResult(unreadCount);
    }

    @GetMapping
    public ResponseResult<PageVo> getNotifications(@RequestParam Long userId,
                                                   @RequestParam(defaultValue = "1")Integer pageNum,
                                                   @RequestParam(defaultValue = "10")Integer pageSize) {
        PageVo notifications = notificationService.getNotifications(userId, pageNum, pageSize);
        return ResponseResult.okResult(notifications);
    }

    @PostMapping("/{notificationId}/read")
    public ResponseResult markAsRead(@RequestParam Long userId,
                                     @PathVariable String notificationId) {
        boolean res = notificationService.markAsReadAndDelete(userId, notificationId);
        return res? ResponseResult.okResult() : ResponseResult.errorResult(500, "标记已读失败");
    }

    @PostMapping("/read-all")
    public ResponseResult markAllAsRead(@RequestParam Long userId) {
        notificationService.markAllAsReadAndDelete(userId);
        return ResponseResult.okResult();
    }
}
