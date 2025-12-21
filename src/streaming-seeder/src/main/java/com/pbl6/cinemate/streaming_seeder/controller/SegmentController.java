package com.pbl6.cinemate.streaming_seeder.controller;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pbl6.cinemate.shared.security.CurrentUser;
import com.pbl6.cinemate.shared.security.UserPrincipal;
import com.pbl6.cinemate.streaming_seeder.client.MovieServiceClient;
import com.pbl6.cinemate.streaming_seeder.client.PaymentServiceClient;
import com.pbl6.cinemate.streaming_seeder.dto.CachedSegment;
import com.pbl6.cinemate.streaming_seeder.dto.ContentAccessRequest;
import com.pbl6.cinemate.streaming_seeder.dto.ContentAccessResponse;
import com.pbl6.cinemate.streaming_seeder.exception.ContentAccessDeniedException;
import com.pbl6.cinemate.streaming_seeder.service.OriginSegmentFetcher;
import com.pbl6.cinemate.streaming_seeder.service.SeederService;
import com.pbl6.cinemate.streaming_seeder.service.SegmentFileServer;
import com.pbl6.cinemate.streaming_seeder.service.SegmentLocator;
import com.pbl6.cinemate.streaming_seeder.validation.SegmentIdentifierValidator;

@RestController
@RequestMapping("/api/v1/streams/")
public class SegmentController {

    private static final Logger log = LoggerFactory.getLogger(SegmentController.class);

    private final SegmentIdentifierValidator validator;
    private final SegmentLocator segmentLocator;
    private final SegmentFileServer fileServer;
    private final OriginSegmentFetcher originSegmentFetcher;
    private final SeederService seederService;
    private final PaymentServiceClient paymentServiceClient;
    private final MovieServiceClient movieServiceClient;

    public SegmentController(
            SegmentIdentifierValidator validator,
            SegmentLocator segmentLocator,
            SegmentFileServer fileServer,
            OriginSegmentFetcher originSegmentFetcher,
            SeederService seederService,
            PaymentServiceClient paymentServiceClient,
            MovieServiceClient movieServiceClient) {
        this.validator = Objects.requireNonNull(validator);
        this.segmentLocator = Objects.requireNonNull(segmentLocator);
        this.fileServer = Objects.requireNonNull(fileServer);
        this.originSegmentFetcher = Objects.requireNonNull(originSegmentFetcher);
        this.seederService = Objects.requireNonNull(seederService);
        this.paymentServiceClient = Objects.requireNonNull(paymentServiceClient);
        this.movieServiceClient = Objects.requireNonNull(movieServiceClient);
    }

    @GetMapping("/movies/{movieId}/master.m3u8")
    public ResponseEntity<Resource> getMasterPlaylist(
            @PathVariable("movieId") String movieId,
            @CurrentUser UserPrincipal userPrincipal) throws IOException {
        if (!validator.isSafeIdentifier(movieId)) {
            return ResponseEntity.badRequest().build();
        }
        
        return serveSegment(movieId, null, "master");
    }

    @GetMapping("/movies/{movieId}/{qualityId}/init.{ext}")
    public ResponseEntity<Resource> getInitSegment(
            @PathVariable("movieId") String movieId,
            @PathVariable("qualityId") String qualityId,
            @PathVariable("ext") String ext,
            @CurrentUser UserPrincipal userPrincipal) throws IOException {
        if (!validator.isSafeIdentifier(movieId) || !validator.isSafeIdentifier(qualityId)) {
            return ResponseEntity.badRequest().build();
        }   
        
        return serveSegment(movieId, qualityId, "init");
    }

    @GetMapping("/movies/{movieId}/{qualityId}/playlist.m3u8")
    public ResponseEntity<Resource> getVariantPlaylist(
            @PathVariable("movieId") String movieId,
            @PathVariable("qualityId") String qualityId,
            @CurrentUser UserPrincipal userPrincipal) throws IOException {
        if (!validator.isSafeIdentifier(movieId) || !validator.isSafeIdentifier(qualityId)) {
            return ResponseEntity.badRequest().build();
        }

        return serveSegment(movieId, qualityId, "playlist");
    }

    @GetMapping("/movies/{movieId}/{qualityId}/{segmentId}")
    public ResponseEntity<Resource> getSegment(
            @PathVariable("movieId") String movieId,
            @PathVariable("qualityId") String qualityId,
            @PathVariable("segmentId") String segmentId,
            @CurrentUser UserPrincipal userPrincipal) throws IOException {
        if (!validator.isSafeIdentifier(movieId) || !validator.isSafeIdentifier(qualityId)
                || !validator.isSafeIdentifier(segmentId)) {
            return ResponseEntity.badRequest().build();
        }
        
        
        return serveSegment(movieId, qualityId, segmentId);
    }

