package com.github.wangjin.simpletaskscheduler.constant;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Jin Wang
 * @version 1.0
 * @date 2019-11-09 11:23 下午
 */
public interface Constants {

    String TASK_SCHEDULER_CHANNEL = "TASK_SCHEDULER_CHANNEL";
    String TASK_RE_SCHEDULER_CHANNEL = "TASK_RE_SCHEDULER_CHANNEL";
    String THEAD_POOL_POST = "ThreadPool";
    String INIT_TIME = "initTime";
    int SECONDS_PER_MINUTE = 60;

    Map<String, Object> paramsMap = new HashMap<>();

}
