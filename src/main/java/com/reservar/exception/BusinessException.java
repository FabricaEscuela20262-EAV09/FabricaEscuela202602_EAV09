package com.reservar.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final String codigo;

    public BusinessException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }
}
