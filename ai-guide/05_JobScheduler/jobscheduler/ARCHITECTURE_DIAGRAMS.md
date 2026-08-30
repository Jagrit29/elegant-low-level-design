# Job Scheduler - Architecture Diagrams & Visuals

## System Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                      JOB SCHEDULER SYSTEM                        │
└─────────────────────────────────────────────────────────────────┘

                    ┌─────────────────────┐
                    │   INPUT: Jobs       │
                    │  (Job[], numThreads)│
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────────────────┐
                    │     SCHEDULER (Coordinator)     │
                    │  ┌──────────────────────────┐  │
                    │  │ 1. Get jobs from list    │  │
                    │  │ 2. Call strategy.order() │  │
                    │  │ 3. Distribute round-robin│  │
                    │  │ 4. Return 2D array       │  │
                    │  └──────────────────────────┘  │
                    └──────────┬──────────────────────┘
                               │
                ┌──────────────┴───────────────┐
                │   STRATEGY DELEGATION        │
                └──────────────┬───────────────┘
                               │
        ┌──────────────┬───────┼───────┬──────────────┐
        │              │       │       │              │
        ▼              ▼       ▼       ▼              ▼
    FCFS         SJF          FPS    EDF        (Easy to extend)
    Sort by      Sort by      Sort by Filter +  Add new algorithm:
    arrival      duration     priority sort by  Implement interface
                  +priority    +userType deadline
                  +duration    +duration +priority
                                        +duration

                               │
                               ▼
                    ┌─────────────────────────────────┐
                    │  ORDERED JOBS                   │
                    │  [Job_E, Job_B, Job_D, Job_A]  │
                    └──────────┬──────────────────────┘
                               │
                    ┌──────────▼─────────────┐
                    │  ROUND-ROBIN ASSIGN    │
                    │  (i % numThreads)      │
                    └──────────┬─────────────┘
                               │
        ┌──────────────┬───────┴────────┬──────────────┐
        │              │                │              │
        ▼              ▼                ▼              ▼
    Thread 0      Thread 1          Thread 2      Thread N
    [Job_E]       [Job_B]           [Job_D]       [Job_A]
    [Job_D]       [Job_A]           ...
    ...                              

                    ┌─────────────────────┐
                    │  EXECUTION (Runtime) │
                    │  Run on thread pool  │
                    └─────────────────────┘
```

---

## Class Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                  SCHEDULER ARCHITECTURE                         │
└─────────────────────────────────────────────────────────────────┘

                    ┌──────────────────┐
                    │   <<interface>>  │
                    │SchedulingStrategy│
                    ├──────────────────┤
                    │+ schedule()      │
                    └────────┬─────────┘
                             △
            ┌────────────────┼────────────────┬───────────────┐
            │                │                │               │
    ┌───────┴────────┐   ┌──┴──────────┐  ┌─┴──────────┐  ┌─┴──────────┐
    │ FCFSScheduler  │   │ SJFScheduler │  │FPSScheduler│  │EDFScheduler│
    ├────────────────┤   ├─────────────┤  ├───────────┤  ├────────────┤
    │ + schedule()   │   │ + schedule()│  │ + schedule│  │+ schedule()│
    │   (sort by     │   │   (sort by  │  │   (sort by│  │  (filter + │
    │    arrival)    │   │   duration) │  │ priority) │  │ sort)      │
    └────────────────┘   └─────────────┘  └───────────┘  └────────────┘


    ┌─────────────────────────────────────────────────────────┐
    │                    Scheduler                            │
    ├─────────────────────────────────────────────────────────┤
    │ - jobs: List<Job>                                       │
    │ - strategy: SchedulingStrategy                          │
    ├─────────────────────────────────────────────────────────┤
    │ + addJob(Job): void                                     │
    │ + setStrategy(SchedulingStrategy): void                 │
    │ + getSchedulingSequence(int): List<List<Job>>           │
    └─────────────────────────────────────────────────────────┘
            │
            │ uses (HAS-A)
            ▼
    ┌─────────────────────────────────────────┐
    │               Job                       │
    ├─────────────────────────────────────────┤
    │ - name: String                          │
    │ - duration: int                         │
    │ - priority: Priority (P0, P1, P2)       │
    │ - deadline: int                         │
    │ - userType: UserType (ROOT, ADMIN, USER)│
    │ - arrivalOrder: int                     │
    ├─────────────────────────────────────────┤
    │ + getters...                            │
    └─────────────────────────────────────────┘
```

