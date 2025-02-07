package site.hnfy258.VO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.sf.jsqlparser.expression.DateTimeLiteralExpression;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class ArticleDetailVo {
    private Long categoryId;
    private String categoryName;
    private String content;
    private DateTimeLiteralExpression.DateTime createTime;
    private Long id;
    private Long isComment;
    private String title;
    private Long viewCount;
}
