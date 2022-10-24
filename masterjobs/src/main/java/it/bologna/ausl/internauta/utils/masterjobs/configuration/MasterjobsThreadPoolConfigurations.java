package it.bologna.ausl.internauta.utils.masterjobs.configuration;

//package it.bologna.ausl.internauta.service.masterjobs.configuration;
//
//import java.util.concurrent.ScheduledThreadPoolExecutor;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
///**
// *
// * @author gdm
// */
//@Configuration
//public class MasterjobsThreadPoolConfigurations {
//
//    private static final Logger log = LoggerFactory.getLogger(MasterjobsThreadPoolConfigurations.class);
//
//    @Value("${internauta.scheduled-thread-pool-executor.pool-size}")
//    String poolSize;
//
//    @Bean
//    public ScheduledThreadPoolExecutor scheduledThreadPoolExecutor() {
//        log.info("internauta.scheduled-thread-pool-executor.pool-size: " + poolSize);
//        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(Integer.parseInt(poolSize));
//        executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
//        executor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
//        return executor;
//    }
//
//
//}
