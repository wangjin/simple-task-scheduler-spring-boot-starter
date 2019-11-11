package com.github.wangjin.simpletaskscheduler.handler;

/**
 * @author Jin Wang
 * @version 1.0
 * @date 2019-11-07 9:35 下午
 */
public interface ITaskHandler {

    /**
     * 执行任务
     *
     * @param params 入参
     * @return 执行结果
     * @throws Exception 运行异常
     */
    String execute(String params) throws Exception;
}
