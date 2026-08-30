# Job Scheduler - Detailed LLD Study

## Overview
This is a classic low-level design interview problem. It teaches scheduling logic, design patterns, and how a simple object model evolves into a production-grade system.

The goal is not just to sort jobs; it is to design an extensible scheduler that can evolve from a single machine to a distributed execution engine.

---

## Problem statement
Build a scheduler that accepts jobs, decides their execution order, and distributes work across worker threads.

A job may include:
- name
- duration
- priority
- deadline
- user type
- arrival order

The system must support different scheduling policies such as:
- FCFS
- SJF
- Priority Scheduling
- Earliest Deadline First

---

## Phase 1: Core design
### Main entities
- Job
- SchedulingStrategy
- Scheduler
- Worker
- JobComparator or strategy implementation

### Core behavior
- add a job
- choose scheduling policy
- order jobs based on policy
- assign jobs to threads using round robin
- return execution sequence

### Key design decision
The scheduler should depend on a strategy interface instead of hard-coded if-else logic.

---

## Phase 2: Operational realism
Real systems require more than ordering jobs. They need lifecycle tracking and retry logic.

### New entities
- JobStatus
- JobInstance
- RetryPolicy
- ExecutionHistory
- RecurringJob
- JobRepository

### New behavior
- track job states: PENDING, RUNNING, COMPLETED, FAILED
- retry failed jobs
- persist history for debugging and audit
- support recurring jobs
- expose job execution timeline

### Example Java code
```java
public enum JobStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED
}

public class RetryPolicy {
    private int maxAttempts;
    private long initialDelayInSeconds;
    private double backoffMultiplier;

    public int getMaxAttempts() { return maxAttempts; }
    public long getInitialDelayInSeconds() { return initialDelayInSeconds; }
    public double getBackoffMultiplier() { return backoffMultiplier; }
}

public class JobInstance {
    private String jobId;
    private JobStatus status;
    private int attemptCount;
    private long createdAt;
    private long updatedAt;

    public JobStatus getStatus() { return status; }
    public void setStatus(JobStatus status) { this.status = status; }
}
```

### Why this is Phase 2
It adds failure handling, visibility, and operational robustness without changing the core scheduling idea.

---

## Phase 3: Distributed scheduler
A large system cannot rely on a single machine. The scheduler must now be resilient and coordinated across nodes.

### New entities
- DistributedLock
- WorkerPool
- PersistentJobRepository
- SchedulerNode
- CronExpression
- Metrics
- JobQueue

### New behavior
- coordinate workers across machines
- persist jobs and execution state
- ensure only one node executes the same job
- support recurring jobs through Cron expressions
- collect monitoring and SLA metrics

### Example Java code
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

### Why this is Phase 3
This takes the simple scheduler and turns it into a distributed execution framework with reliability and coordination concerns.

---

## Design patterns used
### Strategy Pattern
Used for choosing scheduling algorithms.
```java
public interface SchedulingStrategy {
    List<Job> schedule(List<Job> jobs);
}
```

### Factory Pattern
Used to create strategy instances based on input.

### Observer Pattern
Used when job status changes must notify listeners or dashboards.

---

## OOP principles used
- Abstraction: hide scheduling algorithm details behind a strategy interface
- Encapsulation: job data and execution state are protected
- Polymorphism: different strategies provide different ordering behavior
- Single Responsibility: job data, scheduling logic, execution orchestration, and retries are separated
- Open/Closed Principle: add new schedule algorithms without changing the scheduler itself

---

## Edge cases
- empty job list
- equal priorities
- equal durations
- jobs with impossible deadlines
- retry storms
- duplicate jobs
- simultaneous worker assignment
- scheduler crash mid-execution

---

## Interview angle
This problem is valuable because it shows how a clean core design evolves into a real production system.

The interviewer wants to see:
- how you model responsibilities
- whether you understand trade-offs between algorithms
- whether you can extend the design without breaking the system
- whether you can talk about production concerns like retries, locking, and persistence

---

## Summary
The best way to explain this problem is:
- Phase 1: scheduling logic and algorithm selection
- Phase 2: retry, status, history, and recurring jobs
- Phase 3: distributed coordination, persistence, and worker management

This is one of the strongest LLD topics because it shows both design quality and engineering realism.
