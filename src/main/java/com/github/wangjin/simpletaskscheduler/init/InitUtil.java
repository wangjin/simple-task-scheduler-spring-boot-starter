package com.github.wangjin.simpletaskscheduler.init;

import com.github.wangjin.simpletaskscheduler.constant.Constants;

import java.time.LocalDateTime;

import static com.github.wangjin.simpletaskscheduler.constant.Constants.INIT_TIME;

/**
 * @author Jin Wang
 * @version 1.0
 * @date 2020-06-11 6:23 下午
 */
public class InitUtil {

    public static void setInitTime() {
        Constants.paramsMap.put(INIT_TIME, LocalDateTime.now());
    }

    public static LocalDateTime getInitTIme() {
        return (LocalDateTime) Constants.paramsMap.get(INIT_TIME);
    }
}
