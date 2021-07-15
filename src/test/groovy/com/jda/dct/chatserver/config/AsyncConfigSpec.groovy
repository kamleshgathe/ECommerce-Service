/*
 * Copyright © 2021, JDA Software Group, Inc. ALL RIGHTS RESERVED.
 * <p>
 * This software is the confidential information of JDA Software, Inc., and is licensed
 * as restricted rights software. The use,reproduction, or disclosure of this software
 * is subject to restrictions set forth in your license agreement with JDA.
 */
package com.jda.dct.chatserver.config

import com.jda.dct.chatservice.config.AsyncConfig
import com.jda.dct.email.BaseEmailNotifier
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import spock.lang.Specification

import java.util.concurrent.Executor

class AsyncConfigSpec extends Specification{
    def "test baseEmailNotifier"(){
        given:
        AsyncConfig asyncConfig = new AsyncConfig()
        when:
        BaseEmailNotifier emailNotifier = asyncConfig.baseEmailNotifier()
        then:
        emailNotifier instanceof  BaseEmailNotifier
    }
    def "test taskExecutor"(){
        given:
        AsyncConfig asyncConfig = new AsyncConfig()
        when:
        Executor executor = asyncConfig.taskExecutor()
        then:
        executor instanceof  ThreadPoolTaskExecutor
    }
}
