package it.bologna.ausl.internauta.utils.masterjobs.exceptions;

/**
 *
 * @author gdm
 */
public class MasterjobsReadQueueTimeout extends Exception {

    private final Integer timeoutMillis;
    private final String queue;
    
    public MasterjobsReadQueueTimeout(String queue, Integer timeoutMillis) {
        super();
        this.queue = queue;
        this.timeoutMillis = timeoutMillis;
    }

    public Integer getTimeoutMillis() {
        return timeoutMillis;
    }

    public String getQueue() {
        return queue;
    }
}