---

## Algorithm Flow Diagram

### FCFS Flow
```
Input: [JobA(arrival=3), JobB(arrival=1), JobC(arrival=2)]

Compare all pairs:
  - A(3) vs B(1) → B < A
  - A(3) vs C(2) → C < A
  - B(1) vs C(2) → B < C

Sorted by arrival order:
  B(1) → C(2) → A(3)

Output: [JobB, JobC, JobA]
```

### SJF Flow
```
Input: [JobA(dur=5), JobB(dur=2), JobC(dur=8), JobD(dur=3)]

Sort by duration:
  JobB(2) < JobD(3) < JobA(5) < JobC(8)

If same duration, break tie with priority:
  JobB(2, P0) < JobD(3, P1) → JobB wins if same duration

Output: [JobB, JobD, JobA, JobC]
```

### FPS Flow
```
Input: [
  JobA(P1, ADMIN, 5),
  JobB(P0, ROOT, 2),
  JobC(P2, USER, 8),
  JobE(P0, ROOT, 1)
]

Step 1: Group by priority
  P0: [JobB(ROOT, 2), JobE(ROOT, 1)]
  P1: [JobA(ADMIN, 5)]
  P2: [JobC(USER, 8)]

Step 2: Within each group, sort by userType
  P0: Both ROOT, so tie-break by duration:
      JobE(1) → JobB(2)
  P1: Only one job
  P2: Only one job

Step 3: Within userType groups, sort by duration
  (Already done above)

Output: [JobE, JobB, JobA, JobC]
```

### EDF Flow
```
Input: [
  JobA(dur=5, deadline=10),
  JobB(dur=2, deadline=5),
  JobC(dur=8, deadline=4),  ← IMPOSSIBLE
  JobD(dur=3, deadline=7)
]

Step 1: FILTER - Keep jobs where duration ≤ deadline
  JobA: 5 ≤ 10 ✓ KEEP
  JobB: 2 ≤ 5 ✓ KEEP
  JobC: 8 ≤ 4 ✗ REMOVE (impossible)
  JobD: 3 ≤ 7 ✓ KEEP

  After filtering: [JobA, JobB, JobD]

Step 2: Sort by deadline
  JobB(5) < JobD(7) < JobA(10)

Output: [JobB, JobD, JobA]  (JobC is never scheduled)
```

---

## Execution Timeline

### Scenario 1: FCFS with 2 Threads

```
Ordered Jobs: [E(1s), B(2s), A(5s), C(8s), D(3s)]

Round-Robin Assignment:
  Thread 0: [E, A, D]      (indices 0, 2, 4)
  Thread 1: [B, C]         (indices 1, 3)

Execution Timeline:
  T=0-1s:   Thread 0 runs E(1s)
            Thread 1 runs B(2s)
  
  T=1-3s:   Thread 0 finishes E, runs A(5s)
            Thread 1 still running B
  
  T=2-3s:   Thread 1 finishes B, runs C(8s)
            Thread 0 still running A
  
  T=3-6s:   Thread 0 finishes A, runs D(3s)
            Thread 1 still running C
  
  T=6-8s:   Thread 0 finishes D, idle
            Thread 1 finishes C

Total Time: 10 seconds (max of 1+5+3 = 9s on T0, 2+8 = 10s on T1)
```

### Scenario 2: SJF with 2 Threads (More Efficient)

