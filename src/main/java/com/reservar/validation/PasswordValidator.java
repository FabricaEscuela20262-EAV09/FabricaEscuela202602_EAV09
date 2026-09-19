package com.reservar.validation;

import java.util.ArrayList;
import java.util.List;

public final class PasswordValidator {

    private PasswordValidator() {
    }

    public static List<String> validate(String password) {

        List<String> errores = new ArrayList<>();

        if (password == null || password.length() < 8) {
            errores.add("La contraseña debe tener mínimo 8 caracteres");
        }

        if (password == null || !password.matches(".*[A-Z].*")) {
            errores.add("La contraseña debe contener al menos una mayúscula");
        }

        if (password == null || !password.matches(".*\\d.*")) {
            errores.add("La contraseña debe contener al menos un número");
        }

        if (password == null || !password.matches(".*[^a-zA-Z0-9].*")) {
            errores.add("La contraseña debe contener al menos un carácter especial");
        }

        return errores;
    }
}
