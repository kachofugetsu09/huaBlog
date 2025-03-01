package site.hnfy258.VO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class CategoryVo {
    private Long id;
    private String name;
    //描述
    private String description;
}
