package site.hnfy258.VO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class BlogUserLoginVo {
    private String token;
    private UserInfoVo userInfo;
}