```
Ordered Jobs: [E(1s), B(2s), D(3s), A(5s), C(8s)]

Round-Robin Assignment:
  Thread 0: [E, D, C]      (indices 0, 2, 4)
  Thread 1: [B, A]         (indices 1, 3)

Execution Timeline:
  T=0-1s:   Thread 0 runs E(1s)
            Thread 1 runs B(2s)
  
  T=1-4s:   Thread 0 finishes E, runs D(3s)
            Thread 1 still running B
  
  T=2-7s:   Thread 1 finishes B, runs A(5s)
            Thread 0 still running D
  
  T=4-12s:  Thread 0 finishes D, runs C(8s)
            Thread 1 still running A
  
  T=7-12s:  Thread 1 finishes A, idle
            Thread 0 finishes C

Total Time: 12 seconds (same total because we're distributing the same total work)

Wait Time Reduction:
  FCFS: Avg wait = (0 + 1 + 4 + 2 + 9) / 5 = 3.2s
  SJF:  Avg wait = (0 + 1 + 3 + 2 + 7) / 5 = 2.6s
  
  SJF has better average response time!
```

---

## Sorting Logic Comparison

```
┌────────────┬──────────────────┬──────────────────┬──────────────────┐
│ Algorithm  │ Primary Key      │ Secondary Key    │ Tertiary Key     │
├────────────┼──────────────────┼──────────────────┼──────────────────┤
│ FCFS       │ Arrival Order ↑  │ —                │ —                │
│            │ (earliest first) │                  │                  │
├────────────┼──────────────────┼──────────────────┼──────────────────┤
│ SJF        │ Duration ↑       │ Priority ↑       │ —                │
│            │ (shortest first) │ (P0 first)       │                  │
├────────────┼──────────────────┼──────────────────┼──────────────────┤
│ FPS        │ Priority ↑       │ UserType ↑       │ Duration ↑       │
│            │ (P0 first)       │ (ROOT first)     │ (shorter first)  │
├────────────┼──────────────────┼──────────────────┼──────────────────┤
│ EDF        │ Deadline ↑       │ Priority ↑       │ Duration ↑       │
│            │ (soonest first)  │ (P0 first)       │ (shorter first)  │
│            │ (after filtering)│                  │                  │
└────────────┴──────────────────┴──────────────────┴──────────────────┘

Note: Filter Step for EDF
  Remove jobs where: duration > deadline
  These are impossible to complete in time
```

---

## Round-Robin Distribution

```
Scenario: 5 ordered jobs, 3 threads

Ordered: [Job_1, Job_2, Job_3, Job_4, Job_5]

Round-Robin Logic:
  for (i = 0; i < 5; i++) {
    threadIndex = i % 3
    thread[threadIndex].add(job[i])
  }

Calculation:
  i=0: 0 % 3 = 0 → Thread 0 gets Job_1
  i=1: 1 % 3 = 1 → Thread 1 gets Job_2
  i=2: 2 % 3 = 2 → Thread 2 gets Job_3
  i=3: 3 % 3 = 0 → Thread 0 gets Job_4
  i=4: 4 % 3 = 1 → Thread 1 gets Job_5

Result:
  Thread 0: [Job_1, Job_4]
  Thread 1: [Job_2, Job_5]
  Thread 2: [Job_3]

Balance: Even distribution of ordered jobs
```

---

## Strategy Pattern Visualization

### Without Strategy Pattern (BAD)
```java
class Scheduler {
    String algorithmType;
    
    List<Job> schedule() {
        if (algorithmType.equals("FCFS")) {
            // FCFS logic
        } else if (algorithmType.equals("SJF")) {
            // SJF logic
        } else if (algorithmType.equals("FPS")) {
            // FPS logic
        } else if (algorithmType.equals("EDF")) {
            // EDF logic
        }
        // Adding new algorithm = MODIFY THIS CLASS
        // Violates OCP!
    }
}
```

### With Strategy Pattern (GOOD)
```
                ┌──────────────────────┐
                │  <<interface>>       │
                │ SchedulingStrategy   │
                │  + schedule()        │
                └──────────────────────┘
                         △
        ┌────────────────┼────────────────┐
        │                │                │
    FCFSScheduler   SJFScheduler    FPSScheduler   EDFScheduler
    NewAlgorithm         (etc.)

class Scheduler {
    SchedulingStrategy strategy;
    
    void setStrategy(SchedulingStrategy s) {
        this.strategy = s;  // SWAP at runtime
    }
    
    List<Job> schedule() {
        return strategy.schedule(jobs);  // Delegate
        // Adding new algorithm = NEW CLASS ONLY
        // Zero changes to Scheduler
        // Follows OCP!
    }
}
```

