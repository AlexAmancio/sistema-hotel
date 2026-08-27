## Pasos para arrancar el programa

**1. Toma en cuenta los puertos**

Considera configurar los puertos del servidor web y base de datos en el archivo
`application.properties`.

**2. Crea una base de datos `alquiler`**

El aplicativo buscará la db `alquiler` pero puedes personalizar el nombre en `aplication.properties`.

**3. Crea un usuario administrador**

Necesitas crear un usuario administrador en tu base de datos para poder registrarte
en el sistema.

El siguiente script crea un usuario con username "elon" y contraseña "admin123" (encriptada por Bcrypt)

```sql
insert into usuario (apellidos, contrasena, correo, es_admin, nombres, username) values ('Musk', '$2a$12$gx4jBU180qN4ohLJiyokkuRMZpnhtVmlsVJoCQFrKIeHaXTjI5m5G', 'musk@gmail.com', true, 'Elon', 'elon');
```
