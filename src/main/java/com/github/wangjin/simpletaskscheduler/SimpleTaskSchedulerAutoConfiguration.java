package com.github.wangjin.simpletaskscheduler;

import com.github.wangjin.simpletaskscheduler.listener.TaskSchedulerListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * @author Jin Wang
 * @version 1.0
 * @date 2019-11-09 10:34 下午
 */
@Configuration
public class SimpleTaskSchedulerAutoConfiguration {

    @ConditionalOnProperty(prefix = "simple.task.scheduler", value = "enabled", havingValue = "true")
    @ConditionalOnBean(StringRedisTemplate.class)
    TaskSchedulerListener taskSchedulerListener(ApplicationContext applicationContext, StringRedisTemplate stringRedisTemplate) {
        return new TaskSchedulerListener(applicationContext, stringRedisTemplate);
    }
}
