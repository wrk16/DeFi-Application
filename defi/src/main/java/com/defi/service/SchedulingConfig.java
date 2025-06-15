package com.defi.service;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * Configuration class to enable and configure Spring Boot scheduling
 * This enables the @Scheduled annotations to work
 */
@Configuration
@EnableScheduling
public class SchedulingConfig implements SchedulingConfigurer {

    /**
     * Configure the task scheduler with a thread pool
     * This prevents blocking the main application thread
     */
    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(2); // Number of threads for scheduled tasks
        taskScheduler.setThreadNamePrefix("loan-automation-");
        taskScheduler.setWaitForTasksToCompleteOnShutdown(true);
        taskScheduler.setAwaitTerminationSeconds(60);
        taskScheduler.initialize();
        
        taskRegistrar.setTaskScheduler(taskScheduler);
    }
}