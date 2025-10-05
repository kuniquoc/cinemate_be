package com.pbl6.cinemate.movie.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pbl6.cinemate.movie.entity.Movie;

public interface MovieRepository extends JpaRepository<Movie, UUID> {
}
