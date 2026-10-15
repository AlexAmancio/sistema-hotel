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
nuevo sea mecánico.

## Cómo levantarlo

**1. Base de datos**

Creá una base de datos MySQL llamada `alquiler` (o cambiá el nombre en
`application.properties`). Por defecto se conecta a `root` sin contraseña en
`localhost:3306`.

**2. Usuario de prueba**

Para poder loguearte, insertá un usuario administrador:

```sql
insert into usuario (activo, apellidos, contrasena, correo, es_admin, nombres, username)
values (true, 'Musk', '$2a$12$gx4jBU180qN4ohLJiyokkuRMZpnhtVmlsVJoCQFrKIeHaXTjI5m5G', 'musk@gmail.com', true, 'Elon', 'elon');
```

Usuario: `elon` — Contraseña: `admin123`

También hay un segundo usuario administrador de prueba:

```sql
insert into usuario (activo, apellidos, contrasena, correo, es_admin, nombres, username)
values (true, '', '$2a$10$FFF.RpX0QIMsOS8m.t13wuyr.naioWiRvNydZ.tvWrUX1FWxj79NK', 'gerardo@hoteltunki.com', true, 'Gerardo', 'gerardo');
```

Usuario: `gerardo` — Contraseña: `12345`

Para crear otros usuarios sin pasar por la interfaz, hay un script que genera
el `insert` con la contraseña ya encriptada en bcrypt:

```bash
./scripts/crear-usuario.sh <username> <contraseña> <nombres> <apellidos> <correo> [admin]
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
