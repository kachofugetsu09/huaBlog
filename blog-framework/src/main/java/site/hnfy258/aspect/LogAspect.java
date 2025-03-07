package site.hnfy258.aspect;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import site.hnfy258.annotation.SystemLog;

import javax.servlet.http.HttpServletRequest;
import java.util.UUID;

@Component
@Aspect
@Slf4j

public class LogAspect {
    private static final ThreadLocal<String> LOG_ID = new ThreadLocal<>();
    @Pointcut("@annotation(site.hnfy258.annotation.SystemLog)")
    public void pointcut() {

    }
    @Around("pointcut()")
    public Object printLog(ProceedingJoinPoint joinPoint) throws Throwable {
        Object proceed;
        try {
            LOG_ID.set(UUID.randomUUID().toString());
            handleBefore(joinPoint);
            proceed = joinPoint.proceed();
            handleAfter(proceed);
        } finally {

            // 结束后换行
            log.info("=======End=======" + System.lineSeparator());
            LOG_ID.remove();
        }return proceed;
    }

    private void handleAfter(Object proceed) {
        String formattedResponse = JSON.toJSONString(proceed, true);
        // 打印出参
        log.info("Response       : {}",JSON.toJSONString(proceed));

    }

    private void handleBefore(ProceedingJoinPoint joinPoint) {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = requestAttributes.getRequest();
        // 获取被增强方法上的注解对象
        SystemLog systemLog = getSystemLog(joinPoint);

        log.info("=======Start=======");
        // 打印请求 URL
        log.info("URL            : {}", request.getRequestURL());
        // 打印描述信息
        log.info("BusinessName   : {}", systemLog.bussinessName());
        // 打印 Http method
        log.info("HTTP Method    : {}", request.getMethod());
        // 打印调用 controller 的全路径以及执行方法
        log.info("Class Method   : {}.{}",
                joinPoint.getSignature().getDeclaringTypeName(), joinPoint.getSignature().getName());
        // 打印请求的 IP
        log.info("IP             : {}", request.getRemoteHost());

        // 打印请求入参，但跳过 MultipartFile 类型
        Object[] args = joinPoint.getArgs();
        Object[] filteredArgs = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof MultipartFile) {
                // 如果是 MultipartFile，只记录文件名
                filteredArgs[i] = "MultipartFile[file=" + ((MultipartFile) args[i]).getOriginalFilename() + "]";
            } else {
                filteredArgs[i] = args[i];
            }
        }
        log.info("Request Args   : {}", JSON.toJSONString(filteredArgs));
    }
    private SystemLog getSystemLog(ProceedingJoinPoint joinPoint) {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        SystemLog annotation = methodSignature.getMethod().getAnnotation(SystemLog.class);
        return annotation;
    }
}
