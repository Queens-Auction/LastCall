package org.example.lastcall.common.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuctionConfig {
    // 교환기(Exchange), 큐(Queue), 라우팅 키 를 상수 정의
    public static final String EXCHANGE_NAME = "auction.exchange";
    public static final String QUEUE_NAME = "auction.queue";
    public static final String ROUTING_KEY = "auction.key";

    // topic 타입 교환기 생성
    // -> 여러 라우팅 키 패턴 매칭 가능
    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    // 메시지 저장할 Queue 생성 (지속성 true)
    @Bean
    public Queue queue() {
        return new Queue(QUEUE_NAME);
    }

    // Exchange 와 Queue 를 ROUTING_KEY 로 연결
    @Bean
    public Binding binding(Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY);
    }

    // Jackson 사용한 JSON 직렬화/역직렬화 변환기(Converter)
    // -> 객체를 JSON 형태로 RabbitMQ에 전달할 수 있게 함
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // 메시지 송신용 RabbitTemplate 생성
    // -> 위 MessageConverter를 적용하여 JSON 포맷 메시지 전송
    @Bean(name = "auctionRabbitTemplate")
    public AmqpTemplate auctionRabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }
}
