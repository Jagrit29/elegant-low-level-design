# Job Scheduler - Full Interview Guide

**Level**: Intermediate to Senior  
**Best benchmark**: This is the level of depth expected for a strong LLD interview.

---

## 1. What is a Job Scheduler?
A Job Scheduler decides the execution order of tasks. In interviews, this is a classic modeling problem because it looks simple but tests good design judgment.

Think of it like a hospital triage desk:
- some jobs are urgent
- some are long-running
- some must finish before a deadline
- some should be distributed across workers

A scheduler decides which job goes next and to which worker it should be assigned.

---

## 2. Real-world examples
- email notification systems
- CI/CD pipelines
- video rendering jobs
- ETL pipelines
- cron jobs
- batch processing in warehouses

---

## 3. Problem statement
Design a scheduler that accepts a set of jobs and assigns them to worker threads based on a scheduling policy.

Each job may include:
- name
- duration
- priority
- deadline
- user type
- arrival order

We need to support different policies:
- FCFS
- SJF
- Priority Scheduling
- Earliest Deadline First

---

## 4. Interview approach
A strong answer should start with clarifying questions:
- Is this single-machine or distributed?
- Are jobs recurring or one-time?
- What happens on job failure?
- Are there different user types or roles?
- Should we handle retries and auditing?

This shows the interviewer that you are designing for real systems, not just toy logic.

---

## 5. Phase 1: Core design

### Core entities
- Job
- SchedulingStrategy
- Scheduler
- Worker

### Example design
```java
public class Job {
    private final String name;
    private final int duration;
    private final Priority priority;
    private final int deadline;
    private final UserType userType;
    private final int arrivalOrder;

    public Job(String name, int duration, Priority priority, int deadline,
               UserType userType, int arrivalOrder) {
        this.name = name;
        this.duration = duration;
        this.priority = priority;
        this.deadline = deadline;
        this.userType = userType;
        this.arrivalOrder = arrivalOrder;
    }

    public String getName() { return name; }
    public int getDuration() { return duration; }
    public Priority getPriority() { return priority; }
    public int getDeadline() { return deadline; }
    public UserType getUserType() { return userType; }
    public int getArrivalOrder() { return arrivalOrder; }
}
```

```java
public interface SchedulingStrategy {
    List<Job> schedule(List<Job> jobs);
}
```

```java
public class Scheduler {
    private List<Job> jobs;
    private SchedulingStrategy strategy;

    public Scheduler(List<Job> jobs, SchedulingStrategy strategy) {
        this.jobs = jobs;
        this.strategy = strategy;
    }

    public void setStrategy(SchedulingStrategy strategy) {
        this.strategy = strategy;
    }

    public List<List<Job>> getSchedulingSequence(int numberOfThreads) {
        List<Job> ordered = strategy.schedule(jobs);
        List<List<Job>> result = new ArrayList<>();
        for (int i = 0; i < numberOfThreads; i++) {
            result.add(new ArrayList<>());
        }

        for (int i = 0; i < ordered.size(); i++) {
            int threadIndex = i % numberOfThreads;
            result.get(threadIndex).add(ordered.get(i));
        }

        return result;
    }
}
```

### Why this is the right design
The scheduler does not know how ordering is implemented. It just delegates to a strategy. This is the Strategy Pattern.

---

## 6. Phase 2: Retry, status, recurring jobs, history
Once the scheduler works, the real system adds operational concerns.

### New requirements
- retry on failure
- track status of each job
- support recurring jobs
- keep execution history
- handle timeouts and dead letters

### New entities
- JobStatus
- JobInstance
- RetryPolicy
- ExecutionHistory
- RecurringJob

