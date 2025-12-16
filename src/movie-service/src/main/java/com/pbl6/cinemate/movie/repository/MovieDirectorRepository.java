package com.pbl6.cinemate.movie.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.pbl6.cinemate.movie.entity.MovieDirector;

@Repository
public interface MovieDirectorRepository extends JpaRepository<MovieDirector, UUID> {

    @Query("SELECT md FROM MovieDirector md JOIN FETCH md.director WHERE md.movie.id = :movieId")
    List<MovieDirector> findByMovieIdWithDirector(@Param("movieId") UUID movieId);

    List<MovieDirector> findByMovieId(UUID movieId);

    List<MovieDirector> findByDirectorId(UUID directorId);

    @Modifying
    @Query("DELETE FROM MovieDirector md WHERE md.movie.id = :movieId")
    void deleteByMovieId(@Param("movieId") UUID movieId);

    @Modifying
    @Query("DELETE FROM MovieDirector md WHERE md.director.id = :directorId")
    void deleteByDirectorId(@Param("directorId") UUID directorId);

    void deleteByMovieIdAndDirectorId(UUID movieId, UUID directorId);
}
