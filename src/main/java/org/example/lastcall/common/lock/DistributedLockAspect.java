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

	@Around("@annotation(distributedLock)")
	public Object around(ProceedingJoinPoint pjp, DistributedLock distributedLock) throws Throwable {
		String originKey = parseKey(pjp, distributedLock.key());
		String lockKey = "lock:" + originKey;

		long waitTime = distributedLock.waitTime();
		long leaseTime = distributedLock.leaseTime();

		RLock rLock = redissonClient.getLock(lockKey);

		boolean lockAcquired = false;

		try {
			lockAcquired = rLock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);

			if (!lockAcquired) {
				log.warn("[RedissonLock] 락 획득 실패 - lockKey: {}", lockKey);
				throw new BusinessException(LockErrorCode.LOCK_ACQUISITION_FAILED);
			}

			log.info("[RedissonLock] 락 획득 성공 - lockKey: {}", lockKey);

			return pjp.proceed();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.error("[RedissonLock] 락 획득 중 인터럽트 발생", e);
			throw new BusinessException(LockErrorCode.LOCK_INTERRUPTED);
		} finally {
			if (lockAcquired && rLock.isHeldByCurrentThread()) {
				try {
					rLock.unlock();
					log.info("[RedissonLock] 락 해제 완료 - lockKey: {}", lockKey);
				} catch (IllegalMonitorStateException e) {
					log.warn("[RedissonLock] 이미 해제된 락 또는 스레드 불일치 - lockKey: {}", lockKey, e);
				}
			}
		}
	}

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
