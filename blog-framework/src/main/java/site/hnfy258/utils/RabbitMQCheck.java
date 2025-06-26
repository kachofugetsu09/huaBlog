package site.hnfy258.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Properties;

/**
 * RabbitMQ连接检查工具
 */
@Slf4j
@Component
public class RabbitMQCheck {
    
    @Autowired
    private ConnectionFactory connectionFactory;
    
    @Autowired
    private RabbitAdmin rabbitAdmin;
    
    /**
     * 在应用启动时检查RabbitMQ连接
     */
    @PostConstruct
    public void checkRabbitMQConnection() {
        try {
            Properties properties = rabbitAdmin.getQueueProperties("article.moderation.queue");
            if (properties != null) {
                log.info("RabbitMQ连接成功，队列article.moderation.queue存在");
            } else {
                log.warn("RabbitMQ连接成功，但队列article.moderation.queue不存在");
            }
            
            Properties commentProperties = rabbitAdmin.getQueueProperties("comment.notification.queue");
            if (commentProperties != null) {
                log.info("RabbitMQ连接成功，队列comment.notification.queue存在");
            } else {
                log.warn("RabbitMQ连接成功，但队列comment.notification.queue不存在");
            }
            
            log.info("RabbitMQ连接信息: {}", connectionFactory.toString());
        } catch (Exception e) {
            log.error("RabbitMQ连接检查失败", e);
            // 不要抛出异常，让应用继续启动
        }
    }
} 