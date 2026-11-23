package com.reservar.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TokenHasherTest {

    @ParameterizedTest
    @CsvSource({
            "abc, ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            "'', e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
    })
    void deberiaGenerarElSha256HexadecimalConocidoCuandoRecibeUnTexto(String entrada, String hashEsperado) {
        // Arrange (valores parametrizados: vectores de prueba oficiales de SHA-256)

        // Act
        String hash = TokenHasher.hash(entrada);

        // Assert
        assertEquals(hashEsperado, hash);
    }

    @Test
    void deberiaGenerarElMismoHashCuandoSeHasheaDosVecesElMismoToken() {
        // Arrange
        String token = "a1b2c3d4e5";
        String primerHash = TokenHasher.hash(token);

        // Act
        String segundoHash = TokenHasher.hash(token);

        // Assert
        assertEquals(primerHash, segundoHash);
    }

    @Test
    void deberiaGenerarHashesDistintosCuandoLosTokensSonDistintos() {
        // Arrange
        String hashA = TokenHasher.hash("token-a");

        // Act
        String hashB = TokenHasher.hash("token-b");

        // Assert
        assertNotEquals(hashA, hashB);
    }

    @Test
    void deberiaLanzarNullPointerExceptionCuandoElTokenEsNulo() {
        // Arrange
        String token = null;

        // Act + Assert
        assertThrows(NullPointerException.class, () -> TokenHasher.hash(token));
    }

    @Test
    void deberiaEnvolverLaFaltaDelAlgoritmoEnUnaExcepcionDeEstado() {
        // Arrange: un JVM sin SHA-256 (caso imposible en la práctica, pero el contrato debe ser claro).
        try (org.mockito.MockedStatic<java.security.MessageDigest> digest =
                     org.mockito.Mockito.mockStatic(java.security.MessageDigest.class)) {
            java.security.NoSuchAlgorithmException causa = new java.security.NoSuchAlgorithmException("SHA-256");
            digest.when(() -> java.security.MessageDigest.getInstance("SHA-256")).thenThrow(causa);

            // Act
            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> TokenHasher.hash("abc"));

            // Assert
            assertEquals(causa, ex.getCause());
        }
    }

    @Test
    void noDeberiaPoderInstanciarseLaClaseUtilitaria() throws Exception {
        // Arrange
        java.lang.reflect.Constructor<TokenHasher> constructor = TokenHasher.class.getDeclaredConstructor();

        // Act
        constructor.setAccessible(true);

        // Assert
        assertEquals(true, java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()));
        org.junit.jupiter.api.Assertions.assertNotNull(constructor.newInstance());
    }
}
