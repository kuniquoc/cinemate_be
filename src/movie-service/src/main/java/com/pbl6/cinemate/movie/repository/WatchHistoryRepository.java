package com.pbl6.cinemate.movie.repository;

import com.pbl6.cinemate.movie.entity.WatchHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WatchHistoryRepository extends JpaRepository<WatchHistory, UUID> {

        Optional<WatchHistory> findByMovieIdAndCustomerId(UUID movieId, UUID customerId);

        @Query("SELECT wh FROM WatchHistory wh WHERE wh.customerId = :customerId ORDER BY wh.updatedAt DESC")
        Page<WatchHistory> findByCustomerIdOrderByUpdatedAtDesc(@Param("customerId") UUID customerId,
                        Pageable pageable);

        @Query("SELECT wh FROM WatchHistory wh WHERE wh.customerId = :customerId " +
                        "AND wh.updatedAt >= :startOfDay AND wh.updatedAt < :endOfDay " +
                        "ORDER BY wh.updatedAt DESC")
        Page<WatchHistory> findByCustomerIdAndDate(
                        @Param("customerId") UUID customerId,
                        @Param("startOfDay") Instant startOfDay,
                        @Param("endOfDay") Instant endOfDay,
                        Pageable pageable);

        @Query(value = "SELECT DATE(wh.updated_at AT TIME ZONE 'UTC') as watch_date, COUNT(*) as count " +
                        "FROM watch_history wh " +
                        "WHERE wh.customer_id = :customerId " +
                        "GROUP BY DATE(wh.updated_at AT TIME ZONE 'UTC') " +
                        "ORDER BY watch_date DESC", countQuery = "SELECT COUNT(DISTINCT DATE(wh.updated_at AT TIME ZONE 'UTC')) "
                                        +
                                        "FROM watch_history wh WHERE wh.customer_id = :customerId", nativeQuery = true)
        Page<Object[]> findDistinctDatesByCustomerId(@Param("customerId") UUID customerId, Pageable pageable);

        void deleteByMovieIdAndCustomerId(UUID movieId, UUID customerId);

        /**
         * Count watch views grouped by date and category within date range
         * Joins watch_history -> movie -> movie_categories -> categories
         * Returns: [date (Date), categoryName (String), count (Long)]
         */
        @Query(value = "SELECT DATE(wh.updated_at AT TIME ZONE 'UTC') as watch_date, " +
                        "c.name as category_name, COUNT(*) as view_count " +
                        "FROM watch_history wh " +
                        "JOIN movies m ON wh.movie_id = m.id " +
                        "JOIN movie_categories mc ON m.id = mc.movie_id " +
                        "JOIN categories c ON mc.category_id = c.id " +
                        "WHERE wh.updated_at >= :startDate AND wh.updated_at < :endDate " +
                        "GROUP BY DATE(wh.updated_at AT TIME ZONE 'UTC'), c.name " +
                        "ORDER BY watch_date DESC, category_name ASC", nativeQuery = true)
        List<Object[]> countWatchViewsByDateAndCategory(
                        @Param("startDate") Instant startDate,
                        @Param("endDate") Instant endDate);
}
