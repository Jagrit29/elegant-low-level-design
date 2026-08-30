package lowleveldesign.systems.jobscheduler;

import java.util.ArrayList;
import java.util.List;

/**
 * Scheduler - Main Coordinator
 * 
 * Design Pattern: STRATEGY PATTERN + COMPOSITION
 * - Scheduler HAS-A SchedulingStrategy (not IS-A)
 * - This allows runtime switching of algorithms
 * 
 * Responsibility:
 * 1. Accept jobs
 * 2. Delegate ordering to strategy
 * 3. Distribute jobs to threads using round-robin
 */
public class Scheduler {
    private List<Job> jobs;
    private SchedulingStrategy strategy;

    /**
     * Constructor - accepts strategy at creation time
     * Can also switch strategies later using setStrategy()
     */
    public Scheduler(SchedulingStrategy strategy) {
        this.strategy = strategy;
        this.jobs = new ArrayList<>();
    }

    /**
     * Add a job to the scheduler
     */
    public void addJob(Job job) {
        jobs.add(job);
    }

    /**
     * Switch the scheduling strategy at runtime
     * This is a key benefit of the Strategy Pattern
     */
    public void setStrategy(SchedulingStrategy newStrategy) {
        this.strategy = newStrategy;
    }

    /**
     * Get the scheduling sequence for N threads
     * 
     * @param numberOfThreads Number of threads available
     * @return 2D array where result[i] = list of jobs for thread i
     */
    public List<List<Job>> getSchedulingSequence(int numberOfThreads) {
        // Step 1: Get ordered jobs from strategy
        List<Job> orderedJobs = strategy.schedule(jobs);

        // Step 2: Distribute to threads using ROUND-ROBIN
        List<List<Job>> threads = new ArrayList<>();
        for (int i = 0; i < numberOfThreads; i++) {
            threads.add(new ArrayList<>());
        }

        // Assign jobs to threads in round-robin fashion
        for (int i = 0; i < orderedJobs.size(); i++) {
            int threadIndex = i % numberOfThreads;
            threads.get(threadIndex).add(orderedJobs.get(i));
        }

        return threads;
    }

    /**
     * Debug: Print all jobs
     */
    public void printJobs() {
        System.out.println("Jobs in scheduler:");
        for (int i = 0; i < jobs.size(); i++) {
            System.out.println("  [" + i + "] " + jobs.get(i));
        }
    }

    /**
     * Debug: Print scheduling result for given threads
     */
    public void printSchedulingResult(int numberOfThreads) {
        List<List<Job>> result = getSchedulingSequence(numberOfThreads);
        System.out.println("\n--- Scheduling Result (" + numberOfThreads + " threads) ---");
        for (int i = 0; i < result.size(); i++) {
            System.out.print("Thread " + i + ": ");
            for (Job job : result.get(i)) {
                System.out.print(job.getName() + " ");
            }
            System.out.println();
        }
    }
}
