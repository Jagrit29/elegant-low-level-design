package lowleveldesign.systems.jobscheduler;

import java.util.*;
import java.util.concurrent.*;

public class JobSchedulerPhase2And3Demo {

    enum Priority {
        P0, P1, P2
    }

    enum JobStatus {
        PENDING,
        RUNNING,
        SUCCESS,
        FAILED,
        RETRYING
    }

    static class Job {
        private final String id;
        private final String name;
        private final int durationInSeconds;
        private final Priority priority;
        private JobStatus status;
        private int attemptCount;

        public Job(String id, String name, int durationInSeconds, Priority priority) {
            this.id = id;
            this.name = name;
            this.durationInSeconds = durationInSeconds;
            this.priority = priority;
            this.status = JobStatus.PENDING;
            this.attemptCount = 0;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public int getDurationInSeconds() {
            return durationInSeconds;
        }

        public Priority getPriority() {
            return priority;
        }

        public JobStatus getStatus() {
            return status;
        }

        public void setStatus(JobStatus status) {
            this.status = status;
        }

        public int getAttemptCount() {
            return attemptCount;
        }

        public void incrementAttempt() {
            attemptCount++;
        }
    }

    static class RetryPolicy {
        private final int maxAttempts;
        private final long initialDelayInSeconds;
        private final double backoffMultiplier;

        public RetryPolicy(int maxAttempts, long initialDelayInSeconds, double backoffMultiplier) {
            this.maxAttempts = maxAttempts;
            this.initialDelayInSeconds = initialDelayInSeconds;
            this.backoffMultiplier = backoffMultiplier;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public long nextDelaySeconds(int attemptNumber) {
            if (attemptNumber <= 0) {
                return initialDelayInSeconds;
            }
            return (long) (initialDelayInSeconds * Math.pow(backoffMultiplier, attemptNumber - 1));
        }
    }

    static class JobExecutionRecord {
        private final String jobId;
        private final String jobName;
        private final JobStatus status;
        private final int attemptCount;
        private final long startedAt;
        private final long finishedAt;

        public JobExecutionRecord(String jobId, String jobName, JobStatus status,
                                 int attemptCount, long startedAt, long finishedAt) {
            this.jobId = jobId;
            this.jobName = jobName;
            this.status = status;
            this.attemptCount = attemptCount;
            this.startedAt = startedAt;
            this.finishedAt = finishedAt;
        }
    }

    static class DistributedLock {
        private final Set<String> activeJobIds = new HashSet<>();

        public synchronized boolean tryAcquire(String jobId) {
            if (activeJobIds.contains(jobId)) {
                return false;
            }
            activeJobIds.add(jobId);
            return true;
        }

        public synchronized void release(String jobId) {
            activeJobIds.remove(jobId);
        }
    }

    static class Scheduler {
        private final List<Job> jobs = new ArrayList<>();
        private final RetryPolicy retryPolicy = new RetryPolicy(3, 5, 2.0);
        private final DistributedLock lock = new DistributedLock();
        private final Map<String, JobExecutionRecord> history = new HashMap<>();

        public void addJob(Job job) {
            jobs.add(job);
        }

        public void run() {
            for (Job job : jobs) {
                processJob(job);
            }
        }

        private void processJob(Job job) {
            job.incrementAttempt();
            job.setStatus(JobStatus.RUNNING);

            boolean acquired = lock.tryAcquire(job.getId());
            if (!acquired) {
                System.out.println("Another scheduler already owns " + job.getName());
                job.setStatus(JobStatus.RETRYING);
                return;
            }

            try {
                System.out.println("Processing job: " + job.getName() + " on thread " + Thread.currentThread().getName());

                if (job.getName().contains("fail")) {
                    throw new RuntimeException("Temporary issue");
                }

                job.setStatus(JobStatus.SUCCESS);
                recordHistory(job, JobStatus.SUCCESS);
                System.out.println("Job succeeded: " + job.getName());
            } catch (Exception e) {
                handleFailure(job);
            } finally {
                lock.release(job.getId());
            }
        }

        private void handleFailure(Job job) {
            int currentAttempt = job.getAttemptCount();
            if (currentAttempt < retryPolicy.getMaxAttempts()) {
                long delay = retryPolicy.nextDelaySeconds(currentAttempt);
                job.setStatus(JobStatus.RETRYING);
                recordHistory(job, JobStatus.RETRYING);
                System.out.println("Job failed: " + job.getName() + ". Retrying after " + delay + " seconds.");
            } else {
                job.setStatus(JobStatus.FAILED);
                recordHistory(job, JobStatus.FAILED);
                System.out.println("Job permanently failed: " + job.getName());
            }
        }

        private void recordHistory(Job job, JobStatus status) {
            long now = System.currentTimeMillis();
            JobExecutionRecord record = new JobExecutionRecord(
                    job.getId(),
                    job.getName(),
                    status,
                    job.getAttemptCount(),
                    now,
                    now
            );
            history.put(job.getId(), record);
        }
    }

    static class WorkerPool {
        private final BlockingQueue<Job> queue = new LinkedBlockingQueue<>();
        private final int size;
        private final List<Thread> workers = new ArrayList<>();
        private volatile boolean running = true;

        public WorkerPool(int size) {
            this.size = size;
        }

        public void submit(Job job) {
            queue.offer(job);
        }

        public void start() {
            for (int i = 0; i < size; i++) {
                Thread worker = new Thread(() -> {
                    while (running || !queue.isEmpty()) {
                        try {
                            Job job = queue.poll(500, TimeUnit.MILLISECONDS);
                            if (job == null) {
                                continue;
                            }
                            System.out.println("Worker took job: " + job.getName());
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                });
                workers.add(worker);
                worker.start();
            }
        }

        public void shutdown() {
            running = false;
            for (Thread worker : workers) {
                worker.interrupt();
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("=== Why locks are needed ===");
        System.out.println("Without a lock, the same job can be picked by two workers.");
        System.out.println("That causes duplicate execution.\n");

        DistributedLock lock = new DistributedLock();
        String jobId = "J-100";

        System.out.println("Try acquiring lock for job J-100: " + lock.tryAcquire(jobId));
        System.out.println("Try acquiring lock again for same job: " + lock.tryAcquire(jobId));
        lock.release(jobId);
        System.out.println();

        System.out.println("=== Phase 2: retry + job state ===");
        Scheduler scheduler = new Scheduler();
        scheduler.addJob(new Job("J1", "send-email", 5, Priority.P1));
        scheduler.addJob(new Job("J2", "fail-once-job", 2, Priority.P0));
        scheduler.addJob(new Job("J3", "generate-report", 4, Priority.P2));
        scheduler.run();
        System.out.println();

        System.out.println("=== Phase 3: distributed worker model ===");
        System.out.println("Real systems do not use one single worker.");
        System.out.println("Multiple workers read from a shared queue and a lock prevents duplicate execution.");

        WorkerPool pool = new WorkerPool(2);
        pool.submit(new Job("W1", "sync-users", 2, Priority.P0));
        pool.submit(new Job("W2", "sync-orders", 3, Priority.P1));
        pool.start();

        try {
            Thread.sleep(600);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        pool.shutdown();
    }
}
