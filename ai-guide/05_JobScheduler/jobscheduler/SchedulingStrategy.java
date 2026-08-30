package lowleveldesign.systems.jobscheduler;

import java.util.List;

/**
 * SchedulingStrategy - STRATEGY PATTERN Interface
 * 
 * Design Principle: Open/Closed Principle (OCP)
 * - Open for extension: New algorithms can be added without modifying existing code
 * - Closed for modification: Scheduler and Job remain unchanged
 * 
 * Benefits:
 * 1. Easy to switch algorithms at runtime
 * 2. Easy to add new algorithms by implementing this interface
 * 3. No changes to Scheduler class when adding new strategy
 */
public interface SchedulingStrategy {
    
    /**
     * Schedule the given jobs according to this strategy's logic
     * 
     * @param jobs List of jobs to schedule
     * @return Ordered list of jobs ready to be distributed to threads
     */
    List<Job> schedule(List<Job> jobs);
}
