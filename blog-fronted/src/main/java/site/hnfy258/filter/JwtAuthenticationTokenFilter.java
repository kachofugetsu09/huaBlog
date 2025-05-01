package site.hnfy258.filter;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import site.hnfy258.domain.ResponseResult;
import site.hnfy258.entity.LoginUser;
import site.hnfy258.enums.AppHttpCodeEnum;
import site.hnfy258.utils.JwtUtil;
import site.hnfy258.utils.RedisCache;
import site.hnfy258.utils.WebUtils;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;

@Component
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationTokenFilter.class);

    @Autowired
    private RedisCache redisCache;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        //获取请求头中的token
        String token = request.getHeader("token");
        log.debug("JWT过滤器 - 请求路径: {}, token: {}", request.getRequestURI(), token);
        
        if(!StringUtils.hasText(token)){
            //说明该接口不需要登录  直接放行
            log.debug("JWT过滤器 - 无token，直接放行: {}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }
        //解析获取userid
        Claims claims = null;
        try {
            claims = JwtUtil.parseJWT(token);
            log.debug("JWT过滤器 - token解析成功，userId: {}", claims.getSubject());
        } catch (Exception e) {
            log.error("JWT过滤器 - token解析失败: {}", e.getMessage());
            //token超时  token非法
            //响应告诉前端需要重新登录
            ResponseResult result = ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
            WebUtils.renderString(response, JSONObject.toJSONString(result));
            return;
        }
        String userId = claims.getSubject();
        //从redis中获取用户信息
        String redisKey = "bloglogin:" + userId;
        log.debug("JWT过滤器 - 尝试从Redis获取用户信息，key: {}", redisKey);
        
        try {
            LoginUser loginUser = redisCache.getCacheObject(redisKey);
            //如果获取不到
            if(Objects.isNull(loginUser)){
                //说明登录过期  提示重新登录
                log.warn("JWT过滤器 - Redis中未找到用户信息: {}", redisKey);
                ResponseResult result = ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
                WebUtils.renderString(response, JSON.toJSONString(result));
                return;
            }
            log.debug("JWT过滤器 - 认证成功，用户ID: {}", userId);
            //存入SecurityContextHolder
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(loginUser,null,null);
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        } catch (Exception e) {
            log.error("JWT过滤器 - 从Redis获取用户信息失败: {}", e.getMessage());
            ResponseResult result = ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
            WebUtils.renderString(response, JSON.toJSONString(result));
            return;
        }

        filterChain.doFilter(request, response);
    }
}
