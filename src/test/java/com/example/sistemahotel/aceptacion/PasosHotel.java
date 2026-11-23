package com.example.sistemahotel.aceptacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.example.sistemahotel.modelos.Categoria;
import com.example.sistemahotel.modelos.Cliente;
import com.example.sistemahotel.modelos.Comprobante;
import com.example.sistemahotel.modelos.Consumo;
import com.example.sistemahotel.modelos.EstadoHabitacion;
import com.example.sistemahotel.modelos.EstadoRegistro;
import com.example.sistemahotel.modelos.Habitacion;
import com.example.sistemahotel.modelos.ProductoConsumo;
import com.example.sistemahotel.modelos.Registro;
import com.example.sistemahotel.modelos.Usuario;
import com.example.sistemahotel.repositories.ICategoriaRepository;
import com.example.sistemahotel.repositories.IClienteRepository;
import com.example.sistemahotel.repositories.IComprobanteRepository;
import com.example.sistemahotel.repositories.IConsumoRepository;
import com.example.sistemahotel.repositories.IHabitacionRepository;
import com.example.sistemahotel.repositories.IProductoConsumoRepository;
import com.example.sistemahotel.repositories.IRegistroRepository;
import com.example.sistemahotel.repositories.IUsuarioRepository;

import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import io.cucumber.java.es.Y;
import jakarta.persistence.EntityManager;

// Pasos de las pruebas de aceptación (BDD). Los datos de cada escenario se crean con los
// repositorios reales y las acciones pasan por las mismas rutas HTTP que usa la
// pantalla (con seguridad, CSRF y validaciones reales). Antes de verificar se
// sincroniza la sesión de JPA para leer lo que realmente quedó en MySQL.
public class PasosHotel {

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ICategoriaRepository categoriaRepositorio;
    @Autowired private IHabitacionRepository habitacionRepositorio;
    @Autowired private IClienteRepository clienteRepositorio;
    @Autowired private IUsuarioRepository usuarioRepositorio;
    @Autowired private IRegistroRepository registroRepositorio;
    @Autowired private IConsumoRepository consumoRepositorio;
    @Autowired private IComprobanteRepository comprobanteRepositorio;
    @Autowired private IProductoConsumoRepository productoRepositorio;

    private RequestPostProcessor sesion;
    private String usuarioEnSesion;
    private Registro estadia;
    private MvcResult resultado;
    private long registrosAntes;

    // ---------- utilidades ----------

    private void sincronizar() {
        entityManager.flush();
        entityManager.clear();
    }

    private MvcResult ejecutar(MockHttpServletRequestBuilder peticion) throws Exception {
        if (sesion != null) {
            peticion = peticion.with(sesion);
        }
        // Como en la aplicación real, cada petición empieza con una sesión de JPA
        // limpia y lo que hizo queda escrito antes de la siguiente.
        sincronizar();
        resultado = mockMvc.perform(peticion.with(csrf())).andReturn();
        sincronizar();
        return resultado;
    }

    private Categoria categoria(String nombre) {
        return StreamSupport.stream(categoriaRepositorio.findAll().spliterator(), false)
            .filter(c -> nombre.equals(c.getNombre())).findFirst().orElseThrow();
    }

    private Habitacion habitacion(String numero) {
        return habitacionRepositorio.findByNumero(numero).orElseThrow();
    }

    private Cliente cliente(String documento) {
        return clienteRepositorio.findByIdentificacion(documento).orElseThrow();
    }

    private List<Registro> estadiasDe(Habitacion hab) {
        return registroRepositorio.findAll().stream()
            .filter(r -> r.getHabitacion() != null && r.getHabitacion().getId() == hab.getId())
            .collect(Collectors.toList());
    }

    private Registro estadiaActual() {
        sincronizar();
        return registroRepositorio.findById(estadia.getId()).orElseThrow();
    }

    private String mensajeDeError() throws Exception {
        Object flash = resultado.getFlashMap().get("error");
        if (flash != null) {
            return flash.toString();
        }
        if (resultado.getModelAndView() != null && resultado.getModelAndView().getModel().get("error") != null) {
            return resultado.getModelAndView().getModel().get("error").toString();
        }
        return resultado.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    }

