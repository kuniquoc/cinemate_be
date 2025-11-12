package com.pbl6.cinemate.movie.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pbl6.cinemate.movie.entity.Movie;

public interface MovieRepository extends JpaRepository<Movie, UUID> {
    
    @Query("""
            SELECT DISTINCT m FROM Movie m
            LEFT JOIN MovieActor ma ON ma.movie.id = m.id
            LEFT JOIN Actor a ON a.id = ma.actor.id
            LEFT JOIN MovieCategory mc ON mc.movieId = m.id
            LEFT JOIN Category c ON c.id = mc.categoryId
            WHERE LOWER(m.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(m.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(m.country) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(a.fullname) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            """)
    Page<Movie> searchMoviesByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
