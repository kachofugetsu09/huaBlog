package site.hnfy258.entity;


import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
/**
 * (Sessions)表实体类
 *
 * @author makejava
 * @since 2025-02-26 21:08:29
 */
@SuppressWarnings("serial")
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("sessions")
public class Sessions  {
@TableId
    private Long id;


    private Long user1Id;

    private Long user2Id;



}
