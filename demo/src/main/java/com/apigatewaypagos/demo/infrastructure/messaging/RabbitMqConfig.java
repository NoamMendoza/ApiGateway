package com.apigatewaypagos.demo.infrastructure.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {
    public static final String EXCHANGE_NAME = "payment.exchange";
    public static final String QUEUE_COMPLETED = "payment.completed.queue";
    public static final String ROUTING_KEY_COMPLETED = "payment.completed.#";

    @Bean
    public Queue paymentCompletedQueue(){
        return new Queue(QUEUE_COMPLETED, true);
    }

    @Bean
    public TopicExchange paymentExchange(){
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Binding bindingPaymentCompleted(Queue paymentCompletedQueue, TopicExchange paymentExchange){
        return BindingBuilder.bind(paymentCompletedQueue).to(paymentExchange).with(ROUTING_KEY_COMPLETED);
    }

    @Bean
    public MessageConverter jsonMessageConverter(){
        return new Jackson2JsonMessageConverter();
    }
}

