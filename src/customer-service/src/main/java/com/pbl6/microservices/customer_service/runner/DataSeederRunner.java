package com.pbl6.microservices.customer_service.runner;

import com.pbl6.microservices.customer_service.entity.Customer;
import com.pbl6.microservices.customer_service.entity.Favorite;
import com.pbl6.microservices.customer_service.enums.Gender;
import com.pbl6.microservices.customer_service.repository.CustomerRepository;
import com.pbl6.microservices.customer_service.repository.FavoriteRepository;
import com.pbl6.cinemate.shared.constants.SeedUUIDs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Seeds initial customer data for development/testing purposes.
 * Only runs when app.seed.enabled=true and the customers table is empty.
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class DataSeederRunner implements ApplicationRunner {

    private final CustomerRepository customerRepository;
    private final FavoriteRepository favoriteRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (customerRepository.count() > 0) {
            log.info("Customers table is not empty. Skipping data seeding.");
            return;
        }

        log.info("Starting customer data seeding...");

        List<Customer> customers = createCustomers();
        customerRepository.saveAll(customers);
        log.info("Successfully seeded {} customers", customers.size());

        List<Favorite> favorites = createFavorites();
        favoriteRepository.saveAll(favorites);
        log.info("Successfully seeded {} favorites", favorites.size());
    }

    private List<Customer> createCustomers() {
        return Arrays.asList(
                // Admin customers (mapped to admin users)
                createCustomer(SeedUUIDs.Customers.CUSTOMER_ADMIN_01, SeedUUIDs.Users.ADMIN_01,
                        "Super", "Admin", Gender.MALE, LocalDate.of(1985, 1, 15)),
                createCustomer(SeedUUIDs.Customers.CUSTOMER_ADMIN_02, SeedUUIDs.Users.ADMIN_02,
                        "System", "Admin", Gender.FEMALE, LocalDate.of(1988, 3, 20)),

                // Regular customers with various genders
                createCustomer(SeedUUIDs.Customers.CUSTOMER_01, SeedUUIDs.Users.USER_01,
                        "John", "Doe", Gender.MALE, LocalDate.of(1990, 5, 10)),
                createCustomer(SeedUUIDs.Customers.CUSTOMER_02, SeedUUIDs.Users.USER_02,
                        "Jane", "Smith", Gender.FEMALE, LocalDate.of(1992, 8, 25)),
                createCustomer(SeedUUIDs.Customers.CUSTOMER_03, SeedUUIDs.Users.USER_03,
                        "Robert", "Johnson", Gender.MALE, LocalDate.of(1988, 12, 5)),
                createCustomer(SeedUUIDs.Customers.CUSTOMER_04, SeedUUIDs.Users.USER_04,
                        "Emily", "Williams", Gender.FEMALE, LocalDate.of(1995, 2, 14)),
                createCustomer(SeedUUIDs.Customers.CUSTOMER_05, SeedUUIDs.Users.USER_05,
                        "Michael", "Brown", Gender.MALE, LocalDate.of(1987, 7, 30)),
                createCustomer(SeedUUIDs.Customers.CUSTOMER_06, SeedUUIDs.Users.USER_06,
                        "Sarah", "Davis", Gender.FEMALE, LocalDate.of(1993, 11, 18)),
                createCustomer(SeedUUIDs.Customers.CUSTOMER_07, SeedUUIDs.Users.USER_07,
                        "David", "Miller", Gender.MALE, LocalDate.of(1991, 4, 22)),
                createCustomer(SeedUUIDs.Customers.CUSTOMER_08, SeedUUIDs.Users.USER_08,
                        "Lisa", "Wilson", Gender.FEMALE, LocalDate.of(1989, 9, 8)),
                createCustomer(SeedUUIDs.Customers.CUSTOMER_09, SeedUUIDs.Users.USER_09,
                        "James", "Moore", Gender.OTHER, LocalDate.of(1994, 6, 12)),
                createCustomer(SeedUUIDs.Customers.CUSTOMER_10, SeedUUIDs.Users.USER_10,
                        "Jennifer", "Taylor", Gender.FEMALE, LocalDate.of(1996, 1, 28)),
                createCustomer(SeedUUIDs.Customers.CUSTOMER_11, SeedUUIDs.Users.USER_11,
                        "William", "Anderson", Gender.MALE, LocalDate.of(1986, 10, 3)),
                createCustomer(SeedUUIDs.Customers.CUSTOMER_12, SeedUUIDs.Users.USER_12,
                        "Jessica", "Thomas", Gender.FEMALE, LocalDate.of(1997, 3, 17)),
                createCustomer(SeedUUIDs.Customers.CUSTOMER_13, SeedUUIDs.Users.USER_13,
                        "Daniel", "Jackson", Gender.OTHER, LocalDate.of(1990, 8, 9)));
    }

    private Customer createCustomer(UUID id, UUID accountId, String firstName, String lastName,
            Gender gender, LocalDate dateOfBirth) {
        return Customer.builder()
                .id(id)
                .accountId(accountId)
                .firstName(firstName)
                .lastName(lastName)
                .gender(gender)
                .dateOfBirth(dateOfBirth)
                .displayLang("en")
                .isAnonymous(false)
                .build();
    }

    private List<Favorite> createFavorites() {
        List<Favorite> favorites = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // Create favorites mapping customers to movies
        // Customer 1 favorites: Movie 1, 2, 3
        favorites.add(createFavorite(SeedUUIDs.Customers.CUSTOMER_01, SeedUUIDs.Movies.MOVIE_01, now));
        favorites.add(createFavorite(SeedUUIDs.Customers.CUSTOMER_01, SeedUUIDs.Movies.MOVIE_02, now));
        favorites.add(createFavorite(SeedUUIDs.Customers.CUSTOMER_01, SeedUUIDs.Movies.MOVIE_03, now));

        // Customer 2 favorites: Movie 2, 4, 5, 6
        favorites.add(createFavorite(SeedUUIDs.Customers.CUSTOMER_02, SeedUUIDs.Movies.MOVIE_02, now));
        favorites.add(createFavorite(SeedUUIDs.Customers.CUSTOMER_02, SeedUUIDs.Movies.MOVIE_04, now));
        favorites.add(createFavorite(SeedUUIDs.Customers.CUSTOMER_02, SeedUUIDs.Movies.MOVIE_05, now));
        favorites.add(createFavorite(SeedUUIDs.Customers.CUSTOMER_02, SeedUUIDs.Movies.MOVIE_06, now));

        // Customer 3 favorites: Movie 1, 7
        favorites.add(createFavorite(SeedUUIDs.Customers.CUSTOMER_03, SeedUUIDs.Movies.MOVIE_01, now));
        favorites.add(createFavorite(SeedUUIDs.Customers.CUSTOMER_03, SeedUUIDs.Movies.MOVIE_07, now));

        // Customer 4 favorites: Movie 8, 9, 10
        favorites.add(createFavorite(SeedUUIDs.Customers.CUSTOMER_04, SeedUUIDs.Movies.MOVIE_08, now));
        favorites.add(createFavorite(SeedUUIDs.Customers.CUSTOMER_04, SeedUUIDs.Movies.MOVIE_09, now));
        favorites.add(createFavorite(SeedUUIDs.Customers.CUSTOMER_04, SeedUUIDs.Movies.MOVIE_10, now));

        // Customer 5 favorites: Movie 3, 11, 12
        favorites.add(createFavorite(SeedUUIDs.Customers.CUSTOMER_05, SeedUUIDs.Movies.MOVIE_03, now));
        favorites.add(createFavorite(SeedUUIDs.Customers.CUSTOMER_05, SeedUUIDs.Movies.MOVIE_11, now));
        favorites.add(createFavorite(SeedUUIDs.Customers.CUSTOMER_05, SeedUUIDs.Movies.MOVIE_12, now));

        return favorites;
    }

    private Favorite createFavorite(UUID customerId, UUID movieId, LocalDateTime createdAt) {
        return Favorite.builder()
                .customerId(customerId)
                .movieId(movieId)
                .createdAt(createdAt)
                .build();
    }
}
