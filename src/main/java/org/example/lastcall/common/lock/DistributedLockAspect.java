package org.example.lastcall.common.lock;

import java.util.concurrent.TimeUnit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.example.lastcall.common.exception.BusinessException;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@Order(-1)
public class DistributedLockAspect {
	private final RedissonClient redissonClient;
	private final ExpressionParser parser = new SpelExpressionParser();

	/**
	 * 메서드 실행 전 락을 획득하고, 실행 후 해제
	 * @param pjp               실행 대상 메서드
	 * @param distributedLock   락 설정 어노테이션
	 * @return 메서드 실행 결과
	 * @throws Throwable        예외 발생 시
	 */
	@Around("@annotation(distributedLock)")
	public Object around(ProceedingJoinPoint pjp, DistributedLock distributedLock) throws Throwable {
		// 지금 실행하려는 메서드(pjp)와 어노테이션에 지정된 key 값을 조합해서 실제 redis에서 사용할 진짜 락 키 문자열을 만들어내는 과정
		String originKey = parseKey(pjp, distributedLock.key());
		// 네임스페이스를 추가하여 redis 내 다른 키와 겹치지 않도록 함
		String lockKey = "lock:" + originKey;

		long waitTime = distributedLock.waitTime();
		long leaseTime = distributedLock.leaseTime();

		// 지정된 이름으로 RLock 객체를 생성하며, Redis의 key로 사용됨
		RLock rLock = redissonClient.getLock(lockKey);

		// 락 획득 성공 여부
		boolean lockAcquired = false;

		try {
			// 주어진 시간동안 락 획득을 시도하며, 성공하면 지정된 시간 후 자동으로 해제 됨
			lockAcquired = rLock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
			// rLock.lock();

			// 락 획득 실패 시
			if (!lockAcquired) {
				log.warn("[RedissonLock] 락 획득 실패 - lockKey: {}", lockKey);
				throw new BusinessException(LockErrorCode.LOCK_ACQUISITION_FAILED);
			}
			// 락 획득 성공 시
			log.info("[RedissonLock] 락 획득 성공 - lockKey: {}", lockKey);

			// 원래 비지니스 메서드 실행
			return pjp.proceed();
		} catch (InterruptedException e) {
			// 락 대기 중 스레드 인터럽트 발생 시 인터럽트 상태 복원
			Thread.currentThread().interrupt();

			log.error("[RedissonLock] 락 획득 중 인터럽트 발생", e);
			throw new BusinessException(LockErrorCode.LOCK_INTERRUPTED);
		} finally {
			// 현재 스레드가 해당 락을 보유하고 있는지 여부를 반환
			if (lockAcquired && rLock.isHeldByCurrentThread()) {
				try {
					// 현재 스레드가 보유한 락을 해제함. 반드시 finally 블록에서 호출해야 함
					rLock.unlock();
					log.info("[RedissonLock] 락 해제 완료 - lockKey: {}", lockKey);
				} catch (IllegalMonitorStateException e) {
					log.warn("[RedissonLock] 이미 해제된 락 또는 스레드 불일치 - lockKey: {}", lockKey, e);
				}
			}
		}
	}

	/**
	 * SpEL 표현식을 기반으로 락 키를 생성함
	 *
	 * @param pjp                메서드 실행 정보
	 * @param keyExpression        SpEL 표현식
	 * @return 생성된 락 키
	 */
	private String parseKey(ProceedingJoinPoint pjp, String keyExpression) {
		MethodSignature signature = (MethodSignature)pjp.getSignature();
		String[] parameterNames = signature.getParameterNames();
		Object[] args = pjp.getArgs();

		EvaluationContext context = new StandardEvaluationContext();

		for (int i = 0; i < parameterNames.length; i++) {
			context.setVariable(parameterNames[i], args[i]);
		}

		return parser.parseExpression(keyExpression).getValue(context, String.class);
	}
}
