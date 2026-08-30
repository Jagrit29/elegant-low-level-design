package lowleveldesign.systems.jobscheduler;

public class JobExecutionRecord {
    private final String jobId;
    private final String jobName;
    private final JobStatus status;
    private final int attemptCount;
    private final long startedAt;
    private final long finishedAt;

    public JobExecutionRecord(String jobId,
                             String jobName,
                             JobStatus status,
                             int attemptCount,
                             long startedAt,
                             long finishedAt) {
        this.jobId = jobId;
        this.jobName = jobName;
        this.status = status;
        this.attemptCount = attemptCount;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
    }

    public String getJobId() {
        return jobId;
    }

    public String getJobName() {
        return jobName;
    }

    public JobStatus getStatus() {
        return status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public long getFinishedAt() {
        return finishedAt;
    }
}
