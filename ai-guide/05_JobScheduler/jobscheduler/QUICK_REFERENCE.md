# Job Scheduler - Quick Reference Cheat Sheet

## Algorithm at a Glance

### FCFS (First Come First Serve)
```
Sorting: Arrival Order
Comparator: job1.getArrivalOrder() vs job2.getArrivalOrder()
Real Example: Barber shop queue (first person first)
Pros: Fair, no starvation
Cons: Inefficient, ignores priority/deadline
```

### SJF (Shortest Job First)
```
Sorting: Duration (primary), Priority (secondary)
Comparator: job1.getDuration() vs job2.getDuration()
Real Example: Restaurant cooking (short orders first)
Pros: Minimizes average wait time
Cons: Can starve long jobs, ignores deadline
```

### FPS (Fixed Priority Scheduling)
```
Sorting: Priority (primary), UserType (secondary), Duration (tertiary)
Comparator: Priority → UserType → Duration
Real Example: Hospital ER (critical patients first)
Pros: Respects business rules, authority-aware
Cons: Ignores deadline, low priority jobs starve
```

### EDF (Earliest Deadline First)
```
Sorting: Filter (remove if duration > deadline), 
         then Deadline (primary), Priority (secondary), Duration (tertiary)
Comparator: Deadline → Priority → Duration
Real Example: Exam prep (study nearest exam first)
Pros: Meets deadlines, optimal for timeliness
Cons: Skips impossible jobs, ignores priority if deadlines far
```

---

## Code Templates

### Template 1: Simple Sorting (FCFS, SJF)
```java
public List<Job> schedule(List<Job> jobs) {
    List<Job> result = new ArrayList<>(jobs);
    result.sort((job1, job2) -> 
        Integer.compare(job1.getField(), job2.getField())
    );
    return result;
}
```

### Template 2: Multiple Sort Keys (FPS, EDF)
```java
public List<Job> schedule(List<Job> jobs) {
    List<Job> result = new ArrayList<>(jobs);
    result.sort((job1, job2) -> {
        int first = Integer.compare(...);
        if (first != 0) return first;
        
        int second = Integer.compare(...);
        if (second != 0) return second;
        
        return Integer.compare(...);
    });
    return result;
}
```

### Template 3: Filter + Sort (EDF)
```java
public List<Job> schedule(List<Job> jobs) {
    List<Job> filtered = jobs.stream()
        .filter(job -> job.getDuration() <= job.getDeadline())
        .collect(Collectors.toList());
    
    filtered.sort((job1, job2) -> 
        Integer.compare(job1.getDeadline(), job2.getDeadline())
    );
    return filtered;
}
```

### Template 4: Round-Robin Distribution
```java
for (int i = 0; i < orderedJobs.size(); i++) {
    int threadIndex = i % numberOfThreads;
    threads.get(threadIndex).add(orderedJobs.get(i));
}
```

---

## Key Design Concepts

### Strategy Pattern
```
Why: Different algorithms, same interface
How: 
  - Interface: SchedulingStrategy
  - Implementations: FCFSScheduler, SJFScheduler, FPSScheduler, EDFScheduler
  - Usage: Scheduler HAS-A Strategy
  
Benefit: Add new algorithms without changing Scheduler
```

### Composition > Inheritance
```
BAD: class Scheduler extends FCFSScheduler
      (tight coupling, can only use one algorithm)

GOOD: class Scheduler {
          SchedulingStrategy strategy;
      }
      (loose coupling, can switch at runtime)
```

### Separation of Concerns
```
Job: Data only (name, duration, priority, etc.)
     No scheduling logic

SchedulingStrategy: Sorting logic only
                   No distribution logic

Scheduler: Orchestration only (uses strategy, distributes jobs)
          No job attributes, no algorithm details
```

---

## Common Tradeoffs

| Metric | FCFS | SJF | FPS | EDF |
|--------|------|-----|-----|-----|
| **Fairness** | ✅ High | ❌ Low | ⚠️ Medium | ⚠️ Medium |
| **Efficiency** | ❌ Low | ✅ High | ⚠️ Medium | ⚠️ Medium |
| **Deadline Compliance** | ❌ 0% | ❌ 0% | ❌ 0% | ✅ 100% |
| **Simplicity** | ✅ Easy | ✅ Easy | ⚠️ Medium | ⚠️ Medium |
| **Starvation Risk** | ❌ None | ✅ High | ✅ High | ❌ None |

