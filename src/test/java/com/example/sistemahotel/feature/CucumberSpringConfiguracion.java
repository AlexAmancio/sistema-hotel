package com.example.sistemahotel.feature;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import io.cucumber.spring.CucumberContextConfiguration;

// Las pruebas feature levantan la aplicación completa: controladores, servicios
// y repositorios reales sobre MySQL, sin simulaciones. Cada escenario corre dentro
// de una transacción que se deshace al terminar (ver Hooks), así no deja datos.
@CucumberContextConfiguration
@SpringBootTest
@AutoConfigureMockMvc
public class CucumberSpringConfiguracion {
}
