package site.hnfy258.VO;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class MessagesVo {
    private Long sessionId;
    private Long userId;
    private String avatar;
    private String lastMessage;
    private  Integer unreadCount;
    private String nickname;

}
