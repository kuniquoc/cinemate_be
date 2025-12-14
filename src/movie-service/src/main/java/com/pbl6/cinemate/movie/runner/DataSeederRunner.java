package com.pbl6.cinemate.movie.runner;

import com.pbl6.cinemate.movie.entity.*;
import com.pbl6.cinemate.movie.enums.MovieProcessStatus;
import com.pbl6.cinemate.movie.enums.MovieStatus;
import com.pbl6.cinemate.movie.repository.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

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

        // Data definitions
        private static final List<CategoryData> CATEGORY_DATA = Arrays.asList(
                        new CategoryData("Action", "Action movies with exciting sequences"),
                        new CategoryData("Comedy", "Funny and entertaining movies"),
                        new CategoryData("Drama", "Dramatic storytelling and character development"),
                        new CategoryData("Horror", "Scary and thrilling horror movies"),
                        new CategoryData("Sci-Fi", "Science fiction and futuristic themes"),
                        new CategoryData("Romance", "Love stories and romantic plots"),
                        new CategoryData("Thriller", "Suspenseful and exciting thrillers"),
                        new CategoryData("Animation", "Animated movies for all ages"),
                        new CategoryData("Documentary", "Real-life stories and educational content"),
                        new CategoryData("Fantasy", "Magical and fantastical worlds"),
                        new CategoryData("Adventure", "Exciting journeys and explorations"),
                        new CategoryData("Mystery", "Puzzling plots and detective stories"));

        private static final List<ActorData> ACTOR_DATA = Arrays.asList(
                        new ActorData("Leonardo DiCaprio", "American actor known for Titanic, Inception",
                                        LocalDate.of(1974, 11, 11)),
                        new ActorData("Tom Hanks", "American actor known for Forrest Gump, Cast Away",
                                        LocalDate.of(1956, 7, 9)),
                        new ActorData("Scarlett Johansson",
                                        "American actress known for Black Widow, Lost in Translation",
                                        LocalDate.of(1984, 11, 22)),
                        new ActorData("Brad Pitt", "American actor known for Fight Club, Troy",
                                        LocalDate.of(1963, 12, 18)),
                        new ActorData("Natalie Portman", "Israeli-American actress known for Black Swan, Thor",
                                        LocalDate.of(1981, 6, 9)),
                        new ActorData("Robert Downey Jr.", "American actor known for Iron Man, Sherlock Holmes",
                                        LocalDate.of(1965, 4, 4)),
                        new ActorData("Margot Robbie", "Australian actress known for Barbie, Wolf of Wall Street",
                                        LocalDate.of(1990, 7, 2)),
                        new ActorData("Christian Bale", "British actor known for The Dark Knight, American Psycho",
                                        LocalDate.of(1974, 1, 30)),
                        new ActorData("Emma Stone", "American actress known for La La Land, Easy A",
                                        LocalDate.of(1988, 11, 6)),
                        new ActorData("Matthew McConaughey",
                                        "American actor known for Interstellar, Dallas Buyers Club",
                                        LocalDate.of(1969, 11, 4)));

        private static final List<DirectorData> DIRECTOR_DATA = Arrays.asList(
                        new DirectorData("Christopher Nolan",
                                        "British-American director known for Inception, Interstellar",
                                        LocalDate.of(1970, 7, 30)),
                        new DirectorData("Steven Spielberg",
                                        "American director known for Jurassic Park, Schindler's List",
                                        LocalDate.of(1946, 12, 18)),
                        new DirectorData("Martin Scorsese", "American director known for Goodfellas, The Departed",
                                        LocalDate.of(1942, 11, 17)),
                        new DirectorData("Quentin Tarantino", "American director known for Pulp Fiction, Kill Bill",
                                        LocalDate.of(1963, 3, 27)),
                        new DirectorData("Denis Villeneuve", "Canadian director known for Dune, Blade Runner 2049",
                                        LocalDate.of(1967, 10, 3)),
                        new DirectorData("Hayao Miyazaki",
                                        "Japanese director known for Spirited Away, My Neighbor Totoro",
                                        LocalDate.of(1941, 1, 5)),
                        new DirectorData("Bong Joon-ho", "South Korean director known for Parasite, Memories of Murder",
                                        LocalDate.of(1969, 9, 14)),
                        new DirectorData("Greta Gerwig", "American director known for Lady Bird, Barbie",
                                        LocalDate.of(1983, 8, 4)));

        private static final List<MovieData> MOVIE_DATA = Arrays.asList(
                        new MovieData("Inception",
                                        "A skilled thief is offered a chance to erase his past crimes by planting an idea into someone's subconscious.",
                                        MovieStatus.DRAFT, MovieProcessStatus.UPLOADING, false, 13, 2010, "USA",
                                        LocalDate.of(2010, 7, 16), 1, Arrays.asList("Action", "Sci-Fi", "Thriller"),
                                        Arrays.asList("Leonardo DiCaprio"), Arrays.asList("Christopher Nolan")),
                        new MovieData("Interstellar",
                                        "A team of explorers travel through a wormhole to ensure humanity's survival.",
                                        MovieStatus.DRAFT, MovieProcessStatus.UPLOADING, true, 13, 2014, "USA",
                                        LocalDate.of(2014, 11, 7), 2, Arrays.asList("Sci-Fi", "Drama", "Adventure"),
                                        Arrays.asList("Matthew McConaughey"), Arrays.asList("Christopher Nolan")),
                        new MovieData("The Dark Knight",
                                        "Batman faces his nemesis, the Joker, who seeks to create chaos in Gotham.",
                                        MovieStatus.DRAFT, MovieProcessStatus.UPLOADING, true, 13, 2008, "USA",
                                        LocalDate.of(2008, 7, 18), 3, Arrays.asList("Action", "Drama", "Thriller"),
                                        Arrays.asList("Christian Bale"), Arrays.asList("Christopher Nolan")),
                        new MovieData("Parasite",
                                        "A poor family schemes to become employed by a wealthy family by infiltrating their household.",
                                        MovieStatus.DRAFT, MovieProcessStatus.UPLOADING, false, 16, 2019, "South Korea",
                                        LocalDate.of(2019, 5, 30), 4, Arrays.asList("Drama", "Thriller", "Comedy"),
                                        Arrays.asList(), Arrays.asList("Bong Joon-ho")),
                        new MovieData("Spirited Away",
                                        "A young girl becomes trapped in a mysterious spirit world after her parents are transformed into pigs.",
                                        MovieStatus.DRAFT, MovieProcessStatus.UPLOADING, false, 7, 2001, "Japan",
                                        LocalDate.of(2001, 7, 20), 5,
                                        Arrays.asList("Animation", "Fantasy", "Adventure"), Arrays.asList(),
                                        Arrays.asList("Hayao Miyazaki")),
                        new MovieData("Pulp Fiction",
                                        "The lives of two mob hitmen, a boxer, a gangster and his wife intertwine in four tales of violence.",
                                        MovieStatus.DRAFT, MovieProcessStatus.UPLOADING, true, 18, 1994, "USA",
                                        LocalDate.of(1994, 10, 14), 6, Arrays.asList("Drama", "Thriller"),
                                        Arrays.asList("Tom Hanks"), Arrays.asList("Quentin Tarantino")),
                        new MovieData("The Wolf of Wall Street",
                                        "Based on the true story of Jordan Belfort, from his rise to a wealthy stock-broker to his fall.",
                                        MovieStatus.DRAFT, MovieProcessStatus.UPLOADING, true, 18, 2013, "USA",
                                        LocalDate.of(2013, 12, 25), 7, Arrays.asList("Drama", "Comedy"),
                                        Arrays.asList("Leonardo DiCaprio", "Margot Robbie"),
                                        Arrays.asList("Martin Scorsese")),
                        new MovieData("Dune: Part Two",
                                        "Paul Atreides unites with the Fremen to exact revenge against the conspirators who destroyed his family.",
                                        MovieStatus.DRAFT, MovieProcessStatus.UPLOADING, true, 13, 2024, "USA",
                                        LocalDate.of(2024, 3, 1), 8, Arrays.asList("Sci-Fi", "Action", "Adventure"),
                                        Arrays.asList(), Arrays.asList("Denis Villeneuve")),
                        new MovieData("Barbie",
                                        "Barbie suffers a crisis that leads her to question her world and her existence.",
                                        MovieStatus.DRAFT, MovieProcessStatus.UPLOADING, false, 13, 2023, "USA",
                                        LocalDate.of(2023, 7, 21), 9, Arrays.asList("Comedy", "Fantasy", "Adventure"),
                                        Arrays.asList("Margot Robbie"), Arrays.asList("Greta Gerwig")),
                        new MovieData("Jurassic Park",
                                        "A paleontologist visits an almost complete theme park on an island in Central America.",
                                        MovieStatus.DRAFT, MovieProcessStatus.UPLOADING, false, 13, 1993, "USA",
                                        LocalDate.of(1993, 6, 11), 10, Arrays.asList("Sci-Fi", "Action", "Adventure"),
                                        Arrays.asList(), Arrays.asList("Steven Spielberg")),
                        new MovieData("Black Swan",
                                        "A committed dancer struggles to maintain her sanity after winning the lead role in a production of Swan Lake.",
                                        MovieStatus.DRAFT, MovieProcessStatus.UPLOADING, true, 16, 2010, "USA",
                                        LocalDate.of(2010, 12, 17), 11, Arrays.asList("Drama", "Thriller", "Mystery"),
                                        Arrays.asList("Natalie Portman"), Arrays.asList()),
                        new MovieData("Iron Man",
                                        "After being held captive, billionaire engineer Tony Stark creates a unique weaponized suit of armor.",
                                        MovieStatus.DRAFT, MovieProcessStatus.UPLOADING, false, 13, 2008, "USA",
                                        LocalDate.of(2008, 5, 2), 12, Arrays.asList("Action", "Sci-Fi", "Adventure"),
                                        Arrays.asList("Robert Downey Jr.", "Scarlett Johansson"), Arrays.asList()),
                        new MovieData("Upcoming Thriller", "A suspenseful thriller coming soon to the platform.",
                                        MovieStatus.PRIVATE, MovieProcessStatus.COMPLETED, true, 16, 2025, "USA",
                                        LocalDate.of(2025, 6, 1), null, Arrays.asList("Thriller", "Mystery"),
                                        Arrays.asList("Brad Pitt"), Arrays.asList("Christopher Nolan")),
                        new MovieData("Secret Documentary", "An exclusive documentary not yet released to the public.",
                                        MovieStatus.PRIVATE, MovieProcessStatus.COMPLETED, false, 13, 2025, "USA",
                                        LocalDate.of(2025, 3, 15), null, Arrays.asList("Documentary"), Arrays.asList(),
                                        Arrays.asList("Steven Spielberg")),
                        new MovieData("Animation Preview", "A preview of an upcoming animated feature.",
                                        MovieStatus.PRIVATE, MovieProcessStatus.COMPLETED, false, 7, 2025, "Japan",
                                        LocalDate.of(2025, 8, 1), null, Arrays.asList("Animation", "Fantasy"),
                                        Arrays.asList(), Arrays.asList("Hayao Miyazaki")));

        // Data classes
        private record CategoryData(String name, String description) {
        }

        private record ActorData(String fullname, String biography, LocalDate dateOfBirth) {
        }

        private record DirectorData(String fullname, String biography, LocalDate dateOfBirth) {
        }

        private record MovieData(String title, String description, MovieStatus status, MovieProcessStatus processStatus,
                        boolean isVip, int age, int year, String country, LocalDate releaseDate, Integer rank,
                        List<String> categoryNames, List<String> actorNames, List<String> directorNames) {
        }

        @Override
        @Transactional
        public void run(ApplicationArguments args) {
                seedCategories();
                seedActors();
                seedDirectors();
                seedMovies();
        }

        private void seedCategories() {
                if (categoryRepository.count() > 0) {
                        log.info("Categories table is not empty. Skipping category seeding.");
                        return;
                }

                log.info("Starting category data seeding...");

                List<Category> categories = new ArrayList<>();
                for (CategoryData data : CATEGORY_DATA) {
                        categories.add(createCategory(data.name, data.description));
                }

                categoryRepository.saveAll(categories);
                log.info("Successfully seeded {} categories", categories.size());
        }

        private Category createCategory(String name, String description) {
                return Category.builder()
                                .name(name)
                                .description(description)
                                .createdAt(Instant.now())
                                .updatedAt(Instant.now())
                                .build();
        }

        private void seedActors() {
                if (actorRepository.count() > 0) {
                        log.info("Actors table is not empty. Skipping actor seeding.");
                        return;
                }

                log.info("Starting actor data seeding...");

                Instant now = Instant.now();
                List<Actor> actors = new ArrayList<>();
                for (ActorData data : ACTOR_DATA) {
                        actors.add(createActor(data.fullname, data.biography, data.dateOfBirth, now));
                }

                actorRepository.saveAll(actors);
                log.info("Successfully seeded {} actors", actors.size());
        }

        private Actor createActor(String fullname, String biography, LocalDate dob, Instant now) {
                return Actor.builder()
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
                List<Director> directors = new ArrayList<>();
                for (DirectorData data : DIRECTOR_DATA) {
                        directors.add(createDirector(data.fullname, data.biography, data.dateOfBirth, now));
                }

                directorRepository.saveAll(directors);
                log.info("Successfully seeded {} directors", directors.size());
        }

        private Director createDirector(String fullname, String biography, LocalDate dob, Instant now) {
                return Director.builder()
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

                // Create maps for lookup by name
                Map<String, Category> categoriesMap = categoryRepository.findAll().stream()
                                .collect(Collectors.toMap(Category::getName, c -> c));
                Map<String, Actor> actorsMap = actorRepository.findAll().stream()
                                .collect(Collectors.toMap(Actor::getFullname, a -> a));
                Map<String, Director> directorsMap = directorRepository.findAll().stream()
                                .collect(Collectors.toMap(Director::getFullname, d -> d));

                Instant now = Instant.now();
                List<Movie> movies = new ArrayList<>();
                for (MovieData data : MOVIE_DATA) {
                        List<Category> movieCategories = data.categoryNames.stream()
                                        .map(categoriesMap::get)
                                        .filter(Objects::nonNull)
                                        .collect(Collectors.toList());
                        List<Actor> movieActors = data.actorNames.stream()
                                        .map(actorsMap::get)
                                        .filter(Objects::nonNull)
                                        .collect(Collectors.toList());
                        List<Director> movieDirectors = data.directorNames.stream()
                                        .map(directorsMap::get)
                                        .filter(Objects::nonNull)
                                        .collect(Collectors.toList());

                        movies.add(createMovie(data.title, data.description, data.status, data.processStatus,
                                        data.isVip, data.age, data.year, data.country, data.releaseDate, now, data.rank,
                                        movieCategories, movieActors, movieDirectors));
                }

                movieRepository.saveAll(movies);
                log.info("Successfully seeded {} movies", movies.size());
        }

        private Movie createMovie(String title, String description, MovieStatus status,
                        MovieProcessStatus processStatus, boolean isVip, int age, int year,
                        String country, LocalDate releaseDate, Instant now, Integer rank,
                        List<Category> movieCategories, List<Actor> movieActors, List<Director> movieDirectors) {
                Movie movie = Movie.builder()
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
                        mc.setCreatedAt(now);
                        mc.setUpdatedAt(now);
                        movie.getMovieCategories().add(mc);
                }

                // Add actors
                for (Actor actor : movieActors) {
                        MovieActor ma = new MovieActor(movie, actor);
                        ma.setCreatedAt(now);
                        ma.setUpdatedAt(now);
                        movie.getMovieActors().add(ma);
                }

                // Add directors
                for (Director director : movieDirectors) {
                        MovieDirector md = new MovieDirector(movie, director);
                        md.setCreatedAt(now);
                        md.setUpdatedAt(now);
                        movie.getMovieDirectors().add(md);
                }

                return movie;
        }
}
