package org.example.lastcall.common.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class AuctionConfig {
    public static final String EXCHANGE_NAME = "auction.exchange";
    public static final String START_QUEUE_NAME = "auction.start.queue";
    public static final String END_QUEUE_NAME = "auction.end.queue";
    public static final String START_ROUTING_KEY = "auction.start.key";
    public static final String END_ROUTING_KEY = "auction.end.key";

    @Bean
    public CustomExchange delayExchange() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-delayed-type", "direct");

        return new CustomExchange(EXCHANGE_NAME, "x-delayed-message", true, false, args);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean(name = "auctionRabbitTemplate")
    public AmqpTemplate auctionRabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());

        return rabbitTemplate;
    }

    @Bean
    public Queue startQueue() {
        return new Queue(START_QUEUE_NAME, true);
    }

    @Bean
    public Binding startBinding(Queue startQueue, CustomExchange delayExchange) {
        return BindingBuilder.bind(startQueue).to(delayExchange).with(START_ROUTING_KEY).noargs();
    }

    @Bean
    public Queue endQueue() {
        return new Queue(END_QUEUE_NAME, true);
    }

    @Bean
    public Binding endBinding(Queue endQueue, CustomExchange delayExchange) {
        return BindingBuilder.bind(endQueue).to(delayExchange).with(END_ROUTING_KEY).noargs();
    }
}
