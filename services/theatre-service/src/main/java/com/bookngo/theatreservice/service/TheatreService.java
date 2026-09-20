package com.bookngo.theatreservice.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookngo.theatreservice.dto.ConfigureSeatsRequest;
import com.bookngo.theatreservice.dto.PhysicalSeatCreateRequest;
import com.bookngo.theatreservice.dto.PhysicalSeatResponse;
import com.bookngo.theatreservice.dto.ScreenCreateRequest;
import com.bookngo.theatreservice.dto.ScreenResponse;
import com.bookngo.theatreservice.dto.TheatreCreateRequest;
import com.bookngo.theatreservice.dto.TheatreResponse;
import com.bookngo.theatreservice.entity.OperatorAssignmentStatus;
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

@Service
@Transactional
public class TheatreService {

    private final TheatreRepository theatreRepository;
    private final ScreenRepository screenRepository;
    private final PhysicalSeatRepository physicalSeatRepository;
    private final TheatreOperatorAssignmentRepository assignmentRepository;
    private final OperatorAuthorizationService operatorAuthorizationService;

    public TheatreService(
            TheatreRepository theatreRepository,
            ScreenRepository screenRepository,
            PhysicalSeatRepository physicalSeatRepository,
            TheatreOperatorAssignmentRepository assignmentRepository,
            OperatorAuthorizationService operatorAuthorizationService) {
        this.theatreRepository = theatreRepository;
        this.screenRepository = screenRepository;
        this.physicalSeatRepository = physicalSeatRepository;
        this.assignmentRepository = assignmentRepository;
        this.operatorAuthorizationService = operatorAuthorizationService;
    }

    @Transactional(readOnly = true)
    public List<TheatreResponse> listTheatres(String city) {
        List<Theatre> theatres;
        if (city != null && !city.trim().isEmpty()) {
            theatres = theatreRepository.findByCityIgnoreCaseAndStatus(city.trim(), TheatreStatus.ACTIVE);
        } else {
            theatres = theatreRepository.findByStatus(TheatreStatus.ACTIVE);
        }
        return theatres.stream().map(this::toTheatreResponse).toList();
    }

    @Transactional(readOnly = true)
    public TheatreResponse getTheatreById(UUID theatreId) {
        Theatre theatre = findTheatreOrThrow(theatreId);
        return toTheatreResponse(theatre);
    }

    public TheatreResponse createTheatre(TheatreCreateRequest request, UUID userId, boolean isAdmin) {
        Theatre theatre = Theatre.builder()
                .name(request.getName().trim())
                .address(request.getAddress().trim())
                .city(request.getCity().trim())
                .status(TheatreStatus.ACTIVE)
                .build();

        Theatre savedTheatre = theatreRepository.save(theatre);

        // If created by a theatre operator, record assignment
        if (!isAdmin && userId != null) {
            TheatreOperatorAssignment assignment = TheatreOperatorAssignment.builder()
                    .theatre(savedTheatre)
                    .userId(userId)
                    .status(OperatorAssignmentStatus.ACTIVE)
                    .build();
            assignmentRepository.save(assignment);
        }

        return toTheatreResponse(savedTheatre);
    }

    public TheatreResponse updateTheatre(UUID theatreId, TheatreCreateRequest request, UUID userId, boolean isAdmin) {
        Theatre theatre = findTheatreOrThrow(theatreId);
        operatorAuthorizationService.verifyOperatorAssignedToTheatre(theatreId, userId, isAdmin);

        theatre.setName(request.getName().trim());
        theatre.setAddress(request.getAddress().trim());
        theatre.setCity(request.getCity().trim());

        Theatre updatedTheatre = theatreRepository.save(theatre);
        return toTheatreResponse(updatedTheatre);
    }

    @Transactional(readOnly = true)
    public List<ScreenResponse> getScreensByTheatre(UUID theatreId) {
        findTheatreOrThrow(theatreId);
        List<Screen> screens = screenRepository.findByTheatre_TheatreId(theatreId);
        return screens.stream().map(this::toScreenResponse).toList();
    }

