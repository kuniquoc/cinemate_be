package com.pbl6.cinemate.movie.repository;

import com.pbl6.cinemate.movie.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    @Query("SELECT r FROM Review r WHERE r.deletedAt IS NULL")
    List<Review> findAllActiveReviews();

    @Query("SELECT r FROM Review r WHERE r.movie.id = :movieId AND r.deletedAt IS NULL")
    List<Review> findByMovieIdAndDeletedAtIsNull(@Param("movieId") UUID movieId);

    @Query("SELECT r FROM Review r WHERE r.customerId = :customerId AND r.deletedAt IS NULL")
    List<Review> findByCustomerIdAndDeletedAtIsNull(@Param("customerId") UUID customerId);

    @Query("SELECT r FROM Review r WHERE r.id = :id AND r.deletedAt IS NULL")
    Optional<Review> findByIdAndDeletedAtIsNull(@Param("id") UUID id);

    @Query("SELECT r FROM Review r WHERE r.movie.id = :movieId AND r.customerId = :customerId AND r.deletedAt IS NULL")
    Optional<Review> findByMovieIdAndCustomerIdAndDeletedAtIsNull(@Param("movieId") UUID movieId, @Param("customerId") UUID customerId);

    @Query("SELECT AVG(r.stars) FROM Review r WHERE r.movie.id = :movieId AND r.deletedAt IS NULL")
    Double findAverageStarsByMovieId(@Param("movieId") UUID movieId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.movie.id = :movieId AND r.deletedAt IS NULL")
    Long countByMovieIdAndDeletedAtIsNull(@Param("movieId") UUID movieId);
}
