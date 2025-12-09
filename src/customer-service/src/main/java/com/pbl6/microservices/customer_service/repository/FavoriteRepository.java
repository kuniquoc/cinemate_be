package com.pbl6.microservices.customer_service.repository;

import com.pbl6.microservices.customer_service.entity.Favorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface FavoriteRepository extends JpaRepository<Favorite, UUID> {
    List<Favorite> findByCustomerId(UUID customerId);

    Page<Favorite> findByCustomerId(UUID customerId, Pageable pageable);

    void deleteByCustomerIdAndMovieId(UUID customerId, UUID movieId);

    boolean existsByCustomerIdAndMovieId(UUID customerId, UUID movieId);

    /**
     * Count favorites grouped by date and movieId within date range
     * Returns: [date (LocalDate), movieId (UUID), count (Long)]
     */
    @Query(value = "SELECT DATE(f.created_at) as favorite_date, f.movie_id, COUNT(*) as count " +
            "FROM favorites f " +
            "WHERE f.created_at >= :startDate AND f.created_at < :endDate " +
            "GROUP BY DATE(f.created_at), f.movie_id " +
            "ORDER BY favorite_date DESC", nativeQuery = true)
    List<Object[]> countFavoritesByDateAndMovie(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
