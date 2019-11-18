package com.github.wangjin.simpletaskscheduler;

import com.github.wangjin.simpletaskscheduler.listener.TaskSchedulerListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import static com.github.wangjin.simpletaskscheduler.constant.Constants.TASK_SCHEDULER_CHANNEL;

/**
 * @author Jin Wang
 * @version 1.0
 * @date 2019-11-09 10:34 下午
 */
@Configuration
@AutoConfigureAfter({RedisAutoConfiguration.class})
public class SimpleTaskSchedulerAutoConfiguration {

    @Value("${simple.task.scheduler.executorName}")
    private String executorName;

    @Bean
    @ConditionalOnProperty(prefix = "simple.task.scheduler", value = "enabled", havingValue = "true")
    @ConditionalOnBean(StringRedisTemplate.class)
    TaskSchedulerListener taskSchedulerListener(ApplicationContext applicationContext, StringRedisTemplate stringRedisTemplate) {
        return new TaskSchedulerListener(applicationContext, stringRedisTemplate, executorName);
    }

    @Bean
    RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory, TaskSchedulerListener taskSchedulerListener) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(taskSchedulerListener, new ChannelTopic(TASK_SCHEDULER_CHANNEL));
        return container;
    }
}
