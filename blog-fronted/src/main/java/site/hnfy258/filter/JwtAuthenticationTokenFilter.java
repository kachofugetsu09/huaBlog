package site.hnfy258.filter;

import com.alibaba.fastjson.JSON;
import io.jsonwebtoken.Claims;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import site.hnfy258.domain.ResponseResult;
import site.hnfy258.entity.LoginUser;
import site.hnfy258.enums.AppHttpCodeEnum;
import site.hnfy258.utils.JwtUtil;
import site.hnfy258.utils.RedisCache;
import site.hnfy258.utils.WebUtils;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;

@Component

public class JwtAuthenticationTokenFilter  extends OncePerRequestFilter {

    private final RedisCache redisCache;

    public JwtAuthenticationTokenFilter(RedisCache redisCache) {
        this.redisCache = redisCache;
    }

    /**
     * @param httpServletRequest
     * @param httpServletResponse
     * @param filterChain
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest,
                                    HttpServletResponse httpServletResponse,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        //获取请求头中的token
        String token = httpServletRequest.getHeader("token");
        if(token == null){
            filterChain.doFilter(httpServletRequest, httpServletResponse);
            return;
        }
        //解析获取userid
        Claims claims = null;
        if (token != null) {
            try {
                claims = JwtUtil.parseJWT(token);
            } catch (Exception e) {
                e.printStackTrace();
                ResponseResult result = ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
                WebUtils.renderString(httpServletResponse, JSON.toJSONString(result));
                return;
            }
        }
        String userId = claims.getSubject();
        //从redis中获得用户信息
        LoginUser loginUser =  redisCache.getCacheObject("bloglogin" + userId);
        if(Objects.isNull(loginUser)){
            ResponseResult result = ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
            WebUtils.renderString(httpServletResponse, JSON.toJSONString(result));
            return;
        }
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(loginUser, null, null));
        //存入securityContextHolder
        filterChain.doFilter(httpServletRequest, httpServletResponse);
    }
}
