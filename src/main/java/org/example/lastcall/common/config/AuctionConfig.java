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
    // 교환기(Exchange), 큐(Queue), 라우팅 키 를 상수 정의
    // 교환기
    public static final String EXCHANGE_NAME = "auction.exchange";
    // 큐 이름
    public static final String START_QUEUE_NAME = "auction.start.queue";
    public static final String END_QUEUE_NAME = "auction.end.queue";
    // 라우팅 키
    public static final String START_ROUTING_KEY = "auction.start.key";
    public static final String END_ROUTING_KEY = "auction.end.key";

    // [공용] //
    // Delay Exchange 설정 (지연 큐)
    // -> 메시지 바로 큐로 보내지 않고, 지정된 시간(x-delay)만큼 대기 후 전달
    @Bean
    public CustomExchange delayExchange() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-delayed-type", "direct");
        return new CustomExchange(EXCHANGE_NAME, "x-delayed-message", true, false, args);
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

    // [경매 시작 전용]
    // 메시지 저장할 Queue 생성 (지속성 true)
    @Bean
    public Queue startQueue() {
        return new Queue(START_QUEUE_NAME);
    }

    // Exchange 와 Queue 를 ROUTING_KEY 로 연결
    @Bean
    public Binding startBinding(Queue startQueue, CustomExchange delayExchange) {
        return BindingBuilder.bind(startQueue).to(delayExchange).with(START_ROUTING_KEY).noargs();
    }

    // [경매 종료 전용]
    // 메시지 저장할 Queue 생성 (지속성 true)
    @Bean
    public Queue endQueue() {
        return new Queue(END_QUEUE_NAME);
    }

    // Exchange 와 Queue 를 ROUTING_KEY 로 연결
    @Bean
    public Binding endBinding(Queue endQueue, CustomExchange delayExchange) {
        // to.(delayExchange) : 메시지가 통과할 교환기 지정 -> 이 메시지는 지연큐 통해 들어옴을 명시
        // .with(ROUTING_KEY) : 어떤 라우팅 키로 메시지 구분할지 결정
        // .noargs() : .with()로 지정한 라우팅 키 외에 추가가 없어 기본 연결로 끝낸다는 뜻
        return BindingBuilder.bind(endQueue).to(delayExchange).with(END_ROUTING_KEY).noargs();
    }
}