---

## Complexity Analysis

```
┌──────────────────┬─────────────────┬──────────────┬─────────────────┐
│ Operation        │ Time Complexity │ Space        │ Example (n=1000)│
├──────────────────┼─────────────────┼──────────────┼─────────────────┤
│ Sorting (all     │ O(n log n)      │ O(n)         │ ~10,000 ops     │
│ algorithms)      │ (Java uses      │              │                 │
│                  │  mergesort/     │              │                 │
│                  │  quicksort)     │              │                 │
├──────────────────┼─────────────────┼──────────────┼─────────────────┤
│ Filtering (EDF)  │ O(n)            │ O(n)         │ 1,000 ops       │
│                  │ (one pass)      │              │                 │
├──────────────────┼─────────────────┼──────────────┼─────────────────┤
│ Round-robin      │ O(n)            │ O(n)         │ 1,000 ops       │
│ distribution     │ (one pass)      │              │                 │
├──────────────────┼─────────────────┼──────────────┼─────────────────┤
│ Total scheduling │ O(n log n)      │ O(n)         │ ~10,000 ops     │
│                  │ (sorting        │              │                 │
│                  │ dominates)      │              │                 │
└──────────────────┴─────────────────┴──────────────┴─────────────────┘

For 10,000 jobs/minute:
  = ~167 jobs/second
  = Sort time per batch: < 1ms
  = Easily handles requirement
```

---

## Tradeoff Visualization

```
╔════════════════════════════════════════════════════════════╗
║         ALGORITHM TRADEOFF MATRIX                          ║
╠════════════════════════════════════════════════════════════╣

FAIRNESS (Everyone treated equally)
  FCFS: ████████████ (100%)
  SJF:  ██░░░░░░░░░░ (20%)
  FPS:  ████░░░░░░░░ (40%)
  EDF:  ████░░░░░░░░ (40%)

EFFICIENCY (Minimize total time)
  FCFS: ████░░░░░░░░ (40%)
  SJF:  ████████████ (100%)
  FPS:  ████████░░░░ (70%)
  EDF:  ████████░░░░ (70%)

DEADLINE COMPLIANCE
  FCFS: ░░░░░░░░░░░░ (0%)
  SJF:  ░░░░░░░░░░░░ (0%)
  FPS:  ░░░░░░░░░░░░ (0%)
  EDF:  ████████████ (100%)

PRIORITY RESPECT
  FCFS: ░░░░░░░░░░░░ (0%)
  SJF:  ░░░░░░░░░░░░ (0%)
  FPS:  ████████████ (100%)
  EDF:  ████░░░░░░░░ (40%)

IMPLEMENTATION SIMPLICITY
  FCFS: ████████████ (100%)
  SJF:  ████████░░░░ (80%)
  FPS:  ██████░░░░░░ (60%)
  EDF:  ████░░░░░░░░ (40%)

╚════════════════════════════════════════════════════════════╝

RECOMMENDATION BY USE CASE:
  - Fairness required: Use FCFS
  - Max throughput: Use SJF
  - Business rules important: Use FPS
  - Deadlines critical: Use EDF
  - Unknown requirements: Start with FPS
```

---

## Decision Tree: Choosing an Algorithm

```
                    START: Scheduling Problem
                            │
                            ▼
                    Fairness crucial?
                    /              \
                  YES              NO
                  │                │
                  ▼                ▼
               FCFS           Deadlines critical?
                              /              \
                            YES              NO
                            │                │
                            ▼                ▼
                           EDF          Priority important?
                                        /              \
                                      YES              NO
                                      │                │
                                      ▼                ▼
                                     FPS              SJF
                                   (Business)    (Efficiency)
```

---

These diagrams should help you visualize the concepts during interview preparation!
