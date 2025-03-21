package site.hnfy258.entity;

import java.util.Date;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
/**
 * (Messages)表实体类
 *
 * @author makejava
 * @since 2025-02-26 21:42:46
 */
@SuppressWarnings("serial")
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("private_messages")
public class Messages  {
@TableId
    private Long id;


    private Long senderId;

    private Long receiverId;

    private String content;

    private Integer isRead;

    private Date createdTime;

    private Long sessionId;



}
