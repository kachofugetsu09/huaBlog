package site.hnfy258.VO;

import lombok.Data;

@Data
public class NotificationVo {
    private String notificationId;
    private Long fromUserId;
    private String fromUserAvatar;
    private String fromUserNickName;
    private Long commentId;
    private String commentType;
    private Long articleId;
    private Long createTime;
    private Boolean read;
}