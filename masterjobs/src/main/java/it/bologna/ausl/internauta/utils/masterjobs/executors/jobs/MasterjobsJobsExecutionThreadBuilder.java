package it.bologna.ausl.internauta.utils.masterjobs.executors.jobs;

/**
 *
 * @author gdm
 */
public interface MasterjobsJobsExecutionThreadBuilder {
    public MasterjobsJobsExecutionThread self(MasterjobsJobsExecutionThread self);
    public MasterjobsJobsExecutionThread activeThreadsSetName(String activeThreadsSetName);
    public MasterjobsJobsExecutionThread commandsStreamName(String commandsStreamName);
    public MasterjobsJobsExecutionThread inQueueNormal(String inQueueNormal);
    public MasterjobsJobsExecutionThread inQueueHigh(String inQueueNormal);
    public MasterjobsJobsExecutionThread inQueueHighest(String inQueueNormal);
    public MasterjobsJobsExecutionThread workQueue(String workQueue);
    public MasterjobsJobsExecutionThread errorQueue(String errorQueue);
    public MasterjobsJobsExecutionThread waitQueue(String waitQueue);
    public MasterjobsJobsExecutionThread outQueue(String outQueue);
    public MasterjobsJobsExecutionThread sleepMillis(int sleepMillis);
    public MasterjobsJobsExecutionThread queueReadTimeoutMillis(int queueReadTimeoutMillis);
    public MasterjobsJobsExecutionThread useDebuggingOptions(boolean useDebuggingOptions);
    public MasterjobsJobsExecutionThread ip(String ip);
}
