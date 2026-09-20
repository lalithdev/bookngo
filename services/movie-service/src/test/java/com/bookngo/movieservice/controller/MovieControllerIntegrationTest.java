package com.bookngo.movieservice.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookngo.movieservice.dto.MovieCreateRequest;
import com.bookngo.movieservice.entity.Movie;
import com.bookngo.movieservice.entity.MovieStatus;
import com.bookngo.movieservice.repository.MovieRepository;
import com.bookngo.movieservice.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MovieControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private JwtService jwtService;

    private Movie activeMovie1;
    private Movie activeMovie2;
    private Movie inactiveMovie;

    private String adminToken;
    private String customerToken;
    private String operatorToken;

    @BeforeEach
    void setUp() {
        movieRepository.deleteAll();

        activeMovie1 = Movie.builder()
                .title("Inception")
                .description("Mind-bending thriller")
                .language("English")
                .genres(List.of("Action", "Sci-Fi"))
                .durationMinutes(148)
                .releaseDate(LocalDate.of(2010, 7, 16))
                .posterReference("https://example.com/inception.jpg")
                .status(MovieStatus.ACTIVE)
                .build();
        activeMovie1 = movieRepository.save(activeMovie1);

        activeMovie2 = Movie.builder()
                .title("Interstellar")
                .description("Space exploration")
                .language("English")
                .genres(List.of("Adventure", "Drama", "Sci-Fi"))
                .durationMinutes(169)
                .releaseDate(LocalDate.of(2014, 11, 7))
                .posterReference("https://example.com/interstellar.jpg")
                .status(MovieStatus.ACTIVE)
                .build();
        activeMovie2 = movieRepository.save(activeMovie2);

        inactiveMovie = Movie.builder()
                .title("Old Archived Movie")
                .description("No longer showing")
                .language("Spanish")
                .genres(List.of("Drama"))
                .durationMinutes(90)
                .releaseDate(LocalDate.of(1995, 1, 1))
                .posterReference("https://example.com/old.jpg")
                .status(MovieStatus.INACTIVE)
                .build();
        inactiveMovie = movieRepository.save(inactiveMovie);

        UUID adminId = UUID.randomUUID();
        adminToken = jwtService.generateToken(adminId, List.of("ADMIN"));

        UUID customerId = UUID.randomUUID();
        customerToken = jwtService.generateToken(customerId, List.of("CUSTOMER"));

        UUID operatorId = UUID.randomUUID();
        operatorToken = jwtService.generateToken(operatorId, List.of("THEATRE_OPERATOR"));
    }

    @Test
    void testListMovies_Public_DefaultFilters_ReturnsOnlyActiveMovies() throws Exception {
        mockMvc.perform(get("/api/v1/movies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.content[1].status").value("ACTIVE"));
    }

    @Test
    void testListMovies_FilterByTitle() throws Exception {
        mockMvc.perform(get("/api/v1/movies")
                .param("title", "Inception"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Inception"));
    }

    @Test
    void testListMovies_FilterByStatus_Inactive() throws Exception {
        mockMvc.perform(get("/api/v1/movies")
                .param("status", "INACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Old Archived Movie"))
                .andExpect(jsonPath("$.content[0].status").value("INACTIVE"));
    }

    @Test
    void testGetMovieById_Public_Success() throws Exception {
        mockMvc.perform(get("/api/v1/movies/" + activeMovie1.getMovieId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movieId").value(activeMovie1.getMovieId().toString()))
                .andExpect(jsonPath("$.title").value("Inception"))
                .andExpect(jsonPath("$.durationMinutes").value(148))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void testGetMovieById_NotFound_Returns404() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/movies/" + nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void testCreateMovie_UnauthorizedWithoutToken() throws Exception {
        MovieCreateRequest request = MovieCreateRequest.builder()
                .title("Oppenheimer")
                .language("English")
                .genres(List.of("Biography", "Drama"))
                .durationMinutes(180)
                .build();

        mockMvc.perform(post("/api/v1/movies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void testCreateMovie_ForbiddenForCustomerAndOperator() throws Exception {
        MovieCreateRequest request = MovieCreateRequest.builder()
                .title("Oppenheimer")
                .language("English")
                .genres(List.of("Biography", "Drama"))
                .durationMinutes(180)
                .build();

        mockMvc.perform(post("/api/v1/movies")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(post("/api/v1/movies")
                .header("Authorization", "Bearer " + operatorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void testCreateMovie_Admin_Success_Returns201() throws Exception {
        MovieCreateRequest request = MovieCreateRequest.builder()
                .title("Oppenheimer")
                .description("Story of American scientist J. Robert Oppenheimer")
                .language("English")
                .genres(List.of("Biography", "Drama", "History"))
                .durationMinutes(180)
                .releaseDate(LocalDate.of(2023, 7, 21))
                .posterReference("https://example.com/oppenheimer.jpg")
                .build();

        mockMvc.perform(post("/api/v1/movies")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Oppenheimer"))
                .andExpect(jsonPath("$.language").value("English"))
                .andExpect(jsonPath("$.durationMinutes").value(180))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.movieId").isNotEmpty());
    }

    @Test
    void testCreateMovie_ValidationFailure_Returns400() throws Exception {
        MovieCreateRequest invalidRequest = MovieCreateRequest.builder()
                .title("") // Blank title
                .language("English")
                .genres(List.of()) // Empty genres
                .durationMinutes(-10) // Invalid duration
                .build();

        mockMvc.perform(post("/api/v1/movies")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void testUpdateMovie_Admin_Success_Returns200() throws Exception {
        MovieCreateRequest updateRequest = MovieCreateRequest.builder()
                .title("Inception Remastered")
                .description("Updated high-def remaster")
                .language("English")
                .genres(List.of("Action", "Sci-Fi", "Thriller"))
                .durationMinutes(150)
                .releaseDate(LocalDate.of(2010, 7, 16))
                .posterReference("https://example.com/inception_remastered.jpg")
                .build();

        mockMvc.perform(put("/api/v1/movies/" + activeMovie1.getMovieId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movieId").value(activeMovie1.getMovieId().toString()))
                .andExpect(jsonPath("$.title").value("Inception Remastered"))
                .andExpect(jsonPath("$.durationMinutes").value(150));
    }

    @Test
    void testUpdateMovie_NotFound_Returns404() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        MovieCreateRequest updateRequest = MovieCreateRequest.builder()
                .title("Does Not Exist")
                .language("English")
                .genres(List.of("Action"))
                .durationMinutes(120)
                .build();

        mockMvc.perform(put("/api/v1/movies/" + nonExistentId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void testDeleteMovie_Admin_Success_DeactivatesAndReturns204() throws Exception {
        mockMvc.perform(delete("/api/v1/movies/" + activeMovie1.getMovieId())
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // Verify that movie in database is now INACTIVE
        Movie deactivated = movieRepository.findById(activeMovie1.getMovieId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(MovieStatus.INACTIVE, deactivated.getStatus());
    }

    @Test
    void testDeleteMovie_NotFound_Returns404() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        mockMvc.perform(delete("/api/v1/movies/" + nonExistentId)
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void testDeleteMovie_WithoutToken_Returns401() throws Exception {
        mockMvc.perform(delete("/api/v1/movies/" + activeMovie1.getMovieId()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void testDeleteMovie_ForbiddenForCustomer() throws Exception {
        mockMvc.perform(delete("/api/v1/movies/" + activeMovie1.getMovieId())
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void testDeleteMovie_WithInvalidUuid_Returns400BadRequest() throws Exception {
        mockMvc.perform(delete(java.net.URI.create("/api/v1/movies/invalid-uuid-123"))
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
                .andExpect(jsonPath("$.message").value("Invalid format for parameter 'movieId'"));
    }

    @Test
    void testCreateMovie_WithNestedMetadata_PersistsAndReturnsMetadata() throws Exception {
        java.util.Map<String, Object> metadata = java.util.Map.of(
                "director", "Christopher Nolan",
                "cast", List.of("Matthew McConaughey", "Anne Hathaway", "Jessica Chastain"),
                "boxOffice", java.util.Map.of("budget", 165000000, "gross", 701729206)
        );

        MovieCreateRequest request = MovieCreateRequest.builder()
                .title("Interstellar")
                .description("A team of explorers travel through a wormhole in space.")
                .language("English")
                .genres(List.of("Adventure", "Drama", "Sci-Fi"))
                .durationMinutes(169)
                .releaseDate(LocalDate.of(2014, 11, 7))
                .posterReference("https://example.com/interstellar.jpg")
                .metadata(metadata)
                .build();

        String responseBody = mockMvc.perform(post("/api/v1/movies")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Interstellar"))
                .andExpect(jsonPath("$.metadata.director").value("Christopher Nolan"))
                .andExpect(jsonPath("$.metadata.cast[0]").value("Matthew McConaughey"))
                .andExpect(jsonPath("$.metadata.boxOffice.budget").value(165000000))
                .andReturn().getResponse().getContentAsString();

        com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(responseBody);
        UUID createdMovieId = UUID.fromString(jsonNode.get("movieId").asText());

        // Verify database round-trip directly from repository
        Movie persisted = movieRepository.findById(createdMovieId).orElseThrow();
        org.junit.jupiter.api.Assertions.assertNotNull(persisted.getMetadata());
        org.junit.jupiter.api.Assertions.assertEquals("Christopher Nolan", persisted.getMetadata().get("director"));

        // Verify GET returns metadata
        mockMvc.perform(get("/api/v1/movies/" + createdMovieId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata.director").value("Christopher Nolan"))
                .andExpect(jsonPath("$.metadata.cast[1]").value("Anne Hathaway"));
    }

    @Test
    void testUpdateMovie_WithUpdatedMetadata_UpdatesSuccessfully() throws Exception {
        java.util.Map<String, Object> updatedMetadata = java.util.Map.of(
                "director", "Christopher Nolan",
                "remastered", true,
                "aspectRatio", "1.43:1 IMAX"
        );

        MovieCreateRequest updateRequest = MovieCreateRequest.builder()
                .title("Inception Remastered")
                .language("English")
                .genres(List.of("Action", "Sci-Fi"))
                .durationMinutes(148)
                .metadata(updatedMetadata)
                .build();

        mockMvc.perform(put("/api/v1/movies/" + activeMovie1.getMovieId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata.remastered").value(true))
                .andExpect(jsonPath("$.metadata.aspectRatio").value("1.43:1 IMAX"));

        // Verify database persistence
        Movie updated = movieRepository.findById(activeMovie1.getMovieId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("1.43:1 IMAX", updated.getMetadata().get("aspectRatio"));
    }
}
