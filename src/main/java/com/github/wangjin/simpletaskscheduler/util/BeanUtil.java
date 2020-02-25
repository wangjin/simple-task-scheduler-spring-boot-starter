package com.github.wangjin.simpletaskscheduler.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * @author Jin Wang
 * @version 1.0
 * @date 2020-02-05 2:26 下午
 */
public class BeanUtil {

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    /**
     * 注册Spring Bean
     *
     * @param beanName bean名称
     * @return bean实例
     */
    private ThreadPoolTaskScheduler createSpringBean(String beanName) {
        AbstractBeanDefinition rawBeanDefinition = BeanDefinitionBuilder.genericBeanDefinition(ThreadPoolTaskScheduler.class).getRawBeanDefinition();
        BeanDefinitionRegistry beanFactory = (BeanDefinitionRegistry) applicationContext.getBeanFactory();
        beanFactory.registerBeanDefinition(beanName, rawBeanDefinition);
        return applicationContext.getBean(beanName, ThreadPoolTaskScheduler.class);
    }

    /**
     * 根据bean名称获取bean
     *
     * @param beanName bean名称
     * @return bean实例
     */
    private ThreadPoolTaskScheduler getBean(String beanName) {
        return applicationContext.getBean(beanName, ThreadPoolTaskScheduler.class);
    }

    /**
     * 根据bean名称删除bean
     *
     * @param beanName bean名称
     */
    private void removeBean(String beanName) {
        BeanDefinitionRegistry beanFactory = (BeanDefinitionRegistry) applicationContext.getBeanFactory();
        beanFactory.removeBeanDefinition(beanName);
    }
}
