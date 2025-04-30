package site.hnfy258.filter;

import com.alibaba.fastjson.JSON;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import site.hnfy258.domain.ResponseResult;
import site.hnfy258.enums.AppHttpCodeEnum;
import site.hnfy258.utils.JwtUtil;
import site.hnfy258.utils.WebUtils;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE) // 确保这个过滤器最先执行
public class RateLimitFilter extends OncePerRequestFilter {

    @Autowired
    private StringRedisTemplate redisTemplate;
    
    private final DefaultRedisScript<Long> rateLimitScript;
    
    // IP限流参数
    private static final int IP_RATE_LIMIT_MAX = 60; // 每分钟最大请求数
    private static final int IP_RATE_LIMIT_WINDOW = 60; // 时间窗口(秒)
    private static final int IP_BLOCK_TIME = 1800; // IP封禁时间(秒)，30分钟
    
    // 用户限流参数（针对敏感操作如评论）
    private static final int USER_SENSITIVE_RATE_LIMIT = 10; // 敏感操作每分钟最大请求数
    private static final int USER_SENSITIVE_WINDOW = 60; // 时间窗口(秒)
    private static final int USER_BLOCK_TIME = 3600; // 用户封禁时间(秒)，1小时
    
    // 敏感操作路径列表
    private static final Set<String> SENSITIVE_PATHS = new HashSet<>(Arrays.asList(
        "/comment/addComment"
    ));
    
    // IP白名单
    private static final Set<String> IP_WHITELIST = new HashSet<>(Arrays.asList(
        "127.0.0.1",
        "localhost",
        "0:0:0:0:0:0:0:1"
        // 添加你的管理员IP
    ));
    
    public RateLimitFilter() {
        rateLimitScript = new DefaultRedisScript<>();
        rateLimitScript.setScriptText(getLuaScript());
        rateLimitScript.setResultType(Long.class);
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // 获取客户端IP
        String clientIp = getClientIp(request);
        
        // 检查IP是否在白名单中
        if (IP_WHITELIST.contains(clientIp)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // 获取请求路径
        String requestPath = request.getRequestURI();
        
        // 1. 先进行IP限流检查
        Long ipResult = redisTemplate.execute(
            rateLimitScript,
            Collections.singletonList("ip_rate_limit:" + clientIp),
            String.valueOf(IP_RATE_LIMIT_MAX),
            String.valueOf(IP_RATE_LIMIT_WINDOW),
            String.valueOf(IP_BLOCK_TIME)
        );
        
        // 如果IP被限流，直接返回
        if (ipResult != null && ipResult == 0) {
            ResponseResult result429 = ResponseResult.errorResult(AppHttpCodeEnum.SYSTEM_ERROR, "请求过于频繁，请稍后再试");
            WebUtils.renderString(response, JSON.toJSONString(result429));
            return;
        }
        
        // 2. 对敏感操作进行用户级别限流
        if (SENSITIVE_PATHS.contains(requestPath)) {
            // 获取token
            String token = request.getHeader("token");
            if (StringUtils.hasText(token)) {
                try {
                    // 解析token获取用户ID
                    Claims claims = JwtUtil.parseJWT(token);
                    String userId = claims.getSubject();
                    
                    // 执行用户级别限流
                    Long userResult = redisTemplate.execute(
                        rateLimitScript,
                        Collections.singletonList("user_rate_limit:" + userId + ":" + requestPath),
                        String.valueOf(USER_SENSITIVE_RATE_LIMIT),
                        String.valueOf(USER_SENSITIVE_WINDOW),
                        String.valueOf(USER_BLOCK_TIME)
                    );
                    
                    // 如果用户被限流，直接返回
                    if (userResult != null && userResult == 0) {
                        ResponseResult result429 = ResponseResult.errorResult(AppHttpCodeEnum.SYSTEM_ERROR, "操作过于频繁，请稍后再试");
                        WebUtils.renderString(response, JSON.toJSONString(result429));
                        return;
                    }
                } catch (Exception e) {

                }
            }
        }
        
        // 通过所有限流检查，继续过滤器链
        filterChain.doFilter(request, response);
    }
    
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个代理的情况，第一个IP为客户端真实IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
    
    private String getLuaScript() {
        // Lua脚本实现限流逻辑
        return "local key = KEYS[1]\n" +
               "local max = tonumber(ARGV[1])\n" +
               "local window = tonumber(ARGV[2])\n" +
               "local blockTime = tonumber(ARGV[3])\n" +
               "\n" +
               "-- 检查是否被封禁\n" +
               "local isBlocked = redis.call('get', key..':blocked')\n" +
               "if isBlocked then\n" +
               "    return 0\n" +
               "end\n" +
               "\n" +
               "-- 获取当前计数\n" +
               "local current = redis.call('get', key)\n" +
               "if current then\n" +
               "    current = tonumber(current)\n" +
               "    if current >= max then\n" +
               "        -- 超过限制，封禁\n" +
               "        redis.call('set', key..':blocked', 1, 'EX', blockTime)\n" +
               "        return 0\n" +
               "    else\n" +
               "        -- 增加计数\n" +
               "        redis.call('incr', key)\n" +
               "        return current + 1\n" +
               "    end\n" +
               "else\n" +
               "    -- 第一次访问，设置计数为1，并设置过期时间\n" +
               "    redis.call('setex', key, window, 1)\n" +
               "    return 1\n" +
               "end";
    }
}
