package br.com.sellerintelligence.sync.infrastructure.rabbitmq;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConditionalOnClass(name = "org.springframework.amqp.rabbit.core.RabbitTemplate")
@ConditionalOnProperty(name = "spring.rabbitmq.listener.simple.auto-startup", havingValue = "true", matchIfMissing = true)
@EnableRabbit
public class RabbitMQConfig {

    public static final String SYNC_EXCHANGE = "sync.exchange";
    public static final String SYNC_QUEUE = "sync.job.queue";
    public static final String SYNC_ROUTING_KEY = "sync.job.request";

    public static final String SYNC_DLX = "sync.dlx";
    public static final String SYNC_DLQ = "sync.job.dlq";
    public static final String SYNC_DLQ_ROUTING_KEY = "sync.job.dead";

    @Bean
    public DirectExchange syncExchange() {
        return new DirectExchange(SYNC_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange syncDeadLetterExchange() {
        return new DirectExchange(SYNC_DLX, true, false);
    }

    @Bean
    public Queue syncQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", SYNC_DLX);
        args.put("x-dead-letter-routing-key", SYNC_DLQ_ROUTING_KEY);
        return new Queue(SYNC_QUEUE, true, false, false, args);
    }

    @Bean
    public Queue syncDeadLetterQueue() {
        return new Queue(SYNC_DLQ, true, false, false);
    }

    @Bean
    public Binding syncBinding(Queue syncQueue, DirectExchange syncExchange) {
        return BindingBuilder.bind(syncQueue).to(syncExchange).with(SYNC_ROUTING_KEY);
    }

    @Bean
    public Binding syncDeadLetterBinding(Queue syncDeadLetterQueue, DirectExchange syncDeadLetterExchange) {
        return BindingBuilder.bind(syncDeadLetterQueue).to(syncDeadLetterExchange).with(SYNC_DLQ_ROUTING_KEY);
    }
}
