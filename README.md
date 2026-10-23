# Estadía — Sistema Hotel

![Pipeline de pruebas](https://github.com/AlexAmancio/sistema-hotel/actions/workflows/tests.yml/badge.svg)
![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1.4-brightgreen)
![MySQL](https://img.shields.io/badge/MySQL-8-blue)

Sistema web de check-in / check-out para hoteles, desarrollado con Spring Boot.
Cubre el ciclo completo de una estadía: reservar, registrar la entrada, cargar
consumos durante la estancia, hacer el check-out y emitir el comprobante — con
un panel de reportes y un calendario de ocupación para el día a día del hotel.

## ¿Qué hace?

- **Check-in / check-out** de huéspedes, con cálculo automático de tarifa,
  noches y saldo pendiente. La tarifa respeta temporada alta si la categoría
  de la habitación la tiene configurada.
- **Reservas múltiples**: una reserva puede cubrir varias habitaciones a la
  vez, cada una con su propio aforo y huéspedes; se valida aforo y
  solapamiento de fechas por cada habitación antes de confirmar nada.
- **Extensión de estadía**, con recálculo de tarifa y validación de que no se
  cruce con otra reserva activa de la misma habitación.
- Gestión de **clientes, habitaciones, categorías y consumos** adicionales
  durante la estadía.
- **Autocompletado de datos del cliente por DNI**, vía apiperu.dev (consulta
  server-side; el token nunca viaja al navegador).
- **Calendario visual de ocupación**: un clic en un día libre abre el
  formulario de reserva con la habitación y la fecha ya precargadas.
- **Reportes**: KPIs del día (ocupación, check-ins, saldo pendiente) e
  ingresos / ocupación promedio por período, en una sola pantalla.
- **Búsqueda global** de clientes, habitaciones y registros.
- **Comprobantes internos** (demostración, no válidos ante SUNAT: serie,
  correlativo, IGV desglosado y un snapshot en JSON pensado para enchufar más
  adelante un OSE homologado sin rediseñar nada).
- **Usuarios con roles** (admin / staff), autoservicio de perfil y cambio de
  contraseña, y bloqueo temporal por intentos fallidos de login.

## Stack

| Capa | Tecnología |
|---|---|
| Lenguaje / runtime | Java 17 |
| Framework | Spring Boot 3.1.4 |
| Persistencia | Spring Data JPA (Hibernate) |
| Seguridad | Spring Security 6 |
| Vistas | Thymeleaf |
| Base de datos | MySQL |

## Arquitectura

Capas clásicas de Spring MVC: **Controller → Service (interfaz + implementación) →
Repository (Spring Data JPA) → MySQL**, con Thymeleaf renderizando las vistas
del lado del servidor. 13 controladores, 10 servicios y sus respectivos
repositorios, todos siguiendo el mismo patrón para que agregar un módulo
nuevo sea mecánico. Los servicios usan inyección por campo (`@Autowired` en
el atributo, sin constructor), por eso los tests los instancian con `new` y
les inyectan los mocks con `ReflectionTestUtils.setField(...)`.

Paquetes: `config` (seguridad y atributos globales de vista), `controllers`,
`interfaces` (contratos de los servicios), `modelos` (entidades JPA),
`repositories` (interfaces Spring Data), `security` (handlers de
autenticación), `servicios` (implementación) y `util` (fechas y texto).

### Modelo de datos

```
Cliente ──┐
          ├─< Registro >──── Habitacion ──> Categoria
Usuario ──┘      │  ↑
                  │  └── Reserva ──> Cliente / Usuario
                  └─< Consumo
                  └─1 Comprobante
```

- `Reserva` agrupa una o varias `Registro` (`@OneToMany(mappedBy = "reserva")`):
  una reserva de varias habitaciones genera un `Registro` por habitación, cada
  uno con su propio estado y fechas.
- `Registro` es el núcleo: referencia `Cliente`, `Habitacion`, `Usuario` (quién
  hizo el check-in) y, opcionalmente, la `Reserva` de la que viene. Tiene su
  lista de `Consumo` (`@OneToMany(mappedBy = "registro")`) y, al cerrar, un
  `Comprobante` (`@OneToOne`, único por registro).
- `Habitacion` referencia su `Categoria`, que define la tarifa (y una tarifa
  alternativa opcional para temporada alta).
- `Habitacion` y `Registro` tienen un campo `@Version` (bloqueo optimista de
  JPA): si dos personas editan el mismo registro a la vez, la segunda
  escritura falla en vez de pisar silenciosamente los datos de la primera —
  corrige un bug real que hubo antes, donde el estado de una habitación se
  sobrescribía sin darse cuenta.

### Login, sesión y bloqueo por intentos fallidos

`SecurityConfig` registra a `UsuarioService` como `UserDetailsService`.
`UsuarioServiceDetails` envuelve un `Usuario` como `UserDetails`: le da
`ROLE_STAFF` siempre y `ROLE_ADMIN` además si `esAdmin`, y `isEnabled()`
refleja el flag `activo` (un usuario desactivado no puede loguearse aunque
la contraseña sea correcta).

Antes de tocar la base de datos, `UsuarioService.loadUserByUsername()`
resuelve la IP real del request (`LoginAttemptService.resolveClientIp()`,
soporta `X-Forwarded-For` detrás de proxy) y pregunta si esa IP está
bloqueada. Si lo está, lanza `LockedException` sin siquiera buscar el
usuario — el bloqueo es **por IP**, no por cuenta, así que toda la
recepción compartiendo una misma salida a internet se bloquea junta después
de `security.login.maxAttempts` intentos fallidos (por defecto 3, libera a
los `security.login.lockTimeMinutes` minutos, por defecto 5).

Un login fallido lo captura `CustomAuthenticationFailureHandler`, que
registra el intento (`loginAttemptService.registerAttempt(ip, user, false)`)
y redirige a `/login?error=...` con un mensaje distinto según si esa IP ya
quedó bloqueada o no. Un login exitoso pasa por el `successHandler` del
`SecurityConfig`, que registra el intento como exitoso y limpia el contador.
Al cerrar sesión, el `logoutSuccessHandler` guarda `logoutTime` en el
usuario — es lo que después se usa para saber quién tiene sesión activa.
La sesión está limitada a una por usuario (`maximumSessions(1)` con un
`SessionRegistry`), y `GlobalModelAttributes` (un `@ControllerAdvice`)
inyecta el nombre y rol del usuario logueado en el modelo de cada vista, sin
que cada controlador tenga que hacerlo a mano.

### Reservas: aforo y solapamiento por habitación

`ReservaService.crear()` recibe una reserva que puede traer varias
habitaciones. Antes de persistir nada, valida **para cada habitación**
(no solo la primera) que el número de huéspedes no exceda el aforo y que
el rango de fechas no se solape con otro `Registro` activo de esa misma
habitación (`existeSolapamiento`). Todo corre dentro de una única
transacción (`@Transactional`): si cualquier habitación falla la
validación, no se crea ninguna — evita el caso de reservar 3 habitaciones y
que solo 2 queden realmente libres. La tarifa de cada noche se calcula con
`Categoria.precioParaFecha(fecha)`, que usa la tarifa de temporada alta si
la categoría tiene una ventana de fechas configurada para eso y la fecha
cae dentro.

### Comprobante: idempotencia, correlativo e IGV

`ComprobanteService.emitir(registro)` primero busca si ya existe un
comprobante para ese registro (`findByRegistroId`) y, si existe, lo
devuelve en vez de crear uno nuevo — emitir dos veces el comprobante de la
misma estadía no duplica nada. El correlativo se calcula como
`countBySerie("C001") + 1`, y el desglose de IGV sale de
`valorVenta = total / 1.18`, `igv = total - valorVenta`, redondeado a 2
decimales. Además arma un snapshot en JSON (con Jackson `ObjectMapper`) de
los datos del comprobante, pensado para que, el día que haga falta emitir
comprobantes válidos ante SUNAT, ese JSON alimente un OSE homologado (por
ejemplo Nubefact) sin tener que rediseñar el modelo. Estos comprobantes son
una demostración interna, no documentos tributarios válidos.

### Autocompletado de cliente por DNI

`ClientesController` expone `GET /clientes/dni/{dni}`, valida que el DNI
tenga 8 dígitos (`dni.matches("\\d{8}")`) y, si pasa, el propio backend
consulta `apiperu.dev` con un token leído de `application.properties`
(`apiperu.token`, configurable por variable de entorno `APIPERU_TOKEN`) y
le devuelve al navegador solo el nombre/apellido ya resuelto. El token
nunca se expone en el HTML ni en JavaScript del cliente.

## Cómo levantarlo

**1. Base de datos**

El repo incluye un script con toda la base de datos actual (clientes,
habitaciones, registros, consumos, usuarios, catálogo de productos, etc.) en
[`bd/respaldo_bd_actual.sql`](bd/respaldo_bd_actual.sql). Con MySQL corriendo
(se conecta a `root` sin contraseña en `localhost:3306` por defecto —
cambiá eso en `application.properties` si tu entorno es distinto):

```bash
mysql -uroot < bd/respaldo_bd_actual.sql
```

El script crea la base `alquiler` si no existe y carga todos los datos de una
sola vez — no hace falta crear la base a mano ni insertar un usuario para
poder loguearte, ya vienen incluidos.

**2. Usuario de prueba**

El script ya trae un usuario administrador de prueba:

| Usuario | Contraseña |
|---|---|
| `elon` | `admin123` |

También trae otras 8 cuentas de staff (sin permisos de administrador) con
distintas contraseñas no publicadas acá — para loguearte con alguna de ellas,
o para crear una cuenta nueva, usá el script de abajo.

Para crear otros usuarios sin pasar por la interfaz, hay un script que genera
el `insert` con la contraseña ya encriptada en bcrypt:

```bash
./scripts/crear-usuario.sh <username> <contraseña> <nombres> <apellidos> <correo> [admin]
```

Si preferís arrancar con una base vacía en vez de cargar todos los datos de
ejemplo, basta con crear una base `alquiler` vacía e insertar un solo
usuario:

```sql
insert into usuario (activo, apellidos, contrasena, correo, es_admin, nombres, username)
values (true, 'Musk', '$2a$12$gx4jBU180qN4ohLJiyokkuRMZpnhtVmlsVJoCQFrKIeHaXTjI5m5G', 'musk@gmail.com', true, 'Elon', 'elon');
```

**3. Arrancar la app**

```bash
./mvnw spring-boot:run
```

La app queda en `http://localhost:8090`.

## Con Docker

```bash
docker build -t sistema-hotel .
docker run -p 8090:8090 sistema-hotel
```

Requiere una base MySQL accesible desde el contenedor (ajustá
`spring.datasource.url` según corresponda).

## Pruebas

```bash
./mvnw test
```

61 pruebas automatizadas en tres niveles:

- **Unitarias puras** (`TextoUtilsTest`, `RegistroTest`, `UsuarioServiceDetailsTest`):
  sin Spring, sin base de datos.
- **Unitarias con Mockito** (`RegistroServiceTest`, `LoginAttemptServiceTest` y los
  7 servicios restantes: `CategoriaService`, `ClienteService`, `ComprobanteService`,
  `ConsumoService`, `HabitacionService`, `ReservaService`, `UsuarioService`):
  repositorios mockeados, sin levantar contexto de Spring.
- **De integración** (`ConsumoControllerTest`, `SistemahotelApplicationTests`):
  contexto de Spring completo con MockMvc, contra MySQL real.

## Integración continua

Cada push y cada pull request a `main` disparan el pipeline
[`pipeline-tests`](.github/workflows/tests.yml), que corre los tres grupos de
pruebas en paralelo (las dos primeras no esperan a la base de datos) y publica
un resumen de pasadas/falladas en cada ejecución.

## Equipo — Grupo 5

| Integrante | Rol |
|---|---|
| Alex Valencia Amancio | Director de Proyecto |
| Ariana Milagros Bayona Abad | Integrante del equipo |
| Jaime David Becerra Llamo | Integrante del equipo |
| Jerson Budchin Burga Espinoza | Integrante del equipo |
| Tania Medali Cadillo Pipa | Integrante del equipo |
| Adrian Anderson Castro Cuba Angeles | Integrante del equipo |
| Jose Sair Flavio Herrera Canchari | Integrante del equipo |
