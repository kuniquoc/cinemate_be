package com.pbl6.cinemate.streaming.signaling.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "streaming")
public class SignalingProperties {

    private final Signaling signaling = new Signaling();
    private final Playback playback = new Playback();
    private final Messaging messaging = new Messaging();

    public Signaling getSignaling() {
        return signaling;
    }

    public Playback getPlayback() {
        return playback;
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

    public static class Playback {

        @Min(1)
        private int maxActivePeers = 3;

        @NotNull
        private Duration peerConnectTimeout = Duration.ofSeconds(5);

        @NotNull
        private Duration segmentRequestWait = Duration.ofMillis(150);

        @NotNull
        private Duration whoHasQueryTimeout = Duration.ofMillis(150);

        @NotNull
        private Duration fallbackHttpTimeout = Duration.ofMillis(700);

        @Min(1)
        private int minBufferPrefetch = 3;

        @Min(0)
        private int criticalBufferThreshold = 1;

        public int getMaxActivePeers() {
            return maxActivePeers;
        }

        public void setMaxActivePeers(int maxActivePeers) {
            this.maxActivePeers = maxActivePeers;
        }

        public Duration getPeerConnectTimeout() {
            return peerConnectTimeout;
        }

        public void setPeerConnectTimeout(Duration peerConnectTimeout) {
            this.peerConnectTimeout = peerConnectTimeout;
        }

        public Duration getSegmentRequestWait() {
            return segmentRequestWait;
        }

        public void setSegmentRequestWait(Duration segmentRequestWait) {
            this.segmentRequestWait = segmentRequestWait;
        }

        public Duration getWhoHasQueryTimeout() {
            return whoHasQueryTimeout;
        }

        public void setWhoHasQueryTimeout(Duration whoHasQueryTimeout) {
            this.whoHasQueryTimeout = whoHasQueryTimeout;
        }

        public Duration getFallbackHttpTimeout() {
            return fallbackHttpTimeout;
        }

        public void setFallbackHttpTimeout(Duration fallbackHttpTimeout) {
            this.fallbackHttpTimeout = fallbackHttpTimeout;
        }

        public int getMinBufferPrefetch() {
            return minBufferPrefetch;
        }

        public void setMinBufferPrefetch(int minBufferPrefetch) {
            this.minBufferPrefetch = minBufferPrefetch;
        }

        public int getCriticalBufferThreshold() {
            return criticalBufferThreshold;
        }

        public void setCriticalBufferThreshold(int criticalBufferThreshold) {
            this.criticalBufferThreshold = criticalBufferThreshold;
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
