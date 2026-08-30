package lowleveldesign.systems.jobscheduler;

public interface DistributedLock {
    boolean tryAcquire(String key, long timeoutMs);
    void release(String key);
}
