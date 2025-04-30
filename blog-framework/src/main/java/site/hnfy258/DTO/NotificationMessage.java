package site.hnfy258.DTO;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class NotificationMessage implements Serializable {
    //评论ID
    private Long commentId;
    //评论类型
    private String commentType;

    //评论人ID
    private Long fromUserId;
    //评论人头像
    private String fromUserAvatar;
    //评论人昵称
    private String fromUserNickName;

    //接收者ID
    private Long toUserId;

    //评论文章ID
    private Long articleId;
    //评论时间
    private Date createTime;

}
