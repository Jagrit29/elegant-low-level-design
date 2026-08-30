package lowleveldesign.systems.jobscheduler;

public class RetryPolicy {
    private final int maxAttempts;
    private final long initialDelayInSeconds;
    private final double backoffMultiplier;

    public RetryPolicy(int maxAttempts, long initialDelayInSeconds, double backoffMultiplier) {
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("maxAttempts must be positive");
        }
        if (initialDelayInSeconds < 0) {
            throw new IllegalArgumentException("initialDelayInSeconds cannot be negative");
        }
        if (backoffMultiplier < 1.0) {
            throw new IllegalArgumentException("backoffMultiplier must be >= 1.0");
        }

        this.maxAttempts = maxAttempts;
        this.initialDelayInSeconds = initialDelayInSeconds;
        this.backoffMultiplier = backoffMultiplier;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public long getInitialDelayInSeconds() {
        return initialDelayInSeconds;
    }

    public double getBackoffMultiplier() {
        return backoffMultiplier;
    }

    public long nextDelaySeconds(int attemptNumber) {
        if (attemptNumber <= 0) {
            return initialDelayInSeconds;
        }
        return (long) (initialDelayInSeconds * Math.pow(backoffMultiplier, attemptNumber - 1));
    }
}
