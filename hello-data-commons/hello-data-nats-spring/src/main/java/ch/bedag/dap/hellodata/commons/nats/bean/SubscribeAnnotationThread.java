/*
 * Copyright © 2024, Kanton Bern
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package ch.bedag.dap.hellodata.commons.nats.bean;

import ch.bedag.dap.hellodata.commons.nats.annotation.JetStreamSubscribe;
import ch.bedag.dap.hellodata.commons.nats.util.NatsStreamUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.nats.client.*;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.ConsumerInfo;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.time.StopWatch;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;

/**
 * One stream subject configuration = one corresponding thread to delegate messages to beans subscribing
 */
@Log4j2
public class SubscribeAnnotationThread extends Thread {
    private static final int MAX_RETRY_DELAY_MS = 60000; // 1 minute
    private static final int INITIAL_RETRY_DELAY_MS = 1000; // 1 second
    private static final int MESSAGE_FETCH_TIMEOUT_SECONDS = 10;

    private final Connection natsConnection;
    private final JetStreamSubscribe subscribeAnnotation;
    @Getter
    private final List<BeanMethodWrapper> beanWrappers;
    private final String durableName;
    private final ExecutorService executorService;
    private final ObjectMapper objectMapper;

    private JetStreamSubscription subscription;
    private volatile boolean running = true;

    /**
     * Creates a new subscription thread for the given NATS configuration.
     */
    SubscribeAnnotationThread(Connection natsConnection, JetStreamSubscribe sub, List<BeanMethodWrapper> beanWrappers,
                              String durableName, ExecutorService executorService) {
        log.debug("[NATS] Creating subscription thread for beans listening to {}", beanWrappers.get(0).subscriptionId());
        this.natsConnection = natsConnection;
        this.subscribeAnnotation = sub;
        this.beanWrappers = beanWrappers;
        this.durableName = durableName;
        this.executorService = executorService;
        this.objectMapper = createObjectMapper();

        // Initialize subscription
        subscribe();
    }

    /**
     * Creates and configures the ObjectMapper for JSON deserialization.
     */
    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    /**
     * Unsubscribes from the NATS stream.
     */
    public void unsubscribe() {
        if (this.subscription != null) {
            this.subscription.unsubscribe();
            log.info("[NATS] Unsubscribed from NATS connection for stream {} and subject {}",
                    this.subscribeAnnotation.event().getStreamName(),
                    subscribeAnnotation.event().getSubject());
            this.subscription = null;
        }
    }

    @Override
    public void run() {
        BackoffStrategy backoff = new ExponentialBackoffStrategy(INITIAL_RETRY_DELAY_MS, MAX_RETRY_DELAY_MS);

        while (running) {
            try {
                ensureConsumerExists();
                fetchAndProcessMessage();
                backoff.reset();
            } catch (InterruptedException e) {
                handleInterruption(e);
            } catch (Exception e) {
                handleRunException(e, backoff);
            }
        }
    }

    /**
     * Fetches and processes a message from the NATS subscription.
     */
    private void fetchAndProcessMessage() throws InterruptedException {
        if (this.subscription == null) {
            log.warn("[NATS] Subscription to NATS is null. Please check if NATS is available for stream {} and subject {}",
                    this.subscribeAnnotation.event().getStreamName(),
                    subscribeAnnotation.event().getSubject());
            return;
        }

        log.debug("[NATS] Fetching message for stream {} and subject {}",
                this.subscribeAnnotation.event().getStreamName(),
                subscribeAnnotation.event().getSubject());

        Message message = this.subscription.nextMessage(Duration.ofSeconds(MESSAGE_FETCH_TIMEOUT_SECONDS));
        if (message != null) {
            log.debug("[NATS] Message received for stream {} and subject {}",
                    this.subscribeAnnotation.event().getStreamName(),
                    subscribeAnnotation.event().getSubject());

            if (this.subscribeAnnotation.asyncRun()) {
                processMessageAsynchronously(message);
            } else {
                processMessageSynchronously(message);
            }
        } else {
            log.debug("[NATS] No message available for stream {} and subject {}",
                    this.subscribeAnnotation.event().getStreamName(),
                    subscribeAnnotation.event().getSubject());
        }
    }

    private void handleInterruption(InterruptedException e) {
        log.error("[NATS] Thread interrupted", e);
        Thread.currentThread().interrupt();
        running = false;
    }

    /**
     * Handles exceptions that occur during the main run loop.
     */
    private void handleRunException(Exception e, BackoffStrategy backoff) {
        log.error("[NATS] Error processing messages: ", e);
        try {
            Thread.sleep(backoff.getNextDelay());
            if (e instanceof IllegalStateException) { // This subscription is inactive.
                log.info("[NATS] Attempting to resubscribe after IllegalStateException");
                subscribe();
            }
        } catch (InterruptedException ie) {
            handleInterruption(ie);
        }
    }

    /**
     * Returns the list of subscription IDs this thread is handling.
     */
    public List<String> getSubscriptionIds() {
        return this.beanWrappers.stream()
                .map(BeanMethodWrapper::subscriptionId)
                .toList();
    }

    /**
     * Shuts down the subscription thread.
     */
    public void shutdown() {
        log.info("[NATS] Shutting down subscription thread");
        running = false;
        unsubscribe();
    }

