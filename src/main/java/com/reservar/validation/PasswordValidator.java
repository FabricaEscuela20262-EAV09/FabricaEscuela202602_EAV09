package com.reservar.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class PasswordValidator {

    // find() sobre una clase de caracteres evita el backtracking de ".*X.*" con matches() (Sonar java:S8786).
    private static final Pattern MAYUSCULA = Pattern.compile("[A-Z]");
    private static final Pattern DIGITO = Pattern.compile("\\d");
    private static final Pattern ESPECIAL = Pattern.compile("[^a-zA-Z0-9]");

    private PasswordValidator() {
    }

    public static List<String> validate(String password) {

        List<String> errores = new ArrayList<>();

        if (password == null || password.length() < 8) {
            errores.add("La contraseña debe tener mínimo 8 caracteres");
        }

        if (password == null || !MAYUSCULA.matcher(password).find()) {
            errores.add("La contraseña debe contener al menos una mayúscula");
        }

        if (password == null || !DIGITO.matcher(password).find()) {
            errores.add("La contraseña debe contener al menos un número");
        }

        if (password == null || !ESPECIAL.matcher(password).find()) {
            errores.add("La contraseña debe contener al menos un carácter especial");
        }

        return errores;
    }
}
