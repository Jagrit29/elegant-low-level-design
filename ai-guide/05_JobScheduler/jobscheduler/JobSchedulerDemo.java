package lowleveldesign.systems.jobscheduler;

/**
 * Job Scheduler Demo - Learning Example
 * 
 * This demonstrates all 4 scheduling algorithms with the same set of jobs.
 * You can see how each algorithm produces different scheduling orders.
 */
public class JobSchedulerDemo {

    public static void main(String[] args) {
        // Create 5 sample jobs
        Job jobA = new Job("JobA", 5, Priority.P1, 10, UserType.ADMIN, 0);
        Job jobB = new Job("JobB", 2, Priority.P0, 5, UserType.ROOT, 1);
        Job jobC = new Job("JobC", 8, Priority.P2, 4, UserType.USER, 2);
        Job jobD = new Job("JobD", 3, Priority.P1, 7, UserType.ADMIN, 3);
        Job jobE = new Job("JobE", 1, Priority.P0, 20, UserType.ROOT, 4);

        int numberOfThreads = 2;

        System.out.println("========================================");
        System.out.println("   JOB SCHEDULER - PHASE 1 DEMO");
        System.out.println("========================================\n");

        System.out.println("📋 Jobs to Schedule:");
        System.out.println("  JobA: duration=5s,  priority=P1, deadline=10s, user=ADMIN");
        System.out.println("  JobB: duration=2s,  priority=P0, deadline=5s,  user=ROOT");
        System.out.println("  JobC: duration=8s,  priority=P2, deadline=4s,  user=USER");
        System.out.println("  JobD: duration=3s,  priority=P1, deadline=7s,  user=ADMIN");
        System.out.println("  JobE: duration=1s,  priority=P0, deadline=20s, user=ROOT");
        System.out.println("\n⚙️  Number of Threads: " + numberOfThreads);

        // ============================================
        // Algorithm 1: FCFS
        // ============================================
        System.out.println("\n" + "=".repeat(40));
        System.out.println("1️⃣  FCFS (First Come First Serve)");
        System.out.println("=".repeat(40));
        System.out.println("📌 Logic: Process jobs by arrival order only");
        System.out.println("Expected Order: A → B → C → D → E\n");

        Scheduler fcfsScheduler = new Scheduler(new FCFSScheduler());
        fcfsScheduler.addJob(jobA);
        fcfsScheduler.addJob(jobB);
        fcfsScheduler.addJob(jobC);
        fcfsScheduler.addJob(jobD);
        fcfsScheduler.addJob(jobE);
        fcfsScheduler.printSchedulingResult(numberOfThreads);

        // ============================================
        // Algorithm 2: SJF
        // ============================================
        System.out.println("\n" + "=".repeat(40));
        System.out.println("2️⃣  SJF (Shortest Job First)");
        System.out.println("=".repeat(40));
        System.out.println("📌 Logic: Sort by duration, then priority");
        System.out.println("Expected Order: E(1s) → B(2s) → D(3s) → A(5s) → C(8s)\n");

        Scheduler sjfScheduler = new Scheduler(new SJFScheduler());
        sjfScheduler.addJob(jobA);
        sjfScheduler.addJob(jobB);
        sjfScheduler.addJob(jobC);
        sjfScheduler.addJob(jobD);
        sjfScheduler.addJob(jobE);
        sjfScheduler.printSchedulingResult(numberOfThreads);

        // ============================================
        // Algorithm 3: FPS
        // ============================================
        System.out.println("\n" + "=".repeat(40));
        System.out.println("3️⃣  FPS (Fixed Priority Scheduling)");
        System.out.println("=".repeat(40));
        System.out.println("📌 Logic: Sort by priority, then userType, then duration");
        System.out.println("Expected Order: B(P0,ROOT) → E(P0,ROOT) → A(P1,ADMIN) → D(P1,ADMIN) → C(P2,USER)\n");

        Scheduler fpsScheduler = new Scheduler(new FPSScheduler());
        fpsScheduler.addJob(jobA);
        fpsScheduler.addJob(jobB);
        fpsScheduler.addJob(jobC);
        fpsScheduler.addJob(jobD);
        fpsScheduler.addJob(jobE);
        fpsScheduler.printSchedulingResult(numberOfThreads);

        // ============================================
        // Algorithm 4: EDF
        // ============================================
        System.out.println("\n" + "=".repeat(40));
        System.out.println("4️⃣  EDF (Earliest Deadline First)");
        System.out.println("=".repeat(40));
        System.out.println("📌 Logic: Filter jobs (remove if duration > deadline), sort by deadline");
        System.out.println("⚠️  JobC excluded (duration 8s > deadline 4s)");
        System.out.println("Expected Order: B(5s) → D(7s) → A(10s) → E(20s)\n");

        Scheduler edfScheduler = new Scheduler(new EDFScheduler());
        edfScheduler.addJob(jobA);
        edfScheduler.addJob(jobB);
        edfScheduler.addJob(jobC);
        edfScheduler.addJob(jobD);
        edfScheduler.addJob(jobE);
        edfScheduler.printSchedulingResult(numberOfThreads);

        // ============================================
        // Bonus: Strategy Switching at Runtime
        // ============================================
        System.out.println("\n" + "=".repeat(40));
        System.out.println("🔄 BONUS: Runtime Strategy Switching");
        System.out.println("=".repeat(40));
        System.out.println("Starting with FCFS...");

        Scheduler flexibleScheduler = new Scheduler(new FCFSScheduler());
        flexibleScheduler.addJob(jobA);
        flexibleScheduler.addJob(jobB);
        flexibleScheduler.addJob(jobC);
        flexibleScheduler.addJob(jobD);
        flexibleScheduler.addJob(jobE);
        flexibleScheduler.printSchedulingResult(numberOfThreads);

        System.out.println("\nSwitching to SJF...");
        flexibleScheduler.setStrategy(new SJFScheduler());
        flexibleScheduler.printSchedulingResult(numberOfThreads);

        System.out.println("\nSwitching to EDF...");
        flexibleScheduler.setStrategy(new EDFScheduler());
        flexibleScheduler.printSchedulingResult(numberOfThreads);

        System.out.println("\n" + "=".repeat(40));
        System.out.println("✅ Demo Complete!");
        System.out.println("=".repeat(40));
    }
}
