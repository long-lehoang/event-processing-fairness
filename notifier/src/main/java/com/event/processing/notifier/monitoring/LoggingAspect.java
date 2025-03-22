package com.event.processing.notifier.monitoring;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Aspect
@Component
public class LoggingAspect {

  @Around("execution(* com.event.processing.notifier..*(..))")
  public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
    long start = System.currentTimeMillis();

    // Extract method name & parameters
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    String methodName = signature.getDeclaringTypeName() + "." + signature.getName();
    Object[] args = joinPoint.getArgs();

    log.info("Executing: {} with args: {}", methodName, Arrays.toString(args));

    try {
      Object result = joinPoint.proceed(); // Execute method
      long executionTime = System.currentTimeMillis() - start;

      log.info("Completed: {} in {} ms", methodName, executionTime);
      return result;
    } catch (Exception ex) {
      log.error("Error in {}: {}", methodName, ex.getMessage(), ex);
      throw ex;
    }
  }
}