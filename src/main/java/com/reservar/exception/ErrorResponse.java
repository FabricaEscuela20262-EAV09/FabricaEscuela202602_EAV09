package com.reservar.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class ErrorResponse {

  private String codigo;
  private String mensaje;
  private LocalDateTime fecha;
  private List<String> errores;
}
