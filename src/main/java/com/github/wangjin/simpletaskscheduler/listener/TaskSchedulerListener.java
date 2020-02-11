package com.github.wangjin.simpletaskscheduler.listener;

/**
 * @author Jin Wang
 * @version 1.0
 * @date 2019-11-07 5:55 下午
 */

import com.alibaba.fastjson.JSON;
import com.github.wangjin.simpletaskscheduler.annotation.TaskHandler;
import com.github.wangjin.simpletaskscheduler.entity.TaskScheduler;
import com.github.wangjin.simpletaskscheduler.handler.ITaskHandler;
import com.github.wangjin.simpletaskscheduler.runnable.TaskRunnable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.github.wangjin.simpletaskscheduler.constant.Constants.THEAD_POOL_POST;
import static org.springframework.util.ObjectUtils.isEmpty;

@Slf4j
public class TaskSchedulerListener implements MessageListener {

    private static final Byte FIXED_DELAY = 2;

    private ConfigurableApplicationContext applicationContext;

    private StringRedisTemplate stringRedisTemplate;

    private String executorName;

    private static final String TASK_PRE = "TASK-";
    private static final String TASK_ACTION_START = "start";
    private static final String TASK_ACTION_STOP = "stop";

    public TaskSchedulerListener(ConfigurableApplicationContext applicationContext, StringRedisTemplate stringRedisTemplate, String executorName) {
        this.applicationContext = applicationContext;
        this.stringRedisTemplate = stringRedisTemplate;
        this.executorName = executorName;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        if (!isEmpty(message.getBody())) {
            //构建taskScheduler对象
            TaskScheduler taskScheduler = JSON.parseObject(new String(message.getBody(), StandardCharsets.UTF_8), TaskScheduler.class);

            // 对应调度线程池名称
            String theadPoolName = taskScheduler.getHandlerName() + THEAD_POOL_POST;

            if (TASK_ACTION_STOP.equalsIgnoreCase(taskScheduler.getAction())) {
                log.debug("关闭线程池[{}]", theadPoolName);
                // 找到并关闭对应线程池
                ThreadPoolTaskScheduler threadPoolTaskScheduler = applicationContext.getBean(theadPoolName, ThreadPoolTaskScheduler.class);
                threadPoolTaskScheduler.shutdown();
                ((BeanDefinitionRegistry) applicationContext.getBeanFactory()).removeBeanDefinition(theadPoolName);
            } else if (TASK_ACTION_START.equalsIgnoreCase(taskScheduler.getAction())) {

                // 是否单节点任务，为true使用任务ID作为redis锁执行任务，其他跳过
                boolean singleNode = taskScheduler.getIsSingleNode() == 1;
                // 配置执行器名称后，如果名称不一致，则不执行后续
                if (!isEmpty(this.executorName) && !isEmpty(taskScheduler.getExecutorName()) && !taskScheduler.getExecutorName().equals(this.executorName)) {
                    log.debug("执行器名称未配置或不一致，配置执行器名称：{},当前执行器名称：{}", this.executorName, executorName);
                    return;
                }

                // 获取注解为TaskHandler的bean
                Map<String, Object> beansWithAnnotation = applicationContext.getBeansWithAnnotation(TaskHandler.class);
                if (!beansWithAnnotation.isEmpty()) {
                    if (singleNode) {
                        String lockName = TASK_PRE + taskScheduler.getId() + ":" + taskScheduler.getRandomId();
                        Long increment = stringRedisTemplate.opsForValue().increment(lockName);
                        stringRedisTemplate.expire(lockName, 5, TimeUnit.SECONDS);
                        if (increment == null || increment != 1) {
                            // 未获得锁则跳过后续执行
                            try {
                                log.debug("当前节点[{}]未竞争到单节点锁，结束调度", InetAddress.getLocalHost().getHostName());
                            } catch (UnknownHostException e) {
                                log.error("获取hostname失败", e);
                            }
                            return;
                        }
                    }
                    ITaskHandler iTaskHandler = (ITaskHandler) beansWithAnnotation.get(taskScheduler.getHandlerName());
                    if (iTaskHandler != null) {

                        ThreadPoolTaskScheduler threadPoolTaskScheduler;
                        try {
                            threadPoolTaskScheduler = applicationContext.getBean(theadPoolName, ThreadPoolTaskScheduler.class);
                        } catch (NoSuchBeanDefinitionException e) {
                            // Bean不存在
                            AbstractBeanDefinition rawBeanDefinition = BeanDefinitionBuilder.genericBeanDefinition(ThreadPoolTaskScheduler.class).getRawBeanDefinition();
                            BeanDefinitionRegistry beanFactory = (BeanDefinitionRegistry) applicationContext.getBeanFactory();
                            beanFactory.registerBeanDefinition(theadPoolName, rawBeanDefinition);
                            threadPoolTaskScheduler = applicationContext.getBean(theadPoolName, ThreadPoolTaskScheduler.class);
                        }

                        if (taskScheduler.getIsOnlyExecuteOnce() != null && taskScheduler.getIsOnlyExecuteOnce() == 1) {
                            // 单次执行
                            threadPoolTaskScheduler.submit(new TaskRunnable(iTaskHandler, taskScheduler.getParams()));
                        } else if (FIXED_DELAY.equals(taskScheduler.getScheduleType())) {
                            // 基于FIXED DELAY执行
                            threadPoolTaskScheduler.scheduleWithFixedDelay(new TaskRunnable(iTaskHandler, taskScheduler.getParams()), Long.parseLong(taskScheduler.getCronExpressionOrFixedDelay()));
                        } else {
                            // 基于CRON表达式执行
                            threadPoolTaskScheduler.schedule(new TaskRunnable(iTaskHandler, taskScheduler.getParams()), new CronTrigger(taskScheduler.getCronExpressionOrFixedDelay()));
                        }
                    }
                }
            }
        }
    }

}
