/**
 * Copyright © 2021, JDA Software Group, Inc. ALL RIGHTS RESERVED.
 * <p>
 * This software is the confidential information of JDA Software, Inc., and is licensed
 * as restricted rights software. The use,reproduction, or disclosure of this software
 * is subject to restrictions set forth in your license agreement with JDA.
 */
package com.jda.dct.chatservice.config;

import com.jda.dct.email.BaseEmailNotifier;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AsyncConfig {
    /**
     * Create Base Email Notifier for email service.
     *
     * @return
     */
    @Bean(name = "baseEmailNotifier")
    public BaseEmailNotifier baseEmailNotifier() {
        return new BaseEmailNotifier();
    }

    /**
     * for @async annotation thread pool setup.
     *
     * @return
     */
    @Bean(name = "emailThreadPoolTaskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(3);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("email-notification-");
        executor.initialize();
        return executor;
    }
}
