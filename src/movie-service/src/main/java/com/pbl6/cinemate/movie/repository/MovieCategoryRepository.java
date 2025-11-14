package com.pbl6.cinemate.movie.repository;

import com.pbl6.cinemate.movie.entity.MovieCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    @Modifying
    @Query("DELETE FROM MovieCategory mc WHERE mc.movie.id = :movieId")
    void deleteByMovieId(@Param("movieId") UUID movieId);

    @Modifying
    @Query("DELETE FROM MovieCategory mc WHERE mc.category.id = :categoryId")
    void deleteByCategoryId(@Param("categoryId") UUID categoryId);

    void deleteByMovieIdAndCategoryId(UUID movieId, UUID categoryId);
}