package com.github.wangjin.simpletaskscheduler.runnable;

import com.github.wangjin.simpletaskscheduler.handler.ITaskHandler;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Jin Wang
 * @version 1.0
 * @date 2020-02-05 2:17 下午
 */
@Slf4j
@AllArgsConstructor
public class TaskRunnable implements Runnable {

    private ITaskHandler iTaskHandler;

    private String params;

    @Override
    public void run() {
        try {
            iTaskHandler.execute(params);
        } catch (Exception e) {
            log.error("[task execute failed]", e);
        }
    }
}
