package com.event.processing.dlq_service.config;

import lombok.RequiredArgsConstructor;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configuration for thread pools used in the application.
 * This class centralizes thread pool configuration to ensure proper resource management.
 */
@Configuration
@EnableAsync(proxyTargetClass = true)
@RequiredArgsConstructor
public class ThreadPoolConfig implements AsyncConfigurer {

  private final AsyncExceptionHandler asyncExceptionHandler;
  @Value("${thread-pool.retry.core-size:4}")
  private int retryPoolCoreSize;
  @Value("${thread-pool.retry.max-size:10}")
  private int retryPoolMaxSize;
  @Value("${thread-pool.retry.queue-capacity:100}")
  private int retryPoolQueueCapacity;
  @Value("${thread-pool.retry.keep-alive-seconds:60}")
  private int retryPoolKeepAliveSeconds;
  @Value("${thread-pool.retry.thread-name-prefix:retry-task-}")
  private String retryPoolThreadNamePrefix;

  /**
   * Creates a thread pool for retry operations.
   * This pool is configured with a rejection policy that logs and aborts the caller.
   *
   * @return A configured ThreadPoolTaskExecutor
   */
  @Bean(name = "retryTaskExecutor")
  public Executor retryTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

    // Core configuration
    executor.setCorePoolSize(retryPoolCoreSize);
    executor.setMaxPoolSize(retryPoolMaxSize);
    executor.setQueueCapacity(retryPoolQueueCapacity);
    executor.setKeepAliveSeconds(retryPoolKeepAliveSeconds);
    executor.setThreadNamePrefix(retryPoolThreadNamePrefix);

    // Rejection policy
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

    // Wait for tasks to complete on shutdown, but with a reasonable timeout
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(30); // 30 seconds should be enough for most tasks

    // Allow core threads to time out
    executor.setAllowCoreThreadTimeOut(true);

    return executor;
  }

  /**
   * Configures the default async executor.
   * This method is called by Spring to determine the executor to use for @Async methods
   * when no specific executor is specified.
   *
   * @return The default executor for @Async methods
   */
  @Override
  public Executor getAsyncExecutor() {
    // Use the retryTaskExecutor as the default executor for @Async methods
    return retryTaskExecutor();
  }

  /**
   * Configures the exception handler for async methods.
   * This handler is called when an @Async method throws an exception.
   *
   * @return The exception handler for async methods
   */
  @Override
  public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
    return asyncExceptionHandler;
  }
}
