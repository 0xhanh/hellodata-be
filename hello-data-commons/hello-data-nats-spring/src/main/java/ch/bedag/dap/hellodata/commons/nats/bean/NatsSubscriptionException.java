package ch.bedag.dap.hellodata.commons.nats.bean;

public class NatsSubscriptionException extends RuntimeException {
    public NatsSubscriptionException(String message) {
        super(message);
    }

    public NatsSubscriptionException(String message, Throwable cause) {
        super(message, cause);
    }
}