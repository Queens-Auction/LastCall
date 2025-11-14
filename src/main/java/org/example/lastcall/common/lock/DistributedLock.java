package org.example.lastcall.common.lock;

// 분산락 적용용 어노테이션
// 특정 메서드 실행 시 Redisson 분산락을 획득/반환하도록 처리함

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)            // 메서드에서만 적용 가능함
@Retention(RetentionPolicy.RUNTIME)    // 런타임 시점 접근 가능
@Documented                            // 관례적으로 넣음 (필수는 아님)
public @interface DistributedLock {
	String key();                    // 락을 식별하기 위한 Redis key

	long waitTime() default 5;        // 락 획득 대기 시간 (초)

	long leaseTime() default 10;    // 락 점유 시간 (초)
}
