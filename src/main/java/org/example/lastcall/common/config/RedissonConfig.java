package org.example.lastcall.common.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {
	@Value("${spring.data.redis.host}")
	private String host;

	@Value("${spring.data.redis.port}")
	private int port;

	@Bean
	public RedissonClient redissonClient() {
		Config config = new Config();
		config.useSingleServer()                            // Redis 서버 연결 방식 지정 (Redis 단일 서버 모드 사용)
			.setAddress("redis://" + host + ":" + port);    // Redis 서버 주소 (호스트와 포트)

		return Redisson.create(config);                     // RedissonClient 객체 생성 및 반환
	}
}
