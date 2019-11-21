package com.github.wangjin.simpletaskscheduler.listener;

/**
 * @author Jin Wang
 * @version 1.0
 * @date 2019-11-07 5:55 下午
 */

import com.alibaba.fastjson.JSONObject;
import com.github.wangjin.simpletaskscheduler.annotation.TaskHandler;
import com.github.wangjin.simpletaskscheduler.handler.ITaskHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.springframework.util.ObjectUtils.isEmpty;

@Slf4j
public class TaskSchedulerListener implements MessageListener {

    private ApplicationContext applicationContext;

    private StringRedisTemplate stringRedisTemplate;

    private String executorName;

    private static final String TASK_PRE = "TASK-";

    public TaskSchedulerListener(ApplicationContext applicationContext, StringRedisTemplate stringRedisTemplate, String executorName) {
        this.applicationContext = applicationContext;
        this.stringRedisTemplate = stringRedisTemplate;
        this.executorName = executorName;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        if (!isEmpty(message.getBody())) {
            JSONObject jsonObject = JSONObject.parseObject(new String(message.getBody(), StandardCharsets.UTF_8));
            // 任务ID
            int taskId = jsonObject.getIntValue("id");
            // 任务执行器名称
            String taskHandlerName = jsonObject.getString("handlerName");
            // 任务参数
            String params = jsonObject.getString("params");
            // 是否单节点任务，为true使用任务ID作为redis锁执行任务，其他跳过
            boolean singleNode = jsonObject.getIntValue("isSingleNode") == 1;
            // 随机任务ID
            String randomId = jsonObject.getString("randomId");
            // 随机任务ID
            String executorName = jsonObject.getString("executorName");

            // 配置执行器名称后，如果名称不一致，则不执行后续
            if (!isEmpty(this.executorName) && !isEmpty(executorName) && !executorName.equals(this.executorName)) {
                log.warn("执行器名称未配置或不一致，配置执行器名称：{},当前执行器名称：{}", this.executorName, executorName);
                return;
            }

            // 获取注解为TaskHandler的bean
            Map<String, Object> beansWithAnnotation = applicationContext.getBeansWithAnnotation(TaskHandler.class);
            if (!beansWithAnnotation.isEmpty()) {
                if (singleNode) {
                    String lockName = TASK_PRE + taskId + ":" + randomId;
                    Long increment = stringRedisTemplate.opsForValue().increment(lockName);
                    stringRedisTemplate.expire(lockName, 5, TimeUnit.SECONDS);
                    if (increment == null || increment != 1) {
                        // 未获得锁则跳过后续执行
                        try {
                            log.warn("当前节点[{}]未竞争到单节点锁，结束调度", InetAddress.getLocalHost().getHostName());
                        } catch (UnknownHostException e) {
                            log.error("获取hostname失败", e);
                        }
                        return;
                    }
                }
                ITaskHandler iTaskHandler = (ITaskHandler) beansWithAnnotation.get(taskHandlerName);
                if (iTaskHandler != null) {
                    try {
                        String execute = iTaskHandler.execute(params);
                    } catch (Exception e) {
                        log.error("[Simple-Task-Scheduler] interrupted by exception", e);
                    }
                }
            }
        }
    }

}
