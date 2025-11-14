package com.pbl6.cinemate.movie.repository;

import com.pbl6.cinemate.movie.entity.MovieCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MovieCategoryRepository extends JpaRepository<MovieCategory, UUID> {
    @Query("SELECT mc FROM MovieCategory mc JOIN FETCH mc.category WHERE mc.movie.id = :movieId")
    List<MovieCategory> findByMovieIdWithCategory(@Param("movieId") UUID movieId);

    List<MovieCategory> findByMovieId(UUID movieId);

    List<MovieCategory> findByCategoryId(UUID categoryId);

    void deleteByMovieId(UUID movieId);

    void deleteByMovieIdAndCategoryId(UUID movieId, UUID categoryId);
}