package com.bookngo.theatreservice.controller;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookngo.theatreservice.dto.ConfigureSeatsRequest;
import com.bookngo.theatreservice.dto.PhysicalSeatCreateRequest;
import com.bookngo.theatreservice.dto.ScreenCreateRequest;
import com.bookngo.theatreservice.dto.TheatreCreateRequest;
import com.bookngo.theatreservice.entity.OperatorAssignmentStatus;
import com.bookngo.theatreservice.entity.PhysicalSeat;
import com.bookngo.theatreservice.entity.PhysicalSeatStatus;
import com.bookngo.theatreservice.entity.Screen;
import com.bookngo.theatreservice.entity.ScreenStatus;
import com.bookngo.theatreservice.entity.Theatre;
import com.bookngo.theatreservice.entity.TheatreOperatorAssignment;
import com.bookngo.theatreservice.entity.TheatreStatus;
import com.bookngo.theatreservice.repository.PhysicalSeatRepository;
import com.bookngo.theatreservice.repository.ScreenRepository;
import com.bookngo.theatreservice.repository.TheatreOperatorAssignmentRepository;
import com.bookngo.theatreservice.repository.TheatreRepository;
import com.bookngo.theatreservice.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TheatreControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TheatreRepository theatreRepository;

    @Autowired
    private ScreenRepository screenRepository;

    @Autowired
    private PhysicalSeatRepository physicalSeatRepository;

    @Autowired
    private TheatreOperatorAssignmentRepository assignmentRepository;

    @Autowired
    private JwtService jwtService;

    private Theatre activeTheatre1;
    private Theatre activeTheatre2;
    private Screen screen1;

    private UUID operator1Id;
    private UUID operator2Id;
    private UUID adminId;
    private UUID customerId;

    private String operator1Token;
    private String operator2Token;
    private String adminToken;
    private String customerToken;

    @BeforeEach
    void setUp() {
        physicalSeatRepository.deleteAll();
        screenRepository.deleteAll();
        assignmentRepository.deleteAll();
        theatreRepository.deleteAll();

        operator1Id = UUID.randomUUID();
        operator2Id = UUID.randomUUID();
        adminId = UUID.randomUUID();
        customerId = UUID.randomUUID();

        operator1Token = jwtService.generateToken(operator1Id, List.of("THEATRE_OPERATOR"));
        operator2Token = jwtService.generateToken(operator2Id, List.of("THEATRE_OPERATOR"));
        adminToken = jwtService.generateToken(adminId, List.of("ADMIN"));
        customerToken = jwtService.generateToken(customerId, List.of("CUSTOMER"));

        activeTheatre1 = theatreRepository.save(Theatre.builder()
                .name("PVR Forum")
                .address("Koramangala")
                .city("Bengaluru")
                .status(TheatreStatus.ACTIVE)
                .build());

        activeTheatre2 = theatreRepository.save(Theatre.builder()
                .name("INOX Inorbit")
                .address("Cyberabad")
                .city("Hyderabad")
                .status(TheatreStatus.ACTIVE)
                .build());

        // Assign operator1 to activeTheatre1
        assignmentRepository.save(TheatreOperatorAssignment.builder()
                .theatre(activeTheatre1)
                .userId(operator1Id)
                .status(OperatorAssignmentStatus.ACTIVE)
                .build());

        screen1 = screenRepository.save(Screen.builder()
                .theatre(activeTheatre1)
                .name("Audi 1")
                .screenFormat("IMAX")
                .status(ScreenStatus.ACTIVE)
                .build());
    }

    @Test
    void listTheatres_public_returnsAllActiveTheatres() throws Exception {
        mockMvc.perform(get("/api/v1/theatres"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void listTheatres_filterByCity_returnsMatchingTheatres() throws Exception {
        mockMvc.perform(get("/api/v1/theatres").param("city", "Bengaluru"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("PVR Forum")));
    }

    @Test
    void getTheatreById_public_returnsTheatreDetails() throws Exception {
        mockMvc.perform(get("/api/v1/theatres/{theatreId}", activeTheatre1.getTheatreId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.theatreId", is(activeTheatre1.getTheatreId().toString())))
                .andExpect(jsonPath("$.name", is("PVR Forum")));
    }

    @Test
    void getTheatreById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/theatres/{theatreId}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("NOT_FOUND")));
    }

    @Test
    void createTheatre_anonymous_returns401() throws Exception {
        TheatreCreateRequest request = new TheatreCreateRequest("Cinepolis", "Bannerghatta", "Bengaluru");

        mockMvc.perform(post("/api/v1/theatres")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
    }

    @Test
    void createTheatre_customerRole_returns403() throws Exception {
        TheatreCreateRequest request = new TheatreCreateRequest("Cinepolis", "Bannerghatta", "Bengaluru");

        mockMvc.perform(post("/api/v1/theatres")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("FORBIDDEN")));
    }

    @Test
    void createTheatre_theatreOperator_createsAndAssignsOperator() throws Exception {
        TheatreCreateRequest request = new TheatreCreateRequest("Cinepolis", "Bannerghatta", "Bengaluru");

        mockMvc.perform(post("/api/v1/theatres")
                .header("Authorization", "Bearer " + operator1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Cinepolis")))
                .andExpect(jsonPath("$.status", is("ACTIVE")));

        List<Theatre> theatres = theatreRepository.findAll();
        Theatre created = theatres.stream()
                .filter(t -> t.getName().equals("Cinepolis"))
                .findFirst().orElseThrow();

        boolean isAssigned = assignmentRepository.existsByTheatre_TheatreIdAndUserIdAndStatus(
                created.getTheatreId(), operator1Id, OperatorAssignmentStatus.ACTIVE);
        assertTrue(isAssigned);
    }

    @Test
    void createTheatre_admin_createsSuccessfully() throws Exception {
        TheatreCreateRequest request = new TheatreCreateRequest("PVR Nexus", "Koramangala", "Bengaluru");

        mockMvc.perform(post("/api/v1/theatres")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("PVR Nexus")));
    }

    @Test
    void createTheatre_invalidPayload_returns400() throws Exception {
        TheatreCreateRequest request = new TheatreCreateRequest("", "", "");

        mockMvc.perform(post("/api/v1/theatres")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
    }

    @Test
    void updateTheatre_unassignedOperator_returns403() throws Exception {
        TheatreCreateRequest request = new TheatreCreateRequest("PVR Forum Renamed", "Koramangala", "Bengaluru");

        // operator2 is NOT assigned to activeTheatre1
        mockMvc.perform(put("/api/v1/theatres/{theatreId}", activeTheatre1.getTheatreId())
                .header("Authorization", "Bearer " + operator2Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("FORBIDDEN")));
    }

    @Test
    void updateTheatre_assignedOperator_updatesSuccessfully() throws Exception {
        TheatreCreateRequest request = new TheatreCreateRequest("PVR Forum Renamed", "Koramangala", "Bengaluru");

        mockMvc.perform(put("/api/v1/theatres/{theatreId}", activeTheatre1.getTheatreId())
                .header("Authorization", "Bearer " + operator1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("PVR Forum Renamed")));
    }

    @Test
    void updateTheatre_admin_updatesSuccessfullyWithoutAssignment() throws Exception {
        TheatreCreateRequest request = new TheatreCreateRequest("INOX Renamed by Admin", "Cyberabad", "Hyderabad");

        mockMvc.perform(put("/api/v1/theatres/{theatreId}", activeTheatre2.getTheatreId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("INOX Renamed by Admin")));
    }

    @Test
    void getScreensByTheatre_public_returnsScreens() throws Exception {
        mockMvc.perform(get("/api/v1/theatres/{theatreId}/screens", activeTheatre1.getTheatreId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Audi 1")));
    }

    @Test
    void createScreen_assignedOperator_createsScreen() throws Exception {
        ScreenCreateRequest request = new ScreenCreateRequest("Audi 2", "4DX");

        mockMvc.perform(post("/api/v1/theatres/{theatreId}/screens", activeTheatre1.getTheatreId())
                .header("Authorization", "Bearer " + operator1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Audi 2")))
                .andExpect(jsonPath("$.screenFormat", is("4DX")));
    }

    @Test
    void createScreen_unassignedOperator_returns403() throws Exception {
        ScreenCreateRequest request = new ScreenCreateRequest("Audi 2", "4DX");

        mockMvc.perform(post("/api/v1/theatres/{theatreId}/screens", activeTheatre1.getTheatreId())
                .header("Authorization", "Bearer " + operator2Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("FORBIDDEN")));
    }

    @Test
    void createScreen_duplicateName_returns409() throws Exception {
        ScreenCreateRequest request = new ScreenCreateRequest("Audi 1", "IMAX");

        mockMvc.perform(post("/api/v1/theatres/{theatreId}/screens", activeTheatre1.getTheatreId())
                .header("Authorization", "Bearer " + operator1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("CONFLICT")));
    }

    @Test
    void configurePhysicalSeats_assignedOperator_configuresSeats() throws Exception {
        ConfigureSeatsRequest request = new ConfigureSeatsRequest(List.of(
                new PhysicalSeatCreateRequest("A", "1", "REGULAR"),
                new PhysicalSeatCreateRequest("A", "2", "REGULAR"),
                new PhysicalSeatCreateRequest("B", "1", "PREMIUM")
        ));

        mockMvc.perform(post("/api/v1/screens/{screenId}/seats", screen1.getScreenId())
                .header("Authorization", "Bearer " + operator1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].rowLabel", is("A")))
                .andExpect(jsonPath("$[0].seatNumber", is("1")))
                .andExpect(jsonPath("$[0].status", is("ACTIVE")));
    }

    @Test
    void configurePhysicalSeats_customer_returns403() throws Exception {
        ConfigureSeatsRequest request = new ConfigureSeatsRequest(List.of(
                new PhysicalSeatCreateRequest("A", "1", "REGULAR")
        ));

        mockMvc.perform(post("/api/v1/screens/{screenId}/seats", screen1.getScreenId())
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("FORBIDDEN")));
    }

    @Test
    void configurePhysicalSeats_unassignedOperator_returns403() throws Exception {
        ConfigureSeatsRequest request = new ConfigureSeatsRequest(List.of(
                new PhysicalSeatCreateRequest("A", "1", "REGULAR")
        ));

        mockMvc.perform(post("/api/v1/screens/{screenId}/seats", screen1.getScreenId())
                .header("Authorization", "Bearer " + operator2Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("FORBIDDEN")));
    }

    @Test
    void configurePhysicalSeats_duplicateInRequest_returns409() throws Exception {
        ConfigureSeatsRequest request = new ConfigureSeatsRequest(List.of(
                new PhysicalSeatCreateRequest("A", "1", "REGULAR"),
                new PhysicalSeatCreateRequest("A", "1", "VIP")
        ));

        mockMvc.perform(post("/api/v1/screens/{screenId}/seats", screen1.getScreenId())
                .header("Authorization", "Bearer " + operator1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("CONFLICT")));
    }

    @Test
    void configurePhysicalSeats_seatAlreadyExistsInScreen_returns409() throws Exception {
        physicalSeatRepository.save(PhysicalSeat.builder()
                .screen(screen1)
                .rowLabel("A")
                .seatNumber("1")
                .seatCategory("REGULAR")
                .status(PhysicalSeatStatus.ACTIVE)
                .build());

        ConfigureSeatsRequest request = new ConfigureSeatsRequest(List.of(
                new PhysicalSeatCreateRequest("A", "1", "REGULAR")
        ));

        mockMvc.perform(post("/api/v1/screens/{screenId}/seats", screen1.getScreenId())
                .header("Authorization", "Bearer " + operator1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("CONFLICT")));
    }

    @Test
    void getPhysicalSeatsByScreen_public_returnsConfiguredSeats() throws Exception {
        physicalSeatRepository.save(PhysicalSeat.builder()
                .screen(screen1)
                .rowLabel("A")
                .seatNumber("1")
                .seatCategory("REGULAR")
                .status(PhysicalSeatStatus.ACTIVE)
                .build());

        mockMvc.perform(get("/api/v1/screens/{screenId}/seats", screen1.getScreenId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].rowLabel", is("A")))
                .andExpect(jsonPath("$[0].seatNumber", is("1")));
    }
}