---

## Time Complexity

```
Sorting: O(n log n) - where n = number of jobs
Filtering: O(n)
Distribution: O(n)
Overall: O(n log n)

Space: O(n) for output lists
```

---

## Edge Cases Quick Check

- [ ] Empty job list → [] for each thread
- [ ] More jobs than threads → round-robin handles
- [ ] More threads than jobs → some threads empty
- [ ] All jobs miss deadline (EDF) → no jobs scheduled
- [ ] Same priority & userType → use duration
- [ ] Identical jobs → order arbitrary but deterministic

---

## Interview Talking Points

1. **Opening Statement** (30 sec)
   > "This is a scheduling problem. I'll use Strategy Pattern to support multiple algorithms. Each algorithm has a different sorting logic - the key insight is that different business needs require different ordering."

2. **Algorithm Explanation** (2 min each)
   > "FCFS processes by arrival order - simple but inefficient. SJF minimizes average wait time but can starve long jobs. FPS respects business priorities. EDF meets deadlines by filtering impossible jobs. Each has tradeoffs."

3. **Design Pattern Justification** (1 min)
   > "Strategy Pattern allows adding new algorithms without modifying existing code. This follows the Open/Closed Principle. We can also switch algorithms at runtime based on system load."

4. **Round-Robin Explanation** (1 min)
   > "Round-robin distributes jobs fairly across threads. Each thread gets roughly equal work. If we didn't use round-robin, one thread might get all short jobs while another gets all long jobs."

5. **Real-World Connection** (1 min)
   > "Job Scheduler is used in [email notifications, batch processing, CI/CD pipelines, video transcoding]. Different systems need different algorithms based on their constraints."

---

## What Interviewers Want to Hear

✅ **Good**:
- "Let me clarify requirements..."
- "I'll use Strategy Pattern because..."
- "Here's an example with numbers..."
- "This edge case is handled by..."
- "In production, I'd add..."
- "Different algorithms have tradeoffs..."

❌ **Bad**:
- "I'll just code it up"
- "It's obvious we need..."
- "This is the only way to do it"
- "Edge cases aren't important"
- "My code is self-explanatory"

---

## Files to Know

```
jobscheduler/
├── Job.java                 # Priority P0, P1, P2 & UserType ROOT, ADMIN, USER
├── SchedulingStrategy.java  # Interface with schedule(List<Job>) method
├── FCFSScheduler.java       # Sort by arrivalOrder
├── SJFScheduler.java        # Sort by duration, then priority
├── FPSScheduler.java        # Sort by priority, then userType, then duration
├── EDFScheduler.java        # Filter + sort by deadline
├── Scheduler.java           # Main class that uses strategy + round-robin
├── JobSchedulerDemo.java    # Run this to see all algorithms
└── INTERVIEW_GUIDE.md       # This comprehensive guide
```

---

## To Run the Demo

```bash
cd /Users/jagrit/Projects/elegant-low-level-design
javac -d . lowleveldesign/systems/jobscheduler/*.java
java lowleveldesign.systems.jobscheduler.JobSchedulerDemo
```

Output shows all 4 algorithms with the same jobs - see how different ordering produces different distributions!

---

## Practice Problems

### Practice 1: Add a new algorithm (LRU - Least Recently Used)
```java
// Sort by time last executed
// Ensure all jobs get fair time
public class LRUScheduler implements SchedulingStrategy {
    // Your implementation
}
```

### Practice 2: Handle job status
```java
enum JobStatus {
    PENDING, RUNNING, SUCCESS, FAILED, CANCELLED
}

// Track execution history
// Show which jobs are pending, running, done
```

### Practice 3: Implement retry logic
```java
// If a job fails, re-add with exponential backoff
// delay = initialDelay * (backoffMultiplier ^ attempt)
```

### Practice 4: Add job dependencies
```java
// Job B runs only after Job A completes
// Creates a DAG scheduling problem
```

---

## This Teaches You

1. ✅ **Strategy Pattern** - How to make code extensible
2. ✅ **Comparators** - Multi-key sorting in Java
3. ✅ **Algorithm Analysis** - Different algorithms have different tradeoffs
4. ✅ **Design Thinking** - From problem to entities to code
5. ✅ **Real-World Systems** - How actual schedulers work (OS, Kubernetes, AWS, etc.)

---

**Remember**: Understand the WHY, not just the HOW. Interviewers test thinking, not memory!
