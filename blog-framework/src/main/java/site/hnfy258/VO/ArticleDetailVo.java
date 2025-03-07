package site.hnfy258.VO;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.sf.jsqlparser.expression.DateTimeLiteralExpression;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class ArticleDetailVo {
    /**
     * 文章标题
     */
    private Long id;

    /**
     * 文章分类id
     */
    private Long categoryId;

    /**
     * 文章分类
     */
    private String categoryName;

    /**
     * 文章内容
     */
    private String content;

    /**
     * 文章缩略图
     */
    private String thumbnail;

    /**
     * 文章摘要
     */
    private String summary;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private Date createTime;

    /**
     * 文章标题
     */
    private String title;

    /**
     * 浏览量
     */
    private Long viewCount;
}
