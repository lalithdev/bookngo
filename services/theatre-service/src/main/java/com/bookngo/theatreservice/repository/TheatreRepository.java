package com.bookngo.theatreservice.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bookngo.theatreservice.entity.Theatre;
import com.bookngo.theatreservice.entity.TheatreStatus;

@Repository
public interface TheatreRepository extends JpaRepository<Theatre, UUID> {

    List<Theatre> findByStatus(TheatreStatus status);

    List<Theatre> findByCityIgnoreCaseAndStatus(String city, TheatreStatus status);

    List<Theatre> findByCityIgnoreCase(String city);
}
