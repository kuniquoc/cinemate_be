package com.pbl6.cinemate.streaming.seeder;

import java.nio.file.Path;
import java.time.Instant;

public record CachedSegment(String streamId, String segmentId, Path path, Instant lastModified) {
}
