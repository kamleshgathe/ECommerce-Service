/**
 * Copyright © 2019, JDA Software Group, Inc. ALL RIGHTS RESERVED.
 * <p>
 * This software is the confidential information of JDA Software, Inc., and is licensed
 * as restricted rights software. The use,reproduction, or disclosure of this software
 * is subject to restrictions set forth in your license agreement with JDA.
 */

package com.jda.dct.chatservice;

import com.jda.dct.contexts.SpringAuthContextConfig;
import com.jda.dct.oauth.OAuth2ResourceServerConfig;
import com.jda.dct.tracing.DctTracingConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(exclude = {
    KafkaAutoConfiguration.class,
    DataSourceAutoConfiguration.class,
    JpaRepositoriesAutoConfiguration.class
})
@ComponentScan(basePackages = {
    "com.jda.dct.kafka",
    "com.jda.dct.chatservice",
    "com.jda.dct.persist",
    "com.jda.dct.search",
    "com.jda.dct.foundation.process",
    "com.jda.dct.ignitecaches.springimpl",
    "com.jda.dct.foundation.tenantutils",
    "com.jda.dct.exec.lib.actions",
    "com.jda.dct.domain.util",
    "com.jda.dct.exec.lib.relationships"
})

@EntityScan(basePackages = {"com.jda.dct.domain"})
@Import({SpringAuthContextConfig.class, OAuth2ResourceServerConfig.class, DctTracingConfig.class})
@EnableJpaRepositories(basePackages = {"com.jda.dct.ignitecaches.springimpl",
    "com.jda.dct.chatservice.repository"})
public class ChatServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatServiceApplication.class, args);
    }

}
