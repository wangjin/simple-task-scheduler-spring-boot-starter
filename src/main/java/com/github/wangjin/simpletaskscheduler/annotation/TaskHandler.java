package com.github.wangjin.simpletaskscheduler.annotation;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * @author Jin Wang
 * @version 1.0
 * @date 2019-11-07 6:22 下午
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface TaskHandler {

    @AliasFor("name")
    String value() default "";

    @AliasFor("value")
    String name() default "";
}
