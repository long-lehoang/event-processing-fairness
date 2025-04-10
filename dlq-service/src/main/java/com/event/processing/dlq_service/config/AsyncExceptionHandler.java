package com.event.processing.dlq_service.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Custom exception handler for asynchronous methods.
 * This handler logs exceptions thrown by @Async methods.
 */
@Slf4j
@Component
public class AsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

  /**
   * Handles exceptions thrown by asynchronous methods.
   *
   * @param ex     The exception thrown
   * @param method The method that threw the exception
   * @param params The parameters passed to the method
   */
  @Override
  public void handleUncaughtException(Throwable ex, Method method, Object... params) {
    String className = method.getDeclaringClass().getSimpleName();
    String methodName = method.getName();
    String paramString = Arrays.toString(params);

    log.error("Async method '{}' in class '{}' with parameters {} threw exception: {}",
        methodName, className, paramString, ex.getMessage(), ex);
  }
}
