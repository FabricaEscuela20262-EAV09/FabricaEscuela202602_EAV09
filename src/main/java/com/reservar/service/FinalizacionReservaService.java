package com.reservar.service;

import com.reservar.entity.enums.EstadoReserva;
import com.reservar.repository.ReservaRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class FinalizacionReservaService {
    private final ReservaRepository reservaRepository;
    private final Clock clock;

    public FinalizacionReservaService(ReservaRepository reservaRepository, Clock clock) {
        this.reservaRepository = reservaRepository;
        this.clock = clock;
    }

    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void completarFinalizadas() {
        reservaRepository.completarFinalizadas(EstadoReserva.CONFIRMADA,
                EstadoReserva.COMPLETADA, LocalDateTime.now(clock));
    }
}
