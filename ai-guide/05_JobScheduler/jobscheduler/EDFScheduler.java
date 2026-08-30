package lowleveldesign.systems.jobscheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * EDF Scheduler - Earliest Deadline First
 * 
 * Sorting Logic:
 * 1. FILTER: Remove jobs where duration > deadline (impossible to meet deadline)
 * 2. SORT: By deadline (ascending), then by priority, then by duration
 * 
 * Example:
 * Job A: duration=5s, deadline=10s ✓ (included)
 * Job B: duration=2s, deadline=5s ✓ (included)
 * Job C: duration=8s, deadline=4s ✗ (excluded - duration > deadline)
 * Job D: duration=3s, deadline=7s ✓ (included)
 * Job E: duration=1s, deadline=20s ✓ (included)
 * 
 * After filtering: A, B, D, E
 * Sorted by deadline: B(5s) → D(7s) → A(10s) → E(20s)
 * 
 * Sorting Rule:
 * - Primary: Deadline (ascending) - nearest deadline first
 * - Secondary: Priority (ascending) - higher priority first
 * - Tertiary: Duration (ascending) - shorter duration first
 * 
 * Pros: Meets as many deadlines as possible
 * Cons: Ignores jobs that cannot meet their deadline (they're dropped)
 */
public class EDFScheduler implements SchedulingStrategy {

    @Override
    public List<Job> schedule(List<Job> jobs) {
        // Step 1: Filter - keep only jobs that CAN meet their deadline
        List<Job> validJobs = jobs.stream()
                .filter(job -> job.getDuration() <= job.getDeadline())
                .collect(Collectors.toList());

        // Step 2: Sort by deadline, then priority, then duration
        validJobs.sort((job1, job2) -> {
            // First compare by deadline
            int deadlineCompare = Integer.compare(job1.getDeadline(), job2.getDeadline());
            if (deadlineCompare != 0) {
                return deadlineCompare; // Earliest deadline first
            }

            // If deadlines are equal, compare by priority
            int priorityCompare = Integer.compare(job1.getPriority().getValue(), job2.getPriority().getValue());
            if (priorityCompare != 0) {
                return priorityCompare; // Higher priority first
            }

            // If both are equal, compare by duration
            return Integer.compare(job1.getDuration(), job2.getDuration());
        });

        return validJobs;
    }
}
