package com.example.sistemahotel;

import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

// El método main solo delega en SpringApplication.run; se verifica sin
// levantar el servidor.
@Tag("mockito")
class SistemahotelApplicationMainTest {

    @Test
    void mainArrancaLaAplicacionConSusArgumentos() {
        String[] args = {"--server.port=0"};
        try (MockedStatic<SpringApplication> spring = mockStatic(SpringApplication.class)) {
            SistemahotelApplication.main(args);
            spring.verify(() -> SpringApplication.run(SistemahotelApplication.class, args));
        }
    }
}
