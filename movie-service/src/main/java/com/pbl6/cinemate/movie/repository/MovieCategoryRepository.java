package com.pbl6.cinemate.movie.repository;

import com.pbl6.cinemate.movie.entity.MovieCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MovieCategoryRepository extends JpaRepository<MovieCategory, Long> {
    List<MovieCategory> findByMovieId(UUID movieId);
    List<MovieCategory> findByCategoryId(UUID categoryId);
    void deleteByMovieId(UUID movieId);
    void deleteByMovieIdAndCategoryId(UUID movieId, UUID categoryId);
}