package com.srijan.portfolio.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class AsyncConfig {

    @Bean(name = "resumeTaskExecutor")
    public TaskExecutor resumeTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("resume-job-");
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(50);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    @Bean(destroyMethod = "shutdown")
    public ExecutorService aiOrchestratorExecutor() {
        return Executors.newFixedThreadPool(4, runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("ai-orchestrator");
            thread.setDaemon(true);
            return thread;
        });
    }
}
