package org.example.lastcall.common.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// EntityManager 를 스프링이 자동 주입
// -> JPAQueryFactory 를 Bean 으로 등록해서 어디서든 @Autowired나 @RequiredArgsConstructor로 사용 가능
@Configuration
public class QueryDslConfig {
    @Bean
    public JPAQueryFactory jpaQueryFactory(EntityManager em) {
        return new JPAQueryFactory(em);
    }
}
