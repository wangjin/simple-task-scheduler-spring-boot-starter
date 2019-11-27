package com.github.wangjin.simpletaskscheduler.entity;

import lombok.Data;

/**
 * @author Zhu Xiu
 * @version 1.0
 * @date 2019-11-27 11:09
 */
@Data
public class TaskScheduler {
    /**
     * 主键
     */
    private Long id;

    /**
     * 任务名称
     */
    private String name;

    /**
     * 注册bean名称
     */
    private String handlerName;

    /**
     * cron表达式或间隔
     */
    private String cronExpressionOrFixedDelay;

    /**
     * 是否单节点任务 1：是 0：否
     */
    private Byte isSingleNode;

    /**
     * 是否单节点任务 1：是 0：否
     */
    private Byte isOnlyExecuteOnce;

    /**
     * 参数
     */

    private String params;

    /**
     * 状态 0：未启动1：启动
     */
    private Byte status;

    /**
     * 执行器名称
     */
    private String executorName;

    /**
     * 调度类型 1：cron 2：fixedDelay
     */
    private Byte scheduleType;

    /**
     * 随机ID
     */
    private String randomId;

}
