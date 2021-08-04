package com.github.wangjin.simpletaskscheduler.listener;

/**
 * @author Jin Wang
 * @version 1.0
 * @date 2019-11-07 5:55 下午
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wangjin.simpletaskscheduler.annotation.TaskHandler;
import com.github.wangjin.simpletaskscheduler.entity.TaskScheduler;
import com.github.wangjin.simpletaskscheduler.handler.ITaskHandler;
import com.github.wangjin.simpletaskscheduler.init.InitUtil;
import com.github.wangjin.simpletaskscheduler.runnable.TaskRunnable;
import lombok.SneakyThrows;
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
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.github.wangjin.simpletaskscheduler.constant.Constants.SECONDS_PER_MINUTE;
import static com.github.wangjin.simpletaskscheduler.constant.Constants.THEAD_POOL_POST;
import static org.springframework.util.ObjectUtils.isEmpty;

@Slf4j
public class TaskSchedulerListener implements MessageListener {

    private static final Byte FIXED_DELAY = 2;
    private static final String TASK_PRE = "TASK-";
    private static final String TASK_ACTION_START = "start";
    private static final String TASK_ACTION_STOP = "stop";
    private final ConfigurableApplicationContext applicationContext;
    private final StringRedisTemplate stringRedisTemplate;
    private final String executorName;

    public TaskSchedulerListener(ConfigurableApplicationContext applicationContext, StringRedisTemplate stringRedisTemplate, String executorName) {
        this.applicationContext = applicationContext;
        this.stringRedisTemplate = stringRedisTemplate;
        this.executorName = executorName;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {

        Optional.of(message.getBody()).ifPresent(msg -> {
            //构建taskScheduler对象
            TaskScheduler taskScheduler;
            try {
                taskScheduler = new ObjectMapper().readValue(message.getBody(), TaskScheduler.class);
            } catch (IOException e) {
                log.error("JSON解析失败", e);
                return;
            }

            // 对应调度线程池名称
            String theadPoolName = taskScheduler.getHandlerName() + THEAD_POOL_POST;

            if (TASK_ACTION_STOP.equalsIgnoreCase(taskScheduler.getAction())) {
                log.debug("关闭线程池[{}]", theadPoolName);
                // 找到并关闭对应线程池
                ThreadPoolTaskScheduler threadPoolTaskScheduler = applicationContext.getBean(theadPoolName, ThreadPoolTaskScheduler.class);
                threadPoolTaskScheduler.setWaitForTasksToCompleteOnShutdown(false);
                threadPoolTaskScheduler.shutdown();
                ((BeanDefinitionRegistry) applicationContext.getBeanFactory()).removeBeanDefinition(theadPoolName);
            } else if (TASK_ACTION_START.equalsIgnoreCase(taskScheduler.getAction())) {

                // 是否单节点任务，为true使用任务ID作为redis锁执行任务，其他跳过
                boolean singleNode = taskScheduler.getIsSingleNode() == 1;
                // 配置执行器名称后，如果名称不一致，则不执行后续
                if (!isEmpty(this.executorName) && !isEmpty(taskScheduler.getExecutorName()) && !taskScheduler.getExecutorName().contains(this.executorName)) {
                    log.debug("执行器名称未配置或不一致，配置执行器名称：{},要求执行器名称：{}", this.executorName, taskScheduler.getExecutorName());
                    return;
                }

                // 获取注解为TaskHandler的bean
                Map<String, Object> beansWithAnnotation = applicationContext.getBeansWithAnnotation(TaskHandler.class);
                if (!beansWithAnnotation.isEmpty()) {
                    if (singleNode) {
                        String randomId = taskScheduler.getRandomId();
                        int lockSeconds = 10;
                        if (ObjectUtils.isEmpty(randomId)) {
                            // randomId为空即为自动启动任务，防止重复执行，延迟锁时间
                            randomId = "SINGLE_NODE_ID";
                            lockSeconds = 120;
                            LocalDateTime initTime = InitUtil.getInitTime();
                            // 初始化时间大于120秒
                            if (isEmpty(initTime) || Duration.between(initTime, LocalDateTime.now()).toMinutes() * SECONDS_PER_MINUTE > lockSeconds) {
                                return;
                            }
                        }
                        String lockName = TASK_PRE + taskScheduler.getId() + ":" + randomId;
                        Long increment = stringRedisTemplate.opsForValue().increment(lockName);
                        stringRedisTemplate.expire(lockName, lockSeconds, TimeUnit.SECONDS);
                        if (increment == null || increment != 1) {
                            // 未获得锁则跳过后续执行
                            try {
                                log.warn("当前节点[{}]未竞争到单节点锁，结束调度[{}]", InetAddress.getLocalHost().getHostName(), taskScheduler.getName());
                            } catch (UnknownHostException e) {
                                log.error("获取hostname失败", e);
                            }
                            return;
                        }
                    }
                    runTask(taskScheduler, theadPoolName, beansWithAnnotation);
                }
            }

        });
    }

    /**
     * 运行任务
     *
     * @param taskScheduler       任务调度器
     * @param theadPoolName       线程池名称
     * @param beansWithAnnotation 注解
     */
    private void runTask(TaskScheduler taskScheduler, String theadPoolName, Map<String, Object> beansWithAnnotation) {
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

            // 当前线程池中已经有线程则不提交新任务
            if (threadPoolTaskScheduler.getActiveCount() > 0) {
                return;
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
