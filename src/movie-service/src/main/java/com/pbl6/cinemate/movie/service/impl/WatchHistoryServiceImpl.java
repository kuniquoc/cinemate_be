package com.pbl6.cinemate.movie.service.impl;

import com.pbl6.cinemate.movie.dto.request.WatchProgressRequest;
import com.pbl6.cinemate.movie.dto.response.*;
import com.pbl6.cinemate.movie.entity.Movie;
import com.pbl6.cinemate.movie.entity.WatchHistory;
import com.pbl6.cinemate.movie.repository.*;
import com.pbl6.cinemate.movie.client.InteractionRecommenderClient;
import com.pbl6.cinemate.movie.client.dto.WatchEventRequest;
import com.pbl6.cinemate.movie.service.WatchHistoryService;
import com.pbl6.cinemate.shared.dto.general.PaginatedResponse;
import com.pbl6.cinemate.shared.exception.NotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WatchHistoryServiceImpl implements WatchHistoryService {

        private final WatchHistoryRepository watchHistoryRepository;
        private final MovieRepository movieRepository;
        private final MovieCategoryRepository movieCategoryRepository;
        private final InteractionRecommenderClient interactionClient;

        @Transactional
        @Override
        public void saveWatchProgress(UUID movieId, UUID customerId, WatchProgressRequest request) {
                log.info("Saving watch progress for movie ID: {} by customer ID: {}", movieId, customerId);

                Movie movie = movieRepository.findById(movieId)
                                .orElseThrow(() -> new NotFoundException("Movie not found with ID: " + movieId));

                Optional<WatchHistory> existingHistory = watchHistoryRepository.findByMovieIdAndCustomerId(movieId,
                                customerId);

                WatchHistory watchHistory;
                if (existingHistory.isPresent()) {
                        if (request.lastWatchedPosition() > movie.getDuration()) {
                                request = new WatchProgressRequest(movie.getDuration());
                        }

                        watchHistory = existingHistory.get();
                        watchHistory.setLastWatchedPosition(request.lastWatchedPosition());
                        watchHistory.setTotalDuration(movie.getDuration());
                } else {
                        watchHistory = WatchHistory.builder()
                                        .movie(movie)
                                        .customerId(customerId)
                                        .lastWatchedPosition(request.lastWatchedPosition())
                                        .totalDuration(movie.getDuration())
                                        .build();
                }

                watchHistoryRepository.save(watchHistory);
                log.info("Watch progress saved successfully for movie ID: {} by customer ID: {}", movieId, customerId);

                // Best-effort: send watch event to interaction recommender
                try {
                        Integer watchDuration = movie.getDuration() != null ? movie.getDuration().intValue() : null;
                        Double progress = watchHistory.getProgressPercent();
                        var watchReq = WatchEventRequest.create(
                                        customerId,
                                        movieId,
                                        watchDuration,
                                        null,
                                        null,
                                        null,
                                        progress);
                        interactionClient.trackWatchEvent(watchReq);
                } catch (Exception e) {
                        log.debug("Failed to send watch event: {}", e.getMessage());
                }
        }

        @Transactional(readOnly = true)
        @Override
        public WatchProgressResponse getWatchProgress(UUID movieId, UUID customerId) {
                Movie movie = movieRepository.findById(movieId)
                                .orElseThrow(() -> new NotFoundException("Movie not found with ID: " + movieId));

                long totalDuration = movie.getDuration() != null ? movie.getDuration() : 0L;

                long lastWatched = watchHistoryRepository.findByMovieIdAndCustomerId(movieId, customerId)
                                .map(WatchHistory::getLastWatchedPosition)
                                .orElse(0L);

                return new WatchProgressResponse(lastWatched, totalDuration);
        }

        @Transactional(readOnly = true)
        @Override
        public PaginatedResponse<WatchHistoryDateResponse> getWatchHistoryDates(UUID customerId, int page, int size) {
                log.info("Getting watch history dates for customer ID: {}", customerId);

                var pageable = PageRequest.of(page, size);
                Page<Object[]> datePage = watchHistoryRepository.findDistinctDatesByCustomerId(customerId, pageable);

                List<WatchHistoryDateResponse> dates = datePage.getContent().stream()
                                .map(row -> {
                                        LocalDate date = ((Date) row[0]).toLocalDate();
                                        Long count = ((Number) row[1]).longValue();
                                        return new WatchHistoryDateResponse(date, count);
                                })
                                .toList();

                return new PaginatedResponse<>(dates, datePage.getNumber(), datePage.getSize(),
                                datePage.getTotalPages());
        }

        @Transactional(readOnly = true)
        @Override
        public PaginatedResponse<WatchHistoryResponse> getWatchHistoryByDate(UUID customerId, LocalDate date, int page,
                        int size) {
                log.info("Getting watch history for customer ID: {} on date: {}", customerId, date);

                var pageable = PageRequest.of(page, size);

                // Convert LocalDate to start and end of day in UTC
                Instant startOfDay = date.atStartOfDay(ZoneOffset.UTC).toInstant();
                Instant endOfDay = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

                Page<WatchHistory> historyPage = watchHistoryRepository.findByCustomerIdAndDate(
                                customerId, startOfDay, endOfDay, pageable);

                List<WatchHistoryResponse> historyList = historyPage.getContent().stream()
                                .map(this::mapToWatchHistoryResponse)
                                .toList();

                return new PaginatedResponse<>(historyList, historyPage.getNumber(), historyPage.getSize(),
                                historyPage.getTotalPages());
        }

        @Transactional(readOnly = true)
        @Override
        public Long getLastWatchedPosition(UUID movieId, UUID customerId) {
                if (customerId == null) {
                        return null;
                }
                return watchHistoryRepository.findByMovieIdAndCustomerId(movieId, customerId)
                                .map(WatchHistory::getLastWatchedPosition)
                                .orElse(null);
        }

        @Transactional
        @Override
        public void deleteWatchHistory(UUID movieId, UUID customerId) {
                log.info("Deleting watch history for movie ID: {} by customer ID: {}", movieId, customerId);
                watchHistoryRepository.deleteByMovieIdAndCustomerId(movieId, customerId);
        }

        private WatchHistoryResponse mapToWatchHistoryResponse(WatchHistory watchHistory) {
                Movie movie = watchHistory.getMovie();

                List<CategoryResponse> categories = getCategoriesForMovie(movie.getId());

                return new WatchHistoryResponse(
                                movie.getId(),
                                movie.getTitle(),
                                movie.getDescription(),
                                movie.getQualities(),
                                movie.getHorizontalPoster(),
                                movie.getAge(),
                                movie.getYear(),
                                categories,
                                watchHistory.getLastWatchedPosition(),
                                watchHistory.getTotalDuration(),
                                watchHistory.getProgressPercent());
        }

        private List<CategoryResponse> getCategoriesForMovie(UUID movieId) {
                return movieCategoryRepository.findByMovieIdWithCategory(movieId)
                                .stream()
                                .map(mc -> CategoryResponse.builder()
                                                .id(mc.getCategory().getId())
                                                .name(mc.getCategory().getName())
                                                .build())
                                .toList();
        }
}
