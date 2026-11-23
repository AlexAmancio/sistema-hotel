package com.example.sistemahotel.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import com.example.sistemahotel.modelos.Categoria;
import com.example.sistemahotel.modelos.Cliente;
import com.example.sistemahotel.modelos.Consumo;
import com.example.sistemahotel.modelos.EstadoHabitacion;
import com.example.sistemahotel.modelos.EstadoRegistro;
import com.example.sistemahotel.modelos.Habitacion;
import com.example.sistemahotel.modelos.ProductoConsumo;
import com.example.sistemahotel.modelos.Registro;
import com.example.sistemahotel.modelos.Usuario;
import com.example.sistemahotel.repositories.ICategoriaRepository;
import com.example.sistemahotel.repositories.IClienteRepository;
import com.example.sistemahotel.repositories.IConsumoRepository;
import com.example.sistemahotel.repositories.IHabitacionRepository;
import com.example.sistemahotel.repositories.IProductoConsumoRepository;
import com.example.sistemahotel.repositories.IRegistroRepository;
import com.example.sistemahotel.repositories.IUsuarioRepository;

import jakarta.persistence.EntityManager;

/**
 * Base de las pruebas feature, al estilo de las pruebas feature de Laravel.
 *
 * <ul>
 *   <li>Levanta la aplicación completa (controladores, servicios, repositorios,
 *       seguridad y MySQL) sin simular ninguna capa propia del sistema.</li>
 *   <li>Cada prueba corre en una transacción que se deshace al terminar, como
 *       {@code RefreshDatabase}: la base queda igual que antes.</li>
 *   <li>Ofrece los mismos ayudantes que Laravel: {@code actingAs}, {@code get},
 *       {@code post}, {@code assertRedirect}, {@code assertSessionHas},
 *       {@code assertSee}, {@code assertDatabaseHas} y
 *       {@code assertDatabaseMissing}, más fábricas para crear datos.</li>
 *   <li>Solo se simula el envío de correo (como {@code Mail::fake()}), porque es
 *       un servicio externo.</li>
 * </ul>
 */
@Tag("feature")
// El correo se simula; se apaga el chequeo de salud de correo de Actuator, que
// necesita el remitente real.
@SpringBootTest(properties = "management.health.mail.enabled=false")
// Sin imprimir el resultado al fallar: el toString de Lombok entre Registro y
// Consumo es circular y taparía el error real con un StackOverflowError.
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Transactional
public abstract class FeatureTestCase {

    @Autowired protected MockMvc mockMvc;
    @Autowired protected JdbcTemplate jdbc;
    @Autowired protected EntityManager entityManager;
    @Autowired protected PasswordEncoder passwordEncoder;
    @Autowired protected IUsuarioRepository usuarios;
    @Autowired protected IClienteRepository clientes;
    @Autowired protected ICategoriaRepository categorias;
    @Autowired protected IHabitacionRepository habitaciones;
    @Autowired protected IRegistroRepository registros;
    @Autowired protected IConsumoRepository consumos;
    @Autowired protected IProductoConsumoRepository productos;

    @MockBean
    protected JavaMailSender mailSender;

    private RequestPostProcessor sesion;
    private String ip = "127.0.0.1";

    // ------------------------------------------------------------------
    // Sesión y peticiones HTTP
    // ------------------------------------------------------------------

    /** Equivalente a {@code $this->actingAs($usuario)}. */
    protected FeatureTestCase actingAs(Usuario usuario) {
        sesion = usuario.isEsAdmin()
            ? user(usuario.getUsername()).roles("ADMIN", "STAFF")
            : user(usuario.getUsername()).roles("STAFF");
        return this;
    }

    /** Petición sin sesión iniciada (invitado). */
    protected FeatureTestCase comoInvitado() {
        sesion = null;
        return this;
    }

    protected FeatureTestCase desdeLaIp(String ip) {
        this.ip = ip;
        return this;
    }

    protected MvcResult get(String ruta, Object... parametros) throws Exception {
        return ejecutar(MockMvcRequestBuilders.get(ruta), parametros);
    }

    protected MvcResult post(String ruta, Object... parametros) throws Exception {
        return ejecutar(MockMvcRequestBuilders.post(ruta).with(csrf()), parametros);
    }

