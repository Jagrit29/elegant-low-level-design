package lowleveldesign.systems.jobscheduler;

/**
 * Job - Pure data entity that represents a job to be scheduled
 * 
 * Design principle: Separation of Concerns
 * - Job only holds data, no scheduling logic
 * - Scheduling logic is delegated to SchedulingStrategy
 */
public class Job {
    private String name;
    private int duration;           // in seconds
    private Priority priority;      // P0 > P1 > P2
    private int deadline;           // in seconds
    private UserType userType;      // ROOT > ADMIN > USER
    private int arrivalOrder;       // when job was submitted (0-indexed)

    // Constructor
    public Job(String name, int duration, Priority priority, int deadline, UserType userType, int arrivalOrder) {
        this.name = name;
        this.duration = duration;
        this.priority = priority;
        this.deadline = deadline;
        this.userType = userType;
        this.arrivalOrder = arrivalOrder;
    }

    // Getters
    public String getName() {
        return name;
    }

    public int getDuration() {
        return duration;
    }

    public Priority getPriority() {
        return priority;
    }

    public int getDeadline() {
        return deadline;
    }

    public UserType getUserType() {
        return userType;
    }

    public int getArrivalOrder() {
        return arrivalOrder;
    }

    @Override
    public String toString() {
        return String.format("%s(dur=%ds, p=%s, deadline=%ds, user=%s)",
                name, duration, priority, deadline, userType);
    }
}

/**
 * ENUMS for Priority and UserType
 * 
 * Priority: Lower numeric value = higher priority
 * - P0 = 0 (highest)
 * - P1 = 1
 * - P2 = 2 (lowest)
 */
enum Priority {
    P0(0),
    P1(1),
    P2(2);

    private final int value;

    Priority(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}

/**
 * UserType: Lower numeric value = higher authority
 * - ROOT = 0 (highest)
 * - ADMIN = 1
 * - USER = 2 (lowest)
 */
enum UserType {
    ROOT(0),
    ADMIN(1),
    USER(2);

    private final int value;

    UserType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
