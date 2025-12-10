package com.pbl6.cinemate.movie.runner;

import com.pbl6.cinemate.movie.entity.*;
import com.pbl6.cinemate.movie.enums.MovieProcessStatus;
import com.pbl6.cinemate.movie.enums.MovieStatus;
import com.pbl6.cinemate.movie.repository.*;
import com.pbl6.cinemate.shared.constants.SeedUUIDs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Seeds initial movie data for development/testing purposes.
 * Only runs when app.seed.enabled=true and the movies table is empty.
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class DataSeederRunner implements ApplicationRunner {

    private final CategoryRepository categoryRepository;
    private final ActorRepository actorRepository;
    private final DirectorRepository directorRepository;
    private final MovieRepository movieRepository;
    private final ReviewRepository reviewRepository;
    private final WatchHistoryRepository watchHistoryRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedCategories();
        seedActors();
        seedDirectors();
        seedMovies();
        seedReviews();
        seedWatchHistory();
    }

    private void seedCategories() {
        if (categoryRepository.count() > 0) {
            log.info("Categories table is not empty. Skipping category seeding.");
            return;
        }

        log.info("Starting category data seeding...");

        List<Category> categories = Arrays.asList(
                createCategory(SeedUUIDs.Categories.ACTION, "Action", "Action movies with exciting sequences"),
                createCategory(SeedUUIDs.Categories.COMEDY, "Comedy", "Funny and entertaining movies"),
                createCategory(SeedUUIDs.Categories.DRAMA, "Drama", "Dramatic storytelling and character development"),
                createCategory(SeedUUIDs.Categories.HORROR, "Horror", "Scary and thrilling horror movies"),
                createCategory(SeedUUIDs.Categories.SCI_FI, "Sci-Fi", "Science fiction and futuristic themes"),
                createCategory(SeedUUIDs.Categories.ROMANCE, "Romance", "Love stories and romantic plots"),
                createCategory(SeedUUIDs.Categories.THRILLER, "Thriller", "Suspenseful and exciting thrillers"),
                createCategory(SeedUUIDs.Categories.ANIMATION, "Animation", "Animated movies for all ages"),
                createCategory(SeedUUIDs.Categories.DOCUMENTARY, "Documentary",
                        "Real-life stories and educational content"),
                createCategory(SeedUUIDs.Categories.FANTASY, "Fantasy", "Magical and fantastical worlds"),
                createCategory(SeedUUIDs.Categories.ADVENTURE, "Adventure", "Exciting journeys and explorations"),
                createCategory(SeedUUIDs.Categories.MYSTERY, "Mystery", "Puzzling plots and detective stories"));

        categoryRepository.saveAll(categories);
        log.info("Successfully seeded {} categories", categories.size());
    }

    private Category createCategory(UUID id, String name, String description) {
        return Category.builder()
                .id(id)
                .name(name)
                .description(description)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private void seedActors() {
        if (actorRepository.count() > 0) {
            log.info("Actors table is not empty. Skipping actor seeding.");
            return;
        }

        log.info("Starting actor data seeding...");

        Instant now = Instant.now();
        List<Actor> actors = Arrays.asList(
                createActor(SeedUUIDs.Actors.ACTOR_01, "Leonardo DiCaprio",
                        "American actor known for Titanic, Inception", LocalDate.of(1974, 11, 11), now),
                createActor(SeedUUIDs.Actors.ACTOR_02, "Tom Hanks", "American actor known for Forrest Gump, Cast Away",
                        LocalDate.of(1956, 7, 9), now),
                createActor(SeedUUIDs.Actors.ACTOR_03, "Scarlett Johansson",
                        "American actress known for Black Widow, Lost in Translation", LocalDate.of(1984, 11, 22), now),
                createActor(SeedUUIDs.Actors.ACTOR_04, "Brad Pitt", "American actor known for Fight Club, Troy",
                        LocalDate.of(1963, 12, 18), now),
                createActor(SeedUUIDs.Actors.ACTOR_05, "Natalie Portman",
                        "Israeli-American actress known for Black Swan, Thor", LocalDate.of(1981, 6, 9), now),
                createActor(SeedUUIDs.Actors.ACTOR_06, "Robert Downey Jr.",
                        "American actor known for Iron Man, Sherlock Holmes", LocalDate.of(1965, 4, 4), now),
                createActor(SeedUUIDs.Actors.ACTOR_07, "Margot Robbie",
                        "Australian actress known for Barbie, Wolf of Wall Street", LocalDate.of(1990, 7, 2), now),
                createActor(SeedUUIDs.Actors.ACTOR_08, "Christian Bale",
                        "British actor known for The Dark Knight, American Psycho", LocalDate.of(1974, 1, 30), now),
                createActor(SeedUUIDs.Actors.ACTOR_09, "Emma Stone", "American actress known for La La Land, Easy A",
                        LocalDate.of(1988, 11, 6), now),
                createActor(SeedUUIDs.Actors.ACTOR_10, "Matthew McConaughey",
                        "American actor known for Interstellar, Dallas Buyers Club", LocalDate.of(1969, 11, 4), now));

        actorRepository.saveAll(actors);
        log.info("Successfully seeded {} actors", actors.size());
    }

    private Actor createActor(UUID id, String fullname, String biography, LocalDate dob, Instant now) {
        return Actor.builder()
                .id(id)
                .fullname(fullname)
                .biography(biography)
                .dateOfBirth(dob)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private void seedDirectors() {
        if (directorRepository.count() > 0) {
            log.info("Directors table is not empty. Skipping director seeding.");
            return;
        }

        log.info("Starting director data seeding...");

        Instant now = Instant.now();
        List<Director> directors = Arrays.asList(
                createDirector(SeedUUIDs.Directors.DIRECTOR_01, "Christopher Nolan",
                        "British-American director known for Inception, Interstellar", LocalDate.of(1970, 7, 30), now),
                createDirector(SeedUUIDs.Directors.DIRECTOR_02, "Steven Spielberg",
                        "American director known for Jurassic Park, Schindler's List", LocalDate.of(1946, 12, 18), now),
                createDirector(SeedUUIDs.Directors.DIRECTOR_03, "Martin Scorsese",
                        "American director known for Goodfellas, The Departed", LocalDate.of(1942, 11, 17), now),
                createDirector(SeedUUIDs.Directors.DIRECTOR_04, "Quentin Tarantino",
                        "American director known for Pulp Fiction, Kill Bill", LocalDate.of(1963, 3, 27), now),
                createDirector(SeedUUIDs.Directors.DIRECTOR_05, "Denis Villeneuve",
                        "Canadian director known for Dune, Blade Runner 2049", LocalDate.of(1967, 10, 3), now),
                createDirector(SeedUUIDs.Directors.DIRECTOR_06, "Hayao Miyazaki",
                        "Japanese director known for Spirited Away, My Neighbor Totoro", LocalDate.of(1941, 1, 5), now),
                createDirector(SeedUUIDs.Directors.DIRECTOR_07, "Bong Joon-ho",
                        "South Korean director known for Parasite, Memories of Murder", LocalDate.of(1969, 9, 14), now),
                createDirector(SeedUUIDs.Directors.DIRECTOR_08, "Greta Gerwig",
                        "American director known for Lady Bird, Barbie", LocalDate.of(1983, 8, 4), now));

        directorRepository.saveAll(directors);
        log.info("Successfully seeded {} directors", directors.size());
    }

    private Director createDirector(UUID id, String fullname, String biography, LocalDate dob, Instant now) {
        return Director.builder()
                .id(id)
                .fullname(fullname)
                .biography(biography)
                .dateOfBirth(dob)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private void seedMovies() {
        if (movieRepository.count() > 0) {
            log.info("Movies table is not empty. Skipping movie seeding.");
            return;
        }

        log.info("Starting movie data seeding...");

        // Fetch all categories, actors, directors
        Map<UUID, Category> categories = new HashMap<>();
        categoryRepository.findAll().forEach(c -> categories.put(c.getId(), c));

        Map<UUID, Actor> actors = new HashMap<>();
        actorRepository.findAll().forEach(a -> actors.put(a.getId(), a));

        Map<UUID, Director> directors = new HashMap<>();
        directorRepository.findAll().forEach(d -> directors.put(d.getId(), d));

        Instant now = Instant.now();

        List<Movie> movies = new ArrayList<>();

        // PUBLIC Movies (12)
        movies.add(createMovie(SeedUUIDs.Movies.MOVIE_01, "Inception",
                "A skilled thief is offered a chance to erase his past crimes by planting an idea into someone's subconscious.",
                MovieStatus.PUBLIC, MovieProcessStatus.COMPLETED, false, 13, 2010, "USA",
                LocalDate.of(2010, 7, 16), now, 1,
                List.of(categories.get(SeedUUIDs.Categories.ACTION), categories.get(SeedUUIDs.Categories.SCI_FI),
                        categories.get(SeedUUIDs.Categories.THRILLER)),
                List.of(actors.get(SeedUUIDs.Actors.ACTOR_01)),
                List.of(directors.get(SeedUUIDs.Directors.DIRECTOR_01))));

        movies.add(createMovie(SeedUUIDs.Movies.MOVIE_02, "Interstellar",
                "A team of explorers travel through a wormhole to ensure humanity's survival.",
                MovieStatus.PUBLIC, MovieProcessStatus.COMPLETED, true, 13, 2014, "USA",
                LocalDate.of(2014, 11, 7), now, 2,
                List.of(categories.get(SeedUUIDs.Categories.SCI_FI), categories.get(SeedUUIDs.Categories.DRAMA),
                        categories.get(SeedUUIDs.Categories.ADVENTURE)),
                List.of(actors.get(SeedUUIDs.Actors.ACTOR_10)),
                List.of(directors.get(SeedUUIDs.Directors.DIRECTOR_01))));

        movies.add(createMovie(SeedUUIDs.Movies.MOVIE_03, "The Dark Knight",
                "Batman faces his nemesis, the Joker, who seeks to create chaos in Gotham.",
                MovieStatus.PUBLIC, MovieProcessStatus.COMPLETED, true, 13, 2008, "USA",
                LocalDate.of(2008, 7, 18), now, 3,
                List.of(categories.get(SeedUUIDs.Categories.ACTION), categories.get(SeedUUIDs.Categories.DRAMA),
                        categories.get(SeedUUIDs.Categories.THRILLER)),
                List.of(actors.get(SeedUUIDs.Actors.ACTOR_08)),
                List.of(directors.get(SeedUUIDs.Directors.DIRECTOR_01))));

        movies.add(createMovie(SeedUUIDs.Movies.MOVIE_04, "Parasite",
                "A poor family schemes to become employed by a wealthy family by infiltrating their household.",
                MovieStatus.PUBLIC, MovieProcessStatus.COMPLETED, false, 16, 2019, "South Korea",
                LocalDate.of(2019, 5, 30), now, 4,
                List.of(categories.get(SeedUUIDs.Categories.DRAMA), categories.get(SeedUUIDs.Categories.THRILLER),
                        categories.get(SeedUUIDs.Categories.COMEDY)),
                List.of(),
                List.of(directors.get(SeedUUIDs.Directors.DIRECTOR_07))));

        movies.add(createMovie(SeedUUIDs.Movies.MOVIE_05, "Spirited Away",
                "A young girl becomes trapped in a mysterious spirit world after her parents are transformed into pigs.",
                MovieStatus.PUBLIC, MovieProcessStatus.COMPLETED, false, 7, 2001, "Japan",
                LocalDate.of(2001, 7, 20), now, 5,
                List.of(categories.get(SeedUUIDs.Categories.ANIMATION), categories.get(SeedUUIDs.Categories.FANTASY),
                        categories.get(SeedUUIDs.Categories.ADVENTURE)),
                List.of(),
                List.of(directors.get(SeedUUIDs.Directors.DIRECTOR_06))));

        movies.add(createMovie(SeedUUIDs.Movies.MOVIE_06, "Pulp Fiction",
                "The lives of two mob hitmen, a boxer, a gangster and his wife intertwine in four tales of violence.",
                MovieStatus.PUBLIC, MovieProcessStatus.COMPLETED, true, 18, 1994, "USA",
                LocalDate.of(1994, 10, 14), now, 6,
                List.of(categories.get(SeedUUIDs.Categories.DRAMA), categories.get(SeedUUIDs.Categories.THRILLER)),
                List.of(actors.get(SeedUUIDs.Actors.ACTOR_02)),
                List.of(directors.get(SeedUUIDs.Directors.DIRECTOR_04))));

        movies.add(createMovie(SeedUUIDs.Movies.MOVIE_07, "The Wolf of Wall Street",
                "Based on the true story of Jordan Belfort, from his rise to a wealthy stock-broker to his fall.",
                MovieStatus.PUBLIC, MovieProcessStatus.COMPLETED, true, 18, 2013, "USA",
                LocalDate.of(2013, 12, 25), now, 7,
                List.of(categories.get(SeedUUIDs.Categories.DRAMA), categories.get(SeedUUIDs.Categories.COMEDY)),
                List.of(actors.get(SeedUUIDs.Actors.ACTOR_01), actors.get(SeedUUIDs.Actors.ACTOR_07)),
                List.of(directors.get(SeedUUIDs.Directors.DIRECTOR_03))));

        movies.add(createMovie(SeedUUIDs.Movies.MOVIE_08, "Dune: Part Two",
                "Paul Atreides unites with the Fremen to exact revenge against the conspirators who destroyed his family.",
                MovieStatus.PUBLIC, MovieProcessStatus.COMPLETED, true, 13, 2024, "USA",
                LocalDate.of(2024, 3, 1), now, 8,
                List.of(categories.get(SeedUUIDs.Categories.SCI_FI), categories.get(SeedUUIDs.Categories.ACTION),
                        categories.get(SeedUUIDs.Categories.ADVENTURE)),
                List.of(),
                List.of(directors.get(SeedUUIDs.Directors.DIRECTOR_05))));

        movies.add(createMovie(SeedUUIDs.Movies.MOVIE_09, "Barbie",
                "Barbie suffers a crisis that leads her to question her world and her existence.",
                MovieStatus.PUBLIC, MovieProcessStatus.COMPLETED, false, 13, 2023, "USA",
                LocalDate.of(2023, 7, 21), now, 9,
                List.of(categories.get(SeedUUIDs.Categories.COMEDY), categories.get(SeedUUIDs.Categories.FANTASY),
                        categories.get(SeedUUIDs.Categories.ADVENTURE)),
                List.of(actors.get(SeedUUIDs.Actors.ACTOR_07)),
                List.of(directors.get(SeedUUIDs.Directors.DIRECTOR_08))));

        movies.add(createMovie(SeedUUIDs.Movies.MOVIE_10, "Jurassic Park",
                "A paleontologist visits an almost complete theme park on an island in Central America.",
                MovieStatus.PUBLIC, MovieProcessStatus.COMPLETED, false, 13, 1993, "USA",
                LocalDate.of(1993, 6, 11), now, 10,
                List.of(categories.get(SeedUUIDs.Categories.SCI_FI), categories.get(SeedUUIDs.Categories.ACTION),
                        categories.get(SeedUUIDs.Categories.ADVENTURE)),
                List.of(),
                List.of(directors.get(SeedUUIDs.Directors.DIRECTOR_02))));

        movies.add(createMovie(SeedUUIDs.Movies.MOVIE_11, "Black Swan",
                "A committed dancer struggles to maintain her sanity after winning the lead role in a production of Swan Lake.",
                MovieStatus.PUBLIC, MovieProcessStatus.COMPLETED, true, 16, 2010, "USA",
                LocalDate.of(2010, 12, 17), now, 11,
                List.of(categories.get(SeedUUIDs.Categories.DRAMA), categories.get(SeedUUIDs.Categories.THRILLER),
                        categories.get(SeedUUIDs.Categories.MYSTERY)),
                List.of(actors.get(SeedUUIDs.Actors.ACTOR_05)),
                List.of()));

        movies.add(createMovie(SeedUUIDs.Movies.MOVIE_12, "Iron Man",
                "After being held captive, billionaire engineer Tony Stark creates a unique weaponized suit of armor.",
                MovieStatus.PUBLIC, MovieProcessStatus.COMPLETED, false, 13, 2008, "USA",
                LocalDate.of(2008, 5, 2), now, 12,
                List.of(categories.get(SeedUUIDs.Categories.ACTION), categories.get(SeedUUIDs.Categories.SCI_FI),
                        categories.get(SeedUUIDs.Categories.ADVENTURE)),
                List.of(actors.get(SeedUUIDs.Actors.ACTOR_06), actors.get(SeedUUIDs.Actors.ACTOR_03)),
                List.of()));

        // PRIVATE Movies (3)
        movies.add(createMovie(SeedUUIDs.Movies.MOVIE_13, "Upcoming Thriller",
                "A suspenseful thriller coming soon to the platform.",
                MovieStatus.PRIVATE, MovieProcessStatus.COMPLETED, true, 16, 2025, "USA",
                LocalDate.of(2025, 6, 1), now, null,
                List.of(categories.get(SeedUUIDs.Categories.THRILLER), categories.get(SeedUUIDs.Categories.MYSTERY)),
                List.of(actors.get(SeedUUIDs.Actors.ACTOR_04)),
                List.of(directors.get(SeedUUIDs.Directors.DIRECTOR_01))));

        movies.add(createMovie(SeedUUIDs.Movies.MOVIE_14, "Secret Documentary",
                "An exclusive documentary not yet released to the public.",
                MovieStatus.PRIVATE, MovieProcessStatus.COMPLETED, false, 13, 2025, "USA",
                LocalDate.of(2025, 3, 15), now, null,
                List.of(categories.get(SeedUUIDs.Categories.DOCUMENTARY)),
                List.of(),
                List.of(directors.get(SeedUUIDs.Directors.DIRECTOR_02))));

        movies.add(createMovie(SeedUUIDs.Movies.MOVIE_15, "Animation Preview",
                "A preview of an upcoming animated feature.",
                MovieStatus.PRIVATE, MovieProcessStatus.COMPLETED, false, 7, 2025, "Japan",
                LocalDate.of(2025, 8, 1), now, null,
                List.of(categories.get(SeedUUIDs.Categories.ANIMATION), categories.get(SeedUUIDs.Categories.FANTASY)),
                List.of(),
                List.of(directors.get(SeedUUIDs.Directors.DIRECTOR_06))));

        movieRepository.saveAll(movies);
        log.info("Successfully seeded {} movies", movies.size());
    }

    private Movie createMovie(UUID id, String title, String description, MovieStatus status,
            MovieProcessStatus processStatus, boolean isVip, int age, int year,
            String country, LocalDate releaseDate, Instant now, Integer rank,
            List<Category> movieCategories, List<Actor> movieActors, List<Director> movieDirectors) {
        Movie movie = Movie.builder()
                .id(id)
                .title(title)
                .description(description)
                .status(status)
                .processStatus(processStatus)
                .isVip(isVip)
                .age(age)
                .year(year)
                .country(country)
                .releaseDate(releaseDate)
                .qualities(Arrays.asList("720p", "1080p", "4K"))
                .rank(rank)
                .createdAt(now)
                .updatedAt(now)
                .build();

        // Add categories
        for (Category category : movieCategories) {
            MovieCategory mc = new MovieCategory(movie, category);
            movie.getMovieCategories().add(mc);
        }

        // Add actors
        for (Actor actor : movieActors) {
            MovieActor ma = new MovieActor(movie, actor);
            movie.getMovieActors().add(ma);
        }

        // Add directors
        for (Director director : movieDirectors) {
            MovieDirector md = new MovieDirector(movie, director);
            movie.getMovieDirectors().add(md);
        }

        return movie;
    }

    private void seedReviews() {
        if (reviewRepository.count() > 0) {
            log.info("Reviews table is not empty. Skipping review seeding.");
            return;
        }

        log.info("Starting review data seeding...");

        List<Movie> movies = movieRepository.findAll();
        if (movies.isEmpty()) {
            log.warn("No movies found. Skipping review seeding.");
            return;
        }

        List<Review> reviews = new ArrayList<>();
        Instant now = Instant.now();

        // Create 15 reviews spread across movies
        UUID[] customerIds = {
                SeedUUIDs.Customers.CUSTOMER_01, SeedUUIDs.Customers.CUSTOMER_02, SeedUUIDs.Customers.CUSTOMER_03,
                SeedUUIDs.Customers.CUSTOMER_04, SeedUUIDs.Customers.CUSTOMER_05, SeedUUIDs.Customers.CUSTOMER_06,
                SeedUUIDs.Customers.CUSTOMER_07, SeedUUIDs.Customers.CUSTOMER_08, SeedUUIDs.Customers.CUSTOMER_09
        };

        String[] reviewContents = {
                "Amazing movie! Highly recommended.",
                "Great storyline and excellent acting.",
                "A masterpiece of cinema.",
                "Good movie but a bit slow in the middle.",
                "Visually stunning and emotionally powerful.",
                "One of the best movies I've ever seen!",
                "Interesting concept, well executed.",
                "The performances were outstanding.",
                "A bit overrated but still enjoyable.",
                "Brilliant direction and cinematography.",
                "Would watch again!",
                "Perfect for a movie night.",
                "The ending was unexpected and satisfying.",
                "A true cinematic experience.",
                "Loved every minute of it!"
        };

        String[] customerNames = { "John Doe", "Jane Smith", "Robert Johnson", "Emily Williams",
                "Michael Brown", "Sarah Davis", "David Miller", "Lisa Wilson", "James Moore" };

        int reviewIndex = 0;
        for (int i = 0; i < Math.min(movies.size(), 12) && reviewIndex < 15; i++) {
            Movie movie = movies.get(i);
            if (movie.getStatus() == MovieStatus.PUBLIC) {
                int customerIndex = reviewIndex % customerIds.length;
                reviews.add(Review.builder()
                        .movie(movie)
                        .customerId(customerIds[customerIndex])
                        .content(reviewContents[reviewIndex % reviewContents.length])
                        .stars((reviewIndex % 5) + 1) // Stars from 1 to 5
                        .userName(customerNames[customerIndex])
                        .createdAt(now)
                        .updatedAt(now)
                        .build());
                reviewIndex++;
            }
        }

        reviewRepository.saveAll(reviews);
        log.info("Successfully seeded {} reviews", reviews.size());
    }

    private void seedWatchHistory() {
        if (watchHistoryRepository.count() > 0) {
            log.info("WatchHistory table is not empty. Skipping watch history seeding.");
            return;
        }

        log.info("Starting watch history data seeding...");

        List<Movie> movies = movieRepository.findAll();
        if (movies.isEmpty()) {
            log.warn("No movies found. Skipping watch history seeding.");
            return;
        }

        List<WatchHistory> watchHistories = new ArrayList<>();

        UUID[] customerIds = {
                SeedUUIDs.Customers.CUSTOMER_01, SeedUUIDs.Customers.CUSTOMER_02,
                SeedUUIDs.Customers.CUSTOMER_03, SeedUUIDs.Customers.CUSTOMER_04,
                SeedUUIDs.Customers.CUSTOMER_05
        };

        int historyIndex = 0;
        for (int i = 0; i < Math.min(movies.size(), 10) && historyIndex < 10; i++) {
            Movie movie = movies.get(i);
            if (movie.getStatus() == MovieStatus.PUBLIC) {
                UUID customerId = customerIds[historyIndex % customerIds.length];
                long totalDuration = 7200L; // 2 hours
                long watchedPosition = (historyIndex % 3 == 0) ? totalDuration
                        : (long) (totalDuration * (0.3 + (historyIndex * 0.1)));

                watchHistories.add(WatchHistory.builder()
                        .movie(movie)
                        .customerId(customerId)
                        .lastWatchedPosition(Math.min(watchedPosition, totalDuration))
                        .totalDuration(totalDuration)
                        .progressPercent(Math.min(100.0, (watchedPosition * 100.0) / totalDuration))
                        .build());
                historyIndex++;
            }
        }

        watchHistoryRepository.saveAll(watchHistories);
        log.info("Successfully seeded {} watch histories", watchHistories.size());
    }
}
