package com.reservar.repository;

import com.reservar.entity.HistorialReserva;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HistorialReservaRepository
        extends JpaRepository<HistorialReserva, Long> {
    List<HistorialReserva> findByReservaIdReservaOrderByFechaCambioAscIdHistorialAsc(Long idReserva);
}
