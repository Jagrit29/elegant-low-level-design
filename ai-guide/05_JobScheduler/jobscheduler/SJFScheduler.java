package lowleveldesign.systems.jobscheduler;

import java.util.ArrayList;
import java.util.List;

/**
 * SJF Scheduler - Shortest Job First
 * 
 * Sorting Logic: By duration (ascending), then by priority if tie
 * 
 * Example:
 * Job A: duration=5s, priority=P1
 * Job B: duration=2s, priority=P0
 * Job C: duration=8s, priority=P2
 * Job D: duration=3s, priority=P1
 * Job E: duration=1s, priority=P0
 * 
 * Order: E(1s) → B(2s) → D(3s) → A(5s) → C(8s)
 * 
 * Sorting Rule:
 * - Primary: Duration (ascending) - shortest first
 * - Secondary: Priority (ascending) - higher priority (lower value) first
 * 
 * Pros: Minimizes average waiting time
 * Cons: Long jobs may starve if short jobs keep arriving
 */
public class SJFScheduler implements SchedulingStrategy {

    @Override
    public List<Job> schedule(List<Job> jobs) {
        List<Job> result = new ArrayList<>(jobs);

        // Sort by duration first, then by priority
        result.sort((job1, job2) -> {
            // First compare by duration
            int durationCompare = Integer.compare(job1.getDuration(), job2.getDuration());
            if (durationCompare != 0) {
                return durationCompare; // Shorter duration first
            }
            // If durations are equal, compare by priority
            return Integer.compare(job1.getPriority().getValue(), job2.getPriority().getValue());
        });

        return result;
    }
}
