package org.autorabit.salesforcecontextgraph.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class ThreadConfig {

    @Bean("loadDependenciesExecutor")
    public ThreadPoolTaskExecutor loadDependenciesExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(100);
        executor.setMaxPoolSize(100);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("loadDependenciesExecutor-");
        executor.initialize();
        return executor;
    }
}