    /** Envía los parámetros como pares nombre, valor (igual que un formulario). */
    private MvcResult ejecutar(MockHttpServletRequestBuilder peticion, Object... parametros) throws Exception {
        for (int k = 0; k + 1 < parametros.length; k += 2) {
            Object valor = parametros[k + 1];
            if (valor instanceof List<?> lista) {
                peticion = peticion.param(parametros[k].toString(), lista.stream().map(String::valueOf).toArray(String[]::new));
            } else {
                peticion = peticion.param(parametros[k].toString(), String.valueOf(valor));
            }
        }
        if (sesion != null) {
            peticion = peticion.with(sesion);
        }
        final String ipPeticion = ip;
        peticion = peticion.with(request -> { request.setRemoteAddr(ipPeticion); return request; });
        // Como en producción, cada petición empieza con una sesión de JPA limpia.
        refrescar();
        MvcResult resultado = mockMvc.perform(peticion).andReturn();
        refrescar();
        return resultado;
    }

    /** Escribe en la base lo pendiente y descarta lo que JPA tenga en memoria. */
    protected void refrescar() {
        entityManager.flush();
        entityManager.clear();
    }

    // ------------------------------------------------------------------
    // Verificaciones de respuesta
    // ------------------------------------------------------------------

    protected void assertStatus(MvcResult resultado, int status) {
        assertEquals(status, resultado.getResponse().getStatus());
    }

    protected void assertOk(MvcResult resultado) {
        assertStatus(resultado, 200);
    }

    protected void assertForbidden(MvcResult resultado) {
        assertStatus(resultado, 403);
    }

    protected void assertRedirect(MvcResult resultado, String destino) {
        assertEquals(destino, resultado.getResponse().getRedirectedUrl());
    }

    /** Mensaje flash (redirección) o de modelo (vista) con la clave indicada. */
    protected void assertSessionHas(MvcResult resultado, String clave) {
        assertNotNull(mensaje(resultado, clave), "No llegó el mensaje '" + clave + "'");
    }

    protected void assertSessionHas(MvcResult resultado, String clave, String texto) {
        String valor = mensaje(resultado, clave);
        assertNotNull(valor, "No llegó el mensaje '" + clave + "'");
        assertTrue(valor.contains(texto), "Se esperaba '" + texto + "' y llegó: " + valor);
    }

    private String mensaje(MvcResult resultado, String clave) {
        Object flash = resultado.getFlashMap().get(clave);
        if (flash != null) {
            return flash.toString();
        }
        if (resultado.getModelAndView() != null && resultado.getModelAndView().getModel().get(clave) != null) {
            return resultado.getModelAndView().getModel().get(clave).toString();
        }
        return null;
    }

    /** Equivalente a {@code $response->assertSee('texto')}. */
    protected void assertSee(MvcResult resultado, String texto) throws Exception {
        String cuerpo = resultado.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(cuerpo.contains(texto), "La respuesta no contiene '" + texto + "'");
    }

    protected void assertDontSee(MvcResult resultado, String texto) throws Exception {
        String cuerpo = resultado.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(!cuerpo.contains(texto), "La respuesta no debería contener '" + texto + "'");
    }

    protected Object modelo(MvcResult resultado, String atributo) {
        return resultado.getModelAndView().getModel().get(atributo);
    }

    // ------------------------------------------------------------------
    // Verificaciones de base de datos
    // ------------------------------------------------------------------

    /** Equivalente a {@code assertDatabaseHas('tabla', [...])}. */
    protected void assertDatabaseHas(String tabla, Map<String, ?> columnas) {
        assertTrue(contar(tabla, columnas) > 0, "No hay filas en " + tabla + " con " + columnas);
    }

    /** Equivalente a {@code assertDatabaseMissing('tabla', [...])}. */
    protected void assertDatabaseMissing(String tabla, Map<String, ?> columnas) {
        assertEquals(0, contar(tabla, columnas), "No debería haber filas en " + tabla + " con " + columnas);
    }

    protected void assertDatabaseCount(String tabla, Map<String, ?> columnas, int cantidad) {
        assertEquals(cantidad, contar(tabla, columnas), "Filas en " + tabla + " con " + columnas);
    }

    protected int contar(String tabla, Map<String, ?> columnas) {
        refrescar();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM ").append(tabla);
        List<Object> valores = new ArrayList<>();
        String unir = " WHERE ";
        for (Map.Entry<String, ?> c : columnas.entrySet()) {
            if (c.getValue() == null) {
                sql.append(unir).append(c.getKey()).append(" IS NULL");
            } else {
                sql.append(unir).append(c.getKey()).append(" = ?");
                valores.add(c.getValue() instanceof Enum<?> e ? e.name() : c.getValue());
            }
            unir = " AND ";
        }
        return jdbc.queryForObject(sql.toString(), Integer.class, valores.toArray());
    }

