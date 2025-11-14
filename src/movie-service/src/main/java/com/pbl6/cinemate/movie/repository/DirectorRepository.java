package com.pbl6.cinemate.movie.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pbl6.cinemate.movie.entity.Director;

@Repository
public interface DirectorRepository extends JpaRepository<Director, UUID> {
}
