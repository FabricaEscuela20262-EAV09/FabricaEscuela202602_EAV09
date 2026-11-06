package com.reservar.repository;

import com.reservar.entity.HistorialReserva;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistorialReservaRepository
        extends JpaRepository<HistorialReserva, Long> {
}