    private Usuario crearUsuario(String username, String contrasena, boolean esAdmin, boolean activo) {
        Usuario u = new Usuario();
        u.setUsername(username);
        u.setNombres("Usuario");
        u.setApellidos("Prueba");
        u.setCorreo(username + "@ejemplo.test");
        u.setContrasena(passwordEncoder.encode(contrasena));
        u.setEsAdmin(esAdmin);
        u.setActivo(activo);
        return usuarioRepositorio.save(u);
    }

    // ---------- datos del escenario ----------

    @Dado("una categoría {string} con precio {double}")
    public void unaCategoria(String nombre, double precio) {
        Categoria c = new Categoria();
        c.setNombre(nombre);
        c.setPrecio(precio);
        categoriaRepositorio.save(c);
    }

    @Dado("una categoría {string} con precio {double} y temporada alta a {double} del {string} al {string}")
    public void unaCategoriaConTemporada(String nombre, double precio, double alto, String desde, String hasta) throws Exception {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
        Categoria c = new Categoria();
        c.setNombre(nombre);
        c.setPrecio(precio);
        c.setPrecioTemporadaAlta(alto);
        c.setTemporadaAltaInicio(f.parse(desde));
        c.setTemporadaAltaFin(f.parse(hasta));
        categoriaRepositorio.save(c);
    }

    @Y("una habitación {string} de la categoría {string} con aforo {int}")
    public void unaHabitacion(String numero, String categoria, int aforo) {
        Habitacion h = new Habitacion();
        h.setNumero(numero);
        h.setPiso(9);
        h.setAforo(aforo);
        h.setCategoria(categoria(categoria));
        h.setEstado(EstadoHabitacion.DISPONIBLE);
        habitacionRepositorio.save(h);
    }

    @Y("un cliente {string} con documento {string}")
    public void unCliente(String nombreCompleto, String documento) {
        String[] partes = nombreCompleto.split(" ", 2);
        Cliente c = new Cliente();
        c.setNombres(partes[0]);
        c.setApellidos(partes.length > 1 ? partes[1] : "Prueba");
        c.setIdentificacion(documento);
        clienteRepositorio.save(c);
    }

    @Y("el administrador {string} con sesión iniciada")
    public void elAdministradorConSesion(String username) {
        crearUsuario(username, "ClaveQA2026", true, true);
        usuarioEnSesion = username;
        sesion = user(username).roles("ADMIN", "STAFF");
    }

    @Y("la recepcionista {string} con sesión iniciada")
    public void laRecepcionistaConSesion(String username) {
        crearUsuario(username, "ClaveQA2026", false, true);
        usuarioEnSesion = username;
        sesion = user(username).roles("STAFF");
    }

    @Dado("un usuario de recepción {string} con contraseña {string}")
    public void unUsuarioActivo(String username, String contrasena) {
        crearUsuario(username, contrasena, false, true);
    }

    @Dado("un usuario desactivado {string} con contraseña {string}")
    public void unUsuarioDesactivado(String username, String contrasena) {
        crearUsuario(username, contrasena, false, false);
    }

