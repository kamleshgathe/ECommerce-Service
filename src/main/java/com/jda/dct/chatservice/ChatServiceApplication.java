/**
 * Copyright © 2020, JDA Software Group, Inc. ALL RIGHTS RESERVED.
 * <p>
 * This software is the confidential information of JDA Software, Inc., and is licensed
 * as restricted rights software. The use,reproduction, or disclosure of this software
 * is subject to restrictions set forth in your license agreement with JDA.
 */
package com.jda.dct.chatservice;

import com.jda.luminate.app.i18n.LuminateMessageSourceConfig;
import com.jda.luminate.security.contexts.SpringAuthContextConfig;
import com.jda.luminate.security.oauth.OAuth2ResourceServerConfig;
import com.jda.luminate.tracing.LuminateTracingConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.elasticsearch.rest.RestClientAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(exclude = {
    KafkaAutoConfiguration.class,
    DataSourceAutoConfiguration.class,
    JpaRepositoriesAutoConfiguration.class,
    ElasticsearchAutoConfiguration.class,
    RestClientAutoConfiguration.class
})
@ComponentScan(basePackages = {
    "com.jda.dct.kafka",
    "com.jda.dct.chatservice",
    "com.jda.dct.persist",
    "com.jda.dct.search",
    "com.jda.dct.foundation.process",
    "com.jda.dct.ignitecaches.autoconfig",
    "com.jda.dct.ignitecaches.springimpl",
    "com.jda.dct.foundation.tenantutils",
    "com.jda.dct.exec.lib.actions",
    "com.jda.dct.domain.util",
    "com.jda.luminate.app.exception",
    "com.jda.dct.exec.lib.relationships",
    "com.jda.dct.exec.permission",
    "com.jda.dct.exec.statemachine",
    "com.jda.dct.exec.compute",
    "com.jda.dct.exec.lib.actions",
    "com.jda.dct.exec.util",
    "com.jda.dct.exec.facade",
    "com.jda.luminate.messaging.kafka",
    "com.jda.luminate.common.aspects",
    "com.jda.luminate.io.documentstore",
    "com.jda.luminate.ingest.rest.services",
    "com.jda.luminate.documentation",
    "com.jda.luminate.common.cache"
})

@EntityScan(basePackages = {"com.jda.dct.domain"})
@Import({SpringAuthContextConfig.class, OAuth2ResourceServerConfig.class, LuminateTracingConfig.class,
    LuminateMessageSourceConfig.class})
@EnableJpaRepositories(basePackages = {"com.jda.dct.ignitecaches.springimpl",
    "com.jda.dct.chatservice.repository"})

public class ChatServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatServiceApplication.class, args);
    }

}
