package com.pbl6.microservices.customer_service.repository;

import com.pbl6.microservices.customer_service.entity.Favorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FavoriteRepository extends JpaRepository<Favorite, UUID> {
    List<Favorite> findByCustomerId(UUID customerId);

    Page<Favorite> findByCustomerId(UUID customerId, Pageable pageable);

    void deleteByCustomerIdAndMovieId(UUID customerId, UUID movieId);

    boolean existsByCustomerIdAndMovieId(UUID customerId, UUID movieId);
}
