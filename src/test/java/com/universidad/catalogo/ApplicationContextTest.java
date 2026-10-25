package com.universidad.catalogo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
class ApplicationContextTest {

    @Autowired HealthEndpoint health;

    @Test
    void contextLoads_conPerfilDev_construyeTodosLosBeans() {
        // Verifica que todos los beans de la aplicacion se construyen
        // correctamente con el perfil dev, sin necesitar una base de datos
        // externa (usa H2 en memoria).
    }

    @Test
    void health_conPerfilDev_respondeUp() {
        // El mismo indicador que consulta el smoke test del pipeline
        assertThat(health.health().getStatus()).isEqualTo(Status.UP);
    }
}
