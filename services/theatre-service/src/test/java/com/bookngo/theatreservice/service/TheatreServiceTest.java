package com.bookngo.theatreservice.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.bookngo.theatreservice.dto.ConfigureSeatsRequest;
import com.bookngo.theatreservice.dto.PhysicalSeatCreateRequest;
import com.bookngo.theatreservice.dto.PhysicalSeatResponse;
import com.bookngo.theatreservice.dto.ScreenCreateRequest;
import com.bookngo.theatreservice.dto.ScreenResponse;
import com.bookngo.theatreservice.dto.TheatreCreateRequest;
import com.bookngo.theatreservice.dto.TheatreResponse;
import com.bookngo.theatreservice.entity.PhysicalSeat;
import com.bookngo.theatreservice.entity.PhysicalSeatStatus;
import com.bookngo.theatreservice.entity.Screen;
import com.bookngo.theatreservice.entity.ScreenStatus;
import com.bookngo.theatreservice.entity.Theatre;
import com.bookngo.theatreservice.entity.TheatreOperatorAssignment;
import com.bookngo.theatreservice.entity.TheatreStatus;
import com.bookngo.theatreservice.exception.ConflictException;
import com.bookngo.theatreservice.exception.ResourceNotFoundException;
import com.bookngo.theatreservice.repository.PhysicalSeatRepository;
import com.bookngo.theatreservice.repository.ScreenRepository;
import com.bookngo.theatreservice.repository.TheatreOperatorAssignmentRepository;
import com.bookngo.theatreservice.repository.TheatreRepository;
import com.bookngo.theatreservice.security.OperatorAuthorizationService;

@ExtendWith(MockitoExtension.class)
class TheatreServiceTest {

    @Mock
    private TheatreRepository theatreRepository;

    @Mock
    private ScreenRepository screenRepository;

    @Mock
    private PhysicalSeatRepository physicalSeatRepository;

    @Mock
    private TheatreOperatorAssignmentRepository assignmentRepository;

    @Mock
    private OperatorAuthorizationService operatorAuthorizationService;

    @InjectMocks
    private TheatreService theatreService;

    private Theatre testTheatre;
    private Screen testScreen;
    private UUID theatreId;
    private UUID screenId;
    private UUID operatorId;

    @BeforeEach
    void setUp() {
        theatreId = UUID.randomUUID();
        screenId = UUID.randomUUID();
        operatorId = UUID.randomUUID();

        testTheatre = Theatre.builder()
                .theatreId(theatreId)
                .name("PVR Cinemas")
                .address("MG Road")
                .city("Bengaluru")
                .status(TheatreStatus.ACTIVE)
                .build();

        testScreen = Screen.builder()
                .screenId(screenId)
                .theatre(testTheatre)
                .name("Screen 1")
                .screenFormat("IMAX")
                .status(ScreenStatus.ACTIVE)
                .build();
    }

    @Test
    void listTheatres_withoutCity_returnsActiveTheatres() {
        when(theatreRepository.findByStatus(TheatreStatus.ACTIVE)).thenReturn(List.of(testTheatre));

        List<TheatreResponse> result = theatreService.listTheatres(null);

        assertEquals(1, result.size());
        assertEquals("PVR Cinemas", result.get(0).getName());
        verify(theatreRepository).findByStatus(TheatreStatus.ACTIVE);
    }

    @Test
    void listTheatres_withCity_returnsFilteredTheatres() {
        when(theatreRepository.findByCityIgnoreCaseAndStatus("Bengaluru", TheatreStatus.ACTIVE))
                .thenReturn(List.of(testTheatre));

        List<TheatreResponse> result = theatreService.listTheatres("Bengaluru");

        assertEquals(1, result.size());
        assertEquals("Bengaluru", result.get(0).getCity());
        verify(theatreRepository).findByCityIgnoreCaseAndStatus("Bengaluru", TheatreStatus.ACTIVE);
    }

    @Test
    void getTheatreById_existingTheatre_returnsTheatre() {
        when(theatreRepository.findById(theatreId)).thenReturn(Optional.of(testTheatre));

        TheatreResponse response = theatreService.getTheatreById(theatreId);

        assertNotNull(response);
        assertEquals(theatreId, response.getTheatreId());
    }

