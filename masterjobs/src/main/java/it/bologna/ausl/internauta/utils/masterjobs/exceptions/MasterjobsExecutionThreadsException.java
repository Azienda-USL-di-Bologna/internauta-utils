package it.bologna.ausl.internauta.utils.masterjobs.exceptions;

/**
 *
 * @author gdm
 */
public class MasterjobsExecutionThreadsException extends Exception {
    private Long jobId;
    private String jobName;

    public MasterjobsExecutionThreadsException(String message) {
        super(message);
    }

    public MasterjobsExecutionThreadsException(Throwable cause) {
        super(cause);
    }

    public MasterjobsExecutionThreadsException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public MasterjobsExecutionThreadsException(Long jobId, String jobName, String message) {
        super(message);
        this.jobId = jobId;
        this.jobName = jobName;
    }

    public MasterjobsExecutionThreadsException(Long jobId, String jobName, String message, Throwable cause) {
        super(message, cause);
        this.jobId = jobId;
        this.jobName = jobName;
    }

    public MasterjobsExecutionThreadsException(Long jobId, String jobName, Throwable cause) {
        super(cause);
        this.jobId = jobId;
        this.jobName = jobName;
    }

    public Long getJobId() {
        return jobId;
    }

    public String getJobName() {
        return jobName;
    }
}
