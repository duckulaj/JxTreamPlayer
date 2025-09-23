package com.hawkins.xtreamjson.component;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Aspect
@Component
@Slf4j
@ConditionalOnExpression("${aspect.enabled:true}")
public class ExecutionTimeAdvice {

    @Around("@annotation(com.hawkins.xtreamjson.annotations.TrackExecutionTime)")
    public Object executionTime(ProceedingJoinPoint point) throws Throwable {
        long startTime = System.nanoTime();
        Object object = point.proceed();
        long endTime = System.nanoTime();

        log.info(String.format("Class Name: %s. Method Name: %s. Time taken for Execution is : %d ms",
                point.getSignature().getDeclaringTypeName(),
                point.getSignature().getName(),
                (endTime - startTime) / 1_000_000));
        return object;
    }
}