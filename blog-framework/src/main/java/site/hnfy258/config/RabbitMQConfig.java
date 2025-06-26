package site.hnfy258.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class RabbitMQConfig {

    // 定义交换机名称
    public static final String ARTICLE_EXCHANGE = "article.exchange";
    public static final String COMMENT_EXCHANGE = "comment.exchange";
    
    // 定义队列名称
    public static final String ARTICLE_MODERATION_QUEUE = "article.moderation.queue";
    public static final String COMMENT_NOTIFICATION_QUEUE = "comment.notification.queue";
    
    // 定义路由键
    public static final String ARTICLE_MODERATION_ROUTING_KEY = "article.moderation";
    public static final String COMMENT_NOTIFICATION_ROUTING_KEY = "comment.notification";

    @Value("${spring.rabbitmq.host:localhost}")
    private String host;
    
    @Value("${spring.rabbitmq.port:5672}")
    private int port;
    
    @Value("${spring.rabbitmq.username:admin}")
    private String username;
    
    @Value("${spring.rabbitmq.password:admin123}")
    private String password;

    // 配置连接工厂
    @Bean
    @Primary
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setHost(host);
        connectionFactory.setPort(port);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        // 开启发布确认
        connectionFactory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        // 开启发布返回
        connectionFactory.setPublisherReturns(true);
        return connectionFactory;
    }

    // 配置RabbitAdmin，用于声明队列、交换机等资源
    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
        // 设置忽略声明异常
        rabbitAdmin.setIgnoreDeclarationExceptions(true);
        // 自动启动
        rabbitAdmin.setAutoStartup(true);
        return rabbitAdmin;
    }

    // 配置消息转换器
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // 配置RabbitTemplate
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        // 设置消息发送确认回调
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                System.out.println("消息发送失败：" + cause);
            }
        });
        // 设置消息发送失败回调
        rabbitTemplate.setReturnsCallback(returned -> {
            System.out.println("消息发送失败，返回路由：" + returned.getRoutingKey() + ", 返回消息：" + returned.getMessage());
        });
        return rabbitTemplate;
    }

    // 文章处理相关配置
    @Bean
    public DirectExchange articleExchange() {
        return new DirectExchange(ARTICLE_EXCHANGE, true, false);
    }

    @Bean
    public Queue articleModerationQueue() {
        return new Queue(ARTICLE_MODERATION_QUEUE, true, false, false);
    }

    @Bean
    public Binding articleModerationBinding() {
        return BindingBuilder
                .bind(articleModerationQueue())
                .to(articleExchange())
                .with(ARTICLE_MODERATION_ROUTING_KEY);
    }

    // 评论通知相关配置
    @Bean
    public DirectExchange commentExchange() {
        return new DirectExchange(COMMENT_EXCHANGE, true, false);
    }

    @Bean
    public Queue commentNotificationQueue() {
        return new Queue(COMMENT_NOTIFICATION_QUEUE, true, false, false);
    }

    @Bean
    public Binding commentNotificationBinding() {
        return BindingBuilder
                .bind(commentNotificationQueue())
                .to(commentExchange())
                .with(COMMENT_NOTIFICATION_ROUTING_KEY);
    }
} 