### Example code
```java
public enum JobStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    RETRYING
}

public class RetryPolicy {
    private final int maxAttempts;
    private final long initialDelaySeconds;
    private final double backoffMultiplier;

    public RetryPolicy(int maxAttempts, long initialDelaySeconds, double backoffMultiplier) {
        this.maxAttempts = maxAttempts;
        this.initialDelaySeconds = initialDelaySeconds;
        this.backoffMultiplier = backoffMultiplier;
    }

    public int getMaxAttempts() { return maxAttempts; }
    public long getInitialDelaySeconds() { return initialDelaySeconds; }
    public double getBackoffMultiplier() { return backoffMultiplier; }
}

public class JobInstance {
    private final String jobId;
    private JobStatus status;
    private int attemptCount;

    public JobInstance(String jobId) {
        this.jobId = jobId;
        this.status = JobStatus.PENDING;
        this.attemptCount = 0;
    }

    public void markRunning() { status = JobStatus.RUNNING; }
    public void markCompleted() { status = JobStatus.COMPLETED; }
    public void markFailed() { status = JobStatus.FAILED; }
    public void incrementAttempt() { attemptCount++; }
}
```

### Why this matters
A system is not “done” once scheduling order is correct. Real systems need retry semantics and replayability.

---

## 7. Phase 3: Distributed scheduler
Now we are at senior-level design.

### New concerns
- multiple scheduler nodes
- persistent queue
- worker coordination
- lock management
- metrics and monitoring
- cron jobs and recurring schedules

### New entities
- DistributedLock
- WorkerPool
- PersistentJobRepository
- SchedulerNode
- CronExpression
- JobQueue
- Metrics

### Example code
```java
public interface DistributedLock {
    boolean tryAcquire(String key, long timeoutMs);
    void release(String key);
}

public class WorkerPool {
    private final int size;
    private final List<Thread> workers = new ArrayList<>();

    public WorkerPool(int size) {
        this.size = size;
    }

    public void start() {
        for (int i = 0; i < size; i++) {
            Thread worker = new Thread(() -> {
                // process jobs
            });
            workers.add(worker);
            worker.start();
        }
    }
}

public class CronExpression {
    private final String expression;

    public CronExpression(String expression) {
        this.expression = expression;
    }

    public boolean matches(long timestamp) {
        return true;
    }
}
```

### Why this matters
In distributed systems, multiple scheduler instances can race to execute the same job. You need a lock or a repository that guarantees single execution.

---

## 8. Design patterns used

### Strategy Pattern
This is the main pattern. Each scheduling algorithm implements the same interface.

### Factory Pattern
For creating different strategy implementations based on config.

### Observer Pattern
For job state notifications and monitoring events.

---

## 9. OOP and SOLID principles

### Encapsulation
Job data and scheduling state are hidden behind proper methods.

### Abstraction
Viewers of the scheduler do not need to know how the strategy works internally.

### Polymorphism
Different scheduling strategies all satisfy the same interface.

### Single Responsibility
- Job stores data
- Scheduler orchestrates
- Strategy decides ordering
- RetryPolicy handles retries

### Open/Closed Principle
Add a new scheduling algorithm without changing existing scheduler logic.

---

## 10. Interview Q&A

### Q1. Why use Strategy Pattern?
Because scheduler behavior can change without changing worker coordination logic. This is a classic OCP example.

### Q2. What happens in Phase 2?
We add monitoring, history, retries, and recurring jobs. The scheduler becomes operationally safe.

### Q3. What happens in Phase 3?
We add distributed execution, persistence, coordination, and lock management.

### Q4. Why is a retry policy separate from scheduler logic?
Because retry rules are a policy concern, not a scheduling concern. They change independently.

### Q5. Why not use one big class with if-else?
Because that class becomes impossible to maintain and violates the Open/Closed Principle.

---

## 11. Edge cases
- empty job list
- equal priority and duration
- jobs with impossible deadlines
- scheduler crash after starting a job
- duplicate job creation
- dead-letter jobs after max retries
- multiple workers picking same job
- high retry storms

---

## 12. Final assessment
This is one of the strongest LLD problems because it evolves naturally:
- Phase 1: algorithmic ordering
- Phase 2: operational reliability
- Phase 3: distributed coordination

That progression is exactly what interviewers want to see in a senior-level answer.
