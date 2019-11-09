package com.github.wangjin.simpletaskscheduler.listener;

/**
 * @author Jin Wang
 * @version 1.0
 * @date 2019-11-07 5:55 下午
 */

import com.alibaba.fastjson.JSONObject;
import com.github.wangjin.simpletaskscheduler.annotation.TaskHandler;
import com.github.wangjin.simpletaskscheduler.handler.ITaskHandler;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.springframework.util.ObjectUtils.isEmpty;

public class TaskSchedulerListener implements MessageListener {

    private ApplicationContext applicationContext;

    private StringRedisTemplate stringRedisTemplate;

    private static final String TASK_PRE = "TASK-";

    public TaskSchedulerListener(ApplicationContext applicationContext, StringRedisTemplate stringRedisTemplate) {
        this.applicationContext = applicationContext;
        this.stringRedisTemplate = stringRedisTemplate;
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

            // 获取注解为TaskHandler的bean
            Map<String, Object> beansWithAnnotation = applicationContext.getBeansWithAnnotation(TaskHandler.class);
            if (!beansWithAnnotation.isEmpty()) {
                if (singleNode) {
                    String lockName = TASK_PRE + taskId + ":" + randomId;
                    Long increment = stringRedisTemplate.opsForValue().increment(lockName);
                    stringRedisTemplate.expire(lockName, 5, TimeUnit.SECONDS);
                    if (increment == null || increment != 1) {
                        // 未获得锁则跳过后续执行
                        return;
                    }
                }
                ITaskHandler iTaskHandler = (ITaskHandler) beansWithAnnotation.get(taskHandlerName);
                if (iTaskHandler != null) {
                    String execute = iTaskHandler.execute(params);
                }
            }
        }
    }

}