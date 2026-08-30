package lowleveldesign.systems.jobscheduler;

import java.util.ArrayList;
import java.util.List;

/**
 * FPS Scheduler - Fixed Priority Scheduling
 * 
 * Sorting Logic: By priority first, then by userType, then by duration
 * 
 * Example (assuming userType assignments):
 * Job A: priority=P1, userType=ADMIN, duration=5s
 * Job B: priority=P0, userType=ROOT, duration=2s
 * Job C: priority=P2, userType=USER, duration=8s
 * Job D: priority=P1, userType=ADMIN, duration=3s
 * Job E: priority=P0, userType=ROOT, duration=1s
 * 
 * Order: B(P0,ROOT) → E(P0,ROOT) → A(P1,ADMIN) → D(P1,ADMIN) → C(P2,USER)
 * 
 * Sorting Rule:
 * - Primary: Priority (ascending) - higher priority (lower value) first
 * - Secondary: UserType (ascending) - higher authority (lower value) first
 * - Tertiary: Duration (ascending) - shorter duration first
 * 
 * Pros: Respects system priorities and user authority
 * Cons: Doesn't consider deadlines
 */
public class FPSScheduler implements SchedulingStrategy {

    @Override
    public List<Job> schedule(List<Job> jobs) {
        List<Job> result = new ArrayList<>(jobs);

        // Sort by priority, then by userType, then by duration
        result.sort((job1, job2) -> {
            // First compare by priority
            int priorityCompare = Integer.compare(job1.getPriority().getValue(), job2.getPriority().getValue());
            if (priorityCompare != 0) {
                return priorityCompare; // Higher priority (lower value) first
            }

            // If priorities are equal, compare by userType
            int userTypeCompare = Integer.compare(job1.getUserType().getValue(), job2.getUserType().getValue());
            if (userTypeCompare != 0) {
                return userTypeCompare; // Higher authority (lower value) first
            }

            // If both are equal, compare by duration
            return Integer.compare(job1.getDuration(), job2.getDuration());
        });

        return result;
    }
}
