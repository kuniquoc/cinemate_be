package com.pbl6.cinemate.streaming.messaging;

import com.pbl6.cinemate.streaming.config.StreamingProperties;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.stereotype.Component;

@Component
public class KafkaStreamEventSubscriber implements StreamEventSubscriber {

    private static final Logger log = LoggerFactory.getLogger(KafkaStreamEventSubscriber.class);
    private final ConcurrentKafkaListenerContainerFactory<String, String> containerFactory;
    private final StreamingProperties properties;
    private final Map<String, MessageListenerContainer> containers = new ConcurrentHashMap<>();

    public KafkaStreamEventSubscriber(
        ConcurrentKafkaListenerContainerFactory<String, String> containerFactory,
        StreamingProperties properties
    ) {
        this.containerFactory = containerFactory;
        this.properties = properties;
    }

    @Override
    public void ensureSubscribed(String streamId) {
        containers.computeIfAbsent(streamId, this::startContainer);
    }

    private MessageListenerContainer startContainer(String streamId) {
        String topic = topicForStream(streamId);
        ConcurrentMessageListenerContainer<String, String> container = containerFactory.createContainer(topic);
        ContainerProperties containerProperties = container.getContainerProperties();
    MessageListener<String, String> messageListener = (ConsumerRecord<String, String> consumerRecord) ->
            log.debug("Received Kafka event for stream {}: key={} value={} partition={} offset={} ",
                streamId,
        consumerRecord.key(),
        consumerRecord.value(),
        consumerRecord.partition(),
        consumerRecord.offset());
        containerProperties.setMessageListener(messageListener);
        String groupId = properties.getMessaging().getGroupId();
        if (groupId != null && !groupId.isBlank()) {
            containerProperties.setGroupId(groupId);
        }
        container.start();
        log.info("Subscribed to Kafka topic {}", topic);
        return container;
    }

    private String topicForStream(String streamId) {
        return properties.getMessaging().getTopicPrefix() + streamId + ".events";
    }
}
