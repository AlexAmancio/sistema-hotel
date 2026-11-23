package com.example.sistemahotel.aceptacion;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

// Punto de entrada de las pruebas de aceptación (BDD). Ejecuta los archivos .feature de
// src/test/resources/aceptacion con Cucumber. Cada característica lleva la
// etiqueta @aceptacion, que el pipeline usa con -Dgroups=aceptacion.
@Suite(failIfNoTests = false)
@IncludeEngines("cucumber")
@SelectClasspathResource("aceptacion")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.example.sistemahotel.aceptacion")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty, summary")
class CucumberAceptacionTest {
}
