package com.pbl6.cinemate.streaming.signaling.config;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "streaming")
public class SignalingProperties {

    private final Signaling signaling = new Signaling();
    private final Messaging messaging = new Messaging();

    public Signaling getSignaling() {
        return signaling;
    }

    public Messaging getMessaging() {
        return messaging;
    }

    public static class Signaling {

        @NotNull
        private Duration redisTtlSegmentKeys = Duration.ofSeconds(90);

        @NotNull
        private Duration peerLastSeenTtl = Duration.ofSeconds(60);

        public Duration getRedisTtlSegmentKeys() {
            return redisTtlSegmentKeys;
        }

        public void setRedisTtlSegmentKeys(Duration redisTtlSegmentKeys) {
            this.redisTtlSegmentKeys = redisTtlSegmentKeys;
        }

        public Duration getPeerLastSeenTtl() {
            return peerLastSeenTtl;
        }

        public void setPeerLastSeenTtl(Duration peerLastSeenTtl) {
            this.peerLastSeenTtl = peerLastSeenTtl;
        }
    }

    public static class Messaging {

        @NotNull
        private String topicPrefix = "stream.";

        private String groupId = "streaming-signaling";

        public String getTopicPrefix() {
            return topicPrefix;
        }

        public void setTopicPrefix(String topicPrefix) {
            this.topicPrefix = topicPrefix;
        }

        public String getGroupId() {
            return groupId;
        }

        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }
    }
}
