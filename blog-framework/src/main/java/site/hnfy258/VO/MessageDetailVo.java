package site.hnfy258.VO;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import site.hnfy258.entity.Messages;
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class MessageDetailVo extends Messages {
    private String senderAvatar;

    /**
     * Receiver's avatar URL
     */
    private String receiverAvatar;

    /**
     * Flag indicating if the message is from the current user
     */
    private Boolean isCurrentUser;
    private String nickname;
}
