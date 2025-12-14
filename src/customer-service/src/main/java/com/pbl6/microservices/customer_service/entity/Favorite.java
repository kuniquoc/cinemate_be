package com.pbl6.microservices.customer_service.entity;

import com.pbl6.cinemate.shared.entity.AbstractBaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "favorites", uniqueConstraints = @UniqueConstraint(columnNames = {"customer_id", "movie_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Favorite extends AbstractBaseEntity {

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "movie_id", nullable = false)
    private UUID movieId;
}

