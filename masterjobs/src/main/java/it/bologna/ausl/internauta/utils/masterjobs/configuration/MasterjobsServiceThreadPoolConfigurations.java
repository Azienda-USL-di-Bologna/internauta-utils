package it.bologna.ausl.internauta.utils.masterjobs.configuration;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MasterjobsServiceThreadPoolConfigurations {

    private static final Logger log = LoggerFactory.getLogger(MasterjobsServiceThreadPoolConfigurations.class);

    @Value("${masterjobs.manager.services-executor.scheduled-threads.pool-size}")
    private String poolSize;

    @Bean(name = "masterjobsScheduledThreadPoolExecutor")
    public ScheduledThreadPoolExecutor masterjobsScheduledThreadPoolExecutor() {
        log.info("masterjobs.manager.services-executor.scheduled-threads.pool-size: " + poolSize);
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(Integer.parseInt(poolSize));
        executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        executor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        return executor;
    }

}