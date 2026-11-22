package com.example.sistemahotel.feature;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

// Punto de entrada de las pruebas feature (BDD). Ejecuta los archivos .feature de
// src/test/resources/features con Cucumber. Cada característica lleva la
// etiqueta @feature, que el pipeline usa con -Dgroups=feature.
@Suite(failIfNoTests = false)
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.example.sistemahotel.feature")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty, summary")
class CucumberFeatureTest {
}
