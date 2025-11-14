package com.pbl6.cinemate.movie.repository;

import com.pbl6.cinemate.movie.entity.MovieActor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MovieActorRepository extends JpaRepository<MovieActor, UUID> {

    @Query("SELECT ma FROM MovieActor ma JOIN FETCH ma.actor WHERE ma.movie.id = :movieId")
    List<MovieActor> findByMovieIdWithActor(@Param("movieId") UUID movieId);

    List<MovieActor> findByMovieId(UUID movieId);

    @Modifying
    @Query("DELETE FROM MovieActor ma WHERE ma.movie.id = :movieId")
    void deleteByMovieId(@Param("movieId") UUID movieId);

    @Query("SELECT COUNT(ma) > 0 FROM MovieActor ma WHERE ma.movie.id = :movieId AND ma.actor.id = :actorId")
    boolean existsByMovieIdAndActorId(@Param("movieId") UUID movieId, @Param("actorId") UUID actorId);
}
