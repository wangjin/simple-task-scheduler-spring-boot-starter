package com.wangjin.test;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author Jin Wang
 * @version 1.0
 * @date 2020-06-11 6:28 下午
 */
public class TimeTest {

    @Test
    public void contextLoads() throws Exception {

        LocalDateTime now = LocalDateTime.now();

        LocalDateTime parse = LocalDateTime.parse("2020-06-11 18:20:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        long l = Duration.between(parse, now).toMinutes();
        System.out.println(l);

    }
}
