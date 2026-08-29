# Estadía — Sistema Hotel

Sistema web de check-in / check-out para hoteles, desarrollado con Spring Boot. Proyecto
de la materia Administración de Proyectos de Software.

## ¿Qué hace?

- Registro de check-in y check-out de huéspedes, con cálculo automático de tarifa,
  noches y saldo pendiente.
- Reservas múltiples: una reserva puede cubrir varias habitaciones a la vez, cada una
  con su propio aforo y huéspedes.
- Gestión de clientes, habitaciones, categorías y consumos adicionales durante la estadía.
- Emisión de comprobantes internos (demostración, no válidos ante SUNAT).
- Usuarios con roles (admin / staff) y control de acceso por Spring Security, incluyendo
  bloqueo temporal por intentos fallidos de login.

## Stack

- Java 17, Spring Boot 3.1.4
- Spring Data JPA (Hibernate), Spring Security 6, Thymeleaf
- MySQL

## Cómo levantarlo

**1. Base de datos**

Creá una base de datos MySQL llamada `alquiler` (o cambiá el nombre en
`application.properties`). Por defecto se conecta a `root` sin contraseña en
`localhost:3306`.

**2. Usuario de prueba**

Para poder loguearte, insertá un usuario administrador:

```sql
insert into usuario (apellidos, contrasena, correo, es_admin, nombres, username)
values ('Musk', '$2a$12$gx4jBU180qN4ohLJiyokkuRMZpnhtVmlsVJoCQFrKIeHaXTjI5m5G', 'musk@gmail.com', true, 'Elon', 'elon');
```

Usuario: `elon` — Contraseña: `admin123`

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

## Tests

```bash
./mvnw test
```
