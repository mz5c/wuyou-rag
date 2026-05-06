package com.wuyou.rag.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String DOCUMENT_PROCESS_QUEUE = "document.process.queue";

    @Bean
    public Queue documentProcessQueue() {
        return QueueBuilder.durable(DOCUMENT_PROCESS_QUEUE).build();
    }
}
