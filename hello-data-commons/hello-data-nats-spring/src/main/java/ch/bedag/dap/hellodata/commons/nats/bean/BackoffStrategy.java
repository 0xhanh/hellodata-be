package ch.bedag.dap.hellodata.commons.nats.bean;

interface BackoffStrategy {
    long getNextDelay();
    void reset();
}

class ExponentialBackoffStrategy implements BackoffStrategy {
    private final long initialDelay;
    private final long maxDelay;
    private long currentDelay = 0;

    public ExponentialBackoffStrategy(long initialDelay, long maxDelay) {
        this.initialDelay = initialDelay;
        this.maxDelay = maxDelay;
    }

    @Override
    public long getNextDelay() {
        if (currentDelay == 0) {
            currentDelay = initialDelay;
        } else {
            currentDelay = Math.min(currentDelay * 2, maxDelay);
        }
        return currentDelay;
    }

    @Override
    public void reset() {
        currentDelay = 0;
    }
}