    protected <T> T columna(String sql, Class<T> tipo, Object... argumentos) {
        refrescar();
        return jdbc.queryForObject(sql, tipo, argumentos);
    }

    // ------------------------------------------------------------------
    // Fábricas de datos (equivalentes a Modelo::create([...]))
    // ------------------------------------------------------------------

    protected Usuario crearAdmin(String username) {
        return crearUsuario(username, "Secreto2026", true, true);
    }

    protected Usuario crearRecepcionista(String username) {
        return crearUsuario(username, "Secreto2026", false, true);
    }

    protected Usuario crearUsuario(String username, String contrasena, boolean esAdmin, boolean activo) {
        Usuario u = new Usuario();
        u.setUsername(username);
        u.setNombres("Usuario");
        u.setApellidos("Prueba");
        u.setCorreo(username + "@tunki.test");
        u.setContrasena(passwordEncoder.encode(contrasena));
        u.setEsAdmin(esAdmin);
        u.setActivo(activo);
        return usuarios.save(u);
    }

    protected Categoria crearCategoria(String nombre, double precio) {
        Categoria c = new Categoria();
        c.setNombre(nombre);
        c.setPrecio(precio);
        c.setDescripcion("Categoría de prueba");
        return categorias.save(c);
    }

    protected Categoria crearCategoriaConTemporada(String nombre, double precio, double precioAlto, String desde, String hasta) throws Exception {
        Categoria c = crearCategoria(nombre, precio);
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
        c.setPrecioTemporadaAlta(precioAlto);
        c.setTemporadaAltaInicio(f.parse(desde));
        c.setTemporadaAltaFin(f.parse(hasta));
        return categorias.save(c);
    }

    protected Habitacion crearHabitacion(String numero, Categoria categoria, int aforo) {
        return crearHabitacion(numero, categoria, aforo, EstadoHabitacion.DISPONIBLE);
    }

    protected Habitacion crearHabitacion(String numero, Categoria categoria, int aforo, EstadoHabitacion estado) {
        Habitacion h = new Habitacion();
        h.setNumero(numero);
        h.setPiso(9);
        h.setAforo(aforo);
        h.setCategoria(categoria);
        h.setEstado(estado);
        return habitaciones.save(h);
    }

    protected Cliente crearCliente(String nombres, String apellidos, String documento) {
        Cliente c = new Cliente();
        c.setNombres(nombres);
        c.setApellidos(apellidos);
        c.setIdentificacion(documento);
        c.setCiudad("Oxapampa");
        c.setPais("Perú");
        return clientes.save(c);
    }

    protected Registro crearEstadia(Cliente cliente, Habitacion habitacion, Date checkIn, int noches, double tarifa, double pagado, EstadoRegistro estado, Usuario usuario) {
        Registro r = new Registro();
        r.setCliente(cliente);
        r.setHabitacion(habitacion);
        r.setCheckIn(checkIn);
        r.setNoches(noches);
        r.setNumHuespedes(1);
        r.setTarifa(tarifa);
        r.setMontoPagado(pagado);
        r.setEstado(estado);
        r.setUsuario(usuario);
        if (estado == EstadoRegistro.ACTIVA) {
            habitacion.setEstado(EstadoHabitacion.OCUPADA);
            habitaciones.save(habitacion);
        }
        return registros.save(r);
    }

    protected ProductoConsumo crearProducto(String nombre, double precio, boolean activo) {
        ProductoConsumo p = new ProductoConsumo();
        p.setNombre(nombre);
        p.setPrecioSugerido(precio);
        p.setActivo(activo);
        return productos.save(p);
    }

    protected Consumo crearConsumo(Registro registro, String descripcion, double monto, ProductoConsumo producto) {
        Consumo c = new Consumo();
        c.setRegistro(registro);
        c.setDescripcion(descripcion);
        c.setMonto(monto);
        c.setProducto(producto);
        return consumos.save(c);
    }

    /** Fecha relativa a hoy a una hora fija (para estadías de hoy o vencidas). */
    protected Date diasDesdeHoy(int dias, int hora) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, dias);
        cal.set(Calendar.HOUR_OF_DAY, hora);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    protected Date fecha(String yyyyMMddTHHmm) throws Exception {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm").parse(yyyyMMddTHHmm);
    }
}
