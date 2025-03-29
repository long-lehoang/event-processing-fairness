package com.event.processing.notifier.monitoring;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Aspect-oriented logging component for monitoring method execution times and
 * parameters.
 * This class provides cross-cutting logging functionality across the
 * application,
 * automatically logging method entry, exit, execution time, and any exceptions.
 * <p>
 * Key features:
 * - Method execution time tracking
 * - Parameter logging
 * - Exception logging
 * - Automatic logging for all methods in the application package
 *
 * @author LongLe
 * @version 1.0
 */
@Slf4j
@Aspect
@Component
public class LoggingAspect {

  /**
   * Around advice that logs method execution details.
   * This method intercepts all method executions in the application package and
   * logs:
   * - Method name and parameters before execution
   * - Execution time after successful completion
   * - Any exceptions that occur during execution
   *
   * @param joinPoint The join point representing the method being executed
   * @return The result of the method execution
   * @throws Throwable Any exception that occurs during method execution
   */
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