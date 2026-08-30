package lowleveldesign.systems.jobscheduler;

import java.util.ArrayList;
import java.util.List;

/**
 * FCFS Scheduler - First Come First Serve
 * 
 * Sorting Logic: By arrival order only
 * 
 * Example:
 * Job A: arrival=0, duration=5s
 * Job B: arrival=1, duration=2s
 * Job C: arrival=2, duration=8s
 * 
 * Order: A → B → C
 * 
 * Pros: Fair, simple
 * Cons: Long jobs block short jobs (convoy effect)
 */
public class FCFSScheduler implements SchedulingStrategy {

    @Override
    public List<Job> schedule(List<Job> jobs) {
        // Create a copy to avoid modifying original list
        List<Job> result = new ArrayList<>(jobs);

        // Sort by arrival order (ascending)
        result.sort((job1, job2) -> Integer.compare(job1.getArrivalOrder(), job2.getArrivalOrder()));

        return result;
    }
}