    /**
     * Ensures the consumer exists, creating it if necessary.
     */
    private void ensureConsumerExists() {
        try {
            JetStreamManagement jsm = natsConnection.jetStreamManagement();
            ConsumerInfo consumerInfo = jsm.getConsumerInfo(
                    this.subscribeAnnotation.event().getStreamName(),
                    durableName);

            if (consumerInfo == null) {
                log.warn("[NATS] Consumer {} for stream {} not found. Re-subscribing...",
                        durableName,
                        this.subscribeAnnotation.event().getStreamName());
                subscribe();
            }
        } catch (JetStreamApiException | IOException e) {
            log.error("[NATS] Error checking consumer status for stream {} and subject {}. Re-subscribing...",
                    this.subscribeAnnotation.event().getStreamName(),
                    subscribeAnnotation.event().getSubject(),
                    e);
            subscribe();
        }
    }

    /**
     * Processes a message asynchronously with timeout handling.
     */
    private void processMessageAsynchronously(Message message) {
        // Create a CompletableFuture for the message processing
        CompletableFuture<Void> processingTask = CompletableFuture.runAsync(
                () -> processMessageSynchronously(message),
                executorService);

        // Set up timeout handling
        ScheduledExecutorService timeoutScheduler = Executors.newScheduledThreadPool(1);
        ScheduledFuture<?> timeoutTask = timeoutScheduler.schedule(() -> {
            if (!processingTask.isDone()) {
                log.warn("[NATS] Message processing exceeded timeout of {} minutes. Cancelling task.",
                        this.subscribeAnnotation.timeoutMinutes());
                processingTask.cancel(true);
            }
        }, this.subscribeAnnotation.timeoutMinutes(), TimeUnit.MINUTES);

        // Clean up resources when the task completes
        processingTask.whenComplete((result, throwable) -> {
            timeoutTask.cancel(false);
            timeoutScheduler.shutdown();

            if (throwable != null) {
                log.error("[NATS] Error during asynchronous message processing", throwable);
            }
        });
    }

    /**
     * Creates or updates the NATS subscription.
     */
    private void subscribe() {
        try {
            log.debug("[NATS] Subscribing to NATS for stream {} and subject {}",
                    this.subscribeAnnotation.event().getStreamName(),
                    this.subscribeAnnotation.event().getSubject());

            // Ensure stream exists
            NatsStreamUtil.createOrUpdateStream(
                    natsConnection.jetStreamManagement(),
                    this.subscribeAnnotation.event().getStreamName(),
                    this.subscribeAnnotation.event().getSubject());

            // Configure and create subscription
            JetStream jetStream = natsConnection.jetStream();
            ConsumerConfiguration consumerConfig = createConsumerConfiguration();
            PushSubscribeOptions pushSubscribeOptions = createSubscribeOptions(consumerConfig);

            this.subscription = jetStream.subscribe(
                    this.subscribeAnnotation.event().getSubject(),
                    pushSubscribeOptions);

            log.info("[NATS] Successfully subscribed to stream {} and subject {}",
                    this.subscribeAnnotation.event().getStreamName(),
                    this.subscribeAnnotation.event().getSubject());
        } catch (IOException | JetStreamApiException e) {
            this.subscription = null;
            log.error("[NATS] Failed to subscribe to stream {} and subject {}",
                    this.subscribeAnnotation.event().getStreamName(),
                    this.subscribeAnnotation.event().getSubject(),
                    e);
        }
    }

    /**
     * Creates the consumer configuration for the subscription.
     */
    private ConsumerConfiguration createConsumerConfiguration() {
        return ConsumerConfiguration.builder()
                .name(this.durableName)
                .durable(this.durableName)
                .ackWait(Duration.ofMinutes(this.subscribeAnnotation.timeoutMinutes()))
                .build();
    }

    /**
     * Creates the subscribe options for the subscription.
     */
    private PushSubscribeOptions createSubscribeOptions(ConsumerConfiguration consumerConfig) {
        return PushSubscribeOptions.builder()
                .name(this.durableName)
                .durable(this.durableName)
                .configuration(consumerConfig)
                .build();
    }

    /**
     * Processes a message synchronously by passing it to all registered bean methods.
     */
    private void processMessageSynchronously(Message message) {
        try {
            Class<?> targetClass = this.subscribeAnnotation.event().getDataClass();
            StopWatch watch = new StopWatch();
            watch.start();

            // Deserialize message data
            Object messageData = objectMapper.readValue(message.getData(), targetClass);

            // Invoke all registered bean methods
            for (BeanMethodWrapper beanWrapper : beanWrappers) {
                log.debug("[NATS] Passing message to bean {}", beanWrapper.bean().getClass().getName());
                log.debug("[NATS] Expected type: {}, Method parameter type: {}",
                        targetClass.getName(),
                        beanWrapper.method().getParameterTypes()[0].getName());

                try {
                    beanWrapper.method().invoke(beanWrapper.bean(), messageData);
                    log.debug("[NATS] Message processing completed for {}. Duration: {}",
                            beanWrapper.bean().getClass().getName(),
                            watch.formatTime());
                } catch (IllegalAccessException | InvocationTargetException e) {
                    log.error("[NATS] Error invoking method on bean {}",
                            beanWrapper.bean().getClass().getName(),
                            e);
                }
            }

            // Acknowledge the message
            message.ack();
        } catch (IOException | CompletionException e) {
            log.error("[NATS] Error deserializing message data", e);
        }
    }
}
