package com.reservar;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

// Prueba de integración: necesita la base de datos real (variables DB_USER / DB_PWORD).
// Sin ellas se omite para que `mvn verify` y el análisis de SonarCloud no dependan de Neon.
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "DB_USER", matches = ".+")
class ReservarApplicationTests {

	@Test
	void contextLoads() {
	}

}
