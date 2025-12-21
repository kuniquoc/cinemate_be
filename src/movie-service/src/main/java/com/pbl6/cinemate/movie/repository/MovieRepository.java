package com.pbl6.cinemate.movie.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pbl6.cinemate.movie.entity.Movie;
import com.pbl6.cinemate.movie.enums.MovieStatus;

public interface MovieRepository extends JpaRepository<Movie, UUID> {

    @Query("SELECT m FROM Movie m WHERE :status IS NULL OR m.status = :status")
    Page<Movie> findAllByStatus(@Param("status") MovieStatus status, Pageable pageable);

    @Query("""
            SELECT DISTINCT m FROM Movie m
            LEFT JOIN MovieActor ma ON ma.movie.id = m.id
            LEFT JOIN Actor a ON a.id = ma.actor.id
            LEFT JOIN MovieCategory mc ON mc.movie.id = m.id
            LEFT JOIN Category c ON c.id = mc.category.id
            WHERE (:status IS NULL OR m.status = :status)
                AND (LOWER(m.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(m.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(m.country) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(a.fullname) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<Movie> searchMoviesByKeyword(@Param("keyword") String keyword, @Param("status") MovieStatus status,
                                      Pageable pageable);

    @Query("SELECT m FROM Movie m ORDER BY m.rank ASC NULLS LAST")
    List<Movie> findTop10ByOrderByRankAsc(Pageable pageable);
    
    List<Movie> findByStatus(MovieStatus status);
    
    @Query("SELECT m FROM Movie m WHERE m.status = :status AND m.rank IS NOT NULL ORDER BY m.rank ASC")
    List<Movie> findTop10ByStatusOrderByRankAsc(@Param("status") MovieStatus status, Pageable pageable);
}
