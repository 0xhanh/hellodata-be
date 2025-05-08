package ch.bedag.dap.hellodata.commons.nats.bean;

import ch.bedag.dap.hellodata.commons.nats.annotation.JetStreamSubscribe;
import ch.bedag.dap.hellodata.commons.nats.util.NatsStreamUtil;
import io.nats.client.*;
        import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.ConsumerInfo;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.time.Duration;

@Log4j2
public class SubscriptionManager {
    private final Connection natsConnection;
    private final JetStreamSubscribe subscribeAnnotation;
    private final String durableName;
    private JetStreamSubscription subscription;

    public SubscriptionManager(Connection natsConnection,
                               JetStreamSubscribe subscribeAnnotation,
                               String durableName) {
        this.natsConnection = natsConnection;
        this.subscribeAnnotation = subscribeAnnotation;
        this.durableName = durableName;
    }

    public void ensureSubscriptionActive() {
        try {
            JetStreamManagement jsm = natsConnection.jetStreamManagement();
            ConsumerInfo consumerInfo = jsm.getConsumerInfo(
                    subscribeAnnotation.event().getStreamName(),
                    durableName
            );

            if (consumerInfo == null || subscription == null) {
                log.warn("[NATS] Consumer {} for stream {} not found or subscription is null. Re-subscribing...",
                        durableName, subscribeAnnotation.event().getStreamName());
                resubscribe();
            }
        } catch (JetStreamApiException | IOException e) {
            log.error("[NATS] Error checking subscription status. Re-subscribing...", e);
            resubscribe();
        }
    }

    public Message fetchMessage(Duration timeout) throws InterruptedException {
        if (subscription == null) {
            log.warn("[NATS] Subscription is null for stream {} and subject {}",
                    subscribeAnnotation.event().getStreamName(),
                    subscribeAnnotation.event().getSubject());
            return null;
        }

        return subscription.nextMessage(timeout);
    }

    public void resubscribe() {
        try {
            unsubscribe();
            createSubscription();
        } catch (Exception e) {
            log.error("[NATS] Failed to resubscribe", e);
            subscription = null;
        }
    }

    public void unsubscribe() {
        if (subscription != null) {
            try {
                subscription.unsubscribe();
                log.info("[NATS] Unsubscribed from stream {} and subject {}",
                        subscribeAnnotation.event().getStreamName(),
                        subscribeAnnotation.event().getSubject());
            } catch (Exception e) {
                log.error("[NATS] Error unsubscribing", e);
            } finally {
                subscription = null;
            }
        }
    }

    private void createSubscription() throws IOException, JetStreamApiException {
        log.debug("[NATS] Creating subscription for stream {} and subject {}",
                subscribeAnnotation.event().getStreamName(),
                subscribeAnnotation.event().getSubject());

        NatsStreamUtil.createOrUpdateStream(
                natsConnection.jetStreamManagement(),
                subscribeAnnotation.event().getStreamName(),
                subscribeAnnotation.event().getSubject()
        );

        JetStream jetStream = natsConnection.jetStream();
        ConsumerConfiguration consumerConfig = ConsumerConfiguration.builder()
                .name(durableName)
                .durable(durableName)
                .ackWait(Duration.ofMinutes(subscribeAnnotation.timeoutMinutes()))
                .build();

        PushSubscribeOptions options = PushSubscribeOptions.builder()
                .name(durableName)
                .durable(durableName)
                .configuration(consumerConfig)
                .build();

        subscription = jetStream.subscribe(
                subscribeAnnotation.event().getSubject(),
                options
        );

        log.info("[NATS] Successfully created subscription for stream {} and subject {}",
                subscribeAnnotation.event().getStreamName(),
                subscribeAnnotation.event().getSubject());
    }
}