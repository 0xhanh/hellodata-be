package ch.bedag.dap.hellodata.commons.nats.bean;

import ch.bedag.dap.hellodata.commons.nats.annotation.JetStreamSubscribe;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.time.StopWatch;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.*;

@Log4j2
@RequiredArgsConstructor
class MessageProcessor {
    private final ObjectMapper objectMapper;
    private final List<BeanMethodWrapper> beanWrappers;
    private final JetStreamSubscribe subscribeAnnotation;

    public void processSync(Message message) {
        try {
            Object messageData = deserializeMessage(message);
            processMessageData(messageData);
            message.ack();
        } catch (Exception e) {
            log.error("[NATS] Error processing message", e);
            throw new NatsSubscriptionException("Failed to process message", e);
        }
    }

    public void processAsync(Message message, ExecutorService executorService) {
        CompletableFuture.runAsync(() -> processSync(message), executorService)
                .orTimeout(subscribeAnnotation.timeoutMinutes(), TimeUnit.MINUTES)
                .exceptionally(throwable -> {
                    log.error("[NATS] Async processing failed", throwable);
                    return null;
                });
    }

    private Object deserializeMessage(Message message) throws IOException {
        return objectMapper.readValue(message.getData(),
                subscribeAnnotation.event().getDataClass());
    }

    /**
     * Logs the completion of message processing for a bean wrapper.
     */
    private void logProcessingComplete(BeanMethodWrapper wrapper, StopWatch watch) {
        log.debug("[NATS] Message processing completed for bean {} in {}",
                wrapper.bean().getClass().getName(),
                watch.formatTime());
    }

    private void processMessageData(Object messageData) {
        StopWatch watch = new StopWatch();
        watch.start();

        for (BeanMethodWrapper wrapper : beanWrappers) {
            try {
                wrapper.method().invoke(wrapper.bean(), messageData);
                logProcessingComplete(wrapper, watch);
            } catch (Exception e) {
                throw new NatsSubscriptionException(
                        "Failed to invoke method on bean: " + wrapper.bean().getClass().getName(), e);
            }
        }
    }
}