    public ScreenResponse createScreen(UUID theatreId, ScreenCreateRequest request, UUID userId, boolean isAdmin) {
        Theatre theatre = findTheatreOrThrow(theatreId);
        operatorAuthorizationService.verifyOperatorAssignedToTheatre(theatreId, userId, isAdmin);

        String trimmedName = request.getName().trim();
        if (screenRepository.existsByTheatre_TheatreIdAndName(theatreId, trimmedName)) {
            throw new ConflictException("Screen with name '" + trimmedName + "' already exists for this theatre");
        }

        Screen screen = Screen.builder()
                .theatre(theatre)
                .name(trimmedName)
                .screenFormat(request.getScreenFormat() != null ? request.getScreenFormat().trim() : null)
                .status(ScreenStatus.ACTIVE)
                .build();

        Screen savedScreen = screenRepository.save(screen);
        return toScreenResponse(savedScreen);
    }

    @Transactional(readOnly = true)
    public List<PhysicalSeatResponse> getPhysicalSeatsByScreen(UUID screenId) {
        findScreenOrThrow(screenId);
        List<PhysicalSeat> seats = physicalSeatRepository.findByScreen_ScreenId(screenId);
        return seats.stream().map(this::toPhysicalSeatResponse).toList();
    }

    public List<PhysicalSeatResponse> configurePhysicalSeats(
            UUID screenId, ConfigureSeatsRequest request, UUID userId, boolean isAdmin) {
        Screen screen = findScreenOrThrow(screenId);
        UUID theatreId = screen.getTheatre().getTheatreId();
        operatorAuthorizationService.verifyOperatorAssignedToTheatre(theatreId, userId, isAdmin);

        Set<String> seenPositionsInRequest = new HashSet<>();
        List<PhysicalSeat> seatsToSave = new ArrayList<>();

        for (PhysicalSeatCreateRequest seatReq : request.getSeats()) {
            String rowLabel = seatReq.getRowLabel().trim();
            String seatNumber = seatReq.getSeatNumber().trim();
            String seatKey = rowLabel + ":" + seatNumber;

            if (!seenPositionsInRequest.add(seatKey)) {
                throw new ConflictException(
                        "Duplicate seat in request: row " + rowLabel + ", number " + seatNumber);
            }

            if (physicalSeatRepository.existsByScreen_ScreenIdAndRowLabelAndSeatNumber(screenId, rowLabel, seatNumber)) {
                throw new ConflictException(
                        "Physical seat already exists at row " + rowLabel + ", number " + seatNumber);
            }

            PhysicalSeat seat = PhysicalSeat.builder()
                    .screen(screen)
                    .rowLabel(rowLabel)
                    .seatNumber(seatNumber)
                    .seatCategory(seatReq.getSeatCategory().trim())
                    .status(PhysicalSeatStatus.ACTIVE)
                    .build();

            seatsToSave.add(seat);
        }

        List<PhysicalSeat> savedSeats = physicalSeatRepository.saveAll(seatsToSave);
        return savedSeats.stream().map(this::toPhysicalSeatResponse).toList();
    }

    private Theatre findTheatreOrThrow(UUID theatreId) {
        return theatreRepository.findById(theatreId)
                .orElseThrow(() -> new ResourceNotFoundException("Theatre not found with id: " + theatreId));
    }

    private Screen findScreenOrThrow(UUID screenId) {
        return screenRepository.findById(screenId)
                .orElseThrow(() -> new ResourceNotFoundException("Screen not found with id: " + screenId));
    }

    private TheatreResponse toTheatreResponse(Theatre theatre) {
        return TheatreResponse.builder()
                .theatreId(theatre.getTheatreId())
                .name(theatre.getName())
                .address(theatre.getAddress())
                .city(theatre.getCity())
                .locationMetadata(theatre.getLocationMetadata())
                .status(theatre.getStatus())
                .build();
    }

    private ScreenResponse toScreenResponse(Screen screen) {
        return ScreenResponse.builder()
                .screenId(screen.getScreenId())
                .theatreId(screen.getTheatre().getTheatreId())
                .name(screen.getName())
                .screenFormat(screen.getScreenFormat())
                .status(screen.getStatus())
                .build();
    }

    private PhysicalSeatResponse toPhysicalSeatResponse(PhysicalSeat seat) {
        return PhysicalSeatResponse.builder()
                .physicalSeatId(seat.getPhysicalSeatId())
                .screenId(seat.getScreen().getScreenId())
                .rowLabel(seat.getRowLabel())
                .seatNumber(seat.getSeatNumber())
                .seatCategory(seat.getSeatCategory())
                .status(seat.getStatus())
                .build();
    }
}
