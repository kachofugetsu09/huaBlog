package site.hnfy258.entity;

import java.util.Date;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 评论表(Comment)表实体类
 *
 * @author makejava
 * @since 2025-02-09 04:31:26
 */
@SuppressWarnings("serial")
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("sg_comment")
public class Comment  {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

//评论类型（0代表文章评论，1代表友链评论）
    private String type;
//文章id
    private Long articleId;
//根评论id
    private Long rootId;
//评论内容
    private String content;
//所回复的目标评论的userid
    private Long toCommentUserId;
//回复目标评论id
    private Long toCommentId;

    /**
     * 创建人的用户id
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
    /**
     * 更新人
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
//删除标志（0代表未删除，1代表已删除）
    private Integer delFlag;

}
