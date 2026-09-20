package com.bookngo.theatreservice.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bookngo.theatreservice.entity.Screen;

@Repository
public interface ScreenRepository extends JpaRepository<Screen, UUID> {

    List<Screen> findByTheatre_TheatreId(UUID theatreId);

    boolean existsByTheatre_TheatreIdAndName(UUID theatreId, String name);

    Optional<Screen> findByTheatre_TheatreIdAndName(UUID theatreId, String name);
}