    @Test
    void getTheatreById_nonexistentTheatre_throwsResourceNotFound() {
        when(theatreRepository.findById(theatreId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> theatreService.getTheatreById(theatreId));
    }

    @Test
    void createTheatre_byOperator_createsTheatreAndAssignment() {
        TheatreCreateRequest request = new TheatreCreateRequest("INOX", "Forum Mall", "Bengaluru");
        when(theatreRepository.save(any(Theatre.class))).thenAnswer(invocation -> {
            Theatre t = invocation.getArgument(0);
            t.setTheatreId(theatreId);
            return t;
        });

        TheatreResponse response = theatreService.createTheatre(request, operatorId, false);

        assertNotNull(response);
        assertEquals("INOX", response.getName());
        verify(assignmentRepository, times(1)).save(any(TheatreOperatorAssignment.class));
    }

    @Test
    void createTheatre_byAdmin_createsTheatreWithoutOperatorAssignment() {
        TheatreCreateRequest request = new TheatreCreateRequest("INOX", "Forum Mall", "Bengaluru");
        when(theatreRepository.save(any(Theatre.class))).thenAnswer(invocation -> {
            Theatre t = invocation.getArgument(0);
            t.setTheatreId(theatreId);
            return t;
        });

        TheatreResponse response = theatreService.createTheatre(request, null, true);

        assertNotNull(response);
        verify(assignmentRepository, never()).save(any(TheatreOperatorAssignment.class));
    }

    @Test
    void updateTheatre_authorizedOperator_updatesSuccessfully() {
        TheatreCreateRequest updateReq = new TheatreCreateRequest("PVR Superplex", "Updated Road", "Bengaluru");
        when(theatreRepository.findById(theatreId)).thenReturn(Optional.of(testTheatre));
        when(theatreRepository.save(any(Theatre.class))).thenAnswer(inv -> inv.getArgument(0));

        TheatreResponse response = theatreService.updateTheatre(theatreId, updateReq, operatorId, false);

        assertEquals("PVR Superplex", response.getName());
        assertEquals("Updated Road", response.getAddress());
        verify(operatorAuthorizationService).verifyOperatorAssignedToTheatre(theatreId, operatorId, false);
    }

    @Test
    void updateTheatre_unauthorizedOperator_throwsAccessDenied() {
        TheatreCreateRequest updateReq = new TheatreCreateRequest("PVR Superplex", "Updated Road", "Bengaluru");
        when(theatreRepository.findById(theatreId)).thenReturn(Optional.of(testTheatre));
        doThrow(new AccessDeniedException("Theatre operator is not assigned to this theatre"))
                .when(operatorAuthorizationService).verifyOperatorAssignedToTheatre(theatreId, operatorId, false);

        assertThrows(AccessDeniedException.class,
                () -> theatreService.updateTheatre(theatreId, updateReq, operatorId, false));
    }

    @Test
    void createScreen_authorizedOperator_createsScreen() {
        ScreenCreateRequest req = new ScreenCreateRequest("Screen 2", "4DX");
        when(theatreRepository.findById(theatreId)).thenReturn(Optional.of(testTheatre));
        when(screenRepository.existsByTheatre_TheatreIdAndName(theatreId, "Screen 2")).thenReturn(false);
        when(screenRepository.save(any(Screen.class))).thenAnswer(inv -> {
            Screen s = inv.getArgument(0);
            s.setScreenId(UUID.randomUUID());
            return s;
        });

        ScreenResponse response = theatreService.createScreen(theatreId, req, operatorId, false);

        assertNotNull(response);
        assertEquals("Screen 2", response.getName());
        assertEquals("4DX", response.getScreenFormat());
    }

    @Test
    void createScreen_duplicateName_throwsConflictException() {
        ScreenCreateRequest req = new ScreenCreateRequest("Screen 1", "IMAX");
        when(theatreRepository.findById(theatreId)).thenReturn(Optional.of(testTheatre));
        when(screenRepository.existsByTheatre_TheatreIdAndName(theatreId, "Screen 1")).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> theatreService.createScreen(theatreId, req, operatorId, false));
    }

    @Test
    void configurePhysicalSeats_validSeats_configuresSuccessfully() {
        ConfigureSeatsRequest req = new ConfigureSeatsRequest(List.of(
                new PhysicalSeatCreateRequest("A", "1", "REGULAR"),
                new PhysicalSeatCreateRequest("A", "2", "REGULAR")
        ));

        when(screenRepository.findById(screenId)).thenReturn(Optional.of(testScreen));
        when(physicalSeatRepository.existsByScreen_ScreenIdAndRowLabelAndSeatNumber(screenId, "A", "1")).thenReturn(false);
        when(physicalSeatRepository.existsByScreen_ScreenIdAndRowLabelAndSeatNumber(screenId, "A", "2")).thenReturn(false);
        when(physicalSeatRepository.saveAll(any())).thenAnswer(inv -> {
            List<PhysicalSeat> seats = inv.getArgument(0);
            seats.forEach(s -> s.setPhysicalSeatId(UUID.randomUUID()));
            return seats;
        });

        List<PhysicalSeatResponse> responses = theatreService.configurePhysicalSeats(
                screenId, req, operatorId, false);

        assertEquals(2, responses.size());
        assertEquals("A", responses.get(0).getRowLabel());
        assertEquals(PhysicalSeatStatus.ACTIVE, responses.get(0).getStatus());
    }

    @Test
    void configurePhysicalSeats_duplicateInRequest_throwsConflict() {
        ConfigureSeatsRequest req = new ConfigureSeatsRequest(List.of(
                new PhysicalSeatCreateRequest("A", "1", "REGULAR"),
                new PhysicalSeatCreateRequest("A", "1", "PREMIUM")
        ));

        when(screenRepository.findById(screenId)).thenReturn(Optional.of(testScreen));

        assertThrows(ConflictException.class,
                () -> theatreService.configurePhysicalSeats(screenId, req, operatorId, false));
    }

    @Test
    void configurePhysicalSeats_duplicateInDb_throwsConflict() {
        ConfigureSeatsRequest req = new ConfigureSeatsRequest(List.of(
                new PhysicalSeatCreateRequest("A", "1", "REGULAR")
        ));

        when(screenRepository.findById(screenId)).thenReturn(Optional.of(testScreen));
        when(physicalSeatRepository.existsByScreen_ScreenIdAndRowLabelAndSeatNumber(screenId, "A", "1")).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> theatreService.configurePhysicalSeats(screenId, req, operatorId, false));
    }
}