    @Y("una estadía activa del cliente {string} en la habitación {string} el {string} por {int} noches con tarifa {double} y pago {double}")
    public void unaEstadiaActiva(String documento, String numero, String checkIn, int noches, double tarifa, double pago) throws Exception {
        Habitacion h = habitacion(numero);
        h.setEstado(EstadoHabitacion.OCUPADA);
        habitacionRepositorio.save(h);
        Registro r = new Registro();
        r.setCliente(cliente(documento));
        r.setHabitacion(h);
        r.setCheckIn(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm").parse(checkIn));
        r.setNoches(noches);
        r.setNumHuespedes(1);
        r.setTarifa(tarifa);
        r.setMontoPagado(pago);
        r.setEstado(EstadoRegistro.ACTIVA);
        if (usuarioEnSesion != null) {
            r.setUsuario(usuarioRepositorio.findByUsername(usuarioEnSesion).orElse(null));
        }
        estadia = registroRepositorio.save(r);
        sincronizar();
    }

    @Y("un producto del catálogo {string} a {double} {string}")
    public void unProducto(String nombre, double precio, String estado) {
        ProductoConsumo p = new ProductoConsumo();
        p.setNombre(nombre);
        p.setPrecioSugerido(precio);
        p.setActivo("activo".equals(estado));
        productoRepositorio.save(p);
    }

    // ---------- acciones ----------

    @Cuando("registro un check-in del cliente {string} en la habitación {string} el {string} por {int} noches con {int} huéspedes")
    public void registroUnCheckIn(String documento, String numero, String checkIn, int noches, int huespedes) throws Exception {
        Habitacion h = habitacion(numero);
        registrosAntes = registroRepositorio.count();
        // Igual que la pantalla: primero pide la tarifa al servidor y luego guarda.
        String tarifa = ejecutar(get("/habitaciones/calcular-tarifa/" + h.getId())
            .param("noches", String.valueOf(noches)).param("checkIn", checkIn)).getResponse().getContentAsString();
        ejecutar(post("/guardar/")
            .param("cliente", String.valueOf(cliente(documento).getId()))
            .param("habitacion", String.valueOf(h.getId()))
            .param("checkIn", checkIn)
            .param("noches", String.valueOf(noches))
            .param("numHuespedes", String.valueOf(huespedes))
            .param("tarifa", tarifa)
            .param("montoPagado", "0")
            .param("metodoPago", "EFECTIVO")
            .param("estado", "ACTIVA"));
        sincronizar();
        List<Registro> estadias = estadiasDe(h);
        estadia = estadias.isEmpty() ? null : estadias.get(estadias.size() - 1);
    }

    @Cuando("reservo para el cliente {string} las habitaciones {string} el {string} por {int} noches con {string} huéspedes")
    public void reservo(String documento, String numeros, String checkIn, int noches, String huespedes) throws Exception {
        registrosAntes = registroRepositorio.count();
        List<String> lista = Arrays.asList(numeros.split(","));
        List<String> cantidades = Arrays.asList(huespedes.split(","));
        MockHttpServletRequestBuilder peticion = post("/reservas/guardar/")
            .param("cliente", String.valueOf(cliente(documento).getId()))
            .param("checkIn", checkIn)
            .param("noches", String.valueOf(noches));
        List<String> ids = new ArrayList<>();
        for (int k = 0; k < lista.size(); k++) {
            Habitacion h = habitacion(lista.get(k).trim());
            ids.add(String.valueOf(h.getId()));
            peticion = peticion.param("huespedes_" + h.getId(), cantidades.get(k).trim());
        }
        peticion = peticion.param("habitaciones", ids.toArray(new String[0]));
        ejecutar(peticion);
        sincronizar();
    }

    @Cuando("agrego el consumo {string} de {double} a la estadía")
    public void agregoConsumo(String descripcion, double monto) throws Exception {
        ejecutar(post("/consumos/agregar/" + estadia.getId() + "/").param("descripcion", descripcion).param("monto", String.valueOf(monto)));
    }

    @Cuando("cargo el producto {string} desde su atajo a la estadía")
    public void cargoProductoDesdeAtajo(String nombre) throws Exception {
        ProductoConsumo p = StreamSupport.stream(productoRepositorio.findAll().spliterator(), false)
            .filter(x -> nombre.equals(x.getNombre())).findFirst().orElseThrow();
        ejecutar(post("/consumos/agregar/" + estadia.getId() + "/").param("productoId", String.valueOf(p.getId()))
            .param("descripcion", p.getNombre()).param("monto", String.valueOf(p.getPrecioSugerido())));
    }

    @Cuando("intento cancelar la estadía")
    public void intentoCancelar() throws Exception {
        ejecutar(post("/cancelar/" + estadia.getId() + "/"));
    }

    @Cuando("marco la estadía como no-show")
    public void marcoNoShow() throws Exception {
        ejecutar(post("/no-show/" + estadia.getId() + "/"));
    }

    @Cuando("hago el check-out de la estadía")
    public void hagoCheckout() throws Exception {
        ejecutar(post("/checkout/" + estadia.getId() + "/"));
    }

    @Cuando("emito el comprobante de la estadía")
    public void emitoComprobante() throws Exception {
        ejecutar(post("/comprobantes/emitir/" + estadia.getId() + "/"));
    }

    @Cuando("registro al cliente {string} {string} con documento {string}")
    public void registroCliente(String nombres, String apellidos, String documento) throws Exception {
        ejecutar(post("/clientes/guardar/").param("nombres", nombres).param("apellidos", apellidos)
            .param("identificacion", documento).param("tipoDocumento", "DNI"));
        sincronizar();
    }

    @Cuando("uso el alta rápida para {string} {string} con documento {string}")
    public void usoAltaRapida(String nombres, String apellidos, String documento) throws Exception {
        ejecutar(post("/clientes/crear-rapido/").param("nombres", nombres).param("apellidos", apellidos).param("identificacion", documento));
        sincronizar();
    }

    @Cuando("intento abrir {string}")
    public void intentoAbrir(String ruta) throws Exception {
        ejecutar(get(ruta));
    }

    @Cuando("abro los consumos de la estadía")
    public void abroConsumos() throws Exception {
        ejecutar(get("/consumos/" + estadia.getId() + "/"));
    }

    @Cuando("intento eliminar el producto {string}")
    public void intentoEliminarProducto(String nombre) throws Exception {
        ProductoConsumo p = StreamSupport.stream(productoRepositorio.findAll().spliterator(), false)
            .filter(x -> nombre.equals(x.getNombre())).findFirst().orElseThrow();
        ejecutar(post("/productos-consumo/eliminar/" + p.getId() + "/"));
        sincronizar();
    }

    @Cuando("inicio sesión como {string} con contraseña {string} desde la IP {string}")
    public void inicioSesion(String username, String contrasena, String ip) throws Exception {
        sesion = null;
        resultado = mockMvc.perform(post("/login").param("username", username).param("password", contrasena).with(csrf())
            .with(request -> { request.setRemoteAddr(ip); return request; })).andReturn();
    }

    @Cuando("fallo {int} veces la contraseña de {string} desde la IP {string}")
    public void falloVeces(int veces, String username, String ip) throws Exception {
        for (int k = 0; k < veces; k++) {
            inicioSesion(username, "incorrecta-" + k, ip);
        }
    }

    // ---------- verificaciones ----------

    @Entonces("la estadía queda {string} con tarifa {double}")
    public void laEstadiaQueda(String estado, double tarifa) {
        assertNotNull(estadia, "No se creó la estadía. Respuesta: " + resultado.getFlashMap());
        Registro r = estadiaActual();
        assertEquals(EstadoRegistro.valueOf(estado), r.getEstado());
        assertEquals(tarifa, r.getTarifa(), 0.001);
    }

    @Y("la habitación {string} queda {string}")
    public void laHabitacionQueda(String numero, String estado) {
        sincronizar();
        assertEquals(EstadoHabitacion.valueOf(estado), habitacion(numero).getEstado());
    }

    @Y("la estadía queda a nombre de {string}")
    public void aNombreDe(String username) {
        assertEquals(username, estadiaActual().getUsuario().getUsername());
    }

    @Entonces("se muestra el error {string}")
    public void seMuestraElError(String mensaje) throws Exception {
        assertTrue(mensajeDeError().contains(mensaje), "Se esperaba el error \"" + mensaje + "\" y llegó: " + mensajeDeError());
    }

    @Y("no se crea ninguna estadía nueva")
    public void noSeCreaNinguna() {
        sincronizar();
        assertEquals(registrosAntes, registroRepositorio.count());
    }

    @Entonces("se crean {int} estadías activas para el cliente {string}")
    public void seCreanEstadias(int cantidad, String documento) {
        sincronizar();
        Cliente c = cliente(documento);
        long activas = registroRepositorio.findAll().stream()
            .filter(r -> r.getCliente() != null && r.getCliente().getId() == c.getId() && r.getEstado() == EstadoRegistro.ACTIVA).count();
        assertEquals(cantidad, activas);
        assertEquals(registrosAntes + cantidad, registroRepositorio.count());
    }

    @Entonces("el total de la estadía es {double} y el saldo pendiente es {double}")
    public void totalYSaldo(double total, double saldo) {
        Registro r = estadiaActual();
        r.setConsumos(StreamSupport.stream(consumoRepositorio.findAll().spliterator(), false)
            .filter(c -> c.getRegistro() != null && c.getRegistro().getId() == r.getId()).collect(Collectors.toList()));
        assertEquals(total, r.getTotalEstadia(), 0.001);
        assertEquals(saldo, r.getSaldoPendiente(), 0.001);
    }

    @Entonces("la estadía sigue {string}")
    public void laEstadiaSigue(String estado) {
        assertEquals(EstadoRegistro.valueOf(estado), estadiaActual().getEstado());
    }

    @Y("se registra la hora real de salida")
    public void horaRealDeSalida() {
        assertNotNull(estadiaActual().getCheckOutReal());
    }

    @Y("la estadía tiene {int} consumo(s)")
    public void consumosDeLaEstadia(int cantidad) {
        sincronizar();
        long n = StreamSupport.stream(consumoRepositorio.findAll().spliterator(), false)
            .filter(c -> c.getRegistro() != null && c.getRegistro().getId() == estadia.getId()).count();
        assertEquals(cantidad, n);
    }

    @Entonces("el comprobante tiene serie {string}, valor de venta {double}, IGV {double} y total {double}")
    public void elComprobante(String serie, double valorVenta, double igv, double total) {
        sincronizar();
        Comprobante c = comprobanteRepositorio.findByRegistroId(estadia.getId()).orElseThrow();
        assertEquals(serie, c.getSerie());
        assertEquals(valorVenta, c.getValorVenta(), 0.001);
        assertEquals(igv, c.getIgv(), 0.001);
        assertEquals(total, c.getTotal(), 0.001);
    }

    @Y("la estadía tiene un solo comprobante")
    public void unSoloComprobante() {
        sincronizar();
        long n = StreamSupport.stream(comprobanteRepositorio.findAll().spliterator(), false)
            .filter(c -> c.getRegistro() != null && c.getRegistro().getId() == estadia.getId()).count();
        assertEquals(1, n);
    }

    @Entonces("el cliente con documento {string} queda guardado como {string}")
    public void clienteGuardadoComo(String documento, String nombreCompleto) {
        Cliente c = cliente(documento);
        assertEquals(nombreCompleto, c.getNombres() + " " + c.getApellidos());
    }

    @Y("solo hay {int} cliente(s) con el documento {string}")
    public void soloHayClientes(int cantidad, String documento) {
        long n = clienteRepositorio.findAll().stream().filter(c -> documento.equals(c.getIdentificacion())).count();
        assertEquals(cantidad, n);
    }

    @Entonces("el acceso es denegado")
    public void accesoDenegado() {
        assertEquals(403, resultado.getResponse().getStatus());
    }

    @Entonces("entra al sistema")
    public void entraAlSistema() {
        assertEquals("/", resultado.getResponse().getRedirectedUrl());
    }

    @Entonces("vuelve al inicio de sesión con el aviso {string}")
    public void vuelveAlLogin(String aviso) throws Exception {
        String destino = java.net.URLDecoder.decode(resultado.getResponse().getRedirectedUrl(), "UTF-8");
        assertTrue(destino.startsWith("/login?error"), "Destino: " + destino);
        assertTrue(destino.contains(aviso), "Destino: " + destino);
    }

    @Entonces("el producto {string} no aparece entre los atajos")
    public void noApareceEntreAtajos(String nombre) {
        @SuppressWarnings("unchecked")
        List<ProductoConsumo> atajos = (List<ProductoConsumo>) resultado.getModelAndView().getModel().get("listaProductos");
        assertFalse(atajos.stream().anyMatch(p -> nombre.equals(p.getNombre())));
    }

    @Entonces("el producto {string} aparece entre los atajos")
    public void apareceEntreAtajos(String nombre) {
        @SuppressWarnings("unchecked")
        List<ProductoConsumo> atajos = (List<ProductoConsumo>) resultado.getModelAndView().getModel().get("listaProductos");
        assertTrue(atajos.stream().anyMatch(p -> nombre.equals(p.getNombre())));
    }

    @Y("el producto {string} sigue en el catálogo")
    public void sigueEnElCatalogo(String nombre) {
        assertTrue(StreamSupport.stream(productoRepositorio.findAll().spliterator(), false).anyMatch(p -> nombre.equals(p.getNombre())));
    }

    @Y("la respuesta indica el nombre {string}")
    public void respuestaNombre(String nombre) throws Exception {
        String json = resultado.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(json.contains(nombre), "Respuesta: " + json);
    }

    // Referencias para que el compilador no marque estos tipos como sin uso.
    @SuppressWarnings("unused")
    private static final Class<?>[] TIPOS = {Consumo.class, Map.class};
}
