# Job Scheduler - Detailed Phase 2 and Phase 3 Design

This is the part that turns a basic scheduling problem into a real interview-level design answer.

Phase 1 is about order. Phase 2 is about reliability. Phase 3 is about scale and distributed coordination.

---

## 1. Why Phase 2 is required

In Phase 1, the questions are simple:
- Which job runs first?
- Which strategy is used?
- How are jobs distributed across workers?

That is enough for a toy system, but not for a real system.

In real systems, we must also ask:
- What if a job fails unexpectedly?
- What if one worker crashes in the middle?
- Should the scheduler retry the job automatically?
- How many times should we retry?
- Can the same job be processed twice by mistake?
- How do we know what happened last time?

These are the exact reasons we add Phase 2.

---

## 2. What is a lock?

A lock is a synchronization mechanism.

It ensures that only one thread or one machine is allowed to do a critical action at a time.

### Simple example

```java
public class SimpleLock {
    private boolean locked = false;

    public synchronized boolean tryAcquire() {
        if (locked) {
            return false;
        }
        locked = true;
        return true;
    }

    public synchronized void release() {
        locked = false;
    }
}
```

### Why do we need it?

Because many workers can read the same queue at the same time.

If two workers both pick the same job, they may both execute it.

That creates duplicate work.

### Real-life analogy

Think of a lock like a bathroom key.

Only one person can hold the key at a time.

If the key is not controlled, two people may walk in at the same time, which causes chaos.

In job scheduling, the “key” is the right to process a single job.

---

## 3. Race condition explained simply

A race condition happens when two or more workers try to access shared state at the same time and the final result depends on timing.

### Example

- Worker 1 reads job A
- Worker 2 reads job A
- Both believe they own the job
- Both execute the same logic

The result can be:
- duplicate emails
- double payment processing
- duplicate reports
- inconsistent database updates

This is exactly why locks are needed.

---

## 4. What is a distributed lock?

A local lock works inside one JVM or one machine.

A distributed lock works across multiple machines or processes.

### Example

Suppose you have 3 scheduler servers.

- Scheduler 1 picks job X
- Scheduler 2 also picks job X
- Scheduler 3 also picks job X

Without a distributed lock, all three can process the same job.

This is not acceptable in production.

### Typical distributed lock sources

- Redis distributed lock
- database row lock
- Zookeeper lock
- coordinator service

The key idea is the same:

Only one node can hold the lock for a given job ID at a time.

This prevents duplicate execution at scale.

---

## 5. Phase 2 design in practical terms

### 5.1 Job status lifecycle

A job should not just be “present” or “absent”. It should have a state.

```java
public enum JobStatus {
    PENDING,
    RUNNING,
    SUCCESS,
    FAILED,
    RETRYING
}
```

This answers questions like:
- Is it waiting to run?
- Is it currently executing?
- Did it fail?
- Is it being retried?
- Did it finish successfully?

### 5.2 Retry policy

If a job fails due to a temporary cause like a network issue, we usually retry it.

But we do not retry forever.

We put a maximum retry limit and a delay.

```java
public class RetryPolicy {
    private final int maxAttempts;
    private final long initialDelayInSeconds;
    private final double backoffMultiplier;

    public RetryPolicy(int maxAttempts, long initialDelayInSeconds, double backoffMultiplier) {
        this.maxAttempts = maxAttempts;
        this.initialDelayInSeconds = initialDelayInSeconds;
        this.backoffMultiplier = backoffMultiplier;
    }

    public long nextDelaySeconds(int attemptNumber) {
        if (attemptNumber <= 0) {
            return initialDelayInSeconds;
        }
        return (long) (initialDelayInSeconds * Math.pow(backoffMultiplier, attemptNumber - 1));
    }
}
```

This is called exponential backoff.

Example:
- attempt 1 fails -> wait 5s
- attempt 2 fails -> wait 10s
- attempt 3 fails -> wait 20s

This prevents retry storms.

### 5.3 Execution history

We also keep a log of job execution.

```java
public class JobExecutionRecord {
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
```

This helps when someone asks:
- Why did this job fail?
- How many times did it retry?
- What was the final status?
- When did it run last time?

This is important in real systems because production problems are not solved by guessing.

---

## 6. What is Phase 3 really doing?

Phase 3 is the distributed version of the design.

A single scheduler process is no longer enough.

### We add:
- multiple scheduler nodes
- multiple workers
- a shared queue
- distributed coordination
- persistent job storage or repository
- lock management across nodes
- monitoring and metrics

### Core idea

The system becomes bigger than a single machine.

Now the main questions become:
- How do multiple machines share work safely?
- How do they avoid running the same job twice?
- What if one scheduler crashes while another keeps working?
- How is state persisted so jobs are not lost?

These are Phase 3 concerns.

---

## 7. Single-file Java example for interview learning

This is a clean, beginner-friendly, single-file example showing the exact concepts you asked for.

```java
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
```

---

## 8. Why this is the right interview answer

This is the structure a strong interviewer expects:

1. Explain the current problem clearly.
2. Explain the basic design for Phase 1.
3. Explain the real-world need for Phase 2.
4. Explain the reason for locks and retries.
5. Explain why Phase 3 starts to require distributed coordination.
6. Talk about duplicate execution and exactly-once semantics.
7. Show that you understand operational correctness, not just sorting.

---

## 9. Final interview answer to memorize

> Phase 1 solves the ordering problem by choosing which job goes first. Phase 2 adds job states, retry policies, and execution history so that failed jobs are handled correctly and can be audited. Phase 3 moves the scheduler to a distributed system where multiple workers and multiple machines exist. At that point, locks become critical because without them, the same job can be picked by multiple workers and executed twice. So the real design is: scheduler for ordering, retry state for reliability, and distributed locks plus worker coordination for scale.

This is the answer that shows you understand both the algorithm and the production system.