    /**
     * Check if user has access to the content
     * @param movieId The movie ID
     * @param userPrincipal The authenticated user
     * @throws ContentAccessDeniedException if user doesn't have access with detailed reason
     */
    private void checkAccess(String movieId, UserPrincipal userPrincipal) {
        try {
            UUID movieUUID = UUID.fromString(movieId);
            
            // Get movie category IDs from movie-service
            List<UUID> categoryIds = movieServiceClient.getMovieCategoryIds(movieUUID);
            
            if (categoryIds == null || categoryIds.isEmpty()) {
                log.warn("No categories found for movie: {}", movieId);
                // Allow access if no categories (shouldn't happen in production)
                return;
            }
            
            // Check content access via payment-service
            ResponseEntity<Map<String, Object>> response = paymentServiceClient.checkContentAccess(
                    userPrincipal.getId(),
                    categoryIds,
                    0 // We don't track watch time in streaming-seeder
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                Object data = body.get("data");
                
                if (data instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> accessData = (Map<String, Object>) data;
                    Object allowed = accessData.get("allowed");
                    
                    if (allowed instanceof Boolean) {
                        boolean hasAccess = (Boolean) allowed;
                        
                        if (!hasAccess) {
                            // Extract detailed error information
                            String reason = (String) accessData.getOrDefault("reason", 
                                    "You don't have access to this content");
                            Boolean isKid = (Boolean) accessData.get("isKid");
                            Integer remainingTime = (Integer) accessData.get("remainingWatchTimeMinutes");
                            
                            @SuppressWarnings("unchecked")
                            List<Object> blockedCategoryIdsRaw = (List<Object>) accessData.get("blockedCategoryIds");
                            
                            // Convert to List<UUID> - handle both String and UUID types
                            List<UUID> blockedCategoryIds = null;
                            if (blockedCategoryIdsRaw != null && !blockedCategoryIdsRaw.isEmpty()) {
                                blockedCategoryIds = blockedCategoryIdsRaw.stream()
                                        .map(obj -> {
                                            if (obj instanceof String) {
                                                return UUID.fromString((String) obj);
                                            } else if (obj instanceof UUID) {
                                                return (UUID) obj;
                                            }
                                            return null;
                                        })
                                        .filter(Objects::nonNull)
                                        .collect(java.util.stream.Collectors.toList());
                            }
                            
                            // Clean up reason message - remove UUID mention if it's a category block
                            if (reason.contains("category") && reason.contains("is blocked")) {
                                reason = "Content restricted by parent";
                            }
                            
                            // Fetch category names if there are blocked categories
                            List<String> blockedCategoryNames = null;
                            if (blockedCategoryIds != null && !blockedCategoryIds.isEmpty()) {
                                try {
                                    Map<String, String> categoryNamesMap = movieServiceClient.getCategoryNames(blockedCategoryIds);
                                    blockedCategoryNames = blockedCategoryIds.stream()
                                            .map(id -> categoryNamesMap.getOrDefault(id.toString(), id.toString()))
                                            .collect(java.util.stream.Collectors.toList());
                                } catch (Exception e) {
                                    log.warn("Failed to fetch category names: {}", e.getMessage());
                                    // Use IDs as fallback
                                    blockedCategoryNames = blockedCategoryIds.stream()
                                            .map(UUID::toString)
                                            .collect(java.util.stream.Collectors.toList());
                                }
                            }
                            
                            log.info("Content access denied for user {} on movie {}: {}", 
                                    userPrincipal.getId(), movieId, reason);
                            
                            throw new ContentAccessDeniedException(
                                    reason, 
                                    isKid, 
                                    remainingTime, 
                                    blockedCategoryNames
                            );
                        }
                        
                        log.info("Content access granted for user {} on movie {}", 
                                userPrincipal.getId(), movieId);
                        return;
                    }
                }
            }
            
            log.warn("Failed to check content access for user {} on movie {}: response status={}, body={}", 
                    userPrincipal.getId(), movieId, response.getStatusCode(), response.getBody());
            throw new ContentAccessDeniedException(
                    "Unable to verify content access. Please try again later."
            );
            
        } catch (ContentAccessDeniedException e) {
            // Re-throw our custom exception
            throw e;
        } catch (Exception e) {
            log.error("Error checking content access for movie {}: {} - {}", 
                    movieId, e.getClass().getSimpleName(), e.getMessage(), e);
            throw new ContentAccessDeniedException(
                    "An error occurred while verifying your access to this content. Please try again later."
            );
        }
    }

    /**
     * Serves a segment by locating it in cache or fetching from origin.
     */
    private ResponseEntity<Resource> serveSegment(String movieId, String qualityId, String segmentId)
            throws IOException {
        Path path = segmentLocator.locate(movieId, qualityId, segmentId);

        if (path == null) {
            // Try to fetch from origin
            Optional<CachedSegment> fetched = originSegmentFetcher.fetchFromOrigin(movieId, qualityId, segmentId);
            if (fetched.isPresent()) {
                CachedSegment segment = fetched.get();
                seederService.registerFetchedSegment(segment);
                path = segment.path();
            }
        }

        if (path == null) {
            log.debug("Segment {} not found for movie {} quality {}", segmentId, movieId, qualityId);
            return ResponseEntity.notFound().build();
        }

        return fileServer.serve(path);
    }
}
