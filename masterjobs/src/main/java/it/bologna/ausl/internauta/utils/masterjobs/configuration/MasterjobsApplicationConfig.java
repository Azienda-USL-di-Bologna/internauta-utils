package it.bologna.ausl.internauta.utils.masterjobs.configuration;

import java.net.DatagramSocket;
import java.net.InetAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author gdm
 */
@Configuration
public class MasterjobsApplicationConfig {
    private static final Logger log = LoggerFactory.getLogger(MasterjobsApplicationConfig.class);
    
    @Value("${masterjobs.manager.use-debugging-options:false}")
    private boolean useDebuggingOptions;
    
    @Value("${masterjobs.manager.jobs-executor.redis-active-threads-set-name}")
    private String activeThreadsSetName;
    
    @Value("${masterjobs.manager.jobs-executor.commands-stream-name}")
    private String commandsStreamName;
    
    @Value("${masterjobs.manager.jobs-executor.in-redis-queue-normal}")
    private String inQueueNormal;
    
    @Value("${masterjobs.manager.jobs-executor.in-redis-queue-high}")
    private String inQueueHigh;
    
    @Value("${masterjobs.manager.jobs-executor.in-redis-queue-highest}")
    private String inQueueHighest;
    
    @Value("${masterjobs.manager.jobs-executor.work-redis-queue}")
    private String workQueue;
    
    @Value("${masterjobs.manager.jobs-executor.error-redis-queue}")
    private String errorQueue;
    
    @Value("${masterjobs.manager.jobs-executor.wait-redis-queue}")
    private String waitQueue;
    
    @Value("${masterjobs.manager.jobs-executor.out-redis-queue}")
    private String outQueue;
    
    @Value("${masterjobs.manager.jobs-executor.sleep-millis}")
    private int sleepMillis;
    
    @Value("${masterjobs.manager.jobs-executor.queue-read-timeout-millis}")
    private int queueReadTimeoutMillis;
    
    @Value("${masterjobs.manager.jobs-executor.normal-priority-threads-number}")
    private int normalPriorityThreadsNumber;
    
    @Value("${masterjobs.manager.jobs-executor.high-priority-threads-number}")
    private int highPriorityThreadsNumber;
    
    @Value("${masterjobs.manager.jobs-executor.highest-priority-threads-number}")
    private int highestPriorityThreadsNumber;
    
    @Value("${masterjobs.manager.jobs-executor.wait-queue-threads-number}")
    private Integer waitQueueThreadsNumber;
    
    @Value("${server.port}")
    private Integer port;
    
    private String machineIp;
    

    public boolean isUseDebuggingOptions() {
        return useDebuggingOptions;
    }

    public void setUseDebuggingOptions(boolean useDebuggingOptions) {
        this.useDebuggingOptions = useDebuggingOptions;
    }

    public String getActiveThreadsSetName() {
        return activeThreadsSetName;
    }

    public String getCommandsStreamName() {
        return commandsStreamName;
    }

    public String getInQueueNormal() {
        return inQueueNormal;
    }

    public String getInQueueHigh() {
        return inQueueHigh;
    }

    public String getInQueueHighest() {
        return inQueueHighest;
    }

    public String getWorkQueue() {
        return workQueue;
    }

    public String getErrorQueue() {
        return errorQueue;
    }

    public String getWaitQueue() {
        return waitQueue;
    }

    public String getOutQueue() {
        return outQueue;
    }

    public int getSleepMillis() {
        return sleepMillis;
    }

    public int getQueueReadTimeoutMillis() {
        return queueReadTimeoutMillis;
    }

    public int getNormalPriorityThreadsNumber() {
        return normalPriorityThreadsNumber;
    }

    public int getHighPriorityThreadsNumber() {
        return highPriorityThreadsNumber;
    }

    public int getHighestPriorityThreadsNumber() {
        return highestPriorityThreadsNumber;
    }

    public Integer getWaitQueueThreadsNumber() {
        return waitQueueThreadsNumber;
    }
    
    public Integer getHttpPort() {
        return port;
    }
    

    public String getMachineIp() {
        /* 
        calcola l'indirizzo ip della macchina
        codice preso da: https://stackoverflow.com/a/38342964
        */
        if (this.machineIp == null) {
            try(final DatagramSocket socket = new DatagramSocket()){
                socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
                this.machineIp = socket.getLocalAddress().getHostAddress();
            } catch (Exception ex) {
                log.error("error retrieving machine ip", ex);
            }
        }
        return this.machineIp;
    }
}
