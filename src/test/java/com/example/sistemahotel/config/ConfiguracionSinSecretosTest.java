package com.example.sistemahotel.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

// Defectos D3, D4 y D5: ningún secreto ni la conexión con root quedan escritos
// en el código o en la configuración versionada. Se leen los archivos tal como
// están en el repositorio.
@Tag("unitaria")
class ConfiguracionSinSecretosTest {

    private static String propiedadesTexto;
    private static Properties propiedades;
    private static String securityConfig;

    @BeforeAll
    static void leer() throws Exception {
        Path p = Path.of("src/main/resources/application.properties");
        propiedadesTexto = Files.readString(p);
        propiedades = new Properties();
        try (var in = Files.newBufferedReader(p)) {
            propiedades.load(in);
        }
        securityConfig = Files.readString(Path.of("src/main/java/com/example/sistemahotel/config/SecurityConfig.java"));
    }

    @Test
    void d3_el_token_de_apiperu_no_tiene_valor_escrito_en_la_configuracion() {
        assertTrue(propiedades.getProperty("apiperu.token").matches("\\$\\{APIPERU_TOKEN:\\}"), propiedades.getProperty("apiperu.token"));
    }

    @Test
    void d3_no_hay_claves_largas_en_hexadecimal_en_la_configuracion() {
        assertFalse(Pattern.compile("[0-9a-f]{32,}").matcher(propiedadesTexto).find());
    }

    @Test
    void d4_la_clave_de_recuerdame_no_esta_escrita_en_el_codigo() {
        assertFalse(securityConfig.contains("estadia-remember-me-key"));
        assertFalse(Pattern.compile("rememberMe\\.key\\(\"").matcher(securityConfig).find());
        assertTrue(propiedades.getProperty("security.remember-me.key").startsWith("${REMEMBER_ME_KEY"));
    }

    @Test
    void d5_la_conexion_no_usa_root_ni_tiene_la_clave_escrita() {
        String usuario = propiedades.getProperty("spring.datasource.username");
        assertFalse(usuario.equalsIgnoreCase("root") || usuario.contains(":root}"), usuario);
        assertTrue(propiedades.getProperty("spring.datasource.password").matches("\\$\\{DB_PASSWORD\\}"));
    }

    @Test
    void los_secretos_se_leen_de_un_archivo_env_que_no_se_versiona() throws Exception {
        assertTrue(propiedades.getProperty("spring.config.import").contains(".env"));
        assertTrue(Files.readAllLines(Path.of(".gitignore")).contains(".env"));
        assertTrue(Files.exists(Path.of(".env.example")));
    }
}
